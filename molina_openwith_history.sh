#!/bin/sh
# Molina: Buka dengan aplikasi lain + riwayat Mundur/Maju di Penjelajah (idempotent).
# Jalankan dari dalam folder repo: bash molina_openwith_history.sh
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
XM = os.path.join(ROOT, "app/src/main/res/xml/molina_file_paths.xml")

# ---- C1. Buka dengan aplikasi lain ----
sub(WD, "    onShare: ((List<File>) -> Boolean)? = null,\n",
    "    onShare: ((List<File>) -> Boolean)? = null,\n    onOpenWith: ((File) -> Boolean)? = null,\n",
    "onOpenWith: ((File) -> Boolean)?", "parameter dialog: onOpenWith")
sub(WD, '            else -> say("Tidak ada aplikasi Molina untuk membuka jenis berkas ini. Pakai menu Alat untuk menyalin atau mengompres.")',
    '            onOpenWith != null && f.isFile -> onOpenWith?.invoke(f)\n'
    '            else -> say("Tidak ada aplikasi Molina untuk membuka jenis berkas ini. Pakai menu Alat untuk menyalin atau mengompres.")',
    "onOpenWith != null && f.isFile", "smartOpen: jenis tak dikenal dibuka lewat chooser sistem")
sub(WD, "INFO, SHARE }", "INFO, SHARE, OPEN_WITH }", "SHARE, OPEN_WITH }", "ikon OPEN_WITH (enum)")
sub(WD, "            W7Ico.SHARE -> {\n",
    "            W7Ico.OPEN_WITH -> {\n"
    "                line(2f, 4f, 2f, 13f); line(2f, 13f, 11f, 13f); line(11f, 13f, 11f, 9f); line(2f, 4f, 7f, 4f)\n"
    "                line(8f, 8f, 14f, 2f); line(10f, 2f, 14f, 2f); line(14f, 2f, 14f, 6f)\n"
    "            }\n"
    "            W7Ico.SHARE -> {\n",
    "W7Ico.OPEN_WITH -> {", "ikon OPEN_WITH (gambar)")
sub(WD, '                                    W7MenuItem(W7Ico.SHARE, "Bagikan", onShare != null && tg.any { it.isFile }) {',
    '                                    W7MenuItem(W7Ico.OPEN_WITH, "Buka dengan...", onOpenWith != null && one?.isFile == true) {\n'
    '                                        toolsOpen = false\n'
    '                                        one?.let { onOpenWith?.invoke(it) }\n'
    '                                    }\n'
    '                                    W7MenuItem(W7Ico.SHARE, "Bagikan", onShare != null && tg.any { it.isFile }) {',
    'W7MenuItem(W7Ico.OPEN_WITH, "Buka dengan..."', "menu Alat: Buka dengan...")
sub(EB, "    /** Bagikan berkas (bukan folder) lewat share sheet Android memakai FileProvider. */",
    '''    /** Buka berkas dengan aplikasi lain lewat chooser sistem (FileProvider, izin baca sementara). */
    fun openWith(ctx: Context, f: File): Boolean {
        if (!f.isFile || !f.canRead()) {
            Toast.makeText(ctx, "Berkas tidak bisa dibaca.", Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
            val mime = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Buka dengan")
            if (ctx !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(chooser)
            true
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(ctx, "Tidak ada aplikasi yang bisa membuka jenis berkas ini.", Toast.LENGTH_LONG).show()
            false
        } catch (e: IllegalArgumentException) {
            Toast.makeText(ctx, "Lokasi berkas ini tidak bisa dibuka dengan aplikasi lain.", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal membuka: " + (e.message ?: "tidak diketahui"), Toast.LENGTH_LONG).show()
            false
        }
    }

    /** Bagikan berkas (bukan folder) lewat share sheet Android memakai FileProvider. */''',
    "fun openWith(ctx: Context", "ExplorerBridge.openWith")
sub(SS, "            onShare = { files -> MolinaExplorer.share(aptCtx, files) },\n",
    "            onShare = { files -> MolinaExplorer.share(aptCtx, files) },\n            onOpenWith = { f -> MolinaExplorer.openWith(aptCtx, f) },\n",
    "onOpenWith = { f -> MolinaExplorer.openWith", "SettingsScreen: onOpenWith")
sub(XM, '    <external-path name="internal_storage" path="." />\n',
    '    <external-path name="internal_storage" path="." />\n    <files-path name="debian_home" path="rootfs/debian/root/" />\n',
    "debian_home", "FileProvider: beranda Debian bisa dibagikan/dibuka")

# ---- C2. Riwayat Mundur/Maju ----
sub(WD, "import androidx.compose.runtime.mutableStateOf\n",
    "import androidx.compose.runtime.mutableStateListOf\nimport androidx.compose.runtime.mutableStateOf\n",
    "import androidx.compose.runtime.mutableStateListOf", "import mutableStateListOf")
