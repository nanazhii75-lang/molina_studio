#!/usr/bin/env python3
"""Tahap 2b: engine backup/restore rootfs (RootfsBackup) + penghapusan aman (SafeTree).
Jalankan dari root repo: cd /storage/emulated/0/molina-studio && python3 /jalur/ke/patch_step2b_rootfs_backup.py
Idempotent: aman dijalankan berulang."""
import pathlib, sys

def write_new(path, text, mode=None):
    p = pathlib.Path(path)
    if p.exists() and p.read_text(encoding="utf-8") == text:
        print(f"[skip] {path}: sudah sama"); return
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8")
    if mode: p.chmod(mode)
    print(f"[ok]   {path}")

write_new('app/src/main/java/com/molina/studio/terminal/RootfsBackup.kt', r'''package com.molina.studio.terminal

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * Backup, restore, dan hapus rootfs Debian. Murni JVM (tanpa Android) agar bisa diuji; operasi yang butuh
 * stat/chmod/symlink gaya Unix lewat [RootfsPosix] (di Android: Os.*).
 *
 * Prinsip: symlink TIDAK PERNAH diikuti (symlink guest bisa menunjuk ke storage nyata di host);
 * hasil ditulis ke berkas/folder sementara lalu ditukar atomik; gagal/batal tidak menyentuh rootfs yang ada.
 */
interface RootfsPosix {
    /** Bit izin (0..0xFFF) dari lstat, tanpa mengikuti symlink. */
    fun mode(f: File): Int
    fun chmod(f: File, mode: Int)
    fun symlink(link: File, target: String)
}

class RootfsBackupException(message: String) : IOException(message)
class BackupCancelled : RuntimeException()

class BackupControl {
    @Volatile var cancelled = false
    fun cancel() { cancelled = true }
    internal fun check() { if (cancelled) throw BackupCancelled() }
}

enum class BackupPhase { Scan, Write, Inspect, Extract, Swap }

data class ExportResult(val entries: Int, val bytes: Long, val skipped: Int)
data class ArchiveInfo(val entries: Int, val bytes: Long, val looksLikeRootfs: Boolean)

/** Hapus pohon folder TANPA mengikuti symlink (File.deleteRecursively mengikutinya dan bisa menghapus data di luar). */
object SafeTree {
    /** true bila [root] sudah tidak ada setelahnya. Symlink dihapus sebagai tautan, targetnya tidak disentuh. */
    fun delete(root: File): Boolean {
        val p = root.toPath()
        if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) return true
        try {
            Files.walkFileTree(p, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val d = dir.toFile() // bukan symlink: walkFileTree tidak mengikuti tautan
                    d.setReadable(true); d.setWritable(true); d.setExecutable(true)
                    return FileVisitResult.CONTINUE
                }
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.deleteIfExists(file); return FileVisitResult.CONTINUE
                }
                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE
                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    try { Files.deleteIfExists(dir) } catch (e: IOException) { /* dibiarkan; dicek di akhir */ }
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (e: IOException) { /* dicek di akhir */ }
        return !Files.exists(p, LinkOption.NOFOLLOW_LINKS)
    }

    /** true bila path kanonik (mengikuti symlink) [f] berada di dalam [rootCanon]. */
    fun isInside(f: File, rootCanon: String): Boolean {
        val c = f.canonicalPath
        return c == rootCanon || c.startsWith(rootCanon + File.separator)
    }
}

/** Implementasi JVM biasa (Linux). Android memakai Os.lstat/chmod/symlink di lapisan integrasi. */
object NioPosix : RootfsPosix {
    override fun mode(f: File): Int =
        (Files.getAttribute(f.toPath(), "unix:mode", LinkOption.NOFOLLOW_LINKS) as Int) and 0xFFF
    override fun chmod(f: File, mode: Int) {
        Files.setAttribute(f.toPath(), "unix:mode", mode)
    }
    override fun symlink(link: File, target: String) {
        Files.createSymbolicLink(link.toPath(), java.nio.file.Paths.get(target))
    }
}

private class ByteCounter(input: InputStream) : FilterInputStream(input) {
    @Volatile var count = 0L
    override fun read(): Int { val r = super.read(); if (r >= 0) count++; return r }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val r = super.read(b, off, len); if (r > 0) count += r; return r
    }
}

object RootfsBackup {
    /** Isi folder ini tidak ikut dicadangkan (foldernya tetap dicatat): dibuat ulang/volatil. */
    private val SKIP_CONTENTS = setOf("proc", "sys", "dev", "run", "tmp", "etc/molina/proc")

    private const val SPACE_MARGIN = 64L * 1024 * 1024

    /**
     * Cadangkan [rootfs] ke [dest] (.tar.gz) lewat berkas .part lalu ganti nama. Sesi terminal harus berhenti dulu:
     * berkas yang berubah saat dibaca menggagalkan pencadangan (tidak ada arsip rusak yang tersisa).
     */
    fun export(
        rootfs: File, dest: File, posix: RootfsPosix,
        ctl: BackupControl = BackupControl(),
        onProgress: (BackupPhase, Int) -> Unit = { _, _ -> },
    ): ExportResult {
        if (!rootfs.isDirectory) throw RootfsBackupException("Rootfs tidak ditemukan")
        val root = rootfs.toPath()
        onProgress(BackupPhase.Scan, 0)
        val total = scanBytes(root, ctl).coerceAtLeast(1L)

        val part = File(dest.path + ".part")
        part.parentFile?.mkdirs()
        part.delete()
        var entries = 0
        var skipped = 0
        var done = 0L
        var last = -1
        try {
            TarArchiveOutputStream(GzipCompressorOutputStream(part.outputStream().buffered(256 * 1024))).use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
                tar.setAddPaxHeadersForNonAsciiNames(true)
                val buf = ByteArray(64 * 1024)
                Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        ctl.check()
                        val rel = relName(root, dir)
                        if (rel.isNotEmpty()) {
                            val e = TarArchiveEntry("$rel/")
                            fill(e, dir.toFile(), posix, 0x1ED)
                            tar.putArchiveEntry(e); tar.closeArchiveEntry(); entries++
                        }
                        return if (rel in SKIP_CONTENTS) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        ctl.check()
                        val rel = relName(root, file)
                        val f = file.toFile()
                        when {
                            attrs.isSymbolicLink -> {
                                val e = TarArchiveEntry(rel, TarConstants.LF_SYMLINK)
                                e.linkName = Files.readSymbolicLink(file).toString()
                                fill(e, f, posix, 0x1FF)
                                tar.putArchiveEntry(e); tar.closeArchiveEntry(); entries++
                            }
                            attrs.isRegularFile -> {
                                val size = attrs.size()
                                val e = TarArchiveEntry(rel)
                                e.size = size
                                fill(e, f, posix, 0x1A4)
                                tar.putArchiveEntry(e)
                                f.inputStream().use { input ->
                                    var left = size
                                    while (left > 0) {
                                        ctl.check()
                                        val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                                        if (n < 0) throw RootfsBackupException("Berkas berubah saat dicadangkan: $rel")
                                        tar.write(buf, 0, n)
                                        left -= n
                                        done += n
                                        val p = ((done * 100) / total).toInt().coerceIn(0, 100)
                                        if (p != last) { last = p; onProgress(BackupPhase.Write, p) }
                                    }
                                    if (input.read() >= 0) throw RootfsBackupException("Berkas berubah saat dicadangkan: $rel")
                                }
                                tar.closeArchiveEntry(); entries++
                            }
                            else -> skipped++ // fifo, socket, perangkat
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                        throw RootfsBackupException("Tidak bisa membaca ${relName(root, file)}: ${exc.message}")
                    }
                })
            }
            moveInto(part, dest)
        } catch (t: Throwable) {
            part.delete()
            throw t
        }
        onProgress(BackupPhase.Write, 100)
        return ExportResult(entries, done, skipped)
    }

    /** Baca arsip sampai habis: jumlah entri, total byte terurai, dan apakah tampak seperti rootfs Linux. */
    fun inspect(archive: File, ctl: BackupControl = BackupControl()): ArchiveInfo {
        var entries = 0
        var bytes = 0L
        var osRelease = false
        var etc = false
        var usr = false
        openTar(archive).use { tar ->
            while (true) {
                val e = tar.nextEntry as? TarArchiveEntry ?: break
                ctl.check()
                entries++
                if (e.isFile) bytes += e.size
                when (cleanName(e.name).trimEnd('/')) {
                    "etc/os-release" -> osRelease = true
                    "etc" -> etc = true
                    "usr" -> usr = true
                }
            }
        }
        return ArchiveInfo(entries, bytes, osRelease || (etc && usr))
    }

    /**
     * Pulihkan [archive] menggantikan [rootfs]: periksa arsip dan ruang kosong, ekstrak ke "<nama>.restore",
     * lalu tukar (yang lama jadi "<nama>.old", dihapus setelah berhasil; dikembalikan bila penukaran gagal).
     * Pemanggil wajib menghentikan semua sesi terminal lebih dulu.
     */
    fun restore(
        archive: File, rootfs: File, posix: RootfsPosix,
        ctl: BackupControl = BackupControl(),
        freeSpaceBytes: Long = rootfs.absoluteFile.parentFile?.usableSpace ?: Long.MAX_VALUE,
        onProgress: (BackupPhase, Int) -> Unit = { _, _ -> },
    ) {
        if (!archive.isFile) throw RootfsBackupException("Berkas cadangan tidak ditemukan")
        val base = rootfs.absoluteFile.parentFile ?: throw RootfsBackupException("Lokasi rootfs tidak valid")
        val staging = File(base, rootfs.name + ".restore")
        val old = File(base, rootfs.name + ".old")

        onProgress(BackupPhase.Inspect, 0)
        val info = try { inspect(archive, ctl) } catch (e: BackupCancelled) { throw e }
            catch (e: IOException) { throw RootfsBackupException("Arsip rusak atau bukan tar.gz: ${e.message}") }
        if (!info.looksLikeRootfs) throw RootfsBackupException("Arsip ini tidak tampak seperti rootfs Linux")
        val need = info.bytes + SPACE_MARGIN
        if (freeSpaceBytes < need) {
            throw RootfsBackupException("Ruang kosong kurang: perlu ${need / (1024 * 1024)} MB, tersedia ${freeSpaceBytes / (1024 * 1024)} MB")
        }

        SafeTree.delete(staging)
        staging.mkdirs()
        try {
            extractInto(archive, staging, posix, ctl) { onProgress(BackupPhase.Extract, it) }
            onProgress(BackupPhase.Swap, 0)
            ctl.check()
            if (!SafeTree.delete(old)) throw RootfsBackupException("Tidak bisa membersihkan sisa rootfs lama (.old)")
            val hadOld = rootfs.exists()
            if (hadOld && !rootfs.renameTo(old)) throw RootfsBackupException("Gagal menyisihkan rootfs lama")
            if (!staging.renameTo(rootfs)) {
                if (hadOld) old.renameTo(rootfs) // kembalikan
                throw RootfsBackupException("Gagal memasang rootfs hasil pemulihan; rootfs lama dikembalikan")
            }
        } catch (t: Throwable) {
            SafeTree.delete(staging)
            throw t
        }
        SafeTree.delete(old) // gagal di sini tidak fatal: dibersihkan pada pemulihan berikutnya
        onProgress(BackupPhase.Swap, 100)
    }

    /** Ekstrak ke [root] (folder kosong). Nama tak tepercaya: ".." dan keluar-rootfs lewat symlink dilewati. */
    internal fun extractInto(
        archive: File, root: File, posix: RootfsPosix, ctl: BackupControl, onPercent: (Int) -> Unit,
    ) {
        val rootPath = root.toPath().toAbsolutePath().normalize()
        val rootCanon = root.canonicalPath
        val counter = ByteCounter(archive.inputStream().buffered(256 * 1024))
        val total = archive.length().coerceAtLeast(1L)
        var last = -1
        TarArchiveInputStream(GzipCompressorInputStream(counter)).use { tar ->
            while (true) {
                val e = tar.nextEntry as? TarArchiveEntry ?: break
                ctl.check()
                val name = cleanName(e.name)
                if (name.isEmpty() || name.contains('\u0000')) continue
                val target = rootPath.resolve(name).normalize()
                if (target == rootPath || !target.startsWith(rootPath)) continue
                val f = target.toFile()
                val parent = f.parentFile
                if (parent != null) {
                    parent.mkdirs()
                    if (!SafeTree.isInside(parent, rootCanon)) continue
                }
                val perm = e.mode and 0xFFF
                when {
                    e.isDirectory -> {
                        if (Files.isSymbolicLink(f.toPath())) f.delete()
                        f.mkdirs()
                        posix.chmod(f, perm or 0x1C0) // u+rwx
                    }
                    e.isSymbolicLink -> {
                        SafeTree.delete(f)
                        posix.symlink(f, e.linkName)
                    }
                    e.isLink -> { // hardlink -> salinan (Android melarang hardlink)
                        val src = rootPath.resolve(cleanName(e.linkName)).normalize()
                        val sf = src.toFile()
                        if (src.startsWith(rootPath) && sf.isFile && SafeTree.isInside(sf, rootCanon)) {
                            SafeTree.delete(f)
                            sf.copyTo(f, overwrite = true)
                            posix.chmod(f, perm or 0x180)
                        }
                    }
                    e.isFile -> {
                        if (Files.isSymbolicLink(f.toPath()) || f.isDirectory) SafeTree.delete(f)
                        f.outputStream().use { out -> tar.copyTo(out, 64 * 1024) }
                        posix.chmod(f, perm or 0x180) // u+rw
                        f.setLastModified(e.lastModifiedTime.toMillis())
                    }
                    else -> {} // fifo/perangkat dilewati
                }
                val p = ((counter.count * 100) / total).toInt().coerceIn(0, 100)
                if (p != last) { last = p; onPercent(p) }
            }
        }
    }

    private fun openTar(archive: File) =
        TarArchiveInputStream(GzipCompressorInputStream(archive.inputStream().buffered(256 * 1024)))

    private fun cleanName(raw: String): String = raw.removePrefix("./").trimStart('/')

    private fun relName(root: Path, p: Path): String =
        root.relativize(p).toString().replace(File.separatorChar, '/')

    private fun fill(e: TarArchiveEntry, f: File, posix: RootfsPosix, fallbackMode: Int) {
        e.mode = try { posix.mode(f) } catch (ex: Exception) { fallbackMode }
        e.setUserId(0); e.setGroupId(0); e.userName = ""; e.groupName = ""
        try { e.setModTime(Files.getLastModifiedTime(f.toPath(), LinkOption.NOFOLLOW_LINKS)) } catch (ex: Exception) {}
    }

    private fun scanBytes(root: Path, ctl: BackupControl): Long {
        var sum = 0L
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                ctl.check()
                return if (relName(root, dir) in SKIP_CONTENTS) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (attrs.isRegularFile) sum += attrs.size()
                return FileVisitResult.CONTINUE
            }
            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
        })
        return sum
    }

    private fun moveInto(part: File, dest: File) {
        try {
            Files.move(part.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(part.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
''')

