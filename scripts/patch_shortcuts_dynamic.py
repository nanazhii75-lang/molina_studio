import os, sys
ROOT = os.getcwd()
M = ROOT + "/app/src/main"
J = M + "/java/com/molina/studio"
RES = M + "/res"
MAIN, RECENT, ACTIONS = J + "/MainActivity.kt", J + "/cinema/CinemaRecent.kt", J + "/cinema/CinemaActions.kt"
SCREEN = J + "/settings/SettingsScreen.kt"
SHORT = J + "/MolinaShortcuts.kt"
PROG = ROOT + "/progres.md"
for p in (MAIN, RECENT, ACTIONS, SCREEN):
    if not os.path.isfile(p):
        sys.exit("BERHENTI: jalankan dari root repo molina-studio (tidak ada " + os.path.basename(p) + ")")
if not os.path.isfile(RES + "/drawable/ic_launcher_background.xml"):
    sys.exit("BERHENTI: ic_launcher_background.xml tidak ada (struktur res berubah?)")
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

# ===== ikon: foreground vektor + adaptive icon (pola sama dengan ic_shortcut_code) =====
def fg(color, path):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            '    <path\n        android:fillColor="#00000000"\n'
            '        android:strokeColor="' + color + '"\n'
            '        android:strokeWidth="6"\n        android:strokeLineCap="round"\n'
            '        android:strokeLineJoin="round"\n'
            '        android:pathData="' + path + '" />\n</vector>\n')

def adaptive(name):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/ic_launcher_background" />\n'
            '    <foreground android:drawable="@drawable/ic_shortcut_' + name + '_foreground" />\n'
            '</adaptive-icon>\n')

for name, color, path in (
    ("last", "#B9A7E8", "M44,38L44,70L72,54Z"),
    ("downloads", "#6CB4FF", "M54,32L54,60M40,48L54,62L68,48M36,74L72,74"),
    ("storage", "#8FD0A0", "M32,44L76,44L76,68L32,68ZM32,56L76,56M66,62L67,62"),
):
    newfile(RES + "/drawable/ic_shortcut_" + name + "_foreground.xml", fg(color, path))
    newfile(RES + "/mipmap-anydpi-v26/ic_shortcut_" + name + ".xml", adaptive(name))

