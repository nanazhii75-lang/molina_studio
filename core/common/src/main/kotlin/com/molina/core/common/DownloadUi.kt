package com.molina.core.common

import java.util.Locale

/** Format teks untuk halaman Unduhan. Murni JVM supaya bisa diuji tanpa Android. */
object DownloadFormat {
    /** Ukuran berkas/laju: B, KB, MB, GB (basis 1024). Nilai negatif dianggap 0. */
    fun size(bytes: Long): String {
        val b = bytes.coerceAtLeast(0L)
        return when {
            b >= 1L shl 30 -> String.format(Locale.ROOT, "%.2f GB", b / (1L shl 30).toDouble())
            b >= 1L shl 20 -> String.format(Locale.ROOT, "%.1f MB", b / (1L shl 20).toDouble())
            b >= 1L shl 10 -> String.format(Locale.ROOT, "%.0f KB", b / (1L shl 10).toDouble())
            else -> "$b B"
        }
    }

    /** Sisa waktu ringkas: "42d", "3m 05d", "1j 02m". Nilai negatif dianggap 0. */
    fun eta(seconds: Long): String {
        val s = seconds.coerceAtLeast(0L)
        return when {
            s >= 3600 -> "${s / 3600}j ${String.format(Locale.ROOT, "%02d", (s % 3600) / 60)}m"
            s >= 60 -> "${s / 60}m ${String.format(Locale.ROOT, "%02d", s % 60)}d"
            else -> "${s}d"
        }
    }
}

/** Pemetaan path hasil unduhan (dilihat dari Debian PRoot) ke path Android. */
object DownloadPaths {
    private const val DEBIAN_SDCARD = "/sdcard"

    /**
     * `/sdcard/...` (Debian) -> path di penyimpanan internal Android, [base] = akar penyimpanan internal
     * (mis. /storage/emulated/0). null bila path di luar /sdcard, relatif, memuat segmen `..` atau karakter
     * kontrol: keluaran skrip diperlakukan tidak tepercaya dan tidak boleh keluar dari /sdcard.
     */
    fun toAndroid(debianPath: String, base: String): String? {
        if (debianPath != DEBIAN_SDCARD && !debianPath.startsWith("$DEBIAN_SDCARD/")) return null
        if (debianPath.any { it.isISOControl() }) return null
        if (debianPath.split('/').any { it == ".." }) return null
        val root = base.trimEnd('/')
        if (root.isEmpty() || !root.startsWith("/")) return null
        return root + debianPath.removePrefix(DEBIAN_SDCARD)
    }
}

/** Hasil penggabungan URL ke kolom input: [added] baru ditambahkan, [skipped] sudah ada, [dropped] melewati batas. */
data class UrlMerge(val text: String, val added: Int, val skipped: Int, val dropped: Int)

/** Penggabungan URL dari luar halaman (mis. kartu hasil pencarian Cinema) ke kolom URL. */
object DownloadUrls {
    /** Pecah teks input per spasi/baris, buang yang kosong dan duplikat (urutan dipertahankan). */
    fun split(text: String): List<String> = DownloadCommand.clean(text.split(Regex("\\s+")))

    /**
     * Tambahkan [incoming] ke [existing]: duplikat dilewati, total dibatasi [DownloadCommand.MAX_URLS].
     * Hasil satu URL per baris. URL yang ada tidak diubah urutannya.
     */
    fun merge(existing: String, incoming: List<String>): UrlMerge {
        val list = split(existing).toMutableList()
        var added = 0
        var skipped = 0
        var dropped = 0
        for (u in DownloadCommand.clean(incoming)) {
            when {
                u in list -> skipped++
                list.size >= DownloadCommand.MAX_URLS -> dropped++
                else -> { list.add(u); added++ }
            }
        }
        return UrlMerge(list.joinToString("\n"), added, skipped, dropped)
    }
}
