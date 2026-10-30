package com.molina.core.common

import java.io.File

/**
 * Model kartu poster untuk rak film Molina Cinema (SIDE-3). Murni JVM supaya bisa diuji:
 * menggabungkan riwayat putar (CinemaRecent) dengan pendamping dari `molina_dl --sidecar`
 * (MediaSidecar). Semua fungsi tahan berkas hilang/rusak dan tidak melempar.
 */
object ShelfCard {
    /** Masukan satu entri riwayat: path lokal atau URL, judul tersimpan, dan jenis media. */
    class Entry(val ref: String, val title: String, val video: Boolean)

    /**
     * @property thumbPath path video lokal yang dimuat W7Thumb (poster sidecar diprioritaskan di sana); null untuk URL/audio.
     * @property hasPoster true bila ada poster sidecar, UI memberi lencana kecil dan tidak memakai monogram.
     */
    data class Card(
        val ref: String,
        val title: String,
        val subtitle: String,
        val duration: String?,
        val video: Boolean,
        val thumbPath: String?,
        val hasPoster: Boolean,
        val monogram: String,
    )

    const val MAX_CARDS = 8

    fun build(entries: List<Entry>, max: Int = MAX_CARDS): List<Card> =
        entries.asSequence().filter { it.ref.isNotBlank() }.take(max.coerceAtLeast(0)).map { of(it) }.toList()

    fun of(e: Entry): Card {
        val local = e.ref.startsWith("/")
        val file = if (local) File(e.ref) else null
        val found = if (file != null && e.video) MediaSidecar.find(file) else null
        val info = MediaSidecar.readInfo(found?.info)
        val title = info?.title ?: e.title.ifBlank { fallbackTitle(e.ref) }
        val parts = listOfNotNull(info?.channel, info?.year?.toString()).ifEmpty { listOfNotNull(origin(e.ref)) }
        return Card(
            ref = e.ref,
            title = title,
            subtitle = parts.joinToString(" \u00B7 "),
            duration = info?.durationSec?.let { clock(it) },
            video = e.video,
            thumbPath = if (local && e.video) e.ref else null,
            hasPoster = found?.poster != null,
            monogram = monogram(title),
        )
    }

    /** 75 -> "1:15", 3725 -> "1:02:05"; <= 0 -> "0:00". */
    fun clock(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        val h = s / 3600
        val m = s % 3600 / 60
        val r = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, r) else "%d:%02d".format(m, r)
    }

    /** Huruf pertama judul (huruf besar) untuk placeholder poster; "M" bila tidak ada huruf/angka. */
    fun monogram(title: String): String {
        val cp = title.codePoints().toArray().firstOrNull { Character.isLetterOrDigit(it) } ?: return "M"
        return String(Character.toChars(cp)).uppercase()
    }

    private fun origin(ref: String): String? =
        if (ref.startsWith("/")) File(ref).parentFile?.name?.takeIf { it.isNotEmpty() }
        else runCatching { java.net.URI(ref).host }.getOrNull()?.takeIf { it.isNotEmpty() }

    private fun fallbackTitle(ref: String): String =
        if (ref.startsWith("/")) File(ref).name else (origin(ref) ?: ref)
}
