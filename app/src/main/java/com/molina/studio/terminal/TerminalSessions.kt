package com.molina.studio.terminal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.molina.studio.AppExit
import android.os.Environment
import android.system.Os
import android.system.OsConstants
import com.molina.studio.log.LogBus
import com.molina.core.common.LogLevel
import com.molina.terminal.TerminalSession
import java.io.File
import kotlin.math.min

/**
 * Daftar sesi Debian (PRoot), maksimal [MAX_SESSIONS]. Sesi tetap hidup walau tab aplikasi
 * berpindah (dijaga TerminalService). Seluruh mutasi daftar terjadi di main thread,
 * kecuali killAll() saat aplikasi keluar.
 */
object TerminalSessions {
    const val MAX_SESSIONS = 5

    /** Nama folder kerja Molina di storage bersama (satu-satunya yang di-bind secara default). */
    internal const val WORKSPACE_DIR = "molina-studio"

    /** true: bila sesi terakhir berakhir, matikan semua proses lalu tutup aplikasi. */
    private const val EXIT_APP_ON_SESSION_END = true

    val client = MolinaSessionClient()

    /** Daftar sesi (observable oleh Compose). */
    val sessions = mutableStateListOf<TerminalSession>()

    var currentIndex by mutableIntStateOf(0); private set

    /** Naik tiap judul sesi berubah, agar daftar di drawer dihitung ulang. */
    var titleTick by mutableIntStateOf(0); private set

    /** Sesi aktif sebagai state (dipakai TerminalScreen untuk berpindah tab). */
    val active: TerminalSession? get() = sessions.getOrNull(currentIndex)

    @Volatile var currentSession: TerminalSession? = null
        private set

    @Volatile private var appContext: Context? = null

    /** Lingkup storage saat sesi dibuat: bind PRoot tetap selama sesi hidup, walau setelan berubah. */
    private val fullAccessBySession = java.util.Collections.synchronizedMap(java.util.WeakHashMap<TerminalSession, Boolean>())

    /** true/false = sesi dibuat dengan/tanpa Akses penyimpanan penuh; null = tidak diketahui. */
    fun fullAccessOf(s: TerminalSession?): Boolean? = if (s == null) null else fullAccessBySession[s]

    fun canAdd(): Boolean = sessions.size < MAX_SESSIONS

    /** Pastikan minimal ada satu sesi; kembalikan sesi aktif. */
    @Synchronized
    fun ensureSession(context: Context): TerminalSession {
        val app = context.applicationContext
        appContext = app
        sessions.removeAll { !it.isRunning }
        if (sessions.isEmpty()) {
            sessions.add(debianShell(app))
            currentIndex = 0
            LogBus.log("terminal", LogLevel.Info, "session", "Sesi Debian [1] dimulai")
        }
        currentIndex = currentIndex.coerceIn(0, sessions.lastIndex)
        val s = sessions[currentIndex]
        currentSession = s
        TerminalService.ensureRunning(app)
        return s
    }

    /** Sesi baru (NEW SESSION). null bila sudah mencapai batas [MAX_SESSIONS]. */
    @Synchronized
    fun newSession(context: Context): TerminalSession? {
        if (!canAdd()) return null
        val app = context.applicationContext
        appContext = app
        val s = debianShell(app)
        sessions.add(s)
        select(sessions.lastIndex)
        LogBus.log("terminal", LogLevel.Info, "session", "Sesi Debian [${sessions.size}] dimulai")
        TerminalService.ensureRunning(app)
        return s
    }

    fun select(index: Int) {
        if (index !in sessions.indices) return
        currentIndex = index
        currentSession = sessions[index]
    }

    fun titleChanged() { titleTick++ }

    /** Tombol ✕ di drawer: bunuh proses sesi (grup proses) lalu tutup tabnya. */
    fun closeSession(index: Int) {
        val s = sessions.getOrNull(index) ?: return
        LogBus.log("terminal", LogLevel.Info, "session", "Sesi [${index + 1}] ditutup pengguna")
        kill(s)
        remove(s, exitDelayMs = 0L)
    }

