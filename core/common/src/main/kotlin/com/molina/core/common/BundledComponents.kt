package com.molina.core.common

/** Seberapa penting komponen bagi Molina. */
enum class ComponentRole(val label: String) {
    Wajib("Wajib"),
    Pendukung("Pendukung"),
    Opsional("Opsional")
}

/** Dari mana komponen datang. */
enum class ComponentOrigin(val label: String) {
    Apk("Dibundel di APK"),
    Runtime("Diunduh saat dipakai"),
    Debian("Dipasang di Debian")
}

/**
 * Jaminan keamanan yang BENAR-BENAR ada di repo. [Sha256] hanya boleh dipakai bila ada pin hash nyata
 * ([BundledComponent.pinRef]); scripts/bundle_gate.py menggagalkan CI bila klaim ini tidak punya pin.
 */
enum class Assurance(val label: String) {
    Sha256("SHA-256 dipin dan diverifikasi"),
    Commit("Sumber dipin ke commit"),
    Version("Versi dipin di Gradle"),
    Apt("Repositori resmi Debian (apt)"),
    Asset("Aset dibundel beserta lisensi")
}

/**
 * Satu komponen pihak ketiga yang ada di Molina. [version] kosong = ditentukan saat berjalan (mis. yt-dlp).
 * [source] ditulis tanpa skema URL agar gerbang telemetri tetap mengawasi semua host di kode.
 */
data class BundledComponent(
    val id: String,
    val name: String,
    val version: String,
    val role: ComponentRole,
    val origin: ComponentOrigin,
    val assurance: Assurance,
    val source: String,
    val license: String,
    val usedBy: String,
    val note: String,
    val pinRef: String? = null
)

/** Daftar tunggal komponen yang dibundel/dipakai Molina. JVM murni agar bisa diuji. */
object BundledComponents {
    const val PIN_NEWPIPE = "scripts/newpipe/pins.sha256"
    const val PIN_NATIVE = "native/prebuilt.env:NATIVE_SHA256"
    const val PIN_ROOTFS = "Rootfs.kt:ROOTFS_SHA256"
    const val PIN_YTDLP = "yt-dlp:SHA2-256SUMS"

    /** [newpipeVersion] disuntikkan dari NewPipeInfo.VERSION agar hanya ada satu sumber kebenaran. */
    fun all(newpipeVersion: String): List<BundledComponent> = listOf(
        BundledComponent(
            "newpipe", "NewPipeExtractor", newpipeVersion, ComponentRole.Wajib, ComponentOrigin.Apk, Assurance.Sha256,
            "TeamNewPipe/NewPipeExtractor via JitPack", "GPL-3.0", "Pencarian dan pemutaran YouTube (Cinema)",
            "Mesin utama. Jar dan dependensinya dipin SHA-256; build gagal bila hash tidak cocok.", PIN_NEWPIPE
        ),
        BundledComponent(
            "libmpv", "libmpv + FFmpeg", "FFmpeg n9.0.2", ComponentRole.Wajib, ComponentOrigin.Apk, Assurance.Sha256,
            "mpv-android snapshot 62013d4 (mpv, dav1d, libass, libplacebo, FFmpeg dipin commit)", "Lihat THIRD_PARTY_NOTICES.md",
            "Molina Player / Cinema", "Dibangun lewat CI dari commit dipin; tgz hasil build diverifikasi SHA-256 saat build APK.", PIN_NATIVE
        ),
        BundledComponent(
            "terminal", "Terminal emulator + view", "Termux v0.118.0", ComponentRole.Wajib, ComponentOrigin.Apk, Assurance.Commit,
            "termux/termux-app commit 6e2689f", "Apache-2.0", "Molina Terminal",
            "Kode sumber disalin dan di-rebrand (com.molina.terminal); atribusi dipertahankan.", null
        ),
        BundledComponent(
            "proot", "PRoot", "commit 01f83b88", ComponentRole.Pendukung, ComponentOrigin.Apk, Assurance.Commit,
            "green-green-avk/build-proot-android", "GPL-2.0-or-later", "Debian, yt-dlp, Script execution",
            "Biner prebuilt di jniLibs (arm64). Tanpa PRoot, fitur berbasis Debian tidak jalan.", null
        ),
        BundledComponent(
            "rootfs", "Debian rootfs", "trixie-slim arm64", ComponentRole.Pendukung, ComponentOrigin.Runtime, Assurance.Sha256,
            "debuerreotype/docker-debian-artifacts commit 9c4818e", "Beragam (paket Debian)", "Terminal Debian, alat Debian, yt-dlp",
            "Diunduh saat dipasang; hash SHA-256 diperiksa dan unduhan yang tidak cocok dibuang.", PIN_ROOTFS
        ),
        BundledComponent(
            "ytdlp", "yt-dlp", "", ComponentRole.Opsional, ComponentOrigin.Runtime, Assurance.Sha256,
            "yt-dlp/yt-dlp rilis resmi", "Unlicense", "Mesin cadangan unduhan (default mati)",
            "Hanya diunduh saat Anda menekan Pasang/Perbarui; diverifikasi lewat SHA2-256SUMS rilis.", PIN_YTDLP
        ),
        BundledComponent(
            "sora", "sora-editor + TextMate", "0.23.6", ComponentRole.Pendukung, ComponentOrigin.Apk, Assurance.Version,
            "Rosemoe/sora-editor (Maven Central)", "LGPL-2.1-or-later", "Editor Code",
            "Versi sengaja ditahan di 0.23.6 (syarat compileSdk dan Kotlin).", null
        ),
        BundledComponent(
            "fonts", "JetBrains Mono + Fira Code", "Fira Code 6.2", ComponentRole.Opsional, ComponentOrigin.Apk, Assurance.Asset,
            "JetBrains/JetBrainsMono, tonsky/FiraCode", "SIL OFL 1.1", "Pilihan font Editor Code",
            "Salinan lisensi OFL disertakan.", null
        ),
        BundledComponent(
            "archive", "commons-compress, zip4j, xz", "1.27.1 / 2.11.5 / 1.10", ComponentRole.Pendukung, ComponentOrigin.Apk, Assurance.Version,
            "Apache Commons, srikanth-lingala/zip4j, tukaani.org (Maven Central)", "Apache-2.0 / 0BSD", "Arsip di Penjelajah (zip, tar, 7z)",
            "RAR/unrar sengaja tidak disertakan (syarat lisensi).", null
        ),
        BundledComponent(
            "python3", "Python 3", "", ComponentRole.Opsional, ComponentOrigin.Debian, Assurance.Apt,
            "apt Debian", "PSF", "yt-dlp dan skrip Python", "Dipasang dari Paket > Alat Debian. yt-dlp membutuhkannya.", null
        ),
        BundledComponent(
            "git", "Git", "", ComponentRole.Opsional, ComponentOrigin.Debian, Assurance.Apt,
            "apt Debian", "GPL-2.0", "Terminal Debian", "Dipasang dari Paket > Alat Debian.", null
        ),
        BundledComponent(
            "ffmpeg", "FFmpeg (Debian)", "", ComponentRole.Opsional, ComponentOrigin.Debian, Assurance.Apt,
            "apt Debian", "LGPL/GPL", "Konversi audio/video, unduhan yt-dlp", "Dipasang dari Paket > Alat Debian.", null
        )
    )

    /** Jumlah komponen per peran (untuk ringkasan di UI). */
    fun roleCounts(items: List<BundledComponent>): Map<ComponentRole, Int> =
        ComponentRole.values().associateWith { r -> items.count { it.role == r } }
}
