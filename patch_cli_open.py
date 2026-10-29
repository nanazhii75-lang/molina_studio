import os, sys

def find_root():
    for c in (os.getcwd(), "/storage/emulated/0/molina-studio", "/sdcard/molina-studio"):
        if os.path.isfile(os.path.join(c, "settings.gradle.kts")) and os.path.isdir(os.path.join(c, "app")):
            return c
    sys.exit("GAGAL: repo molina-studio tidak ditemukan. Jalankan dari dalam repo.")

ROOT = find_root()
P = "app/src/main/java/com/molina/studio/cmd/CommandProtocol.kt"
S = "app/src/main/java/com/molina/studio/cmd/CommandServer.kt"
C = "app/src/main/assets/molina/molina-cli.py"
TK = "scripts/app-tests/CommandProtocolTests.kt"
TP = "scripts/app-tests/test_molina_cli.py"

OPEN_FN = r"""    /** Buka berkas di editor Code (tab Code); MainActivity yang mengonsumsi CodeNav.pendingFiles. */
    private fun open(arg: String): String {
        val paths = CommandProtocol.splitPaths(arg)
        if (paths.isEmpty()) return CommandProtocol.err("usage", "open butuh minimal satu berkas")
        if (paths.size > CommandProtocol.MAX_OPEN) {
            return CommandProtocol.err("many", "maksimal ${CommandProtocol.MAX_OPEN} berkas")
        }
        val root = Environment.getExternalStorageDirectory().absolutePath
        val files = ArrayList<File>(paths.size)
        for (p in paths) {
            val host = CommandProtocol.guestToHost(p, root)
                ?: return CommandProtocol.err("path", "path harus di bawah /sdcard atau /storage/emulated/0: $p")
            val f = File(host)
            if (!f.isFile) return CommandProtocol.err("missing", "bukan berkas: $p")
            files.add(f)
        }
        Handler(Looper.getMainLooper()).post { CodeNav.pendingFiles = files }
        return CommandProtocol.ok("membuka ${files.size} berkas di Code")
    }

"""

CMD_OPEN = r'''def cmd_open(args):
    if not args:
        sys.stderr.write("molina: open butuh minimal satu berkas\n")
        return 2
    if len(args) > 10:
        sys.stderr.write("molina: open maksimal 10 berkas sekaligus\n")
        return 2
    paths = []
    for a in args:
        p = os.path.abspath(os.path.expanduser(a))
        if "\t" in p or "\n" in p or "\r" in p:
            sys.stderr.write("molina: nama berkas mengandung TAB atau baris baru (tidak didukung)\n")
            return 2
        paths.append(p)
    return run("open", "\t".join(paths))


'''

KT_TESTS = r'''    // open: parse dan pemisah path
    eq("open verb", okReq("open\t/sdcard/a.py\n")?.verb, Verb.OPEN)
    eq("open arg", okReq("open\t/sdcard/a.py\n")?.arg, "/sdcard/a.py")
    eq("open tanpa arg", badCode("open\n"), "usage")
    eq("splitPaths dua", CommandProtocol.splitPaths("/sdcard/a b.py\t/sdcard/c.py"), listOf("/sdcard/a b.py", "/sdcard/c.py"))
    eq("splitPaths kosong", CommandProtocol.splitPaths(""), emptyList<String>())
    eq("splitPaths TAB saja", CommandProtocol.splitPaths("\t\t"), emptyList<String>())
    eq("splitPaths dipangkas", CommandProtocol.splitPaths("  /sdcard/a  "), listOf("/sdcard/a"))
    check("MAX_OPEN", CommandProtocol.MAX_OPEN == 10)

'''

PY_TESTS = r'''    def test_open_joins_paths_with_tab(self):
        code, out, _ = run_cli("open", "/sdcard/a.py", "/sdcard/b c.py")
        self.assertEqual(code, 0)
        self.assertEqual(self.srv.lines[-1], "open\t/sdcard/a.py\t/sdcard/b c.py")

    def test_open_usage(self):
        self.assertEqual(run_cli("open")[0], 2)
        self.assertEqual(run_cli("open", *["/sdcard/x"] * 11)[0], 2)
        self.assertEqual(run_cli("open", "/sdcard/a\tb")[0], 2)
        self.assertEqual(self.srv.lines, [])

'''

