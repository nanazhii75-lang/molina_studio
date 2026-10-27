#!/bin/sh
# Molina: Penjelajah: riwayat folder dan beranda Debian (idempotent; aman dijalankan ulang).
# Jalankan dari dalam folder repo: bash molina_recent_debian.sh
set -e
python3 << 'PYEOF'
import os, sys

def _is_repo(p):
    return bool(p) and os.path.isfile(os.path.join(p, "app/src/main/AndroidManifest.xml"))
# MOLINA_ROOT dipakai Terminal Molina untuk /sdcard/Molina, jadi sengaja TIDAK dipakai di sini.
ROOT = next((p for p in (os.environ.get("MOLINA_REPO"), os.getcwd(),
                         "/storage/emulated/0/molina-studio", "/sdcard/molina-studio") if _is_repo(p)), "")
if not ROOT:
    sys.exit("Repo molina-studio tidak ditemukan. Jalankan dari dalam folder repo atau set MOLINA_REPO.")
J = os.path.join(ROOT, "app/src/main/java/com/molina/studio")
changed = []

def rd(p):
    with open(p, encoding="utf-8") as f:
        return f.read()

def wr(p, s):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(s)

def sub(path, old, new, marker, label):
    s = rd(path)
    if marker in s:
        print("  = " + label + " (sudah ada)")
        return
    if s.count(old) != 1:
        sys.exit("GAGAL [" + label + "]: anchor ditemukan " + str(s.count(old)) + "x di " + path)
    wr(path, s.replace(old, new))
    changed.append(label)
    print("  + " + label)

def newfile(path, content, label):
    if os.path.exists(path):
        print("  = " + label + " (sudah ada)")
        return
    wr(path, content)
    changed.append(label)
    print("  + " + label)

def progres(tag, text):
    pg = os.path.join(ROOT, "progres.md")
    if os.path.isfile(pg) and tag not in rd(pg):
        with open(pg, "a", encoding="utf-8", newline="\n") as f:
            f.write(text)
        changed.append("progres.md"); print("  + progres.md")
    else:
        print("  = progres.md (sudah ada / tidak ditemukan)")

WD = os.path.join(J, "cinema/Win7OpenDialog.kt")
EB = os.path.join(J, "settings/ExplorerBridge.kt")
SS = os.path.join(J, "settings/SettingsScreen.kt")

# ---- A1. Riwayat folder ----
newfile(os.path.join(J, "cinema/ExplorerRecent.kt"), '''package com.molina.studio.cinema

import android.content.Context
import org.json.JSONArray
import java.io.File

/** Riwayat folder terakhir Penjelajah (maks. 5, terbaru di depan) dan folder terakhir yang dibuka. */
internal object ExplorerRecent {
    private const val PREFS = "molina_explorer"
    private const val KEY = "recent_v1"
    private const val MAX = 5

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun paths(ctx: Context): MutableList<String> {
        val raw = prefs(ctx).getString(KEY, null) ?: return mutableListOf()
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { a.getString(it) }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    /** Folder terakhir yang masih ada, terbaru lebih dulu. */
    fun load(ctx: Context): List<File> = paths(ctx).map { File(it) }.filter { it.isDirectory }

    fun last(ctx: Context): File? = load(ctx).firstOrNull()

    fun push(ctx: Context, dir: File) {
        if (!dir.isDirectory) return
        val l = paths(ctx)
        l.removeAll { it == dir.path }
        l.add(0, dir.path)
        while (l.size > MAX) l.removeAt(l.size - 1)
        prefs(ctx).edit().putString(KEY, JSONArray(l).toString()).apply()
    }
}
''', "ExplorerRecent.kt (riwayat folder)")

# ---- A2. Dialog Penjelajah ----
sub(WD, "import com.molina.studio.terminal.TerminalSessions\n",
    "import com.molina.studio.terminal.RootfsManager\nimport com.molina.studio.terminal.TerminalSessions\n",
    "import com.molina.studio.terminal.RootfsManager", "import RootfsManager")
sub(WD, "private fun w7Drives(): List<W7Place> {",
    '''/** Beranda Debian (/root di rootfs) sebagai akar tambahan agar berkas Debian bisa dikelola dari Penjelajah. */
private fun w7DebianPlaces(ctx: Context, browse: Boolean): List<W7Place> {
    if (!browse) return emptyList()
    val home = File(RootfsManager.rootfsDir(ctx), "root")
    return if (home.isDirectory) listOf(W7Place("Debian", home)) else emptyList()
}

private fun w7Drives(): List<W7Place> {''',
    "private fun w7DebianPlaces", "fungsi w7DebianPlaces")
sub(WD, "    val vols = remember { w7Drives() }",
    "    val vols = remember { w7Drives() + w7DebianPlaces(ctx, browse) }",
    "w7Drives() + w7DebianPlaces", "akar Debian di daftar Komputer")
sub(WD, "    var dir by remember { mutableStateOf(startDir?.takeIf { it.isDirectory } ?: vols.first().dir) }",
    "    // Mode jelajah: tanpa startDir, mulai dari folder terakhir yang dibuka (bila masih ada), bukan selalu dari akar\n"
    "    var dir by remember { mutableStateOf(startDir?.takeIf { it.isDirectory } ?: (if (browse) ExplorerRecent.last(ctx) else null) ?: vols.first().dir) }\n"
    "    val recents = remember(dir) {\n"
    "        if (browse) ExplorerRecent.load(ctx).filter { r -> r != dir && vols.none { it.dir == r } && favs.none { it.dir == r } }.take(5)\n"
    "        else emptyList()\n"
    "    }",
    "ExplorerRecent.last(ctx)", "buka di folder terakhir + daftar terakhir")
