package com.molina.core.common

/** Label umur unggahan berbahasa Indonesia untuk hasil pencarian: "3 hari lalu", "2 tahun lalu". */
object RelativeTime {
    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val DAY = 24 * HOUR

    /** [atMs] dan [nowMs] epoch ms. Hasil adalah perkiraan kasar (bulan = 30 hari, tahun = 365 hari). */
    fun ago(atMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val d = nowMs - atMs
        return when {
            d < MIN -> "baru saja"
            d < HOUR -> "${d / MIN} menit lalu"
            d < DAY -> "${d / HOUR} jam lalu"
            d < 7 * DAY -> "${d / DAY} hari lalu"
            d < 30 * DAY -> "${d / (7 * DAY)} minggu lalu"
            d < 365 * DAY -> "${d / (30 * DAY)} bulan lalu"
            else -> "${d / (365 * DAY)} tahun lalu"
        }
    }

    private val EN_AGO = Regex("""(\d+)\s+(second|minute|hour|day|week|month|year)s?\s+ago""", RegexOption.IGNORE_CASE)

    private fun translate(text: String): String {
        val m = EN_AGO.find(text) ?: return text
        val n = m.groupValues[1]
        val unit = when (m.groupValues[2].lowercase()) {
            "second" -> "detik"
            "minute" -> "menit"
            "hour" -> "jam"
            "day" -> "hari"
            "week" -> "minggu"
            "month" -> "bulan"
            else -> "tahun"
        }
        return "$n $unit lalu"
    }

    /** Label untuk satu hasil, atau null bila waktu unggah tidak diketahui. */
    fun label(item: StreamSearchItem, nowMs: Long = System.currentTimeMillis()): String? =
        item.uploadedAtMs?.let { ago(it, nowMs) }
            ?: item.uploadedText?.trim()?.takeIf { it.isNotEmpty() }?.let(::translate)
}
