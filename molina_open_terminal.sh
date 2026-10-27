#!/data/data/com.termux/files/usr/bin/bash
# Molina: Buka Terminal + Bagikan (idempotent). Jalankan dari folder repo: bash molina_open_terminal.sh
set -e
python3 << 'PYEOF'
import os, re, sys

def _is_repo(p):
    return bool(p) and os.path.isfile(os.path.join(p, "app/src/main/AndroidManifest.xml"))
# Catatan: MOLINA_ROOT dipakai Terminal Molina untuk /sdcard/Molina, jadi sengaja TIDAK dipakai di sini.
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
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(s)

def sub(path, old, new, marker, label):
    """Ganti `old` dengan `new` satu kali; lewati bila `marker` sudah ada. Gagal keras bila anchor hilang."""
    s = rd(path)
    if marker in s:
        print("  = " + label + " (sudah ada)")
        return
    if s.count(old) != 1:
        sys.exit("GAGAL [" + label + "]: anchor ditemukan " + str(s.count(old)) + "x di " + path)
    wr(path, s.replace(old, new))
    changed.append(label)
    print("  + " + label)

# ---------- 1. TerminalSessions: cwd awal sesi ----------
TS = os.path.join(J, "terminal/TerminalSessions.kt")
sub(TS, "fun newSession(context: Context): TerminalSession? {",
    "fun newSession(context: Context, guestCwd: String? = null): TerminalSession? {",
    "fun newSession(context: Context, guestCwd", "TerminalSessions.newSession(guestCwd)")
sub(TS, "        val s = debianShell(app)\n        sessions.add(s)",
    "        val s = debianShell(app, guestCwd)\n        sessions.add(s)",
    "debianShell(app, guestCwd)", "newSession meneruskan cwd")
sub(TS, "private fun debianShell(ctx: Context): TerminalSession {",
    "private fun debianShell(ctx: Context, cwd: String? = null): TerminalSession {",
    "debianShell(ctx: Context, cwd", "debianShell(cwd)")
sub(TS, 'val guestCwd = "/root" //',
    'val guestCwd = cwd?.takeIf { it.startsWith("/") } ?: "/root" //',
    "cwd?.takeIf", "guestCwd dari parameter")

# ---------- 2. ExplorerBridge ----------
EB = os.path.join(J, "settings/ExplorerBridge.kt")
sub(EB, "import android.content.Context\n",
    "import android.content.Context\nimport android.content.Intent\n",
    "import android.content.Intent", "import Intent")
sub(EB, "import com.molina.studio.code.CodeNav\n",
    "import com.molina.studio.code.CodeNav\nimport com.molina.studio.code.ScriptNav\n",
    "import com.molina.studio.code.ScriptNav", "import ScriptNav")
sub(EB, "fun visibleInDebian(f: File): Boolean {",
    "fun visibleInDebian(f: File, forNewSession: Boolean = false): Boolean {",
    "forNewSession: Boolean", "visibleInDebian(forNewSession)")
sub(EB, "        val full = TerminalSessions.fullAccessOf(TerminalSessions.currentSession) ?: TerminalPrefs.fullStorageAccess",
    "        val full = if (forNewSession) TerminalPrefs.fullStorageAccess\n            else TerminalSessions.fullAccessOf(TerminalSessions.currentSession) ?: TerminalPrefs.fullStorageAccess",
    "if (forNewSession) TerminalPrefs", "full access untuk sesi baru")

