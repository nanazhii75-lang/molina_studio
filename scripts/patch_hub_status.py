import os, sys
ROOT = os.getcwd()
S = ROOT + "/app/src/main/java/com/molina/studio/settings"
SCREEN, BACKUP = S + "/SettingsScreen.kt", S + "/BackupManager.kt"
HUB, ABOUT = S + "/HubStatusUi.kt", S + "/AboutUi.kt"
PROG = ROOT + "/progres.md"
for p in (SCREEN, BACKUP):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + os.path.basename(p) + ")")
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

# ===== berkas baru: HubStatusUi.kt (kartu status + komponen tabel bersama) =====
newfile(HUB, r'''package com.molina.studio.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.studio.automation.AutoState
import com.molina.studio.terminal.RootfsManager
import com.molina.studio.terminal.RootfsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Nada status pada kartu Molina Hub: Ok hijau, Warn kuning, Bad merah, Idle abu-abu. */
internal enum class HubTone { Ok, Warn, Bad, Idle }

internal class HubRow(
    val label: String,
    val value: String,
    val tone: HubTone = HubTone.Idle,
    val onClick: (() -> Unit)? = null
)

private fun hubToneColor(t: HubTone): Color = when (t) {
    HubTone.Ok -> Color(0xFF4CC38A)
    HubTone.Warn -> Color(0xFFE5A93C)
    HubTone.Bad -> Color(0xFFE5645A)
    HubTone.Idle -> W7D.TextDim
}

internal fun hubTime(ms: Long): String =
    if (ms <= 0L) "belum pernah"
    else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))

/** Baris status dari data yang sudah ada (tanpa pemeriksaan baru): Debian, apt, yt-dlp, cadangan, otomasi. */
internal fun hubStatusRows(
    rootfs: RootfsState,
    aptPending: Int,
    auto: AutoState,
    backupMs: Long,
    onSystem: (() -> Unit)? = null,
    onPackages: (() -> Unit)? = null,
    onAutomation: (() -> Unit)? = null
): List<HubRow> {
    val debian = when (rootfs) {
        RootfsState.Ready -> Pair("Siap", HubTone.Ok)
        RootfsState.Missing -> Pair("Belum terpasang", HubTone.Warn)
        RootfsState.Verifying -> Pair("Memverifikasi rootfs", HubTone.Idle)
        is RootfsState.Downloading -> Pair("Mengunduh ${rootfs.percent}%", HubTone.Idle)
        is RootfsState.Extracting -> Pair("Mengekstrak ${rootfs.percent}%", HubTone.Idle)
        is RootfsState.Failed -> Pair("Gagal dipasang", HubTone.Bad)
    }
    val apt = when {
        aptPending < 0 -> Pair("Belum diperiksa", HubTone.Idle)
        aptPending == 0 -> Pair("Semua terbaru", HubTone.Ok)
        else -> Pair("$aptPending pembaruan tersedia", HubTone.Warn)
    }
    val yt = when {
        auto.ytdlpInstalled.isEmpty() -> Pair("Belum terpasang atau belum diketahui", HubTone.Idle)
        auto.ytdlpOutdated -> Pair("${auto.ytdlpInstalled} (terbaru ${auto.ytdlpLatest})", HubTone.Warn)
        else -> Pair(auto.ytdlpInstalled, HubTone.Ok)
    }
    val backup =
        if (backupMs <= 0L) Pair("Belum ada cadangan", HubTone.Warn) else Pair(hubTime(backupMs), HubTone.Ok)
    val autoRow =
        if (auto.autoUpdate) Pair("Aktif, cek terakhir ${hubTime(auto.lastCheck)}", HubTone.Ok)
        else Pair("Mati", HubTone.Idle)
    return listOf(
        HubRow("Debian", debian.first, debian.second, onSystem),
        HubRow("Paket Debian", apt.first, apt.second, onSystem),
        HubRow("yt-dlp", yt.first, yt.second, onPackages),
        HubRow("Cadangan", backup.first, backup.second, onAutomation),
        HubRow("Pembaruan otomatis", autoRow.first, autoRow.second, onAutomation)
    )
}

/** Tabel bergaya Aero (sama dengan W7SystemSummary); [dots] menampilkan titik status per baris. */
@Composable
internal fun HubCard(title: String, rows: List<HubRow>, dots: Boolean = true) {
    val shape = RoundedCornerShape(3.dp)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, W7D.Hairline, shape).background(W7D.Card)) {
        Text(
            title, color = W7D.NavHeader, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(W7D.BarTop, W7D.BarBottom)))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        )
        rows.forEach { r ->
            val click = r.onClick
            Row(
                Modifier.fillMaxWidth()
                    .then(if (click != null) Modifier.clickable(onClick = click) else Modifier)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dots) {
                    Box(Modifier.size(8.dp).background(hubToneColor(r.tone), CircleShape))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    r.label, color = W7D.TextDim, fontSize = 12.sp,
                    modifier = Modifier.width(if (dots) 112.dp else 96.dp)
                )
                Text(r.value, color = W7D.Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** Kartu "Status Molina" di beranda Settings; ketuk baris = buka halaman terkait. */
@Composable
internal fun W7HubStatusCard(
    aptPending: Int,
    auto: AutoState,
    onSystem: () -> Unit,
    onPackages: () -> Unit,
    onAutomation: () -> Unit
) {
    val ctx = LocalContext.current
    remember { RootfsManager.refresh(ctx) }
    val rootfs by RootfsManager.state.collectAsState()
    val backupMs = remember(auto.lastBackup) { maxOf(auto.lastBackup, BackupManager.lastBackupMs(ctx)) }
    HubCard("Status Molina", hubStatusRows(rootfs, aptPending, auto, backupMs, onSystem, onPackages, onAutomation))
    Spacer(Modifier.height(12.dp))
}
''')

