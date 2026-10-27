import com.molina.core.common.*

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

fun main() {
    // DownloadFormat.size
    check("size: byte", DownloadFormat.size(0) == "0 B" && DownloadFormat.size(1023) == "1023 B")
    check("size: KB", DownloadFormat.size(1024) == "1 KB" && DownloadFormat.size(1536) == "2 KB")
    check("size: MB", DownloadFormat.size(1L shl 20) == "1.0 MB" && DownloadFormat.size(5L * (1L shl 20) + (1L shl 19)) == "5.5 MB")
    check("size: GB", DownloadFormat.size(3L shl 30) == "3.00 GB")
    check("size: negatif jadi 0", DownloadFormat.size(-5) == "0 B")
    check("size: titik desimal tidak bergantung locale", java.util.Locale.getDefault().let {
        java.util.Locale.setDefault(java.util.Locale("id", "ID")); val r = DownloadFormat.size(1L shl 20); java.util.Locale.setDefault(it); r == "1.0 MB"
    })

    // DownloadFormat.eta
    check("eta: detik", DownloadFormat.eta(0) == "0d" && DownloadFormat.eta(42) == "42d")
    check("eta: menit", DownloadFormat.eta(60) == "1m 00d" && DownloadFormat.eta(185) == "3m 05d")
    check("eta: jam", DownloadFormat.eta(3600) == "1j 00m" && DownloadFormat.eta(3725) == "1j 02m")
    check("eta: negatif jadi 0", DownloadFormat.eta(-9) == "0d")

    // DownloadPaths.toAndroid
    val base = "/storage/emulated/0"
    check("path: file di /sdcard", DownloadPaths.toAndroid("/sdcard/Molina/Media/Video/a.mp4", base) == "/storage/emulated/0/Molina/Media/Video/a.mp4")
    check("path: akar /sdcard", DownloadPaths.toAndroid("/sdcard", base) == "/storage/emulated/0")
    check("path: base dengan garis miring akhir", DownloadPaths.toAndroid("/sdcard/x.mp3", "/storage/emulated/0/") == "/storage/emulated/0/x.mp3")
    check("path: di luar /sdcard ditolak", DownloadPaths.toAndroid("/root/unduhan/a.mp4", base) == null && DownloadPaths.toAndroid("/sdcardx/a.mp4", base) == null)
    check("path: relatif ditolak", DownloadPaths.toAndroid("sdcard/a.mp4", base) == null && DownloadPaths.toAndroid("", base) == null)
    check("path: traversal ditolak", DownloadPaths.toAndroid("/sdcard/../data/a", base) == null && DownloadPaths.toAndroid("/sdcard/a/../../b", base) == null)
    check("path: titik biasa di nama berkas diterima", DownloadPaths.toAndroid("/sdcard/a..b/c...mp4", base) == "/storage/emulated/0/a..b/c...mp4")
    check("path: karakter kontrol ditolak", DownloadPaths.toAndroid("/sdcard/a\u0000b", base) == null && DownloadPaths.toAndroid("/sdcard/a\nb", base) == null)
    check("path: base tidak valid ditolak", DownloadPaths.toAndroid("/sdcard/a", "") == null && DownloadPaths.toAndroid("/sdcard/a", "relatif") == null)

    // DownloadUrls.split
    check("split: spasi, baris, tab, CRLF", DownloadUrls.split("https://a.test/1\r\nhttps://a.test/2\t https://a.test/3") == listOf("https://a.test/1", "https://a.test/2", "https://a.test/3"))
    check("split: kosong dan duplikat dibuang", DownloadUrls.split("  \n https://a.test/1 \n\nhttps://a.test/1") == listOf("https://a.test/1"))

    // DownloadUrls.merge
    val m1 = DownloadUrls.merge("", listOf("https://a.test/1", "https://a.test/2"))
    check("merge: ke kolom kosong", m1.text == "https://a.test/1\nhttps://a.test/2" && m1.added == 2 && m1.skipped == 0 && m1.dropped == 0)
    val m2 = DownloadUrls.merge("https://a.test/1\n", listOf("https://a.test/1", " https://a.test/2 ", ""))
    check("merge: duplikat dilewati, spasi dipangkas", m2.text == "https://a.test/1\nhttps://a.test/2" && m2.added == 1 && m2.skipped == 1)
    val m3 = DownloadUrls.merge("https://a.test/1 https://a.test/2", listOf("https://a.test/3"))
    check("merge: urutan lama dipertahankan, baru di belakang", m3.text.lines() == listOf("https://a.test/1", "https://a.test/2", "https://a.test/3"))
    val many = List(60) { "https://a.test/$it" }
    val m4 = DownloadUrls.merge("", many)
    check("merge: dibatasi MAX_URLS", m4.added == DownloadCommand.MAX_URLS && m4.dropped == 10 && DownloadUrls.split(m4.text).size == DownloadCommand.MAX_URLS)
    val m5 = DownloadUrls.merge(m4.text, listOf("https://a.test/0", "https://b.test/x"))
    check("merge: penuh -> yang sudah ada tetap dihitung skipped, baru dropped", m5.skipped == 1 && m5.dropped == 1 && m5.added == 0)
    check("merge: hasil lolos validasi bila URL sah", DownloadCommand.validate(DownloadRequest(DownloadUrls.split(m1.text))) == null)

    // DownloadChoices.sanitize
    val dc0 = DownloadChoices.sanitize(null, null, null, null, null)
    check("choices: semua null -> bawaan", dc0 == DownloadChoices())
    val dc1 = DownloadChoices.sanitize(false, 720, "mkv", "flac", "320k")
    check("choices: nilai valid dipertahankan", !dc1.video && dc1.height == 720 && dc1.videoExt == "mkv" && dc1.audioFmt == "flac" && dc1.bitrate == "320k")
    val dc2 = DownloadChoices.sanitize(true, 999, "avi", "ogg", "64k")
    check("choices: nilai tidak valid -> bawaan per bidang", dc2 == DownloadChoices())
    val dc3 = DownloadChoices.sanitize(false, 1234, "mkv", "wav", "128k")
    check("choices: sebagian valid, sebagian jatuh ke bawaan", !dc3.video && dc3.height == 0 && dc3.videoExt == "mkv" && dc3.audioFmt == "mp3" && dc3.bitrate == "128k")
    check("choices: semua HEIGHTS lolos validasi perintah", DownloadChoices.HEIGHTS.all { DownloadCommand.validate(DownloadRequest(listOf("https://a.test/1"), height = it)) == null })
    check("choices: bawaan lolos validasi perintah", DownloadCommand.validate(DownloadRequest(listOf("https://a.test/1"), videoExt = dc0.videoExt, audioFormat = dc0.audioFmt, bitrate = dc0.bitrate)) == null)

    if (fails > 0) { println("$fails uji GAGAL"); kotlin.system.exitProcess(1) }
    println("semua uji lulus")
}
