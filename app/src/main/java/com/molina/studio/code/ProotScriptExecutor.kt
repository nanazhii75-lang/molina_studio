package com.molina.studio.code

import android.content.Context
import android.os.Environment
import com.molina.code.preview.GuestPaths
import com.molina.code.run.RunHandle
import com.molina.code.run.RunListener
import com.molina.code.run.RunRequest
import com.molina.code.run.RunResult
import com.molina.code.run.ScriptExecutor
import com.molina.core.common.AppLog
import com.molina.core.common.LogLevel
import com.molina.core.common.YtdlpCommand
import com.molina.studio.system.AndroidProcessOps
import com.molina.studio.terminal.MolinaDns
import com.molina.studio.terminal.RootfsManager
import com.molina.studio.terminal.StorageBinds
import com.molina.studio.terminal.TerminalPrefs
import com.molina.studio.terminal.TerminalSessions
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Argumen proot untuk satu skrip. Murni (tanpa Android) supaya mudah diuji. */
internal object ScriptCommand {
    const val GUEST_TMP_DIR = "/tmp/molina-run"

    /**
     * Argv libproot.so (tanpa argv[0]). [storageBinds] sudah berbentuk `--bind=host:guest`.
     * `env -i` menjaga lingkungan guest bersih; PYTHONUNBUFFERED supaya keluaran Python mengalir langsung.
     */
    fun prootArgs(
        rootfs: String,
        cwd: String,
        storageBinds: List<String>,
        interpreter: String,
        guestScript: String,
    ): List<String> = buildList {
        add("--link2symlink"); add("--kill-on-exit"); add("-0")
        add("--rootfs=$rootfs"); add("--cwd=$cwd")
        add("--bind=/dev"); add("--bind=/proc"); add("--bind=/sys")
        addAll(storageBinds)
        add("/usr/bin/env"); add("-i")
        add("HOME=/root"); add("LANG=C.UTF-8"); add("TERM=dumb")
        add("MALLOC_ARENA_MAX=2"); add("DEBIAN_FRONTEND=noninteractive")
        add("PYTHONDONTWRITEBYTECODE=1"); add("PYTHONIOENCODING=utf-8"); add("PYTHONUNBUFFERED=1")
        add("PATH=${YtdlpCommand.GUEST_PATH}")
        add(interpreter); add(guestScript)
    }
}

/**
 * Menjalankan skrip dari editor Code di Debian PRoot milik Molina Terminal. Aturan:
 * - satu skrip pada satu waktu,
 * - isi editor ditulis ke berkas sementara di rootfs (/tmp/molina-run) dan dihapus sesudahnya,
 * - batas waktu dan batas keluaran; seluruh pohon proses dimatikan bila dibatalkan atau lewat waktu,
 * - isi skrip tidak pernah masuk log (hanya kode keluar dan lamanya).
 */
internal class ProotScriptExecutor(context: Context) : ScriptExecutor {
    private val app = context.applicationContext

    override fun start(request: RunRequest, listener: RunListener): RunHandle {
        val handle = Handle()
        if (!busy.compareAndSet(false, true)) {
            listener.onFinished(failed("Masih ada skrip yang berjalan."))
            return handle
        }
        val worker = Thread({
            try {
                listener.onFinished(runSync(request, listener, handle))
            } catch (e: Exception) {
                listener.onFinished(failed("Gagal menjalankan: ${e.javaClass.simpleName}"))
            } finally {
                activeHandle = null
                busy.set(false)
            }
        }, "molina-script")
        worker.isDaemon = true
        handle.thread = worker
        activeHandle = handle
        worker.start()
        return handle
    }

    private class Handle : RunHandle {
        @Volatile var thread: Thread? = null
        val cancelled = AtomicBoolean(false)
        override fun cancel() {
            if (cancelled.compareAndSet(false, true)) thread?.interrupt()
        }
    }

    private fun failed(msg: String) = RunResult(-1, false, false, false, 0L, msg)

