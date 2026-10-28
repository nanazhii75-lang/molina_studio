import os, sys
ROOT = os.getcwd()
A = ROOT + "/app/src/main/java/com/molina/studio"
S, L, C = A + "/settings", A + "/log", A + "/cinema"
SCREEN, LOGSCR = S + "/SettingsScreen.kt", L + "/LogScreen.kt"
HUB = S + "/HubStatusUi.kt"
HWDEC, PLAYER, REPORT = C + "/HwdecBridge.kt", S + "/PlayerSettingsUi.kt", L + "/BugReport.kt"
PROG = ROOT + "/progres.md"
for p in (SCREEN, LOGSCR):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + os.path.basename(p) + ")")
if not os.path.isfile(HUB):
    sys.exit("BERHENTI: terapkan dulu scripts/patch_hub_status.py (tidak ada HubStatusUi.kt)")
fails = []

def rd(p):
    return open(p, encoding="utf-8").read()

def sub(p, old, new, marker):
    s = rd(p)
    if marker in s:
        print("SKIP  ", os.path.basename(p), "[" + marker[:40] + "]"); return
    if s.count(old) != 1:
        print("GAGAL ", os.path.basename(p), "pola tidak unik/tidak ada:", old[:60].replace("\n", " "))
        fails.append(p); return
    open(p, "w", encoding="utf-8").write(s.replace(old, new))
    print("UBAH  ", os.path.basename(p), "[" + marker[:40] + "]")

def newfile(p, content):
    if os.path.isfile(p) and rd(p) == content:
        print("SKIP  ", os.path.basename(p), "(sudah sama)"); return
    open(p, "w", encoding="utf-8").write(content)
    print("BARU  ", os.path.basename(p))

# ===== cinema/HwdecBridge.kt: sakelar dekoder perangkat keras (kunci sama dengan Setelan pemutar) =====
newfile(HWDEC, r'''package com.molina.studio.cinema

import android.content.Context

/**
 * Jembatan Settings Molina -> pemutar mpv untuk dekoder perangkat keras (MediaCodec). Memakai kunci yang
 * sama dengan Setelan pemutar (hardware_decoding, bawaan true); MpvPrefs membacanya saat pemutar dimulai,
 * jadi perubahan berlaku pada video berikutnya yang dibuka.
 */
internal object HwdecBridge {
    private const val KEY = "hardware_decoding"

    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(ctx.applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Boolean = try {
        sp(ctx).getBoolean(KEY, true)
    } catch (e: ClassCastException) {
        true
    }

    fun setEnabled(ctx: Context, v: Boolean) {
        sp(ctx).edit().putBoolean(KEY, v).apply()
    }
}
''')