    /** Dipanggil client saat sebuah sesi berakhir (mis. perintah exit). */
    fun onSessionFinished(finished: TerminalSession) {
        LogBus.log("terminal", LogLevel.Info, "session", "Sesi terminal berakhir")
        remove(finished, exitDelayMs = 800L)
    }

    private fun remove(s: TerminalSession, exitDelayMs: Long) {
        val idx = sessions.indexOf(s)
        if (idx < 0) return // sudah dihapus (mis. lewat tombol ✕)
        sessions.removeAt(idx)
        if (sessions.isEmpty()) {
            currentSession = null
            currentIndex = 0
            onAllSessionsEnded(exitDelayMs)
            return
        }
        currentIndex = when {
            idx < currentIndex -> currentIndex - 1
            idx == currentIndex -> min(idx, sessions.lastIndex)
            else -> currentIndex
        }
        currentSession = sessions[currentIndex]
    }

    private fun onAllSessionsEnded(exitDelayMs: Long) {
        if (EXIT_APP_ON_SESSION_END) {
            AppExit.exit(delayMs = exitDelayMs)
            return
        }
        appContext?.let { TerminalService.requestStop(it) }
    }

    private fun kill(s: TerminalSession) {
        if (!s.isRunning) return
        val pid = s.pid
        if (pid > 1) {
            try { Os.kill(-pid, OsConstants.SIGKILL) } catch (e: Exception) {}
        }
        s.finishIfRunning()
    }

    /** Aksi "Keluar": matikan semua sesi (grup proses + proses utama) dan kosongkan state. */
    @Synchronized
    fun killAll() {
        sessions.toList().forEach { kill(it) }
        sessions.clear()
        currentSession = null
        currentIndex = 0
    }

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
        MolinaShell.installAptConf(rootfsDir)
        MolinaShell.writePromptEnv(rootfsDir, TerminalPrefs.promptTheme)
        MolinaDns.writeResolvConf(ctx, rootfsDir)

        // Lingkup storage: default hanya folder kerja Molina; "Akses penuh" membuka seluruh storage.
        val fullAccess = TerminalPrefs.fullStorageAccess
        var appVisible = fullAccess // /sdcard/Molina terlihat di guest (akses penuh, atau bind appRoot terpasang)
        val storageBinds: Array<String> = if (fullAccess) {
            File(rootfsDir, "storage/emulated/0").mkdirs()
            arrayOf("--bind=$storage:/sdcard", "--bind=$storage:/storage/emulated/0")
        } else {
            val ws = File(storage, WORKSPACE_DIR).apply { mkdirs() }
            File(rootfsDir, "sdcard/$WORKSPACE_DIR").mkdirs()
            File(rootfsDir, "storage/emulated/0/$WORKSPACE_DIR").mkdirs()
            val appBinds = StorageBinds.app(ctx, rootfsDir)
            appVisible = appBinds.isNotEmpty()
            arrayOf(
                "--bind=${ws.absolutePath}:/sdcard/$WORKSPACE_DIR",
                "--bind=${ws.absolutePath}:/storage/emulated/0/$WORKSPACE_DIR",
                *appBinds.toTypedArray()
            )
        }

        val args = arrayOf(
            "proot", "--link2symlink", "--kill-on-exit", "-0",
            "--rootfs=$rootfs", "--cwd=$guestCwd",
            "--bind=/dev", "--bind=/proc", "--bind=/sys",
            *storageBinds,
            "/usr/bin/env", "-i",
            "HOME=/root", "TERM=xterm-256color", "LANG=C.UTF-8",
            // debconf tanpa dialog (PRoot tidak mengizinkan tcsetattr); arena glibc dibatasi demi RAM.
            "DEBIAN_FRONTEND=noninteractive", "MALLOC_ARENA_MAX=2",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "MOLINA_HOME=${MolinaShell.HOME_DIR}",
            *(if (appVisible) arrayOf("MOLINA_ROOT=${MolinaShell.APP_ROOT}") else emptyArray<String>()),
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
        val session = TerminalSession(
            "$nativeDir/libproot.so", ctx.filesDir.absolutePath, args, env, 2000, client
        )
        fullAccessBySession[session] = fullAccess
        return session
    }
}
