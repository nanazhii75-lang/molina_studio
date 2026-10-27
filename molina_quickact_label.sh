#!/bin/sh
# Molina: aksi cepat kontekstual, ikon >_, nama sesi = folder (idempotent).
# Jalankan dari dalam folder repo: bash molina_quickact_label.sh
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

# ---- 1. Win7Button: warna aksen opsional ----
WA = os.path.join(J, "cinema/Win7Aero.kt")
sub(WA, "    isDefault: Boolean = false,\n    enabled: Boolean = true,\n    onClick: () -> Unit\n) {\n    val shape = RoundedCornerShape(3.dp)",
    "    isDefault: Boolean = false,\n    enabled: Boolean = true,\n    accent: Color? = null,\n    onClick: () -> Unit\n) {\n    val shape = RoundedCornerShape(3.dp)",
    "accent: Color? = null", "Win7Button(accent) parameter")
sub(WA, ") { Text(label, color = if (enabled) Win7.Ink else Win7.TextDim, fontSize = 13.sp) }",
    ") { Text(label, color = if (enabled) (accent ?: Win7.Ink) else Win7.TextDim, fontSize = 13.sp) }",
    "accent ?: Win7.Ink", "Win7Button(accent) warna teks")

# ---- 2. Dialog Penjelajah: tombol bawah ----
WD = os.path.join(J, "cinema/Win7OpenDialog.kt")
OLD1 = '''                            Win7Button("Salin path", modifier = Modifier.weight(1f), enabled = onCopyPath != null, onClick = { onCopyPath?.invoke(target) })
                            val insertList = if (multi && picked.isNotEmpty()) picked.map { File(it) } else listOf(target)
'''
NEW1 = '''                            val insertList = if (multi && picked.isNotEmpty()) picked.map { File(it) } else listOf(target)
                            // Aksi cepat mengikuti seleksi: media -> Putar, berkas teks -> Edit, selain itu -> Properti
                            val qMedia = onPlay != null && insertList.isNotEmpty() &&
                                insertList.all { it.isFile && it.extension.lowercase() in W7_MEDIA }
                            val qText = !qMedia && onOpenInCode != null && insertList.size == 1 &&
                                insertList[0].isFile && ExplorerList.looksText(insertList[0])
                            Win7Button(
                                if (qMedia) "Putar" else if (qText) "Edit" else "Properti",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (qMedia) {
                                        val fn = onPlay
                                        if (fn != null) scope.launch {
                                            val ok = fn(insertList)
                                            say(if (ok) "Memutar di mpv: " + w7Names(insertList) else "Tidak ada media yang bisa diputar", ok)
                                        }
                                    } else if (qText) onOpenInCode?.invoke(insertList)
                                    else sheet = W7Sheet.Info(insertList)
                                }
                            )
'''
sub(WD, OLD1, NEW1, "val qMedia", "tombol bawah: aksi cepat kontekstual")
sub(WD, '''Win7Button("Buka Terminal", modifier = Modifier.weight(1.3f), enabled = onOpenTerminal != null, onClick = { onOpenTerminal?.invoke(target) })''',
    '''Win7Button(">_ Terminal", modifier = Modifier.weight(1.2f), accent = Color(0xFF2E7D4F), enabled = onOpenTerminal != null, onClick = { onOpenTerminal?.invoke(target) })''',
    '">_ Terminal"', "tombol Terminal: ikon >_ + aksen hijau")
sub(WD, '                                    W7MenuItem(W7Ico.INFO, "Properti", true) {',
    '''                                    W7MenuItem(W7Ico.COPY, "Salin path", onCopyPath != null && (one != null || tg.isEmpty())) {
                                        toolsOpen = false
                                        onCopyPath?.invoke(one ?: dir)
                                    }
                                    W7MenuItem(W7Ico.INFO, "Properti", true) {''',
    '"Salin path", onCopyPath', "menu Alat: Salin path")

# ---- 3. Nama sesi = folder tujuan ----
TS = os.path.join(J, "terminal/TerminalSessions.kt")
sub(TS, "fun newSession(context: Context, guestCwd: String? = null): TerminalSession? {",
    "fun newSession(context: Context, guestCwd: String? = null, label: String? = null): TerminalSession? {",
    "label: String? = null): TerminalSession?", "newSession(label)")
