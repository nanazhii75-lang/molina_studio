import com.molina.core.common.*

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

private val U = listOf("https://a.test/1")

fun main() {
    val d = DownloadChoices()
    check("bawaan: tanpa subtitle, sampul aktif, tanpa batas", d.subs.isEmpty() && d.subsMode == "embed" && d.cover && d.limitRate.isEmpty())

    // sanitize: nilai baru
    val ok = DownloadChoices.sanitize(true, 720, "mkv", "flac", "320k", listOf("id", "en"), "file", false, "2M")
    check("sanitize: nilai valid dipertahankan", ok.subs == listOf("id", "en") && ok.subsMode == "file" && !ok.cover && ok.limitRate == "2M")
    val bad = DownloadChoices.sanitize(true, 0, "mp4", "mp3", "192k", listOf("xx", "yy"), "zip", null, "9M")
    check("sanitize: nilai tidak valid jatuh ke bawaan", bad == DownloadChoices())
    check("sanitize: huruf besar bahasa dinormalkan", DownloadChoices.sanitize(null, null, null, null, null, listOf("ID")).subs == listOf("id"))
    check("sanitize: urutan bahasa di luar preset ditolak", DownloadChoices.sanitize(null, null, null, null, null, listOf("en", "id")).subs.isEmpty())
    check("sanitize: pemanggilan lama (5 argumen) tetap jalan", DownloadChoices.sanitize(null, null, null, null, null) == DownloadChoices())

    // semua preset lolos validasi perintah dan argumen benar
    check("preset subtitle semua lolos validasi", DownloadChoices.SUB_PRESETS.all { DownloadCommand.validate(DownloadRequest(U, subs = it)) == null })
    check("batas kecepatan semua lolos validasi", DownloadChoices.LIMIT_RATES.all { DownloadCommand.validate(DownloadRequest(U, limitRate = it.ifEmpty { null })) == null })
    check("label: subtitle", DownloadChoices.SUB_PRESETS.map(DownloadChoices::subsLabel) == listOf("Tanpa", "ID", "EN", "ID+EN"))
    check("label: kecepatan", DownloadChoices.LIMIT_RATES.map(DownloadChoices::rateLabel) == listOf("Bebas", "500K", "2M", "5M"))

    // riwayat: build -> encode -> decode membawa pilihan baru
    val req = DownloadRequest(U, subs = listOf("id", "en"), subsMode = "file", cover = false, limitRate = "2M")
    val e = DownloadHistory.build(1L, req, DownloadSessionState())!!
    check("build: pilihan baru ikut tercatat", e.choices.subs == listOf("id", "en") && e.choices.subsMode == "file" && !e.choices.cover && e.choices.limitRate == "2M")
    val back = DownloadHistory.decode(DownloadHistory.encode(listOf(e)))
    check("encode/decode: pilihan baru utuh", back.size == 1 && back[0].choices == e.choices)

    // retryRequest membawa opsi baru ke argumen skrip
    val rv = e.retryRequest()
    check("retry: lolos validasi dan membawa opsi", DownloadCommand.validate(rv) == null && rv.subs == listOf("id", "en") && rv.limitRate == "2M")
    val args = DownloadCommand.args(rv)
    check("retry: argumen video memuat --subs/--subs-mode/--limit-rate", args.containsAll(listOf("--subs", "id,en", "--subs-mode", "file", "--limit-rate", "2M")))
    val ea = DownloadHistory.build(2L, DownloadRequest(U, mode = DownloadMode.AUDIO, cover = false), DownloadSessionState())!!
    check("retry: audio tanpa sampul memuat --no-cover", DownloadCommand.args(ea.retryRequest()).contains("--no-cover"))
    check("retry: tanpa batas -> tidak ada --limit-rate", !DownloadCommand.args(DownloadHistory.build(3L, DownloadRequest(U), DownloadSessionState())!!.retryRequest()).contains("--limit-rate"))

    // kompatibilitas: berkas riwayat lama (tanpa kunci baru) tetap terbaca dan jatuh ke bawaan
    val old = """{"v":1,"entries":[{"at":5,"video":false,"h":0,"vx":"mp4","af":"flac","br":"320k","c":false,"fatal":null,"items":[{"u":"https://a.test/9","t":null,"ok":false,"e":null,"f":[]}]}]}"""
    val o = DownloadHistory.decode(old)
    check("decode: riwayat lama tetap terbaca", o.size == 1 && o[0].choices == DownloadChoices(false, 0, "mp4", "flac", "320k"))
    val junk = """{"v":1,"entries":[{"at":6,"video":true,"sb":"zz","sm":7,"cv":"x","lr":99,"items":[{"u":"https://a.test/1","ok":false}]}]}"""
    val j = DownloadHistory.decode(junk)
    check("decode: kunci baru rusak jatuh ke bawaan tanpa melempar", j.size == 1 && j[0].choices == DownloadChoices())

    // label mode
    check("modeLabel: bawaan tidak berubah", DownloadHistoryEntry(1, DownloadChoices(), false, null, e.items).modeLabel() == "Video Terbaik MP4")
    check("modeLabel: subtitle dan batas tampil", e.modeLabel() == "Video Terbaik MP4 · sub ID+EN .srt · maks 2M/dtk")
    check("modeLabel: audio tidak menampilkan subtitle", DownloadHistoryEntry(1, DownloadChoices(video = false, subs = listOf("id")), false, null, e.items).modeLabel() == "Audio MP3 192k")

    if (fails > 0) { println("$fails pemeriksaan GAGAL"); System.exit(1) }
    println("semua uji lulus")
}
