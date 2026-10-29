package com.molina.studio.cmd

/**
 * Protokol CLI `molina` (Debian -> aplikasi). Kotlin murni agar bisa diuji di JVM.
 * Satu permintaan per koneksi: "verb[TAB]argumen\n". Balasan satu baris:
 *   "OK[ teks]\n" atau "ERR kode teks\n".
 */
object CommandProtocol {
    /** Abstract-namespace Unix socket (sama gaya dengan @molina.log). */
    const val SOCKET_NAME = "molina.cmd"
    const val VERSION = 1
    const val MAX_LINE = 4096

    enum class Verb(val wire: String) {
        PING("ping"), VERSION("version"), STATUS("status"), PLAY("play"), STOP("stop")
    }

    class Request(val verb: Verb, val arg: String)

    sealed interface Parsed {
        class Ok(val req: Request) : Parsed
        class Bad(val code: String, val message: String) : Parsed
    }

    fun parse(line: String): Parsed {
        val t = line.trimEnd('\r', '\n')
        if (t.isBlank()) return Parsed.Bad("empty", "perintah kosong")
        val tab = t.indexOf('\t')
        val word = (if (tab < 0) t else t.substring(0, tab)).trim().lowercase()
        val arg = if (tab < 0) "" else t.substring(tab + 1).trim()
        val verb = Verb.entries.firstOrNull { it.wire == word }
            ?: return Parsed.Bad("unknown", "perintah tidak dikenal: ${oneLine(word)}")
        if (verb == Verb.PLAY && arg.isEmpty()) return Parsed.Bad("usage", "play butuh path atau URL")
        return Parsed.Ok(Request(verb, arg))
    }

    fun ok(text: String = ""): String = if (text.isEmpty()) "OK\n" else "OK ${oneLine(text)}\n"

    fun err(code: String, text: String): String = "ERR $code ${oneLine(text)}\n"

    /** Satu baris, tanpa TAB/CR/LF, maks 300 karakter (balasan dan log tidak boleh memecah protokol). */
    fun oneLine(s: String): String = s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').take(300)

    /**
     * Path yang terlihat di Debian (/sdcard atau /storage/emulated/0) -> path Android di bawah
     * [storageRoot]. Null bila bukan path mutlak, mengandung NUL atau "..", atau di luar dua akar itu.
     */
    fun guestToHost(guest: String, storageRoot: String): String? {
        val g = guest.trim()
        if (!g.startsWith("/") || g.indexOf('\u0000') >= 0) return null
        val rest = when {
            g == "/sdcard" || g == "/storage/emulated/0" -> ""
            g.startsWith("/sdcard/") -> g.substring("/sdcard".length)
            g.startsWith("/storage/emulated/0/") -> g.substring("/storage/emulated/0".length)
            else -> return null
        }
        val parts = rest.split('/').filter { it.isNotEmpty() && it != "." }
        if (parts.any { it == ".." }) return null
        val root = storageRoot.trimEnd('/')
        return if (parts.isEmpty()) root else root + "/" + parts.joinToString("/")
    }
}
