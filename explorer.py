import os, sys

CANDIDATES = [os.getcwd(), "/storage/emulated/0/molina-studio", "/sdcard/molina-studio"]
REL = "app/src/main/java/com/molina/studio/cinema/"
SET = "app/src/main/java/com/molina/studio/settings/SettingsScreen.kt"
ROOT = next((p for p in CANDIDATES if os.path.isfile(os.path.join(p, REL + "Win7OpenDialog.kt"))), None)
if ROOT is None:
    sys.exit("GAGAL: repo molina_studio tidak ditemukan. Jalankan dari folder repo atau pastikan /storage/emulated/0/molina-studio ada.")
print("Repo:", ROOT)


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def save(p, text):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def write_if_changed(p, text):
    if os.path.isfile(p) and read(p) == text:
        print("= sudah sama:", os.path.relpath(p, ROOT))
        return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    save(p, text)
    print("+ ditulis   :", os.path.relpath(p, ROOT))


def replace_once(text, old, new, what):
    n = text.count(old)
    if n != 1:
        sys.exit("GAGAL: jangkar '%s' ditemukan %d kali (harus 1). Tidak ada berkas diubah.\n%s" % (what, n, old[:140]))
    return text.replace(old, new, 1)


def slice_replace(text, start, end, new, what, include_end=False):
    if text.count(start) != 1:
        sys.exit("GAGAL: awal '%s' ditemukan %d kali (harus 1)." % (what, text.count(start)))
    s = text.index(start)
    e = text.index(end, s) if end is not None else len(text)
    if end is not None and text.count(end) < 1:
        sys.exit("GAGAL: akhir '%s' tidak ditemukan." % what)
    return text[:s] + new + text[e:]


