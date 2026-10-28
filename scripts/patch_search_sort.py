import os, sys
ROOT = os.getcwd()
A = ROOT + "/app/src/main/java/com/molina/studio/cinema"
C = ROOT + "/core/common/src/main/kotlin/com/molina/core/common"
T = ROOT + "/scripts/core-tests"
RES, HOME, RT = A + "/CinemaResults.kt", A + "/CinemaHome.kt", C + "/RelativeTime.kt"
for p in (RES, HOME, RT):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + os.path.basename(p) + ")")
fails = []

def rd(p):
    return open(p, encoding="utf-8").read()

def sub(p, old, new, marker):
    s = rd(p)
    if marker in s:
        print("SKIP  ", os.path.basename(p), "[" + marker[:40] + "]"); return
    if s.count(old) != 1:
        print("GAGAL ", os.path.basename(p), "pola tidak unik/tidak ada:", old[:60].replace("\n", " "))
        fails.append(p); return
    open(p, "w", encoding="utf-8").write(s.replace(old, new))
    print("UBAH  ", os.path.basename(p), "[" + marker[:40] + "]")

def newfile(p, content, marker, mode=None):
    if os.path.isfile(p) and marker in rd(p):
        print("SKIP  ", os.path.basename(p), "[berkas ada]"); return
    open(p, "w", encoding="utf-8").write(content)
    if mode: os.chmod(p, mode)
    print("BUAT  ", os.path.basename(p))

# ===== :core:common =====
newfile(C + "/SearchOrder.kt", r'''package com.molina.core.common

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
''', "object SearchOrder")

sub(RT, '''    /** Label untuk satu hasil, atau null bila waktu unggah tidak diketahui. */''',
'''    /**
     * Perkiraan waktu unggah (epoch ms) untuk pengurutan: uploadedAtMs bila ada, kalau tidak dari teks
     * "3 days ago" (kasar: bulan = 30 hari, tahun = 365 hari). null = tidak diketahui.
     */
    fun estimateAtMs(item: StreamSearchItem, nowMs: Long = System.currentTimeMillis()): Long? {
        item.uploadedAtMs?.let { return it }
        val m = EN_AGO.find(item.uploadedText.orEmpty()) ?: return null
        val n = m.groupValues[1].toLongOrNull() ?: return null
        val unit = when (m.groupValues[2].lowercase()) {
            "second" -> 1_000L
            "minute" -> MIN
            "hour" -> HOUR
            "day" -> DAY
            "week" -> 7 * DAY
            "month" -> 30 * DAY
            else -> 365 * DAY
        }
        return nowMs - n * unit
    }

    /** Label untuk satu hasil, atau null bila waktu unggah tidak diketahui. */''',
    "fun estimateAtMs(")

