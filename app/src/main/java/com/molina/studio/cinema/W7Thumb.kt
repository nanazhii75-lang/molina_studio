package com.molina.studio.cinema

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [FILES-THUMB] Thumbnail video kecil untuk panel Penjelajah. Dekode selalu di Dispatchers.IO,
 * hasil disimpan di LruCache (32 entri, bitmap maks. 160x90) sehingga thread utama tidak terblokir.
 * Kegagalan (codec tidak didukung, berkas rusak) dicatat agar tidak dicoba berulang.
 */
internal object W7Thumb {
    private val VIDEO = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts", "m2ts", "mts", "flv", "wmv", "mpg", "mpeg", "ogv", "vob", "rmvb"
    )
    private const val MAX_W = 160
    private const val MAX_H = 90
    private val cache = LruCache<String, Bitmap>(32)
    private val failed: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet<String>())

    fun isVideo(f: File): Boolean = f.isFile && f.extension.lowercase() in VIDEO

    suspend fun load(f: File): Bitmap? {
        val key = f.path + "|" + f.lastModified()
        cache.get(key)?.let { return it }
        if (key in failed) return null
        val bmp = withContext(Dispatchers.IO) { extract(f) }
        if (bmp != null) cache.put(key, bmp) else failed.add(key)
        return bmp
    }

    private fun extract(f: File): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(f.path)
            val durMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val atUs = if (durMs > 0L) durMs * 100L else 0L // 10% dari durasi
            val frame = r.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: r.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            val fw = frame.width
            val fh = frame.height
            if (fw <= 0 || fh <= 0) return null
            val s = minOf(MAX_W.toFloat() / fw, MAX_H.toFloat() / fh, 1f)
            if (s >= 1f) frame else {
                val out = Bitmap.createScaledBitmap(frame, maxOf(1, (fw * s).toInt()), maxOf(1, (fh * s).toInt()), true)
                if (out !== frame) frame.recycle()
                out
            }
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (e: Exception) { }
        }
    }
}
