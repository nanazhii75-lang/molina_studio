package com.molina.studio.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.molina.studio.security.PrivacyPrefs
import com.molina.studio.stream.StreamEnginePrefs

/** Halaman "Keamanan jaringan": kebijakan URL untuk mpv dan yt-dlp. Jaringan dianggap tidak tepercaya. */
@Composable
internal fun NetworkSecurityPage() {
    val lan by StreamEnginePrefs.allowLan.collectAsState()
    val secure by PrivacyPrefs.secureScreen.collectAsState()
    Category("Kebijakan URL")
    SettingsItem(
        "Daftar putih skema",
        "Hanya http, https, rtmp(s), rtsp, dan berkas lokal. concat:, edl://, fd://, data: dan sejenisnya " +
            "selalu ditolak, begitu pula alamat perangkat sendiri (127.x) dan metadata cloud (169.254.x)."
    ) {}
    SwitchRow(
        "Izinkan jaringan lokal (LAN)",
        "Untuk NAS, Jellyfin, DLNA (192.168.x.x, 10.x.x.x, nama .local). Default mati supaya tautan " +
            "jahat tidak bisa memakai aplikasi untuk memindai jaringan rumah Anda.",
        lan
    ) { StreamEnginePrefs.enableLan(it) }
    Category("Privasi layar")
    SwitchRow(
        "Layar privat (Terminal dan Pengaturan)",
        "Menyembunyikan isi layar dari tangkapan layar, perekam layar, dan daftar aplikasi terbaru, " +
            "serta menolak sentuhan saat tertutup overlay aplikasi lain. Matikan bila Anda perlu " +
            "screenshot atau memakai aplikasi filter cahaya biru. Pemutar video tidak terpengaruh.",
        secure
    ) { PrivacyPrefs.setSecureScreen(it) }
    Category("Catatan")
    SettingsItem(
        "Tautan http dan rtmp tidak terenkripsi",
        "Isinya bisa dibaca atau diubah siapa pun di jaringan yang sama. Gunakan https bila tersedia."
    ) {}
}
