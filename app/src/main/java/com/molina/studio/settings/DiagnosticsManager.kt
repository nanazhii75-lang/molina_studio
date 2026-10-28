package com.molina.studio.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.molina.core.common.LogRedact
import com.molina.core.common.MolinaDir
import com.molina.studio.AppPaths
import com.molina.studio.automation.AutoPrefs
import com.molina.studio.cinema.MpvVersionBridge
import com.molina.studio.log.BugReport
import com.molina.studio.log.LogBus
import com.molina.studio.terminal.CrashReporter
import com.molina.studio.terminal.RootfsManager
import com.molina.studio.terminal.TerminalPrefs
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Diagnostik sekali klik: satu ZIP di <appRoot>/Documents/Export berisi ringkasan perangkat dan versi
 * (ringkasan.txt), laporan bug gabungan (laporan-bug.txt), dan laporan crash (crash/). Semua teks melewati
 * LogRedact karena folder Export terbaca aplikasi lain: URL hanya skema + host, token disamarkan.
 * Operasi disk: panggil di luar main thread.
 */
internal object DiagnosticsManager {
    private const val PREFIX = "molina-diagnostik-"
    private const val KEEP = 3
    private const val CRASH_IN_REPORT = 3
    private const val CRASH_IN_ZIP = 10
    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault())
    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    private fun logLevelName(level: Int): String = when (level) {
        TerminalPrefs.LOG_OFF -> "mati"
        TerminalPrefs.LOG_NORMAL -> "normal"
        TerminalPrefs.LOG_DEBUG -> "debug"
        TerminalPrefs.LOG_VERBOSE -> "verbose"
        else -> "?"
    }

    private fun summary(ctx: Context, status: List<Pair<String, String>>): String {
        val pi = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
        @Suppress("DEPRECATION")
        val code = pi?.versionCode?.toString() ?: "?"
        val native = ctx.applicationInfo.nativeLibraryDir
        fun lib(name: String) = if (File(native, name).isFile) "ada" else "TIDAK ADA"
        val auto = AutoPrefs.state.value
        val rows = listOf(
            "Dibuat" to DATE.format(Instant.now()),
            "Molina Studio" to "${pi?.versionName ?: "?"} (kode $code)",
            "Paket" to ctx.packageName,
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Perangkat" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
            "libmpv" to (MpvVersionBridge.mpv(ctx) ?: "belum tercatat"),
            "FFmpeg" to (MpvVersionBridge.ffmpeg(ctx) ?: "belum tercatat"),
            "yt-dlp terpasang" to auto.ytdlpInstalled.ifEmpty { "belum diketahui" },
            "yt-dlp terbaru" to auto.ytdlpLatest.ifEmpty { "belum diperiksa" },
            "Level log Terminal" to logLevelName(TerminalPrefs.settings.value.logLevel),
            "PRoot (libproot.so)" to lib("libproot.so"),
            "PRoot loader" to lib("libproot_loader.so")
        )
        return buildString {
            appendLine("MOLINA DIAGNOSTIK")
            rows.forEach { appendLine("${it.first.padEnd(20)}: ${it.second}") }
            appendLine()
            appendLine("--- Status Molina ---")
            status.forEach { appendLine("${it.first.padEnd(20)}: ${it.second}") }
        }
    }

    /** Buat ZIP diagnostik baru dan hapus yang lama (menyisakan [KEEP] terbaru). */
    fun build(ctx: Context): File {
        val app = ctx.applicationContext
        AptHistory.init(app)
        AutoPrefs.init(app)
        TerminalPrefs.init(app)
        RootfsManager.refresh(app)
        val auto = AutoPrefs.state.value
        val status = hubStatusRows(
            RootfsManager.state.value, AptHistory.pending.value, auto,
            maxOf(auto.lastBackup, BackupManager.lastBackupMs(app))
        ).map { it.label to it.value }
        val crashes = CrashReporter.list(app)
        val report = BugReport.build(app, LogBus.entries.value, status, crashes.take(CRASH_IN_REPORT))
        val head = summary(app, status)

        val dir = AppPaths.get(app).dir(MolinaDir.Export)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("folder tidak bisa dibuat: ${dir.path}")
        val out = File(dir, "$PREFIX${STAMP.format(Instant.now())}.zip")
        val tmp = File(dir, out.name + ".tmp")
        ZipOutputStream(tmp.outputStream().buffered()).use { z ->
            fun put(name: String, text: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(LogRedact.text(text).toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            put("ringkasan.txt", head)
            put("laporan-bug.txt", report)
            crashes.take(CRASH_IN_ZIP).forEach { put("crash/${it.name}", CrashReporter.read(it)) }
        }
        if (!tmp.renameTo(out)) throw IOException("gagal menyimpan ${out.name}")
        dir.listFiles { f -> f.isFile && f.name.startsWith(PREFIX) && f.name.endsWith(".zip") }
            ?.sortedByDescending { it.name }?.drop(KEEP)?.forEach { it.delete() }
        return out
    }

    /** Buka chooser sistem untuk membagikan ZIP (FileProvider, izin baca sementara). */
    fun share(ctx: Context, f: File) {
        try {
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "Bagikan diagnostik")
            if (ctx !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "Tidak ada aplikasi untuk membagikan berkas. ZIP ada di: ${f.path}", Toast.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(ctx, "ZIP tersimpan di: ${f.path}", Toast.LENGTH_LONG).show()
        }
    }
}
