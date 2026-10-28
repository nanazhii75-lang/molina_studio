python3 << 'PYEOF'
import os, sys
ROOT = os.environ.get("MOLINA_ROOT", "/storage/emulated/0/molina-studio")
SRC = ROOT + "/app/src/main/java/com/molina/studio"
fails = []

def rd(p):
    return open(p, encoding="utf-8").read()

if not os.path.isfile(SRC + "/automation/AutoPrefs.kt"):
    print("BERHENTI: jalankan dulu patch_molina_auto.sh (automation/AutoPrefs.kt belum ada)")
    sys.exit(2)

def write_new(rel, text):
    p = SRC + "/" + rel
    os.makedirs(os.path.dirname(p), exist_ok=True)
    if os.path.isfile(p) and rd(p) == text:
        print("SKIP  ", rel); return
    open(p, "w", encoding="utf-8").write(text)
    print("TULIS ", rel)

def sub(p, old, new, marker):
    s = rd(p)
    if marker in s:
        print("SKIP  ", os.path.basename(p), "[" + marker[:30] + "]"); return
    if s.count(old) != 1:
        print("GAGAL ", os.path.basename(p), "pola tidak unik/tidak ada:", old[:50].replace("\n", " "))
        fails.append(p); return
    open(p, "w", encoding="utf-8").write(s.replace(old, new))
    print("UBAH  ", os.path.basename(p), "[" + marker[:30] + "]")

# ---------------- berkas baru ----------------
write_new("cinema/ResumeBridge.kt", r'''package com.molina.studio.cinema

import android.content.Context

/**
 * Jembatan Settings Molina -> pemutar mpv untuk "ingat posisi putar". Memakai kunci yang sama dengan
 * Setelan pemutar (save_position di SharedPreferences bawaan); MpvPrefs mendengarkan perubahan itu,
 * sehingga sakelar di sini langsung berlaku tanpa memulai ulang.
 */
internal object ResumeBridge {
    private const val KEY = "save_position"

    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(ctx.applicationContext.packageName + "_preferences", Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Boolean = try {
        sp(ctx).getBoolean(KEY, false)
    } catch (e: ClassCastException) {
        false
    }

    fun setEnabled(ctx: Context, v: Boolean) {
        sp(ctx).edit().putBoolean(KEY, v).apply()
    }
}
''')

write_new("automation/PostDownload.kt", r'''package com.molina.studio.automation

import android.content.Context
import android.os.Environment
import com.molina.core.common.AppLog
import com.molina.core.common.DownloadItemStatus
import com.molina.core.common.DownloadPaths
import com.molina.core.common.DownloadSessionState
import com.molina.core.common.LogLevel
import com.molina.studio.cinema.CinemaRecent
import java.io.File

/**
 * Aksi setelah unduhan selesai: berkas media yang berhasil diunduh ditambahkan ke "Terakhir diputar" Molina Cinema
 * (satu ketukan "Lanjutkan" di beranda Cinema). Hanya bila sakelarnya menyala; tanpa notifikasi tambahan.
 */
object PostDownload {
    private val MEDIA = setOf("mp4", "mkv", "webm", "mov", "m4v", "mp3", "m4a", "opus", "flac", "ogg", "wav", "aac")

    fun onFinished(ctx: Context, session: DownloadSessionState) {
        try {
            AutoPrefs.init(ctx)
            if (!AutoPrefs.state.value.addToCinema) return
            val base = Environment.getExternalStorageDirectory().absolutePath
            var added = 0
            // Dibalik agar item pertama berakhir paling atas di riwayat.
            for (item in session.items.asReversed()) {
                if (item.status != DownloadItemStatus.DONE) continue
                val media = item.files.lastOrNull { it.name.substringAfterLast('.', "").lowercase() in MEDIA } ?: continue
                val host = DownloadPaths.toAndroid(media.path, base)?.let { File(it) } ?: continue
                if (!host.isFile) continue
                CinemaRecent.add(ctx, host.absolutePath, item.title?.takeIf { it.isNotBlank() } ?: host.name)
                added++
            }
            if (added > 0) AppLog.log("system", LogLevel.Info, "auto", "unduhan -> riwayat Cinema: $added berkas")
        } catch (e: Exception) {
            AppLog.log("system", LogLevel.Warn, "auto", "aksi pasca-unduh gagal: ${e.javaClass.simpleName}")
        }
    }
}
''')

# ---------------- ubah berkas yang ada ----------------
ap = SRC + "/automation/AutoPrefs.kt"
sub(ap, "    val autoBackup: Boolean = false,\n    val lastCheck", "    val autoBackup: Boolean = false,\n    val addToCinema: Boolean = false,\n    val lastCheck", "val addToCinema")
sub(ap, '            autoBackup = p.getBoolean("auto_backup", false),\n',
    '            autoBackup = p.getBoolean("auto_backup", false),\n            addToCinema = p.getBoolean("dl_to_cinema", false),\n', 'dl_to_cinema", false')
sub(ap, "    fun markChecked(ctx: Context) {",
    '    fun setAddToCinema(ctx: Context, v: Boolean) =\n        change(ctx, { it.putBoolean("dl_to_cinema", v) }, { it.copy(addToCinema = v) })\n\n    fun markChecked(ctx: Context) {',
    "fun setAddToCinema")

bm = SRC + "/settings/BackupManager.kt"
sub(bm, 'only = setOf("auto_update", "auto_backup")', 'only = setOf("auto_update", "auto_backup", "dl_to_cinema")', '"dl_to_cinema")')