# ================================================================ FileOps.kt (v2)
FILE_OPS = r'''package com.molina.studio.cinema

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Operasi berkas murni untuk Penjelajah Molina (tanpa Android/Compose). Semua fungsi bersifat blocking:
 * panggil dari Dispatchers.IO. Kegagalan dilaporkan lewat IOException berpesan bahasa Indonesia.
 * Operasi panjang menerima [Ctl] untuk progres dan pembatalan.
 */
internal object FileOps {
    class Props(
        val file: File,
        val isDir: Boolean,
        val size: Long,
        val files: Int,
        val folders: Int,
        val modified: Long,
        val canRead: Boolean,
        val canWrite: Boolean,
        val symlink: Boolean
    )

    /** Kendali operasi panjang: [total] byte (diisi operasi), progres 0..1 dilaporkan per persen, [check] melempar bila dibatalkan. */
    class Ctl(private val cancel: AtomicBoolean, private val report: (Float) -> Unit) {
        @Volatile var total = 0L
        private var done = 0L
        private var last = -1
        fun check() { if (cancel.get()) throw IOException("Dibatalkan") }
        fun add(n: Long) {
            done += n
            val t = total
            if (t > 0) {
                val p = (done * 100 / t).coerceIn(0L, 100L).toInt()
                if (p != last) { last = p; report(p / 100f) }
            }
        }
    }

    private fun isLink(f: File): Boolean = try { Files.isSymbolicLink(f.toPath()) } catch (e: Exception) { false }
    private fun present(f: File): Boolean = f.exists() || isLink(f)
    private fun asIo(e: Exception, fallback: String): IOException = if (e is IOException) e else IOException(e.message ?: fallback)

    /** True bila [child] sama dengan [parent] atau berada di dalamnya (jalur kanonik). */
    fun isInside(parent: File, child: File): Boolean {
        val p = parent.canonicalFile.path
        val c = child.canonicalFile.path
        return c == p || c.startsWith(p + File.separator)
    }

    /** Nama bebas bentrok di [dir]: "nama (1).ext", "nama (2).ext", dst. */
    fun uniqueTarget(dir: File, name: String, keepExt: Boolean = true): File {
        var f = File(dir, name)
        if (!present(f)) return f
        val dot = name.lastIndexOf('.')
        val base = if (keepExt && dot > 0) name.substring(0, dot) else name
        val ext = if (keepExt && dot > 0) name.substring(dot) else ""
        var i = 1
        while (true) {
            f = File(dir, "$base ($i)$ext")
            if (!present(f)) return f
            i++
        }
    }

    private fun validName(raw: String): String {
        val n = raw.trim()
        if (n.isEmpty()) throw IOException("Nama tidak boleh kosong")
        if (n == "." || n == ".." || n.contains('/') || n.contains('\u0000')) throw IOException("Nama tidak valid")
        if (n.length > 255) throw IOException("Nama terlalu panjang (maks. 255 karakter)")
        return n
    }

    private fun pump(i: InputStream, o: OutputStream, ctl: Ctl) {
        val buf = ByteArray(128 * 1024)
        while (true) {
            ctl.check()
            val n = i.read(buf)
            if (n < 0) break
            o.write(buf, 0, n)
            ctl.add(n.toLong())
        }
    }

    private fun checkDest(src: File, destDir: File) {
        if (!present(src)) throw IOException("Sumber tidak ditemukan: " + src.name)
        if (!destDir.isDirectory) throw IOException("Folder tujuan tidak ditemukan")
        if (!destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        if (src.isDirectory && !isLink(src) && isInside(src, destDir)) {
            throw IOException("Folder tidak bisa dimasukkan ke dalam dirinya sendiri")
        }
    }

    private fun copyRec(src: File, dst: File, ctl: Ctl) {
        ctl.check()
        if (isLink(src)) {
            try {
                Files.createSymbolicLink(dst.toPath(), Files.readSymbolicLink(src.toPath()))
            } catch (e: Exception) {
                throw IOException("Tautan simbolik tidak dapat disalin: " + src.name)
            }
            return
        }
        if (src.isDirectory) {
            if (!dst.mkdirs() && !dst.isDirectory) throw IOException("Gagal membuat folder " + dst.name)
            val kids = src.listFiles() ?: throw IOException("Folder tidak dapat dibaca: " + src.name)
            for (k in kids) copyRec(k, File(dst, k.name), ctl)
        } else {
            src.inputStream().use { i -> dst.outputStream().use { o -> pump(i, o, ctl) } }
        }
        dst.setLastModified(src.lastModified())
    }

    private fun deleteRec(f: File, ctl: Ctl? = null): Boolean {
        ctl?.check()
        if (!isLink(f) && f.isDirectory) {
            val kids = f.listFiles()
            if (kids != null) for (k in kids) if (!deleteRec(k, ctl)) return false
        }
        return f.delete()
    }

    /** Total byte semua isi [files] (tautan simbolik tidak diikuti). */
    fun sizeOf(files: List<File>): Long {
        var s = 0L
        fun walk(f: File) {
            if (isLink(f)) return
            if (f.isDirectory) f.listFiles()?.forEach { walk(it) } else s += f.length()
        }
        files.forEach { walk(it) }
        return s
    }

    /** Salin semua [files] ke [destDir]. Nama bentrok diberi akhiran " (1)". Mengembalikan hasil salinan. */
    fun copyAll(files: List<File>, destDir: File, ctl: Ctl): List<File> {
        ctl.total = sizeOf(files)
        val out = ArrayList<File>()
        for (f in files) {
            ctl.check()
            checkDest(f, destDir)
            val t = uniqueTarget(destDir, f.name, keepExt = !f.isDirectory)
            try {
                copyRec(f, t, ctl)
            } catch (e: Exception) {
                deleteRec(t)
                throw asIo(e, "Gagal menyalin")
            }
            out.add(t)
        }
        return out
    }

    /** Pindahkan semua [files] ke [destDir]: rename cepat; bila beda volume salin lalu hapus sumber. */
    fun moveAll(files: List<File>, destDir: File, ctl: Ctl): List<File> {
        ctl.total = sizeOf(files)
        val out = ArrayList<File>()
        for (f in files) {
            ctl.check()
            checkDest(f, destDir)
            if (f.parentFile?.canonicalFile == destDir.canonicalFile) {
                throw IOException("\"" + f.name + "\" sudah berada di folder ini")
            }
            val t = uniqueTarget(destDir, f.name, keepExt = !f.isDirectory)
            val sz = sizeOf(listOf(f))
            if (f.renameTo(t)) { ctl.add(sz); out.add(t); continue }
            try {
                copyRec(f, t, ctl)
            } catch (e: Exception) {
                deleteRec(t)
                throw asIo(e, "Gagal memindahkan")
            }
            if (!deleteRec(f)) throw IOException("Sudah disalin ke tujuan, tetapi \"" + f.name + "\" tidak bisa dihapus")
            out.add(t)
        }
        return out
    }

    /** Hapus semua [files]. Jalur di [guard] (jalur absolut) dilindungi dan ditolak. Mengembalikan jumlah item terhapus. */
    fun deleteAll(files: List<File>, guard: Set<String>, ctl: Ctl): Int {
        for (f in files) if (f.absolutePath in guard) throw IOException("Folder ini dilindungi dan tidak bisa dihapus: " + f.name)
        var n = 0
        for (f in files) {
            ctl.check()
            if (!present(f)) continue
            if (!deleteRec(f, ctl)) throw IOException("Gagal menghapus: " + f.name)
            n++
        }
        return n
    }

    /** Buat folder [name] di [parent]. */
    fun mkdir(parent: File, name: String): File {
        val n = validName(name)
        if (!parent.isDirectory || !parent.canWrite()) throw IOException("Folder ini tidak bisa ditulisi")
        val t = File(parent, n)
        if (present(t)) throw IOException("Sudah ada \"" + n + "\" di folder ini")
        if (!t.mkdir()) throw IOException("Gagal membuat folder (periksa izin tulis)")
        return t
    }

    /** Ubah nama [src] (hanya nama, tanpa jalur). Mengembalikan File baru. */
    fun rename(src: File, newName: String): File {
        val n = validName(newName)
        if (!present(src)) throw IOException("Sumber tidak ditemukan: " + src.name)
        if (n == src.name) return src
        val parent = src.parentFile ?: throw IOException("Folder induk tidak ditemukan")
        val target = File(parent, n)
        // Penyimpanan bersama tidak peka huruf besar/kecil: ganti kapitalisasi nama sendiri tetap diizinkan.
        if (!n.equals(src.name, ignoreCase = true) && present(target)) {
            throw IOException("Sudah ada \"" + n + "\" di folder ini")
        }
        if (!src.renameTo(target)) throw IOException("Gagal mengubah nama (periksa izin tulis)")
        return target
    }

    /** Kompres [files] menjadi satu ZIP di [destDir]. Satu item: nama item; banyak: nama folder tujuan. Tautan dilewati. */
    fun zipAll(files: List<File>, destDir: File, ctl: Ctl): File {
        if (files.isEmpty()) throw IOException("Tidak ada yang dikompres")
        for (f in files) if (!present(f)) throw IOException("Sumber tidak ditemukan: " + f.name)
        if (!destDir.isDirectory || !destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        val one = files.singleOrNull()
        val stem = if (one != null) {
            if (one.isDirectory) one.name else one.nameWithoutExtension.ifEmpty { one.name }
        } else destDir.name.ifEmpty { "Arsip" }
        val out = uniqueTarget(destDir, "$stem.zip", keepExt = true)
        ctl.total = sizeOf(files)
        try {
            ZipOutputStream(out.outputStream().buffered()).use { z ->
                val skip = out.canonicalFile
                for (f in files) addZip(z, f, f.name, skip, ctl)
            }
        } catch (e: Exception) {
            out.delete()
            throw asIo(e, "Gagal mengompres")
        }
        return out
    }

    private fun addZip(z: ZipOutputStream, f: File, path: String, skip: File, ctl: Ctl) {
        ctl.check()
        if (isLink(f) || f.canonicalFile == skip) return
        if (f.isDirectory) {
            z.putNextEntry(ZipEntry("$path/").apply { time = f.lastModified() })
            z.closeEntry()
            val kids = f.listFiles() ?: return
            for (k in kids.sortedBy { it.name }) addZip(z, k, path + "/" + k.name, skip, ctl)
        } else {
            z.putNextEntry(ZipEntry(path).apply { time = f.lastModified() })
            f.inputStream().use { pump(it, z, ctl) }
            z.closeEntry()
        }
    }

    /** Ekstrak [zip] ke folder baru bernama sama di [destDir]. Entri yang keluar dari folder tujuan (zip-slip) ditolak. */
    fun unzip(zip: File, destDir: File, ctl: Ctl): File {
        if (!zip.isFile) throw IOException("Berkas ZIP tidak ditemukan")
        if (!destDir.isDirectory || !destDir.canWrite()) throw IOException("Folder tujuan tidak bisa ditulisi")
        val target = uniqueTarget(destDir, zip.nameWithoutExtension.ifEmpty { "Ekstrak" }, keepExt = false)
        val base = target.canonicalFile
        try {
            ZipFile(zip).use { zf ->
                val entries = zf.entries().toList()
                var total = 0L
                for (en in entries) if (!en.isDirectory && en.size > 0) total += en.size
                ctl.total = total
                if (!target.mkdirs()) throw IOException("Gagal membuat folder " + target.name)
                for (en in entries) {
                    ctl.check()
                    val out = File(target, en.name).canonicalFile
                    if (out.path != base.path && !out.path.startsWith(base.path + File.separator)) {
                        throw IOException("Entri ZIP tidak aman: " + en.name)
                    }
                    if (en.isDirectory) { out.mkdirs(); continue }
                    out.parentFile?.mkdirs()
                    zf.getInputStream(en).use { i -> out.outputStream().use { o -> pump(i, o, ctl) } }
                    if (en.time > 0) out.setLastModified(en.time)
                }
            }
        } catch (e: Exception) {
            deleteRec(target)
            throw asIo(e, "Gagal mengekstrak")
        }
        return target
    }

    /** Properti [f]; untuk folder ukuran dan isi dihitung rekursif (tautan dihitung sebagai berkas, tidak diikuti). */
    fun props(f: File): Props {
        var size = 0L
        var files = 0
        var folders = 0
        fun walk(d: File) {
            val kids = d.listFiles() ?: return
            for (k in kids) {
                if (isLink(k)) { files++; continue }
                if (k.isDirectory) { folders++; walk(k) } else { files++; size += k.length() }
            }
        }
        val link = isLink(f)
        val dir = f.isDirectory && !link
        if (dir) walk(f) else size = f.length()
        return Props(f, dir, size, files, folders, f.lastModified(), f.canRead(), f.canWrite(), link)
    }

    fun readable(b: Long): String {
        if (b < 1024) return "$b B"
        val u = arrayOf("KB", "MB", "GB", "TB")
        var v = b.toDouble()
        var i = -1
        do { v /= 1024.0; i++ } while (v >= 1024.0 && i < u.lastIndex)
        return String.format(Locale("id", "ID"), "%.1f %s", v, u[i])
    }
}
'''

