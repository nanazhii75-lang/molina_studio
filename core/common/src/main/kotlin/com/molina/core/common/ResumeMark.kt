package com.molina.core.common

import java.io.File
import java.security.MessageDigest

/**
 * Penanda "sudah pernah dilanjutkan" untuk kartu rak film. mpv menulis posisi terakhir lewat
 * `write-watch-later-config` ke berkas bernama MD5 (heksa huruf besar) dari path/URL, isinya `start=<detik>`.
 * Murni JVM; semua fungsi tahan berkas hilang/rusak (hasil null, tidak melempar).
 */
object ResumeMark {
    /** Posisi di bawah ini dianggap belum dilanjutkan (mpv bisa menulis start=0 / nilai sangat kecil). */
    const val MIN_START_SEC = 1.0
    private const val MAX_BYTES = 4096L

    /** Nama berkas watch_later mpv untuk [ref]: MD5 heksa huruf besar. */
    fun key(ref: String): String =
        MessageDigest.getInstance("MD5").digest(ref.toByteArray(Charsets.UTF_8)).joinToString("") { "%02X".format(it) }

    /** Posisi lanjut (detik) dari [dirs] pertama yang punya berkasnya; null bila tidak ada atau tidak sah. */
    fun readStart(dirs: List<File>, ref: String): Double? {
        if (ref.isBlank()) return null
        val name = key(ref)
        for (d in dirs) {
            val f = File(d, name)
            val v = try {
                if (!f.isFile || f.length() > MAX_BYTES) null else parseStart(f.readText(Charsets.UTF_8))
            } catch (e: java.io.IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
            if (v != null) return v
        }
        return null
    }

    /** Ambil nilai `start=` pertama yang terbatas dan >= [MIN_START_SEC]. */
    fun parseStart(text: String): Double? {
        for (line in text.lineSequence()) {
            val t = line.trim()
            if (!t.startsWith("start=")) continue
            val v = t.removePrefix("start=").trim().toDoubleOrNull() ?: return null
            return v.takeIf { it.isFinite() && it >= MIN_START_SEC }
        }
        return null
    }

    /** Pecahan 0..1 untuk garis aksen; null bila durasi tidak diketahui atau posisi tidak masuk akal. */
    fun fraction(startSec: Double?, durationSec: Int?): Float? {
        if (startSec == null || durationSec == null || durationSec <= 0) return null
        if (startSec >= durationSec) return null
        return (startSec / durationSec).toFloat().coerceIn(0.02f, 0.98f)
    }
}
