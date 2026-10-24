package com.molina.cinema

import com.molina.core.common.AppLog
import com.molina.core.common.LogLevel
import com.molina.core.common.StreamHeaders
import com.molina.core.common.UrlPolicy

/**
 * Menyusun argumen perintah mpv `loadfile` dengan opsi per-entri (header HTTP).
 * Opsi per-entri menempel pada entri playlist mpv, jadi tiap video antrean membawa headernya
 * sendiri tanpa antrean tambahan. Fungsi murni, tanpa Android.
 *
 * Sintaks mpv: `loadfile <url> <flag> <index> <opsi>`; <index> -1 = bawaan (butuh mpv >= 0.38).
 * Nilai memakai awalan panjang-tetap `%N%` (N = jumlah byte) agar koma/titik dua di dalam
 * nilai tidak dibaca sebagai pemisah. Berlapis: tiap header, lalu seluruh daftar.
 */
object MpvLoad {
    const val REPLACE = "replace"
    const val APPEND = "append"

    fun loadfileArgs(url: String, flag: String, headers: Map<String, String>, title: String? = null): Array<String> {
        // Jaringan dianggap tidak tepercaya: URL yang melanggar kebijakan tidak pernah sampai ke mpv.
        val verdict = UrlPolicy.check(url)
        if (verdict is UrlPolicy.Verdict.Blocked) {
            AppLog.log("net", LogLevel.Warn, "policy", "ditolak: ${verdict.reason.name}")
            return arrayOf("show-text", "Diblokir: ${verdict.reason.message}", "5000")
        }
        val opts = listOfNotNull(headerOption(headers), titleOption(title))
        if (opts.isEmpty()) return arrayOf("loadfile", url, flag)
        return arrayOf("loadfile", url, flag, "-1", opts.joinToString(","))
    }

    /** Opsi `force-media-title=...` (maks 200 karakter, tanpa karakter kontrol) atau null. */
    fun titleOption(title: String?): String? {
        val t = title?.filter { it >= ' ' && it != '\u007f' }?.trim()?.take(200)
        if (t.isNullOrEmpty()) return null
        return "force-media-title=" + fixed(t)
    }

    /** Opsi `http-header-fields=...` atau null bila tidak ada header valid. */
    fun headerOption(headers: Map<String, String>): String? {
        val clean = StreamHeaders.sanitize(headers)
        if (clean.isEmpty()) return null
        val list = clean.entries.joinToString(",") { fixed("${it.key}: ${it.value}") }
        return "http-header-fields=" + fixed(list)
    }

    private fun fixed(s: String): String = "%${s.toByteArray(Charsets.UTF_8).size}%$s"
}
