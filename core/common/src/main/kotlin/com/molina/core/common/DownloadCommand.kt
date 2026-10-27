package com.molina.core.common

enum class DownloadMode(val arg: String) { VIDEO("video"), AUDIO("audio") }

/** Permintaan satu sesi unduhan dari UI. Divalidasi oleh [DownloadCommand.validate] sebelum dijalankan. */
data class DownloadRequest(
    val urls: List<String>,
    val mode: DownloadMode = DownloadMode.VIDEO,
    val height: Int = 0,
    val videoExt: String = "mp4",
    val audioFormat: String = "mp3",
    val bitrate: String = "192k",
    val dir: String? = null,
)

/** Argumen `molina_dl.py --json ...` untuk halaman Unduhan. Murni (tanpa Android). */
object DownloadCommand {
    const val SCRIPT_ASSET = "molina/molina_dl.py"
    const val MAX_URLS = 50
    const val MAX_URL_CHARS = 2048
    const val MAX_DIR_CHARS = 1024
    const val TIMEOUT_MS = 6L * 60 * 60 * 1000

    val VIDEO_EXTS = setOf("mp4", "mkv")
    val AUDIO_FORMATS = setOf("mp3", "m4a", "flac")
    val BITRATES = setOf("128k", "192k", "320k")

    private val URL_RE = Regex("^https?://[^\\s/\$.?#][^\\s]*\$", RegexOption.IGNORE_CASE)

    /** URL dipangkas, yang kosong dibuang, duplikat dibuang (urutan dipertahankan). */
    fun clean(urls: List<String>): List<String> = urls.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** Pesan galat siap tampil bila [req] tidak boleh dijalankan; null bila aman. Pesan tidak memuat query URL. */
    fun validate(req: DownloadRequest): String? {
        val urls = clean(req.urls)
        if (urls.isEmpty()) return "Tempel minimal satu URL."
        if (urls.size > MAX_URLS) return "Maksimal $MAX_URLS URL per sesi."
        urls.firstOrNull { it.length > MAX_URL_CHARS || it.any { c -> c.isISOControl() } || !URL_RE.matches(it) }?.let {
            return "URL tidak valid (harus diawali http:// atau https://): " + LogRedact.target(it).take(80)
        }
        if (req.height !in 0..4320) return "Tinggi video tidak valid."
        if (req.videoExt !in VIDEO_EXTS) return "Format video tidak didukung."
        if (req.audioFormat !in AUDIO_FORMATS) return "Format audio tidak didukung."
        if (req.bitrate !in BITRATES) return "Bitrate tidak didukung."
        req.dir?.let {
            if (!it.startsWith("/") || it.length > MAX_DIR_CHARS || it.any { c -> c.isISOControl() }) {
                return "Folder tujuan harus berupa jalur absolut."
            }
        }
        return null
    }

    /**
     * Argv skrip (setelah berkas skrip). Panggil [validate] lebih dulu. Semua URL ditaruh setelah `--`
     * sehingga tidak pernah terbaca sebagai opsi; tidak ada shell di jalur eksekusi.
     */
    fun args(req: DownloadRequest): List<String> = buildList {
        add("--json")
        add("--mode"); add(req.mode.arg)
        if (req.mode == DownloadMode.VIDEO) {
            if (req.height > 0) { add("--height"); add(req.height.toString()) }
            add("--ext"); add(req.videoExt)
        } else {
            add("--audio-format"); add(req.audioFormat)
            add("--bitrate"); add(req.bitrate)
        }
        req.dir?.let { add("--dir"); add(it) }
        add("--")
        addAll(clean(req.urls))
    }
}
