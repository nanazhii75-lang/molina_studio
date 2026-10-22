package com.molina.studio.terminal

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.Locale

data class StatsSnapshot(
    val rxBps: Long = 0,
    val txBps: Long = 0,
    val cpuPercent: Int = 0,
    val ramMb: Int = 0,
    val totalRamMb: Int = 0
) {
    fun format(): String =
        "\u2193 ${speed(rxBps)}    \u2191 ${speed(txBps)}    CPU $cpuPercent%    " +
            "RAM app $ramMb MB/${String.format(ID, "%.1f", totalRamMb / 1024.0)}GB"

    private fun speed(bps: Long): String {
        val kb = bps / 1024.0
        return if (kb < 1000) String.format(ID, "%.1f KB/s", kb)
        else String.format(ID, "%.1f MB/s", kb / 1024.0)
    }

    private companion object {
        val ID: Locale = Locale("id", "ID")
    }
}

/** Statistik seluruh proses ber-UID aplikasi (termasuk proot dan guest Debian). */
object SystemStats {
    fun flow(context: Context): Flow<StatsSnapshot> = flow {
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val uid = Process.myUid()
        val clk = Os.sysconf(OsConstants._SC_CLK_TCK).coerceAtLeast(1L)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val totalMb = (ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem / MB).toInt()

        var lastRx = TrafficStats.getUidRxBytes(uid)
        var lastTx = TrafficStats.getUidTxBytes(uid)
        var lastTicks = ownPids(uid).sumOf { cpuTicks(it) }
        var lastAt = SystemClock.elapsedRealtime()
        var ramMb = 0
        var n = 0
        while (true) {
            delay(1000)
            val now = SystemClock.elapsedRealtime()
            val dt = (now - lastAt).coerceAtLeast(1L) / 1000.0
            lastAt = now

            val pids = ownPids(uid)
            val ticks = pids.sumOf { cpuTicks(it) }
            val cpu = ((ticks - lastTicks).coerceAtLeast(0L) / clk.toDouble() / dt * 100.0 / cores)
                .toInt().coerceIn(0, 100)
            lastTicks = ticks

            val rx = TrafficStats.getUidRxBytes(uid)
            val tx = TrafficStats.getUidTxBytes(uid)
            val rxBps = if (rx >= 0 && lastRx >= 0) ((rx - lastRx) / dt).toLong().coerceAtLeast(0) else 0L
            val txBps = if (tx >= 0 && lastTx >= 0) ((tx - lastTx) / dt).toLong().coerceAtLeast(0) else 0L
            lastRx = rx
            lastTx = tx

            if (n++ % 3 == 0) ramMb = pssMb(am, pids)
            emit(StatsSnapshot(rxBps, txBps, cpu, ramMb, totalMb))
        }
    }.flowOn(Dispatchers.IO)

    private const val MB = 1024L * 1024L

    private fun ownPids(uid: Int): List<Int> {
        val dirs = File("/proc").listFiles() ?: return emptyList()
        val out = ArrayList<Int>()
        for (d in dirs) {
            val pid = d.name.toIntOrNull() ?: continue
            val owner = try { Os.stat("/proc/$pid").st_uid } catch (e: Exception) { continue }
            if (owner == uid) out.add(pid)
        }
        return out
    }

    /** utime + stime (dalam clock tick) dari /proc/<pid>/stat. */
    private fun cpuTicks(pid: Int): Long = try {
        val f = File("/proc/$pid/stat").readText().substringAfterLast(')').trim().split(' ')
        f[11].toLong() + f[12].toLong()
    } catch (e: Exception) {
        0L
    }

    private fun pssMb(am: ActivityManager, pids: List<Int>): Int = try {
        if (pids.isEmpty()) 0
        else (am.getProcessMemoryInfo(pids.toIntArray()).sumOf { it.totalPss.toLong() } / 1024L).toInt()
    } catch (e: Exception) {
        0
    }
}
