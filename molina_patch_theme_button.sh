#!/usr/bin/env bash
# Molina UI: token MolinaTheme (satu emas #E6B450) + tombol Pasang/Coba lagi dipersempit.
# Jalankan dari root repo:  cd /sdcard/molina-studio && bash molina_patch_theme_button.sh
# Idempotent. Semua perubahan dihitung di memori dulu; bila ada anchor tak cocok, tidak ada file yang ditulis.
python3 << 'PYEOF'
import os, sys

CANDIDATES = [os.environ.get("MOLINA_ROOT", ""), os.getcwd(),
              "/sdcard/molina-studio", "/storage/emulated/0/molina-studio"]
APP = "app/src/main/java/com/molina/studio/"
TERM = APP + "terminal/"
root = next((c for c in CANDIDATES if c and os.path.isfile(os.path.join(c, TERM + "TerminalScreen.kt"))), None)
if root is None:
    sys.exit("GAGAL: repo molina-studio tidak ditemukan. cd ke root repo atau set MOLINA_ROOT.")
print("Root repo:", root)

THEME = r'''package com.molina.studio.ui

import androidx.compose.ui.graphics.Color

/** Token desain Molina: satu sumber warna merek dan status untuk seluruh UI dan shell. */
object MolinaTheme {
    // Emas merek, acuan visual #E6B450. Nilai ARGB dan SGR (ANSI truecolor) harus tetap sama.
    const val GOLD_ARGB = 0xFFE6B450.toInt()
    const val GOLD_SGR = "38;2;230;180;80"

    val Gold = Color(GOLD_ARGB)
    val Background = Color(0xFF0A0A0F)
    val OnBackground = Color(0xFFEDEDED)
    val Good = Color(0xFF69F0AE)
    val Warn = Color(0xFFFFD54F)
    val Bad = Color(0xFFFF8A80)
    val Muted = Color(0xFF9AA0A6)
    val CardBg = Color(0xFF14161B)
    val CardLine = Color(0xFF2A2D35)
}
'''

files, orig = {}, {}

def load(rel):
    if rel not in files:
        with open(os.path.join(root, rel), encoding="utf-8") as f:
            files[rel] = orig[rel] = f.read()
    return files[rel]

def sub(rel, old, new):
    s = load(rel)
    if new in s:
        return
    if s.count(old) != 1:
        sys.exit("GAGAL: anchor tidak ditemukan tepat satu kali di %s:\n  %s" % (rel, old.strip().splitlines()[0]))
    files[rel] = s.replace(old, new)

def add_import(rel, fq):
    s = load(rel)
    line = "import %s\n" % fq
    if line in s:
        return
    if "\nimport " not in s:
        sys.exit("GAGAL: tidak ada blok import di " + rel)
    files[rel] = s.replace("\nimport ", "\n" + line + "import ", 1)

TOKEN_IMPORT = "com.molina.studio.ui.MolinaTheme"

# MainActivity: skema warna
sub(APP + "MainActivity.kt",
    "private val MolinaColors = darkColorScheme(\n"
    "    background = Color(0xFF0A0A0F),\n"
    "    surface = Color(0xFF0A0A0F),\n"
    "    primary = Color(0xFFE6B450),\n"
    "    onBackground = Color(0xFFEDEDED),\n"
    "    onSurface = Color(0xFFEDEDED)\n"
    ")",
    "private val MolinaColors = darkColorScheme(\n"
    "    background = MolinaTheme.Background,\n"
    "    surface = MolinaTheme.Background,\n"
    "    primary = MolinaTheme.Gold,\n"
    "    onBackground = MolinaTheme.OnBackground,\n"
    "    onSurface = MolinaTheme.OnBackground\n"
    ")")
add_import(APP + "MainActivity.kt", TOKEN_IMPORT)

# Notifikasi service
sub(TERM + "TerminalService.kt", ".setColor(0xFFE6B450.toInt())", ".setColor(MolinaTheme.GOLD_ARGB)")
add_import(TERM + "TerminalService.kt", TOKEN_IMPORT)

# Drawer sesi
sub(TERM + "SessionDrawer.kt", "private val Gold = Color(0xFFE6B450)", "private val Gold = MolinaTheme.Gold")
add_import(TERM + "SessionDrawer.kt", TOKEN_IMPORT)

