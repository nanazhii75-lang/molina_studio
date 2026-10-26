package com.molina.studio.cinema

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Operasi berkas murni untuk Penjelajah Molina (tanpa Android/Compose). Semua fungsi bersifat blocking:
 * panggil dari Dispatchers.IO. Kegagalan dilaporkan lewat IOException berpesan bahasa Indonesia.
 * Operasi panjang menerima [Ctl] untuk progres dan pembatalan.
 */
internal object FileOps {
    class Props(
        val file: File,
        val isDir: Boolean,
        val size: Long,
        val files: Int,
        val folders: Int,
        val modified: Long,
        val canRead: Boolean,
        val canWrite: Boolean,
        val symlink: Boolean
    )

    /** Kendali operasi panjang: [total] byte (diisi operasi), progres 0..1 dilaporkan per persen, [check] melempar bila dibatalkan. */
    class Ctl(private val cancel: AtomicBoolean, private val report: (Float) -> Unit) {
        @Volatile var total = 0L
        private var done = 0L
        private var last = -1
        fun check() { if (cancel.get()) throw IOException("Dibatalkan") }
        fun add(n: Long) {
            done += n
            val t = total
            if (t > 0) {
                val p = (done * 100 / t).coerceIn(0L, 100L).toInt()
                if (p != last) { last = p; report(p / 100f) }
            }
        }
    }

    private fun isLink(f: File): Boolean = try { Files.isSymbolicLink(f.toPath()) } catch (e: Exception) { false }
    private fun present(f: File): Boolean = f.exists() || isLink(f)
    private fun asIo(e: Exception, fallback: String): IOException = if (e is IOException) e else IOException(e.message ?: fallback)

    /** True bila [child] sama dengan [parent] atau berada di dalamnya (jalur kanonik). */
    fun isInside(parent: File, child: File): Boolean {
        val p = parent.canonicalFile.path
        val c = child.canonicalFile.path
        return c == p || c.startsWith(p + File.separator)
    }

    /** Nama bebas bentrok di [dir]: "nama (1).ext", "nama (2).ext", dst. */
    fun uniqueTarget(dir: File, name: String, keepExt: Boolean = true): File {
        var f = File(dir, name)
        if (!present(f)) return f
        val dot = name.lastIndexOf('.')
        val base = if (keepExt && dot > 0) name.substring(0, dot) else name
        val ext = if (keepExt && dot > 0) name.substring(dot) else ""
        var i = 1
        while (true) {
            f = File(dir, "$base ($i)$ext")
            if (!present(f)) return f
            i++
        }
    }

    private fun validName(raw: String): String {
        val n = raw.trim()
        if (n.isEmpty()) throw IOException("Nama tidak boleh kosong")
        if (n == "." || n == ".." || n.contains('/') || n.contains('\u0000')) throw IOException("Nama tidak valid")
        if (n.length > 255) throw IOException("Nama terlalu panjang (maks. 255 karakter)")
        return n
    }

    private fun pump(i: InputStream, o: OutputStream, ctl: Ctl) {
        val buf = ByteArray(128 * 1024)
        while (true) {
            ctl.check()
            val n = i.read(buf)
            if (n < 0) break
            o.write(buf, 0, n)
            ctl.add(n.toLong())
        }
    }