# ===== berkas baru: AboutUi.kt (About lengkap + Salin info debug) =====
newfile(ABOUT, r'''package com.molina.studio.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.molina.studio.automation.AutoPrefs
import com.molina.studio.terminal.RootfsManager

/** Halaman About: versi, perangkat, status ringkas, tombol Salin info debug, dan atribusi lisensi. */
@Composable
internal fun HubAboutPage() {
    val ctx = LocalContext.current
    remember { AptHistory.init(ctx); AutoPrefs.init(ctx); RootfsManager.refresh(ctx) }
    val rootfs by RootfsManager.state.collectAsState()
    val pending by AptHistory.pending.collectAsState()
    val auto by AutoPrefs.state.collectAsState()
    val base = remember {
        val pi = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
        @Suppress("DEPRECATION")
        val code = pi?.versionCode?.toString() ?: "?"
        listOf(
            "Molina Studio" to "${pi?.versionName ?: "?"} (kode $code)",
            "Paket" to ctx.packageName,
            "Diperbarui" to hubTime(pi?.lastUpdateTime ?: 0L),
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Perangkat" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Arsitektur" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "?")
        )
    }
    val status = hubStatusRows(rootfs, pending, auto, maxOf(auto.lastBackup, BackupManager.lastBackupMs(ctx)))
    HubCard("Versi dan perangkat", base.map { HubRow(it.first, it.second) }, dots = false)
    Spacer(Modifier.height(12.dp))
    HubCard("Status Molina", status)
    Spacer(Modifier.height(8.dp))
    W7Link({ W7IconInfo() }, "Salin info debug", "Salin ringkasan versi dan status untuk laporan bug") {
        val text = (base + status.map { it.label to it.value }).joinToString("\n") { "${it.first}: ${it.second}" }
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Molina info debug", text))
        Toast.makeText(ctx, "Info debug disalin", Toast.LENGTH_SHORT).show()
    }
    Category("Lisensi dan atribusi")
    SettingsItem(
        "Terminal emulator",
        "Berbasis Termux v0.118.0 (terminal-emulator, terminal-view; GPLv3). Lihat THIRD_PARTY_NOTICES.md."
    ) {}
    SettingsItem("PRoot", "PRoot prebuilt untuk Android arm64 (GPL-2.0-or-later).") {}
    SettingsItem("Debian", "Rootfs Debian trixie-slim arm64 resmi, diunduh saat runtime dan diverifikasi SHA-256.") {}
    SettingsItem("Pemutar", "Berbasis mpv-android. Lihat feature/mpv/LICENSE-mpv-android.") {}
    SettingsItem("Editor Code", "sora-editor 0.23.6 (LGPL-2.1-or-later), tanpa modifikasi.") {}
}
''')

# ===== BackupManager: waktu cadangan terakhir dari berkas =====
sub(BACKUP, "    fun hasBackup(ctx: Context): Boolean = file(ctx).isFile\n",
    "    fun hasBackup(ctx: Context): Boolean = file(ctx).isFile\n\n"
    "    /** Waktu ubah berkas cadangan gabungan (ms); 0 = belum ada. */\n"
    "    fun lastBackupMs(ctx: Context): Long = file(ctx).takeIf { it.isFile }?.lastModified() ?: 0L\n",
    "fun lastBackupMs(")