write_new('app/src/main/java/com/molina/studio/terminal/AndroidRootfsPosix.kt', r'''package com.molina.studio.terminal

import android.system.Os
import java.io.File

/** Operasi Unix untuk RootfsBackup di Android (lstat tidak mengikuti symlink). */
internal object AndroidRootfsPosix : RootfsPosix {
    override fun mode(f: File): Int = Os.lstat(f.path).st_mode and 0xFFF
    override fun chmod(f: File, mode: Int) = Os.chmod(f.path, mode)
    override fun symlink(link: File, target: String) = Os.symlink(target, link.path)
}
''')

write_new('scripts/app-tests/RootfsBackupTests.kt', r'''import com.molina.studio.terminal.ArchiveInfo
import com.molina.studio.terminal.BackupCancelled
import com.molina.studio.terminal.BackupControl
import com.molina.studio.terminal.BackupPhase
import com.molina.studio.terminal.NioPosix
import com.molina.studio.terminal.RootfsBackup
import com.molina.studio.terminal.RootfsBackupException
import com.molina.studio.terminal.SafeTree
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlin.system.exitProcess

private var passed = 0
private var failed = 0
private fun check(name: String, ok: Boolean) {
    if (ok) passed++ else { failed++; println("GAGAL: $name") }
}
private inline fun <reified T : Throwable> throwsA(name: String, block: () -> Unit) {
    try { block(); check("$name (tidak melempar)", false) }
    catch (e: Throwable) { check("$name (${e.javaClass.simpleName}: ${e.message})", e is T) }
}
private fun sym(link: File, target: String) = Files.createSymbolicLink(link.toPath(), java.nio.file.Paths.get(target))
private fun isLink(f: File) = Files.isSymbolicLink(f.toPath())
private fun mode(f: File) = NioPosix.mode(f)

/** Rootfs mini: dua folder inti, mode beragam, symlink (relatif, absolut guest, ke luar), nama unik, jalur panjang. */
private fun makeRootfs(root: File, outside: File) {
    File(root, "etc").mkdirs(); File(root, "usr/bin").mkdirs(); File(root, "root").mkdirs()
    File(root, "etc/os-release").writeText("ID=debian\n")
    File(root, "usr/bin/tool").apply { writeText("#!/bin/sh\necho hi\n"); NioPosix.chmod(this, 0b111_101_101) }
    File(root, "root/rahasia").apply { writeText("x"); NioPosix.chmod(this, 0b110_000_000) }
    File(root, "root/sayang😍.txt").writeText("unicode")
    File(root, "root/kosong").mkdirs()
    val deep = File(root, "root/" + "dir_panjang_".repeat(12) + "/" + "sub_".repeat(15)).apply { mkdirs() }
    File(deep, "berkas_dengan_nama_yang_cukup_panjang_untuk_pax.txt").writeText("panjang")
    sym(File(root, "bin"), "usr/bin")                 // relatif
    sym(File(root, "etc/alt"), "/usr/bin/tool")       // absolut (guest)
    sym(File(root, "root/ws"), outside.absolutePath)  // menunjuk ke direktori NYATA di luar rootfs
    // isi volatil yang tidak boleh ikut
    File(root, "proc").mkdirs(); File(root, "proc/x").writeText("v")
    File(root, "tmp").mkdirs(); File(root, "tmp/sampah").writeText("v")
    File(root, "etc/molina/proc").mkdirs(); File(root, "etc/molina/proc/stat").writeText("v")
}

private fun writeTar(dest: File, block: (TarArchiveOutputStream) -> Unit) {
    TarArchiveOutputStream(GzipCompressorOutputStream(dest.outputStream())).use { t ->
        t.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX); block(t)
    }
}
private fun TarArchiveOutputStream.file(name: String, text: String, mode: Int = 0b110_100_100) {
    val e = TarArchiveEntry(name); e.size = text.length.toLong(); e.mode = mode
    putArchiveEntry(e); write(text.toByteArray()); closeArchiveEntry()
}
private fun TarArchiveOutputStream.dir(name: String) {
    val e = TarArchiveEntry("$name/"); e.mode = 0b111_101_101; putArchiveEntry(e); closeArchiveEntry()
}
private fun TarArchiveOutputStream.link(name: String, target: String) {
    val e = TarArchiveEntry(name, TarConstants.LF_SYMLINK); e.linkName = target; putArchiveEntry(e); closeArchiveEntry()
}

fun main() {
    val tmp = Files.createTempDirectory("rootfsbk").toFile()
    try {
        val outside = File(tmp, "storage").apply { mkdirs() }
        val keep = File(outside, "penting.txt").apply { writeText("data pengguna") }
        val src = File(tmp, "src/debian")
        makeRootfs(src, outside)

        // ---- SafeTree ----
        val dcopy = File(tmp, "dcopy"); File(dcopy, "a").mkdirs(); File(dcopy, "a/f").writeText("1")
        sym(File(dcopy, "a/ws"), outside.absolutePath); sym(File(dcopy, "mati"), "/tidak/ada")
        check("SafeTree.delete true", SafeTree.delete(dcopy))
        check("SafeTree: folder hilang", !dcopy.exists())
        check("SafeTree: target symlink TIDAK terhapus", keep.isFile && keep.readText() == "data pengguna")
        check("SafeTree: path tak ada = true", SafeTree.delete(File(tmp, "tidakada")))
        val ro = File(tmp, "ro/sub").apply { mkdirs() }; File(ro, "f").writeText("x"); ro.setWritable(false); ro.parentFile.setWritable(false)
        check("SafeTree: folder tanpa izin tulis", SafeTree.delete(File(tmp, "ro")))

        // ---- export ----
        val arc = File(tmp, "bk/debian.tar.gz")
        val phases = mutableSetOf<BackupPhase>()
        val r = RootfsBackup.export(src, arc, NioPosix) { ph, _ -> phases += ph }
        check("export: berkas ada, .part hilang", arc.isFile && !File(arc.path + ".part").exists())
        check("export: fase Scan+Write", BackupPhase.Scan in phases && BackupPhase.Write in phases)
        check("export: entri > 0, tanpa lewati", r.entries > 10 && r.skipped == 0)
        val info: ArchiveInfo = RootfsBackup.inspect(arc)
        check("inspect: tampak rootfs", info.looksLikeRootfs && info.entries == r.entries)
        check("inspect: byte terurai = isi berkas", info.bytes == r.bytes)

        // ---- restore ke lokasi baru ----
        val dst = File(tmp, "dst/debian").apply { parentFile.mkdirs() }
        val pr = mutableListOf<BackupPhase>()
        RootfsBackup.restore(arc, dst, NioPosix) { ph, _ -> if (ph !in pr) pr += ph }
        check("restore: urutan fase", pr == listOf(BackupPhase.Inspect, BackupPhase.Extract, BackupPhase.Swap))
        check("restore: isi berkas", File(dst, "etc/os-release").readText() == "ID=debian\n")
        check("restore: mode 755", mode(File(dst, "usr/bin/tool")) == 0b111_101_101)
        check("restore: mode 600", mode(File(dst, "root/rahasia")) == 0b110_000_000)
        check("restore: unicode", File(dst, "root/sayang😍.txt").readText() == "unicode")
        check("restore: folder kosong", File(dst, "root/kosong").isDirectory)
        check("restore: jalur panjang", File(dst, "root/" + "dir_panjang_".repeat(12) + "/" + "sub_".repeat(15) + "/berkas_dengan_nama_yang_cukup_panjang_untuk_pax.txt").readText() == "panjang")
        check("restore: symlink relatif", isLink(File(dst, "bin")) && Files.readSymbolicLink(File(dst, "bin").toPath()).toString() == "usr/bin")
        check("restore: symlink absolut guest", Files.readSymbolicLink(File(dst, "etc/alt").toPath()).toString() == "/usr/bin/tool")
        check("restore: symlink ke luar tetap tautan", isLink(File(dst, "root/ws")) && keep.isFile)
        check("restore: isi volatil tidak ikut", !File(dst, "proc/x").exists() && !File(dst, "tmp/sampah").exists() && !File(dst, "etc/molina/proc/stat").exists())
        check("restore: folder volatil tetap ada", File(dst, "proc").isDirectory && File(dst, "tmp").isDirectory)
        check("restore: tidak ada sisa .restore/.old", !File(dst.parentFile, "debian.restore").exists() && !File(dst.parentFile, "debian.old").exists())

        // ---- restore menggantikan rootfs yang ada (berisi symlink ke luar) ----
        File(dst, "etc/hanya-di-lama").writeText("lama")
        RootfsBackup.restore(arc, dst, NioPosix)
        check("ganti: berkas lama hilang", !File(dst, "etc/hanya-di-lama").exists())
        check("ganti: isi baru ada", File(dst, "etc/os-release").isFile)
        check("ganti: data di luar rootfs selamat", keep.isFile && keep.readText() == "data pengguna")
        check("ganti: tanpa .old", !File(dst.parentFile, "debian.old").exists())

        // ---- gagal: arsip rusak (terpotong) -> rootfs lama utuh ----
        val broken = File(tmp, "broken.tar.gz"); broken.writeBytes(arc.readBytes().copyOf((arc.length() / 2).toInt()))
        File(dst, "etc/penanda").writeText("masih lama")
        throwsA<RootfsBackupException>("arsip rusak ditolak") { RootfsBackup.restore(broken, dst, NioPosix) }
        check("rusak: rootfs lama utuh", File(dst, "etc/penanda").isFile)
        check("rusak: tanpa .restore", !File(dst.parentFile, "debian.restore").exists())

        // ---- gagal: bukan rootfs ----
        val notRoot = File(tmp, "bukan.tar.gz"); writeTar(notRoot) { it.file("foto.jpg", "abc"); it.file("catatan.txt", "x") }
        throwsA<RootfsBackupException>("bukan rootfs ditolak") { RootfsBackup.restore(notRoot, dst, NioPosix) }
        check("bukan rootfs: rootfs utuh", File(dst, "etc/penanda").isFile)

        // ---- gagal: ruang kosong kurang ----
        throwsA<RootfsBackupException>("ruang kurang ditolak") { RootfsBackup.restore(arc, dst, NioPosix, freeSpaceBytes = 1024) }
        check("ruang kurang: rootfs utuh, tanpa .restore", File(dst, "etc/penanda").isFile && !File(dst.parentFile, "debian.restore").exists())

        // ---- batal di tengah ekstrak ----
        val ctl = BackupControl()
        throwsA<BackupCancelled>("batal saat ekstrak") {
            RootfsBackup.restore(arc, dst, NioPosix, ctl) { ph, _ -> if (ph == BackupPhase.Extract) ctl.cancel() }
        }
        check("batal: rootfs utuh, tanpa .restore", File(dst, "etc/penanda").isFile && !File(dst.parentFile, "debian.restore").exists())
        val ctl2 = BackupControl()
        throwsA<BackupCancelled>("batal saat export") {
            RootfsBackup.export(src, File(tmp, "bk/batal.tar.gz"), NioPosix, ctl2) { _, _ -> ctl2.cancel() }
        }
        check("batal export: tanpa berkas/.part", !File(tmp, "bk/batal.tar.gz").exists() && !File(tmp, "bk/batal.tar.gz.part").exists())

        // ---- serangan: path traversal dan tulis lewat symlink ----
        val evil = File(tmp, "evil.tar.gz")
        val victim = File(tmp, "victim").apply { mkdirs() }
        writeTar(evil) {
            it.dir("etc"); it.file("etc/os-release", "ID=x\n"); it.dir("usr")
            it.file("../pwn1.txt", "jahat"); it.file("a/../../pwn2.txt", "jahat"); it.file("/abs/aman.txt", "ok")
            it.link("etc/tautan", victim.absolutePath)           // tautan ke luar
            it.file("etc/tautan/pwn3.txt", "jahat")              // tulis lewat tautan
            it.link("usr/lib", "../../..")
            it.file("usr/lib/pwn4.txt", "jahat")
        }
        val dst2 = File(tmp, "dst2/debian").apply { parentFile.mkdirs() }
        RootfsBackup.restore(evil, dst2, NioPosix)
        check("serangan: '../' tidak keluar", !File(tmp, "pwn1.txt").exists() && !File(tmp, "dst2/pwn1.txt").exists() && !File(tmp, "pwn2.txt").exists() && !File(tmp, "dst2/pwn2.txt").exists())
        check("serangan: tidak menulis lewat tautan", victim.list()!!.isEmpty() && !File(tmp, "pwn4.txt").exists())
        check("serangan: nama absolut dijinakkan", File(dst2, "abs/aman.txt").readText() == "ok")

        // ---- export menimpa dest yang sudah ada ----
        RootfsBackup.export(src, arc, NioPosix)
        check("export ulang: tetap valid", RootfsBackup.inspect(arc).looksLikeRootfs)
    } finally {
        // rapikan, tanpa mengikuti symlink
        File(tmp, "ro").let { it.setWritable(true) }
        SafeTree.delete(tmp)
    }
    println("RootfsBackupTests: $passed lulus, $failed gagal")
    if (failed > 0) exitProcess(1)
}
''')

