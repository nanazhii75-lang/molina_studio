package com.molina.code.preview

/**
 * [CODE-PYRUN] Aturan murni (tanpa Android) untuk "Jalankan di Terminal" berkas Python:
 * mode HEREDOC (`python3 << 'PYEOF'` ... `PYEOF`) dan mode FILE (`python3 nama.py`).
 * Diuji di JVM (scripts/code-tests/PythonRunTests.kt).
 */
object PythonTerminalRun {
    const val DELIMITER = "PYEOF"
    const val MAX_NAME = 60

    /** Hasil normalisasi nama: [name] berisi nama berkas ber-ekstensi .py, atau [error] berisi alasan. */
    class PyName(val name: String?, val error: String?)

    /** Ekstensi .py (tanpa membedakan huruf besar/kecil). */
    fun isPythonName(name: String?): Boolean =
        name != null && name.length > 3 && name.endsWith(".py", ignoreCase = true)

    /** Nama untuk kolom isian: nama berkas tanpa .py (kosong bila belum ada nama). */
    fun suggestStem(name: String?): String {
        val n = name?.trim().orEmpty()
        val stem = if (n.endsWith(".py", ignoreCase = true)) n.dropLast(3) else n
        return if (stem.isEmpty() || stem.startsWith("Untitled", ignoreCase = true)) "skrip" else stem
    }

    /**
     * Pengguna cukup mengisi nama; ekstensi dideteksi otomatis: ".py" (huruf apa pun) dibuang lalu
     * ditambahkan lagi dalam huruf kecil, jadi `tes`, `tes.py` dan `tes.PY` semuanya menjadi `tes.py`.
     */
    fun normalizeName(input: String): PyName {
        var s = input.trim()
        if (s.endsWith(".py", ignoreCase = true)) s = s.dropLast(3)
        if (s.isEmpty()) return PyName(null, "Isi nama berkas")
        if (s.length > MAX_NAME) return PyName(null, "Nama terlalu panjang (maks. $MAX_NAME karakter)")
        val ok = s.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '-' || it == '.' }
        if (!ok) return PyName(null, "Nama hanya boleh huruf, angka, titik, - dan _")
        if (s.startsWith('.') || s.endsWith('.') || ".." in s) return PyName(null, "Titik pada nama tidak valid")
        return PyName("$s.py", null)
    }

    /** Pembatas heredoc yang tidak bentrok dengan isi: PYEOF, PYEOF_1, PYEOF_2, ... */
    fun uniqueDelimiter(body: String): String {
        val lines = body.lineSequence().map { it.trimEnd() }.toHashSet()
        if (DELIMITER !in lines) return DELIMITER
        var i = 1
        while ("${DELIMITER}_$i" in lines) i++
        return "${DELIMITER}_$i"
    }

    /**
     * Mode 1: `python3 << 'PYEOF'` + isi + `PYEOF`. Pembatas dikutip sehingga shell tidak mengubah isi
     * ($, `, \). Null bila isi kosong. Catatan: stdin dipakai heredoc, jadi input() tidak berfungsi.
     */
    fun heredoc(text: String): String? {
        val body = ScriptLaunch.normalizeEol(text).trimEnd('\n')
        if (body.isBlank()) return null
        val d = uniqueDelimiter(body)
        return "python3 << '$d'\n$body\n$d"
    }

    /**
     * Mode 2: `python3 nama.py`, dijalankan dari folder berkas lewat subshell sehingga folder aktif
     * Terminal tidak berubah dan import/berkas relatif tetap benar. [guestPath] jalur absolut di Debian.
     */
    fun fileCommand(guestPath: String): String {
        val i = guestPath.lastIndexOf('/')
        val dir = if (i <= 0) "/" else guestPath.substring(0, i)
        val name = guestPath.substring(i + 1)
        return "(cd ${ScriptLaunch.shellQuote(dir)} && python3 ${ScriptLaunch.shellQuote(name)})"
    }

    /** Pratinjau singkat perintah untuk dialog: maksimal [maxLines] baris, sisanya "...". */
    fun preview(command: String, maxLines: Int = 4): String {
        val lines = command.lines()
        return if (lines.size <= maxLines) command else lines.take(maxLines).joinToString("\n") + "\n..."
    }
}
