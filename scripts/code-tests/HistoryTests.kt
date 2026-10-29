import com.molina.code.history.DiffOp
import com.molina.code.history.HistoryFormat
import com.molina.code.history.HistoryKind
import com.molina.code.history.HistoryStore
import com.molina.code.history.LineDiff
import java.io.File
import java.nio.file.Files
import java.util.TimeZone

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

fun main() {
    val tmp = Files.createTempDirectory("histtest").toFile()
    try {
        var t = 1_000_000L
        val store = HistoryStore(File(tmp, "history")) { t }
        val f = File(tmp, "proj/App.kt")

        // ---- record / list / read
        check("kosong di awal", store.list(f).isEmpty())
        val v1 = store.record(f, "satu\ndua\n", HistoryKind.BEFORE)
        t += 10
        val v2 = store.record(f, "satu\ndua\ntiga\n", HistoryKind.SAVED)
        check("record mengembalikan versi", v1 != null && v2 != null)
        check("metadata: baris dan karakter", v2!!.lines == 3 && v2.chars == 14 && v2.kind == HistoryKind.SAVED)
        val l = store.list(f)
        check("list: terbaru dulu", l.size == 2 && l[0].name == v2.name && l[1].name == v1!!.name)
        check("read: isi persis", store.read(f, l[1]) == "satu\ndua\n" && store.read(f, l[0]) == "satu\ndua\ntiga\n")
        check("read: unicode", store.record(f, "héllo ✓ 日本\n", HistoryKind.SAVED).let { v -> v != null && store.read(f, v) == "héllo ✓ 日本\n" })

        // ---- dedupe dan monotonik
        check("dedupe: isi sama dengan terbaru = null", store.record(f, "héllo ✓ 日本\n", HistoryKind.BEFORE) == null)
        val a = store.record(f, "x\n", HistoryKind.SAVED)
        t -= 5_000 // jam mundur
        val b = store.record(f, "y\n", HistoryKind.SAVED)
        check("waktu tetap naik walau jam mundur", a != null && b != null && b.time > a.time && store.list(f).first().name == b.name)
        check("isi lama yang kembali tetap dicatat (bukan terbaru)", store.record(f, "x\n", HistoryKind.SAVED) != null)

        // ---- berkas berbeda terpisah, path kanonik sama
        val g = File(tmp, "proj/Other.kt")
        store.record(g, "lain\n", HistoryKind.SAVED)
        check("riwayat per berkas terpisah", store.list(g).size == 1 && store.list(f).size > 3)
        check("path tidak kanonik = berkas sama", store.list(File(tmp, "proj/../proj/App.kt")).size == store.list(f).size)

        // ---- batas versi
        val h = File(tmp, "many.txt")
        for (i in 1..(HistoryStore.MAX_VERSIONS + 15)) { t += 1; store.record(h, "isi $i\n", HistoryKind.SAVED) }
        val hl = store.list(h)
        check("pangkas: maksimal MAX_VERSIONS", hl.size == HistoryStore.MAX_VERSIONS)
        check("pangkas: yang terbaru bertahan", store.read(h, hl.first()) == "isi ${HistoryStore.MAX_VERSIONS + 15}\n")

        // ---- terlalu besar, rusak, hapus
        check("terlalu besar ditolak", store.record(File(tmp, "big.txt"), "x".repeat(HistoryStore.MAX_CHARS + 1), HistoryKind.SAVED) == null)
        val vv = store.list(g).first()
        File(tmp, "history").listFiles()!!.flatMap { d -> d.listFiles()!!.toList() }.first { it.name == vv.name }.writeBytes(byteArrayOf(1, 2, 3))
        check("snapshot rusak: read = null, tanpa crash", store.read(g, vv) == null)
        store.clear(g)
        check("clear menghapus riwayat berkas", store.list(g).isEmpty() && store.list(f).isNotEmpty())

        // ---- pangkas global
        store.trimGlobal(1)
        check("trimGlobal: dibuang sampai di bawah batas", store.list(f).size < 5 && store.list(h).size < 5)
        store.trimGlobal(0)
        check("trimGlobal 0: semua kosong dan folder dibersihkan", store.list(f).isEmpty() && File(tmp, "history").listFiles()!!.none { it.name.startsWith("h") })

        // ---- LineDiff
        val d1 = LineDiff.diff("a\nb\nc\n", "a\nb\nc\n")
        check("diff: identik", !d1.changed && d1.lines.size == 3)
        val d2 = LineDiff.diff("a\nb\nc\nd\ne\n", "a\nB\nc\nd\ne\nf\n")
        check("diff: ubah + tambah", d2.added == 2 && d2.removed == 1)
        check("diff: urutan DEL sebelum ADD pada baris diganti", d2.lines.map { it.op } == listOf(DiffOp.SAME, DiffOp.DEL, DiffOp.ADD, DiffOp.SAME, DiffOp.SAME, DiffOp.SAME, DiffOp.ADD))
        val del = d2.lines.first { it.op == DiffOp.DEL }
        val add = d2.lines.first { it.op == DiffOp.ADD }
        check("diff: nomor baris", del.oldNo == 2 && del.newNo == 0 && add.newNo == 2 && add.oldNo == 0)
        check("diff: dari kosong", LineDiff.diff("", "x\ny\n").let { it.added == 2 && it.removed == 0 })
        check("diff: ke kosong", LineDiff.diff("x\ny\n", "").let { it.added == 0 && it.removed == 2 })
        val d3 = LineDiff.diff("1\n2\n3\n4\n5\n6\n", "1\n3\n4\n5\n7\n6\n")
        check("diff: LCS minimal", d3.removed == 1 && d3.added == 1)
        val rebuilt = d3.lines.filter { it.op != DiffOp.DEL }.joinToString("\n") { it.text } + "\n"
        check("diff: ADD+SAME menyusun ulang teks baru", rebuilt == "1\n3\n4\n5\n7\n6\n")
        val rebuiltOld = d3.lines.filter { it.op != DiffOp.ADD }.joinToString("\n") { it.text } + "\n"
        check("diff: DEL+SAME menyusun ulang teks lama", rebuiltOld == "1\n2\n3\n4\n5\n6\n")
        val bigA = (1..3000).joinToString("\n") { "a$it" } + "\n"
        val bigB = (1..3000).joinToString("\n") { "b$it" } + "\n"
        val d4 = LineDiff.diff(bigA, bigB)
        check("diff: terlalu besar = kasar tapi benar", d4.coarse && d4.removed == 3000 && d4.added == 3000)
        val c = LineDiff.compact(LineDiff.diff((1..20).joinToString("\n") { "l$it" } + "\n", (1..20).joinToString("\n") { if (it == 10) "X" else "l$it" } + "\n"), 2)
        check("compact: konteks 2 + ringkasan", c.count { it.skipped > 0 } == 2 && c.first().skipped == 7 && c.last().skipped == 8 && c.count { it.line != null } == 6)
        check("compact: tanpa perubahan = kosong", LineDiff.compact(d1).isEmpty())
        check("diff: beda akhir baris saja = tak berubah", !LineDiff.diff("a\nb", "a\nb\n").changed)

        // ---- format
        val tz = TimeZone.getTimeZone("UTC")
        val now = 1_730_203_200_000L // 29 Okt 2024 12:00 UTC
        check("waktu: hari ini", HistoryFormat.time(now - 3_600_000, now, tz) == "Hari ini 11:00")
        check("waktu: kemarin", HistoryFormat.time(now - 86_400_000, now, tz) == "Kemarin 12:00")
        check("waktu: tanggal", HistoryFormat.time(now - 5 * 86_400_000, now, tz) == "24 Okt 12:00")
        check("waktu: beda tahun", HistoryFormat.time(now - 400L * 86_400_000, now, tz).contains("2023"))
        check("ukuran", HistoryFormat.size(850) == "850 B" && HistoryFormat.size(1229) == "1,2 KB" && HistoryFormat.size(2 * 1024 * 1024) == "2,0 MB")
        check("countLines", HistoryStore.countLines("") == 0 && HistoryStore.countLines("a") == 1 && HistoryStore.countLines("a\n") == 1 && HistoryStore.countLines("a\nb") == 2)
    } finally {
        tmp.deleteRecursively()
    }
    println(if (fails == 0) "SEMUA LULUS" else "$fails GAGAL")
    if (fails != 0) kotlin.system.exitProcess(1)
}