write_new('scripts/app-tests/run_rootfs_backup.sh', r'''#!/usr/bin/env bash
# Uji JVM RootfsBackup (export/inspect/restore, SafeTree, penolakan arsip rusak/bukan rootfs/ruang kurang, batal, traversal).
# Butuh: kotlinc (>= 2.1) dan JAR di ARCHIVE_DEPS (dipisah ':'): commons-compress 1.27.1, commons-io, commons-lang3, commons-codec.
# Contoh: ARCHIVE_DEPS=$(ls ~/jars/*.jar | tr '\n' ':') KOTLINC=/opt/kotlinc/bin/kotlinc bash scripts/app-tests/run_rootfs_backup.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
KC="${KOTLINC:-kotlinc}"
: "${ARCHIVE_DEPS:?isi ARCHIVE_DEPS dengan daftar JAR dependensi (dipisah titik dua)}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$KC" -nowarn -cp "$ARCHIVE_DEPS" -d "$OUT" app/src/main/java/com/molina/studio/terminal/RootfsBackup.kt scripts/app-tests/RootfsBackupTests.kt
java -cp "$OUT:$ARCHIVE_DEPS:$(dirname "$(command -v "$KC")")/../lib/kotlin-stdlib.jar" RootfsBackupTestsKt
''', 0o755)

p = pathlib.Path("app/src/main/java/com/molina/studio/terminal/Rootfs.kt")
s = p.read_text(encoding="utf-8")
edits = [
    ('File(app.filesDir, "rootfs/debian.tmp").deleteRecursively()',
     'SafeTree.delete(File(app.filesDir, "rootfs/debian.tmp"))'),
    ('        tmp.deleteRecursively()\n', '        SafeTree.delete(tmp)\n'),
    ('        dst.deleteRecursively()\n', '        SafeTree.delete(dst)\n'),
]
changed = False
for old, new in edits:
    if old in s:
        if s.count(old) != 1:
            sys.exit(f"[GAGAL] Rootfs.kt: pola tidak unik: {old!r}")
        s = s.replace(old, new); changed = True; print(f"[ok]   Rootfs.kt: {new.strip()}")
    elif new in s:
        print(f"[skip] Rootfs.kt: {new.strip()} sudah ada")
    else:
        sys.exit(f"[GAGAL] Rootfs.kt: pola tidak ditemukan: {old!r}")
if changed:
    p.write_text(s, encoding="utf-8")