OPEN_TERM = '''    /**
     * Buka Terminal di folder [f] (folder induk bila berkas): sesi Debian baru dengan cwd di sana,
     * lalu minta MainActivity pindah ke tab Terminal. Bila batas sesi penuh, `cd` di sesi aktif.
     * Mengembalikan true bila Terminal ditampilkan (pemanggil boleh menutup Penjelajah).
     */
    fun openTerminalHere(ctx: Context, f: File): Boolean {
        val dir = (if (f.isDirectory) f else f.parentFile)
        if (dir == null || !dir.isDirectory) {
            Toast.makeText(ctx, "Folder tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!visibleInDebian(dir, forNewSession = true)) {
            Toast.makeText(
                ctx,
                "Folder ini tidak terlihat di Debian. Aktifkan \\"Akses penyimpanan penuh\\" di Setelan Terminal, atau pindahkan ke folder Molina atau molina-studio.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        val target = debianPath(dir)
        val created = try { TerminalSessions.newSession(ctx, target) } catch (e: Exception) { null }
        if (created != null) {
            Toast.makeText(ctx, "Terminal dibuka di " + target, Toast.LENGTH_SHORT).show()
        } else if (TerminalSessions.currentSession?.emulator != null && visibleInDebian(dir)) {
            TerminalActions.send("cd " + quote(target) + "\\r")
            Toast.makeText(ctx, "Batas sesi penuh: cd di sesi aktif ke " + target, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(ctx, "Batas sesi penuh (" + TerminalSessions.MAX_SESSIONS + "). Tutup salah satu sesi terlebih dahulu.", Toast.LENGTH_LONG).show()
            return false
        }
        ScriptNav.openTerminal = true
        return true
    }

    /** Bagikan berkas (bukan folder) lewat share sheet Android memakai FileProvider. */
    fun share(ctx: Context, files: List<File>): Boolean {
        val fs = files.filter { it.isFile && it.canRead() }.take(50)
        if (fs.isEmpty()) {
            Toast.makeText(ctx, "Pilih berkas (bukan folder) untuk dibagikan.", Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            val auth = ctx.packageName + ".fileprovider"
            val uris = ArrayList(fs.map { androidx.core.content.FileProvider.getUriForFile(ctx, auth, it) })
            val intent = if (uris.size == 1) {
                val mime = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(fs[0].extension.lowercase()) ?: "application/octet-stream"
                Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uris[0]) }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "*/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(intent, "Bagikan")
            if (ctx !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(chooser)
            true
        } catch (e: IllegalArgumentException) {
            Toast.makeText(ctx, "Lokasi berkas ini tidak bisa dibagikan (di luar penyimpanan internal).", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal membagikan: " + (e.message ?: "tidak diketahui"), Toast.LENGTH_LONG).show()
            false
        }
    }

'''
s = rd(EB)
if "fun openTerminalHere" in s:
    print("  = openTerminalHere/share (sudah ada)")
else:
    m = re.search(r"    /\*\*\n     \* Jalankan \[f\] di sesi aktif.*?(?=    /\*\* Jalankan `cd` ke folder)", s, re.S)
    if not m:
        sys.exit("GAGAL: blok runFile tidak ditemukan di ExplorerBridge.kt")
    s = s[:m.start()] + OPEN_TERM + s[m.end():]
    # sendToTerminal (ada-Enter) tidak lagi dipakai: hapus
    m2 = re.search(r"    /\*\* Tempel path \(sudah di-quote\) ke input shell sesi aktif, tanpa menekan Enter\. \*/\n    fun sendToTerminal.*?(?=    /\*\* Sisipkan path ter-quote)", s, re.S)
    if m2:
        s = s[:m2.start()] + s[m2.end():]
    wr(EB, s)
    changed.append("ExplorerBridge: openTerminalHere + share, hapus runFile/sendToTerminal")
    print("  + ExplorerBridge: openTerminalHere + share, hapus runFile/sendToTerminal")

# ---------- 3. Win7OpenDialog ----------
WD = os.path.join(J, "cinema/Win7OpenDialog.kt")
sub(WD, "    onSendToTerminal: ((File) -> Unit)? = null,\n    onRunInTerminal: ((File) -> Unit)? = null,\n",
    "    onOpenTerminal: ((File) -> Unit)? = null,\n    onShare: ((List<File>) -> Boolean)? = null,\n",
    "onOpenTerminal: ((File)", "parameter dialog: onOpenTerminal/onShare")
