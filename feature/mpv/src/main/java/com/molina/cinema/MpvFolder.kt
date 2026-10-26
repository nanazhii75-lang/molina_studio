package com.molina.cinema

import java.io.File

/**
 * Pintu publik untuk :app: memilih berkas media pertama (urutan alami) dalam sebuah folder.
 * Memakai daftar ekstensi dan pengurutan yang sama dengan Utils.folderPlaylist, sehingga
 * MPVActivity (extra folder_playlist) membangun playlist dari folder yang sama.
 */
object MpvFolder {
    private val SKIP = setOf("cue", "m3u", "m3u8", "pls", "strm", "vlc")

    /** Path berkas media pertama di [dir] (tidak rekursif), atau null bila tidak ada. */
    fun firstMediaIn(dir: File): String? {
        if (!dir.isDirectory) return null
        val files = dir.listFiles { f ->
            f.isFile && !f.name.startsWith(".") &&
                f.extension.lowercase().let { it in Utils.MEDIA_EXTENSIONS && it !in SKIP }
        } ?: return null
        return files.sortedWith { a, b -> Utils.naturalCompare(a.name, b.name) }.firstOrNull()?.path
    }
}
