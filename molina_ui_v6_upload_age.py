#!/usr/bin/env python3
"""Molina Cinema v6: keterangan waktu unggah ('3 hari lalu') pada hasil pencarian YouTube di Home dan player. Idempotent.
Prasyarat: v3 dan v4 sudah dijalankan."""
import os, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
COMMON = os.path.join(ROOT, "core/common/src/main/kotlin/com/molina/core/common")
NP = os.path.join(ROOT, "engine/newpipe/src/main/kotlin/com/molina/engine/newpipe/NewPipeStreamResolver.kt")
RESULTS = os.path.join(ROOT, "app/src/main/java/com/molina/studio/cinema/CinemaResults.kt")
MA = os.path.join(ROOT, "feature/mpv/src/main/java/com/molina/cinema/MPVActivity.kt")
for p in (COMMON, NP, RESULTS, MA):
    if not os.path.exists(p): sys.exit("Path tidak ditemukan: %s (jalankan di root repo; v3 dan v4 harus sudah jalan)" % p)

def rd(p): return open(p, encoding="utf-8").read()
def patch(path, old, new, tag, marker=None):
    t = rd(path)
    if (marker or new) in t: print("skip  ", tag); return
    if t.count(old) != 1: sys.exit("Pola tidak ditemukan/ganda (%s): %s" % (tag, old))
    open(path, "w", encoding="utf-8").write(t.replace(old, new)); print("patch ", tag)

# 1) model
patch(os.path.join(COMMON, "StreamResolver.kt"),
"""    val durationSec: Long? = null,
    val thumbnailUrl: String? = null,
)""",
"""    val durationSec: Long? = null,
    val thumbnailUrl: String? = null,
    /** Waktu unggah (epoch ms); null = tidak diketahui (mis. siaran langsung). Untuk YouTube berupa perkiraan. */
    val uploadedAtMs: Long? = null,
    /** Teks waktu unggah mentah dari sumber (mis. \"2 days ago\"), dipakai bila [uploadedAtMs] kosong. */
    val uploadedText: String? = null,
)""", "StreamSearchItem.uploadedAt", marker="val uploadedAtMs: Long? = null,")

# 2) formatter (Indonesia)
rt = os.path.join(COMMON, "RelativeTime.kt")
RT = '''package com.molina.core.common

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

    private val EN_AGO = Regex("""(\\d+)\\s+(second|minute|hour|day|week|month|year)s?\\s+ago""", RegexOption.IGNORE_CASE)

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
'''
if os.path.exists(rt) and rd(rt) == RT: print("skip   RelativeTime.kt")
else: open(rt, "w", encoding="utf-8").write(RT); print("tulis  RelativeTime.kt")

# 3) NewPipe
patch(NP,
"""                        thumbnailUrl = i.thumbnails.orEmpty().maxByOrNull { it.height }?.url,
                    )""",
"""                        thumbnailUrl = i.thumbnails.orEmpty().maxByOrNull { it.height }?.url,
                        uploadedAtMs = i.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli(),
                        uploadedText = i.textualUploadDate?.takeIf { it.isNotBlank() },
                    )""", "NewPipe uploadedAt", marker="uploadedAtMs = i.uploadDate")

# 4) yt-dlp
patch(os.path.join(COMMON, "YtdlpSearch.kt"),
"""        val item = StreamSearchItem(
            pageUrl = page,
            title = title,
            uploader = txt(o["channel"]) ?: txt(o["uploader"]),
            durationSec = (o["duration"] as? Number)?.toLong()?.takeIf { it > 0 },
            thumbnailUrl = thumb(o["thumbnails"]),
        )
        return Hit(item, id, secs?.times(1000) ?: day(txt(o["upload_date"])))""",
"""        val at = secs?.times(1000) ?: day(txt(o["upload_date"]))
        val item = StreamSearchItem(
            pageUrl = page,
            title = title,
            uploader = txt(o["channel"]) ?: txt(o["uploader"]),
            durationSec = (o["duration"] as? Number)?.toLong()?.takeIf { it > 0 },
            thumbnailUrl = thumb(o["thumbnails"]),
            uploadedAtMs = at,
        )
        return Hit(item, id, at)""", "yt-dlp uploadedAt", marker="uploadedAtMs = at,")

# 5) Home (Compose)
patch(RESULTS,
"""listOfNotNull(e.item.uploader, e.item.durationSec?.let { duration(it) })""",
"""listOfNotNull(e.item.uploader, e.item.durationSec?.let { duration(it) }, RelativeTime.label(e.item))""",
"Home meta", marker="RelativeTime.label(e.item)")
patch(RESULTS, "import com.molina.core.common.StreamSearchItem\n",
      "import com.molina.core.common.RelativeTime\nimport com.molina.core.common.StreamSearchItem\n",
      "Home import", marker="import com.molina.core.common.RelativeTime")

# 6) Player
patch(MA,
r'''        return parts.joinToString(" \u00B7 ")''',
r'''        com.molina.core.common.RelativeTime.label(item)?.let { parts.add(it) }
        return parts.joinToString(" \u00B7 ")''',
"Player meta", marker="com.molina.core.common.RelativeTime.label(item)")

pg = os.path.join(ROOT, "progres.md")
if os.path.exists(pg):
    p = rd(pg)
    if "Cinema UI v6" not in p:
        open(pg, "w", encoding="utf-8").write(p.rstrip("\n") + """

## 2026-10-25 - Cinema UI v6 (umur unggahan di hasil pencarian)
- [x] feat(search): StreamSearchItem + uploadedAtMs/uploadedText; NewPipe mengisi dari uploadDate (+ teks mentah), yt-dlp dari timestamp/upload_date; RelativeTime.label membuat teks Indonesia ('3 hari lalu', '2 tahun lalu'; teks 'N days ago' diterjemahkan); tampil di baris meta hasil pencarian Home dan player
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji: DateWrapper.offsetDateTime() pada NewPipeExtractor v0.26.5 (jika gagal kompilasi, ganti ke date()/teks mentah), YouTube hanya memberi umur perkiraan sehingga bulan/tahun kasar; siaran langsung tanpa label
- [ ] ide berikutnya: urutkan Terbaru (sort) pada dialog hasil, penanda 'BARU' untuk < 7 hari
""")
        print("patch  progres.md")
    else: print("skip   progres.md")
print("selesai.")
