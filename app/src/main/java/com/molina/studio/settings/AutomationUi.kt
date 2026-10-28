package com.molina.studio.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.molina.studio.cinema.ResumeBridge
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.molina.studio.automation.AutoPrefs
import com.molina.studio.automation.AutoState
import com.molina.studio.automation.AutoWork
import com.molina.studio.automation.UpdateChecker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Menumpuk lencana di tepi kanan sebuah baris (mis. W7Link) tanpa mengubah tanda tangan W7Link. */
@Composable
internal fun BadgeOverlay(badge: String?, aero: Boolean, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        content()
        if (badge != null) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) { W7Badge(badge, aero) }
        }
    }
}

private fun whenText(at: Long, never: String): String =
    if (at <= 0L) never else SimpleDateFormat("d MMM HH.mm", Locale("id", "ID")).format(Date(at))

private fun ytdlpText(s: AutoState): String = when {
    s.ytdlpInstalled.isEmpty() && s.ytdlpLatest.isEmpty() -> "Belum diperiksa, atau yt-dlp belum terpasang"
    s.ytdlpOutdated -> "Pembaruan tersedia: ${s.ytdlpInstalled} \u2192 ${s.ytdlpLatest}"
    s.ytdlpInstalled.isNotEmpty() -> "Terbaru (${s.ytdlpInstalled})"
    else -> "Rilis terbaru: ${s.ytdlpLatest}"
}

/** Settings > Sistem > Otomasi dan pembaruan: pemeriksaan pembaruan otomatis dan cadangan harian. */
@Composable
internal fun AutomationPage() {
    val ctx = LocalContext.current
    remember { AutoPrefs.init(ctx); AptHistory.init(ctx) }
    val st by AutoPrefs.state.collectAsState()
    val pending by AptHistory.pending.collectAsState()
    val running by UpdateChecker.running.collectAsState()

    Category("Pembaruan otomatis")
    SwitchRow(
        "Periksa pembaruan otomatis",
        "Maksimal sekali per 24 jam dan hanya saat Wi-Fi. Menyegarkan lencana paket Debian (ubin Sistem) dan " +
            "yt-dlp (ubin Paket) tanpa membuka dialog. Tanpa notifikasi dan tidak memasang apa pun.",
        st.autoUpdate
    ) {
        AutoPrefs.setAutoUpdate(ctx, it)
        AutoWork.sync(ctx)
    }
    SettingsItem(
        if (running) "Memeriksa\u2026" else "Periksa sekarang",
        "Terakhir: " + whenText(st.lastCheck, "belum pernah") + ". Berjalan di latar dan bisa memakai data seluler."
    ) { if (!running) UpdateChecker.checkNow(ctx) }
    SettingsItem("Paket Debian", AptHistory.checkSubtitle(pending)) {}
    SettingsItem("yt-dlp", ytdlpText(st)) {}

    Category("Cadangan otomatis")
    SwitchRow(
        "Cadangan gabungan harian",
        "Menyimpan penanda video, snippet Terminal, dan setelan ke folder Backup sekali per hari. " +
            "Salinan sebelumnya dijaga sebagai molina-backup.prev.json.",
        st.autoBackup
    ) {
        AutoPrefs.setAutoBackup(ctx, it)
        AutoWork.sync(ctx)
    }
    SettingsItem("Cadangan otomatis terakhir", whenText(st.lastBackup, "belum pernah")) {}

    Category("Produktivitas")
    var resume by remember { mutableStateOf(ResumeBridge.enabled(ctx)) }
    SwitchRow(
        "Lanjutkan dari posisi terakhir",
        "Molina Player mengingat posisi putar dan melanjutkannya. Sama dengan setelan Simpan posisi di pemutar. " +
            "Kartu Lanjutkan di beranda Cinema memakainya.",
        resume
    ) {
        resume = it
        ResumeBridge.setEnabled(ctx, it)
    }
    SwitchRow(
        "Unduhan masuk ke Cinema",
        "Setelah unduhan selesai, video atau audio yang berhasil otomatis muncul di Terakhir diputar Molina Cinema " +
            "agar bisa diputar dengan satu ketukan.",
        st.addToCinema
    ) { AutoPrefs.setAddToCinema(ctx, it) }
}
