package com.molina.core.common

/** Penyaring URL untuk jalur yt-dlp: URL media langsung tidak perlu memulai proses proot. */
object YtdlUrls {
    private val DIRECT_EXT = setOf(
        "m3u8", "mpd", "mp4", "m4v", "mkv", "webm", "mov", "avi", "flv", "ts",
        "mp3", "m4a", "aac", "opus", "ogg", "flac", "wav",
    )

    /** True bila [url] adalah halaman web (http/https) yang perlu diurai yt-dlp; false untuk media langsung. */
    fun needsYtdl(url: String): Boolean {
        val u = url.trim()
        val scheme = u.substringBefore("://", "").lowercase()
        if (scheme != "http" && scheme != "https") return false
        val path = u.substringAfter("://").substringBefore('#').substringBefore('?')
        val last = path.substringAfter('/', "").substringAfterLast('/')
        val ext = if ('.' in last) last.substringAfterLast('.').lowercase() else ""
        return ext !in DIRECT_EXT
    }
}
