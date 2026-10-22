package com.molina.studio

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.system.Os
import android.system.OsConstants
import com.molina.studio.terminal.TerminalSessions
import java.io.File
import java.lang.ref.WeakReference

/**
 * Pintu keluar tunggal aplikasi: tidak boleh ada proses Molina yang tersisa.
 *
 * Urutan: hook modul (mpv, editor, ...) -> matikan sesi terminal -> sapu SEMUA proses
 * ber-UID aplikasi (proot, guest Debian, anak proses yatim, helper mpv) -> tutup Activity
 * dan hapus dari recents -> bunuh proses aplikasi sendiri.
 *
 * Modul baru wajib mendaftarkan pembersihan, misalnya:
 *   AppExit.register("mpv") { MPVLib.destroy() }
 */
object AppExit {
    private val hooks = LinkedHashMap<String, () -> Unit>()
    @Volatile private var activityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    @Synchronized
    fun register(name: String, hook: () -> Unit) {
        hooks[name] = hook
    }

    @Synchronized
    fun unregister(name: String) {
        hooks.remove(name)
    }

    @Synchronized
    private fun snapshotHooks(): List<() -> Unit> = hooks.values.toList()

    /** Aman dipanggil dari thread mana pun; pekerjaan berat berjalan di thread terpisah. */
    fun exit() {
        Thread({
            for (h in snapshotHooks()) {
                try { h() } catch (e: Throwable) {}
            }
            try { TerminalSessions.killAll() } catch (e: Throwable) {}
            repeat(3) {
                sweepOwnProcesses()
                try { Thread.sleep(60) } catch (e: InterruptedException) {}
            }
            val main = Handler(Looper.getMainLooper())
            main.post {
                activityRef?.get()?.finishAndRemoveTask()
                main.postDelayed({ Process.killProcess(Process.myPid()) }, 200)
            }
        }, "molina-exit").start()
    }

    /** SIGKILL ke setiap proses lain milik UID aplikasi ini (dibaca dari /proc). */
    private fun sweepOwnProcesses() {
        val me = Process.myPid()
        val uid = Process.myUid()
        val dirs = File("/proc").listFiles() ?: return
        for (d in dirs) {
            val pid = d.name.toIntOrNull() ?: continue
            if (pid == me) continue
            val line = try {
                File(d, "status").useLines { seq -> seq.firstOrNull { it.startsWith("Uid:") } }
            } catch (e: Exception) {
                null
            } ?: continue
            val realUid = line.substringAfter("Uid:").trim()
                .split(Regex("\\s+")).firstOrNull()?.toIntOrNull() ?: continue
            if (realUid == uid) {
                try { Os.kill(pid, OsConstants.SIGKILL) } catch (e: Exception) {}
            }
        }
    }
}