sub(WD, '    fun go(to: File) { dir = to; selected = null; fileName = ""; message = ""; query = ""; undo = null }',
    '    fun go(to: File) { dir = to; selected = null; fileName = ""; message = ""; query = ""; undo = null; if (browse) ExplorerRecent.push(ctx, to) }',
    "undo = null; if (browse) ExplorerRecent.push(ctx, to)", "go(): simpan riwayat folder")
sub(WD, "modifier: Modifier, onBin: (() -> Unit)? = null, onGo: (File) -> Unit) {",
    "modifier: Modifier, onBin: (() -> Unit)? = null, recents: List<File> = emptyList(), onGo: (File) -> Unit) {",
    "recents: List<File> = emptyList(), onGo", "W7NavPane: parameter recents")
sub(WD, "onBin = if (browse) ({ showBin = true }) else null) { go(it) }",
    "onBin = if (browse) ({ showBin = true }) else null, recents = recents) { go(it) }",
    "recents = recents) { go(it) }", "pemanggil W7NavPane")
sub(WD, '        Spacer(Modifier.height(8.dp))\n        W7NavHeader("Komputer", false)',
    '        if (recents.isNotEmpty()) {\n'
    '            Spacer(Modifier.height(8.dp))\n'
    '            W7NavHeader("Terakhir", false)\n'
    '            recents.forEach { r -> W7NavItem(r.name.ifEmpty { "/" }, dir == r, { Win7Folder(16.dp) }) { onGo(r) } }\n'
    '        }\n'
    '        Spacer(Modifier.height(8.dp))\n        W7NavHeader("Komputer", false)',
    'W7NavHeader("Terakhir", false)', "panel kiri: bagian Terakhir")

# ---- A3. Jembatan: path Debian untuk berkas di rootfs ----
sub(EB, "    private fun base(): String = Environment.getExternalStorageDirectory().absolutePath\n",
    "    private fun base(): String = Environment.getExternalStorageDirectory().absolutePath\n\n"
    "    /** Lokasi rootfs Debian di sisi Android; diisi saat Penjelajah dibuka. */\n"
    "    @Volatile private var rootfsPath: String? = null\n\n"
    "    fun attach(ctx: Context) {\n"
    "        rootfsPath = com.molina.studio.terminal.RootfsManager.rootfsDir(ctx).absolutePath\n"
    "    }\n",
    "fun attach(ctx: Context)", "ExplorerBridge.attach")
sub(EB, "        val b = base()\n        val p = f.absolutePath\n        return when {",
    "        val b = base()\n        val p = f.absolutePath\n"
    "        rootfsPath?.let { r ->\n"
    "            if (p == r) return \"/\"\n"
    "            if (p.startsWith(\"$r/\")) return p.substring(r.length)\n"
    "        }\n"
    "        return when {",
    "rootfsPath?.let { r ->", "debianPath memetakan rootfs ke path Debian")
sub(EB, '        return roots.any { p == it || p.startsWith("$it/") }',
    '        if (rootfsPath?.let { r -> p == r || p.startsWith("$r/") } == true) return true\n'
    '        return roots.any { p == it || p.startsWith("$it/") }',
    "if (rootfsPath?.let { r -> p == r", "berkas di rootfs selalu terlihat di Debian")
sub(SS, "    remember { AptHistory.init(aptCtx) }\n",
    "    remember { AptHistory.init(aptCtx) }\n    remember { MolinaExplorer.attach(aptCtx) }\n",
    "MolinaExplorer.attach(aptCtx)", "SettingsScreen: attach rootfs")

progres("EXPLORER-RECENT-DEBIAN", '''
## 2026-10-27 - Penjelajah: riwayat folder dan beranda Debian [EXPLORER-RECENT-DEBIAN]
- [x] feat(explorer): Penjelajah dibuka di folder terakhir (startDir dari tab Unduhan tetap diutamakan); panel kiri punya bagian "Terakhir" (5 folder terakhir, tanpa duplikat favorit/akar)
- [x] feat(explorer): akar "Debian" (rootfs/root) di daftar Komputer: kelola berkas Debian dari Penjelajah; path dipetakan ke path Debian (/root/...), sehingga Salin path, Sisipkan, dan Buka Terminal memakai path yang benar. Akar ini tidak bisa dinaiki ke atas, terlindung dari hapus/ubah nama, dan punya .molina-trash sendiri (Recycle Bin per volume)
- [x] Dampak: cinema/ExplorerRecent.kt (baru), cinema/Win7OpenDialog.kt, settings/ExplorerBridge.kt, settings/SettingsScreen.kt. Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (parameter baru W7NavPane, import RootfsManager)
- [ ] uji perangkat: buka Berkas, masuk folder, tutup, buka lagi (kembali ke folder terakhir); bagian Terakhir muncul dan bekerja; tombol "Buka folder di Penjelajah" dari Unduhan tetap membuka folder unduhan; akar Debian terlihat, Naik tidak keluar dari /root; di Debian > Buka Terminal (prompt di /root/...) dan Salin path (mulai /root); hapus berkas di Debian > Recycle Bin > Urungkan
''')

print("\nSelesai. Perubahan baru: " + str(len(changed)))
PYEOF

RD="$(python3 -c 'import os,sys;print(os.path.abspath(sys.argv[1]))' "${MOLINA_REPO:-.}")"
[ -f "$RD/app/src/main/AndroidManifest.xml" ] || RD="$(ls -d /storage/emulated/0/molina-studio /sdcard/molina-studio 2>/dev/null | head -1)"
cd "$RD" && git add app progres.md && { git diff --cached --quiet && echo "Tidak ada perubahan baru untuk di-commit." || { git commit -m "feat(explorer): riwayat folder dan beranda Debian di Penjelajah" && git pull --rebase -q && git push; }; }
