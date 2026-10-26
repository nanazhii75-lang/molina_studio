// Uji JVM murni: DownloadEventParser, DownloadReducer, BundledScriptPolicy.
// Jalankan: bash scripts/core-tests/run.sh
import com.molina.core.common.*
import com.molina.core.common.BundledScriptPolicy.Action

var failed = 0
var total = 0
fun check(name: String, ok: Boolean, detail: () -> String = { "" }) {
    total++
    if (!ok) { failed++; println("GAGAL: $name ${detail()}") }
}
fun <T> eq(name: String, a: T, b: T) = check(name, a == b) { "diharapkan=$b hasil=$a" }

fun p(s: String) = DownloadEventParser.parse(s)

fun main() {
    // --- parser
    eq("hello", p("""{"v":1,"ev":"hello","script":"molina_dl","version":3}"""), DownloadEvent.Hello(3))
    eq("queue", p("""{"v":1,"ev":"queue","total":2}"""), DownloadEvent.Queue(2))
    eq("item", p("""{"v":1,"ev":"item","i":1,"n":2,"url":"http://a/b"}"""), DownloadEvent.Item(1, 2, "http://a/b"))
    eq("info", p("""{"v":1,"ev":"info","i":1,"title":"T","id":"x","duration":12.5,"uploader":null,"thumbnail":null,"heights":[1080,720]}"""),
        DownloadEvent.Info(1, "T", "x", 12L, null, null, listOf(1080, 720)))
    eq("progress", p("""{"v":1,"ev":"progress","i":1,"stream":"video","pct":46.2,"done":523264,"total":1133678,"speed":497106,"eta":1}"""),
        DownloadEvent.Progress(1, DownloadStream.VIDEO, 46.2, 523264L, 1133678L, 497106L, 1L))
    eq("progress null", p("""{"v":1,"ev":"progress","i":2,"stream":"x","pct":null,"done":null,"total":null,"speed":null,"eta":null}"""),
        DownloadEvent.Progress(2, DownloadStream.SINGLE, null, null, null, null, null))
    eq("stage", p("""{"v":1,"ev":"stage","i":1,"stage":"merge"}"""), DownloadEvent.Stage(1, "merge"))
    eq("done", p("""{"v":1,"ev":"done","i":1,"dir":"/d","existed":true,"files":[{"name":"a [x].mp4","path":"/d/a [x].mp4","size":99},{"bad":1}]}"""),
        DownloadEvent.Done(1, "/d", true, listOf(DownloadedFile("a [x].mp4", "/d/a [x].mp4", 99L))))
    eq("error", p("""{"v":1,"ev":"error","i":0,"code":"no_ytdlp","msg":"m \"q\" \u00e9"}"""), DownloadEvent.Error(0, "no_ytdlp", "m \"q\" é"))
    eq("summary", p("""{"v":1,"ev":"summary","ok":1,"failed":1,"cancelled":true}"""), DownloadEvent.Summary(1, 1, true))
    eq("warn", p("""{"v":1,"ev":"warn","code":"low_space","msg":"x"}"""), DownloadEvent.Warn("low_space", "x"))
    eq("non json", p("Mengunduh..."), null)
    eq("empty", p("  "), null)
    eq("broken", p("""{"v":1,"ev":"queue","total":"""), null)
    eq("other protocol", p("""{"v":2,"ev":"queue","total":2}"""), null)
    eq("no protocol", p("""{"ev":"queue","total":2}"""), null)
    eq("unknown ev", p("""{"v":1,"ev":"zzz"}"""), null)
    eq("array", p("""[1,2]"""), null)

    // --- reducer: antrian 2 item, satu sukses (video+audio+merge), satu gagal
    val lines = listOf(
        """{"v":1,"ev":"hello","script":"molina_dl","version":3}""",
        """{"v":1,"ev":"warn","code":"no_ffmpeg","msg":"tanpa ffmpeg"}""",
        """{"v":1,"ev":"queue","total":2}""",
        """{"v":1,"ev":"item","i":1,"n":2,"url":"http://a"}""",
        """{"v":1,"ev":"info","i":1,"title":"Satu","id":"a","duration":null,"uploader":null,"thumbnail":"http://t","heights":[720]}""",
        """{"v":1,"ev":"progress","i":1,"stream":"video","pct":50.0,"done":5,"total":10,"speed":1,"eta":5}""",
        """{"v":1,"ev":"progress","i":1,"stream":"audio","pct":100.0,"done":2,"total":2,"speed":1,"eta":0}""",
        """{"v":1,"ev":"stage","i":1,"stage":"merge"}""",
        """{"v":1,"ev":"done","i":1,"dir":"/d","existed":false,"files":[{"name":"s.mp4","path":"/d/s.mp4","size":10}]}""",
        """{"v":1,"ev":"item","i":2,"n":2,"url":"http://b"}""",
        """{"v":1,"ev":"error","i":2,"code":"network","msg":"putus"}""",
        """{"v":1,"ev":"summary","ok":1,"failed":1,"cancelled":false}""",
    )
    var st = DownloadSessionState()
    var mid: DownloadSessionState? = null
    for ((idx, l) in lines.withIndex()) {
        val e = p(l) ?: error("baris tak terbaca: $l")
        st = DownloadReducer.reduce(st, e)
        if (idx == 6) mid = st
    }
    eq("items", st.items.size, 2)
    eq("item1 status", st.items[0].status, DownloadItemStatus.DONE)
    eq("item1 title", st.items[0].title, "Satu")
    eq("item1 files", st.items[0].files.size, 1)
    eq("item2 status", st.items[1].status, DownloadItemStatus.FAILED)
    eq("item2 err", st.items[1].errorCode, "network")
    eq("warnings", st.warnings, listOf("tanpa ffmpeg"))
    check("finished", st.finished && st.ok == 1 && st.failed == 1 && !st.cancelled)
    eq("overall mid", DownloadReducer.overallPercent(mid!!.items[0]), 75.0)
    eq("overall done", DownloadReducer.overallPercent(st.items[0]), 100.0)
    eq("overall queued", DownloadReducer.overallPercent(DownloadItemState(1)), 0.0)
    // fatal global + item tanpa Queue
    val f = DownloadReducer.reduce(DownloadSessionState(), DownloadEvent.Error(0, "no_ytdlp", "yt-dlp tidak ada"))
    eq("fatal", f.fatalError, "yt-dlp tidak ada")
    val s2 = DownloadReducer.reduce(DownloadSessionState(), DownloadEvent.Item(3, 3, "http://c"))
    eq("auto-grow", s2.items.size, 3)
    eq("auto-grow running", s2.items[2].status, DownloadItemStatus.RUNNING)
    eq("index jahat", DownloadReducer.reduce(DownloadSessionState(), DownloadEvent.Item(9_999_999, 1, "x")).items.size, 0)
    eq("queue besar dibatasi", DownloadReducer.reduce(DownloadSessionState(), DownloadEvent.Queue(1_000_000)).items.size, 500)
    eq("retry item bersihkan galat",
        DownloadReducer.reduce(st, DownloadEvent.Item(2, 2, "http://b")).items[1].errorMsg, null)

    // --- kebijakan skrip bawaan
    val B = "bbb"; val OLD = "old"; val USR = "usr"
    eq("belum ada", BundledScriptPolicy.decide(null, null, null, B), Action.INSTALL)
    eq("belum ada walau tercatat", BundledScriptPolicy.decide(null, OLD, null, B), Action.INSTALL)
    eq("sama, belum tercatat", BundledScriptPolicy.decide(B, null, null, B), Action.RECORD)
    eq("sama, tercatat lama", BundledScriptPolicy.decide(B, OLD, null, B), Action.RECORD)
    eq("sama, tercatat", BundledScriptPolicy.decide(B, B, null, B), Action.KEEP)
    eq("tidak diedit, bawaan lama", BundledScriptPolicy.decide(OLD, OLD, null, B), Action.UPDATE)
    eq("diedit pengguna", BundledScriptPolicy.decide(USR, OLD, null, B), Action.STAGE_NEW)
    eq("asal tak diketahui", BundledScriptPolicy.decide(USR, null, null, B), Action.STAGE_NEW)
    eq("sudah pernah .new", BundledScriptPolicy.decide(USR, OLD, B, B), Action.KEEP)
    eq(".new versi lebih lama", BundledScriptPolicy.decide(USR, OLD, "ccc", B), Action.STAGE_NEW)

    println(if (failed == 0) "OK: $total pemeriksaan lulus" else "GAGAL: $failed dari $total")
    if (failed != 0) kotlin.system.exitProcess(1)
}
