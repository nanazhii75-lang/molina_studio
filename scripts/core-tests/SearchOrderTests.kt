import com.molina.core.common.RelativeTime
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
