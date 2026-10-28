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

    /** Kanal yang muncul pada >= [minCount] hasil, terbanyak dulu (urutan kemunculan untuk yang sama), maks [limit]. */
    fun related(items: List<StreamSearchItem>, limit: Int = 4, minCount: Int = 2): List<Pair<String, Int>> {
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
