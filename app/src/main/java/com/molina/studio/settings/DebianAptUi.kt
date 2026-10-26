package com.molina.studio.settings

import androidx.compose.runtime.Composable

/** Pintasan perawatan Debian di Settings > Sistem. Dijalankan lewat [ToolRunDialog] di Debian PRoot. */
internal enum class AptJob(val title: String, val command: String, val timeoutMs: Long, val note: String) {
    UpdateUpgrade(
        "Perbarui dan tingkatkan paket",
        "apt update -o APT::Sandbox::User=root && " +
            "apt upgrade -y -o APT::Sandbox::User=root -o Dpkg::Options::=--force-confold -o Dpkg::Options::=--force-confdef",
        50 * 60_000L,
        "Memperbarui daftar paket Debian, lalu meningkatkan semua paket tanpa bertanya. " +
            "Butuh internet, bisa mengunduh banyak data dan memakan beberapa menit. " +
            "Upgrade hanya berjalan jika update berhasil."
    )
}

/** Baris kunci keluaran apt yang disimpan dialog untuk ringkasan (aman walau log terpotong). */
private val APT_KEY_LINES = Regex("""(?i)(\d+ upgraded, \d+ newly installed|packages? can be upgraded|all packages are up to date)""")
private val APT_COUNTS = Regex("""(\d+) upgraded, (\d+) newly installed, (\d+) to remove and (\d+) not upgraded""")

/** Ringkasan satu baris untuk status dialog; null bila keluaran tidak dikenali (mis. bahasa lain). */
private fun aptSummary(lines: List<String>): String? {
    val m = lines.mapNotNull { APT_COUNTS.find(it) }.lastOrNull()
    if (m != null) {
        val (up, added, removed, held) = m.destructured
        return if (up == "0" && added == "0") "Sistem sudah terbaru."
        else buildString {
            append("Ringkasan: $up paket ditingkatkan, $added baru dipasang, $removed dihapus")
            if (held != "0") append(", $held ditahan")
            append(".")
        }
    }
    return if (lines.any { it.contains("up to date", ignoreCase = true) }) "Sistem sudah terbaru." else null
}

@Composable
internal fun DebianAptDialog(job: AptJob, onClose: () -> Unit) {
    ToolRunDialog(
        ToolRun("Debian: ${job.title}", job.command, job.timeoutMs, job.note, true, APT_KEY_LINES, ::aptSummary),
        onClose
    )
}
