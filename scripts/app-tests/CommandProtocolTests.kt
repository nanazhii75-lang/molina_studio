import com.molina.studio.cmd.CommandProtocol
import com.molina.studio.cmd.CommandProtocol.Parsed
import com.molina.studio.cmd.CommandProtocol.Verb

private var fails = 0
private var runs = 0
private fun check(name: String, ok: Boolean) { runs++; if (!ok) { fails++; println("GAGAL: $name") } }
private fun <T> eq(name: String, got: T, want: T) { runs++; if (got != want) { fails++; println("GAGAL: $name -> $got (seharusnya $want)") } }

private fun okReq(line: String): CommandProtocol.Request? = (CommandProtocol.parse(line) as? Parsed.Ok)?.req
private fun badCode(line: String): String? = (CommandProtocol.parse(line) as? Parsed.Bad)?.code

fun main() {
    // parse: perintah tanpa argumen
    eq("ping", okReq("ping\n")?.verb, Verb.PING)
    eq("version", okReq("version")?.verb, Verb.VERSION)
    eq("status CRLF", okReq("status\r\n")?.verb, Verb.STATUS)
    eq("stop", okReq("stop\n")?.verb, Verb.STOP)
    eq("huruf besar", okReq("PING\n")?.verb, Verb.PING)
    eq("ping tanpa arg", okReq("ping\n")?.arg, "")

    // parse: play dengan argumen (spasi di dalam path tetap utuh)
    eq("play verb", okReq("play\t/sdcard/a b.mkv\n")?.verb, Verb.PLAY)
    eq("play arg spasi", okReq("play\t/sdcard/a b.mkv\n")?.arg, "/sdcard/a b.mkv")
    eq("play arg dipangkas", okReq("play\t  /sdcard/x.mp4  \n")?.arg, "/sdcard/x.mp4")
    eq("play url", okReq("play\thttps://example.org/v?a=1\n")?.arg, "https://example.org/v?a=1")
    eq("play TAB di arg dipertahankan", okReq("play\t/sdcard/a\tb\n")?.arg, "/sdcard/a\tb")

    // parse: galat
    eq("kosong", badCode(""), "empty")
    eq("hanya newline", badCode("\n"), "empty")
    eq("spasi saja", badCode("   \n"), "empty")
    eq("play tanpa arg", badCode("play\n"), "usage")
    eq("play arg kosong", badCode("play\t \n"), "usage")
    eq("tidak dikenal", badCode("joget\n"), "unknown")
    eq("pemisah spasi bukan TAB", badCode("play /sdcard/a.mp4\n"), "unknown")

    // balasan: satu baris, tanpa TAB/CR/LF, dibatasi
    eq("ok kosong", CommandProtocol.ok(), "OK\n")
    eq("ok teks", CommandProtocol.ok("pong"), "OK pong\n")
    eq("ok newline disapu", CommandProtocol.ok("a\nb\tc\rd"), "OK a b c d\n")
    eq("err", CommandProtocol.err("path", "tidak valid"), "ERR path tidak valid\n")
    check("err sebaris", CommandProtocol.err("x", "a\nb").count { it == '\n' } == 1)
    check("oneLine batas 300", CommandProtocol.oneLine("x".repeat(1000)).length == 300)

    // guestToHost: pemetaan sah
    val root = "/storage/emulated/0"
    eq("sdcard akar", CommandProtocol.guestToHost("/sdcard", root), root)
    eq("storage akar", CommandProtocol.guestToHost("/storage/emulated/0", root), root)
    eq("sdcard berkas", CommandProtocol.guestToHost("/sdcard/Molina/Media/Video/a.mkv", root), "$root/Molina/Media/Video/a.mkv")
    eq("storage berkas", CommandProtocol.guestToHost("/storage/emulated/0/DCIM/x.mp4", root), "$root/DCIM/x.mp4")
    eq("garis ganda dan titik", CommandProtocol.guestToHost("/sdcard//Molina/./a.mp4", root), "$root/Molina/a.mp4")
    eq("garis miring akhir", CommandProtocol.guestToHost("/sdcard/Molina/", root), "$root/Molina")
    eq("root berakhiran slash", CommandProtocol.guestToHost("/sdcard/a.mp4", "$root/"), "$root/a.mp4")
    eq("spasi di nama", CommandProtocol.guestToHost("/sdcard/Film Baru/a b.mkv", root), "$root/Film Baru/a b.mkv")
    eq("root alternatif", CommandProtocol.guestToHost("/sdcard/a.mp4", "/sdcard"), "/sdcard/a.mp4")

    // guestToHost: penolakan
    eq("relatif", CommandProtocol.guestToHost("film.mp4", root), null)
    eq("kosong", CommandProtocol.guestToHost("", root), null)
    eq("titik-titik di tengah", CommandProtocol.guestToHost("/sdcard/Molina/../DCIM/a.mp4", root), null)
    eq("titik-titik keluar", CommandProtocol.guestToHost("/sdcard/../data/data/x", root), null)
    eq("akhiran titik-titik", CommandProtocol.guestToHost("/sdcard/..", root), null)
    eq("root guest", CommandProtocol.guestToHost("/root/.bashrc", root), null)
    eq("etc", CommandProtocol.guestToHost("/etc/passwd", root), null)
    eq("data aplikasi", CommandProtocol.guestToHost("/data/data/com.molina.studio/files/x", root), null)
    eq("awalan mirip", CommandProtocol.guestToHost("/sdcardx/a.mp4", root), null)
    eq("awalan mirip 2", CommandProtocol.guestToHost("/storage/emulated/01/a.mp4", root), null)
    eq("kartu SD", CommandProtocol.guestToHost("/storage/1234-ABCD/a.mp4", root), null)
    eq("NUL", CommandProtocol.guestToHost("/sdcard/a\u0000.mp4", root), null)
    eq("nama berawalan titik tetap sah", CommandProtocol.guestToHost("/sdcard/.hidden/a.mp4", root), "$root/.hidden/a.mp4")
    eq("nama dengan dua titik di dalamnya sah", CommandProtocol.guestToHost("/sdcard/a..b/c.mp4", root), "$root/a..b/c.mp4")

    // open: parse dan pemisah path
    eq("open verb", okReq("open\t/sdcard/a.py\n")?.verb, Verb.OPEN)
    eq("open arg", okReq("open\t/sdcard/a.py\n")?.arg, "/sdcard/a.py")
    eq("open tanpa arg", badCode("open\n"), "usage")
    eq("splitPaths dua", CommandProtocol.splitPaths("/sdcard/a b.py\t/sdcard/c.py"), listOf("/sdcard/a b.py", "/sdcard/c.py"))
    eq("splitPaths kosong", CommandProtocol.splitPaths(""), emptyList<String>())
    eq("splitPaths TAB saja", CommandProtocol.splitPaths("\t\t"), emptyList<String>())
    eq("splitPaths dipangkas", CommandProtocol.splitPaths("  /sdcard/a  "), listOf("/sdcard/a"))
    check("MAX_OPEN", CommandProtocol.MAX_OPEN == 10)

    check("konstanta", CommandProtocol.SOCKET_NAME == "molina.cmd" && CommandProtocol.VERSION == 1)
    check("verb unik", Verb.entries.map { it.wire }.toSet().size == Verb.entries.size)

    println(if (fails == 0) "LULUS: $runs kasus" else "$fails dari $runs kasus GAGAL")
    if (fails != 0) System.exit(1)
}