write_if_changed(os.path.join(ROOT, REL + "FileOps.kt"), FILE_OPS)

# ================================================================ Win7OpenDialog.kt
DLG = os.path.join(ROOT, REL + "Win7OpenDialog.kt")
src = read(DLG)

if "[FILEOPS-2]" in src:
    print("= Win7OpenDialog.kt sudah dipatch tahap 2 (lewati)")
elif "[FILEOPS-1]" not in src:
    sys.exit("GAGAL: patch tahap 1 (alat berkas) belum diterapkan. Jalankan skrip tahap 1 dulu, lalu ulangi skrip ini.")
else:
    out = src

    # ---- imports
    need = [
        "import androidx.compose.foundation.Canvas",
        "import androidx.compose.ui.geometry.Offset",
        "import androidx.compose.ui.geometry.Size",
        "import androidx.compose.ui.graphics.Path",
        "import androidx.compose.ui.graphics.StrokeCap",
        "import androidx.compose.ui.graphics.drawscope.Fill",
        "import androidx.compose.ui.graphics.drawscope.Stroke",
        "import androidx.compose.ui.platform.LocalFocusManager",
        "import androidx.compose.ui.platform.LocalView",
        "import androidx.compose.ui.window.DialogWindowProvider",
        "import android.view.WindowManager",
        "import android.view.inputmethod.InputMethodManager",
        "import java.util.concurrent.atomic.AtomicBoolean",
        "import kotlin.math.PI",
        "import kotlin.math.cos",
        "import kotlin.math.sin",
    ]
    add = "".join(l + "\n" for l in need if (l + "\n") not in out)
    out = replace_once(out, "import androidx.compose.ui.window.DialogProperties\n",
                       "import androidx.compose.ui.window.DialogProperties\n" + add, "import DialogProperties")

    # ---- model: clip + sheets
    out = replace_once(out,
        "private class W7Clip(val file: File, val cut: Boolean)\n"
        "private sealed class W7Sheet {\n"
        "    class Rename(val file: File) : W7Sheet()\n"
        "    class Info(val file: File) : W7Sheet()\n"
        "}\n",
        "private class W7Clip(val files: List<File>, val cut: Boolean)\n"
        "private sealed class W7Sheet {\n"
        "    class Rename(val file: File) : W7Sheet()\n"
        "    class Info(val files: List<File>) : W7Sheet()\n"
        "    class Delete(val files: List<File>) : W7Sheet()\n"
        "    object NewDir : W7Sheet()\n"
        "}\n"
        "private enum class W7Ico { COPY, CUT, PASTE, RENAME, ZIP, UNZIP, DELETE, NEWDIR, STAR, STAR_OUT, INFO }\n"
        "\n"
        "private fun w7Names(l: List<File>): String = if (l.size == 1) l[0].name else l.size.toString() + \" item\"\n"
        "private fun w7StemEnd(name: String, dir: Boolean): Int =\n"
        "    if (dir) name.length else name.lastIndexOf('.').let { if (it > 0) it else name.length }\n",
        "model clip/sheet")

    # ---- state block
    new_state = r'''    // [FILEOPS-2] alat berkas Penjelajah: pilih banyak, salin, pindah, tempel, hapus, folder baru, ZIP/ekstrak, ubah nama, favorit, properti
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var clip by remember { mutableStateOf<W7Clip?>(null) }
    var toolsOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<W7Sheet?>(null) }
    var messageOk by remember { mutableStateOf(false) }
    var multi by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var progress by remember { mutableStateOf<Float?>(null) }
    val cancelFlag = remember { AtomicBoolean(false) }
    val guard = remember {
        vols.map { it.dir.absolutePath }.toSet() +
            File(Environment.getExternalStorageDirectory(), com.molina.core.common.MolinaPaths.APP_FOLDER).absolutePath
    }
    fun say(text: String, ok: Boolean = false) { message = text; messageOk = ok }
    fun targets(): List<File> = if (multi) picked.map { File(it) } else listOfNotNull(selected?.file)
    fun runOp(op: (FileOps.Ctl) -> String, onDone: () -> Unit = {}) {
        if (busy) return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            say("Izin tulis penyimpanan belum diberikan. Aktifkan izin Penyimpanan di Setelan Android.")
            return
        }
        busy = true
        progress = null
        cancelFlag.set(false)
        say("Memproses\u2026", true)
        scope.launch {
            val ctl = FileOps.Ctl(cancelFlag) { p -> progress = p }
            val r = withContext(Dispatchers.IO) { runCatching { op(ctl) } }
            busy = false
            progress = null
            r.onSuccess { say(it, true); onDone(); selected = null; picked = emptySet(); fileName = ""; tick++ }
                .onFailure { say(it.message ?: "Operasi gagal."); tick++ }
        }
    }
'''
    out = slice_replace(out, "    // [FILEOPS-1] alat berkas Penjelajah",
                        "\n    LaunchedEffect(Unit) { if (!granted) ask.launch", new_state, "blok state")

    # ---- go(): reset pilihan
    out = replace_once(out,
        '    fun go(to: File) { dir = to; selected = null; fileName = ""; message = "" }\n',
        '    fun go(to: File) { dir = to; selected = null; picked = emptySet(); fileName = ""; message = "" }\n',
        "go()")

    # ---- Back: keluar dari mode pilih dulu
    out = replace_once(out,
        "onDismissRequest = { if (!goUp()) onDismiss() },",
        "onDismissRequest = { if (multi) { multi = false; picked = emptySet() } else if (!goUp()) onDismiss() },",
        "onDismissRequest")

    # ---- Keyboard tidak muncul sendiri
    out = replace_once(out,
        "dismissOnClickOutside = false)\n    ) {\n",
        "dismissOnClickOutside = false)\n    ) {\n"
        "        // [FILEOPS-2] keyboard tidak boleh muncul sendiri saat Penjelajah dibuka\n"
        "        val dlgFocus = LocalFocusManager.current\n"
        "        val dlgView = LocalView.current\n"
        "        val dlgWindow = (dlgView.parent as? DialogWindowProvider)?.window\n"
        "        SideEffect {\n"
        "            dlgWindow?.setSoftInputMode(\n"
        "                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE\n"
        "            )\n"
        "        }\n"
        "        LaunchedEffect(Unit) {\n"
        "            dlgFocus.clearFocus(force = true)\n"
        "            delay(250)\n"
        "            dlgFocus.clearFocus(force = true)\n"
        "            (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(dlgView.windowToken, 0)\n"
        "        }\n",
        "dialog keyboard")

    # ---- toolbar: Pilih / Semua / Alat
    new_menu = r'''                    if (browse) {
                        W7Cmd(
                            if (multi) "Selesai" + (if (picked.isNotEmpty()) " (" + picked.size + ")" else "") else "Pilih",
                            !busy
                        ) { multi = !multi; picked = emptySet(); selected = null; fileName = "" }
                        if (multi) W7Cmd("Semua", !busy) {
                            val all = (entries ?: emptyList()).map { it.file.path }.toSet()
                            picked = if (all.isNotEmpty() && picked == all) emptySet() else all
                        }
                    }
                    if (browse) Box {
                        W7Cmd("\u22EE Alat", !busy) { toolsOpen = !toolsOpen }
                        if (toolsOpen) {
                            val tg = targets()
                            val one = tg.singleOrNull()
                            val favTarget = if (tg.isEmpty()) dir else one?.takeIf { it.isDirectory }
                            val isFav = favTarget != null && ExplorerFavorites.contains(ctx, favTarget)
                            val c = clip
                            Popup(
                                alignment = Alignment.TopStart,
                                offset = IntOffset(0, with(density) { 30.dp.roundToPx() }),
                                onDismissRequest = { toolsOpen = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Column(
                                    Modifier.width(250.dp).heightIn(max = 400.dp).background(Win7.Input)
                                        .border(1.dp, Win7.FrameLine).verticalScroll(rememberScrollState())
                                ) {
                                    W7MenuItem(W7Ico.COPY, "Salin", tg.isNotEmpty()) {
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) { clip = W7Clip(tg, false); say("Disalin: " + w7Names(tg), true) }
                                    }
                                    W7MenuItem(W7Ico.CUT, "Pindah", tg.isNotEmpty()) {
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) {
                                            clip = W7Clip(tg, true)
                                            say("Siap dipindah: " + w7Names(tg) + ". Buka folder tujuan lalu Tempel.", true)
                                        }
                                    }
                                    W7MenuItem(W7Ico.PASTE, if (c != null) "Tempel: " + w7Names(c.files) else "Tempel", c != null) {
                                        toolsOpen = false
                                        if (c != null) {
                                            val d = dir
                                            runOp(
                                                op = { ctl ->
                                                    val res = if (c.cut) FileOps.moveAll(c.files, d, ctl) else FileOps.copyAll(c.files, d, ctl)
                                                    val head = if (c.cut) "Dipindah: " else "Ditempel: "
                                                    head + w7Names(res)
                                                },
                                                onDone = { if (c.cut) clip = null }
                                            )
                                        }
                                    }
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                                    W7MenuItem(W7Ico.RENAME, "Ubah nama", one != null) {
                                        toolsOpen = false
                                        if (one != null) sheet = W7Sheet.Rename(one)
                                    }
                                    W7MenuItem(W7Ico.ZIP, "Kompres (ZIP)", tg.isNotEmpty()) {
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) {
                                            val d = dir
                                            runOp(op = { ctl -> "ZIP dibuat: " + FileOps.zipAll(tg, d, ctl).name })
                                        }
                                    }
                                    W7MenuItem(
                                        W7Ico.UNZIP, "Ekstrak ZIP",
                                        one != null && one.isFile && one.extension.equals("zip", ignoreCase = true)
                                    ) {
                                        toolsOpen = false
                                        if (one != null) {
                                            val d = dir
                                            runOp(op = { ctl -> "Diekstrak ke: " + FileOps.unzip(one, d, ctl).name })
                                        }
                                    }
                                    W7MenuItem(W7Ico.DELETE, "Hapus", tg.isNotEmpty()) {
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) sheet = W7Sheet.Delete(tg)
                                    }
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                                    W7MenuItem(W7Ico.NEWDIR, "Folder baru", true) {
                                        toolsOpen = false
                                        sheet = W7Sheet.NewDir
                                    }
                                    W7MenuItem(
                                        if (isFav) W7Ico.STAR else W7Ico.STAR_OUT,
                                        if (isFav) "Hapus dari favorit" else "Pindah ke favorit",
                                        favTarget != null
                                    ) {
                                        toolsOpen = false
                                        if (favTarget != null) {
                                            val nm = favTarget.name.ifEmpty { favTarget.path }
                                            if (isFav) {
                                                ExplorerFavorites.remove(ctx, favTarget)
                                                say("Dihapus dari favorit: " + nm, true)
                                            } else if (w7Favorites(browse).any { it.dir.path == favTarget.path }) {
                                                say("Folder ini sudah ada di favorit bawaan.")
                                            } else {
                                                ExplorerFavorites.add(ctx, favTarget)
                                                say("Ditambahkan ke favorit: " + nm, true)
                                            }
                                            favTick++
                                        }
                                    }
                                    W7MenuItem(W7Ico.INFO, "Properti", true) {
                                        toolsOpen = false
                                        sheet = W7Sheet.Info(if (tg.isEmpty()) listOf(dir) else tg)
                                    }
                                }
                            }
                        }
                    }
'''
    out = slice_replace(out, "                    if (browse) Box {\n                        W7Cmd(",
                        "                }\n                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))\n"
                        "                Row(Modifier.weight(1f).fillMaxWidth()) {\n",
                        new_menu, "menu Alat")

    # ---- daftar: kotak centang di mode pilih
    out = replace_once(out,
        "                        onOpen = { e ->\n                            if (e.isDir) go(e.file)\n",
        "                        multi = multi, picked = picked,\n"
        "                        onOpen = { e ->\n"
        "                            if (multi) picked = if (e.file.path in picked) picked - e.file.path else picked + e.file.path\n"
        "                            else if (e.isDir) go(e.file)\n",
        "onOpen")
    out = replace_once(out,
        "    selected: W7Entry?,\n    onSort: (W7Sort) -> Unit,",
        "    selected: W7Entry?,\n    multi: Boolean = false,\n    picked: Set<String> = emptySet(),\n    onSort: (W7Sort) -> Unit,",
        "W7FileArea params")
    out = replace_once(out,
        "items(entries, key = { it.file.path }) { e -> W7EntryRow(e, e == selected, wide, onOpen) }",
        "items(entries, key = { it.file.path }) { e ->\n"
        "                        val on = e.file.path in picked\n"
        "                        W7EntryRow(e, if (multi) on else e == selected, wide, onOpen, if (multi) on else null)\n"
        "                    }",
        "W7EntryRow call")
    out = replace_once(out,
        "private fun W7EntryRow(e: W7Entry, selected: Boolean, wide: Boolean, onOpen: (W7Entry) -> Unit) {",
        "private fun W7EntryRow(e: W7Entry, selected: Boolean, wide: Boolean, onOpen: (W7Entry) -> Unit, checked: Boolean? = null) {",
        "W7EntryRow def")
    out = replace_once(out,
        "        if (e.isDir) Win7Folder(20.dp) else Win7File(video, 20.dp)\n",
        "        if (checked != null) {\n"
        "            Text(if (checked) \"[x]\" else \"[  ]\", color = Win7.NavHeader, fontSize = 12.sp)\n"
        "            Spacer(Modifier.width(6.dp))\n"
        "        }\n"
        "        if (e.isDir) Win7Folder(20.dp) else Win7File(video, 20.dp)\n",
        "ikon baris")

    # ---- progres + Hentikan
    out = replace_once(out,
        "                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {\n"
        "                        Text(message, color = if (messageOk)",
        "                    if (busy) Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {\n"
        "                        Box(Modifier.weight(1f).height(6.dp).background(Win7.Hairline)) {\n"
        "                            val p = progress\n"
        "                            Box(Modifier.fillMaxHeight().fillMaxWidth(p?.coerceIn(0f, 1f) ?: 0.25f).background(Win7.DefaultLine))\n"
        "                        }\n"
        "                        Text(\n"
        "                            \"Hentikan\", color = Win7.NavHeader, fontSize = 12.sp,\n"
        "                            modifier = Modifier.clickable { cancelFlag.set(true) }.padding(start = 10.dp, top = 4.dp, bottom = 4.dp)\n"
        "                        )\n"
        "                    }\n"
        "                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {\n"
        "                        Text(message, color = if (messageOk)",
        "progres")

    # ---- sheets
    new_sheets = r'''    when (val s = sheet) {
        is W7Sheet.Rename -> W7NameDialog(
            title = if (s.file.isDirectory) "Ubah nama folder" else "Ubah nama berkas",
            initial = s.file.name, selectEnd = w7StemEnd(s.file.name, s.file.isDirectory), okLabel = "OK",
            onCancel = { sheet = null }
        ) { name ->
            sheet = null
            runOp(op = { "Nama diubah: " + FileOps.rename(s.file, name).name })
        }
        W7Sheet.NewDir -> W7NameDialog(
            title = "Folder baru", initial = "Folder baru", selectEnd = "Folder baru".length, okLabel = "Buat",
            onCancel = { sheet = null }
        ) { name ->
            sheet = null
            val d = dir
            runOp(op = { "Folder dibuat: " + FileOps.mkdir(d, name).name })
        }
        is W7Sheet.Delete -> W7ConfirmDialog(s.files, onCancel = { sheet = null }) {
            sheet = null
            runOp(op = { ctl -> "Dihapus: " + FileOps.deleteAll(s.files, guard, ctl) + " item" })
        }
        is W7Sheet.Info -> W7InfoDialog(s.files, if (s.files.size == 1) isVisibleInDebian?.invoke(s.files[0]) else null) { sheet = null }
        null -> Unit
    }

'''
    out = slice_replace(out, "    when (val s = sheet) {\n", "    val frame = RoundedCornerShape(8.dp)\n", new_sheets, "sheets")

    # ---- komposabel pendukung (ganti seluruh ekor tahap 1)
    new_tail = r'''
@Composable
private fun W7MenuItem(ico: W7Ico, label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        W7MenuIcon(ico, enabled)
        Spacer(Modifier.width(10.dp))
        Text(
            label, color = if (enabled) Win7.Ink else Win7.TextDim, fontSize = 13.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Ikon garis 16dp bergaya Aero untuk menu Alat (digambar di Canvas, tanpa aset). */
@Composable
private fun W7MenuIcon(ico: W7Ico, enabled: Boolean) {
    val c = if (!enabled) Win7.Line else when (ico) {
        W7Ico.DELETE -> Color(0xFFE57373)
        W7Ico.STAR, W7Ico.STAR_OUT -> Color(0xFFE0A800)
        else -> Win7.NavHeader
    }
    Canvas(Modifier.size(16.dp)) {
        val u = size.width / 16f
        val sw = 1.4f * u
        val st = Stroke(width = sw, cap = StrokeCap.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(c, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = sw, cap = StrokeCap.Round)
        fun box(x: Float, y: Float, w: Float, h: Float) =
            drawRect(c, Offset(x * u, y * u), Size(w * u, h * u), style = st)
        when (ico) {
            W7Ico.COPY -> { box(2f, 4f, 8f, 10f); box(6f, 2f, 8f, 10f) }
            W7Ico.CUT -> {
                line(4f, 2f, 12f, 11f); line(12f, 2f, 4f, 11f)
                drawCircle(c, radius = 2f * u, center = Offset(4f * u, 13f * u), style = st)
                drawCircle(c, radius = 2f * u, center = Offset(12f * u, 13f * u), style = st)
            }
            W7Ico.PASTE -> { box(3f, 3f, 10f, 11f); drawRect(c, Offset(6f * u, 1.5f * u), Size(4f * u, 3f * u)) }
            W7Ico.RENAME -> { line(8f, 3f, 8f, 13f); line(5f, 3f, 11f, 3f); line(5f, 13f, 11f, 13f) }
            W7Ico.ZIP -> {
                box(3f, 2f, 10f, 12f)
                line(8f, 2f, 8f, 5f); line(6.5f, 6f, 9.5f, 6f); line(6.5f, 8.5f, 9.5f, 8.5f); line(6.5f, 11f, 9.5f, 11f)
            }
            W7Ico.UNZIP -> { box(2f, 8f, 12f, 6f); line(8f, 1.5f, 8f, 9f); line(5.5f, 6f, 8f, 9f); line(10.5f, 6f, 8f, 9f) }
            W7Ico.DELETE -> {
                line(2.5f, 4f, 13.5f, 4f); line(6f, 2.2f, 10f, 2.2f)
                line(4f, 5f, 5f, 14f); line(12f, 5f, 11f, 14f); line(5f, 14f, 11f, 14f)
                line(7f, 6.5f, 7f, 12f); line(9f, 6.5f, 9f, 12f)
            }
            W7Ico.NEWDIR -> {
                box(2f, 5f, 12f, 8f)
                line(2f, 5f, 4f, 3f); line(4f, 3f, 7f, 3f); line(7f, 3f, 8f, 5f)
                line(8f, 7.5f, 8f, 11.5f); line(6f, 9.5f, 10f, 9.5f)
            }
            W7Ico.STAR, W7Ico.STAR_OUT -> {
                val p = Path()
                for (k in 0 until 10) {
                    val r = if (k % 2 == 0) 6.5f else 2.8f
                    val a = -PI / 2 + k * PI / 5
                    val x = (8f + r * cos(a).toFloat()) * u
                    val y = (8.7f + r * sin(a).toFloat()) * u
                    if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, c, style = if (ico == W7Ico.STAR) Fill else st)
            }
            W7Ico.INFO -> {
                drawCircle(c, radius = 6.2f * u, center = Offset(8f * u, 8f * u), style = st)
                line(8f, 7f, 8f, 11.5f)
                drawCircle(c, radius = 0.9f * u, center = Offset(8f * u, 4.9f * u))
            }
        }
    }
}

@Composable
private fun W7Panel(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(shape)
                .background(Brush.verticalGradient(listOf(Win7.FrameTop, Win7.FrameBottom)), shape)
                .border(1.dp, Win7.FrameLine, shape).padding(14.dp)
        ) {
            Text(title, color = Win7.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun W7NameDialog(
    title: String, initial: String, selectEnd: Int, okLabel: String,
    onCancel: () -> Unit, onOk: (String) -> Unit
) {
    var tf by remember(initial) { mutableStateOf(TextFieldValue(initial, TextRange(0, selectEnd.coerceIn(0, initial.length)))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(initial) { delay(150); runCatching { focus.requestFocus() } }
    W7Panel(title, onCancel) {
        BasicTextField(
            value = tf,
            onValueChange = { tf = it },
            singleLine = true,
            textStyle = TextStyle(color = Win7.Ink, fontSize = 14.sp),
            cursorBrush = SolidColor(Win7.Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (tf.text.isNotBlank()) onOk(tf.text) }),
            modifier = Modifier.fillMaxWidth().height(32.dp).background(Win7.Input)
                .border(1.dp, Win7.Field).padding(horizontal = 8.dp).focusRequester(focus),
            decorationBox = { inner -> Box(Modifier.fillMaxSize(), Alignment.CenterStart) { inner() } }
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Win7Button(okLabel, isDefault = true, enabled = tf.text.isNotBlank()) { onOk(tf.text) }
            Spacer(Modifier.width(8.dp))
            Win7Button("Batal", onClick = onCancel)
        }
    }
}

@Composable
private fun W7ConfirmDialog(files: List<File>, onCancel: () -> Unit, onOk: () -> Unit) {
    val hasDir = files.any { it.isDirectory }
    W7Panel("Hapus", onCancel) {
        Text(
            if (files.size == 1) "Hapus \"" + files[0].name + "\"" + (if (hasDir) " beserta seluruh isinya?" else "?")
            else "Hapus " + files.size + " item terpilih" + (if (hasDir) " beserta isi folder?" else "?"),
            color = Win7.Ink, fontSize = 13.sp
        )
        if (files.size > 1) {
            Spacer(Modifier.height(6.dp))
            files.take(4).forEach { Text("\u2022 " + it.name, color = Win7.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (files.size > 4) Text("\u2022 \u2026 dan " + (files.size - 4) + " lainnya", color = Win7.TextDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Penghapusan tidak dapat dibatalkan.", color = Color(0xFFFF8A80), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Win7Button("Hapus", isDefault = true) { onOk() }
            Spacer(Modifier.width(8.dp))
            Win7Button("Batal", onClick = onCancel)
        }
    }
}

@Composable
private fun W7InfoDialog(files: List<File>, debian: Boolean?, onDismiss: () -> Unit) {
    var ps by remember(files) { mutableStateOf<List<FileOps.Props>?>(null) }
    var failed by remember(files) { mutableStateOf(false) }
    LaunchedEffect(files) {
        val r = withContext(Dispatchers.IO) { runCatching { files.map { FileOps.props(it) } } }
        ps = r.getOrNull()
        failed = r.isFailure
    }
    val first = files.first()
    W7Panel("Properti", onDismiss) {
        Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
            W7InfoRow("Nama", if (files.size == 1) first.name.ifEmpty { first.path } else files.size.toString() + " item")
            W7InfoRow("Lokasi", first.parent ?: "/")
            val x = ps
            if (x == null) {
                Text(
                    if (failed) "Properti tidak dapat dibaca." else "Menghitung ukuran\u2026",
                    color = Win7.TextDim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)
                )
            } else if (x.size == 1) {
                val p = x[0]
                W7InfoRow("Jenis", if (p.symlink) "Tautan simbolik" else if (p.isDir) "Folder" else if (first.extension.isEmpty()) "Berkas" else "Berkas ." + first.extension.lowercase())
                W7InfoRow("Ukuran", FileOps.readable(p.size) + " (" + String.format(W7_ID, "%,d", p.size) + " byte)")
                if (p.isDir) W7InfoRow("Isi", p.files.toString() + " berkas, " + p.folders + " folder")
                W7InfoRow("Diubah", w7Date(p.modified))
                W7InfoRow("Izin", (if (p.canRead) "Baca" else "Tanpa baca") + ", " + (if (p.canWrite) "Tulis" else "Tanpa tulis"))
            } else {
                val bytes = x.fold(0L) { a, p -> a + p.size }
                val nFiles = x.fold(0) { a, p -> a + (if (p.isDir) p.files else 1) }
                val nDirs = x.fold(0) { a, p -> a + (if (p.isDir) p.folders + 1 else 0) }
                W7InfoRow("Ukuran", FileOps.readable(bytes) + " (" + String.format(W7_ID, "%,d", bytes) + " byte)")
                W7InfoRow("Isi", nFiles.toString() + " berkas, " + nDirs + " folder")
            }
            if (debian != null) W7InfoRow("Debian", if (debian) "Terlihat di Debian" else "Di luar Debian")
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Win7Button("Tutup", isDefault = true, onClick = onDismiss)
        }
    }
}

@Composable
private fun W7InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Win7.TextDim, fontSize = 12.sp, modifier = Modifier.width(62.dp))
        Text(value, color = Win7.Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}
'''
    out = slice_replace(out, "\n@Composable\nprivate fun W7MenuItem", None, new_tail, "ekor komposabel")

    write_if_changed(DLG, out)

