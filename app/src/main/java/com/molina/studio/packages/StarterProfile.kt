package com.molina.studio.packages

/**
 * [TERM-STARTER1] Profil "Starter": paket apt terkurasi yang dipasang sekali ketuk dari Setelan > Sistem > Debian.
 * Sumbernya hanya repositori resmi Debian (terverifikasi tanda tangan apt). yt-dlp sengaja tidak ikut: ia dipasang
 * di Setelan > Paket karena memverifikasi SHA-256. Idempoten: paket yang sudah ada dilewati apt.
 * ca-certificates WAJIB ada: rootfs trixie-slim tidak membawanya dan apt di sini memakai --no-install-recommends,
 * tanpa itu git/yt-dlp/pip gagal HTTPS (CERTIFICATE_VERIFY_FAILED) [ROOTFS-CA].
 * git sengaja tidak dibundel [STARTER-NOGIT]: opsional, pengguna memasangnya sendiri dari Setelan > Paket.
 * Daftar di [PACKAGES] dan di [SCRIPT] harus sama (dijaga scripts/app-tests/run_starter_profile.sh).
 */
object StarterProfile {
    val PACKAGES = listOf("ca-certificates", "python3", "ffmpeg", "openssh-client", "vim")

    /** Ambang ruang kosong (KB) di [SCRIPT]: 1 GB. df gagal/kosong di PRoot = lanjut tanpa cek. */
    const val MIN_FREE_KB = 1_048_576L

    val SCRIPT: String = """
        set -- $(df -Pk /root 2>/dev/null | awk 'NR==2{print $4}')
        if [ -n "$1" ] && [ "$1" -lt 1048576 ] 2>/dev/null; then
          echo "Ruang kosong kurang dari 1 GB. Kosongkan ruang lalu coba lagi."
          exit 4
        fi
        apt-get update -o APT::Sandbox::User=root && apt-get install -y --no-install-recommends -o APT::Sandbox::User=root ca-certificates python3 ffmpeg openssh-client vim
    """.trimIndent()
}
