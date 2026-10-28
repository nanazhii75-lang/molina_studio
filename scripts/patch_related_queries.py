import os, sys
ROOT = os.getcwd()
A = ROOT + "/app/src/main/java/com/molina/studio/cinema"
C = ROOT + "/core/common/src/main/kotlin/com/molina/core/common"
T = ROOT + "/scripts/core-tests"
RES, HOME, SO, TST = A + "/CinemaResults.kt", A + "/CinemaHome.kt", C + "/SearchOrder.kt", T + "/SearchOrderTests.kt"
for p in (RES, HOME):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + os.path.basename(p) + ")")
for p in (SO, TST):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: terapkan dulu patch_search_sort.py (tidak ada " + os.path.basename(p) + ")")
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

# ===== :core:common/SearchOrder: batas kanal 3 + saran kata kunci dari judul hasil =====
sub(SO, "fun related(items: List<StreamSearchItem>, limit: Int = 4, minCount: Int = 2)",
    "fun related(items: List<StreamSearchItem>, limit: Int = 3, minCount: Int = 2)",
    "limit: Int = 3, minCount: Int = 2")
sub(SO, "    /** Kanal yang muncul pada >= [minCount] hasil",
    r'''    private val WORD = Regex("[\\p{L}\\p{N}]+")
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

    /** Kanal yang muncul pada >= [minCount] hasil''',
    "fun relatedQueries(")

# ===== uji JVM =====
sub(TST, r'''    println(if (fails == 0) "\nSEMUA LOLOS" else "\n$fails GAGAL")''',
    r'''    // saran kata kunci terkait
    val titlesQ = listOf("Lagu Pop Terbaru 2026", "Kumpulan Lagu Pop Terbaru", "Lagu Dangdut Koplo", "Lagu Dangdut Koplo Terbaru", "Official Video Lagu Rock")
        .map { t -> StreamSearchItem("https://example.com/" + t.replace(' ', '-'), t, null, null, null, null, null) }
    check("relatedQueries: frasa dan kata, tanpa kata kueri/umum",
        SearchOrder.relatedQueries(titlesQ, "lagu") == listOf("lagu terbaru", "lagu pop terbaru", "lagu dangdut koplo"))
    check("relatedQueries: batas jumlah", SearchOrder.relatedQueries(titlesQ, "lagu", limit = 1) == listOf("lagu terbaru"))
    check("relatedQueries: kueri kosong tanpa awalan", SearchOrder.relatedQueries(titlesQ, "").first() == "lagu")
    check("relatedQueries: tidak ada pengulangan -> kosong",
        SearchOrder.relatedQueries(listOf(StreamSearchItem("https://e/1", "Satu Dua", null, null, null, null, null)), "x").isEmpty())
    check("relatedQueries: daftar kosong", SearchOrder.relatedQueries(emptyList(), "lagu").isEmpty())
    val ch = listOf("A", "B", "C", "D").flatMap { c -> listOf(item(c + "1", up = c), item(c + "2", up = c)) }
    check("related: batas bawaan 3 kanal", SearchOrder.related(ch).size == 3)

    println(if (fails == 0) "\nSEMUA LOLOS" else "\n$fails GAGAL")''',
    "relatedQueries: frasa dan kata")

# ===== CinemaResults: baris "Cari juga" =====
sub(RES, "    onDismiss: () -> Unit,\n) {\n    val ctx = LocalContext.current\n",
    "    onDismiss: () -> Unit,\n    searchQuery: String = \"\",\n    onSearch: ((String) -> Unit)? = null,\n) {\n    val ctx = LocalContext.current\n",
    "onSearch: ((String) -> Unit)? = null")
sub(RES, "    val undated = remember(results) { results.count { RelativeTime.estimateAtMs(it) == null } }\n",
    "    val undated = remember(results) { results.count { RelativeTime.estimateAtMs(it) == null } }\n"
    "    val suggestions = remember(results, searchQuery) { SearchOrder.relatedQueries(results, searchQuery) }\n",
    "val suggestions = remember(results, searchQuery)")