sub(WD, '    fun go(to: File) { dir = to; selected = null; fileName = ""; message = ""; query = ""; undo = null; if (browse) ExplorerRecent.push(ctx, to) } // [PICK-PERSIST] picked bertahan lintas folder',
    '''    // [NAV-HISTORY] riwayat Mundur/Maju antar folder (maks. 50); tombol bulat di kiri atas = Mundur, bila kosong = Naik
    val backStack = remember { mutableStateListOf<String>() }
    val fwdStack = remember { mutableStateListOf<String>() }
    fun go(to: File) {
        if (to.path != dir.path) {
            backStack.add(dir.path)
            if (backStack.size > 50) backStack.removeAt(0)
            fwdStack.clear()
        }
        dir = to; selected = null; fileName = ""; message = ""; query = ""; undo = null; if (browse) ExplorerRecent.push(ctx, to)
    } // [PICK-PERSIST] picked bertahan lintas folder
    fun jump(to: File) {
        dir = to; selected = null; fileName = ""; message = ""; query = ""; undo = null; if (browse) ExplorerRecent.push(ctx, to)
    }
    fun goBack(): Boolean {
        while (backStack.isNotEmpty()) {
            val p = File(backStack.removeAt(backStack.size - 1))
            if (p.isDirectory) { fwdStack.add(dir.path); jump(p); return true }
        }
        return false
    }
    fun goForward(): Boolean {
        while (fwdStack.isNotEmpty()) {
            val p = File(fwdStack.removeAt(fwdStack.size - 1))
            if (p.isDirectory) { backStack.add(dir.path); jump(p); return true }
        }
        return false
    }''',
    "[NAV-HISTORY]", "riwayat Mundur/Maju (go, goBack, goForward)")
sub(WD, "                    Win7BackOrb(canUp) { goUp() }",
    "                    Win7BackOrb(canUp || backStack.isNotEmpty()) { if (!goBack()) goUp() }",
    "Win7BackOrb(canUp || backStack.isNotEmpty())", "tombol bulat: Mundur lalu Naik")
sub(WD, '                    W7Cmd("Naik", canUp) { goUp() }\n',
    '                    W7Cmd("Naik", canUp) { goUp() }\n                    W7Cmd("Maju", fwdStack.isNotEmpty()) { goForward() }\n',
    'W7Cmd("Maju"', "perintah Maju")

progres("EXPLORER-OPENWITH-HISTORY", '''
## 2026-10-27 - Penjelajah: Buka dengan aplikasi lain, riwayat Mundur/Maju [EXPLORER-OPENWITH-HISTORY]
- [x] feat(explorer): berkas jenis tak dikenal (PDF, gambar, APK, dokumen) yang diketuk ulang kini dibuka lewat chooser sistem (ACTION_VIEW + FileProvider yang sudah ada), bukan lagi pesan "Tidak ada aplikasi Molina"; menu Alat > "Buka dengan..." untuk satu berkas apa pun. ZIP, media (mpv), dan teks (Code) tetap memakai jalur Molina
- [x] feat(explorer): FileProvider kini juga mencakup beranda Debian (files/rootfs/debian/root) sehingga Bagikan dan Buka dengan bekerja untuk berkas di akar "Debian"
- [x] feat(explorer): riwayat Mundur/Maju (maks. 50): tombol bulat kiri atas = Mundur bila ada riwayat, jika tidak Naik; perintah "Maju" di bilah alat; folder yang sudah hilang dilewati
- [x] Dampak: cinema/Win7OpenDialog.kt, settings/ExplorerBridge.kt, settings/SettingsScreen.kt, res/xml/molina_file_paths.xml. AndroidManifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (when baru di smartOpen, fungsi lokal go/goBack/goForward memakai mutableStateListOf)
- [ ] uji perangkat: ketuk PDF/gambar dua kali > chooser muncul; APK > installer; berkas tanpa aplikasi > toast; Alat > Buka dengan... pada satu berkas; berkas di akar Debian > Buka dengan dan Bagikan; masuk folder A > B > C lewat ketukan, tombol bulat kembali ke B lalu A; Maju kembali ke B; lompat lewat Favorit/Terakhir lalu Mundur kembali ke folder sebelumnya; hapus folder lalu Mundur melewati folder yang hilang
- [ ] LANGKAH BERIKUT: daftar "Terbaru" (sudah di peta jalan sesi lain), lalu Folder Molina (lokasi default) di Penyimpanan
''')

print("\nSelesai. Perubahan baru: " + str(len(changed)))
PYEOF

RD="$(python3 -c 'import os,sys;print(os.path.abspath(sys.argv[1]))' "${MOLINA_REPO:-.}")"
[ -f "$RD/app/src/main/AndroidManifest.xml" ] || RD="$(ls -d /storage/emulated/0/molina-studio /sdcard/molina-studio 2>/dev/null | head -1)"
cd "$RD" && git add app progres.md && { git diff --cached --quiet && echo "Tidak ada perubahan baru untuk di-commit." || { git commit -m "feat(explorer): Buka dengan aplikasi lain dan riwayat Mundur/Maju" && git pull --rebase -q && git push; }; }
