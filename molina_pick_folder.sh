#!/bin/sh
# Molina: mode Pilih bisa membuka folder (idempotent).
# Jalankan dari dalam folder repo: bash molina_pick_folder.sh
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
WD = os.path.join(ROOT, "app/src/main/java/com/molina/studio/cinema/Win7OpenDialog.kt")
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

sub(WD, "    onOpen: (W7Entry) -> Unit,\n    onAsk: () -> Unit\n) {",
    "    onOpen: (W7Entry) -> Unit,\n    onEnter: (W7Entry) -> Unit = {},\n    onAsk: () -> Unit\n) {",
    "onEnter: (W7Entry) -> Unit = {}", "W7FileArea: parameter onEnter")
sub(WD, "                        onAsk = { ask.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }",
    "                        onEnter = { e -> go(e.file) },\n                        onAsk = { ask.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }",
    "onEnter = { e -> go(e.file) }", "pemanggil: ketuk > membuka folder")
sub(WD, "W7EntryRow(e, if (multi) on else e == selected, wide, onOpen, if (multi) on else null)",
    "W7EntryRow(e, if (multi) on else e == selected, wide, onOpen, if (multi) on else null, if (multi) onEnter else null)",
    "if (multi) onEnter else null", "daftar entri meneruskan onEnter")
sub(WD, "onOpen: (W7Entry) -> Unit, checked: Boolean? = null) {",
    "onOpen: (W7Entry) -> Unit, checked: Boolean? = null, onEnter: ((W7Entry) -> Unit)? = null) {",
    "checked: Boolean? = null, onEnter", "W7EntryRow: parameter onEnter")
sub(WD, "            textAlign = TextAlign.End, modifier = Modifier.width(if (wide) 76.dp else 68.dp)\n        )\n    }\n}",
    "            textAlign = TextAlign.End, modifier = Modifier.width(if (wide) 76.dp else 68.dp)\n        )\n"
    "        if (checked != null && e.isDir && onEnter != null) {\n"
    "            Box(\n"
    "                Modifier.width(40.dp).heightIn(min = 32.dp).clickable { onEnter(e) },\n"
    "                contentAlignment = Alignment.Center\n"
    "            ) { Text(\"\\u203A\", color = Win7.NavHeader, fontSize = 22.sp) }\n"
    "        }\n    }\n}",
    "onEnter(e) }", "baris folder: tombol > masuk folder di mode Pilih")
sub(WD, ') { multi = !multi; picked = emptySet(); selected = null; fileName = "" }',
    ') {\n                            multi = !multi; picked = emptySet(); selected = null; fileName = ""\n'
    '                            if (multi) say("Mode Pilih: ketuk untuk mencentang berkas atau folder, ketuk \\u203A untuk membuka folder.", true)\n'
    '                        }',
    "Mode Pilih: ketuk untuk mencentang", "petunjuk saat mode Pilih aktif")

PG = os.path.join(ROOT, "progres.md")
if os.path.isfile(PG) and "EXPLORER-PICK-FOLDER" not in rd(PG):
    with open(PG, "a", encoding="utf-8", newline="\n") as f:
        f.write('''
## 2026-10-27 - Penjelajah: mode Pilih bisa membuka folder [EXPLORER-PICK-FOLDER]
- [x] fix(explorer): di mode Pilih, folder bisa dicentang (sudah) tetapi tidak bisa dibuka karena ketukan hanya mencentang. Kini baris folder punya tombol "›" di kanan untuk masuk folder; ketuk bagian lain tetap mencentang. Petunjuk singkat muncul saat mode Pilih diaktifkan
- [x] Dampak: hanya cinema/Win7OpenDialog.kt (parameter onEnter pada W7FileArea dan W7EntryRow). Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah
- [ ] uji perangkat: Pilih > centang folder + berkas > Salin/Pindah/Hapus/Kompres/Sisipkan; ketuk "›" di folder membuka folder (centang direset, Salin/Pindah yang sudah di clipboard tetap bisa Tempel di folder tujuan); Semua mencentang folder juga; ketuk ">" tidak ikut mencentang barisnya
- [ ] LANGKAH BERIKUT: riwayat folder (5 terakhir) di Favorit
''')
    changed.append("progres.md"); print("  + progres.md")
else:
    print("  = progres.md (sudah ada / tidak ditemukan)")
print("\nSelesai. Perubahan baru: " + str(len(changed)))
PYEOF

RD="$(python3 -c 'import os,sys;print(os.path.abspath(sys.argv[1]))' "${MOLINA_REPO:-.}")"
[ -f "$RD/app/src/main/AndroidManifest.xml" ] || RD="$(ls -d /storage/emulated/0/molina-studio /sdcard/molina-studio 2>/dev/null | head -1)"
cd "$RD" && git add app progres.md && { git diff --cached --quiet && echo "Tidak ada perubahan baru untuk di-commit." || { git commit -m "fix(explorer): mode Pilih bisa membuka folder lewat tombol panah" && git push; }; }
