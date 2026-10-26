package com.molina.studio.settings

import androidx.compose.runtime.Composable

/** Pintasan perawatan Debian di Settings > Sistem. Dijalankan lewat [ToolRunDialog] di Debian PRoot. */
internal enum class AptJob(val title: String, val command: String, val timeoutMs: Long, val note: String) {
    Update(
        "apt update", "apt update -o APT::Sandbox::User=root", 10 * 60_000L,
        "Memperbarui daftar paket Debian. Butuh internet."
    ),
    Upgrade(
        "apt upgrade -y",
        "apt upgrade -y -o APT::Sandbox::User=root -o Dpkg::Options::=--force-confold -o Dpkg::Options::=--force-confdef",
        40 * 60_000L,
        "Meningkatkan semua paket Debian tanpa bertanya. Bisa mengunduh banyak data dan memakan beberapa menit. " +
            "Jalankan apt update lebih dulu."
    )
}

@Composable
internal fun DebianAptDialog(job: AptJob, onClose: () -> Unit) {
    ToolRunDialog(ToolRun("Debian: ${job.title}", job.command, job.timeoutMs, job.note, true), onClose)
}
