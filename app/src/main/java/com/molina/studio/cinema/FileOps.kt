package com.molina.studio.cinema

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Operasi berkas murni untuk Penjelajah Molina (tanpa Android/Compose). Semua fungsi bersifat blocking:
 * panggil dari Dispatchers.IO. Kegagalan dilaporkan lewat IOException berpesan bahasa Indonesia.
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

    private fun isLink(f: File): Boolean = try { Files.isSymbolicLink(f.toPath()) } catch (e: Exception) { false }
    private fun present(f: File): Boolean = f.exists() || isLink(f)

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

    private fun checkDest(src: File, destDir: File) {
        if (!present(src)) throw IOException("Sumber tidak ditemukan: " + src.name)
        if (!destDir.isDirectory) throw IOException("Folder tujuan tidak ditemukan")
        if (!destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        if (src.isDirectory && !isLink(src) && isInside(src, destDir)) {
            throw IOException("Folder tidak bisa dimasukkan ke dalam dirinya sendiri")
        }
    }

    private fun copyRec(src: File, dst: File) {
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
            for (k in kids) copyRec(k, File(dst, k.name))
        } else {
            src.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
        }
        dst.setLastModified(src.lastModified())
    }

    private fun deleteRec(f: File): Boolean {
        if (!isLink(f) && f.isDirectory) {
            val kids = f.listFiles()
            if (kids != null) for (k in kids) if (!deleteRec(k)) return false
        }
        return f.delete()
    }

    /** Salin [src] ke [destDir]. Nama bentrok otomatis diberi akhiran " (1)". Mengembalikan hasil salinan. */
    fun copyTo(src: File, destDir: File): File {
        checkDest(src, destDir)
        val target = uniqueTarget(destDir, src.name, keepExt = !src.isDirectory)
        try {
            copyRec(src, target)
        } catch (e: Exception) {
            deleteRec(target)
            throw if (e is IOException) e else IOException(e.message ?: "Gagal menyalin")
        }
        return target
    }

    /** Pindahkan [src] ke [destDir]: rename cepat, bila beda volume salin lalu hapus sumber. */
    fun moveTo(src: File, destDir: File): File {
        checkDest(src, destDir)
        if (src.parentFile?.canonicalFile == destDir.canonicalFile) {
            throw IOException("Berkas sudah berada di folder ini")
        }
        val target = uniqueTarget(destDir, src.name, keepExt = !src.isDirectory)
        if (src.renameTo(target)) return target
        try {
            copyRec(src, target)
        } catch (e: Exception) {
            deleteRec(target)
            throw if (e is IOException) e else IOException(e.message ?: "Gagal memindahkan")
        }
        if (!deleteRec(src)) throw IOException("Sudah disalin ke tujuan, tetapi sumber tidak bisa dihapus")
        return target
    }

    /** Ubah nama [src] (hanya nama, tanpa jalur). Mengembalikan File baru. */
    fun rename(src: File, newName: String): File {
        val n = newName.trim()
        if (n.isEmpty()) throw IOException("Nama tidak boleh kosong")
        if (n == "." || n == ".." || n.contains('/') || n.contains('\u0000')) throw IOException("Nama tidak valid")
        if (n.length > 255) throw IOException("Nama terlalu panjang (maks. 255 karakter)")
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

    /** Kompres [src] (berkas atau folder) menjadi ZIP di [destDir]. Tautan simbolik dilewati. */
    fun zip(src: File, destDir: File): File {
        if (!present(src)) throw IOException("Sumber tidak ditemukan: " + src.name)
        if (!destDir.isDirectory || !destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        val stem = if (src.isDirectory) src.name else src.nameWithoutExtension.ifEmpty { src.name }
        val out = uniqueTarget(destDir, "$stem.zip", keepExt = true)
        try {
            ZipOutputStream(out.outputStream().buffered()).use { z -> addZip(z, src, src.name, out.canonicalFile) }
        } catch (e: Exception) {
            out.delete()
            throw if (e is IOException) e else IOException(e.message ?: "Gagal mengompres")
        }
        return out
    }

    private fun addZip(z: ZipOutputStream, f: File, path: String, skip: File) {
        if (isLink(f) || f.canonicalFile == skip) return
        if (f.isDirectory) {
            z.putNextEntry(ZipEntry("$path/").apply { time = f.lastModified() })
            z.closeEntry()
            val kids = f.listFiles() ?: return
            for (k in kids.sortedBy { it.name }) addZip(z, k, path + "/" + k.name, skip)
        } else {
            z.putNextEntry(ZipEntry(path).apply { time = f.lastModified() })
            f.inputStream().use { it.copyTo(z) }
            z.closeEntry()
        }
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
