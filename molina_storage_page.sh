#!/bin/sh
# Molina: Settings: halaman Penyimpanan (idempotent; aman dijalankan ulang).
# Jalankan dari dalam folder repo: bash molina_storage_page.sh
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
    os.makedirs(os.path.dirname(p), exist_ok=True)
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

def newfile(path, content, label):
    if os.path.exists(path):
        print("  = " + label + " (sudah ada)")
        return
    wr(path, content)
    changed.append(label)
    print("  + " + label)

def progres(tag, text):
    pg = os.path.join(ROOT, "progres.md")
    if os.path.isfile(pg) and tag not in rd(pg):
        with open(pg, "a", encoding="utf-8", newline="\n") as f:
            f.write(text)
        changed.append("progres.md"); print("  + progres.md")
    else:
        print("  = progres.md (sudah ada / tidak ditemukan)")

SS = os.path.join(J, "settings/SettingsScreen.kt")

newfile(os.path.join(J, "settings/StorageUi.kt"), r'''package com.molina.studio.settings

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.molina.core.common.TrashBin
import com.molina.studio.terminal.RootfsManager
import com.molina.studio.terminal.TerminalSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.Locale

private class StorageInfo(
    val rootfs: Long, val aptCache: Long, val appCache: Long, val workspace: Long, val trash: Long,
    val free: Long, val total: Long
)

private fun fmtBytes(b: Long): String = when {
    b >= (1L shl 30) -> String.format(Locale.US, "%.2f GB", b / 1073741824.0)
    b >= (1L shl 20) -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
    b >= (1L shl 10) -> String.format(Locale.US, "%d KB", b shr 10)
    else -> b.toString() + " B"
}

/** Ukuran total folder; symlink tidak diikuti. Berhenti lebih awal bila [cancelled]. */
private fun dirSize(root: File, cancelled: () -> Boolean): Long {
    var total = 0L
    val stack = ArrayDeque<File>()
    stack.addLast(root)
    while (stack.isNotEmpty() && !cancelled()) {
        val f = stack.removeLast()
        try {
            if (Files.isSymbolicLink(f.toPath())) continue
            if (f.isDirectory) f.listFiles()?.forEach { stack.addLast(it) } else total += f.length()
        } catch (e: Exception) {
        }
    }
    return total
}

/** Hapus berkas/folder tanpa mengikuti symlink. */
private fun wipe(f: File) {
    try {
        if (!Files.isSymbolicLink(f.toPath()) && f.isDirectory) f.listFiles()?.forEach { wipe(it) }
        f.delete()
    } catch (e: Exception) {
    }
}

private fun aptArchives(ctx: android.content.Context) = File(RootfsManager.rootfsDir(ctx), "var/cache/apt/archives")

/** Akar yang punya Recycle Bin (.molina-trash): penyimpanan internal, kartu SD, dan beranda Debian. */
private fun trashVolumes(ctx: android.content.Context): List<File> {
    val out = mutableListOf(Environment.getExternalStorageDirectory())
    File("/storage").listFiles()?.forEach {
        if (it.isDirectory && it.canRead() && it.name != "emulated" && it.name != "self") out.add(it)
    }
    val home = File(RootfsManager.rootfsDir(ctx), "root")
    if (home.isDirectory) out.add(home)
    return out
}

private fun measure(ctx: android.content.Context, cancelled: () -> Boolean): StorageInfo {
    val ws = File(Environment.getExternalStorageDirectory(), TerminalSessions.WORKSPACE_DIR)
    val appCache = (ctx.cacheDir.listFiles() ?: emptyArray()).filter { it.name != "proot-tmp" }
        .sumOf { dirSize(it, cancelled) }
    val st = StatFs(ctx.filesDir.absolutePath)
    return StorageInfo(
        dirSize(RootfsManager.rootfsDir(ctx), cancelled), dirSize(aptArchives(ctx), cancelled), appCache,
        if (ws.isDirectory) dirSize(ws, cancelled) else 0L,
        runCatching { TrashBin.usage(trashVolumes(ctx)) }.getOrDefault(0L),
        st.availableBytes, st.totalBytes
    )
}

/** Hapus .deb unduhan apt (dan unduhan setengah jadi); berkas lock dibiarkan. Mengembalikan byte yang dibebaskan. */
private fun cleanApt(ctx: android.content.Context): Long {
    val dir = aptArchives(ctx)
    val before = dirSize(dir) { false }
    dir.listFiles()?.forEach { f ->
        if (f.name == "lock") return@forEach
        if (f.name == "partial") f.listFiles()?.forEach { wipe(it) }
        else if (f.isFile && f.name.endsWith(".deb")) wipe(f)
    }
    return maxOf(0L, before - dirSize(dir) { false })
}

/** Hapus cache aplikasi kecuali proot-tmp (dipakai sesi Terminal yang sedang berjalan). */
private fun cleanAppCache(ctx: android.content.Context): Long {
    val kids = (ctx.cacheDir.listFiles() ?: emptyArray()).filter { it.name != "proot-tmp" }
    val before = kids.sumOf { dirSize(it) { false } }
    kids.forEach { wipe(it) }
    return before
}

@Composable
private fun StorageRow(title: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Text(
            value, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Halaman Penyimpanan: ukuran Debian, cache, Recycle Bin, folder kerja, dan pembersihan aman. */
@Composable
internal fun StoragePage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableStateOf(0) }
    var info by remember { mutableStateOf<StorageInfo?>(null) }
    var msg by remember { mutableStateOf("") }
    var confirmBin by remember { mutableStateOf(false) }
    LaunchedEffect(tick) {
        info = null
        info = withContext(Dispatchers.IO) { measure(ctx) { !isActive } }
    }
    val i = info
    Category("Ruang penyimpanan")
    if (i == null) {
        StorageRow("Menghitung ukuran...", "Rootfs Debian besar, mohon tunggu beberapa detik.")
    } else {
        StorageRow("Penyimpanan internal tersisa", fmtBytes(i.free) + " dari " + fmtBytes(i.total))
        StorageRow("Debian (rootfs)", fmtBytes(i.rootfs))
        StorageRow("Cache apt (.deb unduhan)", fmtBytes(i.aptCache))
        StorageRow("Cache aplikasi", fmtBytes(i.appCache))
        StorageRow("Folder kerja (" + TerminalSessions.WORKSPACE_DIR + ")", fmtBytes(i.workspace))
        StorageRow("Recycle Bin", fmtBytes(i.trash))
    }
    Category("Bersihkan")
    SettingsItem(
        "Bersihkan cache apt",
        "Hapus .deb unduhan di Debian. Paket terpasang tidak terpengaruh. Jangan dijalankan saat apt sedang berjalan."
    ) {
        scope.launch {
            val freed = withContext(Dispatchers.IO) { cleanApt(ctx) }
            msg = "Cache apt dibersihkan: " + fmtBytes(freed) + " dibebaskan."
            tick++
        }
    }
    SettingsItem(
        "Bersihkan cache aplikasi",
        "Hapus berkas sementara aplikasi. Sesi Terminal yang berjalan tidak terganggu."
    ) {
        scope.launch {
            val freed = withContext(Dispatchers.IO) { cleanAppCache(ctx) }
            msg = "Cache aplikasi dibersihkan: " + fmtBytes(freed) + " dibebaskan."
            tick++
        }
    }
    SettingsItem(
        "Kosongkan Recycle Bin",
        "Hapus permanen semua item di Recycle Bin (internal, kartu SD, dan Debian). Tidak bisa dibatalkan."
    ) { confirmBin = true }
    SettingsItem("Hitung ulang", "Segarkan ukuran di atas") { tick++ }
    if (confirmBin) {
        AlertDialog(
            onDismissRequest = { confirmBin = false },
            title = { Text("Kosongkan Recycle Bin?") },
            text = { Text("Semua item di Recycle Bin dihapus permanen dan tidak bisa dipulihkan.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBin = false
                    scope.launch {
                        val n = withContext(Dispatchers.IO) { runCatching { TrashBin.empty(trashVolumes(ctx)) }.getOrDefault(0) }
                        msg = "Recycle Bin dikosongkan: " + n + " entri."
                        tick++
                    }
                }) { Text("Kosongkan") }
            },
            dismissButton = { TextButton(onClick = { confirmBin = false }) { Text("Batal") } }
        )
    }
    if (msg.isNotEmpty()) {
        Text(
            msg, modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall
        )
    }
}
''', "StorageUi.kt (halaman Penyimpanan)")