sub(WD, "    onChangeDir: ((File) -> Unit)? = null,\n    sendEnter: Boolean = false,\n    onSendEnterChange: ((Boolean) -> Unit)? = null,\n", "",
    "\x00never\x00" if "onChangeDir: ((File)" in rd(WD) else "onOpenTerminal: ((File)", "hapus parameter lama")

OLD_BTN = '''                            if (target.isFile && !(multi && picked.size > 1)) {
                                Win7Button("Jalankan", modifier = Modifier.weight(1f), enabled = onRunInTerminal != null, onClick = { onRunInTerminal?.invoke(target) })
                            } else {
                                Win7Button(
                                    "Sisipkan" + (if (insertList.size > 1) " (" + insertList.size + ")" else ""),
                                    modifier = Modifier.weight(1f), enabled = onInsertPaths != null, onClick = { onInsertPaths?.invoke(insertList) }
                                )
                            }
                            Win7Button("cd", modifier = Modifier.weight(1f), enabled = onChangeDir != null && target.isDirectory, onClick = { onChangeDir?.invoke(target) })
'''
NEW_BTN = '''                            Win7Button(
                                "Sisipkan" + (if (insertList.size > 1) " (" + insertList.size + ")" else ""),
                                modifier = Modifier.weight(1f), enabled = onInsertPaths != null, onClick = { onInsertPaths?.invoke(insertList) }
                            )
                            Win7Button("Buka Terminal", modifier = Modifier.weight(1.3f), enabled = onOpenTerminal != null, onClick = { onOpenTerminal?.invoke(target) })
'''
sub(WD, OLD_BTN, NEW_BTN, '"Buka Terminal"', "tombol bawah: Salin path | Sisipkan | Buka Terminal")

sub(WD, "PASTE, RENAME, ZIP, UNZIP, DELETE, NEWDIR, STAR, STAR_OUT, INFO }",
    "PASTE, RENAME, ZIP, UNZIP, DELETE, NEWDIR, STAR, STAR_OUT, INFO, SHARE }",
    "INFO, SHARE }", "ikon SHARE (enum)")
sub(WD, "            W7Ico.INFO -> {\n                drawCircle(c, radius = 6.2f * u",
    "            W7Ico.SHARE -> {\n"
    "                drawCircle(c, radius = 1.8f * u, center = Offset(12f * u, 3.5f * u), style = st)\n"
    "                drawCircle(c, radius = 1.8f * u, center = Offset(4f * u, 8f * u), style = st)\n"
    "                drawCircle(c, radius = 1.8f * u, center = Offset(12f * u, 12.5f * u), style = st)\n"
    "                line(5.6f, 7f, 10.4f, 4.5f); line(5.6f, 9f, 10.4f, 11.5f)\n"
    "            }\n"
    "            W7Ico.INFO -> {\n                drawCircle(c, radius = 6.2f * u",
    "W7Ico.SHARE -> {", "ikon SHARE (gambar)")

SHARE_ITEM = '''                                    W7MenuItem(W7Ico.SHARE, "Bagikan", onShare != null && tg.any { it.isFile }) {
                                        toolsOpen = false
                                        val fn = onShare
                                        if (fn != null) {
                                            val fs = tg.filter { it.isFile }
                                            if (fs.size < tg.size) say("Folder dilewati; hanya berkas yang dibagikan.")
                                            fn(fs)
                                        }
                                    }
'''
ANCHOR = '''                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                                    W7MenuItem(W7Ico.COPY, "Salin", tg.isNotEmpty()) {'''
sub(WD, ANCHOR, SHARE_ITEM + ANCHOR, 'W7MenuItem(W7Ico.SHARE', "menu Alat: Bagikan")

# ---------- 4. SettingsScreen ----------
SS = os.path.join(J, "settings/SettingsScreen.kt")
sub(SS, "    var sendEnter by remember { mutableStateOf(false) }\n", "",
    "\x00never\x00" if "var sendEnter" in rd(SS) else "onOpenTerminal = {", "hapus state sendEnter")