ds = SRC + "/download/DownloadService.kt"
sub(ds, "        val sess = s.session\n", "        val sess = s.session\n        com.molina.studio.automation.PostDownload.onFinished(this, sess)\n", "PostDownload.onFinished")

ui = SRC + "/settings/AutomationUi.kt"
sub(ui, "import androidx.compose.runtime.remember\n",
    "import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\nimport com.molina.studio.cinema.ResumeBridge\n",
    "import com.molina.studio.cinema.ResumeBridge")
sub(ui, '    SettingsItem("Cadangan otomatis terakhir", whenText(st.lastBackup, "belum pernah")) {}\n}',
    '    SettingsItem("Cadangan otomatis terakhir", whenText(st.lastBackup, "belum pernah")) {}\n\n'
    '    Category("Produktivitas")\n'
    '    var resume by remember { mutableStateOf(ResumeBridge.enabled(ctx)) }\n'
    '    SwitchRow(\n'
    '        "Lanjutkan dari posisi terakhir",\n'
    '        "Molina Player mengingat posisi putar dan melanjutkannya. Sama dengan setelan Simpan posisi di pemutar. " +\n'
    '            "Kartu Lanjutkan di beranda Cinema memakainya.",\n'
    '        resume\n'
    '    ) {\n'
    '        resume = it\n'
    '        ResumeBridge.setEnabled(ctx, it)\n'
    '    }\n'
    '    SwitchRow(\n'
    '        "Unduhan masuk ke Cinema",\n'
    '        "Setelah unduhan selesai, video atau audio yang berhasil otomatis muncul di Terakhir diputar Molina Cinema " +\n'
    '            "agar bisa diputar dengan satu ketukan.",\n'
    '        st.addToCinema\n'
    '    ) { AutoPrefs.setAddToCinema(ctx, it) }\n'
    '}', "Category(\"Produktivitas\")")

ch = SRC + "/cinema/CinemaWin7Home.kt"
sub(ch, "                    RecentList(recents, onRecent) { CinemaRecent.clear(ctx) }\n",
    "                    recents.firstOrNull()?.let { first -> ResumeCard(first, ResumeBridge.enabled(ctx)) { onRecent(first) } }\n"
    "                    RecentList(recents, onRecent) { CinemaRecent.clear(ctx) }\n", "ResumeCard(first")
s = rd(ch)
if "private fun ResumeCard" not in s:
    card = r'''

/** Satu ketukan untuk melanjutkan putaran terakhir; teks bawah menyesuaikan setelan "ingat posisi". */
@Composable
private fun ResumeCard(item: CinemaRecent.Item, remembered: Boolean, onPick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color(0x22E6B450), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onPick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("\u25B6", color = MolinaTheme.Gold, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Lanjutkan", color = MolinaTheme.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(item.title, color = Win7.Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (remembered) "Dilanjutkan dari posisi terakhir" else "Diputar dari awal (aktifkan Lanjutkan di Settings > Otomasi)",
                color = Win7.TextDim, fontSize = 11.sp
            )
        }
    }
}
'''
    open(ch, "w", encoding="utf-8").write(s.rstrip("\n") + "\n" + card)
    print("UBAH  ", os.path.basename(ch), "[ResumeCard fungsi]")
else:
    print("SKIP  ", os.path.basename(ch), "[ResumeCard fungsi]")

pg = ROOT + "/progres.md"
entry = '''
## 2026-10-28 - Lanjutkan terakhir + unduhan masuk Cinema [RESUME-POSTDL]
- [x] feat(cinema): kartu "Lanjutkan" di beranda Molina Cinema (item pertama Terakhir diputar, emas), satu ketukan memutar lagi
- [x] feat(settings): Settings > Sistem > Otomasi > Produktivitas: sakelar "Lanjutkan dari posisi terakhir" memakai kunci save_position yang sama dengan Setelan pemutar (`ResumeBridge`), MpvPrefs mendengarkan perubahan sehingga langsung berlaku
- [x] feat(download): `PostDownload.onFinished` dipanggil `DownloadService.finishWith`: video/audio yang berhasil diunduh masuk Terakhir diputar Cinema bila sakelar "Unduhan masuk ke Cinema" menyala (default mati, ikut Cadangan gabungan)
- [x] Dampak: :app saja (cinema/ResumeBridge, automation/PostDownload baru; AutoPrefs, AutomationUi, CinemaWin7Home, DownloadService, BackupManager diubah). Gradle, Manifest, JNI mpv TIDAK disentuh. WAJIB setelah patch_molina_auto.sh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (`background(Color, RoundedCornerShape)`, `var ... by remember` di AutomationPage)
- [ ] uji perangkat (Android 8): putar video, keluar di tengah, nyalakan Lanjutkan, buka Cinema, ketuk kartu > lanjut dari posisi terakhir; nyalakan Unduhan masuk ke Cinema, unduh video, cek Terakhir diputar
- [ ] KETERBATASAN: posisi hanya diingat bila save_position menyala; berkas di dalam rootfs Debian (bukan /sdcard) tidak ditambahkan
'''
if os.path.isfile(pg):
    t = rd(pg)
    if "[RESUME-POSTDL]" in t:
        print("SKIP   progres.md")
    else:
        open(pg, "a", encoding="utf-8").write(("" if t.endswith("\n") else "\n") + entry)
        print("UBAH   progres.md")
else:
    print("GAGAL  progres.md tidak ada"); fails.append(pg)

print("\nSELESAI dengan", len(fails), "kegagalan")
sys.exit(1 if fails else 0)
PYEOF