    private fun runSync(req: RunRequest, out: RunListener, handle: Handle): RunResult {
        val rootfsDir = RootfsManager.rootfsDir(app)
        if (!File(rootfsDir, "usr/bin/env").exists()) {
            return failed("Debian belum terpasang. Buka tab Terminal untuk menyiapkannya.")
        }
        if (req.plan.needsPython && !File(rootfsDir, "usr/bin/python3").exists()) {
            return failed("python3 belum ada di Debian. ${YtdlpCommand.INSTALL_PYTHON_HINT}")
        }

        // Berkas sementara di dalam rootfs, jadi terlihat di guest tanpa bind tambahan.
        val stageDir = File(rootfsDir, "tmp/molina-run").apply { mkdirs() }
        val staged = File(stageDir, "run-${System.nanoTime()}.${req.plan.extension}")
        try {
            staged.writeText(req.scriptText.replace("\r\n", "\n"), Charsets.UTF_8)
        } catch (e: Exception) {
            return failed("Gagal menulis berkas sementara: ${e.javaClass.simpleName}")
        }
        try {
            return exec(req, out, handle, rootfsDir, "${ScriptCommand.GUEST_TMP_DIR}/${staged.name}")
        } finally {
            staged.delete()
        }
    }

    private fun exec(
        req: RunRequest,
        out: RunListener,
        handle: Handle,
        rootfsDir: File,
        guestScript: String,
    ): RunResult {
        val nativeDir = app.applicationInfo.nativeLibraryDir
        val tmp = File(app.cacheDir, "proot-tmp").apply { mkdirs() }
        try { MolinaDns.writeResolvConf(app, rootfsDir) } catch (e: Exception) { }

        // Lingkup storage sama dengan Terminal: default hanya folder kerja Molina yang di-bind.
        val storage = Environment.getExternalStorageDirectory().absolutePath
        val workspace = TerminalSessions.WORKSPACE_DIR
        val full = TerminalPrefs.fullStorageAccess
        val binds: List<String> = if (full) {
            File(rootfsDir, "storage/emulated/0").mkdirs()
            listOf("--bind=$storage:/sdcard", "--bind=$storage:/storage/emulated/0")
        } else {
            val ws = File(storage, workspace).apply { mkdirs() }
            File(rootfsDir, "sdcard/$workspace").mkdirs()
            File(rootfsDir, "storage/emulated/0/$workspace").mkdirs()
            listOf(
                "--bind=${ws.absolutePath}:/sdcard/$workspace",
                "--bind=${ws.absolutePath}:/storage/emulated/0/$workspace",
            ) + StorageBinds.app(app, rootfsDir)
        }
        val cwd = req.hostDir?.let { GuestPaths.of(it, storage, workspace, full, listOf(com.molina.core.common.MolinaPaths.APP_FOLDER)) } ?: "/root"

        val cmd = YtdlpCommand.SH_WRAPPER + "$nativeDir/libproot.so" +
            ScriptCommand.prootArgs(rootfsDir.absolutePath, cwd, binds, req.plan.interpreter, guestScript)
        val pb = ProcessBuilder(cmd).directory(app.filesDir)
        pb.environment().apply {
            clear()
            put("PROOT_LOADER", "$nativeDir/libproot_loader.so")
            put("PROOT_TMP_DIR", tmp.absolutePath)
            put("TMPDIR", tmp.absolutePath)
            put("PATH", "/system/bin")
            put("HOME", app.filesDir.absolutePath)
            System.getenv("ANDROID_ROOT")?.let { put("ANDROID_ROOT", it) }
            System.getenv("ANDROID_DATA")?.let { put("ANDROID_DATA", it) }
        }

        val started = System.nanoTime()
        val proc = try {
            pb.start()
        } catch (e: Exception) {
            return failed("Gagal memulai proot: ${e.javaClass.simpleName}")
        }
        try { proc.outputStream.close() } catch (e: Exception) { }

        val pid = AtomicInteger(-1)
        val sent = AtomicLong(0L)
        val truncated = AtomicBoolean(false)
        val outThread = pump(proc.inputStream, false, true, out, pid, sent, truncated)
        val errThread = pump(proc.errorStream, true, false, out, pid, sent, truncated)

        var finished = false
        var timedOut = false
        // cancel() yang datang sebelum thread ini sempat menunggu tidak bisa menginterupsi: cek dulu.
        var interrupted = handle.cancelled.get()
        try {
            if (!interrupted) {
                finished = proc.waitFor(req.timeoutMs, TimeUnit.MILLISECONDS)
                timedOut = !finished
            }
        } catch (e: InterruptedException) {
            interrupted = true
        } finally {
            if (!finished) {
                // Dibatalkan atau lewat waktu: matikan seluruh pohon (proot + interpreter + anak-anaknya).
                val root = pid.get()
                if (root > 1) {
                    try { AndroidProcessOps.reaper().reapTree(root) } catch (e: Exception) { }
                }
                proc.destroyForcibly()
            }
        }
        // Setelah proses mati, aliran ditutup dan pembaca selesai sendiri; batasi menunggu seperlunya.
        try {
            outThread.join(1500)
            errThread.join(1500)
        } catch (e: InterruptedException) {
            // cancel() terlambat datang; abaikan, proses sudah selesai
        }
        Thread.interrupted() // bersihkan flag interupsi yang mungkin tersisa

        val ms = (System.nanoTime() - started) / 1_000_000
        val code = when {
            !finished -> -1
            else -> try { proc.exitValue() } catch (e: IllegalThreadStateException) { -1 }
        }
        AppLog.log("code", LogLevel.Info, "script", "selesai ${ms}ms exit=$code" +
            (if (timedOut) " (lewat waktu)" else "") + (if (handle.cancelled.get()) " (dibatalkan)" else ""))
        return RunResult(code, timedOut, handle.cancelled.get() || interrupted, truncated.get(), ms)
    }