# Tema prompt Cinema Gold: #D4AF37 -> emas merek
sub(TERM + "TerminalPrefs.kt",
    '        "1;38;2;212;175;55", "1;38;2;196;69;54",\n'
    '        Color(0xFFD4AF37), Color(0xFFC44536)',
    '        "1;${MolinaTheme.GOLD_SGR}", "1;38;2;196;69;54",\n'
    '        MolinaTheme.Gold, Color(0xFFC44536)')
add_import(TERM + "TerminalPrefs.kt", TOKEN_IMPORT)

# Banner shell
sub(TERM + "MolinaShell.kt",
    'const val BRAND_SGR = "1;38;2;212;175;55" // kuning emas',
    'const val BRAND_SGR = "1;${MolinaTheme.GOLD_SGR}" // emas Molina')
add_import(TERM + "MolinaShell.kt", TOKEN_IMPORT)

# SetupPanel: warna dari token + tombol lebih pendek
sp = TERM + "SetupPanel.kt"
sub(sp, "private val SetupCardBg = Color(0xFF14161B)", "private val SetupCardBg = MolinaTheme.CardBg")
sub(sp, "private val SetupCardLine = Color(0xFF2A2D35)", "private val SetupCardLine = MolinaTheme.CardLine")
sub(sp, "private val SetupMuted = Color(0xFF9AA0A6)", "private val SetupMuted = MolinaTheme.Muted")
sub(sp, "private val SetupGood = Color(0xFF69F0AE)", "private val SetupGood = MolinaTheme.Good")
sub(sp, "private val SetupBad = Color(0xFFFF8A80)", "private val SetupBad = MolinaTheme.Bad")
sub(sp, "private val SetupWarn = Color(0xFFFFD54F)", "private val SetupWarn = MolinaTheme.Warn")
sub(sp, "private val SetupButtonText = Color(0xFF1A1405)\n",
        "private val SetupButtonText = Color(0xFF1A1405)\nprivate val SetupButtonWidth = 176.dp\n")
sub(sp, "modifier = Modifier.fillMaxWidth().height(44.dp),", "modifier = Modifier.width(SetupButtonWidth).height(40.dp),")
add_import(sp, TOKEN_IMPORT)

# StatusChips: warna lonjakan dari token
sub(TERM + "StatusChips.kt", "private val ChipWarn = Color(0xFFFFD54F)", "private val ChipWarn = MolinaTheme.Warn")
add_import(TERM + "StatusChips.kt", TOKEN_IMPORT)

# progres.md
ENTRY = ("- 2026-10-23: Token desain: `MolinaTheme` (`ui/MolinaTheme.kt`) jadi satu sumber warna "
         "(emas #E6B450 + Good/Warn/Bad/Muted/Card); MainActivity, SessionDrawer, TerminalService, "
         "SetupPanel, StatusChips memakainya; emas tema prompt Cinema Gold dan banner "
         "`MolinaShell.BRAND_SGR` disatukan dari #D4AF37 ke #E6B450 (SGR 230;180;80); tombol "
         "Pasang/Coba lagi dipersempit (176dp x 40dp). Menunggu CI + uji perangkat.")
pp = "progres.md"
if os.path.isfile(os.path.join(root, pp)):
    t = load(pp)
    if "ui/MolinaTheme.kt" not in t:
        mark = "\n\n## Roadmap Prioritas"
        t = t.replace(mark, "\n" + ENTRY + mark, 1) if mark in t else t.rstrip("\n") + "\n" + ENTRY + "\n"
        files[pp] = t
    files[pp] = files[pp].replace("token desain `MolinaTheme` (emas #D4AF37)",
                                  "token desain `MolinaTheme` (emas #E6B450)")
else:
    print("PERINGATAN: progres.md tidak ditemukan")

# Tulis hasil
theme_path = os.path.join(root, APP + "ui/MolinaTheme.kt")
if os.path.isfile(theme_path) and open(theme_path, encoding="utf-8").read() == THEME:
    print("sudah ada : ui/MolinaTheme.kt")
else:
    os.makedirs(os.path.dirname(theme_path), exist_ok=True)
    with open(theme_path, "w", encoding="utf-8") as f:
        f.write(THEME)
    print("ditulis   : ui/MolinaTheme.kt")
for rel, text in files.items():
    if text != orig[rel]:
        with open(os.path.join(root, rel), "w", encoding="utf-8") as f:
            f.write(text)
        print("dipatch   :", rel)
    else:
        print("sudah ada :", rel)

print("\nSelesai. Berikutnya:")
print("  git add -A && git commit -m \"refactor(ui): token MolinaTheme (emas #E6B450) dan tombol pasang dipersempit\"")
PYEOF
