package com.molina.core.common

/** Urutan hasil pencarian: RELEVANCE = urutan dari mesin pencari, NEWEST/OLDEST = menurut waktu unggah. */
enum class SearchSort(val label: String) {
    RELEVANCE("Relevansi"),
    NEWEST("Terbaru"),
    OLDEST("Terlama"),
}

/** Pengurutan dan penyaringan hasil pencarian (JDK murni, dapat diuji di JVM). */
object SearchOrder {
    /**
     * Urutkan [items]. Hasil tanpa waktu unggah (mis. siaran langsung) selalu di akhir.
     * Pengurutan stabil: hasil sewaktu mempertahankan urutan relevansi.
     */
    fun sort(items: List<StreamSearchItem>, sort: SearchSort, nowMs: Long = System.currentTimeMillis()): List<StreamSearchItem> {
        if (sort == SearchSort.RELEVANCE) return items
        val keyed = items.map { it to RelativeTime.estimateAtMs(it, nowMs) }
        val known = keyed.filter { it.second != null }
        val unknown = keyed.filter { it.second == null }.map { it.first }
        val ordered = if (sort == SearchSort.NEWEST) known.sortedByDescending { it.second } else known.sortedBy { it.second }
        return ordered.map { it.first } + unknown
    }

    /** Saring dalam hasil: cocok bila [query] ada di judul atau nama kanal (tanpa peduli huruf besar/kecil). */
    fun matches(item: StreamSearchItem, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return item.title.contains(q, ignoreCase = true) || item.uploader?.contains(q, ignoreCase = true) == true
    }

    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private val STOP = setOf(
        "yang", "dan", "di", "ke", "dari", "untuk", "dengan", "ini", "itu", "atau", "pada", "dalam", "juga", "para",
        "the", "and", "for", "with", "from", "you", "your", "this", "that", "are", "was", "feat", "ft", "vs",
        "official", "video", "audio", "lyric", "lyrics", "lirik", "full", "clip",
    )

    private fun tokens(s: String): List<String> = WORD.findAll(s.lowercase()).map { it.value }.toList()

    /**
     * Saran pencarian terkait dari judul hasil, tanpa jaringan: kata atau dua kata berurutan yang ada di >= [minCount]
     * judul, selain kata kueri dan kata umum. Hasil berupa kueri utuh ("kueri + frasa"), paling sering dulu,
     * dua kata didahulukan bila sama banyak. Kata yang sudah tercakup frasa dua kata dengan jumlah sama dilewati.
     */
    fun relatedQueries(items: List<StreamSearchItem>, query: String, limit: Int = 4, minCount: Int = 2): List<String> {
        val base = query.trim()
        val qTokens = tokens(base).toSet()
        fun usable(w: String) = w.length >= 3 && !w.all { it.isDigit() } && w !in STOP && w !in qTokens
        val counts = LinkedHashMap<String, Int>()
        for (item in items) {
            val toks = tokens(item.title)
            val seen = LinkedHashSet<String>()
            for (i in toks.indices) {
                if (usable(toks[i])) seen.add(toks[i])
                if (i + 1 < toks.size && usable(toks[i]) && usable(toks[i + 1])) seen.add(toks[i] + " " + toks[i + 1])
            }
            for (s in seen) counts[s] = (counts[s] ?: 0) + 1
        }
        val ranked = counts.entries.filter { it.value >= minCount }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { if (it.key.contains(' ')) 0 else 1 })
        val chosen = ArrayList<Map.Entry<String, Int>>()
        for (e in ranked) {
            if (chosen.size >= limit) break
            val covered = !e.key.contains(' ') && chosen.any { it.key.contains(' ') && it.value == e.value && it.key.split(' ').contains(e.key) }
            if (!covered) chosen.add(e)
        }
        return chosen.map { if (base.isEmpty()) it.key else base + " " + it.key }
    }

    /** Kanal yang muncul pada >= [minCount] hasil, terbanyak dulu (urutan kemunculan untuk yang sama), maks [limit]. */
    fun related(items: List<StreamSearchItem>, limit: Int = 3, minCount: Int = 2): List<Pair<String, Int>> {
        val counts = LinkedHashMap<String, Int>()
        for (i in items) {
            val name = i.uploader?.trim().orEmpty()
            if (name.isNotEmpty()) counts[name] = (counts[name] ?: 0) + 1
        }
        return counts.entries
            .filter { it.value >= minCount }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
}