sub(SS, "Packages, Code, Files, Downloads\n}", "Packages, Code, Files, Downloads, Storage\n}", "Downloads, Storage", "enum Page.Storage")
sub(SS, '    Page.Downloads -> "Unduhan"\n}', '    Page.Downloads -> "Unduhan"\n    Page.Storage -> "Penyimpanan"\n}', 'Page.Storage -> "Penyimpanan"', "titleOf(Storage)")
sub(SS, '    Hit("About", "Versi dan lisensi", Page.About)\n)',
    '    Hit("Penyimpanan", "Ukuran Debian, cache, Recycle Bin, folder kerja, dan bersihkan ruang", Page.Storage),\n'
    '    Hit("Bersihkan cache", "Hapus cache apt dan cache aplikasi", Page.Storage),\n'
    '    Hit("About", "Versi dan lisensi", Page.About)\n)',
    'Hit("Penyimpanan"', "indeks pencarian")
sub(SS, '                        W7Link({ W7IconInfo() }, "About", "Versi dan lisensi") { open(Page.About) }',
    '                        W7Link({ W7IconToolList() }, "Penyimpanan", "Ukuran Debian, cache, Recycle Bin, dan bersihkan ruang") { open(Page.Storage) }\n'
    '                        W7Link({ W7IconInfo() }, "About", "Versi dan lisensi") { open(Page.About) }',
    'open(Page.Storage) }', "tautan di halaman Sistem")
