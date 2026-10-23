package com.molina.studio.terminal

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.molina.core.common.AppVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.Locale

enum class Level { Normal, Warn, High }

data class StatsSnapshot(
    val rxBps: Long = 0,
    val txBps: Long = 0,
    val cpuPercent: Int = 0,
    val ramMb: Int = 0,
    val totalRamMb: Int = 0,
    val cpuLevel: Level = Level.Normal,
    val ramLevel: Level = Level.Normal
) {
    val rxText: String get() = speed(rxBps)
    val txText: String get() = speed(txBps)
    val totalGbText: String get() = String.format(ID, "%.1f", totalRamMb / 1024.0)

    private fun speed(bps: Long): String {
        val kb = bps / 1024.0
        return if (kb < 1000) String.format(ID, "%.1f KB/s", kb)
        else String.format(ID, "%.1f MB/s", kb / 1024.0)
    }

    private companion object {
        val ID: Locale = Locale("id", "ID")
    }
}

/**
 * Statistik seluruh proses ber-UID aplikasi (termasuk proot dan guest Debian).
 * Hemat: berhenti total saat aplikasi di latar belakang, daftar PID di-cache (dipindai tiap
 * 3 dtk), dan PSS (panggilan binder yang mahal) hanya dihitung tiap 5 dtk.
 */
object SystemStats {
    // Ambang warna CPU (% kapasitas total semua core) dan RAM (% RAM perangkat).
    private const val CPU_SPIKE_POINTS = 10
    private const val CPU_WARN = 20
    private const val CPU_HIGH = 40
    private const val RAM_SPIKE_MB = 50
    private const val RAM_WARN_PCT = 10
    private const val RAM_HIGH_PCT = 20
    private const val HOLD_TICKS = 5 // lama warna kuning dipertahankan setelah lonjakan (detik)

    private const val MB = 1024L * 1024L

    fun flow(context: Context): Flow<StatsSnapshot> = flow {
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val uid = Process.myUid()
        val clk = Os.sysconf(OsConstants._SC_CLK_TCK).coerceAtLeast(1L)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val totalMb = (ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem / MB).toInt()

        while (true) {
            AppVisibility.foreground.first { it } // tidur sampai aplikasi terlihat lagi

            var pids = ownPids(uid)
            var lastRx = TrafficStats.getUidRxBytes(uid)
            var lastTx = TrafficStats.getUidTxBytes(uid)
            var lastTicks = pids.sumOf { cpuTicks(it) }
            var lastAt = SystemClock.elapsedRealtime()
            var lastCpu = 0
            var cpuHold = 0
            var ramMb = pssMb(am, pids)
            var ramHold = 0
            var tick = 0

            while (AppVisibility.foreground.value) {
                delay(1000)
                tick++
                val now = SystemClock.elapsedRealtime()
                val dt = (now - lastAt).coerceAtLeast(1L) / 1000.0
                lastAt = now
                if (tick % 3 == 0) pids = ownPids(uid)

                val ticks = pids.sumOf { cpuTicks(it) }
                val cpu = ((ticks - lastTicks).coerceAtLeast(0L) / clk.toDouble() / dt * 100.0 / cores)
                    .toInt().coerceIn(0, 100)
                lastTicks = ticks
                if (cpuHold > 0) cpuHold--
                if (cpu - lastCpu >= CPU_SPIKE_POINTS) cpuHold = HOLD_TICKS
                lastCpu = cpu

                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)
                val rxBps = if (rx >= 0 && lastRx >= 0) ((rx - lastRx) / dt).toLong().coerceAtLeast(0) else 0L
                val txBps = if (tx >= 0 && lastTx >= 0) ((tx - lastTx) / dt).toLong().coerceAtLeast(0) else 0L
                lastRx = rx
                lastTx = tx

                if (ramHold > 0) ramHold--
                if (tick % 5 == 0) {
                    val m = pssMb(am, pids)
                    if (m - ramMb >= RAM_SPIKE_MB) ramHold = HOLD_TICKS
                    ramMb = m
                }

                val cpuLevel = when {
                    cpu >= CPU_HIGH -> Level.High
                    cpu >= CPU_WARN || cpuHold > 0 -> Level.Warn
                    else -> Level.Normal
                }
                val ramPct = if (totalMb > 0) ramMb * 100 / totalMb else 0
                val ramLevel = when {
                    ramPct >= RAM_HIGH_PCT -> Level.High
                    ramPct >= RAM_WARN_PCT || ramHold > 0 -> Level.Warn
                    else -> Level.Normal
                }
                emit(StatsSnapshot(rxBps, txBps, cpu, ramMb, totalMb, cpuLevel, ramLevel))
            }
        }
    }.flowOn(Dispatchers.IO)

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
