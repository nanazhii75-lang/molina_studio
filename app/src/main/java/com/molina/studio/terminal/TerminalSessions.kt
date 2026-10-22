package com.molina.studio.terminal

import android.content.Context
import com.molina.studio.AppExit
import android.os.Environment
import android.system.Os
import android.system.OsConstants
import com.molina.studio.log.LogBus
import com.molina.studio.log.LogLevel
import com.termux.terminal.TerminalSession
import java.io.File

/** Satu sesi Debian (PRoot); tetap hidup walau tab berpindah (dijaga TerminalService). */
object TerminalSessions {
    /** true: perintah exit di shell mematikan semua proses lalu menutup aplikasi. */
    private const val EXIT_APP_ON_SESSION_END = true

    val client = MolinaSessionClient()
    private var session: TerminalSession? = null

    @Volatile var currentSession: TerminalSession? = null
        private set

    @Volatile private var appContext: Context? = null

    @Synchronized
    fun session(context: Context): TerminalSession {
        val app = context.applicationContext
        appContext = app
        val s = session?.takeIf { it.isRunning }
            ?: debianShell(app).also {
                session = it
                LogBus.log("terminal", LogLevel.Info, "session", "Sesi Debian dimulai")
            }
        currentSession = s
        TerminalService.ensureRunning(app)
        return s
    }

    /** Dipanggil saat sesi berakhir: keluar dari aplikasi, atau hentikan service. */
    fun onSessionFinished() {
        LogBus.log("terminal", LogLevel.Info, "session", "Sesi terminal berakhir")
        if (EXIT_APP_ON_SESSION_END) {
            AppExit.exit(delayMs = 800)
            return
        }
        val ctx = appContext ?: return
        val shouldStop = synchronized(this) { session?.isRunning != true }
        if (shouldStop) TerminalService.requestStop(ctx)
    }

    /** Aksi "Keluar": matikan sesi (grup proses + proses utama) dan kosongkan state. */
    @Synchronized
    fun killAll() {
        session?.let { s ->
            if (s.isRunning) {
                val pid = s.pid
                if (pid > 1) {
                    try { Os.kill(-pid, OsConstants.SIGKILL) } catch (e: Exception) {}
                }
                s.finishIfRunning()
            }
        }
        session = null
        currentSession = null
    }

    private fun workDir(): File = File(Environment.getExternalStorageDirectory(), "molina-studio")

    private fun debianShell(ctx: Context): TerminalSession {
        TerminalPrefs.init(ctx)
        val nativeDir = ctx.applicationInfo.nativeLibraryDir
        val rootfsDir = RootfsManager.rootfsDir(ctx)
        val rootfs = rootfsDir.absolutePath
        val tmp = File(ctx.cacheDir, "proot-tmp").apply { mkdirs() }
        val guestCwd = "/root" // home default: prompt tampil sebagai user@localhost:~
        val storage = Environment.getExternalStorageDirectory().absolutePath

        // bashrc Molina (prompt berwarna + banner) dan titik mount /storage/emulated/0 di guest.
        MolinaShell.installRc(ctx, File(rootfsDir, "etc/molina/bashrc"))
        MolinaShell.writePromptEnv(rootfsDir, TerminalPrefs.promptTheme)
        File(rootfsDir, "storage/emulated/0").mkdirs()

        val args = arrayOf(
            "proot", "--link2symlink", "--kill-on-exit", "-0",
            "--rootfs=$rootfs", "--cwd=$guestCwd",
            "--bind=/dev", "--bind=/proc", "--bind=/sys",
            "--bind=$storage:/sdcard",
            "--bind=$storage:/storage/emulated/0",
            "/usr/bin/env", "-i",
            "HOME=/root", "TERM=xterm-256color", "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "MOLINA_HOME=${MolinaShell.HOME_DIR}",
            "MOLINA_BRAND=${MolinaShell.BRAND}",
            "MOLINA_BRAND_SGR=${MolinaShell.BRAND_SGR}",
            "MOLINA_PS_HOME=${TerminalPrefs.promptTheme.homeSgr}",
            "MOLINA_PS_OTHER=${TerminalPrefs.promptTheme.otherSgr}",
            "/bin/bash", "--rcfile", "/etc/molina/bashrc", "-i"
        )
        val env = buildList {
            add("PROOT_LOADER=$nativeDir/libproot_loader.so")
            add("PROOT_TMP_DIR=${tmp.absolutePath}")
            add("TMPDIR=${tmp.absolutePath}")
            add("PATH=/system/bin")
            add("HOME=${ctx.filesDir.absolutePath}")
            System.getenv("ANDROID_ROOT")?.let { add("ANDROID_ROOT=$it") }
            System.getenv("ANDROID_DATA")?.let { add("ANDROID_DATA=$it") }
        }.toTypedArray()
        return TerminalSession(
            "$nativeDir/libproot.so", ctx.filesDir.absolutePath, args, env, 2000, client
        )
    }
}