# ===== settings/PlayerSettingsUi.kt: ikon ubin + halaman Pemutar =====
newfile(PLAYER, r'''package com.molina.studio.settings

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.molina.core.common.MolinaDir
import com.molina.studio.AppPaths
import com.molina.studio.cinema.CinemaActions
import com.molina.studio.cinema.HwdecBridge
import com.molina.studio.cinema.IptvSources
import com.molina.studio.cinema.ResumeBridge

/** Ikon ubin Pemutar di beranda Settings: panel Aero biru dengan segitiga putar. */
@Composable
internal fun W7IconPlayerBig(box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF3A4A5E), Offset(w * 0.10f, h * 0.06f), Size(w * 0.80f, h * 0.88f), CornerRadius(w * 0.08f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF8FC4EE), Color(0xFF2D6DB5))),
            Offset(w * 0.15f, h * 0.11f), Size(w * 0.70f, h * 0.78f), CornerRadius(w * 0.05f)
        )
        val tri = Path().apply {
            moveTo(w * 0.40f, h * 0.30f)
            lineTo(w * 0.40f, h * 0.70f)
            lineTo(w * 0.72f, h * 0.50f)
            close()
        }
        drawPath(tri, Color.White)
    }
}

/** Halaman Pemutar: pemutaran (resume, dekoder), sumber IPTV, dan pintasan ke setelan lengkap pemutar. */
@Composable
internal fun PlayerSettingsPage(onOpenCinema: () -> Unit) {
    val ctx = LocalContext.current
    var resume by remember { mutableStateOf(ResumeBridge.enabled(ctx)) }
    var hw by remember { mutableStateOf(HwdecBridge.enabled(ctx)) }
    var sources by remember { mutableStateOf(IptvSources.load(ctx)) }
    var removing by remember { mutableStateOf<IptvSources.Source?>(null) }
    val iptvPath = remember { runCatching { AppPaths.get(ctx).dir(MolinaDir.Iptv).path }.getOrDefault("") }

    Category("Pemutaran")
    SwitchRow("Ingat posisi putar", "Lanjutkan video dari posisi terakhir saat dibuka lagi.", resume) {
        resume = it
        ResumeBridge.setEnabled(ctx, it)
    }
    SwitchRow(
        "Dekoder perangkat keras",
        "MediaCodec. Matikan bila video hijau atau patah-patah. Berlaku pada video berikutnya yang dibuka.",
        hw
    ) {
        hw = it
        HwdecBridge.setEnabled(ctx, it)
    }

    Category("Sumber IPTV")
    if (sources.isEmpty()) {
        SettingsItem("Belum ada sumber playlist", "Tambahkan dari dialog Saluran di Molina Cinema.") {}
    } else {
        sources.forEach { s -> SettingsItem(s.name, "Ketuk untuk menghapus dari daftar") { removing = s } }
    }
    SettingsItem("Folder IPTV", "$iptvPath\nSalin berkas .m3u atau .m3u8 ke folder ini.") {}

    Category("Pemutar")
    SettingsItem("Setelan pemutar lengkap", "Video, antarmuka, gestur, dan lanjutan") {
        if (!CinemaActions.openSettings(ctx)) {
            Toast.makeText(ctx, "Setelan pemutar tidak bisa dibuka", Toast.LENGTH_SHORT).show()
        }
    }
    SettingsItem("Buka Molina Cinema", "Pindah ke tab Cinema", onOpenCinema)

    removing?.let { s ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Hapus sumber IPTV?") },
            text = { Text(s.name) },
            confirmButton = {
                TextButton(onClick = {
                    IptvSources.remove(ctx, s.url)
                    sources = IptvSources.load(ctx)
                    removing = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Batal") } }
        )
    }
}
''')