newfile(T + "/SearchOrderTests.kt", r'''import com.molina.core.common.RelativeTime
import com.molina.core.common.SearchOrder
import com.molina.core.common.SearchSort
import com.molina.core.common.StreamSearchItem

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

private const val NOW = 1_000_000_000_000L
private const val DAY = 86_400_000L

private fun item(t: String, at: Long? = null, text: String? = null, up: String? = null) =
    StreamSearchItem("https://example.com/$t", t, up, null, null, at, text)

private fun titles(l: List<StreamSearchItem>) = l.joinToString(",") { it.title }

fun main() {
    check("estimate: uploadedAtMs didahulukan", RelativeTime.estimateAtMs(item("a", at = 5L, text = "3 days ago"), NOW) == 5L)
    check("estimate: 3 days ago", RelativeTime.estimateAtMs(item("a", text = "3 days ago"), NOW) == NOW - 3 * DAY)
    check("estimate: Streamed 2 weeks ago", RelativeTime.estimateAtMs(item("a", text = "Streamed 2 weeks ago"), NOW) == NOW - 14 * DAY)
    check("estimate: 1 year ago", RelativeTime.estimateAtMs(item("a", text = "1 year ago"), NOW) == NOW - 365 * DAY)
    check("estimate: teks tak dikenal -> null", RelativeTime.estimateAtMs(item("a", text = "Live"), NOW) == null)
    check("estimate: kosong -> null", RelativeTime.estimateAtMs(item("a"), NOW) == null)

    val a = item("A", text = "3 years ago")
    val b = item("B", at = NOW - DAY)
    val c = item("C")
    val d = item("D", text = "2 hours ago")
    val src = listOf(a, b, c, d)
    check("NEWEST: baru ke lama, tanpa tanggal di akhir", titles(SearchOrder.sort(src, SearchSort.NEWEST, NOW)) == "D,B,A,C")
    check("OLDEST: lama ke baru, tanpa tanggal di akhir", titles(SearchOrder.sort(src, SearchSort.OLDEST, NOW)) == "A,B,D,C")
    check("RELEVANCE: urutan asli", titles(SearchOrder.sort(src, SearchSort.RELEVANCE, NOW)) == "A,B,C,D")
    check("sumber tidak diubah", titles(src) == "A,B,C,D")

    val t1 = item("T1", text = "3 days ago"); val t2 = item("T2", text = "3 days ago"); val t3 = item("T3", text = "3 days ago")
    check("stabil: sewaktu mempertahankan urutan", titles(SearchOrder.sort(listOf(t1, t2, t3), SearchSort.NEWEST, NOW)) == "T1,T2,T3")
    val m1 = item("M1", at = NOW - 5 * DAY); val m2 = item("M2", text = "3 days ago")
    check("campuran ms dan teks", titles(SearchOrder.sort(listOf(m1, m2), SearchSort.NEWEST, NOW)) == "M2,M1")
    check("daftar kosong", SearchOrder.sort(emptyList(), SearchSort.NEWEST, NOW).isEmpty())

    val x = item("Satu", up = "Kanal X"); val y = item("Dua", up = "kanal x"); val z = item("Tiga", up = null)
    check("matches: judul", SearchOrder.matches(x, "sat"))
    check("matches: kanal", SearchOrder.matches(x, "kanal x"))
    check("matches: tidak cocok", !SearchOrder.matches(x, "zzz"))
    check("matches: kueri kosong", SearchOrder.matches(z, "  "))
    check("matches: tanpa kanal tidak error", !SearchOrder.matches(z, "kanal"))

    val r = listOf(item("1", up = "X"), item("2", up = "Y"), item("3", up = "X"), item("4", up = "Z"),
        item("5", up = "Y"), item("6", up = "X"), item("7", up = " "), item("8"))
    check("related: kanal terbanyak, min 2", SearchOrder.related(r) == listOf("X" to 3, "Y" to 2))
    check("related: batas jumlah", SearchOrder.related(r, limit = 1) == listOf("X" to 3))
    check("related: kosong bila tak ada pengulangan", SearchOrder.related(listOf(item("1", up = "A"), item("2", up = "B"))).isEmpty())

    println(if (fails == 0) "\nSEMUA LOLOS" else "\n$fails GAGAL")
    if (fails > 0) System.exit(1)
}
''', "SearchOrder.related")

newfile(T + "/run_search_order.sh", r'''#!/usr/bin/env bash
# Uji JVM murni untuk SearchOrder (urutan waktu, saring, kanal terkait) di :core:common.
# Butuh kotlinc (>= 2.1) di PATH atau KOTLINC=/jalur/kotlinc.
set -euo pipefail
cd "$(dirname "$0")/../.."
KC="${KOTLINC:-kotlinc}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
C=core/common/src/main/kotlin/com/molina/core/common
"$KC" -nowarn -d "$OUT" $C/SearchOrder.kt $C/RelativeTime.kt $C/StreamResolver.kt scripts/core-tests/SearchOrderTests.kt
java -cp "$OUT:$(dirname "$(command -v "$KC")")/../lib/kotlin-stdlib.jar" SearchOrderTestsKt
''', "SearchOrderTestsKt", 0o755)

