package com.molina.studio.settings

/** [SHIELD-CARD] Ringkasan status perlindungan untuk halaman Molina Shield. Murni (tanpa Android) agar mudah diuji. */
internal object ShieldSummary {
    data class Input(
        val shield: Boolean,
        val trackers: Boolean,
        val lanAllowed: Boolean,
        val secureScreen: Boolean,
        val blockPlaylistRtmp: Boolean,
        val debuggable: Boolean,
        val blocked: Int,
        val fingerprint: String?,
    )

    class Result(val title: String, val body: String)

    fun build(i: Input): Result {
        val checks = listOf(
            "Filter http/https (Shield)" to i.shield,
            "Blokir pelacak dan iklan" to i.trackers,
            "Jaringan lokal ditolak" to !i.lanAllowed,
            "Layar privat" to i.secureScreen,
            "Blokir rtmp di playlist (opsional)" to i.blockPlaylistRtmp,
            "Build rilis (tidak debuggable)" to !i.debuggable,
        )
        val on = checks.count { it.second }
        val off = checks.filter { !it.second }.map { it.first }
        val sb = StringBuilder()
        sb.append("Diblokir sesi ini: ").append(i.blocked)
        sb.append("\nTanda tangan: ").append(i.fingerprint?.take(23)?.plus("\u2026") ?: "tidak terbaca")
        if (off.isEmpty()) sb.append("\nSemua perlindungan aktif.")
        else sb.append("\nBelum aktif: ").append(off.joinToString(", "))
        return Result("Perlindungan: $on dari ${checks.size} aktif", sb.toString())
    }
}