# ===== log/BugReport.kt: laporan bug = ringkasan + per kelompok + gabungan kronologis + crash =====
newfile(REPORT, r'''package com.molina.studio.log

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.molina.core.common.LogLevel
import com.molina.core.common.LogRedact
import com.molina.core.common.MolinaDir
import com.molina.studio.AppPaths
import com.molina.studio.automation.AutoPrefs
import com.molina.studio.settings.AptHistory
import com.molina.studio.settings.BackupManager
import com.molina.studio.settings.hubStatusRows
import com.molina.studio.terminal.CrashReporter
import com.molina.studio.terminal.RootfsManager
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Laporan bug dalam satu berkas, empat bagian: (1) ringkasan, (2) PER KELOMPOK (Cinema, Terminal, Sistem,
 * Logcat, Crash) supaya sumber masalah mudah dikenali, (3) GABUNGAN kronologis semua sumber, (4) laporan
 * crash terbaru. Seluruh teks disamarkan (URL dan token) karena folder Export terbaca aplikasi lain.
 */
internal object BugReport {
    private const val GROUP_MAX_LINES = 400
    private const val CRASH_MAX = 3
    private const val CRASH_MAX_CHARS = 12_000

    private val GROUPS = listOf(
        "cinema" to "CINEMA (pemutar mpv)",
        "terminal" to "TERMINAL",
        "sistem" to "SISTEM",
        "logcat" to "LOGCAT",
        "crash" to "CRASH",
        "lainnya" to "LAINNYA"
    )
    private val TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault())

    private fun groupOf(source: String): String = when (source) {
        "cinema", "mpv" -> "cinema"
        "terminal", "sistem", "logcat", "crash" -> source
        else -> "lainnya"
    }

    private fun line(e: LogEntry, withSource: Boolean): String {
        val n = if (e.count > 1) "  \u00D7${e.count}" else ""
        val src = if (withSource) "${e.source}/" else ""
        return "${TS.format(Instant.ofEpochMilli(e.time))} ${e.level.code} $src${e.tag}: ${e.message}$n"
    }

    fun build(ctx: Context, entries: List<LogEntry>, status: List<Pair<String, String>>, crashes: List<File>): String {
        val ver = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
        val byGroup = entries.groupBy { groupOf(it.source) }
        val sb = StringBuilder()
        sb.appendLine("MOLINA LAPORAN BUG")
        sb.appendLine("Dibuat    : ${DATE.format(Instant.now())}")
        sb.appendLine("Versi app : $ver")
        sb.appendLine(
            "Perangkat : ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} " +
                "(API ${Build.VERSION.SDK_INT}), ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}"
        )
        status.forEach { sb.appendLine("${it.first.padEnd(18)}: ${it.second}") }
        sb.appendLine("Isi       : 1) Ringkasan  2) Per kelompok  3) Gabungan kronologis  4) Crash")
        sb.appendLine()

        sb.appendLine("=== 1. RINGKASAN ===")
        sb.appendLine(
            "Total ${entries.size} baris; ${entries.count { it.level == LogLevel.Error }} galat, " +
                "${entries.count { it.level == LogLevel.Warn }} peringatan"
        )
        GROUPS.forEach { (key, title) ->
            val list = byGroup[key].orEmpty()
            if (list.isNotEmpty()) {
                sb.appendLine(
                    "- $title: ${list.size} baris, ${list.count { it.level == LogLevel.Error }} galat, " +
                        "${list.count { it.level == LogLevel.Warn }} peringatan"
                )
            }
        }
        val errs = entries.filter { it.level == LogLevel.Error }.takeLast(10)
        if (errs.isNotEmpty()) {
            sb.appendLine("Galat terakhir:")
            errs.forEach { sb.appendLine("  " + line(it, true)) }
        }
        sb.appendLine()

        sb.appendLine("=== 2. PER KELOMPOK ===")
        GROUPS.forEach { (key, title) ->
            val list = byGroup[key].orEmpty()
            if (list.isEmpty()) return@forEach
            val tags = list.groupingBy { it.tag }.eachCount().entries
                .sortedByDescending { it.value }.take(5).joinToString(", ") { "${it.key} (${it.value})" }
            sb.appendLine("--- $title: ${list.size} baris ---")
            sb.appendLine("Tag terbanyak: $tags")
            if (list.size > GROUP_MAX_LINES) {
                sb.appendLine("(menampilkan $GROUP_MAX_LINES baris terakhir; lengkapnya di bagian 3)")
            }
            list.takeLast(GROUP_MAX_LINES).forEach { sb.appendLine(line(it, false)) }
            sb.appendLine()
        }

        sb.appendLine("=== 3. GABUNGAN KRONOLOGIS (${entries.size} baris, sumber/tag di tiap baris) ===")
        entries.forEach { sb.appendLine(line(it, true)) }
        sb.appendLine()

        sb.appendLine("=== 4. CRASH (${crashes.size} laporan terbaru) ===")
        if (crashes.isEmpty()) sb.appendLine("Tidak ada laporan crash.")
        crashes.forEach { f ->
            sb.appendLine("--- ${CrashReporter.title(f)} ---")
            sb.appendLine(CrashReporter.read(f).take(CRASH_MAX_CHARS))
            sb.appendLine()
        }
        return sb.toString()
    }

    /** Simpan laporan ke <appRoot>/Documents/Export (thread latar; hasil lewat Toast). */
    fun export(ctx: Context) {
        val app = ctx.applicationContext
        val entries = LogBus.entries.value
        Thread({
            val msg = try {
                AptHistory.init(app)
                AutoPrefs.init(app)
                RootfsManager.refresh(app)
                val auto = AutoPrefs.state.value
                val status = hubStatusRows(
                    RootfsManager.state.value, AptHistory.pending.value, auto,
                    maxOf(auto.lastBackup, BackupManager.lastBackupMs(app))
                ).map { it.label to it.value }
                val text = LogRedact.text(build(app, entries, status, CrashReporter.list(app).take(CRASH_MAX)))
                val dir = AppPaths.get(app).dir(MolinaDir.Export)
                if (!dir.isDirectory && !dir.mkdirs()) throw IOException("folder tidak bisa dibuat: ${dir.path}")
                val file = File(dir, "molina-laporan-bug-${STAMP.format(Instant.now())}.txt")
                file.writeText(text, Charsets.UTF_8)
                "Laporan tersimpan: ${file.path}"
            } catch (e: Exception) {
                "Gagal menyimpan laporan: ${e.message}"
            }
            Handler(Looper.getMainLooper()).post { Toast.makeText(app, msg, Toast.LENGTH_LONG).show() }
        }, "molina-bugreport").start()
    }
}
''')

