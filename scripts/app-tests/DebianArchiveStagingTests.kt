package com.molina.studio.cinema

import java.io.File
import java.io.IOException
import java.nio.file.Files

private var fails = 0
private fun check(name: String, ok: Boolean) { if (ok) println("ok   $name") else { fails++; println("GAGAL $name") } }
private fun throwsIo(block: () -> Unit): Boolean = try { block(); false } catch (e: IOException) { true }

private fun tmp(): File = Files.createTempDirectory("molina-stage-test").toFile()
private fun File.put(rel: String, text: String = "x"): File { val f = File(this, rel); f.parentFile.mkdirs(); f.writeText(text); return f }

fun main() {
    // ---- DebianFormats
    fun d(n: String) = DebianFormats.detect(n)
    check("rar", d("a.rar")?.label == "RAR" && d("a.rar")!!.firstVolume && d("a.rar")!!.stem == "a")
    check("RAR huruf besar", d("A.RAR")?.label == "RAR")
    check("iso/cab/wim/deb/rpm/dmg", listOf("a.iso", "b.cab", "c.wim", "d.deb", "e.rpm", "f.dmg", "g.cpio", "h.arj", "i.lzh").all { d(it) != null })
    check("part1.rar pertama", d("x.part1.rar")?.firstVolume == true && d("x.part01.rar")?.firstVolume == true && d("x.part1.rar")!!.stem == "x")
    check("part2.rar bukan pertama", d("x.part2.rar")?.firstVolume == false)
    check("7z.001 pertama, .002 bukan", d("x.7z.001")?.firstVolume == true && d("x.7z.002")?.firstVolume == false && d("x.7z.001")!!.label == "7Z")
    check("zip.001", d("x.zip.001")?.label == "ZIP")
    check("zip/7z/tar bukan urusan Debian", d("a.zip") == null && d("a.7z") == null && d("a.tar.gz") == null)
    check("stem kosong ditolak", d(".rar") == null)
    check("folderName rar", DebianFormats.folderName("Film.part1.rar") == "Film" && DebianFormats.folderName("a.iso") == "a")

    // ---- create / sweepStale
    val dest = tmp()
    val st = DebianArchiveStaging.create(dest, 42L)
    check("create: dir tersembunyi di dest", st.isDirectory && st.parentFile == dest && st.name.startsWith(DebianArchiveStaging.PREFIX))
    check("create: nonce sama ditolak", throwsIo { DebianArchiveStaging.create(dest, 42L) })
    check("create: parent tak ada ditolak", throwsIo { DebianArchiveStaging.create(File(dest, "nope")) })
    check("sweep: baru tidak dibuang", DebianArchiveStaging.sweepStale(dest) == 0 && st.exists())
    st.put("a.txt")
    check("sweep: usang dibuang", DebianArchiveStaging.sweepStale(dest, System.currentTimeMillis() + DebianArchiveStaging.STALE_MS + 1000) == 1 && !st.exists())
    val other = dest.put(".molina-stagex/keep.txt"); // bukan awalan persis? awalan '.molina-stage' + 'x' tetap berawalan -> hanya dir
    check("sweep: berkas biasa tak disentuh", File(dest, "plain.txt").also { it.writeText("p") }.exists() && DebianArchiveStaging.sweepStale(dest, Long.MAX_VALUE / 2) >= 0 && File(dest, "plain.txt").exists())

    // ---- audit
    val a = tmp()
    a.put("f1.txt", "12345"); a.put("sub/f2.txt", "123"); File(a, "empty").mkdir()
    val au = DebianArchiveStaging.audit(a)
    check("audit: hitung", au.files == 2 && au.dirs == 2 && au.bytes == 8L && au.removed == 0)
    val victim = tmp(); val vf = victim.put("secret.txt", "jangan hapus")
    try {
        Files.createSymbolicLink(File(a, "lnk").toPath(), victim.toPath())
        Files.createSymbolicLink(File(a, "lnkfile").toPath(), vf.toPath())
        val au2 = DebianArchiveStaging.audit(a)
        check("audit: tautan dihapus, target utuh", au2.removed == 2 && !File(a, "lnk").exists() && vf.readText() == "jangan hapus")
        // deleteTree tidak mengikuti tautan
        Files.createSymbolicLink(File(a, "lnk2").toPath(), victim.toPath())
        DebianArchiveStaging.deleteTree(a)
        check("deleteTree: tidak mengikuti tautan", !a.exists() && vf.exists() && vf.readText() == "jangan hapus")
    } catch (e: UnsupportedOperationException) { println("lewati: symlink tak didukung") }
    check("audit: stage berupa tautan ditolak", run {
        val real = tmp(); val link = File(tmp(), "ln"); Files.createSymbolicLink(link.toPath(), real.toPath())
        throwsIo { DebianArchiveStaging.audit(link) }
    })

    // ---- commit
    fun fresh(): Pair<File, File> { val dd = tmp(); return dd to DebianArchiveStaging.create(dd) }

    run {
        val (dd, s) = fresh()
        s.put("a.txt", "A"); s.put("dir/b.txt", "B"); s.put("dir/deep/c.txt", "C")
        val r = DebianArchiveStaging.commit(s, dd, null) { false }
        check("commit HERE: berkas+folder", File(dd, "a.txt").readText() == "A" && File(dd, "dir/deep/c.txt").readText() == "C" && r.files == 3 && r.renamed == 0 && r.base == dd.canonicalFile)
    }
    run {
        val (dd, s) = fresh()
        dd.put("a.txt", "LAMA"); dd.put("dir/b.txt", "LAMA-B")
        s.put("a.txt", "BARU"); s.put("dir/b.txt", "BARU-B"); s.put("dir/n.txt", "N")
        val r = DebianArchiveStaging.commit(s, dd, null) { false }
        check("commit: tidak menimpa", File(dd, "a.txt").readText() == "LAMA" && File(dd, "a (1).txt").readText() == "BARU")
        check("commit: folder digabung, berkas bentrok diganti nama", File(dd, "dir/b.txt").readText() == "LAMA-B" && File(dd, "dir/b (1).txt").readText() == "BARU-B" && File(dd, "dir/n.txt").readText() == "N")
        check("commit: hitung renamed/files", r.renamed == 2 && r.files == 3)
    }
    run {
        val (dd, s) = fresh()
        dd.put("Film/lama.txt")
        s.put("x.txt", "X")
        val r = DebianArchiveStaging.commit(s, dd, "Film") { false }
        check("commit NAMED: nama unik", r.base.name == "Film (1)" && File(dd, "Film (1)/x.txt").readText() == "X" && File(dd, "Film/lama.txt").exists())
    }
    run {
        val (dd, s) = fresh()
        s.put("a.txt"); s.put("b.txt"); s.put("c.txt")
        var n = 0
        val ex = try { DebianArchiveStaging.commit(s, dd, "Out") { ++n > 2 }; null } catch (e: IOException) { e }
        check("commit batal: rollback total", ex?.message == "Dibatalkan" && !File(dd, "Out").exists() && File(dd, "a.txt").exists().not())
    }
    run {
        val (dd, s) = fresh()
        dd.put("keep.txt", "K"); s.put("a.txt"); s.put("b.txt")
        var n = 0
        try { DebianArchiveStaging.commit(s, dd, null) { ++n > 1 } } catch (e: IOException) { }
        check("commit batal HERE: hanya yang dibuat dibatalkan", File(dd, "keep.txt").readText() == "K" && !File(dd, "a.txt").exists() && !File(dd, "b.txt").exists())
    }
    check("commit: stage bukan anak dest ditolak", run { val dd = tmp(); val s = tmp(); throwsIo { DebianArchiveStaging.commit(s, dd, null) { false } } })

    // ---- roomError
    val rd = tmp()
    check("roomError: wajar OK", DebianArchiveStaging.roomError(rd, 1024) == null)
    check("roomError: terlalu besar", DebianArchiveStaging.roomError(rd, Long.MAX_VALUE / 4) != null)

    println(if (fails == 0) "SEMUA LULUS" else "$fails GAGAL")
    if (fails > 0) System.exit(1)
}
