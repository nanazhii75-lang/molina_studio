import com.molina.core.common.*
import java.io.File
import java.nio.file.Files
import java.util.TimeZone

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

private fun put(dir: File, name: String, size: Int = 1, mod: Long? = null): File =
    File(dir, name).also { it.writeBytes(ByteArray(size)); if (mod != null) it.setLastModified(mod) }

fun main() {
    val utc = TimeZone.getTimeZone("UTC")

    // ---- size / date / clock
    check("size: byte", TvGuide.size(512) == "512 B" && TvGuide.size(0) == "0 B" && TvGuide.size(-3) == "0 B")
    check("size: KB koma Indonesia", TvGuide.size(1536) == "1,5 KB")
    check("size: MB", TvGuide.size(2L * 1024 * 1024) == "2,0 MB")
    check("size: GB", TvGuide.size((3.4 * 1024 * 1024 * 1024).toLong()) == "3,4 GB")
    check("size: batas 1024 KB -> MB", TvGuide.size(1024L * 1024) == "1,0 MB")
    check("date: format Indonesia", TvGuide.date(1_706_659_200_000L, utc) == "31/01/2024 00.00")
    check("clock: HH.mm", TvGuide.clock(1_706_659_200_000L + 7 * 3600_000L + 5 * 60_000L, utc) == "07.05")

    // ---- step
    check("step: dibatasi", TvGuide.step(0, -1, 5) == 0 && TvGuide.step(4, 1, 5) == 4 && TvGuide.step(2, 1, 5) == 3)
    check("step: kosong -> -1", TvGuide.step(0, 1, 0) == -1)

    val d = Files.createTempDirectory("tv").toFile()
    try {
        // ---- channels
        val a = File(d, "Unduhan").also { it.mkdirs() }
        val b = File(d, "Video").also { it.mkdirs() }
        val ch = TvGuide.channels(listOf("Molina" to File(d, "tidak-ada"), "Unduhan" to a, "Video" to b))
        check("channels: folder hilang dilewati tanpa lubang nomor", ch.map { it.number } == listOf(1, 2) && ch.map { it.label } == listOf("Unduhan", "Video"))
        check("channels: kosong", TvGuide.channels(emptyList()).isEmpty())

        // ---- kindOf
        check("kind: video/audio/lainnya/folder",
            TvGuide.kindOf(put(d, "a.MP4")) == TvGuide.Kind.Video && TvGuide.kindOf(put(d, "a.flac")) == TvGuide.Kind.Audio &&
            TvGuide.kindOf(put(d, "a.txt")) == TvGuide.Kind.Other && TvGuide.kindOf(a) == TvGuide.Kind.Folder)

        // ---- rows
        val r = File(d, "isi").also { it.mkdirs() }
        File(r, "zeta").mkdirs(); File(r, "Alfa").mkdirs(); File(r, ".rahasia").mkdirs()
        put(r, "lama.mp4", mod = 1_000_000_000_000L)
        put(r, "baru.mkv", mod = 1_700_000_000_000L)
        put(r, "lagu.mp3", mod = 1_500_000_000_000L)
        put(r, "catatan.txt"); put(r, ".tersembunyi.mp4"); put(r, "poster.jpg")
        val rows = TvGuide.rows(r)
        check("rows: urutan baris", rows.map { it.title } == listOf("Folder", "Video", "Audio", "Lainnya"))
        check("rows: folder urut nama tanpa tersembunyi", rows[0].items.map { it.name } == listOf("Alfa", "zeta"))
        check("rows: video terbaru dulu", rows[1].items.map { it.name } == listOf("baru.mkv", "lama.mp4"))
        check("rows: audio", rows[2].items.single().name == "lagu.mp3")
        check("rows: lainnya memuat txt dan jpg", rows[3].items.map { it.name }.toSet() == setOf("catatan.txt", "poster.jpg"))
        check("rows: judul tanpa ekstensi", rows[1].items[0].title == "baru" && rows[0].items[0].title == "Alfa")

        val capped = TvGuide.rows(r, 1)
        check("rows: dipotong perRow, total tetap", capped[1].items.size == 1 && capped[1].total == 2)
        check("rows: perRow nol dianggap 1", TvGuide.rows(r, 0)[1].items.size == 1)
        val empty = File(d, "kosong").also { it.mkdirs() }
        check("rows: folder kosong -> tanpa baris", TvGuide.rows(empty).isEmpty())
        check("rows: folder hilang -> kosong", TvGuide.rows(File(d, "gaib")).isEmpty())
        check("rows: berkas bukan folder -> kosong", TvGuide.rows(put(d, "bukan-folder.bin")).isEmpty())

        // ---- describe
        val it0 = TvGuide.Item(put(d, "Film.mp4", size = 2048, mod = 1_706_659_200_000L), TvGuide.Kind.Video, 2048, 1_706_659_200_000L)
        check("describe: video + extra", TvGuide.describe(it0, "1:02:05", utc) == "Video \u00B7 2,0 KB \u00B7 1:02:05 \u00B7 31/01/2024 00.00")
        check("describe: tanpa extra", TvGuide.describe(it0, null, utc) == "Video \u00B7 2,0 KB \u00B7 31/01/2024 00.00")
        check("describe: extra kosong diabaikan", TvGuide.describe(it0, "  ", utc) == TvGuide.describe(it0, null, utc))
        val fd = TvGuide.Item(a, TvGuide.Kind.Folder, 0, 1_706_659_200_000L)
        check("describe: folder tanpa ukuran", TvGuide.describe(fd, null, utc) == "Folder \u00B7 31/01/2024 00.00")

        // ---- freeLabel
        check("freeLabel: folder ada -> berakhiran bebas", TvGuide.freeLabel(d)?.endsWith(" bebas") == true)
        check("freeLabel: folder hilang -> null", TvGuide.freeLabel(File(d, "gaib")) == null)
    } finally {
        d.deleteRecursively()
    }
    if (fails > 0) { println("$fails GAGAL"); System.exit(1) }
    println("semua lulus")
}