# ===== LogScreen: menu aksi =====
sub(LOGSCR,
    '                    DropdownMenuItem(\n                        text = { Text("Kirim pesan uji", fontSize = 13.sp) },',
    '                    DropdownMenuItem(\n'
    '                        text = { Text("Laporan bug (gabungan + per kelompok)", fontSize = 13.sp) },\n'
    '                        onClick = { actionMenu = false; BugReport.export(ctx) }\n'
    '                    )\n'
    '                    DropdownMenuItem(\n                        text = { Text("Kirim pesan uji", fontSize = 13.sp) },',
    "BugReport.export(ctx)")

# ===== SettingsScreen: halaman Pemutar, ubin Home, tautan laporan bug, indeks pencarian =====
sub(SCREEN, "import com.molina.studio.log.LogScreen\n",
    "import com.molina.studio.log.BugReport\nimport com.molina.studio.log.LogScreen\n", "import com.molina.studio.log.BugReport")
sub(SCREEN, "Storage, Monitor, Automation\n}", "Storage, Monitor, Automation, Player\n}", "Automation, Player\n}")
sub(SCREEN, "Page.TerminalApp, Page.Downloads -> Page.Home",
    "Page.TerminalApp, Page.Downloads, Page.Player -> Page.Home", "Page.Downloads, Page.Player -> Page.Home")
sub(SCREEN, '    Page.Automation -> "Otomasi dan pembaruan"\n}',
    '    Page.Automation -> "Otomasi dan pembaruan"\n    Page.Player -> "Pemutar"\n}', 'Page.Player -> "Pemutar"')
sub(SCREEN,
    '    Hit("Cadangan otomatis", "Cadangan gabungan harian ke folder Backup", Page.Automation),\n',
    '    Hit("Cadangan otomatis", "Cadangan gabungan harian ke folder Backup", Page.Automation),\n'
    '    Hit("Pemutar", "Molina Cinema: ingat posisi putar, dekoder, sumber IPTV", Page.Player),\n'
    '    Hit("Ingat posisi putar", "Lanjutkan video dari posisi terakhir", Page.Player),\n'
    '    Hit("Dekoder perangkat keras", "MediaCodec untuk pemutar mpv", Page.Player),\n'
    '    Hit("Sumber IPTV", "Daftar playlist saluran dan Folder IPTV", Page.Player),\n'
    '    Hit("Laporan bug", "Ekspor log gabungan dan per kelompok ke folder Export", Page.System),\n',
    'Hit("Sumber IPTV"')
sub(SCREEN,
    "                            onOpen = { lastTile = Page.Downloads; open(Page.Downloads) }\n                        )\n",
    "                            onOpen = { lastTile = Page.Downloads; open(Page.Downloads) }\n                        )\n"
    "                        W7CategoryTile(\n"
    "                            icon = { W7IconPlayerBig(52.dp) },\n"
    '                            title = "Pemutar",\n'
    "                            links = emptyList(),\n"
    "                            selected = lastTile == Page.Player,\n"
    "                            onOpen = { lastTile = Page.Player; open(Page.Player) }\n"
    "                        )\n",
    "W7IconPlayerBig(52.dp)")