# ===== SettingsScreen: kartu status di Home, About baru, indeks pencarian =====
sub(SCREEN,
    "                    Page.Home -> {\n                        W7CategoryTile(\n                            icon = { W7IconMonitor(52.dp) },",
    "                    Page.Home -> {\n"
    "                        W7HubStatusCard(\n"
    "                            aptPending = aptPending,\n"
    "                            auto = autoSt,\n"
    "                            onSystem = { lastTile = Page.System; open(Page.System) },\n"
    "                            onPackages = { lastTile = Page.Packages; open(Page.Packages) },\n"
    "                            onAutomation = { open(Page.Automation) }\n"
    "                        )\n"
    "                        W7CategoryTile(\n                            icon = { W7IconMonitor(52.dp) },",
    "W7HubStatusCard(")
sub(SCREEN, "                    Page.About -> AboutPage()\n",
    "                    Page.About -> HubAboutPage()\n", "HubAboutPage()")
sub(SCREEN,
    '// ---------- About ----------\n\n@Composable\nprivate fun AboutPage() {\n'
    '    val ctx = LocalContext.current\n'
    '    val version = remember {\n'
    '        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"\n'
    '    }\n'
    '    SettingsItem("Molina Studio", "Versi $version") {}\n'
    '    SettingsItem(\n'
    '        "Terminal emulator",\n'
    '        "Berbasis Termux v0.118.0 (terminal-emulator, terminal-view; GPLv3). Lihat THIRD_PARTY_NOTICES.md."\n'
    '    ) {}\n'
    '}\n\n',
    "// ---------- About: lihat AboutUi.kt ----------\n\n",
    "// ---------- About: lihat AboutUi.kt")
sub(SCREEN, '    Hit("About", "Versi dan lisensi", Page.About)\n)',
    '    Hit("About", "Versi, perangkat, status, dan lisensi", Page.About),\n'
    '    Hit("Salin info debug", "Salin ringkasan versi dan status untuk laporan bug", Page.About),\n'
    '    Hit("Status Molina", "Ringkasan Debian, paket, yt-dlp, dan cadangan di beranda", Page.Home)\n)',
    'Hit("Salin info debug"')

# ===== progres.md =====
ENTRY = '''
## 2026-10-28 - Settings: kartu Status Molina + About lengkap [HUB-STATUS]
- [x] feat(settings): kartu "Status Molina" di beranda Settings (Debian, paket apt, yt-dlp, cadangan, pembaruan otomatis); ketuk baris membuka halaman terkait. Memakai data yang sudah ada (RootfsManager, AptHistory, AutoPrefs), tanpa pemeriksaan/jaringan baru
- [x] feat(settings): About lengkap (versi + kode, waktu pembaruan, Android/API, perangkat, ABI, status ringkas), tombol "Salin info debug", dan atribusi lisensi (Termux, PRoot, Debian, mpv-android, sora-editor)
- [x] BackupManager.lastBackupMs(ctx): waktu ubah berkas cadangan (cadangan manual ikut terbaca, bukan hanya otomatis)
- [x] Dampak: :app settings (HubStatusUi.kt, AboutUi.kt baru; SettingsScreen, BackupManager). Gradle, Manifest, JNI mpv TIDAK disentuh. Tanpa URL baru (aman untuk telemetry_gate)
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah (`when` atas RootfsState sealed, `remember { a; b; c }`)
- [ ] uji perangkat (Android 8): beranda Settings > kartu Status Molina tampil di atas ubin; ketuk baris Debian/yt-dlp/Cadangan > pindah halaman; About > ketuk "Salin info debug" > tempel di catatan
- [ ] KETERBATASAN: versi libmpv dan SHA commit build belum ditampilkan (SHA butuh buildConfig di Gradle; versi mpv hanya tersedia dari log mpv)
'''
if "[HUB-STATUS]" in (rd(PROG) if os.path.isfile(PROG) else ""):
    print("SKIP   progres.md [HUB-STATUS]")
else:
    open(PROG, "a", encoding="utf-8").write(ENTRY); print("UBAH   progres.md [HUB-STATUS]")

if fails:
    sys.exit("SELESAI DENGAN GAGAL: " + str(len(fails)) + " pola tidak cocok; tidak ada yang dipaksa.")
print("OK")
