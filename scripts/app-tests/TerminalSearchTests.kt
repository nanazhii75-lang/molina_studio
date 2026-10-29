import com.molina.studio.terminal.TerminalSearch

private var fails = 0
private var runs = 0
private fun <T> eq(name: String, got: T, want: T) { runs++; if (got != want) { fails++; println("GAGAL: $name -> $got (seharusnya $want)") } }
private fun check(name: String, ok: Boolean) { runs++; if (!ok) { fails++; println("GAGAL: $name") } }

fun main() {
    eq("kueri kosong", TerminalSearch.find("abc", "").size, 0)
    eq("kueri spasi saja", TerminalSearch.find("abc", "   ").size, 0)
    eq("transkrip kosong", TerminalSearch.find("", "abc").size, 0)
    eq("max nol", TerminalSearch.find("abc", "a", 0).size, 0)
    eq("tak cocok", TerminalSearch.find("hello\nworld", "xyz").size, 0)

    val t = "user@host:~$ make\nerror: Foo bar\nok\nFOO again"
    val h = TerminalSearch.find(t, "foo")
    eq("tanpa huruf besar-kecil jumlah", h.size, 2)
    eq("terbaru di atas: baris 4 dulu", h[0].line, 4)
    eq("lalu baris 2", h[1].line, 2)
    eq("rentang kecocokan baris 2", h[1].snippet.substring(h[1].start, h[1].end), "Foo")

    eq("kueri dipangkas", TerminalSearch.find(t, "  ok  ")[0].line, 3)
    eq("titik literal, bukan regex", TerminalSearch.find("axb\na.b", "a.b").size, 1)
    eq("kurung literal", TerminalSearch.find("f(x)\nfx", "(x)").size, 1)
    eq("satu hasil per baris", TerminalSearch.find("foo foo foo", "foo").size, 1)
    eq("spasi di akhir baris diabaikan", TerminalSearch.find("abc   \n", "abc")[0].snippet, "abc")

    val limit = (1..10).joinToString("\n") { "match $it" }
    eq("batas max", TerminalSearch.find(limit, "match", 3).map { it.line }, listOf(10, 9, 8))

    val long = "x".repeat(500) + "NEEDLE" + "y".repeat(500)
    val lh = TerminalSearch.find(long, "needle")[0]
    check("baris panjang dipotong", lh.snippet.length < 300)
    check("kecocokan tetap di rentang", lh.snippet.substring(lh.start, lh.end).equals("NEEDLE", ignoreCase = true))
    check("ada elipsis kiri dan kanan", lh.snippet.startsWith("\u2026") && lh.snippet.endsWith("\u2026"))

    val edge = "NEEDLE" + "y".repeat(500)
    val eh = TerminalSearch.find(edge, "needle")[0]
    check("kecocokan di awal: tanpa elipsis kiri", !eh.snippet.startsWith("\u2026"))
    eq("kecocokan di awal tetap benar", eh.snippet.substring(eh.start, eh.end), "NEEDLE")

    eq("kueri dipotong ke 200", TerminalSearch.find("a".repeat(300) + "\n" + "a".repeat(200), "a".repeat(300))[0].line, 2)

    println("Uji TerminalSearch: $runs pemeriksaan, $fails gagal")
    if (fails > 0) kotlin.system.exitProcess(1)
}
