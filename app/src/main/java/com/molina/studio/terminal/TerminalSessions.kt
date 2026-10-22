package com.molina.studio.terminal

import android.content.Context
import android.os.Environment
import com.termux.terminal.TerminalSession
import java.io.File

/** Menyimpan sesi tetap hidup walau tab berpindah (nanti dipindah ke foreground service). */
object TerminalSessions {
    val client = MolinaSessionClient()

    @Volatile var currentSession: TerminalSession? = null
        private set

    fun main(context: Context): TerminalSession {
        currentSession?.let { if (it.isRunning) return it }

        val home = context.filesDir.absolutePath
        val work = File(Environment.getExternalStorageDirectory(), "molina-studio")
        val cwd = if (work.isDirectory && work.canRead()) work.absolutePath else home
        val env = arrayOf(
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "HOME=$home",
            "TMPDIR=${context.cacheDir.absolutePath}",
            "PATH=/system/bin:/system/xbin",
            "LANG=en_US.UTF-8"
        )
        val session = TerminalSession("/system/bin/sh", cwd, arrayOf("sh"), env, 2000, client)
        currentSession = session
        return session
    }
}
