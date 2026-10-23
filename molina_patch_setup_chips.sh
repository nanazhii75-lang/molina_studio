#!/usr/bin/env bash
# Molina UI: SetupPanel (pemasangan Debian) + StatusChips (bar status berbingkai).
# Jalankan dari root repo:  cd /sdcard/molina-studio && bash molina_patch_setup_chips.sh
# Aman dijalankan berulang kali (idempotent).
python3 << 'PYEOF'
import os, re, sys

CANDIDATES = [os.environ.get("MOLINA_ROOT", ""), os.getcwd(),
              "/sdcard/molina-studio", "/storage/emulated/0/molina-studio"]
REL_TS = "app/src/main/java/com/molina/studio/terminal/TerminalScreen.kt"
root = next((c for c in CANDIDATES if c and os.path.isfile(os.path.join(c, REL_TS))), None)
if root is None:
    sys.exit("GAGAL: repo molina-studio tidak ditemukan. cd ke root repo atau set MOLINA_ROOT.")
tdir = os.path.join(root, "app/src/main/java/com/molina/studio/terminal")
print("Root repo:", root)

STATUS_CHIPS = r'''package com.molina.studio.terminal

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private const val CHIP_TEXT_SP = 9
private val ChipDownload = Color(0xFF81D4FA) // biru muda
private val ChipUpload = Color(0xFFEF5350)   // merah
private val ChipNormal = Color.White
private val ChipWarn = Color(0xFFFFD54F)     // kuning: lonjakan
private val ChipHigh = Color(0xFFFF5252)     // merah: posisi tinggi
private val ChipLocale: Locale = Locale("id", "ID")

/** Bar status 4 chip berbingkai: unduh, unggah, CPU, RAM. Data dari [SystemStats]. */
@Composable
fun StatusChips() {
    val ctx = LocalContext.current
    val stats by remember(ctx) { SystemStats.flow(ctx) }.collectAsState(StatsSnapshot())
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Chip("\u2193 ${compactSpeed(stats.rxBps)}", ChipDownload, Level.Normal, 1.0f)
        Chip("\u2191 ${compactSpeed(stats.txBps)}", ChipUpload, Level.Normal, 1.0f)
        Chip("CPU ${stats.cpuPercent}%", ChipNormal, stats.cpuLevel, 0.9f)
        Chip(compactRam(stats), ChipNormal, stats.ramLevel, 1.5f)
    }
}

@Composable
private fun RowScope.Chip(text: String, color: Color, level: Level, weight: Float) {
    val tint = when (level) {
        Level.Normal -> color
        Level.Warn -> ChipWarn
        Level.High -> ChipHigh
    }
    // Normal: bingkai tipis transparan. Warn/High: bingkai solid, level terbaca dari bentuk.
    val frame = if (level == Level.Normal) tint.copy(alpha = 0.35f) else tint
    Box(
        Modifier
            .weight(weight)
            .border(1.dp, frame, RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = tint,
            fontFamily = FontFamily.Monospace,
            fontSize = CHIP_TEXT_SP.sp,
            fontWeight = if (level == Level.Normal) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

private fun compactSpeed(bps: Long): String {
    val kb = bps / 1024.0
    return if (kb < 1000) String.format(ChipLocale, "%.0fKB/s", kb)
    else String.format(ChipLocale, "%.1fMB/s", kb / 1024.0)
}

private fun compactRam(s: StatsSnapshot): String {
    val used = if (s.ramMb >= 1024) String.format(ChipLocale, "%.1fGB", s.ramMb / 1024.0)
    else "${s.ramMb}MB"
    return "RAM $used/${s.totalGbText}G"
}
'''

