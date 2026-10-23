package com.molina.cinema

import android.os.SystemClock
import com.molina.core.common.LogRedact
import java.util.Locale

/**
 * Riwayat pemutaran untuk tab Log (sumber "mpv", tag "playback").
 * Semua target disamarkan lewat LogRedact: URL hanya skema+host, path lokal
 * hanya nama berkas. Aman dipanggil dari thread event mpv.
 *
 * Batasan: JNI hanya meneruskan id event (tanpa alasan end-file). Karena itu
 * "berakhir sebelum dimuat" dicatat sebagai kemungkinan gagal buka, bukan
 * kepastian galat.
 */
internal object PlaybackLog {
    private const val TAG = "playback"

    @Volatile private var target = ""
    @Volatile private var startedAt = 0L
    @Volatile private var loaded = false

    /** Permintaan putar baru dari intent (onNewIntent). */
    fun onRequest(raw: String) {
        MpvLog.i(TAG, "Permintaan putar: ${LogRedact.target(raw)}")
    }

    /** MPV_EVENT_START_FILE. */
    fun onStart() {
        target = prop("path")?.let { LogRedact.target(it) } ?: "(tidak diketahui)"
        startedAt = SystemClock.elapsedRealtime()
        loaded = false
        MpvLog.i(TAG, "Mulai: $target")
    }

    /** MPV_EVENT_FILE_LOADED. */
    fun onLoaded() {
        loaded = true
        val title = prop("media-title")?.let { LogRedact.text(it) }.orEmpty()
        val duration = runCatching { MPVLib.getPropertyDouble("duration") }.getOrNull()
        val sb = StringBuilder("Dimuat: ").append(target)
        if (duration != null && duration > 0)
            sb.append(" | durasi ").append(clock((duration * 1000).toLong()))
        if (title.isNotEmpty() && title != target)
            sb.append(" | judul ").append(title)
        MpvLog.i(TAG, sb.toString())
    }

    /** MPV_EVENT_END_FILE. Panggil SEBELUM psc.eof(). */
    fun onEnd(positionMs: Long, durationMs: Long) {
        val elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000
        if (!loaded) {
            MpvLog.w(
                TAG,
                "Berakhir sebelum dimuat (kemungkinan gagal dibuka): " +
                    "$target setelah ${elapsed}s"
            )
            return
        }
        MpvLog.i(
            TAG,
            "Selesai: $target | posisi ${clock(positionMs)} dari ${clock(durationMs)}"
        )
    }

    private fun prop(name: String): String? =
        runCatching { MPVLib.getPropertyString(name) }.getOrNull()
            ?.takeIf { it.isNotEmpty() }

    private fun clock(ms: Long): String {
        if (ms < 0) return "?"
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, s)
        else "%d:%02d".format(Locale.ROOT, m, s)
    }
}
