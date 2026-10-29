package com.molina.studio.terminal

/**
 * Pencarian teks di transkrip Terminal (scrollback + layar). JVM murni agar bisa diuji di scripts/app-tests.
 * Pencocokan literal (bukan regex), tanpa membedakan huruf besar-kecil, hasil terbaru di atas,
 * satu hasil per baris, dan cuplikan dipotong di sekitar kecocokan agar baris panjang tetap terbaca.
 */
internal object TerminalSearch {
    const val MAX_QUERY = 200
    const val MAX_HITS = 200
    private const val LEFT = 80
    private const val RIGHT = 160

    /** [line] = nomor baris 1-based dari atas transkrip; [snippet] = cuplikan; [start]/[end] = rentang kecocokan di cuplikan. */
    data class Hit(val line: Int, val snippet: String, val start: Int, val end: Int)

    fun find(transcript: String, query: String, max: Int = MAX_HITS): List<Hit> {
        val q = query.trim().take(MAX_QUERY)
        if (q.isEmpty() || transcript.isEmpty() || max <= 0) return emptyList()
        val lines = transcript.lines()
        val out = ArrayList<Hit>()
        for (i in lines.indices.reversed()) {
            val raw = lines[i].trimEnd()
            val at = raw.indexOf(q, ignoreCase = true)
            if (at < 0) continue
            out.add(window(i + 1, raw, at, at + q.length))
            if (out.size >= max) break
        }
        return out
    }

    private fun window(line: Int, raw: String, s: Int, e: Int): Hit {
        if (raw.length <= LEFT + RIGHT) return Hit(line, raw, s, e)
        val from = maxOf(0, s - LEFT)
        val to = minOf(raw.length, e + RIGHT)
        val prefix = if (from > 0) "\u2026" else ""
        val suffix = if (to < raw.length) "\u2026" else ""
        val body = raw.substring(from, to)
        val shift = prefix.length - from
        return Hit(line, prefix + body + suffix, s + shift, e + shift)
    }
}
