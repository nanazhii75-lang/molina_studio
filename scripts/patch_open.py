import os, sys
ROOT = os.getcwd()
CD = ROOT + "/app/src/main/java/com/molina/studio/cinema"
HOME, CH = CD + "/CinemaHome.kt", CD + "/CinemaChannels.kt"
for p in (HOME, CH):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + os.path.basename(p) + ")")
if "IptvLocal" not in open(CH, encoding="utf-8").read():
    sys.exit("BERHENTI: terapkan dulu patch_iptv_local.py (chip Berkas belum ada)")
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

# ===== CinemaChannels: tombol "Buka folder IPTV" + segarkan otomatis setelah Penjelajah ditutup =====
sub(CH, "    onPlay: (List<IptvChannel>, IptvChannel) -> Unit,\n    onDismiss: () -> Unit,\n) {\n",
    "    onPlay: (List<IptvChannel>, IptvChannel) -> Unit,\n    onDismiss: () -> Unit,\n"
    "    onOpenFolder: (() -> Unit)? = null,\n    refreshTick: Int = 0,\n) {\n",
    "onOpenFolder: (() -> Unit)? = null")
sub(CH, "    LaunchedEffect(searchOpen) { if (searchOpen) focus.requestFocus() }\n",
    "    LaunchedEffect(searchOpen) { if (searchOpen) focus.requestFocus() }\n"
    "    LaunchedEffect(refreshTick) { if (picking) refreshFiles() } // Penjelajah ditutup: daftar berkas disegarkan\n",
    "LaunchedEffect(refreshTick)")
sub(CH,
    "                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {\n"
    "                        Text(\"Segarkan\",",
    "                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {\n"
    "                        if (onOpenFolder != null) Text(\"Buka folder IPTV\", color = MolinaTheme.Gold, fontSize = 14.sp,\n"
    "                            fontWeight = FontWeight.SemiBold,\n"
    "                            modifier = Modifier.clickable { onOpenFolder() }.padding(8.dp))\n"
    "                        else Spacer(Modifier.width(1.dp))\n"
    "                        Text(\"Segarkan\",",
    "Text(\"Buka folder IPTV\"")

# ===== CinemaHome: buka Penjelajah Molina (mode jelajah) tepat di folder IPTV =====
sub(HOME, "import com.molina.core.common.PlaylistPlan\n",
    "import com.molina.core.common.MolinaDir\nimport com.molina.core.common.PlaylistPlan\n",
    "import com.molina.core.common.MolinaDir")
sub(HOME, "import com.molina.studio.ui.MolinaTheme\n",
    "import com.molina.studio.AppPaths\nimport com.molina.studio.settings.MolinaExplorer\nimport com.molina.studio.ui.MolinaTheme\n",
    "import com.molina.studio.settings.MolinaExplorer")
sub(HOME, "    var openFolder by rememberSaveable { mutableStateOf(false) }\n",
    "    var openFolder by rememberSaveable { mutableStateOf(false) }\n"
    "    var iptvFolderOpen by rememberSaveable { mutableStateOf(false) }\n"
    "    var iptvTick by remember { mutableStateOf(0) }\n"
    "    val iptvDir = remember { AppPaths.get(ctx).dir(MolinaDir.Iptv) } // /sdcard/Molina/Media/IPTV\n",
    "var iptvFolderOpen")
sub(HOME, "    if (channelsOpen) CinemaChannelsDialog(\n        onDismiss = { channelsOpen = false },\n",
    "    if (channelsOpen) CinemaChannelsDialog(\n        onDismiss = { channelsOpen = false },\n"
    "        onOpenFolder = { runCatching { iptvDir.mkdirs() }; iptvFolderOpen = true },\n"
    "        refreshTick = iptvTick,\n",
    "onOpenFolder = { runCatching")
sub(HOME, "    if (showResults) SearchResultsDialog(\n",
    "    if (iptvFolderOpen) {\n"
    "        remember { MolinaExplorer.attach(ctx) }\n"
    "        Win7OpenDialog(\n"
    "            onDismiss = { iptvFolderOpen = false; iptvTick++ },\n"
    "            browse = true,\n"
    "            startDir = iptvDir,\n"
    "            onCopyPath = { f -> MolinaExplorer.copyPath(ctx, f) },\n"
    "            onShare = { files -> MolinaExplorer.share(ctx, files) },\n"
    "            onInsertPaths = { fs -> MolinaExplorer.insertPaths(ctx, fs) },\n"
    "            isVisibleInDebian = { f -> MolinaExplorer.visibleInDebian(f) },\n"
    "            onPlay = { files -> MolinaExplorer.play(ctx, files) },\n"
    "        )\n"
    "    }\n"
    "    if (showResults) SearchResultsDialog(\n",
    "if (iptvFolderOpen) {")

# ===== progres.md =====
pg = ROOT + "/progres.md"
entry = """
## 2026-10-28 - Saluran Cinema: tombol "Buka folder IPTV" (Penjelajah langsung di folder IPTV) [IPTV-OPEN-FOLDER]
- [x] feat(cinema): panel Berkas di dialog Saluran punya tombol "Buka folder IPTV": membuka Penjelajah Molina (`Win7OpenDialog(browse = true, startDir = /sdcard/Molina/Media/IPTV)`) di atas dialog Saluran, pola sama dengan UnduhanScreen. Folder dibuat bila belum ada
- [x] Saat Penjelajah ditutup, daftar berkas di panel disegarkan otomatis (`refreshTick`), jadi .m3u yang baru disalin/dipindah langsung muncul
- [x] Dampak: :app cinema (CinemaChannels, CinemaHome). Memakai MolinaExplorer yang sudah ada. Gradle, Manifest, JNI mpv, :core:common TIDAK disentuh. Wajib setelah patch_iptv_local.py
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah (parameter baru berdefault, `iptvTick++` pada state delegasi)
- [ ] uji perangkat (Android 8): Saluran > Berkas > Buka folder IPTV > Penjelajah terbuka di folder IPTV; salin/tempel .m3u ke sana, tutup > daftar berkas ikut terbarui tanpa Segarkan; ketuk berkas > saluran termuat
"""
if os.path.isfile(pg):
    s = rd(pg)
    if "[IPTV-OPEN-FOLDER]" in s:
        print("SKIP   progres.md")
    else:
        open(pg, "a", encoding="utf-8").write(("" if s.endswith("\n") else "\n") + entry)
        print("UBAH   progres.md")
else:
    print("GAGAL  progres.md tidak ada"); fails.append(pg)

print("\nSELESAI dengan", len(fails), "kegagalan")
sys.exit(1 if fails else 0)