sub(SCREEN, "                    Page.Stream -> StreamEnginePage()\n",
    "                    Page.Player -> PlayerSettingsPage(onOpenCinema = { com.molina.studio.ShortcutNav.tab = 0 })\n"
    "                    Page.Stream -> StreamEnginePage()\n",
    "Page.Player -> PlayerSettingsPage(")
sub(SCREEN,
    '                        W7Link({ W7IconLog() }, "Log", "Log aktivitas mpv, Terminal, dan sistem") { open(Page.Log) }\n',
    '                        W7Link({ W7IconLog() }, "Log", "Log aktivitas mpv, Terminal, dan sistem") { open(Page.Log) }\n'
    '                        W7Link({ W7IconLog() }, "Laporan bug", "Gabungan kronologis dan per kelompok (Cinema, Terminal, Sistem, Logcat, Crash) ke folder Export") { BugReport.export(aptCtx) }\n',
    "BugReport.export(aptCtx)")

# ===== progres.md =====
ENTRY = '''
## 2026-10-28 - Settings: ubin Pemutar + laporan bug gabungan dan per kelompok [HUB-PLAYER-REPORT]
- [x] feat(settings): ubin "Pemutar" di beranda Settings > halaman Pemutar: sakelar Ingat posisi putar (ResumeBridge), sakelar Dekoder perangkat keras (HwdecBridge, kunci hardware_decoding), daftar sumber IPTV (ketuk = hapus, URL tidak ditampilkan), jalur Folder IPTV, pintasan ke Setelan pemutar lengkap dan tab Cinema
- [x] feat(log): BugReport.export: satu berkas molina-laporan-bug-<stempel>.txt di Documents/Export berisi (1) ringkasan, (2) PER KELOMPOK (Cinema, Terminal, Sistem, Logcat, Crash; tag terbanyak per kelompok, maks 400 baris terakhir), (3) GABUNGAN kronologis semua sumber, (4) 3 laporan crash terbaru; semua disamarkan LogRedact (URL dan token)
- [x] Tautan: menu titik tiga di halaman Log dan "Laporan bug" di halaman Sistem. Ekspor log lama (Salin, Bagikan, Simpan .txt terfilter) tidak diubah
- [x] Dampak: :app settings (PlayerSettingsUi.kt baru, SettingsScreen), :app cinema (HwdecBridge.kt baru), :app log (BugReport.kt baru, LogScreen). Gradle, Manifest, JNI mpv TIDAK disentuh. Wajib setelah patch_hub_status.py
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah-sedang (`when` penuh atas enum Page, destrukturisasi Pair di forEach, Path di Canvas)
- [ ] uji perangkat (Android 8): Settings > ubin Pemutar > ubah sakelar, buka video: dekoder mengikuti; ketuk sumber IPTV > hapus; Log > titik tiga > Laporan bug > buka berkas di Documents/Export: bagian 1-4 ada, URL tersamarkan
- [ ] KETERBATASAN: sakelar dekoder berlaku pada video berikutnya (bukan yang sedang diputar); log hanya sejak proses hidup (buffer 1000 baris); folder Export terbaca aplikasi lain, jadi tinjau berkas sebelum dibagikan
'''
if "[HUB-PLAYER-REPORT]" in (rd(PROG) if os.path.isfile(PROG) else ""):
    print("SKIP   progres.md [HUB-PLAYER-REPORT]")
else:
    open(PROG, "a", encoding="utf-8").write(ENTRY); print("UBAH   progres.md [HUB-PLAYER-REPORT]")

if fails:
    sys.exit("SELESAI DENGAN GAGAL: " + str(len(fails)) + " pola tidak cocok; tidak ada yang dipaksa.")
print("OK")