sub(TS, "        val s = debianShell(app, guestCwd)\n        sessions.add(s)",
    "        val s = debianShell(app, guestCwd)\n        if (!label.isNullOrBlank()) labels[s] = label.trim().take(40)\n        sessions.add(s)",
    "labels[s] = label", "simpan label sesi")
sub(TS, "    /** true/false = sesi dibuat dengan/tanpa Akses penyimpanan penuh; null = tidak diketahui. */",
    "    /** Nama sesi dari tempat pembukaannya (mis. folder dari Penjelajah); null = tidak ada. */\n"
    "    private val labels = java.util.Collections.synchronizedMap(java.util.WeakHashMap<TerminalSession, String>())\n\n"
    "    fun labelOf(s: TerminalSession?): String? = if (s == null) null else labels[s]\n\n"
    "    /** true/false = sesi dibuat dengan/tanpa Akses penyimpanan penuh; null = tidak diketahui. */",
    "fun labelOf(s: TerminalSession?)", "peta label sesi")
SD = os.path.join(J, "terminal/SessionDrawer.kt")
sub(SD, 's.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Debian"',
    'TerminalSessions.labelOf(s) ?: s.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Debian"',
    "TerminalSessions.labelOf(s) ?:", "drawer memakai label sesi")
EB = os.path.join(J, "settings/ExplorerBridge.kt")
sub(EB, "TerminalSessions.newSession(ctx, target) } catch",
    'TerminalSessions.newSession(ctx, target, target.substringAfterLast(\'/\').ifEmpty { "sdcard" }) } catch',
    "target.substringAfterLast", "openTerminalHere memberi nama sesi")

# ---- 4. progres.md ----
PG = os.path.join(ROOT, "progres.md")
if os.path.isfile(PG) and "EXPLORER-QUICKACT-LABEL" not in rd(PG):
    with open(PG, "a", encoding="utf-8", newline="\n") as f:
        f.write('''
## 2026-10-27 - Penjelajah: aksi cepat, ikon >_, nama sesi [EXPLORER-QUICKACT-LABEL]
- [x] feat(explorer): tombol bawah kini Aksi cepat kontekstual | Sisipkan | >_ Terminal. Aksi cepat: media = Putar (mpv), berkas teks = Edit (Code), selain itu = Properti. "Salin path" pindah ke menu Alat
- [x] feat(ui): Win7Button punya parameter accent; tombol Terminal berikon >_ hijau (senada tab Terminal)
- [x] feat(term): sesi yang dibuka dari Penjelajah bernama folder tujuan di drawer sesi ("[3] molina-studio"); sesi biasa tetap memakai judul shell atau "Debian"
- [x] Dampak: Win7Aero.kt, Win7OpenDialog.kt, TerminalSessions.kt, SessionDrawer.kt, ExplorerBridge.kt. Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (parameter composable baru, referensi `sheet`/`scope` di baris tombol)
- [ ] uji perangkat: pilih video > tombol "Putar"; pilih .sh/.txt > "Edit" membuka Code; pilih folder atau berkas lain > "Properti"; Alat > Salin path; tombol ">_ Terminal" tidak terpotong; Terminal dari folder > drawer menampilkan nama folder; "Sisipkan (3)" pada seleksi banyak masih muat
- [ ] LANGKAH BERIKUT: riwayat folder (5 terakhir) di Favorit
''')
    changed.append("progres.md"); print("  + progres.md")
else:
    print("  = progres.md (sudah ada / tidak ditemukan)")
print("\nSelesai. Perubahan baru: " + str(len(changed)))
PYEOF

RD="$(python3 -c 'import os,sys;print(os.path.abspath(sys.argv[1]))' "${MOLINA_REPO:-.}")"
[ -f "$RD/app/src/main/AndroidManifest.xml" ] || RD="$(ls -d /storage/emulated/0/molina-studio /sdcard/molina-studio 2>/dev/null | head -1)"
cd "$RD" && git add app progres.md && { git diff --cached --quiet && echo "Tidak ada perubahan baru untuk di-commit." || { git commit -m "feat(explorer): aksi cepat kontekstual, ikon >_ Terminal, nama sesi dari folder" && git push; }; }
