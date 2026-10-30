import com.molina.core.common.*
import java.io.File
import java.nio.file.Files

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

private fun put(dir: File, name: String, text: String = "x"): File = File(dir, name).also { it.writeText(text) }

fun main() {
    // ---- clock
    check("clock: 75 -> 1:15", ShelfCard.clock(75) == "1:15")
    check("clock: 3725 -> 1:02:05", ShelfCard.clock(3725) == "1:02:05")
    check("clock: negatif -> 0:00", ShelfCard.clock(-5) == "0:00")
    check("clock: 59 -> 0:59", ShelfCard.clock(59) == "0:59")

    // ---- monogram
    check("monogram: huruf kecil -> besar", ShelfCard.monogram("film") == "F")
    check("monogram: lewati simbol", ShelfCard.monogram("  [x] Judul") == "X")
    check("monogram: angka", ShelfCard.monogram("2001") == "2")
    check("monogram: kosong -> M", ShelfCard.monogram("") == "M" && ShelfCard.monogram("!!!") == "M")
    check("monogram: unicode", ShelfCard.monogram("\u00E9cole") == "\u00C9")

    val d = Files.createTempDirectory("shelf").toFile()
    try {
        // ---- video lokal dengan sidecar lengkap
        val v = put(d, "Film [abc].mp4")
        put(d, "Film [abc].jpg")
        put(d, "Film [abc].info.json", """{"title":"Pelajaran Bab 1","channel":"Kanal Belajar","duration":3725,"upload_date":"20240131"}""")
        val c = ShelfCard.of(ShelfCard.Entry(v.path, "Film [abc].mp4", true))
        check("of: judul dari info.json", c.title == "Pelajaran Bab 1")
        check("of: subtitle kanal + tahun", c.subtitle == "Kanal Belajar \u00B7 2024")
        check("of: durasi", c.duration == "1:02:05")
        check("of: poster terdeteksi + thumbPath", c.hasPoster && c.thumbPath == v.path)
        check("of: monogram dari judul info", c.monogram == "P")

        // ---- video lokal tanpa sidecar
        val plain = put(d, "Polos.mkv")
        val p = ShelfCard.of(ShelfCard.Entry(plain.path, "Polos.mkv", true))
        check("of: tanpa sidecar memakai judul riwayat", p.title == "Polos.mkv" && p.duration == null && !p.hasPoster)
        check("of: tanpa sidecar subtitle = nama folder", p.subtitle == d.name)
        check("of: tanpa sidecar tetap thumbPath video", p.thumbPath == plain.path)

        // ---- info.json rusak
        val bad = put(d, "Rusak.mp4")
        put(d, "Rusak.info.json", "bukan json {{{")
        val b = ShelfCard.of(ShelfCard.Entry(bad.path, "Rusak.mp4", true))
        check("of: info rusak tidak melempar, judul riwayat", b.title == "Rusak.mp4" && b.duration == null)

        // ---- audio: sidecar diabaikan, tanpa thumbPath
        val a = put(d, "Lagu.mp3")
        put(d, "Lagu.jpg")
        val ac = ShelfCard.of(ShelfCard.Entry(a.path, "Lagu.mp3", false))
        check("of: audio tanpa thumbPath dan tanpa poster sidecar", ac.thumbPath == null && !ac.hasPoster)

        // ---- URL
        val u = ShelfCard.of(ShelfCard.Entry("https://example.test/live/stream.m3u8", "", true))
        check("of: URL judul kosong -> host", u.title == "example.test")
        check("of: URL subtitle host, tanpa thumbPath", u.subtitle == "example.test" && u.thumbPath == null)
        val u2 = ShelfCard.of(ShelfCard.Entry("https://example.test/a", "Siaran", true))
        check("of: URL memakai judul riwayat", u2.title == "Siaran")

        // ---- berkas hilang
        val gone = ShelfCard.of(ShelfCard.Entry("/tidak/ada/Hilang.mp4", "", true))
        check("of: berkas hilang tidak melempar, judul dari nama", gone.title == "Hilang.mp4" && !gone.hasPoster)

        // ---- build
        val many = (1..12).map { ShelfCard.Entry("https://example.test/$it", "T$it", true) }
        val shelf = ShelfCard.build(many)
        check("build: dibatasi MAX_CARDS", shelf.size == ShelfCard.MAX_CARDS)
        check("build: urutan dipertahankan", shelf.first().title == "T1" && shelf.last().title == "T8")
        check("build: ref kosong disaring", ShelfCard.build(listOf(ShelfCard.Entry(" ", "x", true))).isEmpty())
        check("build: max negatif -> kosong", ShelfCard.build(many, -1).isEmpty())
    } finally {
        d.deleteRecursively()
    }
    if (fails > 0) { println("$fails GAGAL"); System.exit(1) }
    println("semua lulus")
}
