import com.molina.code.preview.PythonTerminalRun as P

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

fun main() {
    check("isPythonName .py", P.isPythonName("a.py") && P.isPythonName("A.PY") && !P.isPythonName("a.sh") && !P.isPythonName(".py") && !P.isPythonName(null))

    check("nama tanpa ekstensi", P.normalizeName("tes").name == "tes.py")
    check("nama .py tetap", P.normalizeName("tes.py").name == "tes.py")
    check("nama .PY jadi huruf kecil", P.normalizeName(" tes.PY ").name == "tes.py")
    check("nama dengan titik", P.normalizeName("a.b").name == "a.b.py")
    check("kosong ditolak", P.normalizeName("  ").error != null && P.normalizeName(".py").error != null)
    check("path ditolak", P.normalizeName("../x").error != null && P.normalizeName("a/b").error != null)
    check("spasi dan simbol ditolak", P.normalizeName("a b").error != null && P.normalizeName("a;b").error != null)
    check("titik ujung ditolak", P.normalizeName(".x").error != null && P.normalizeName("x.").error != null)
    check("terlalu panjang", P.normalizeName("a".repeat(61)).error != null && P.normalizeName("a".repeat(60)).name != null)

    check("stem dari nama", P.suggestStem("tes.py") == "tes" && P.suggestStem("Untitled 2") == "skrip" && P.suggestStem(null) == "skrip")

    check("heredoc bentuk dasar", P.heredoc("print(1)\n") == "python3 << 'PYEOF'\nprint(1)\nPYEOF")
    check("heredoc CRLF jadi LF", P.heredoc("a=1\r\nprint(a)\r\n") == "python3 << 'PYEOF'\na=1\nprint(a)\nPYEOF")
    check("heredoc kosong null", P.heredoc("  \n\n") == null && P.heredoc("") == null)
    check("heredoc menjaga \$ dan backtick", P.heredoc("x='\$HOME `id`'")!!.contains("x='\$HOME `id`'"))
    check("pembatas bentrok", P.heredoc("a\nPYEOF\nb") == "python3 << 'PYEOF_1'\na\nPYEOF\nb\nPYEOF_1")
    check("pembatas bentrok dua kali", P.uniqueDelimiter("PYEOF\nPYEOF_1") == "PYEOF_2")
    check("PYEOF di tengah baris aman", P.uniqueDelimiter("print('PYEOF')") == "PYEOF")

    check("fileCommand biasa", P.fileCommand("/sdcard/molina-studio/tes.py") == "(cd '/sdcard/molina-studio' && python3 'tes.py')")
    check("fileCommand root", P.fileCommand("/x.py") == "(cd '/' && python3 'x.py')")
    check("fileCommand kutip", P.fileCommand("/sdcard/a'b/x.py") == "(cd '/sdcard/a'\\''b' && python3 'x.py')")

    check("preview pendek", P.preview("a\nb") == "a\nb")
    check("preview panjang", P.preview("1\n2\n3\n4\n5\n6", 4) == "1\n2\n3\n4\n...")

    if (fails > 0) { println("$fails GAGAL"); System.exit(1) } else println("SEMUA LULUS")
}
