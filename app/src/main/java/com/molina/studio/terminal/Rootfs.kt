package com.molina.studio.terminal

import android.content.Context
import android.system.Os
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

sealed interface RootfsState {
    data object Missing : RootfsState
    data class Downloading(val percent: Int) : RootfsState
    data object Verifying : RootfsState
    data class Extracting(val percent: Int) : RootfsState
    data object Ready : RootfsState
    data class Failed(val message: String) : RootfsState
}

private class CountingStream(input: InputStream) : FilterInputStream(input) {
    @Volatile var count = 0L
    override fun read(): Int {
        val r = super.read(); if (r >= 0) count++; return r
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val r = super.read(b, off, len); if (r > 0) count += r; return r
    }
}

/** Unduh, verifikasi SHA-256, dan ekstrak rootfs Debian trixie-slim arm64 resmi. */
object RootfsManager {
    // debuerreotype/docker-debian-artifacts, dipin ke commit (URL immutable)
    private const val ROOTFS_URL =
        "https://raw.githubusercontent.com/debuerreotype/docker-debian-artifacts/" +
        "9c4818e3bfd35139d3798b0420e9c46836658b2e/trixie/slim/oci/blobs/rootfs.tar.gz"
    private const val ROOTFS_SHA256 =
        "ddc0c1e36a6793411b6c8d48e1ddad2987405d377da92bcf532ed3181c2e4d62"
    private const val ROOTFS_SIZE = 30_200_478L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<RootfsState>(RootfsState.Missing)
    val state: StateFlow<RootfsState> = _state.asStateFlow()
    @Volatile private var running = false

    fun rootfsDir(ctx: Context) = File(ctx.filesDir, "rootfs/debian")
    private fun marker(ctx: Context) = File(ctx.filesDir, "rootfs/debian.ready")

    fun refresh(ctx: Context) {
        if (running) return
        _state.value =
            if (marker(ctx).exists() && rootfsDir(ctx).isDirectory) RootfsState.Ready
            else if (_state.value is RootfsState.Failed) _state.value
            else RootfsState.Missing
    }

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        synchronized(this) {
            if (running) return
            running = true
        }
        scope.launch {
            try {
                doInstall(app)
                _state.value = RootfsState.Ready
            } catch (e: Exception) {
                _state.value = RootfsState.Failed(e.message ?: e.javaClass.simpleName)
            } finally {
                running = false
            }
        }
    }

    private fun doInstall(ctx: Context) {
        val base = File(ctx.filesDir, "rootfs").apply { mkdirs() }
        val part = File(ctx.cacheDir, "debian-rootfs.tar.gz.part")
        val archive = File(ctx.cacheDir, "debian-rootfs.tar.gz")
        marker(ctx).delete()

        if (!(archive.exists() && sha256(archive) == ROOTFS_SHA256)) {
            download(part)
            _state.value = RootfsState.Verifying
            if (sha256(part) != ROOTFS_SHA256) {
                part.delete()
                error("Checksum SHA-256 tidak cocok, unduhan dibuang")
            }
            archive.delete()
            part.renameTo(archive)
        }

        val tmp = File(base, "debian.tmp")
        tmp.deleteRecursively()
        tmp.mkdirs()
        extract(archive, tmp)

        File(tmp, "etc/resolv.conf").apply {
            delete(); writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
        }
        File(tmp, "etc/hosts").apply {
            delete(); writeText("127.0.0.1 localhost\n::1 localhost ip6-localhost\n")
        }
        // apt di PRoot tidak bisa setgroups ke user _apt
        File(tmp, "etc/apt/apt.conf.d").mkdirs()
        File(tmp, "etc/apt/apt.conf.d/01-molina-proot")
            .writeText("APT::Sandbox::User \"root\";\n")

        val dst = rootfsDir(ctx)
        dst.deleteRecursively()
        if (!tmp.renameTo(dst)) error("Gagal memindahkan rootfs ke lokasi akhir")
        marker(ctx).writeText(ROOTFS_SHA256)
        archive.delete()
    }

    private fun download(dest: File) {
        val conn = (URL(ROOTFS_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode != 200) error("Unduhan gagal: HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: ROOTFS_SIZE
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var last = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        val p = ((done * 100) / total).toInt().coerceIn(0, 100)
                        if (p != last) { last = p; _state.value = RootfsState.Downloading(p) }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { i ->
            val b = ByteArray(64 * 1024)
            while (true) {
                val n = i.read(b)
                if (n < 0) break
                md.update(b, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extract(archive: File, root: File) {
        val rootPath = root.toPath().toAbsolutePath().normalize()
        val counting = CountingStream(archive.inputStream().buffered(256 * 1024))
        val total = archive.length()
        var last = -1
        TarArchiveInputStream(GZIPInputStream(counting, 64 * 1024)).use { tar ->
            while (true) {
                val e = (tar.nextEntry as? TarArchiveEntry) ?: break
                val name = e.name.removePrefix("./").trimStart('/')
                if (name.isEmpty()) continue
                val target = rootPath.resolve(name).normalize()
                if (!target.startsWith(rootPath)) continue // cegah path traversal
                val f = target.toFile()
                val perm = e.mode and 0xFFF
                when {
                    e.isDirectory -> {
                        f.mkdirs()
                        Os.chmod(f.path, perm or 0x1C0) // u+rwx
                    }
                    e.isSymbolicLink -> {
                        f.parentFile?.mkdirs()
                        f.delete()
                        Os.symlink(e.linkName, f.path)
                    }
                    e.isLink -> { // hardlink -> salinan (Android melarang hardlink)
                        val src = rootPath.resolve(e.linkName.removePrefix("./").trimStart('/')).normalize()
                        if (src.startsWith(rootPath)) {
                            f.parentFile?.mkdirs()
                            f.delete()
                            src.toFile().copyTo(f, overwrite = true)
                            Os.chmod(f.path, perm or 0x180) // u+rw
                        }
                    }
                    e.isFile -> {
                        f.parentFile?.mkdirs()
                        f.outputStream().use { out -> tar.copyTo(out, 64 * 1024) }
                        Os.chmod(f.path, perm or 0x180)
                    }
                    else -> {} // device/fifo dilewati (di-bind dari host oleh proot)
                }
                val p = if (total > 0) ((counting.count * 100) / total).toInt().coerceIn(0, 100) else 0
                if (p != last) { last = p; _state.value = RootfsState.Extracting(p) }
            }
        }
    }
}
