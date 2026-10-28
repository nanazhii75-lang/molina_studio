package com.molina.studio.settings

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
import com.molina.studio.cinema.MpvVersionBridge
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
            "Arsitektur" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "?"),
            "libmpv" to (MpvVersionBridge.mpv(ctx) ?: "belum tercatat (putar video sekali)"),
            "FFmpeg" to (MpvVersionBridge.ffmpeg(ctx) ?: "belum tercatat (putar video sekali)")
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
