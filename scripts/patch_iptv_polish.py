import os, sys
ROOT = os.getcwd()
CD = ROOT + "/app/src/main/java/com/molina/studio/cinema"
HOME, CH = CD + "/CinemaHome.kt", CD + "/CinemaChannels.kt"
for p in (HOME, CH):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + os.path.basename(p) + ")")
if "onOpenFolder: (() -> Unit)? = null" not in open(CH, encoding="utf-8").read():
    sys.exit("BERHENTI: terapkan dulu patch_open.py (tombol Buka folder IPTV belum ada)")
fails = []

def rd(p):
    return open(p, encoding="utf-8").read()

def sub(p, old, new, marker):
    s = rd(p)
    if marker in s:
        print("SKIP  ", os.path.basename(p), "[" + marker[:40] + "]"); return
    if s.count(old) != 1:
        print("GAGAL ", os.path.basename(p), "pola tidak unik/tidak ada:", old[:60].replace("\n", " "))
        fails.append(p); return
    open(p, "w", encoding="utf-8").write(s.replace(old, new))
    print("UBAH  ", os.path.basename(p), "[" + marker[:40] + "]")

# ===== CinemaChannels =====
# 1) impor ikon folder Molina (Canvas, tanpa glyph font)
sub(CH, "import com.molina.studio.ui.MolinaTheme\n",
    "import com.molina.studio.settings.TabFolderIcon\nimport com.molina.studio.ui.MolinaTheme\n",
    "import com.molina.studio.settings.TabFolderIcon")
# 2) parameter baru: buka berkas di Code
sub(CH, "    refreshTick: Int = 0,\n) {\n",
    "    refreshTick: Int = 0,\n    onOpenInCode: ((File) -> Unit)? = null,\n) {\n",
    "onOpenInCode: ((File) -> Unit)? = null")
# 3) tombol "Folder IPTV" + ikon folder kuning
sub(CH,
    "                        if (onOpenFolder != null) Text(\"Buka folder IPTV\", color = MolinaTheme.Gold, fontSize = 14.sp,\n"
    "                            fontWeight = FontWeight.SemiBold,\n"
    "                            modifier = Modifier.clickable { onOpenFolder() }.padding(8.dp))\n"
    "                        else Spacer(Modifier.width(1.dp))\n",
    "                        if (onOpenFolder != null) {\n"
    "                            Row(Modifier.clickable { onOpenFolder() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {\n"
    "                                Box(Modifier.size(20.dp)) { TabFolderIcon(MolinaTheme.Explorer.Folder) }\n"
    "                                Spacer(Modifier.width(6.dp))\n"
    "                                Text(\"Folder IPTV\", color = MolinaTheme.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)\n"
    "                            }\n"
    "                        } else Spacer(Modifier.width(1.dp))\n",
    "TabFolderIcon(MolinaTheme.Explorer.Folder)")
# 4) petunjuk folder kosong menunjuk ke tombol Folder IPTV
sub(CH, "Belum ada berkas .m3u / .m3u8 di folder ini. Salin playlist ke sana, lalu ketuk Segarkan.",
    "Belum ada berkas .m3u / .m3u8 di folder ini. Ketuk Folder IPTV, salin playlist ke sana, lalu tutup Penjelajah.",
    "Ketuk Folder IPTV, salin playlist")
# 5) tombol "Code" di tiap baris berkas
sub(CH, "list.forEach { e -> FileRow(e) { pickFile(e.file) } }",
    "list.forEach { e -> FileRow(e, onCode = onOpenInCode?.let { fn -> { fn(e.file) } }) { pickFile(e.file) } }",
    "FileRow(e, onCode = onOpenInCode")
sub(CH, "private fun FileRow(e: IptvLocal.Entry, onClick: () -> Unit) {",
    "private fun FileRow(e: IptvLocal.Entry, onCode: (() -> Unit)? = null, onClick: () -> Unit) {",
    "private fun FileRow(e: IptvLocal.Entry, onCode")
