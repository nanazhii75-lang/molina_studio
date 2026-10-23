#!/usr/bin/env bash
# Molina Rootfs: resume unduhan (HTTP Range) + tombol Batal.
# Jalankan SETELAH molina_patch_theme_button.sh di-commit (commit terpisah agar kegagalan CI mudah dilacak).
#   cd /sdcard/molina-studio && bash molina_patch_rootfs_resume.sh
# Idempotent. Perubahan dihitung di memori dulu; bila anchor tak cocok, tidak ada file yang ditulis.
python3 << 'PYEOF'
import os, sys

CANDIDATES = [os.environ.get("MOLINA_ROOT", ""), os.getcwd(),
              "/sdcard/molina-studio", "/storage/emulated/0/molina-studio"]
TERM = "app/src/main/java/com/molina/studio/terminal/"
root = next((c for c in CANDIDATES if c and os.path.isfile(os.path.join(c, TERM + "Rootfs.kt"))), None)
if root is None:
    sys.exit("GAGAL: repo molina-studio tidak ditemukan. cd ke root repo atau set MOLINA_ROOT.")
print("Root repo:", root)

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

def sub_region(rel, start, end, must_contain, new, done_marker):
    """Ganti teks dari awal [start] sampai sebelum [end]."""
    s = load(rel)
    if done_marker in s:
        return
    a, b = s.find(start), s.find(end)
    if a == -1 or b == -1 or b < a or s.count(start) != 1:
        sys.exit("GAGAL: batas blok tidak ditemukan di %s (%s)" % (rel, start.strip()))
    if must_contain not in s[a:b]:
        sys.exit("GAGAL: blok di %s berbeda dari yang diharapkan; patch dihentikan." % rel)
    files[rel] = s[:a] + new + s[b:]

RF = TERM + "Rootfs.kt"

# --- Rootfs.kt ---
sub(RF, "private class CountingStream(",
        "private class InstallCancelled : RuntimeException()\n\nprivate class CountingStream(")

sub(RF, "    @Volatile private var running = false\n",
        "    @Volatile private var running = false\n"
        "    @Volatile private var cancelRequested = false\n"
        "    @Volatile private var activeConn: HttpURLConnection? = null\n")

INSTALL_NEW = '''    fun install(ctx: Context) {
        val app = ctx.applicationContext
        synchronized(this) {
            if (running) return
            running = true
            cancelRequested = false
        }
        scope.launch {
            try {
                doInstall(app)
                _state.value = RootfsState.Ready
            } catch (e: Exception) {
                if (cancelRequested) {
                    // Dibatalkan pengguna: buang ekstrak setengah jadi, sisa unduhan dipertahankan.
                    File(app.filesDir, "rootfs/debian.tmp").deleteRecursively()
                    _state.value = RootfsState.Missing
                } else {
                    _state.value = RootfsState.Failed(e.message ?: e.javaClass.simpleName)
                }
            } finally {
                running = false
            }
        }
    }

    /** Batalkan pemasangan yang berjalan (unduh, verifikasi, atau ekstrak). Sisa unduhan tetap disimpan. */
    fun cancel() {
        if (!running) return
        cancelRequested = true
        scope.launch { activeConn?.disconnect() } // di luar main thread; membangunkan read() yang menggantung
    }

    /** Byte unduhan yang tersimpan (arsip utuh atau sisa .part) untuk dilanjutkan; 0 bila tidak ada. */
    fun partialBytes(ctx: Context): Long {
        val archive = File(ctx.cacheDir, "debian-rootfs.tar.gz")
        val part = File(ctx.cacheDir, "debian-rootfs.tar.gz.part")
        return when {
            archive.isFile -> archive.length()
            part.isFile -> part.length()
            else -> 0L
        }
    }

'''
sub_region(RF, "    fun install(ctx: Context) {", "    private fun doInstall(ctx: Context) {",
           "scope.launch {", INSTALL_NEW, "fun partialBytes(ctx: Context)")

DOWNLOAD_NEW = '''    private fun download(dest: File) {
        var offset = if (dest.isFile) dest.length() else 0L
        if (offset > ROOTFS_SIZE) { dest.delete(); offset = 0L }
        if (offset == ROOTFS_SIZE) return // sisa unduhan sudah utuh; langsung diverifikasi
        val conn = (URL(ROOTFS_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            // Tanpa kompresi transport: offset Range harus sama dengan byte berkas.
            setRequestProperty("Accept-Encoding", "identity")
            if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
        }
        activeConn = conn
        try {
            // 206 = lanjut dari offset; 200 = server mengabaikan Range, ulang dari awal.
            // Kebenaran akhir dijamin SHA-256: bila hasil gabungan salah, .part dibuang saat verifikasi.
            val append = when (val code = conn.responseCode) {
                206 -> true
                200 -> false
                else -> error("Unduhan gagal: HTTP $code")
            }
            val total = if (append) ROOTFS_SIZE else (conn.contentLengthLong.takeIf { it > 0 } ?: ROOTFS_SIZE)
            conn.inputStream.use { input ->
                java.io.FileOutputStream(dest, append).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = if (append) offset else 0L
                    var last = -1
                    while (true) {
                        if (cancelRequested) throw InstallCancelled()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        val p = ((done * 100) / total).toInt().coerceIn(0, 100)
                        if (p != last) { last = p; _state.value = RootfsState.Downloading(p) }
                    }
                }
            }
        } finally {
            activeConn = null
            conn.disconnect()
        }
    }

'''
sub_region(RF, "    private fun download(dest: File) {", "    private fun sha256(f: File): String {",
           "conn.responseCode != 200", DOWNLOAD_NEW, 'bytes=$offset-')