SETUP_PANEL = r'''package com.molina.studio.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private val SetupCardBg = Color(0xFF14161B)
private val SetupCardLine = Color(0xFF2A2D35)
private val SetupMuted = Color(0xFF9AA0A6)
private val SetupGood = Color(0xFF69F0AE)
private val SetupBad = Color(0xFFFF8A80)
private val SetupWarn = Color(0xFFFFD54F)
private val SetupButtonText = Color(0xFF1A1405)
private val SetupLocale: Locale = Locale("id", "ID")

// Puncak disk saat pasang: arsip ~30 MB + hasil ekstrak ~105 MB (arsip dihapus di akhir) + cadangan.
private const val SETUP_REQUIRED_FREE_MB = 140L
// Ukuran unduhan (sama dengan ROOTFS_SIZE di Rootfs.kt); MB ditampilkan diturunkan dari persen.
private const val SETUP_DOWNLOAD_MB = 30.2

private enum class SetupNet { Wifi, Cellular, Ethernet, Other, None }

/**
 * Layar pemasangan Debian: monogram Molina, status, kartu info, langkah dan galat ramah.
 * Murni UI: hanya membaca [RootfsState]; pemasangan dipicu lewat [onInstall].
 */
@Composable
fun SetupPanel(state: RootfsState, onInstall: () -> Unit) {
    val gold = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val installing = state is RootfsState.Downloading ||
        state is RootfsState.Verifying || state is RootfsState.Extracting

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MolinaMonogram()
            Spacer(Modifier.height(10.dp))
            Text("Siapkan lingkungan Linux", color = onSurface, fontSize = 18.sp,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text("Debian trixie \u00B7 arm64", color = SetupMuted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            when {
                state is RootfsState.Failed -> StatusPill("Gagal", SetupBad)
                installing -> StatusPill("Sedang memasang", gold)
                else -> StatusPill("Belum terpasang", SetupWarn)
            }
            Spacer(Modifier.height(14.dp))

            when (state) {
                is RootfsState.Failed -> FailedSection(state.message, onInstall)
                is RootfsState.Downloading, is RootfsState.Verifying, is RootfsState.Extracting ->
                    ProgressSection(state)
                else -> IdleSection(onInstall)
            }

            Spacer(Modifier.height(12.dp))
            Text("Sumber resmi Debian \u00B7 tanpa akun \u00B7 tanpa telemetri",
                color = SetupMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MolinaMonogram() {
    val gold = MaterialTheme.colorScheme.primary
    Box(Modifier.size(40.dp).border(1.5.dp, gold, CircleShape), contentAlignment = Alignment.Center) {
        Text("M", color = gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Row(
        Modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 11.sp)
    }
}

@Composable
private fun SetupCard(
    borderColor: Color = SetupCardLine,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SetupCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 2.dp),
        content = content
    )
}

@Composable
private fun SetupDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(SetupCardLine))
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SetupMuted, fontSize = 12.sp)
        Spacer(Modifier.width(12.dp))
        Text(value, color = valueColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InstallButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val gold = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = gold,
            contentColor = SetupButtonText,
            disabledContainerColor = gold.copy(alpha = 0.25f),
            disabledContentColor = SetupButtonText.copy(alpha = 0.6f)
        )
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun IdleSection(onInstall: () -> Unit) {
    val ctx = LocalContext.current
    val initialNet = remember { currentNet(ctx) }
    val net by produceState(initialNet) {
        while (true) {
            delay(3000)
            value = withContext(Dispatchers.IO) { currentNet(ctx) }
        }
    }
    val free = remember { freeBytes(ctx) }
    val enoughDisk = free < 0 || free >= SETUP_REQUIRED_FREE_MB * 1024L * 1024L
    val freeText = if (free < 0) "tidak diketahui" else "tersedia ${formatBytes(free)}"
    val (netText, netColor) = when (net) {
        SetupNet.Wifi -> "Wi-Fi" to MaterialTheme.colorScheme.onSurface
        SetupNet.Ethernet -> "Ethernet" to MaterialTheme.colorScheme.onSurface
        SetupNet.Cellular -> "Data seluler" to SetupWarn
        SetupNet.Other -> "Terhubung" to MaterialTheme.colorScheme.onSurface
        SetupNet.None -> "Tidak ada koneksi" to SetupBad
    }

    SetupCard {
        InfoRow("Unduhan", "~30 MB")
        SetupDivider()
        InfoRow("Ruang disk", "~105 MB \u00B7 $freeText", if (enoughDisk) SetupGood else SetupBad)
        SetupDivider()
        InfoRow("Jaringan", netText, netColor)
        SetupDivider()
        InfoRow("Verifikasi", "SHA-256")
    }
    if (net == SetupNet.Cellular) {
        Spacer(Modifier.height(6.dp))
        Text("Unduhan ~30 MB akan memakai kuota data seluler.", color = SetupWarn,
            fontSize = 10.sp, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(14.dp))
    val canInstall = net != SetupNet.None && enoughDisk
    InstallButton("Pasang Debian", canInstall, onInstall)
    if (!canInstall) {
        Spacer(Modifier.height(6.dp))
        Text(
            if (net == SetupNet.None) "Tidak ada koneksi internet."
            else "Ruang penyimpanan kurang (butuh ~$SETUP_REQUIRED_FREE_MB MB).",
            color = SetupBad, fontSize = 11.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressSection(state: RootfsState) {
    val gold = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val labels = listOf("Unduh", "Verifikasi", "Ekstrak")
    val current = when (state) {
        is RootfsState.Downloading -> 0
        is RootfsState.Verifying -> 1
        else -> 2
    }
    SetupCard {
        labels.forEachIndexed { i, label ->
            val mark: String
            val markColor: Color
            when {
                i < current -> { mark = "\u2713"; markColor = SetupGood }
                i == current -> { mark = "\u25CF"; markColor = gold }
                else -> { mark = "\u25CB"; markColor = SetupMuted }
            }
            val detail = when {
                i != current -> ""
                state is RootfsState.Downloading -> downloadText(state.percent)
                state is RootfsState.Extracting -> "${state.percent}%"
                else -> "SHA-256"
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mark, color = markColor, fontSize = 12.sp, modifier = Modifier.width(18.dp))
                Text(label, color = if (i <= current) onSurface else SetupMuted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(detail, color = markColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            if (i < labels.lastIndex) SetupDivider()
        }
    }
    Spacer(Modifier.height(12.dp))
    val barModifier = Modifier.fillMaxWidth().height(3.dp)
    when (state) {
        is RootfsState.Downloading -> LinearProgressIndicator(
            progress = { state.percent / 100f }, modifier = barModifier,
            color = gold, trackColor = SetupCardLine)
        is RootfsState.Extracting -> LinearProgressIndicator(
            progress = { state.percent / 100f }, modifier = barModifier,
            color = gold, trackColor = SetupCardLine)
        else -> LinearProgressIndicator(modifier = barModifier, color = gold, trackColor = SetupCardLine)
    }
    Spacer(Modifier.height(8.dp))
    Text("Biarkan aplikasi tetap terbuka sampai selesai.", color = SetupMuted,
        fontSize = 10.sp, textAlign = TextAlign.Center)
}

@Composable
private fun FailedSection(message: String, onRetry: () -> Unit) {
    val ctx = LocalContext.current
    SetupCard(borderColor = SetupBad.copy(alpha = 0.6f)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Text(friendlyError(message), color = SetupBad, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(message, color = SetupMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(14.dp))
    InstallButton("Coba lagi", true, onRetry)
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("molina-setup", "Pemasangan Debian gagal: $message"))
        Toast.makeText(ctx, "Pesan galat disalin", Toast.LENGTH_SHORT).show()
    }) {
        Text("Salin pesan galat", color = SetupMuted, fontSize = 12.sp)
    }
}

private fun friendlyError(msg: String): String {
    val m = msg.lowercase(Locale.ROOT)
    return when {
        "checksum" in m || "sha-256" in m -> "Berkas unduhan rusak (checksum tidak cocok). Coba lagi."
        "no space" in m || "enospc" in m || "ruang" in m -> "Ruang penyimpanan tidak cukup."
        "http" in m -> "Server menolak unduhan. Coba lagi beberapa saat lagi."
        "unable to resolve" in m || "unknownhost" in m || "failed to connect" in m ||
            "timed out" in m || "timeout" in m || "network" in m ->
            "Tidak dapat terhubung ke server. Periksa koneksi lalu coba lagi."
        else -> "Pemasangan gagal. Coba lagi."
    }
}

private fun downloadText(percent: Int): String =
    String.format(SetupLocale, "%.1f / %.1f MB", percent * SETUP_DOWNLOAD_MB / 100.0, SETUP_DOWNLOAD_MB)

private fun formatBytes(b: Long): String =
    if (b >= (1L shl 30)) String.format(SetupLocale, "%.1f GB", b / 1073741824.0)
    else "${b shr 20} MB"

private fun freeBytes(ctx: Context): Long =
    runCatching { StatFs(ctx.filesDir.absolutePath).availableBytes }.getOrDefault(-1L)

private fun currentNet(ctx: Context): SetupNet = runCatching {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        ?: return@runCatching SetupNet.None
    when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> SetupNet.Wifi
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> SetupNet.Ethernet
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> SetupNet.Cellular
        else -> SetupNet.Other
    }
}.getOrDefault(SetupNet.Other)
'''