sub(RES, "                if (searchOpen) OutlinedTextField(\n",
    r'''                // [RESULTS-SUGGEST] pencarian terkait: kata kunci dari judul hasil, ketuk = cari lagi
                if (onSearch != null && suggestions.isNotEmpty()) Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Cari juga", color = MolinaTheme.Muted, fontSize = 12.sp)
                    suggestions.forEach { s -> ResultChip(s, false) { onSearch(s) } }
                }

                if (searchOpen) OutlinedTextField(
''', "// [RESULTS-SUGGEST]")

# ===== CinemaHome: simpan kueri terakhir, ketuk saran = cari ulang =====
sub(HOME, "    var results by remember { mutableStateOf<List<StreamSearchItem>>(emptyList()) }\n",
    "    var results by remember { mutableStateOf<List<StreamSearchItem>>(emptyList()) }\n"
    "    var lastQuery by remember { mutableStateOf(\"\") }\n",
    "var lastQuery by remember")
sub(HOME, "                    results = outcome.value\n                    showResults = true\n",
    "                    results = outcome.value\n                    lastQuery = q\n                    showResults = true\n",
    "lastQuery = q")
sub(HOME, "        results = results,\n        onDismiss = { showResults = false },\n",
    "        results = results,\n        onDismiss = { showResults = false },\n"
    "        searchQuery = lastQuery,\n"
    "        onSearch = { s -> input = s; showResults = false; search(s) },\n",
    "onSearch = { s -> input = s")

# ===== progres.md =====
pg = ROOT + "/progres.md"
entry = """
## 2026-10-28 - Hasil pencarian Cinema: saran "Cari juga" + kanal terkait maks 3 [SEARCH-RELATED-QUERIES]
- [x] feat(core): `SearchOrder.relatedQueries(items, query)` menurunkan saran pencarian dari judul hasil (kata/dua kata berurutan yang ada di >= 2 judul, tanpa kata kueri dan kata umum; hasil berupa kueri utuh "kueri + frasa"). Tanpa panggilan jaringan baru
- [x] style(core): `SearchOrder.related` (kanal terkait) batas bawaan 4 -> 3
- [x] feat(cinema): baris chip "Cari juga" di dialog Hasil pencarian; ketuk = isi kolom pencarian beranda dan cari ulang (`onSearch` -> `search(s)`); Home menyimpan `lastQuery`
- [x] Uji JVM: scripts/core-tests/run_search_order.sh bertambah 6 pengecekan (frasa/kata, batas, kueri kosong, tanpa pengulangan, daftar kosong, kanal maks 3)
- [x] Dampak: :core:common (SearchOrder.kt), :app cinema (CinemaResults, CinemaHome). Gradle, Manifest, JNI mpv TIDAK disentuh. Wajib setelah patch_search_sort.py
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah (parameter baru berdefault, smart cast `onSearch` di lambda)
- [ ] uji perangkat (Android 8): cari "lagu" > chip "Cari juga" muncul (mis. "lagu terbaru"); ketuk > hasil dimuat ulang dan kolom beranda terisi; kanal terkait tampil maks 3
- [ ] KETERBATASAN: saran dari judul hasil saja (bukan sugesti mesin pencari), jadi kualitasnya mengikuti judul; kata umum yang belum ada di daftar kata henti bisa muncul
"""
if os.path.isfile(pg):
    s = rd(pg)
    if "[SEARCH-RELATED-QUERIES]" in s:
        print("SKIP   progres.md")
    else:
        open(pg, "a", encoding="utf-8").write(("" if s.endswith("\n") else "\n") + entry)
        print("UBAH   progres.md")
else:
    print("GAGAL  progres.md tidak ada"); fails.append(pg)

print("\nSELESAI dengan", len(fails), "kegagalan")
sys.exit(1 if fails else 0)
