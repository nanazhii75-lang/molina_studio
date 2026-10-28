import os, sys
ROOT = os.getcwd()
A = ROOT + "/app/src/main/java/com/molina/studio"
C = ROOT + "/feature/code/src/main/kotlin/com/molina/code/ui"
PROG = ROOT + "/progres.md"

# Tema grafit netral (aksen biru TETAP). Frame, kaca Aero, header Terminal/Code, garis, tombol, dan latar
# kartu diganti dari tint biru ke abu netral. Tidak diubah: aksen (Accent 6CB4FF, NavHeader 8FC4EE, SelFill,
# SelLine, 4DA3FF), warna status/semantik, warna ikon jenis berkas, sapuan RGB, warna ANSI Terminal,
# MolinaTheme.Background/CardBg/CardLine/Muted, themes.xml, ikon peluncur, dan CodeColors (tema editor).
def f(h):
    return "0xFF" + h

PLAN = {
    A + "/ui/MolinaTheme.kt": [
        (f("1A2230"), f("1B1B1B")), (f("0A0F16"), f("0A0A0A")), (f("2E4A68"), f("3A3A3A")),
        (f("07090D"), f("070707")), (f("EAEFF5"), f("EDEDED")), (f("9AA5B1"), f("A3A3A3")),
        (f("5A6675"), f("5C5C5C")), (f("3A4654"), f("3A3A3A")), (f("26303B"), f("262626")),
        (f("1E252E"), f("202020")), (f("10151B"), f("111111")), (f("4A5666"), f("4D4D4D")),
        (f("0F1318"), f("101010")), (f("14181E"), f("151515")), (f("05070A"), f("060606")),
    ],
    A + "/settings/Win7SettingsUi.kt": [
        (f("0B0B0F"), f("0B0B0B")), (f("14181E"), f("151515")), (f("05070A"), f("060606")),
        (f("0F1318"), f("101010")), (f("EAEFF5"), f("EDEDED")), (f("9AA5B1"), f("A3A3A3")),
        (f("26303B"), f("262626")), (f("3A4654"), f("3A3A3A")), (f("171C23"), f("181818")),
        (f("3A4A5E"), f("444444")), (f("4A5A6E"), f("505050")), (f("1E2833"), f("202020")),
    ],
    A + "/settings/Win7Glass.kt": [
        (f("4A7DB5"), f("5A5A5A")), (f("244A75"), f("2A2A2A")), (f("0F1F34"), f("101010")),
        ("0x99BFE0FF", "0x99D0D0D0"), (f("151A22"), f("161616")), (f("080A0E"), f("090909")),
    ],
    A + "/terminal/TerminalChrome.kt": [
        (f("3B6EA5"), f("4A4A4A")), (f("244A75"), f("262626")), (f("14263F"), f("111111")),
        (f("BFD9F2"), f("C8C8C8")), ("0x99BFE0FF", "0x99D0D0D0"), ("0x66BFE0FF", "0x66D0D0D0"),
        (f("2A3A50"), f("3A3A3A")), (f("111A28"), f("151515")),
    ],
    A + "/terminal/TerminalScreen.kt": [(f("2E4A68"), f("3A3A3A"))],
    A + "/cinema/CinemaResults.kt": [(f("1C2430"), f("1F1F1F")), (f("121821"), f("141414"))],
    A + "/cinema/W7FileIcon.kt": [(f("1B1E26"), f("1C1C1C"))],
    A + "/cinema/Win7Aero.kt": [(f("1B1E26"), f("1C1C1C"))],
    A + "/settings/DownloadTileIcon.kt": [(f("3A4A5E"), f("444444"))],
    A + "/settings/PackageScriptUi.kt": [(f("3A4A5E"), f("444444"))],
    A + "/settings/PlayerSettingsUi.kt": [(f("3A4A5E"), f("444444"))],
    C + "/CodeWin7.kt": [
        (f("3B6EA5"), f("4A4A4A")), (f("244A75"), f("262626")), (f("14263F"), f("111111")),
        (f("EAF3FC"), f("EEEEEE")), (f("BFD9F2"), f("C8C8C8")), (f("1A2230"), f("1B1B1B")),
        (f("0A0F16"), f("0A0A0A")), (f("2A4F7A"), f("3A3A3A")), (f("0B1522"), f("111111")),
        (f("1E252E"), f("202020")), (f("10151B"), f("111111")), ("0x99BFE0FF", "0x99D0D0D0"),
    ],
    C + "/CodeWin7Dialog.kt": [
        (f("1F3A5C"), f("2C2C2C")), (f("0C1624"), f("111111")), (f("05080D"), f("060606")),
        ("0x99BFE0FF", "0x99D0D0D0"), (f("1E252E"), f("202020")), (f("10151B"), f("111111")),
        (f("4A5666"), f("4D4D4D")),
    ],
}