def write_if_changed(path, content):
    if os.path.isfile(path) and open(path, encoding="utf-8").read() == content:
        print("sudah ada :", os.path.relpath(path, root))
        return
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("ditulis   :", os.path.relpath(path, root))

write_if_changed(os.path.join(tdir, "StatusChips.kt"), STATUS_CHIPS)
write_if_changed(os.path.join(tdir, "SetupPanel.kt"), SETUP_PANEL)

# ---- Patch TerminalScreen.kt ----
ts_path = os.path.join(root, REL_TS)
src = open(ts_path, encoding="utf-8").read()
orig = src

if "StatusChips()" not in src:
    if src.count("        StatusBar()\n") != 1:
        sys.exit("GAGAL: pemanggilan StatusBar() tidak ditemukan tepat satu kali.")
    src = src.replace("        StatusBar()\n", "        StatusChips()\n")

if "SetupPanel(rootfsState)" not in src:
    old = "InstallPanel(rootfsState) { RootfsManager.install(context) }"
    if src.count(old) != 1:
        sys.exit("GAGAL: pemanggilan InstallPanel tidak ditemukan tepat satu kali.")
    src = src.replace(old, "SetupPanel(rootfsState) { RootfsManager.install(context) }")

a = src.find("private val DownloadColor")
if a != -1:
    b = src.find("private fun createTerminalView(")
    if b == -1 or b < a:
        sys.exit("GAGAL: batas blok StatusBar/InstallPanel lama tidak ditemukan.")
    seg = src[a:b]
    if (seg.count("private fun ") != 3 or "private fun StatusBar" not in seg
            or "private fun InstallPanel" not in seg or "private fun levelStyle" not in seg):
        sys.exit("GAGAL: blok lama berbeda dari yang diharapkan, patch dihentikan (tidak ada yang diubah).")
    src = src[:a].rstrip("\n") + "\n\n" + src[b:]