    /**
     * Baca [input] sebagai UTF-8 dan teruskan ke [out]. Pada stdout, baris pertama adalah PID dari
     * pembungkus sh (lihat YtdlpCommand.SH_WRAPPER): dicatat, tidak ditampilkan. Kelebihan keluaran
     * (> MAX_OUTPUT_CHARS) dibuang tetapi aliran tetap dikuras agar proses tidak macet.
     */
    private fun pump(
        input: InputStream,
        isError: Boolean,
        readPid: Boolean,
        out: RunListener,
        pid: AtomicInteger,
        sent: AtomicLong,
        truncated: AtomicBoolean,
    ): Thread {
        val t = Thread({
            try {
                val reader = InputStreamReader(input, Charsets.UTF_8)
                val buf = CharArray(4096)
                val head = StringBuilder()
                var pidDone = !readPid
                while (true) {
                    val n = reader.read(buf)
                    if (n < 0) break
                    var text = String(buf, 0, n)
                    if (!pidDone) {
                        head.append(text)
                        val nl = head.indexOf("\n")
                        if (nl < 0) continue
                        pidDone = true
                        val first = head.substring(0, nl).trim()
                        val rest = head.substring(nl + 1)
                        val p = first.toIntOrNull()
                        if (p != null) {
                            pid.set(p)
                            text = rest
                        } else {
                            text = head.toString() // bukan PID: tampilkan apa adanya
                        }
                        head.setLength(0)
                        if (text.isEmpty()) continue
                    }
                    val room = MAX_OUTPUT_CHARS - sent.get()
                    if (room <= 0) {
                        truncated.set(true)
                        continue
                    }
                    if (text.length > room) {
                        text = text.substring(0, room.toInt())
                        truncated.set(true)
                    }
                    sent.addAndGet(text.length.toLong())
                    out.onOutput(text, isError)
                }
            } catch (e: Exception) {
                // aliran ditutup saat proses dimatikan
            }
        }, if (isError) "molina-script-err" else "molina-script-out")
        t.isDaemon = true
        t.start()
        return t
    }

    companion object {
        private const val MAX_OUTPUT_CHARS = 1_000_000L
        private val busy = AtomicBoolean(false)
        @Volatile private var activeHandle: Handle? = null

        /** True bila ada skrip Code yang sedang berjalan (untuk Penyegaran sistem). */
        fun isRunning(): Boolean = busy.get()

        /** Batalkan skrip yang berjalan; mengembalikan 1 bila ada yang dibatalkan. */
        fun cancelActive(): Int {
            val h = activeHandle ?: return 0
            h.cancel()
            return 1
        }
    }
}
