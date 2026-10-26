package com.molina.core.common

/** Aliran data yang sedang diunduh. SINGLE = satu berkas gabungan (tanpa pemisahan video/audio). */
enum class DownloadStream { VIDEO, AUDIO, SINGLE }

data class DownloadedFile(val name: String, val path: String, val sizeBytes: Long)

/**
 * Event dari `molina_dl.py --json` (protokol v1): satu objek JSON per baris stdout.
 * Murni JVM supaya bisa diuji tanpa Android. Indeks item berbasis 1; 0 = galat global.
 */
sealed interface DownloadEvent {
    data class Hello(val version: Int) : DownloadEvent
    data class Warn(val code: String, val msg: String) : DownloadEvent
    data class Queue(val total: Int) : DownloadEvent
    data class Item(val index: Int, val total: Int, val url: String) : DownloadEvent
    data class Info(
        val index: Int,
        val title: String?,
        val id: String?,
        val durationSec: Long?,
        val uploader: String?,
        val thumbnail: String?,
        val heights: List<Int>,
    ) : DownloadEvent

    data class Progress(
        val index: Int,
        val stream: DownloadStream,
        val percent: Double?,
        val doneBytes: Long?,
        val totalBytes: Long?,
        val speedBps: Long?,
        val etaSec: Long?,
    ) : DownloadEvent

    data class Stage(val index: Int, val stage: String) : DownloadEvent
    data class Done(val index: Int, val dir: String, val existed: Boolean, val files: List<DownloadedFile>) : DownloadEvent
    data class Error(val index: Int, val code: String, val msg: String) : DownloadEvent
    data class Summary(val ok: Int, val failed: Int, val cancelled: Boolean) : DownloadEvent
}

object DownloadEventParser {
    const val PROTOCOL = 1

    /** Satu baris stdout -> event; null untuk baris non-JSON, versi protokol lain, atau event tak dikenal. */
    fun parse(line: String): DownloadEvent? {
        val t = line.trim()
        if (t.isEmpty() || t[0] != '{') return null
        val o = MiniJson.parseObject(t) ?: return null
        if (o.int("v") != PROTOCOL) return null
        return when (o["ev"] as? String) {
            "hello" -> DownloadEvent.Hello(o.int("version") ?: 0)
            "warn" -> DownloadEvent.Warn(o.str("code") ?: "", o.str("msg") ?: "")
            "queue" -> DownloadEvent.Queue(o.int("total") ?: 0)
            "item" -> DownloadEvent.Item(o.int("i") ?: 0, o.int("n") ?: 0, o.str("url") ?: "")
            "info" -> DownloadEvent.Info(
                index = o.int("i") ?: 0,
                title = o.str("title"),
                id = o.str("id"),
                durationSec = o.long("duration"),
                uploader = o.str("uploader"),
                thumbnail = o.str("thumbnail"),
                heights = (o["heights"] as? List<*>).orEmpty().mapNotNull { numToLong(it)?.toInt() },
            )
            "progress" -> DownloadEvent.Progress(
                index = o.int("i") ?: 0,
                stream = when (o["stream"]) {
                    "video" -> DownloadStream.VIDEO
                    "audio" -> DownloadStream.AUDIO
                    else -> DownloadStream.SINGLE
                },
                percent = (o["pct"] as? Number)?.toDouble()?.takeIf { it.isFinite() },
                doneBytes = o.long("done"),
                totalBytes = o.long("total"),
                speedBps = o.long("speed"),
                etaSec = o.long("eta"),
            )
            "stage" -> DownloadEvent.Stage(o.int("i") ?: 0, o.str("stage") ?: "")
            "done" -> DownloadEvent.Done(
                index = o.int("i") ?: 0,
                dir = o.str("dir") ?: "",
                existed = o["existed"] == true,
                files = (o["files"] as? List<*>).orEmpty().mapNotNull { f ->
                    @Suppress("UNCHECKED_CAST")
                    val m = f as? Map<String, Any?> ?: return@mapNotNull null
                    val path = m.str("path") ?: return@mapNotNull null
                    DownloadedFile(m.str("name") ?: path.substringAfterLast('/'), path, m.long("size") ?: 0L)
                },
            )
            "error" -> DownloadEvent.Error(o.int("i") ?: 0, o.str("code") ?: "", o.str("msg") ?: "")
            "summary" -> DownloadEvent.Summary(o.int("ok") ?: 0, o.int("failed") ?: 0, o["cancelled"] == true)
            else -> null
        }
    }

    private fun numToLong(v: Any?): Long? = when (v) {
        is Long -> v
        is Double -> if (v.isFinite()) v.toLong() else null
        else -> null
    }

    private fun Map<String, Any?>.str(k: String): String? = this[k] as? String
    private fun Map<String, Any?>.long(k: String): Long? = numToLong(this[k])
    private fun Map<String, Any?>.int(k: String): Int? = numToLong(this[k])?.toInt()
}