sub(RF, "                val n = i.read(b)\n",
        "                if (cancelRequested) throw InstallCancelled()\n                val n = i.read(b)\n")
sub(RF, "                val e = (tar.nextEntry as? TarArchiveEntry) ?: break\n",
        "                val e = (tar.nextEntry as? TarArchiveEntry) ?: break\n"
        "                if (cancelRequested) throw InstallCancelled()\n")

# --- TerminalScreen.kt: pemanggilan dengan argumen bernama ---
sub(TERM + "TerminalScreen.kt",
    "SetupPanel(rootfsState) { RootfsManager.install(context) }",
    "SetupPanel(\n"
    "                    rootfsState,\n"
    "                    onInstall = { RootfsManager.install(context) },\n"
    "                    onCancel = { RootfsManager.cancel() }\n"
    "                )")

# --- SetupPanel.kt ---
sp = TERM + "SetupPanel.kt"
sub(sp, "fun SetupPanel(state: RootfsState, onInstall: () -> Unit) {",
        "fun SetupPanel(state: RootfsState, onInstall: () -> Unit, onCancel: () -> Unit) {")
sub(sp, "ProgressSection(state)\n", "ProgressSection(state, onCancel)\n")
sub(sp, "private fun ProgressSection(state: RootfsState) {\n",
        "private fun ProgressSection(state: RootfsState, onCancel: () -> Unit) {\n"
        "    var cancelling by remember { mutableStateOf(false) }\n")
sub(sp,
    '    Text("Biarkan aplikasi tetap terbuka sampai selesai.", color = SetupMuted,\n'
    '        fontSize = 10.sp, textAlign = TextAlign.Center)\n}',
    '    Text("Biarkan aplikasi tetap terbuka sampai selesai.", color = SetupMuted,\n'
    '        fontSize = 10.sp, textAlign = TextAlign.Center)\n'
    '    Spacer(Modifier.height(2.dp))\n'
    '    TextButton(onClick = { cancelling = true; onCancel() }, enabled = !cancelling) {\n'
    '        Text(if (cancelling) "Membatalkan..." else "Batal", color = SetupMuted, fontSize = 12.sp)\n'
    '    }\n}')
sub(sp, "    val free = remember { freeBytes(ctx) }\n",
        "    val free = remember { freeBytes(ctx) }\n"
        "    val partial = remember { RootfsManager.partialBytes(ctx) }\n")
sub(sp, '        InfoRow("Unduhan", "~30 MB")\n        SetupDivider()\n',
        '        InfoRow("Unduhan", "~30 MB")\n        SetupDivider()\n'
        '        if (partial > 0) {\n'
        '            InfoRow("Tersimpan", formatBytes(partial), SetupGood)\n'
        '            SetupDivider()\n'
        '        }\n')
sub(sp, 'InstallButton("Pasang Debian", canInstall, onInstall)',
        'InstallButton(if (partial > 0) "Lanjutkan" else "Pasang Debian", canInstall, onInstall)')

# --- progres.md ---
ENTRY = ("- 2026-10-23: Rootfs: unduhan bisa dilanjutkan (HTTP Range ke .part di cacheDir; 206 = lanjut, "
         "200 = ulang dari awal; Accept-Encoding identity; arsip utuh dipakai ulang), "
         "`RootfsManager.cancel()` + tombol Batal (batalkan unduh/verifikasi/ekstrak, bersihkan debian.tmp, "
         "state kembali Missing, sisa unduhan dipertahankan); bila ada sisa, tombol jadi Lanjutkan + baris "
         "Tersimpan. Range diuji manual ke URL rootfs (206, Content-Range total 30200478). "
         "Menunggu CI + uji perangkat.")
pp = "progres.md"
if os.path.isfile(os.path.join(root, pp)):
    t = load(pp)
    if "RootfsManager.cancel()" not in t:
        mark = "\n\n## Roadmap Prioritas"
        files[pp] = t.replace(mark, "\n" + ENTRY + mark, 1) if mark in t else t.rstrip("\n") + "\n" + ENTRY + "\n"
else:
    print("PERINGATAN: progres.md tidak ditemukan")

for rel, text in files.items():
    if text != orig[rel]:
        with open(os.path.join(root, rel), "w", encoding="utf-8") as f:
            f.write(text)
        print("dipatch   :", rel)
    else:
        print("sudah ada :", rel)

print("\nSelesai. Berikutnya:")
print("  git add -A && git commit -m \"feat(term): resume unduhan rootfs dan tombol Batal\"")
PYEOF