for fq in ["androidx.compose.material3.LinearProgressIndicator",
           "androidx.compose.material3.Button",
           "androidx.compose.ui.text.SpanStyle",
           "androidx.compose.ui.text.buildAnnotatedString",
           "androidx.compose.ui.text.font.FontFamily",
           "androidx.compose.ui.text.withStyle"]:
    line = "import %s\n" % fq
    if line in src:
        body = src.replace(line, "")
        rest = "\n".join(l for l in body.splitlines() if not l.startswith("import "))
        if not re.search(r"\b%s\b" % fq.rsplit(".", 1)[1], rest):
            src = body

if src != orig:
    with open(ts_path, "w", encoding="utf-8") as f:
        f.write(src)
    print("dipatch   :", REL_TS)
else:
    print("sudah ada :", REL_TS)

# ---- progres.md ----
ENTRY = ("- 2026-10-23: UI Terminal: panel pemasangan Debian baru (`SetupPanel.kt`: monogram Molina, "
         "pill status, kartu info disk/jaringan/SHA-256, langkah Unduh/Verifikasi/Ekstrak, galat ramah "
         "+ Salin pesan galat) dan bar status 4 chip berbingkai (`StatusChips.kt`, 9sp, bingkai solid "
         "saat Warn/High) menggantikan `InstallPanel`/`StatusBar` di TerminalScreen.kt. Font dikecilkan "
         "(judul 18sp, isi 11-12sp, tombol 14sp). Tombol Batal ditunda (perlu ubah Rootfs.kt). "
         "Menunggu CI + uji perangkat.")
pp = os.path.join(root, "progres.md")
if os.path.isfile(pp):
    t = open(pp, encoding="utf-8").read()
    if "SetupPanel.kt" in t:
        print("sudah ada : progres.md")
    else:
        mark = "\n\n## Roadmap Prioritas"
        t = t.replace(mark, "\n" + ENTRY + mark, 1) if mark in t else t.rstrip("\n") + "\n" + ENTRY + "\n"
        with open(pp, "w", encoding="utf-8") as f:
            f.write(t)
        print("dicatat   : progres.md")
else:
    print("PERINGATAN: progres.md tidak ditemukan di", root)

print("\nSelesai. Berikutnya:")
print("  git add -A && git commit -m \"feat(term): panel pemasangan Debian dan bar status chip\"")
PYEOF