OLD_CALL = '''            onSendToTerminal = { f -> MolinaExplorer.sendToTerminal(aptCtx, f, sendEnter) },
            sendEnter = sendEnter,
            onSendEnterChange = { sendEnter = it },
            onChangeDir = { f -> MolinaExplorer.changeDir(aptCtx, f) },
            onRunInTerminal = { f -> MolinaExplorer.runFile(aptCtx, f) },
'''
NEW_CALL = '''            onOpenTerminal = { f ->
                if (MolinaExplorer.openTerminalHere(aptCtx, f)) {
                    showExplorer = false
                    if (page == Page.Files) page = Page.Home
                }
            },
            onShare = { files -> MolinaExplorer.share(aptCtx, files) },
'''
sub(SS, OLD_CALL, NEW_CALL, "onOpenTerminal = { f ->", "SettingsScreen: pemanggil dialog")

# ---------- 5. FileProvider ----------
XML = os.path.join(ROOT, "app/src/main/res/xml/molina_file_paths.xml")
if os.path.exists(XML):
    print("  = molina_file_paths.xml (sudah ada)")
else:
    wr(XML, '<?xml version="1.0" encoding="utf-8"?>\n<paths>\n    <external-path name="internal_storage" path="." />\n</paths>\n')
    changed.append("res/xml/molina_file_paths.xml"); print("  + molina_file_paths.xml")
MF = os.path.join(ROOT, "app/src/main/AndroidManifest.xml")
sub(MF, "    </application>",
    '''        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/molina_file_paths" />
        </provider>
    </application>''', "molina_file_paths", "AndroidManifest: FileProvider")

print("\nSelesai. Perubahan baru: " + str(len(changed)))
PYEOF

RD="$(python3 -c 'import os,sys;print(os.path.abspath(sys.argv[1]))' "${MOLINA_REPO:-.}")"
[ -f "$RD/app/src/main/AndroidManifest.xml" ] || RD="$(ls -d /storage/emulated/0/molina-studio /sdcard/molina-studio 2>/dev/null | head -1)"
PROG="$RD/progres.md"
grep -q 'EXPLORER-OPENTERM-SHARE' "$PROG" || cat >> "$PROG" << 'EOF'

## 2026-10-27 - Penjelajah: Buka Terminal, Bagikan [EXPLORER-OPENTERM-SHARE]
- [x] refactor(explorer): tombol "Jalankan" dan "cd" diganti "Buka Terminal": membuat sesi Debian baru dengan cwd di folder tujuan (folder induk bila berkas) lalu langsung pindah ke tab Terminal. Batas 5 sesi penuh: fallback cd di sesi aktif. Pengecekan "terlihat di Debian" memakai setelan sesi BARU
- [x] feat(explorer): menu Alat > Bagikan (berkas, FileProvider, share sheet Android); folder dilewati
- [x] cleanup: hapus runFile, sendToTerminal, state/parameter sendEnter, onChangeDir, onRunInTerminal (FileOps.runnerFor dibiarkan, tidak dipakai)
- [x] Dampak: AndroidManifest (+FileProvider ${applicationId}.fileprovider), res/xml/molina_file_paths.xml, TerminalSessions (newSession/debianShell + cwd), ExplorerBridge, Win7OpenDialog, SettingsScreen. JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (Manifest provider baru, parameter composable berubah)
- [ ] uji perangkat: pilih folder > Buka Terminal (prompt di folder itu, tab pindah, sesi lama utuh); pilih berkas > Buka Terminal (folder induk); folder di luar Debian > toast penjelasan; 5 sesi penuh > cd di sesi aktif; Alat > Bagikan 1 berkas ke aplikasi lain; Bagikan 3 berkas; pilih folder > Bagikan nonaktif; berkas di kartu SD > toast "di luar penyimpanan internal"
- [ ] LANGKAH BERIKUT: ekstrak 7z/tar.gz, jelajah isi ZIP, riwayat folder
EOF

cd "$RD" && git add -A && git commit -m "feat(explorer): Buka Terminal (sesi baru di folder) dan Bagikan via FileProvider" && git push
