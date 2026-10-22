package com.molina.studio.terminal

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import java.io.File

enum class SessionKind(val label: String) {
    Debian("Debian"),
    Android("Android sh")
}

/** Sesi tetap hidup walau tab berpindah (nanti dipindah ke foreground service). */
object TerminalSessions {
    val client = MolinaSessionClient()
    private val sessions = HashMap<SessionKind, TerminalSession>()

    @Volatile var currentSession: TerminalSession? = null
        private set

    var selected by mutableStateOf(SessionKind.Debian)

    @Synchronized
    fun session(context: Context, kind: SessionKind): TerminalSession {
        val app = context.applicationContext
        val s = sessions[kind]?.takeIf { it.isRunning }
            ?: create(app, kind).also { sessions[kind] = it }
        currentSession = s
        return s
    }

    private fun create(ctx: Context, kind: SessionKind): TerminalSession = when (kind) {
        SessionKind.Android -> androidShell(ctx)
        SessionKind.Debian -> debianShell(ctx)
    }

    private fun workDir(): File = File(Environment.getExternalStorageDirectory(), "molina-studio")

    private fun androidShell(ctx: Context): TerminalSession {
        val home = ctx.filesDir.absolutePath
        val work = workDir()
        val cwd = if (work.isDirectory && work.canRead()) work.absolutePath else home
        val env = arrayOf(
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "HOME=$home",
            "TMPDIR=${ctx.cacheDir.absolutePath}",
            "PATH=/system/bin:/system/xbin",
            "LANG=en_US.UTF-8"
        )
        return TerminalSession("/system/bin/sh", cwd, arrayOf("sh"), env, 2000, client)
    }

    private fun debianShell(ctx: Context): TerminalSession {
        val nativeDir = ctx.applicationInfo.nativeLibraryDir
        val rootfs = RootfsManager.rootfsDir(ctx).absolutePath
        val tmp = File(ctx.cacheDir, "proot-tmp").apply { mkdirs() }
        val work = workDir()
        val guestCwd = if (work.isDirectory && work.canRead()) "/sdcard/molina-studio" else "/root"
        val storage = Environment.getExternalStorageDirectory().absolutePath

        val args = arrayOf(
            "proot", "--link2symlink", "--kill-on-exit", "-0",
            "--rootfs=$rootfs", "--cwd=$guestCwd",
            "--bind=/dev", "--bind=/proc", "--bind=/sys",
            "--bind=$storage:/sdcard",
            "/usr/bin/env", "-i",
            "HOME=/root", "TERM=xterm-256color", "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "/bin/bash", "--login"
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
