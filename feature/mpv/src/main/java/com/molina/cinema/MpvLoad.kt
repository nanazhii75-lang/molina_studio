package com.molina.cinema

import com.molina.core.common.StreamHeaders

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

    fun loadfileArgs(url: String, flag: String, headers: Map<String, String>): Array<String> {
        val opt = headerOption(headers) ?: return arrayOf("loadfile", url, flag)
        return arrayOf("loadfile", url, flag, "-1", opt)
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