# ================================================================ SettingsScreen.kt (pindah ke root)
SP = os.path.join(ROOT, SET)
s = read(SP)
if "Page.Files" in s:
    print("= SettingsScreen.kt sudah dipatch (lewati)")
else:
    s = replace_once(s, "Scripts, Packages, Code\n}", "Scripts, Packages, Code, Files\n}", "enum Page")
    s = replace_once(s, "    Page.System, Page.Scripts, Page.Packages -> Page.Home\n",
                     "    Page.System, Page.Scripts, Page.Packages, Page.Files -> Page.Home\n", "parentOf")
    s = replace_once(s, '    Page.Packages -> "Paket"\n', '    Page.Packages -> "Paket"\n    Page.Files -> "Berkas"\n', "titleOf")
    s = replace_once(s,
        '    Hit("Penjelajah Molina", "Telusuri berkas, salin path, atau kirim ke Terminal", Page.System),\n',
        '    Hit("Berkas", "Penjelajah Molina: telusuri, salin, pindah, kompres, hapus, kirim path ke Terminal", Page.Files),\n'
        '    Hit("Penjelajah Molina", "Kelola berkas: salin, tempel, pindah, ubah nama, ZIP, favorit, properti", Page.Files),\n',
        "Hit")
    s = replace_once(s, "    LaunchedEffect(page) { focus.clearFocus() }\n",
                     "    LaunchedEffect(page) { focus.clearFocus() }\n"
                     "    // Ubin Berkas di beranda langsung membuka Penjelajah Molina.\n"
                     "    LaunchedEffect(page) { if (page == Page.Files) showExplorer = true }\n",
                     "LaunchedEffect")
    s = replace_once(s, "            onDismiss = { showExplorer = false },\n            browse = true,\n",
                     "            onDismiss = { showExplorer = false; if (page == Page.Files) page = Page.Home },\n            browse = true,\n",
                     "dismiss explorer")
    s = replace_once(s,
        "                            onOpen = { open(Page.Packages) }\n                        )\n",
        "                            onOpen = { open(Page.Packages) }\n                        )\n"
        "                        W7CategoryTile(\n"
        "                            icon = { Win7Folder(52.dp) },\n"
        "                            title = \"Berkas\",\n"
        "                            links = emptyList(),\n"
        "                            onOpen = { open(Page.Files) }\n"
        "                        )\n",
        "ubin Home")
    s = replace_once(s,
        '                        Category("Berkas")\n'
        '                        W7Link({ Win7Folder(32.dp) }, "Penjelajah Molina", "Telusuri berkas, salin path, atau kirim ke Terminal") { showExplorer = true }\n',
        "", "hapus dari Sistem")
    s = replace_once(s,
        "                    Page.Packages -> PackagesPage(onOpenYtdlp = { open(Page.Stream) })\n",
        "                    Page.Packages -> PackagesPage(onOpenYtdlp = { open(Page.Stream) })\n"
        "                    Page.Files -> W7Link({ Win7Folder(32.dp) }, \"Penjelajah Molina\", \"Telusuri, salin, pindah, kompres, hapus, atau kirim path ke Terminal\") { showExplorer = true }\n",
        "halaman Files")
    write_if_changed(SP, s)