# ===== CinemaResults: urutan waktu, saring judul+kanal, chip kanal terkait =====
sub(RES, "import androidx.compose.foundation.clickable\n",
    "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.horizontalScroll\n",
    "import androidx.compose.foundation.horizontalScroll")
sub(RES, "import androidx.compose.foundation.lazy.rememberLazyListState\n",
    "import androidx.compose.foundation.lazy.rememberLazyListState\nimport androidx.compose.foundation.rememberScrollState\n",
    "import androidx.compose.foundation.rememberScrollState")
sub(RES, "import com.molina.core.common.RelativeTime\n",
    "import com.molina.core.common.RelativeTime\nimport com.molina.core.common.SearchOrder\nimport com.molina.core.common.SearchSort\n",
    "import com.molina.core.common.SearchOrder")
sub(RES, "    onPick: (StreamSearchItem) -> Unit,\n",
    "    onPick: (List<StreamSearchItem>, StreamSearchItem) -> Unit,\n",
    "onPick: (List<StreamSearchItem>, StreamSearchItem) -> Unit")
sub(RES, "    val entries = remember(results) { results.mapIndexed { i, r -> ResultEntry(i + 1, r) } }\n",
    "    var sort by remember { mutableStateOf(SearchSort.NEWEST) } // [RESULTS-SORT] bawaan: terbaru dulu\n"
    "    val ordered = remember(results, sort) { SearchOrder.sort(results, sort) }\n"
    "    val entries = remember(ordered) { ordered.mapIndexed { i, r -> ResultEntry(i + 1, r) } }\n"
    "    val related = remember(results) { SearchOrder.related(results) }\n"
    "    val undated = remember(results) { results.count { RelativeTime.estimateAtMs(it) == null } }\n",
    "[RESULTS-SORT] bawaan")
sub(RES, "    LaunchedEffect(searchOpen) { if (searchOpen) focus.requestFocus() }\n",
    "    LaunchedEffect(searchOpen) { if (searchOpen) focus.requestFocus() }\n"
    "    LaunchedEffect(sort) { selected = 1; listState.scrollToItem(0) } // ganti urutan: kembali ke nomor 001\n",
    "LaunchedEffect(sort)")
sub(RES, "        else entries.filter { it.item.title.contains(q, ignoreCase = true) }\n",
    "        else entries.filter { SearchOrder.matches(it.item, q) }\n",
    "SearchOrder.matches(it.item, q)")
sub(RES, "if (t != null) onPick(t.item)", "if (t != null) onPick(ordered, t.item)", "onPick(ordered, t.item)")
sub(RES, "onClick = { onPick(e.item) },", "onClick = { onPick(ordered, e.item) },", "onPick(ordered, e.item)")
sub(RES, "                if (searchOpen) OutlinedTextField(\n",
    r'''                // [RESULTS-SORT] urutan waktu unggah + kanal terkait (ketuk = saring daftar dengan nama kanal)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SearchSort.values().forEach { s -> ResultChip(s.label, s == sort) { sort = s } }
                }
                if (sort != SearchSort.RELEVANCE && undated > 0) Text(
                    "$undated hasil tanpa tanggal ditaruh di akhir", color = MolinaTheme.Muted, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
                if (related.isNotEmpty()) Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Terkait", color = MolinaTheme.Muted, fontSize = 12.sp)
                    related.forEach { (name, n) ->
                        val on = q.equals(name, ignoreCase = true)
                        ResultChip("$name \u00B7 $n", on) {
                            if (on) { query = ""; searchOpen = false } else { query = name; searchOpen = true }
                        }
                    }
                }

                if (searchOpen) OutlinedTextField(
''', "// [RESULTS-SORT] urutan waktu")
sub(RES, "Cari nama atau ketik nomor channel", "Cari judul, kanal, atau nomor", "Cari judul, kanal, atau nomor")

s = rd(RES)
if "private fun ResultChip" in s:
    print("SKIP   ResultChip")
