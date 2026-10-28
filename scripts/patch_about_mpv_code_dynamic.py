import os, sys
ROOT = os.getcwd()
J = ROOT + "/app/src/main/java/com/molina/studio"
MPV = ROOT + "/feature/mpv/src/main/java/com/molina/cinema"
XML = ROOT + "/app/src/main/res/xml/molina_shortcuts.xml"
ABOUT, SHORT = J + "/settings/AboutUi.kt", J + "/MolinaShortcuts.kt"
NOTE, BRIDGE = MPV + "/MpvVersionNote.kt", J + "/cinema/MpvVersionBridge.kt"
ACT = MPV + "/MPVActivity.kt"
PROG = ROOT + "/progres.md"
for p in (ABOUT, SHORT, ACT, XML):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio dan terapkan dulu patch_hub_status.py + "
                 "patch_shortcuts_dynamic.py (tidak ada " + os.path.basename(p) + ")")
fails = []

def rd(p):
    return open(p, encoding="utf-8").read()

def sub(p, old, new, marker):
    s = rd(p)
    if marker in s:
        print("SKIP  ", os.path.basename(p), "[" + marker[:40].replace("\n", " ") + "]"); return
    if s.count(old) != 1:
        print("GAGAL ", os.path.basename(p), "pola tidak unik/tidak ada:", old[:60].replace("\n", " "))
        fails.append(p); return
    open(p, "w", encoding="utf-8").write(s.replace(old, new))
    print("UBAH  ", os.path.basename(p), "[" + marker[:40].replace("\n", " ") + "]")

def newfile(p, content):
    if os.path.isfile(p) and rd(p) == content:
        print("SKIP  ", os.path.basename(p), "(sudah sama)"); return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w", encoding="utf-8").write(content)
    print("BARU  ", os.path.basename(p))

# ===== A. Versi libmpv dan FFmpeg di About =====
# Pemutar mencatat versi dari mpv yang sedang berjalan (properti mpv-version, ffmpeg-version) tiap
# diinisialisasi; About hanya membaca catatan itu, tanpa membuat instance mpv di Settings.
newfile(NOTE, r'''package com.molina.cinema

import android.content.Context

/**
 * Mencatat versi libmpv dan FFmpeg yang benar-benar termuat (properti mpv-version dan ffmpeg-version),
 * dipanggil sekali setelah mpv diinisialisasi. Disimpan di SharedPreferences molina_ui agar halaman About
 * di :app dapat menampilkannya tanpa menyentuh JNI. Kunci disalin di app/.../cinema/MpvVersionBridge.kt.
 */
internal object MpvVersionNote {
    fun record(ctx: Context) {
        val mpv = read("mpv-version")
        val ffmpeg = read("ffmpeg-version")
        if (mpv == null && ffmpeg == null) return
        ctx.applicationContext.getSharedPreferences("molina_ui", Context.MODE_PRIVATE).edit().apply {
            if (mpv != null) putString("mpv_version", mpv)
            if (ffmpeg != null) putString("ffmpeg_version", ffmpeg)
        }.apply()
    }

    private fun read(property: String): String? =
        runCatching { MPVLib.getPropertyString(property) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.take(80)
}
''')

newfile(BRIDGE, r'''package com.molina.studio.cinema

import android.content.Context

/** Pembaca versi libmpv/FFmpeg yang dicatat pemutar (MpvVersionNote di :feature:mpv); null = belum pernah memutar. */
internal object MpvVersionBridge {
    private const val PREFS = "molina_ui"

    fun mpv(ctx: Context): String? = read(ctx, "mpv_version")

    fun ffmpeg(ctx: Context): String? = read(ctx, "ffmpeg_version")

    private fun read(ctx: Context, key: String): String? =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null)?.takeIf { it.isNotBlank() }
}
''')

sub(ACT, "        player.initialize(filesDir.path, cacheDir.path)\n",
    "        player.initialize(filesDir.path, cacheDir.path)\n        MpvVersionNote.record(applicationContext)\n",
    "MpvVersionNote.record(applicationContext)")
sub(ABOUT, "import com.molina.studio.automation.AutoPrefs\n",
    "import com.molina.studio.automation.AutoPrefs\nimport com.molina.studio.cinema.MpvVersionBridge\n",
    "import com.molina.studio.cinema.MpvVersionBridge")