# Penjaga: nilai baru tidak boleh sama dengan nilai lama lain di berkas yang sama (mencegah penggantian berantai).
for path, pairs in PLAN.items():
    olds = {o for o, _ in pairs}
    for _, n in pairs:
        if n in olds:
            sys.exit("BERHENTI: rancangan salah, nilai baru " + n + " bentrok di " + os.path.basename(path))
for path in PLAN:
    if not os.path.isfile(path):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + path.replace(ROOT + "/", "") + ")")

fails = []
changed = 0
for path, pairs in PLAN.items():
    s = open(path, encoding="utf-8").read()
    out, did, miss = s, 0, []
    for old, new in pairs:
        if old in out:
            out = out.replace(old, new); did += 1
        elif new not in out:
            miss.append(old)
    name = os.path.basename(path)
    if miss:
        print("GAGAL ", name, "nilai tidak ditemukan:", ", ".join(miss)); fails.append(path); continue
    if out != s:
        open(path, "w", encoding="utf-8").write(out); changed += 1
        print("UBAH  ", name, "(" + str(did) + " nilai)")
    else:
        print("SKIP  ", name)

ENTRY = '''
## 2026-10-28 - Tema grafit netral: frame Win7 biru jadi hitam/abu, aksen biru tetap [THEME-GRAPHITE]
- [x] refactor(ui): tint biru diganti abu netral di MolinaTheme.Explorer (frame, garis, tombol, input, bar), W7D + Win7Scheme (Settings), kaca Win7Glass, header Aero Terminal (TerminalChrome), header/tab/toolbar/dialog Code (CodeWin7, CodeWin7Dialog), border Terminal, baris hasil Cinema, dan latar ikon
- [x] Peta utama: FrameTop 1A2230>1B1B1B, FrameBottom 0A0F16>0A0A0A, FrameLine 2E4A68>3A3A3A, Hairline 26303B>262626, Line 5A6675>5C5C5C, BtnTop/Bottom 1E252E/10151B>202020/111111, Input 0F1318>101010, Bar 14181E/05070A>151515/060606, Ink EAEFF5>EDEDED, TextDim 9AA5B1>A3A3A3; header Aero 3B6EA5/244A75/14263F>4A4A4A/262626/111111; kaca 4A7DB5/244A75/0F1F34>5A5A5A/2A2A2A/101010; tepi kaca 99BFE0FF>99D0D0D0
- [x] TIDAK diubah (aksen A): Accent 6CB4FF, NavHeader 8FC4EE, SelFill 12263A, SelLine 3C7FB1, tombol default 4DA3FF, rail/garis tab Code 8FC4EE, warna status, ikon jenis berkas, sapuan RGB, ANSI Terminal, MolinaTheme.Background/CardBg/CardLine/Muted, themes.xml, ikon peluncur, CodeColors
- [x] Dampak: hanya literal warna di berkas UI Kotlin (:app dan :feature:code). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi/dilihat di perangkat: periksa kontras teks di header Terminal/Code dan tepi kaca; kembalikan dengan git revert commit ini bila tidak cocok
- [ ] LANJUTAN (opsional): opsi B (aksen emas) dan penyatuan W7D ke MolinaTheme.Explorer (semantik nama berbeda: W7D.Line = Explorer.Field)
'''
p = open(PROG, encoding="utf-8").read() if os.path.isfile(PROG) else ""
if "[THEME-GRAPHITE]" in p:
    print("SKIP   progres.md [THEME-GRAPHITE]")
else:
    open(PROG, "a", encoding="utf-8").write(ENTRY); print("UBAH   progres.md [THEME-GRAPHITE]")

if fails:
    sys.exit("SELESAI DENGAN GAGAL: " + str(len(fails)) + " berkas tidak cocok; berkas lain sudah diproses.")
print("OK (" + str(changed) + " berkas diubah)")