newfile(RES + "/values/molina_shortcuts_dynamic.xml", '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="shortcut_last_short">Putar terakhir</string>
    <string name="shortcut_last_long">Putar terakhir (Molina Cinema)</string>
    <string name="shortcut_downloads_short">Unduhan</string>
    <string name="shortcut_downloads_long">Molina Unduhan</string>
    <string name="shortcut_storage_short">Penyimpanan</string>
    <string name="shortcut_storage_long">Penyimpanan Molina</string>
</resources>
''')

# ===== MolinaShortcuts.kt =====
newfile(SHORT, r'''package com.molina.studio

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.molina.core.common.LogLevel
import com.molina.studio.cinema.CinemaRecent
import com.molina.studio.log.LogBus
import java.util.concurrent.Executors

/**
 * Shortcut dinamis ikon (tekan lama): "Putar terakhir" (hanya bila riwayat Cinema tidak kosong), "Unduhan",
 * dan "Penyimpanan". Pelengkap shortcut statis (molina_shortcuts.xml). Intent hanya membawa nama tujuan
 * (molina_open_tab); URL atau judul riwayat TIDAK dimasukkan karena shortcut dapat dibaca aplikasi launcher.
 * Jumlah yang dipublikasikan dibatasi sisa kuota (maks. per aktivitas dikurangi shortcut statis).
 */
internal object MolinaShortcuts {
    private const val EXTRA_TAB = "molina_open_tab"
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "molina-shortcuts").apply { isDaemon = true } }

    /** Aman dipanggil dari thread mana pun; kerja IPC dilakukan di thread latar. */
    fun refresh(ctx: Context) {
        val app = ctx.applicationContext
        io.execute {
            runCatching { publish(app) }.onFailure {
                LogBus.log("sistem", LogLevel.Warn, "shortcut", "Gagal memperbarui shortcut: ${it.message}")
            }
        }
    }

    private fun item(ctx: Context, id: String, tab: String, short: Int, long: Int, icon: Int, rank: Int): ShortcutInfo =
        ShortcutInfo.Builder(ctx, id)
            .setShortLabel(ctx.getString(short))
            .setLongLabel(ctx.getString(long))
            .setIcon(Icon.createWithResource(ctx, icon))
            .setIntent(Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_MAIN).putExtra(EXTRA_TAB, tab))
            .setRank(rank)
            .build()

    private fun publish(ctx: Context) {
        val sm = ctx.getSystemService(ShortcutManager::class.java) ?: return
        if (sm.isRateLimitingActive) return
        val all = ArrayList<ShortcutInfo>()
        if (CinemaRecent.load(ctx).isNotEmpty()) {
            all += item(ctx, "molina_last", "play_last", R.string.shortcut_last_short, R.string.shortcut_last_long, R.mipmap.ic_shortcut_last, 0)
        }
        all += item(ctx, "molina_downloads", "downloads", R.string.shortcut_downloads_short, R.string.shortcut_downloads_long, R.mipmap.ic_shortcut_downloads, 1)
        all += item(ctx, "molina_storage", "storage", R.string.shortcut_storage_short, R.string.shortcut_storage_long, R.mipmap.ic_shortcut_storage, 2)
        val room = (sm.maxShortcutCountPerActivity - sm.manifestShortcuts.size).coerceAtLeast(0)
        if (!sm.setDynamicShortcuts(all.take(room))) {
            LogBus.log("sistem", LogLevel.Warn, "shortcut", "Shortcut dinamis ditolak sistem (kemungkinan dibatasi laju)")
        }
    }
}
''')

# ===== CinemaActions.playLast =====
sub(ACTIONS, "    fun openSettings(ctx: Context): Boolean =",
    "    /** Putar item teratas riwayat Cinema (shortcut \"Putar terakhir\"); false bila riwayat kosong atau gagal. */\n"
    "    fun playLast(ctx: Context): Boolean {\n"
    "        val r = CinemaRecent.load(ctx).firstOrNull() ?: return false\n"
    "        if (r.ref.startsWith(\"/\")) return playPath(ctx, r.ref)\n"
    "        return when (val i = classify(r.ref)) {\n"
    "            is Input.Direct -> play(ctx, i.uri)\n"
    "            else -> false\n"
    "        }\n"
    "    }\n\n"
    "    fun openSettings(ctx: Context): Boolean =",
    "fun playLast(ctx: Context)")

# ===== CinemaRecent: segarkan shortcut saat riwayat berubah =====
sub(RECENT,
    "        prefs(ctx).edit().putString(KEY, arr.toString()).apply()\n        version++\n    }",
    "        prefs(ctx).edit().putString(KEY, arr.toString()).apply()\n"
    "        com.molina.studio.MolinaShortcuts.refresh(ctx)\n        version++\n    }",
    "apply()\n        com.molina.studio.MolinaShortcuts.refresh(ctx)\n        version++")
sub(RECENT,
    "        prefs(ctx).edit().remove(KEY).apply()\n        version++\n    }",
    "        prefs(ctx).edit().remove(KEY).apply()\n        version++\n"
    "        com.molina.studio.MolinaShortcuts.refresh(ctx)\n    }",
    "version++\n        com.molina.studio.MolinaShortcuts.refresh(ctx)")

# ===== MainActivity: tujuan baru + publikasi saat start =====
sub(MAIN, '                "files" -> Tab.Files.ordinal\n',
    '                "downloads" -> {\n'
    '                    com.molina.studio.download.DownloadNav.openTab = true\n'
    '                    -1\n'
    '                }\n'
    '                "storage" -> {\n'
    '                    com.molina.studio.settings.SettingsNav.openStorage = true\n'
    '                    Tab.Settings.ordinal\n'
    '                }\n'
    '                "play_last" -> {\n'
    '                    if (!com.molina.studio.cinema.CinemaActions.playLast(this)) {\n'
    '                        android.widget.Toast.makeText(this, "Belum ada riwayat putar", android.widget.Toast.LENGTH_SHORT).show()\n'
    '                    }\n'
    '                    Tab.Mpv.ordinal\n'
    '                }\n'
    '                "files" -> Tab.Files.ordinal\n',
    '"play_last" ->')
sub(MAIN, "        LogServer.start()\n        applyPrivacy()\n",
    "        LogServer.start()\n        MolinaShortcuts.refresh(applicationContext)\n        applyPrivacy()\n",
    "MolinaShortcuts.refresh(applicationContext)")

# ===== SettingsNav + SettingsScreen: buka halaman Penyimpanan dari shortcut =====
sub(SCREEN, "    var openDownloads by mutableStateOf(false)\n",
    "    var openDownloads by mutableStateOf(false)\n    var openStorage by mutableStateOf(false)\n",
    "var openStorage by mutableStateOf(false)")
sub(SCREEN,
    "    LaunchedEffect(SettingsNav.openDownloads) {\n"
    "        if (SettingsNav.openDownloads) {\n"
    "            open(Page.Downloads)\n"
    "            SettingsNav.openDownloads = false\n"
    "        }\n"
    "    }\n",
    "    LaunchedEffect(SettingsNav.openDownloads) {\n"
    "        if (SettingsNav.openDownloads) {\n"
    "            open(Page.Downloads)\n"
    "            SettingsNav.openDownloads = false\n"
    "        }\n"
    "    }\n"
    "    LaunchedEffect(SettingsNav.openStorage) {\n"
    "        if (SettingsNav.openStorage) {\n"
    "            open(Page.Storage)\n"
    "            SettingsNav.openStorage = false\n"
    "        }\n"
    "    }\n",
    "LaunchedEffect(SettingsNav.openStorage)")

# ===== progres.md =====
ENTRY = '''
## 2026-10-28 - Shortcut dinamis ikon: Putar terakhir, Unduhan, Penyimpanan [SHORTCUT-DYNAMIC]
- [x] feat(shortcut): `MolinaShortcuts` (ShortcutManager.setDynamicShortcuts, thread latar) menerbitkan "Putar terakhir" (hanya bila riwayat Cinema tidak kosong), "Unduhan", dan "Penyimpanan"; dipanggil saat MainActivity dibuat dan setiap riwayat Cinema berubah (simpan/hapus)
- [x] Privasi: intent hanya memuat nama tujuan (`molina_open_tab` = play_last | downloads | storage); URL dan judul riwayat TIDAK masuk shortcut (bisa dibaca launcher). Tujuan "play_last" diselesaikan saat diketuk lewat `CinemaActions.playLast`
- [x] MainActivity.handleOpenTabIntent: tujuan baru downloads (DownloadNav.openTab), storage (SettingsNav.openStorage > halaman Penyimpanan), play_last (putar item teratas riwayat, pindah ke tab Cinema)
- [x] Ikon baru (adaptive, pola ic_shortcut_code): ic_shortcut_last (lavender), ic_shortcut_downloads (biru), ic_shortcut_storage (hijau); string di values/molina_shortcuts_dynamic.xml
- [x] Dampak: :app (MolinaShortcuts.kt, res baru, MainActivity, CinemaRecent, CinemaActions, SettingsScreen). molina_shortcuts.xml, AndroidManifest.xml, Gradle, JNI mpv TIDAK disentuh; 3 shortcut statis (Cinema, Terminal, Code) tetap
- [ ] BELUM dikompilasi Gradle (CI): risiko rendah-sedang (`Icon.createWithResource` dengan adaptive-icon mipmap, `setDynamicShortcuts`, `when` mengembalikan Int di handleOpenTabIntent)
- [ ] uji perangkat (Android 8): putar video > tekan lama ikon > "Putar terakhir" muncul dan memutar ulang; Settings > Pemutar/Cinema hapus riwayat > shortcut itu hilang; ketuk "Unduhan" dan "Penyimpanan" membuka halaman masing-masing (saat app mati maupun hidup)
- [ ] KETERBATASAN: launcher umumnya hanya menampilkan 4 shortcut dan menaruh yang statis lebih dulu; dengan 3 statis, hanya 1 dinamis (Putar terakhir) yang pasti terlihat. Kuota total dibatasi `maxShortcutCountPerActivity` dikurangi shortcut statis
'''
if "[SHORTCUT-DYNAMIC]" in (rd(PROG) if os.path.isfile(PROG) else ""):
    print("SKIP   progres.md [SHORTCUT-DYNAMIC]")
else:
    open(PROG, "a", encoding="utf-8").write(ENTRY); print("UBAH   progres.md [SHORTCUT-DYNAMIC]")

if fails:
    sys.exit("SELESAI DENGAN GAGAL: " + str(len(fails)) + " pola tidak cocok; tidak ada yang dipaksa.")
print("OK")