# ================================================================ progres.md
PROG = os.path.join(ROOT, "progres.md")
TAG = "[EXPLORER-FILEOPS-2]"
ENTRY = '''
## 2026-10-26 - Penjelajah: keyboard, pindah ke root, kelola berkas lanjutan [EXPLORER-FILEOPS-2]
- [x] fix(explorer): keyboard tidak lagi muncul sendiri saat Penjelajah dibuka (soft input mode HIDDEN pada jendela dialog, clearFocus, dan sembunyikan IME); mengetik di kolom Nama file tetap memunculkannya
- [x] refactor(settings): Penjelajah Molina pindah dari Sistem ke beranda Settings sebagai ubin "Berkas" sejajar Sistem, Script execution, dan Paket (Page.Files membuka dialog otomatis; tutup dialog kembali ke beranda); entri pencarian ikut diperbarui; Category "Berkas" di halaman Sistem dihapus
- [x] feat(explorer): mode Pilih (kotak centang, Semua, Selesai; Back keluar dari mode pilih dulu) sehingga Salin, Pindah, Tempel, Kompres, Hapus bekerja massal
- [x] feat(explorer): Hapus dengan dialog konfirmasi (akar penyimpanan dan /sdcard/Molina dilindungi), Folder baru, Ekstrak ZIP (zip-slip ditolak), Kompres banyak item jadi satu ZIP
- [x] feat(explorer): progres byte + tombol Hentikan untuk salin/pindah/ZIP/ekstrak/hapus; hasil parsial dibersihkan saat gagal atau dihentikan
- [x] feat(explorer): ikon garis bergaya Aero di menu Alat (Canvas, tanpa aset); Properti mendukung banyak item (total ukuran dan isi)
- [x] Dampak: :app saja (cinema/FileOps.kt, cinema/Win7OpenDialog.kt, settings/SettingsScreen.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (Popup/Dialog bertingkat, setSoftInputMode, edit Compose besar). FileOps sudah diuji JVM murni dengan kotlinc di luar CI
- [ ] uji perangkat: buka Settings > Berkas > keyboard TIDAK muncul; ubin Berkas ada di beranda dan tidak lagi di Sistem; Pilih > centang beberapa berkas > Salin > folder lain > Tempel; Pindah massal; Hapus 1 berkas dan 1 folder (konfirmasi muncul, akar penyimpanan ditolak); Folder baru (nama bentrok ditolak); Kompres banyak item; Ekstrak ZIP; salinan berkas besar menampilkan progres lalu Hentikan membatalkan dan tidak meninggalkan sisa; ikon menu tampil; Properti multi-item; Back saat mode Pilih hanya menutup mode Pilih
- [ ] LANGKAH BERIKUT: sortir/pencarian nama di Penjelajah, buka berkas dengan aplikasi lain, kirim ke Script execution dari Penjelajah, tampilan grid
'''
cur = read(PROG) if os.path.isfile(PROG) else ""
if TAG in cur:
    print("= progres.md sudah memuat", TAG)
else:
    save(PROG, cur.rstrip("\n") + "\n" + ENTRY)
    print("+ progres.md diperbarui:", TAG)

print("Selesai.")