    private fun checkDest(src: File, destDir: File) {
        if (!present(src)) throw IOException("Sumber tidak ditemukan: " + src.name)
        if (!destDir.isDirectory) throw IOException("Folder tujuan tidak ditemukan")
        if (!destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        if (src.isDirectory && !isLink(src) && isInside(src, destDir)) {
            throw IOException("Folder tidak bisa dimasukkan ke dalam dirinya sendiri")
        }
    }

    private fun copyRec(src: File, dst: File, ctl: Ctl) {
        ctl.check()
        if (isLink(src)) {
            try {
                Files.createSymbolicLink(dst.toPath(), Files.readSymbolicLink(src.toPath()))
            } catch (e: Exception) {
                throw IOException("Tautan simbolik tidak dapat disalin: " + src.name)
            }
            return
        }
        if (src.isDirectory) {
            if (!dst.mkdirs() && !dst.isDirectory) throw IOException("Gagal membuat folder " + dst.name)
            val kids = src.listFiles() ?: throw IOException("Folder tidak dapat dibaca: " + src.name)
            for (k in kids) copyRec(k, File(dst, k.name), ctl)
        } else {
            src.inputStream().use { i -> dst.outputStream().use { o -> pump(i, o, ctl) } }
        }
        dst.setLastModified(src.lastModified())
    }

    private fun deleteRec(f: File, ctl: Ctl? = null): Boolean {
        ctl?.check()
        if (!isLink(f) && f.isDirectory) {
            val kids = f.listFiles()
            if (kids != null) for (k in kids) if (!deleteRec(k, ctl)) return false
        }
        return f.delete()
    }

    /** Total byte semua isi [files] (tautan simbolik tidak diikuti). */
    fun sizeOf(files: List<File>): Long {
        var s = 0L
        fun walk(f: File) {
            if (isLink(f)) return
            if (f.isDirectory) f.listFiles()?.forEach { walk(it) } else s += f.length()
        }
        files.forEach { walk(it) }
        return s
    }

    /** Salin semua [files] ke [destDir]. Nama bentrok diberi akhiran " (1)". Mengembalikan hasil salinan. */
    fun copyAll(files: List<File>, destDir: File, ctl: Ctl): List<File> {
        ctl.total = sizeOf(files)
        val out = ArrayList<File>()
        for (f in files) {
            ctl.check()
            checkDest(f, destDir)
            val t = uniqueTarget(destDir, f.name, keepExt = !f.isDirectory)
            try {
                copyRec(f, t, ctl)
            } catch (e: Exception) {
                deleteRec(t)
                throw asIo(e, "Gagal menyalin")
            }
            out.add(t)
        }
        return out
    }

    /** Pindahkan semua [files] ke [destDir]: rename cepat; bila beda volume salin lalu hapus sumber. */
    fun moveAll(files: List<File>, destDir: File, ctl: Ctl): List<File> {
        ctl.total = sizeOf(files)
        val out = ArrayList<File>()
        for (f in files) {
            ctl.check()
            checkDest(f, destDir)
            if (f.parentFile?.canonicalFile == destDir.canonicalFile) {
                throw IOException("\"" + f.name + "\" sudah berada di folder ini")
            }
            val t = uniqueTarget(destDir, f.name, keepExt = !f.isDirectory)
            val sz = sizeOf(listOf(f))
            if (f.renameTo(t)) { ctl.add(sz); out.add(t); continue }
            try {
                copyRec(f, t, ctl)
            } catch (e: Exception) {
                deleteRec(t)
                throw asIo(e, "Gagal memindahkan")
            }
            if (!deleteRec(f)) throw IOException("Sudah disalin ke tujuan, tetapi \"" + f.name + "\" tidak bisa dihapus")
            out.add(t)
        }
        return out
    }

    /** Hapus semua [files]. Jalur di [guard] (jalur absolut) dilindungi dan ditolak. Mengembalikan jumlah item terhapus. */
    fun deleteAll(files: List<File>, guard: Set<String>, ctl: Ctl): Int {
        for (f in files) if (f.absolutePath in guard) throw IOException("Folder ini dilindungi dan tidak bisa dihapus: " + f.name)
        var n = 0
        for (f in files) {
            ctl.check()
            if (!present(f)) continue
            if (!deleteRec(f, ctl)) throw IOException("Gagal menghapus: " + f.name)
            n++
        }
        return n
    }

    /** Buat folder [name] di [parent]. */
    fun mkdir(parent: File, name: String): File {
        val n = validName(name)
        if (!parent.isDirectory || !parent.canWrite()) throw IOException("Folder ini tidak bisa ditulisi")
        val t = File(parent, n)
        if (present(t)) throw IOException("Sudah ada \"" + n + "\" di folder ini")
        if (!t.mkdir()) throw IOException("Gagal membuat folder (periksa izin tulis)")
        return t
    }

    /** Ubah nama [src] (hanya nama, tanpa jalur). Mengembalikan File baru. */
    fun rename(src: File, newName: String): File {
        val n = validName(newName)
        if (!present(src)) throw IOException("Sumber tidak ditemukan: " + src.name)
        if (n == src.name) return src
        val parent = src.parentFile ?: throw IOException("Folder induk tidak ditemukan")
        val target = File(parent, n)
        // Penyimpanan bersama tidak peka huruf besar/kecil: ganti kapitalisasi nama sendiri tetap diizinkan.
        if (!n.equals(src.name, ignoreCase = true) && present(target)) {
            throw IOException("Sudah ada \"" + n + "\" di folder ini")
        }
        if (!src.renameTo(target)) throw IOException("Gagal mengubah nama (periksa izin tulis)")
        return target
    }

    /** Kompres [files] menjadi satu ZIP di [destDir]. Satu item: nama item; banyak: nama folder tujuan. Tautan dilewati. */
    fun zipAll(files: List<File>, destDir: File, ctl: Ctl): File {
        if (files.isEmpty()) throw IOException("Tidak ada yang dikompres")
        for (f in files) if (!present(f)) throw IOException("Sumber tidak ditemukan: " + f.name)
        if (!destDir.isDirectory || !destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        val one = files.singleOrNull()
        val stem = if (one != null) {
            if (one.isDirectory) one.name else one.nameWithoutExtension.ifEmpty { one.name }
        } else destDir.name.ifEmpty { "Arsip" }
        val out = uniqueTarget(destDir, "$stem.zip", keepExt = true)
        ctl.total = sizeOf(files)
        try {
            ZipOutputStream(out.outputStream().buffered()).use { z ->
                val skip = out.canonicalFile
                for (f in files) addZip(z, f, f.name, skip, ctl)
            }
        } catch (e: Exception) {
            out.delete()
            throw asIo(e, "Gagal mengompres")
        }
        return out
    }

    private fun addZip(z: ZipOutputStream, f: File, path: String, skip: File, ctl: Ctl) {
        ctl.check()
        if (isLink(f) || f.canonicalFile == skip) return
        if (f.isDirectory) {
            z.putNextEntry(ZipEntry("$path/").apply { time = f.lastModified() })
            z.closeEntry()
            val kids = f.listFiles() ?: return
            for (k in kids.sortedBy { it.name }) addZip(z, k, path + "/" + k.name, skip, ctl)
        } else {
            z.putNextEntry(ZipEntry(path).apply { time = f.lastModified() })
            f.inputStream().use { pump(it, z, ctl) }
            z.closeEntry()
        }
    }

    /** Ekstrak [zip] ke folder baru bernama sama di [destDir]. Entri yang keluar dari folder tujuan (zip-slip) ditolak. */
    fun unzip(zip: File, destDir: File, ctl: Ctl): File {
        if (!zip.isFile) throw IOException("Berkas ZIP tidak ditemukan")
        if (!destDir.isDirectory || !destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        val target = uniqueTarget(destDir, zip.nameWithoutExtension.ifEmpty { "Ekstrak" }, keepExt = false)
        val base = target.canonicalFile
        try {
            ZipFile(zip).use { zf ->
                val entries = zf.entries().toList()
                var total = 0L
                for (en in entries) if (!en.isDirectory && en.size > 0) total += en.size
                ctl.total = total
                if (!target.mkdirs()) throw IOException("Gagal membuat folder " + target.name)
                for (en in entries) {
                    ctl.check()
                    val out = File(target, en.name).canonicalFile
                    if (out.path != base.path && !out.path.startsWith(base.path + File.separator)) {
                        throw IOException("Entri ZIP tidak aman: " + en.name)
                    }
                    if (en.isDirectory) { out.mkdirs(); continue }
                    out.parentFile?.mkdirs()
                    zf.getInputStream(en).use { i -> out.outputStream().use { o -> pump(i, o, ctl) } }
                    if (en.time > 0) out.setLastModified(en.time)
                }
            }
        } catch (e: Exception) {
            deleteRec(target)
            throw asIo(e, "Gagal mengekstrak")
        }
        return target
    }

    /** Properti [f]; untuk folder ukuran dan isi dihitung rekursif (tautan dihitung sebagai berkas, tidak diikuti). */
    fun props(f: File): Props {
        var size = 0L
        var files = 0
        var folders = 0
        fun walk(d: File) {
            val kids = d.listFiles() ?: return
            for (k in kids) {
                if (isLink(k)) { files++; continue }
                if (k.isDirectory) { folders++; walk(k) } else { files++; size += k.length() }
            }
        }
        val link = isLink(f)
        val dir = f.isDirectory && !link
        if (dir) walk(f) else size = f.length()
        return Props(f, dir, size, files, folders, f.lastModified(), f.canRead(), f.canWrite(), link)
    }

    fun readable(b: Long): String {
        if (b < 1024) return "$b B"
        val u = arrayOf("KB", "MB", "GB", "TB")
        var v = b.toDouble()
        var i = -1
        do { v /= 1024.0; i++ } while (v >= 1024.0 && i < u.lastIndex)
        return String.format(Locale("id", "ID"), "%.1f %s", v, u[i])
    }
}