sub(ABOUT,
    '            "Arsitektur" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "?")\n        )',
    '            "Arsitektur" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "?"),\n'
    '            "libmpv" to (MpvVersionBridge.mpv(ctx) ?: "belum tercatat (putar video sekali)"),\n'
    '            "FFmpeg" to (MpvVersionBridge.ffmpeg(ctx) ?: "belum tercatat (putar video sekali)")\n        )',
    "MpvVersionBridge.mpv(ctx)")

# ===== B. Code: dari shortcut statis menjadi dinamis =====
s = rd(XML)
CODE_BLOCK = '''    <shortcut
        android:shortcutId="molina_code"
        android:enabled="true"
        android:icon="@mipmap/ic_shortcut_code"
        android:shortcutShortLabel="@string/shortcut_code_short"
        android:shortcutLongLabel="@string/shortcut_code_long">
        <intent
            android:action="android.intent.action.MAIN"
            android:targetPackage="com.molina.studio"
            android:targetClass="com.molina.studio.MainActivity">
            <extra android:name="molina_open_tab" android:value="code" />
        </intent>
    </shortcut>
'''
sub(XML, CODE_BLOCK, "    <!-- Code: shortcut dinamis (MolinaShortcuts.kt), bukan statis -->\n",
    "Code: shortcut dinamis (MolinaShortcuts.kt)")

sub(SHORT,
    ' * dan "Penyimpanan". Pelengkap shortcut statis (molina_shortcuts.xml). Intent hanya membawa nama tujuan\n',
    ' * "Penyimpanan", dan "Code" (dulu statis). Pelengkap shortcut statis Cinema dan Terminal (molina_shortcuts.xml).\n'
    ' * Urutan = prioritas tampil di launcher (umumnya 4 teratas, statis lebih dulu). Intent hanya membawa nama tujuan\n',
    '"Penyimpanan", dan "Code" (dulu statis)')
sub(SHORT,
    '        val room = (sm.maxShortcutCountPerActivity',
    '        all += item(ctx, "molina_code_dyn", "code", R.string.shortcut_code_short, R.string.shortcut_code_long, R.mipmap.ic_shortcut_code, 3)\n'
    '        val room = (sm.maxShortcutCountPerActivity',
    '"molina_code_dyn"')

# ===== progres.md =====
ENTRY = '''
## 2026-10-28 - About: versi libmpv/FFmpeg + Code jadi shortcut dinamis [ABOUT-MPV-CODE]
- [x] feat(about): baris "libmpv" dan "FFmpeg" di Versi dan perangkat (ikut Salin info debug). Sumber: MpvVersionNote (:feature:mpv) mencatat properti mpv-version dan ffmpeg-version dari mpv yang berjalan, sekali setelah player.initialize; About membaca lewat MpvVersionBridge (SharedPreferences molina_ui). Sebelum pernah memutar video tampil "belum tercatat (putar video sekali)"
- [x] refactor(shortcut): shortcut Code dipindah dari molina_shortcuts.xml (statis) ke MolinaShortcuts (dinamis, id molina_code_dyn, rank 3). Statis tersisa: Cinema, Terminal. Urutan dinamis: Putar terakhir, Unduhan, Penyimpanan, Code
- [x] Dampak: :feature:mpv (MpvVersionNote.kt baru, satu baris di MPVActivity setelah player.initialize; hanya memanggil MPVLib.getPropertyString yang sudah ada, TANPA mengubah binding JNI/C++), :app (MpvVersionBridge, AboutUi, MolinaShortcuts, molina_shortcuts.xml). Manifest dan Gradle TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko rendah
- [ ] uji perangkat (Android 8): putar video sekali > Settings > About: libmpv dan FFmpeg terisi; tekan lama ikon: Cinema, Terminal, lalu dinamis; ketuk Code membuka tab Code
- [ ] KETERBATASAN: launcher menampilkan sekitar 4 shortcut (statis dulu), jadi Penyimpanan dan Code di bawah lipatan pada launcher 4 slot; shortcut Code lama yang sudah disematkan di layar utama (id molina_code) menjadi nonaktif dan perlu disematkan ulang; SHA commit build belum ditampilkan (butuh buildConfig di Gradle)
'''
if "[ABOUT-MPV-CODE]" in (rd(PROG) if os.path.isfile(PROG) else ""):
    print("SKIP   progres.md [ABOUT-MPV-CODE]")
else:
    open(PROG, "a", encoding="utf-8").write(ENTRY); print("UBAH   progres.md [ABOUT-MPV-CODE]")

if fails:
    sys.exit("SELESAI DENGAN GAGAL: " + str(len(fails)) + " pola tidak cocok; tidak ada yang dipaksa.")
print("OK")
