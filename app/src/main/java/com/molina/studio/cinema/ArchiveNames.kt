package com.molina.studio.cinema

import java.io.File
import java.util.Locale

/** [ARCHIVE] Format arsip yang dikenali Penjelajah Molina. */
internal enum class ArchiveFormat(val label: String) {
    ZIP("ZIP"), SEVEN_Z("7Z"), TAR("TAR"), TAR_GZ("TAR.GZ"), TAR_BZ2("TAR.BZ2"), TAR_XZ("TAR.XZ"),
    GZ("GZ"), BZ2("BZ2"), XZ("XZ");

    val isTar: Boolean get() = this == TAR || this == TAR_GZ || this == TAR_BZ2 || this == TAR_XZ
    /** gz/bz2/xz tanpa tar: satu berkas terkompresi. */
    val isSingle: Boolean get() = this == GZ || this == BZ2 || this == XZ
    /** Format yang bisa berkata sandi. */
    val hasPassword: Boolean get() = this == ZIP || this == SEVEN_Z
}

/** [ARCHIVE] Tujuan ekstrak: langsung ke folder tujuan, atau ke subfolder bernama sama dengan arsip. */
internal enum class ExtractTarget { HERE, NAMED_FOLDER }

/**
 * [ARCHIVE] Logika nama/jalur murni (tanpa Android, tanpa pustaka arsip) supaya bisa diuji di JVM.
 * Semua nama entri dari arsip dianggap tidak tepercaya: [cleanEntry] menolak ".." dan [safeTarget] memeriksa jalur kanonik.
 */
internal object ArchiveNames {
    /** Urutan penting: sufiks majemuk (.tar.gz) harus dicek sebelum sufiks tunggal (.gz). */
    private val SUFFIXES: List<Pair<String, ArchiveFormat>> = listOf(
        ".tar.gz" to ArchiveFormat.TAR_GZ, ".tgz" to ArchiveFormat.TAR_GZ,
        ".tar.bz2" to ArchiveFormat.TAR_BZ2, ".tbz2" to ArchiveFormat.TAR_BZ2, ".tbz" to ArchiveFormat.TAR_BZ2,
        ".tar.xz" to ArchiveFormat.TAR_XZ, ".txz" to ArchiveFormat.TAR_XZ,
        ".tar" to ArchiveFormat.TAR, ".7z" to ArchiveFormat.SEVEN_Z, ".zip" to ArchiveFormat.ZIP,
        ".gz" to ArchiveFormat.GZ, ".bz2" to ArchiveFormat.BZ2, ".xz" to ArchiveFormat.XZ
    )

    private fun match(name: String): Pair<String, ArchiveFormat>? {
        val l = name.lowercase(Locale.ROOT)
        return SUFFIXES.firstOrNull { l.endsWith(it.first) && l.length > it.first.length }
    }

    fun detect(name: String): ArchiveFormat? = match(name)?.second

    /** Nama tanpa sufiks arsip ("a.tar.gz" -> "a", "notes.txt.gz" -> "notes.txt"); "Ekstrak" bila kosong. */
    fun stem(name: String): String {
        val m = match(name) ?: return name.ifEmpty { "Ekstrak" }
        return name.substring(0, name.length - m.first.length).ifEmpty { "Ekstrak" }
    }

    /** Nama subfolder tujuan. Untuk gz/bz2/xz tunggal ekstensi isi dibuang ("notes.txt.gz" -> "notes"). */
    fun folderName(name: String): String {
        val s = stem(name)
        val fmt = detect(name)
        if (fmt != null && fmt.isSingle) {
            val cut = s.substringBeforeLast('.', s)
            if (cut.isNotEmpty()) return cut
        }
        return s
    }

    /**
     * Bersihkan nama entri: pemisah Windows jadi "/", awalan "/" dan "./" dibuang, segmen kosong dan "." dilewati.
     * Mengembalikan null bila ada segmen ".." atau karakter NUL (tidak aman), "" bila entri hanya akar (abaikan saja).
     */
    fun cleanEntry(raw: String): String? {
        if (raw.indexOf('\u0000') >= 0) return null
        val parts = ArrayList<String>()
        for (seg in raw.replace('\\', '/').split('/')) {
            if (seg.isEmpty() || seg == ".") continue
            if (seg == "..") return null
            parts.add(seg)
        }
        return parts.joinToString("/")
    }

    /** Apakah entri [clean] termasuk pilihan [only]? null = semua. Kunci folder berakhiran "/" dan mencakup isinya. */
    fun selected(clean: String, isDir: Boolean, only: Set<String>?): Boolean {
        if (only == null) return true
        val key = if (isDir) clean.trimEnd('/') + "/" else clean
        if (key in only) return true
        for (o in only) if (o.endsWith("/") && key.startsWith(o)) return true
        return false
    }

    /** Jalur tujuan entri di bawah [base]; null bila jalur kanonik keluar dari [base] (zip-slip lewat symlink yang sudah ada). */
    fun safeTarget(base: File, clean: String): File? {
        val root = base.canonicalFile
        val out = File(root, clean).canonicalFile
        return if (out.path == root.path || out.path.startsWith(root.path + File.separator)) out else null
    }

    /** "nama (1).ext", "nama (2).ext", ... di [dir] sampai belum dipakai. */
    fun uniqueIn(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (true) {
            f = File(dir, "$base ($i)$ext")
            if (!f.exists()) return f
            i++
        }
    }
}