sub(CH, "        Text(sizeLabel(e.sizeBytes), color = MolinaTheme.Muted, fontSize = 12.sp)\n    }\n}\n",
    "        Text(sizeLabel(e.sizeBytes), color = MolinaTheme.Muted, fontSize = 12.sp)\n"
    "        if (onCode != null) Text(\"Code\", color = MolinaTheme.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,\n"
    "            modifier = Modifier.clickable(onClick = onCode).padding(horizontal = 8.dp, vertical = 4.dp))\n"
    "    }\n}\n",
    "Text(\"Code\", color = MolinaTheme.Gold")

# ===== CinemaHome: dialog Saluran dan Penjelajah IPTV membuka berkas di tab Code =====
sub(HOME, "        refreshTick = iptvTick,\n",
    "        refreshTick = iptvTick,\n"
    "        onOpenInCode = { f -> channelsOpen = false; MolinaExplorer.openInCode(ctx, listOf(f)) },\n",
    "onOpenInCode = { f -> channelsOpen = false")
sub(HOME, "            onPlay = { files -> MolinaExplorer.play(ctx, files) },\n",
    "            onPlay = { files -> MolinaExplorer.play(ctx, files) },\n"
    "            onOpenInCode = { files -> iptvFolderOpen = false; channelsOpen = false; MolinaExplorer.openInCode(ctx, files) },\n",
    "iptvFolderOpen = false; channelsOpen = false;")

# ===== progres.md =====
pg = ROOT + "/progres.md"
entry = """
## 2026-10-28 - Saluran Cinema: poles tombol Folder IPTV, petunjuk folder kosong, Buka di Code [IPTV-POLISH]
- [x] style(cinema): tombol "Buka folder IPTV" jadi "Folder IPTV" dengan ikon folder kuning (`TabFolderIcon` + `MolinaTheme.Explorer.Folder`), sejajar bahasa visual Penjelajah
- [x] Petunjuk folder kosong kini mengarahkan ke tombol Folder IPTV (salin playlist, lalu tutup Penjelajah; daftar tersegarkan otomatis)
- [x] feat(cinema): tiap berkas di panel Berkas punya tombol "Code" yang menutup dialog Saluran lalu membuka berkas di tab Code (`MolinaExplorer.openInCode` -> `CodeNav.pendingFiles`). Penjelajah IPTV juga meneruskan `onOpenInCode`, jadi menu "Buka di Code" dan ketuk ulang berkas teks bekerja
- [x] Dampak: :app cinema (CinemaChannels, CinemaHome). Gradle, Manifest, JNI mpv, :core:common TIDAK disentuh. Wajib setelah patch_open.py
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah (parameter baru berdefault, `onCode?.let { fn -> { fn(e.file) } }`, `Box(Modifier.size(20.dp)) { TabFolderIcon }`)
- [ ] uji perangkat (Android 8): Saluran > Berkas > ikon folder kuning + "Folder IPTV" terbuka di folder IPTV; folder kosong tampil petunjuk baru; ketuk "Code" pada sebuah .m3u > pindah ke tab Code dengan berkas terbuka; di Penjelajah IPTV ketuk ulang .m3u > terbuka di Code
- [ ] KETERBATASAN: setelah pindah ke Code, kembali ke Cinema dialog Saluran sudah tertutup; ketuk berkas lain di baris (bukan "Code") tetap memuat saluran
"""
if os.path.isfile(pg):
    s = rd(pg)
    if "[IPTV-POLISH]" in s:
        print("SKIP   progres.md")
    else:
        open(pg, "a", encoding="utf-8").write(("" if s.endswith("\n") else "\n") + entry)
        print("UBAH   progres.md")
else:
    print("GAGAL  progres.md tidak ada"); fails.append(pg)

print("\nSELESAI dengan", len(fails), "kegagalan")
sys.exit(1 if fails else 0)