else:
    open(RES, "a", encoding="utf-8").write(r'''
/** [RESULTS-SORT] Chip kecil: aktif = emas penuh, tidak aktif = garis tipis. */
@Composable
private fun ResultChip(label: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        label,
        color = if (active) Color.Black else Color.White,
        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.widthIn(max = 180.dp).clip(shape)
            .background(if (active) MolinaTheme.Gold else Color(0x14FFFFFF))
            .border(1.dp, if (active) MolinaTheme.Gold else RowLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
''')
    print("UBAH   ResultChip ditambahkan")

# ===== CinemaHome: playlist pemutar mengikuti urutan yang tampil =====
sub(HOME, r'''        onPick = { r ->
            showResults = false
            val d = CinemaActions.classify(r.pageUrl)
            info = if (d is CinemaActions.Input.Direct && CinemaActions.playResults(ctx, results, r)) "" else FAIL''',
    r'''        onPick = { list, r ->
            showResults = false
            val d = CinemaActions.classify(r.pageUrl)
            info = if (d is CinemaActions.Input.Direct && CinemaActions.playResults(ctx, list, r)) "" else FAIL''',
    "onPick = { list, r ->")

# ===== progres.md =====
pg = ROOT + "/progres.md"
entry = """
## 2026-10-28 - Hasil pencarian Cinema: urut waktu, saring judul+kanal, kanal terkait [SEARCH-SORT-RELATED]
- [x] feat(core): `SearchOrder` (:core:common, JDK murni) + `SearchSort` {Relevansi, Terbaru, Terlama}; `RelativeTime.estimateAtMs` memperkirakan waktu unggah dari uploadedAtMs atau teks "3 days ago" (bulan 30 hari, tahun 365 hari). Pengurutan stabil, hasil tanpa tanggal selalu di akhir
- [x] feat(cinema): dialog Hasil pencarian punya chip Relevansi/Terbaru/Terlama (bawaan Terbaru), nomor 001.. mengikuti urutan tampil, catatan "N hasil tanpa tanggal ditaruh di akhir"
- [x] feat(cinema): kolom Cari kini menyaring judul DAN nama kanal; baris chip "Terkait" menampilkan kanal yang muncul >= 2 kali, ketuk = saring daftar dengan kanal itu, ketuk lagi = lepas
- [x] Playlist pemutar mpv kini mengikuti urutan yang tampil di dialog (`onPick(list, item)` -> `CinemaActions.playResults(ctx, list, r)`), jadi dialog playlist di pemutar ikut terurut
- [x] Uji JVM: scripts/core-tests/run_search_order.sh (21 pengecekan: estimasi waktu, urut baru/lama, stabil, tanpa tanggal di akhir, saring, kanal terkait)
- [x] Dampak: :core:common (+SearchOrder.kt, RelativeTime.kt), :app cinema (CinemaResults, CinemaHome). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah-sedang (`related.forEach { (name, n) -> }`, `SearchSort.values()`, `horizontalScroll(rememberScrollState())` di dalam `if`)
- [ ] uji perangkat (Android 8): cari kata di beranda > chip Terbaru aktif, video terbaru di nomor 001; ketuk Terlama > urutan terbalik; ketuk chip kanal > daftar tersaring; putar item > dialog playlist di pemutar berurutan sama
- [ ] KETERBATASAN: sumber yt-dlp mode flat sering tanpa tanggal, hasilnya masuk "tanpa tanggal" di akhir; tanggal teks YouTube ("3 weeks ago") kasar sehingga hasil sewaktu mengikuti urutan relevansi. Pencarian di dialog playlist PEMUTAR (layout XML) belum diberi chip urutan/kanal
"""
if os.path.isfile(pg):
    s = rd(pg)
    if "[SEARCH-SORT-RELATED]" in s:
        print("SKIP   progres.md")
    else:
        open(pg, "a", encoding="utf-8").write(("" if s.endswith("\n") else "\n") + entry)
        print("UBAH   progres.md")
else:
    print("GAGAL  progres.md tidak ada"); fails.append(pg)

print("\nSELESAI dengan", len(fails), "kegagalan")
sys.exit(1 if fails else 0)