sub(SS, "                    Page.Packages -> PackagesPage(onOpenYtdlp = { open(Page.Stream) })",
    "                    Page.Packages -> PackagesPage(onOpenYtdlp = { open(Page.Stream) })\n                    Page.Storage -> StoragePage()",
    "Page.Storage -> StoragePage()", "isi halaman Storage")

progres("SETTINGS-STORAGE", '''
## 2026-10-27 - Settings: halaman Penyimpanan [SETTINGS-STORAGE]
- [x] feat(settings): Sistem > Penyimpanan: sisa ruang internal, ukuran rootfs Debian, cache apt, cache aplikasi, folder kerja molina-studio, Recycle Bin (internal, kartu SD, Debian); Hitung ulang
- [x] feat(settings): Bersihkan cache apt (hapus .deb unduhan dan partial, berkas lock dibiarkan), Bersihkan cache aplikasi (proot-tmp dilewati karena dipakai sesi Terminal), dan Kosongkan Recycle Bin dengan konfirmasi (TrashBin.usage/empty). Symlink tidak diikuti saat menghitung dan menghapus
- [x] Dampak: settings/StorageUi.kt (baru), settings/SettingsScreen.kt (enum Page, titleOf, indeks pencarian, tautan Sistem). Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (ArrayDeque.removeLast, sumOf pada Long, TrashBin dari :core:common)
- [ ] uji perangkat: Settings > Sistem > Penyimpanan: angka muncul tanpa macet UI; apt install paket lalu Bersihkan cache apt (angka turun, paket tetap jalan); Bersihkan cache aplikasi; hapus berkas di Berkas lalu Kosongkan Recycle Bin (angka Recycle Bin menjadi 0); sesi Terminal tetap normal; pencarian "penyimpanan" menemukan halaman
- [ ] CATATAN: tahap B peta jalan (Penyimpanan dan Folder Molina) tumpang tindih dengan halaman ini; sisa B = lokasi default unduhan/konversi/trash
- [ ] LANGKAH BERIKUT: cadangkan/pulihkan setelan, profil Terminal
''')

print("\nSelesai. Perubahan baru: " + str(len(changed)))
PYEOF

RD="$(python3 -c 'import os,sys;print(os.path.abspath(sys.argv[1]))' "${MOLINA_REPO:-.}")"
[ -f "$RD/app/src/main/AndroidManifest.xml" ] || RD="$(ls -d /storage/emulated/0/molina-studio /sdcard/molina-studio 2>/dev/null | head -1)"
cd "$RD" && git add app progres.md && { git diff --cached --quiet && echo "Tidak ada perubahan baru untuk di-commit." || { git commit -m "feat(settings): halaman Penyimpanan (ukuran Debian, cache, Recycle Bin)" && git pull --rebase -q && git push; }; }
