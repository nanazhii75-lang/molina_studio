package com.molina.studio.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.molina.studio.stream.StreamEnginePrefs

/** Halaman "Keamanan jaringan": kebijakan URL untuk mpv dan yt-dlp. Jaringan dianggap tidak tepercaya. */
@Composable
internal fun NetworkSecurityPage() {
    val lan by StreamEnginePrefs.allowLan.collectAsState()
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
    Category("Catatan")
    SettingsItem(
        "Tautan http dan rtmp tidak terenkripsi",
        "Isinya bisa dibaca atau diubah siapa pun di jaringan yang sama. Gunakan https bila tersedia."
    ) {}
}