# (berkas, penanda idempoten, jangkar, teks, "before"/"after")
PATCHES = [
    (P, 'OPEN("open")', 'PLAY("play"), STOP("stop")', ', OPEN("open")', "after"),
    (P, "verb == Verb.OPEN",
     'if (verb == Verb.PLAY && arg.isEmpty()) return Parsed.Bad("usage", "play butuh path atau URL")',
     'if ((verb == Verb.PLAY || verb == Verb.OPEN) && arg.isEmpty()) return Parsed.Bad("usage", "${verb.wire} butuh argumen")',
     "replace"),
    (P, "fun splitPaths",
     "    fun oneLine(s: String): String = s.replace('\\n', ' ').replace('\\r', ' ').replace('\\t', ' ').take(300)\n",
     "\n    /** Batas berkas per perintah open. */\n    const val MAX_OPEN = 10\n\n"
     "    /** Argumen open: path dipisah TAB; entri kosong dibuang. */\n"
     "    fun splitPaths(arg: String): List<String> = arg.split('\\t').map { it.trim() }.filter { it.isNotEmpty() }\n",
     "after"),
    (S, "import android.os.Handler", "import android.os.Environment\n",
     "import android.os.Handler\nimport android.os.Looper\n", "after"),
    (S, "import com.molina.studio.code.CodeNav", "import com.molina.studio.cinema.CinemaActions\n",
     "import com.molina.studio.code.CodeNav\n", "after"),
    (S, "Verb.OPEN ->",
     "        CommandProtocol.Verb.PLAY -> play(NewTaskContext(app), r.arg)\n",
     "        CommandProtocol.Verb.OPEN -> open(r.arg)\n", "after"),
    (S, "private fun open(", "    /** startActivity dari konteks Application wajib FLAG_ACTIVITY_NEW_TASK;", OPEN_FN, "before"),
    (C, "molina open <berkas>",
     "  molina stop                    hentikan pemutar\n",
     "  molina open <berkas>...        buka berkas (maks 10) di editor Code\n", "after"),
    (C, "def cmd_open(", "def cmd_log(args):\n", CMD_OPEN, "before"),
    (C, 'verb == "open"', '    if verb == "log":\n        return cmd_log(rest)\n',
     '    if verb == "open":\n        return cmd_open(rest)\n', "before"),
    (TK, "open verb", '    check("konstanta", CommandProtocol.SOCKET_NAME', KT_TESTS, "before"),
    (TP, 'verb == "open"', '    if verb == "play":\n        arg = line.split("\\t", 1)[1]\n',
     '    if verb == "open":\n        return "OK membuka\\n"\n', "before"),
    (TP, "test_open_usage", "    def test_server_error_exit_1(self):\n", PY_TESTS, "before"),
]

def read(p):
    with open(os.path.join(ROOT, p), encoding="utf-8", newline="") as fh:
        return fh.read()

work = {}
for path, marker, anchor, text, where in PATCHES:
    src = work.get(path, read(path))
    if marker in src:
        work[path] = src
        print("sudah ada:", marker)
        continue
    if src.count(anchor) != 1:
        sys.exit("GAGAL: jangkar tidak ditemukan tepat satu kali di %s: %r. Tidak ada yang ditulis." % (path, anchor[:60]))
    if where == "after":
        src = src.replace(anchor, anchor + text)
    elif where == "before":
        src = src.replace(anchor, text + anchor)
    else:
        src = src.replace(anchor, text)
    work[path] = src
    print("dipatch:", path, "->", marker)

for path, src in work.items():
    with open(os.path.join(ROOT, path), "w", encoding="utf-8", newline="") as fh:
        fh.write(src)
print("SELESAI. Commit: feat(cmd): molina open (Debian -> editor Code)")
