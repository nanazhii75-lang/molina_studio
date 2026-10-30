package com.molina.studio.cinema

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.composed
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.molina.core.common.LogLevel
import com.molina.studio.log.LogBus
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.isActive
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import com.molina.studio.terminal.RootfsManager
import com.molina.studio.terminal.TerminalSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.molina.studio.ui.MolinaTheme
import com.molina.core.common.TrashBin
import com.molina.core.common.TrashItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class W7Entry(val file: File, val isDir: Boolean, val size: Long, val modified: Long, val rel: String? = null) {
    val name: String get() = file.name
}

private enum class W7Kind(val label: String) { MEDIA("Media (video dan audio)"), ALL("Semua file") }
private class W7Place(val label: String, val dir: File, val molina: Boolean = false)
private class W7Clip(val files: List<File>, val cut: Boolean)
private sealed class W7Sheet {
    class Rename(val file: File) : W7Sheet()
    class Zip(val files: List<File>) : W7Sheet()
    class Compress(val files: List<File>) : W7Sheet() // [ARCHIVE2] dialog Kompres (format, level, nama, sandi ZIP)
    class Password(val zip: File, val wrong: Boolean) : W7Sheet()
    class ZipView(val zip: File) : W7Sheet() // [ZIP-VIEW]
    class Archive(val file: File, val pw: String?) : W7Sheet() // [ARCHIVE] 7z/tar/gz/bz2/xz (dan ZIP lewat "Ekstrak pilihan")
    class ArchivePw(val file: File, val wrong: Boolean) : W7Sheet() // [ARCHIVE]
    class Info(val files: List<File>) : W7Sheet()
    class Delete(val files: List<File>, val permanent: Boolean = false) : W7Sheet()
    object NewDir : W7Sheet()
}
private enum class W7Ico { PLAY, CODE, SCRIPT, COPY, CUT, PASTE, RENAME, ZIP, UNZIP, DELETE, NEWDIR, STAR, STAR_OUT, INFO, SHARE, OPEN_WITH }

private fun w7Names(l: List<File>): String = if (l.size == 1) l[0].name else l.size.toString() + " item"
private fun w7StemEnd(name: String, dir: Boolean): Int =
    if (dir) name.length else name.lastIndexOf('.').let { if (it > 0) it else name.length }

private val W7_ID = Locale("id", "ID")
private val W7_LISTS = setOf("m3u", "m3u8", "pls", "cue")
private val W7_AUDIO = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "wma", "ac3", "dts", "mka")
private val W7_MEDIA = W7_AUDIO + setOf(
    "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts", "m2ts", "mts", "flv", "wmv",
    "mpg", "mpeg", "ogv", "vob", "rmvb", "m3u", "m3u8", "pls", "cue"
)

private fun w7HasStorage(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

private fun w7Favorites(browse: Boolean = false): List<W7Place> = listOfNotNull(
    W7Place("Molina", File(Environment.getExternalStorageDirectory(), com.molina.core.common.MolinaPaths.APP_FOLDER), true),
    if (browse) W7Place("Project", File(Environment.getExternalStorageDirectory(), TerminalSessions.WORKSPACE_DIR)) else null,
    W7Place("Video", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)),
    W7Place("Unduhan", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)),
    W7Place("Musik", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)),
    W7Place("Kamera", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
).filter { it.dir.isDirectory }

/** Beranda Debian (/root di rootfs) sebagai akar tambahan agar berkas Debian bisa dikelola dari Penjelajah. */
private fun w7DebianPlaces(ctx: Context, browse: Boolean): List<W7Place> {
    if (!browse) return emptyList()
    val home = File(RootfsManager.rootfsDir(ctx), "root")
    return if (home.isDirectory) listOf(W7Place("Debian", home)) else emptyList()
}

private fun w7Drives(): List<W7Place> {
    val out = mutableListOf(W7Place("Internal", Environment.getExternalStorageDirectory()))
    File("/storage").listFiles()?.forEach {
        if (it.isDirectory && it.canRead() && it.name != "emulated" && it.name != "self") out.add(W7Place(it.name, it))
    }
    return out
}

/** Isi folder lewat [ExplorerList]; pasangan hasil = (daftar, terpotong?) atau null bila folder tak terbaca. */
private fun w7Load(dir: File, query: String, o: ExplorerList.Opts, cancelled: () -> Boolean): Pair<List<W7Entry>, Boolean>? {
    val r = ExplorerList.list(dir, query, o, cancelled) ?: return null
    return Pair(r.items.map { W7Entry(it.file, it.isDir, it.size, it.modified, it.rel) }, r.truncated)
}

private fun w7Size(b: Long) = String.format(W7_ID, "%,d KB", maxOf(1L, (b + 1023) / 1024))
private fun w7Date(ms: Long) = SimpleDateFormat("dd/MM/yyyy HH.mm", W7_ID).format(Date(ms))

private fun w7Crumbs(dir: File, drives: List<W7Place>): List<Pair<String, File>> {
    val base = drives.filter { dir.path == it.dir.path || dir.path.startsWith(it.dir.path + "/") }
        .maxByOrNull { it.dir.path.length } ?: W7Place(dir.name.ifEmpty { "/" }, dir)
    val out = mutableListOf(Pair(base.label, base.dir))
    var cur = base.dir
    dir.path.removePrefix(base.dir.path).split('/').filter { it.isNotEmpty() }.forEach {
        cur = File(cur, it)
        out.add(Pair(it, cur))
    }
    return out
}

/** Pemilih file lokal bergaya Windows 7 (Aero). Mengembalikan File yang dipilih lewat [onPick]. */
@Composable
fun Win7OpenDialog(
    onDismiss: () -> Unit,
    onPick: (File) -> Unit = {},
    browse: Boolean = false,
    showClose: Boolean = true,
    asScreen: Boolean = false,
    screenModifier: Modifier = Modifier,
    onCopyPath: ((File) -> Unit)? = null,
    onOpenTerminal: ((File) -> Unit)? = null,
    onShare: ((List<File>) -> Boolean)? = null,
    onOpenWith: ((File) -> Boolean)? = null,
    onInsertPaths: ((List<File>) -> Unit)? = null,
    isVisibleInDebian: ((File) -> Boolean)? = null,
    pickFolder: Boolean = false,
    onPickFolder: (File) -> Unit = {},
    onPlay: (suspend (List<File>) -> Boolean)? = null,
    onOpenInCode: ((List<File>) -> Unit)? = null,
    onSendToScripts: ((File) -> Pair<Boolean, String>)? = null,
    startDir: File? = null
) {
    val ctx = LocalContext.current
    var favTick by remember { mutableStateOf(0) } // [FILEOPS-1]
    val favs = remember(favTick) {
        val base = w7Favorites(browse)
        val mine = if (browse) ExplorerFavorites.load(ctx).filter { f -> base.none { it.dir.path == f.path } } else emptyList()
        base + mine.map { W7Place(it.name.ifEmpty { it.path }, it) }
    }
    val vols = remember { w7Drives() + w7DebianPlaces(ctx, browse) }
    val volDirs = remember(vols) { vols.map { it.dir } }
    var granted by remember { mutableStateOf(w7HasStorage(ctx)) }
    // [FILES-PLAYED] riwayat putar Cinema (terbaru di indeks 0) untuk chip di daftar; ikut tersegarkan lewat version
    val playedRefs = remember(CinemaRecent.version) { CinemaRecent.load(ctx).map { w7Norm(it.ref) } }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    // Mode jelajah: tanpa startDir, mulai dari folder terakhir yang dibuka (bila masih ada), bukan selalu dari akar
    var dir by remember { mutableStateOf(startDir?.takeIf { it.isDirectory } ?: (if (browse) ExplorerRecent.last(ctx) else null) ?: vols.first().dir) }
    // [NAV-SWIPE] panel kiri: dock di landscape, drawer geser di potret
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    var navOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = portrait && navOpen) { navOpen = false }
    val recents = remember(dir) {
        if (browse) ExplorerRecent.load(ctx).filter { r -> r != dir && vols.none { it.dir == r } && favs.none { it.dir == r } }.take(5)
        else emptyList()
    }
    var kind by remember { mutableStateOf(if (browse) W7Kind.ALL else W7Kind.MEDIA) }
    // [EXPLORER-SEARCH] pencarian nama + sortir yang diingat
    val savedSort = remember { ExplorerPrefs.load(ctx) }
    var key by remember { mutableStateOf(savedSort.key) }
    var asc by remember { mutableStateOf(savedSort.asc) }
    var foldersFirst by remember { mutableStateOf(savedSort.foldersFirst) }
    var showHidden by remember { mutableStateOf(savedSort.showHidden) }
    var deepSearch by remember { mutableStateOf(savedSort.deep) }
    var playedFirst by remember { mutableStateOf(savedSort.playedFirst) }
    var query by remember { mutableStateOf("") }
    var sortOpen by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }
    var entries by remember { mutableStateOf<List<W7Entry>?>(emptyList()) }
    var selected by remember { mutableStateOf<W7Entry?>(null) }
    var fileName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var undo by remember { mutableStateOf<List<TrashItem>?>(null) }
    var showBin by remember { mutableStateOf(false) }
    var comboOpen by remember { mutableStateOf(false) }
    // [FILEOPS-2] alat berkas Penjelajah: pilih banyak, salin, pindah, tempel, hapus, folder baru, ZIP/ekstrak, ubah nama, favorit, properti
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var clip by remember { mutableStateOf<W7Clip?>(null) }
    var toolsOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<W7Sheet?>(null) }
    var messageOk by remember { mutableStateOf(false) }
    var multi by remember { mutableStateOf(false) }
    var showSelectHelp by remember { mutableStateOf(false) } // [HELP-PANEL]
    var picked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var progress by remember { mutableStateOf<Float?>(null) }
    val cancelFlag = remember { AtomicBoolean(false) }
    val guard = remember {
        vols.map { File(it.dir, TrashBin.DIR).absolutePath }.toSet() + vols.map { it.dir.absolutePath }.toSet() +
            File(Environment.getExternalStorageDirectory(), com.molina.core.common.MolinaPaths.APP_FOLDER).absolutePath
    }
    fun say(text: String, ok: Boolean = false) { message = text; messageOk = ok }
    fun targets(): List<File> = if (multi) picked.map { File(it) } else listOfNotNull(selected?.file)
    fun runOp(op: (FileOps.Ctl) -> String, onDone: () -> Unit = {}, onFail: (Throwable) -> Unit = {}) {
        if (busy) return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            say("Izin tulis penyimpanan belum diberikan. Aktifkan izin Penyimpanan di Setelan Android.")
            return
        }
        busy = true
        progress = null
        cancelFlag.set(false)
        undo = null
        say("Memproses\u2026", true)
        scope.launch {
            val ctl = FileOps.Ctl(cancelFlag) { p -> progress = p }
            val r = withContext(Dispatchers.IO) { runCatching { op(ctl) } }
            busy = false
            progress = null
            r.onSuccess { say(it, true); onDone(); selected = null; picked = emptySet(); fileName = ""; tick++ }
                .onFailure {
                    say(it.message ?: ("Operasi gagal (" + it.javaClass.simpleName + ")"))
                    if (it !is FileOps.NeedPassword && it !is FileOps.WrongPassword && it.message != "Dibatalkan") {
                        LogBus.log("penjelajah", LogLevel.Error, "FileOps", w7Trace(it))
                    }
                    tick++
                    onFail(it)
                }
        }
    }

    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
    LaunchedEffect(Unit) { if (browse) withContext(Dispatchers.IO) { runCatching { TrashBin.autoPurge(volDirs) } } }
    LaunchedEffect(key, asc, foldersFirst, showHidden, deepSearch, playedFirst) {
        ExplorerPrefs.save(ctx, ExplorerPrefs.Data(key, asc, foldersFirst, showHidden, deepSearch, playedFirst))
    }
    LaunchedEffect(dir, kind, key, asc, tick, granted, query, foldersFirst, showHidden, deepSearch) {
        if (!granted) return@LaunchedEffect
        if (query.isNotBlank()) delay(250)
        val opts = ExplorerList.Opts(key, asc, foldersFirst, showHidden, deepSearch, if (kind == W7Kind.MEDIA) W7_MEDIA else null)
        val deep = deepSearch && query.isNotBlank()
        if (deep) say("Mencari sampai subfolder\u2026", true)
        val r = withContext(Dispatchers.IO) { w7Load(dir, query, opts) { !isActive } }
        if (deep && message.startsWith("Mencari")) say("")
        entries = r?.first
        if (r != null && r.second) say("Hasil dibatasi " + ExplorerList.MAX_RESULTS + " teratas. Persempit kata kunci.")
    }

    // [FILES-QUEUE] urutan tampil; opsi "terakhir diputar dulu" bersifat stabil (folder tetap di atas, sisanya urutan asal)
    val shown = remember(entries, playedFirst, playedRefs) {
        val es = entries
        if (es == null || !playedFirst) es
        else es.sortedWith(compareBy<W7Entry>({ if (it.isDir) 0 else 1 }, { if (it.isDir) 0 else playedRefs.indexOf(w7Norm(it.file.path)).let { i -> if (i < 0) 99 else i } }))
    }

    val roots = vols.map { it.dir.path }
    val canUp = dir.path !in roots && dir.parentFile != null
    // [NAV-HISTORY] riwayat Mundur/Maju antar folder (maks. 50); tombol bulat di kiri atas = Mundur, bila kosong = Naik
    val backStack = remember { mutableStateListOf<String>() }
    val fwdStack = remember { mutableStateListOf<String>() }
    fun go(to: File) {
        if (to.path != dir.path) {
            backStack.add(dir.path)
            if (backStack.size > 50) backStack.removeAt(0)
            fwdStack.clear()
        }
        dir = to; selected = null; fileName = ""; message = ""; query = ""; undo = null; if (browse) ExplorerRecent.push(ctx, to)
    } // [PICK-PERSIST] picked bertahan lintas folder
    fun jump(to: File) {
        dir = to; selected = null; fileName = ""; message = ""; query = ""; undo = null; if (browse) ExplorerRecent.push(ctx, to)
    }
    fun goBack(): Boolean {
        while (backStack.isNotEmpty()) {
            val p = File(backStack.removeAt(backStack.size - 1))
            if (p.isDirectory) { fwdStack.add(dir.path); jump(p); return true }
        }
        return false
    }
    fun goForward(): Boolean {
        while (fwdStack.isNotEmpty()) {
            val p = File(fwdStack.removeAt(fwdStack.size - 1))
            if (p.isDirectory) { backStack.add(dir.path); jump(p); return true }
        }
        return false
    }
    fun goUp(): Boolean {
        val parent = dir.parentFile
        if (!canUp || parent == null) return false
        go(parent)
        return true
    }
    /** Ketuk ulang berkas terpilih di Penjelajah: media ke mpv, teks ke Code. */
    fun smartOpen(f: File) {
        val play = onPlay
        val code = onOpenInCode
        when {
            play != null && f.extension.lowercase() in W7_MEDIA -> scope.launch {
                val ok = play(listOf(f))
                say(if (ok) "Memutar di mpv: " + f.name else "Gagal memutar " + f.name, ok)
            }
            f.extension.equals("zip", ignoreCase = true) -> sheet = W7Sheet.ZipView(f)
            f.isFile && ArchiveRoute.supports(f.name) -> sheet = W7Sheet.Archive(f, null) // [ARCHIVE]
            code != null && ExplorerList.looksText(f) -> code(listOf(f))
            onOpenWith != null && f.isFile -> onOpenWith?.invoke(f)
            else -> say("Tidak ada aplikasi Molina untuk membuka jenis berkas ini. Pakai menu Alat untuk menyalin atau mengompres.")
        }
    }
    fun confirm() {
        val typed = fileName.trim()
        if (typed.isEmpty()) return
        val target = selected?.takeIf { it.name == typed }?.file
            ?: File(if (typed.startsWith("/")) typed else dir.path + "/" + typed)
        when {
            target.isDirectory -> go(target)
            target.isFile -> onPick(target)
            else -> say("Berkas tidak ditemukan. Periksa nama berkas lalu coba lagi.")
        }
    }

    if (showBin) W7BinDialog(volDirs, { showBin = false }) { tick++ }
    when (val s = sheet) {
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
        is W7Sheet.Delete -> W7ConfirmDialog(s.files, s.permanent, onCancel = { sheet = null }) {
            sheet = null
            if (s.permanent) {
                runOp(op = { ctl -> "Dihapus permanen: " + FileOps.deleteAll(s.files, guard, ctl) + " item" })
            } else {
                val moved = arrayOfNulls<List<TrashItem>>(1)
                runOp(
                    op = { _ ->
                        val r = TrashBin.trash(s.files, volDirs, guard)
                        moved[0] = r
                        "Dipindahkan ke Recycle Bin: " + r.size + " item"
                    },
                    onDone = { undo = moved[0] }
                )
            }
        }
        is W7Sheet.Info -> W7InfoDialog(s.files, if (s.files.size == 1) isVisibleInDebian?.invoke(s.files[0]) else null) { sheet = null }
        is W7Sheet.Compress -> ArchiveCompressDialog( // [ARCHIVE2]
            s.files, dir.name, onClose = { sheet = null }
        ) { opt ->
            sheet = null
            val d = dir
            val items = s.files
            runOp(op = { ctl ->
                val made = ArchiveOps.compress(ctx, items, d, opt, ctl) // [ARCHIVE6] 7z bersandi lewat Debian
                "Arsip dibuat: " + made.name + " (" + CompressNames.sizeText(made.length()) + (if (!opt.password.isNullOrEmpty()) ", bersandi" else "") + ")"
            })
        }
        is W7Sheet.Zip -> W7PasswordDialog(
            title = "Kompres ZIP berkata sandi", okLabel = "Kompres", confirmField = true,
            note = "Enkripsi AES-256. Simpan kata sandinya: ZIP tidak bisa dibuka tanpa itu.",
            onCancel = { sheet = null }
        ) { pw ->
            sheet = null
            val d = dir
            val items = s.files
            runOp(op = { ctl -> "ZIP terenkripsi dibuat: " + FileOps.zipAll(items, d, ctl, pw).name })
        }
        is W7Sheet.Password -> W7PasswordDialog(
            title = "Ekstrak ZIP", okLabel = "Ekstrak", confirmField = false,
            note = if (s.wrong) "Kata sandi salah. Coba lagi." else "ZIP ini dilindungi kata sandi.",
            onCancel = { sheet = null }
        ) { pw ->
            sheet = null
            val d = dir
            val z = s.zip
            runOp(
                op = { ctl -> "Diekstrak ke: " + FileOps.unzip(z, d, ctl, pw).name },
                onFail = { if (it is FileOps.WrongPassword) sheet = W7Sheet.Password(z, true) }
            )
        }
        is W7Sheet.ZipView -> W7ZipDialog(
            s.zip, onClose = { sheet = null }, onOpenFile = { smartOpen(it) },
            clipFiles = { clip?.files ?: emptyList() }, // [ZIP-EDIT]
            onOpenTerminal = if (onOpenTerminal != null) ({ f: File ->
                if (isVisibleInDebian?.invoke(f) != false) { onOpenTerminal.invoke(f.parentFile ?: f); true } else false
            }) else null
        ) {
            sheet = null
            val z = s.zip
            val d = z.parentFile?.takeIf { it.isDirectory } ?: dir
            runOp(
                op = { ctl -> "Diekstrak ke: " + FileOps.unzip(z, d, ctl).name },
                onFail = { if (it is FileOps.NeedPassword) { say(""); sheet = W7Sheet.Password(z, false) } }
            )
        }
        is W7Sheet.Archive -> ArchiveExtractDialog( // [ARCHIVE]
            s.file, s.pw,
            onNeedPassword = { wrong -> sheet = W7Sheet.ArchivePw(s.file, wrong) },
            onClose = { sheet = null }
        ) { exTarget, exOnly ->
            sheet = null
            val a = s.file
            val apw = s.pw
            val d = a.parentFile?.takeIf { it.isDirectory && it.canWrite() } ?: dir
            runOp(
                op = { ctl -> ArchiveOps.extract(ctx, a, d, exTarget, ctl, apw, exOnly).describe() },
                onFail = {
                    if (it is FileOps.NeedPassword) { say(""); sheet = W7Sheet.ArchivePw(a, false) }
                    else if (it is FileOps.WrongPassword) { say(""); sheet = W7Sheet.ArchivePw(a, true) }
                }
            )
        }
        is W7Sheet.ArchivePw -> W7PasswordDialog( // [ARCHIVE]
            title = "Buka arsip", okLabel = "Buka", confirmField = false,
            note = if (s.wrong) "Kata sandi salah. Coba lagi." else "Arsip ini dilindungi kata sandi.",
            onCancel = { sheet = null }
        ) { pw ->
            sheet = W7Sheet.Archive(s.file, pw)
        }
        null -> Unit
    }

    val frame = RoundedCornerShape(8.dp)
    // Keterangan hasil operasi melayang di atas daftar dan hilang sendiri (tidak menyita tinggi panel bawah)
    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            delay(if (undo != null) 9000L else (3500L + message.length * 30L).coerceAtMost(9000L))
            message = ""
        }
    }
    val backAction: () -> Unit = { if (multi) { multi = false; picked = emptySet() } else if (!goUp()) onDismiss() }
    val content: @Composable () -> Unit = {
        // [FILEOPS-2] keyboard tidak boleh muncul sendiri saat Penjelajah dibuka
        val dlgFocus = LocalFocusManager.current
        val dlgView = LocalView.current
        val dlgWindow = (dlgView.parent as? DialogWindowProvider)?.window
        SideEffect {
            dlgWindow?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        LaunchedEffect(Unit) {
            dlgFocus.clearFocus(force = true)
            delay(250)
            dlgFocus.clearFocus(force = true)
            (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(dlgView.windowToken, 0)
        }
        Column(
            (if (asScreen) screenModifier.fillMaxSize().padding(4.dp) else Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 24.dp))
                .clip(frame)
                .background(Brush.verticalGradient(listOf(Win7.FrameTop, Win7.FrameBottom)), frame)
                .border(1.dp, Win7.FrameLine, frame)
                .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
        ) {
            if (!asScreen) Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.Top) {
                Row(Modifier.weight(1f).padding(top = 5.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Win7Folder(16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(if (browse) "Penjelajah Molina" else if (pickFolder) "Buka folder" else "Buka", color = Win7.Ink, fontSize = 13.sp)
                }
                if (showClose) Win7Close(onDismiss)
            }
            Column(Modifier.weight(1f).fillMaxWidth().border(1.dp, Win7.FrameLine).background(Win7.Face)) {
                Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Win7BackOrb(canUp || backStack.isNotEmpty()) { if (!goBack()) goUp() }
                    if (portrait) W7Cmd("\u2630", true) { navOpen = !navOpen }
                    Spacer(Modifier.width(6.dp))
                    Row(
                        Modifier.weight(1f).height(26.dp).background(Win7.Input)
                            .border(1.dp, Win7.Field).padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val cs = w7Crumbs(dir, vols)
                        val shown = if (cs.size > 2) cs.takeLast(2) else cs
                        if (cs.size > 2) Text("\u2026 \u203A ", color = Win7.TextDim, fontSize = 12.sp)
                        shown.forEachIndexed { i, c ->
                            if (i > 0) Text(" \u203A ", color = Win7.TextDim, fontSize = 12.sp)
                            Text(
                                c.first, color = Win7.Ink, fontSize = 12.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).clickable { go(c.second) }
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    W7SearchBox(query, { query = it; selected = null }, Modifier.width(112.dp))
                }
                if (query.isNotBlank()) Row(
                    Modifier.fillMaxWidth().clickable { deepSearch = !deepSearch }.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (deepSearch) "[x]" else "[  ]", color = Win7.Ink, fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Cari sampai subfolder", color = Win7.TextDim, fontSize = 11.sp)
                }
                Row(
                    Modifier.fillMaxWidth().height(30.dp)
                        .horizontalScroll(rememberScrollState())
                        .background(Brush.verticalGradient(listOf(Win7.BarTop, Win7.BarBottom))),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    W7Cmd("Segarkan", true) { tick++ }
                    Box {
                        W7Cmd("Urut \u25BE", true) { sortOpen = !sortOpen }
                        if (sortOpen) Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(0, with(density) { 30.dp.roundToPx() }),
                            onDismissRequest = { sortOpen = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Column(Modifier.width(240.dp).background(Win7.Input).border(1.dp, Win7.FrameLine)) {
                                W7ChoiceItem(if (key == ExplorerList.Key.NAME) "\u25CF" else "\u25CB", "Nama") { key = ExplorerList.Key.NAME; sortOpen = false }
                                W7ChoiceItem(if (key == ExplorerList.Key.DATE) "\u25CF" else "\u25CB", "Tanggal diubah") { key = ExplorerList.Key.DATE; sortOpen = false }
                                W7ChoiceItem(if (key == ExplorerList.Key.SIZE) "\u25CF" else "\u25CB", "Ukuran") { key = ExplorerList.Key.SIZE; sortOpen = false }
                                W7ChoiceItem(if (key == ExplorerList.Key.TYPE) "\u25CF" else "\u25CB", "Jenis (ekstensi)") { key = ExplorerList.Key.TYPE; sortOpen = false }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                                W7ChoiceItem(if (asc) "\u25CF" else "\u25CB", "Menaik (A-Z, lama ke baru)") { asc = true; sortOpen = false }
                                W7ChoiceItem(if (!asc) "\u25CF" else "\u25CB", "Menurun (Z-A, baru ke lama)") { asc = false; sortOpen = false }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                                W7ChoiceItem(if (foldersFirst) "[x]" else "[  ]", "Folder di atas") { foldersFirst = !foldersFirst; sortOpen = false }
                                W7ChoiceItem(if (showHidden) "[x]" else "[  ]", "Tampilkan berkas tersembunyi") { showHidden = !showHidden; sortOpen = false }
                            }
                        }
                    }
                    if (browse) {
                        W7Cmd(
                            if (multi) "Selesai" + (if (picked.isNotEmpty()) " (" + picked.size + ")" else "") else "Pilih",
                            !busy
                        ) {
                            multi = !multi; picked = emptySet(); selected = null; fileName = ""
                        }
                        if (multi) W7Cmd("Semua", !busy) {
                            val here = (entries ?: emptyList()).map { it.file.path }.toSet()
                            picked = if (here.isNotEmpty() && picked.containsAll(here)) picked - here else picked + here
                        }
                        if (picked.isNotEmpty()) W7Cmd("Kosongkan", !busy) { picked = emptySet() }
                    }
                    if (browse) Box {
                        W7Cmd("\u22EE Alat", !busy) { toolsOpen = !toolsOpen }
                        if (toolsOpen) {
                            val tg = targets()
                            val one = tg.singleOrNull()
                            val favTarget = if (tg.isEmpty()) dir else one?.takeIf { it.isDirectory }
                            val isFav = favTarget != null && ExplorerFavorites.contains(ctx, favTarget)
                            val c = clip
                            val playable = onPlay != null && tg.isNotEmpty() &&
                                (one?.isDirectory == true || tg.all { it.isFile && it.extension.lowercase() in W7_MEDIA })
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
                                    W7MenuItem(W7Ico.PLAY, if (one?.isDirectory == true) "Putar folder di mpv" else "Putar di mpv", playable) {
                                        toolsOpen = false
                                        val fn = onPlay
                                        if (fn != null && tg.isNotEmpty()) scope.launch {
                                            val ok = fn(tg)
                                            say(if (ok) "Memutar di mpv: " + w7Names(tg) else "Tidak ada media yang bisa diputar", ok)
                                        }
                                    }
                                    val qAll = (shown ?: emptyList()).filter { !it.isDir && it.file.extension.lowercase() in W7_MEDIA && it.file.extension.lowercase() !in W7_LISTS }.map { it.file }
                                    val qFrom = one?.takeIf { it.isFile }?.let { sel ->
                                        val i = qAll.indexOf(sel)
                                        if (i >= 0) qAll.drop(i) + qAll.take(i) else null
                                    }
                                    W7MenuItem(W7Ico.PLAY, "Putar semua di daftar ini (" + qAll.size + ")", onPlay != null && qAll.size > 1) {
                                        toolsOpen = false
                                        val fn = onPlay
                                        if (fn != null) scope.launch {
                                            val ok = fn(qAll)
                                            say(if (ok) "Memutar antrean " + qAll.size + " berkas di mpv" else "Tidak ada media yang bisa diputar", ok)
                                        }
                                    }
                                    W7MenuItem(W7Ico.PLAY, "Putar mulai dari item ini", onPlay != null && qFrom != null && qFrom.size > 1) {
                                        toolsOpen = false
                                        val fn = onPlay
                                        if (fn != null && qFrom != null) scope.launch {
                                            val ok = fn(qFrom)
                                            say(if (ok) "Memutar " + qFrom.size + " berkas mulai dari " + qFrom[0].name else "Tidak ada media yang bisa diputar", ok)
                                        }
                                    }
                                    W7MenuItem(W7Ico.PLAY, if (playedFirst) "Urutan: normal" else "Urutan: terakhir diputar dulu", playedRefs.isNotEmpty() || playedFirst) {
                                        toolsOpen = false
                                        playedFirst = !playedFirst
                                    }
                                    W7MenuItem(W7Ico.CODE, "Buka di Code", onOpenInCode != null && tg.any { it.isFile }) {
                                        toolsOpen = false
                                        val fn = onOpenInCode
                                        if (fn != null) {
                                            val texts = tg.filter { ExplorerList.looksText(it) }.take(10)
                                            if (texts.isEmpty()) say("Tidak ada berkas teks yang bisa dibuka di Code.") else fn(texts)
                                        }
                                    }
                                    W7MenuItem(W7Ico.SCRIPT, "Kirim ke Script execution", onSendToScripts != null && one != null && one.isFile) {
                                        toolsOpen = false
                                        val fn = onSendToScripts
                                        if (fn != null && one != null) scope.launch {
                                            val r = withContext(Dispatchers.IO) { fn(one) }
                                            say(r.second, r.first)
                                        }
                                    }
                                    W7MenuItem(W7Ico.OPEN_WITH, "Buka dengan...", onOpenWith != null && one?.isFile == true) {
                                        toolsOpen = false
                                        one?.let { onOpenWith?.invoke(it) }
                                    }
                                    W7MenuItem(W7Ico.SHARE, "Bagikan", onShare != null && tg.any { it.isFile }) {
                                        toolsOpen = false
                                        val fn = onShare
                                        if (fn != null) {
                                            val fs = tg.filter { it.isFile }
                                            if (fs.size < tg.size) say("Folder dilewati; hanya berkas yang dibagikan.")
                                            fn(fs)
                                        }
                                    }
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
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
                                    W7MenuItem(W7Ico.ZIP, "Kompres ZIP + kata sandi\u2026", tg.isNotEmpty()) {
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) sheet = W7Sheet.Zip(tg)
                                    }
                                    W7MenuItem(W7Ico.ZIP, "Kompres\u2026 (7z, tar.gz, ...)", tg.isNotEmpty()) { // [ARCHIVE2]
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) sheet = W7Sheet.Compress(tg)
                                    }
                                    W7MenuItem(
                                        W7Ico.UNZIP, "Ekstrak arsip",
                                        one != null && one.isFile && ArchiveRoute.supports(one.name)
                                    ) { // [ARCHIVE] ZIP tetap lewat jalur lama; format lain lewat ArchiveEngine
                                        toolsOpen = false
                                        if (one != null) {
                                            val d = dir
                                            if (one.extension.equals("zip", ignoreCase = true)) {
                                                runOp(
                                                    op = { ctl -> "Diekstrak ke: " + FileOps.unzip(one, d, ctl).name },
                                                    onFail = { if (it is FileOps.NeedPassword) { say(""); sheet = W7Sheet.Password(one, false) } }
                                                )
                                            } else {
                                                runOp(
                                                    op = { ctl -> ArchiveOps.extract(ctx, one, d, ExtractTarget.NAMED_FOLDER, ctl).describe() },
                                                    onFail = { if (it is FileOps.NeedPassword) { say(""); sheet = W7Sheet.ArchivePw(one, false) } }
                                                )
                                            }
                                        }
                                    }
                                    W7MenuItem(
                                        W7Ico.UNZIP, "Ekstrak pilihan\u2026",
                                        one != null && one.isFile && ArchiveRoute.supports(one.name)
                                    ) { // [ARCHIVE] pilih tujuan dan entri
                                        toolsOpen = false
                                        if (one != null) sheet = W7Sheet.Archive(one, null)
                                    }
                                    W7MenuItem(W7Ico.DELETE, "Hapus", tg.isNotEmpty()) {
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) sheet = W7Sheet.Delete(tg)
                                    }
                                    W7MenuItem(W7Ico.DELETE, "Hapus permanen\u2026", tg.isNotEmpty()) {
                                        toolsOpen = false
                                        if (tg.isNotEmpty()) sheet = W7Sheet.Delete(tg, true)
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
                                    W7MenuItem(W7Ico.COPY, "Salin path", onCopyPath != null && (one != null || tg.isEmpty())) {
                                        toolsOpen = false
                                        onCopyPath?.invoke(one ?: dir)
                                    }
                                    W7MenuItem(W7Ico.INFO, "Properti", true) {
                                        toolsOpen = false
                                        sheet = W7Sheet.Info(if (tg.isEmpty()) listOf(dir) else tg)
                                    }
                                }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxSize()) {
                    if (!portrait) {
                        W7NavPane(favs, vols, dir, Modifier.width(92.dp).fillMaxHeight(), onBin = if (browse) ({ showBin = true }) else null, recents = recents) { go(it) }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Win7.Hairline))
                    }
                    W7FileArea(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        granted = granted, entries = shown, key = key, asc = asc, selected = selected,
                        onSort = { k -> if (key == k) asc = !asc else { key = k; asc = true } },
                        multi = multi, picked = picked, recentRefs = playedRefs,
                        emptyNote = if (query.isNotBlank()) "Tidak ada hasil untuk \"" + query.trim() + "\"" else "Folder ini kosong.",
                        onOpen = { e ->
                            if (multi) picked = if (e.file.path in picked) picked - e.file.path else picked + e.file.path
                            else if (e.isDir) go(e.file)
                            else if (pickFolder) Unit
                            else if (selected?.file == e.file) { if (browse) smartOpen(e.file) else onPick(e.file) }
                            else { selected = e; fileName = e.name; message = "" }
                        },
                        onEnter = { e -> go(e.file) },
                        onAsk = { ask.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
                    )
                }
                if (portrait) {
                    Box(Modifier.align(Alignment.CenterStart).width(16.dp).fillMaxHeight().pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dx -> if (dx > 6f) navOpen = true }
                    })
                    if (navOpen) Box(Modifier.fillMaxSize().background(Color(0x66000000)).pointerInput(Unit) {
                        detectTapGestures { navOpen = false }
                    })
                    androidx.compose.animation.AnimatedVisibility(
                        visible = navOpen, modifier = Modifier.align(Alignment.CenterStart),
                        enter = androidx.compose.animation.slideInHorizontally { -it },
                        exit = androidx.compose.animation.slideOutHorizontally { -it }
                    ) {
                        W7NavPane(
                            favs, vols, dir,
                            Modifier.width(200.dp).fillMaxHeight().pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dx -> if (dx < -8f) navOpen = false }
                            },
                            onBin = if (browse) ({ navOpen = false; showBin = true }) else null, recents = recents
                        ) { navOpen = false; go(it) }
                    }
                }
                if (showSelectHelp) W7HelpPanel(
                    "Mode Pilih", "Pilih beberapa berkas atau folder sekaligus, lalu jalankan aksi dari menu Alat.",
                    listOf(
                        "Centang" to "Ketuk berkas atau folder untuk mencentang atau melepas centang.",
                        "Buka folder" to "Ketuk tanda \u203A di ujung baris untuk masuk ke folder.",
                        "Lintas folder" to "Centang tetap tersimpan saat Anda berpindah folder.",
                        "Pilih cepat" to "Semua mencentang seluruh isi folder ini; Kosongkan menghapus semua centang.",
                        "Selesai" to "Ketuk Selesai atau tombol Kembali untuk keluar dari Mode Pilih."
                    )
                ) { showSelectHelp = false } // [HELP-PANEL]
                if (message.isNotBlank()) W7Toast(
                    message, messageOk,
                    actionLabel = if (undo != null && !busy) "Urungkan" else null,
                    onAction = {
                        val undoItems = undo
                        if (undoItems != null) {
                            undo = null
                            message = ""
                            runOp(op = { _ -> "Dipulihkan: " + TrashBin.restore(undoItems, volDirs).size + " item" })
                        }
                    },
                    onDismiss = { message = "" },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    if (browse) {
                        val dkTarget = selected?.file ?: dir
                        val dkFiles = if (multi && picked.isNotEmpty()) picked.map { File(it) } else listOf(dkTarget)
                        val dkMedia = onPlay != null && dkFiles.isNotEmpty() && dkFiles.all { it.isFile && it.extension.lowercase() in W7_MEDIA }
                        val dkText = !dkMedia && onOpenInCode != null && dkFiles.size == 1 && dkFiles[0].isFile && ExplorerList.looksText(dkFiles[0])
                        val dkZip = !dkMedia && !dkText && dkFiles.size == 1 && dkFiles[0].isFile && dkFiles[0].extension.equals("zip", ignoreCase = true)
                        val dkArc = !dkMedia && !dkText && !dkZip && dkFiles.size == 1 && dkFiles[0].isFile && ArchiveRoute.supports(dkFiles[0].name) // [ARCHIVE]
                        W7BrowseDock(
                            files = dkFiles,
                            isDir = if (selected == null || multi) null else selected!!.isDir,
                            size = selected?.size ?: 0L,
                            modified = selected?.modified ?: 0L,
                            debian = if (dkFiles.size == 1) isVisibleInDebian?.invoke(dkFiles[0]) else null,
                            quickLabel = if (dkMedia) "Putar" else if (dkText) "Edit" else if (dkZip) "Buka ZIP" else if (dkArc) "Buka arsip" else "Properti",
                            insertEnabled = onInsertPaths != null,
                            terminalEnabled = onOpenTerminal != null,
                            onQuick = {
                                if (dkMedia) {
                                    val fn = onPlay
                                    if (fn != null) scope.launch {
                                        val ok = fn(dkFiles)
                                        say(if (ok) "Memutar di mpv: " + w7Names(dkFiles) else "Tidak ada media yang bisa diputar", ok)
                                    }
                                } else if (dkText) onOpenInCode?.invoke(dkFiles)
                                else if (dkZip) sheet = W7Sheet.ZipView(dkFiles[0])
                                else if (dkArc) sheet = W7Sheet.Archive(dkFiles[0], null) // [ARCHIVE]
                                else sheet = W7Sheet.Info(dkFiles)
                            },
                            onInsert = { onInsertPaths?.invoke(dkFiles) },
                            onTerminal = { onOpenTerminal?.invoke(dkTarget) },
                            onOpen = { if (dkTarget.isDirectory) go(dkTarget) else smartOpen(dkTarget) },
                            onCopyPath = { onCopyPath?.invoke(dkTarget); say("Path disalin", true) },
                            onHelp = if (multi) ({ showSelectHelp = true }) else null // [HELP-PANEL]
                        )
                    } else {
                    if (pickFolder) Text(
                        "Folder: " + dir.path, color = Win7.Ink, fontSize = 12.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                    ) else Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nama file:", color = Win7.Ink, fontSize = 12.sp, modifier = Modifier.width(66.dp))
                        BasicTextField(
                            value = fileName,
                            onValueChange = { fileName = it; message = "" },
                            singleLine = true,
                            textStyle = TextStyle(color = Win7.Ink, fontSize = 13.sp),
                            cursorBrush = SolidColor(Win7.Ink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { confirm() }),
                            modifier = Modifier.weight(1f).height(26.dp).tapToFocus().background(Win7.Input)
                                .border(1.dp, Win7.Field).padding(horizontal = 6.dp),
                            decorationBox = { inner -> Box(Modifier.fillMaxSize(), Alignment.CenterStart) { inner() } }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    if (!pickFolder) Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(66.dp))
                        Row(
                            Modifier.weight(1f).height(26.dp)
                                .background(Brush.verticalGradient(listOf(Win7.BarTop, Win7.BarBottom)))
                                .border(1.dp, Win7.Field).clickable { comboOpen = !comboOpen }
                                .padding(start = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(kind.label, color = Win7.Ink, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text("\u25BE", color = Win7.TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                    if (comboOpen) Column(
                        Modifier.padding(start = 66.dp).fillMaxWidth().background(Win7.Input).border(1.dp, Win7.Field)
                    ) {
                        W7Kind.values().forEach { k ->
                            Text(
                                k.label, color = Win7.Ink, fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .background(if (k == kind) Win7.SelFill else Win7.Input)
                                    .clickable { kind = k; comboOpen = false; selected = null }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    }
                    if (busy) Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(6.dp).background(Win7.Hairline)) {
                            val p = progress
                            Box(Modifier.fillMaxHeight().fillMaxWidth(p?.coerceIn(0f, 1f) ?: 0.25f).background(Win7.DefaultLine))
                        }
                        Text(
                            "Hentikan", color = Win7.NavHeader, fontSize = 12.sp,
                            modifier = Modifier.clickable { cancelFlag.set(true) }.padding(start = 10.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                    if (!browse) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        Win7Button(
                            if (pickFolder) "Pilih folder" else "Buka", isDefault = true,
                            enabled = if (pickFolder) granted else fileName.isNotBlank(),
                            onClick = { if (pickFolder) onPickFolder(dir) else confirm() }
                        )
                        if (!asScreen) {
                            Spacer(Modifier.width(8.dp))
                            Win7Button("Batal", onClick = onDismiss)
                        }
                    }
                }
            }
        }
    }
    if (asScreen) {
        BackHandler(onBack = backAction)
        content()
    } else Dialog(
        onDismissRequest = backAction,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) { content() }
}

/** Keterangan hasil operasi: melayang di atas daftar berkas, maks. 3 baris, hilang sendiri atau saat diketuk. */
@Composable
private fun W7Toast(
    text: String, ok: Boolean, actionLabel: String?, onAction: () -> Unit, onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)
    val tone = if (ok) Color(0xFF4CAF50) else Color(0xFFFF8A80)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(shape).background(Color(0xF2202830), shape).border(1.dp, tone.copy(alpha = 0.6f), shape)
            .clickable(onClick = onDismiss).padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("\u25CF ", color = tone, fontSize = 11.sp)
        Text(
            text, color = Color(0xFFEFF3F6), fontSize = 12.sp, maxLines = 3,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        if (actionLabel != null) W7Cmd(actionLabel, true, onAction)
    }
}

/** [HELP-PANEL] Tombol bantuan ringkas: lingkaran emas berisi "?" (hemat tempat di bilah alat). */
private val W7_HELP_YELLOW = Color(0xFFFFF176) // kuning muda

@Composable
internal fun W7HelpIcon(onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 5.dp)) {
        Box(
            Modifier.size(15.dp).border(1.dp, W7_HELP_YELLOW, RoundedCornerShape(8.dp)),
            Alignment.Center
        ) { Text("?", color = W7_HELP_YELLOW, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}

/** [HELP-PANEL] Layar bantuan: judul, ringkasan, lalu daftar istilah-penjelasan yang bisa digulir. */
@Composable
private fun W7HelpPanel(
    title: String, summary: String, items: List<Pair<String, String>>, onDismiss: () -> Unit
) {
    W7Panel(title, onDismiss) {
        Text(summary, color = Win7.TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
            items.forEach { (term, desc) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        term, color = MolinaTheme.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(84.dp)
                    )
                    Text(desc, color = Win7.Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Win7Button("Tutup", isDefault = true) { onDismiss() }
        }
    }
}

@Composable
private fun W7Cmd(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, color = if (enabled) Win7.NavHeader else Win7.TextDim, fontSize = 12.sp,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun W7NavPane(favs: List<W7Place>, vols: List<W7Place>, dir: File, modifier: Modifier, onBin: (() -> Unit)? = null, recents: List<File> = emptyList(), onGo: (File) -> Unit) {
    Column(modifier.background(Win7.NavBg).verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        W7NavHeader("Favorit", true)
        favs.forEach { p -> W7NavItem(p.label, dir == p.dir, { if (p.molina) Win7MolinaFolder(16.dp) else Win7Folder(16.dp) }) { onGo(p.dir) } }
        if (recents.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            W7NavHeader("Terakhir", false)
            recents.forEach { r -> W7NavItem(r.name.ifEmpty { "/" }, dir == r, { Win7Folder(16.dp) }) { onGo(r) } }
        }
        Spacer(Modifier.height(8.dp))
        W7NavHeader("Komputer", false)
        vols.forEach { p -> W7NavItem(p.label, dir == p.dir, { Win7Drive(16.dp) }) { onGo(p.dir) } }
        if (onBin != null) {
            Spacer(Modifier.height(8.dp))
            W7NavItem("Sampah", false, { Win7BinIcon(16.dp) }) { onBin() }
        }
    }
}

@Composable
private fun W7NavHeader(label: String, star: Boolean) {
    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (star) Text("\u2605 ", color = Color(0xFFE0A800), fontSize = 12.sp)
        Text(label, color = Win7.NavHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun W7NavItem(label: String, active: Boolean, icon: @Composable () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp)
            .then(if (active) Modifier.background(Win7.SelFill, shape).border(1.dp, Win7.SelLine, shape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(label, color = Win7.Ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun W7FileArea(
    modifier: Modifier,
    granted: Boolean,
    entries: List<W7Entry>?,
    key: ExplorerList.Key,
    asc: Boolean,
    selected: W7Entry?,
    multi: Boolean = false,
    picked: Set<String> = emptySet(),
    recentRefs: List<String> = emptyList(),
    emptyNote: String = "Folder ini kosong.",
    onSort: (ExplorerList.Key) -> Unit,
    onOpen: (W7Entry) -> Unit,
    onEnter: (W7Entry) -> Unit = {},
    onAsk: () -> Unit
) {
    BoxWithConstraints(modifier.background(Win7.Input)) {
        val wide = maxWidth >= 380.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(24.dp)
                    .background(Brush.verticalGradient(listOf(Win7.BarTop, Win7.BarBottom))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                W7HeadCell("Nama", ExplorerList.Key.NAME, key, asc, Modifier.weight(1f), onSort)
                if (wide) W7HeadCell("Diubah", ExplorerList.Key.DATE, key, asc, Modifier.width(112.dp), onSort)
                W7HeadCell("Ukuran", ExplorerList.Key.SIZE, key, asc, Modifier.width(76.dp), onSort)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
            when {
                !granted -> W7Note("Izin penyimpanan diperlukan untuk menjelajah berkas. Bila tombol tidak merespons, aktifkan izin Penyimpanan di Setelan Android.") {
                    Spacer(Modifier.height(10.dp))
                    Win7Button("Izinkan akses", onClick = onAsk)
                }
                entries == null -> W7Note("Folder ini tidak dapat dibuka.") {}
                entries.isEmpty() -> W7Note(emptyNote) {}
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(entries, key = { it.file.path }) { e ->
                        val on = e.file.path in picked
                        W7EntryRow(
                            e, if (multi) on else e == selected, wide, onOpen, if (multi) on else null, if (multi) onEnter else null,
                            played = if (e.isDir) -1 else recentRefs.indexOf(w7Norm(e.file.path))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun W7HeadCell(label: String, me: ExplorerList.Key, key: ExplorerList.Key, asc: Boolean, modifier: Modifier, onSort: (ExplorerList.Key) -> Unit) {
    Row(
        modifier.fillMaxHeight().clickable { onSort(me) }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Win7.NavHeader, fontSize = 12.sp, maxLines = 1)
        if (key == me) Text(if (asc) " \u25B2" else " \u25BC", color = Win7.TextDim, fontSize = 8.sp)
    }
}

@Composable
private fun W7Note(text: String, extra: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(text, color = Win7.TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
        extra()
    }
}

@Composable
private fun W7EntryRow(
    e: W7Entry, selected: Boolean, wide: Boolean, onOpen: (W7Entry) -> Unit,
    checked: Boolean? = null, onEnter: ((W7Entry) -> Unit)? = null, played: Int = -1
) {
    val shape = RoundedCornerShape(2.dp)
    val video = e.file.extension.lowercase() !in W7_AUDIO
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp)
            .then(
                if (selected) Modifier
                    .background(Brush.verticalGradient(listOf(Color(0xFF16324D), Color(0xFF0F2438))), shape)
                    .border(1.dp, Win7.SelLine, shape)
                else Modifier
            )
            .clickable { onOpen(e) }
            .heightIn(min = if (wide) 26.dp else 40.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (checked != null) {
            Text(if (checked) "[x]" else "[  ]", color = Win7.NavHeader, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
        }
        if (e.isDir) Win7Folder(20.dp) else W7FileIcon(e.file.extension, 22.dp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, color = Win7.Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (played >= 0) W7PlayedChip(played)
            }
            if (e.rel != null) Text(e.rel, color = Win7.TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            else if (!wide) Text(w7Date(e.modified), color = Win7.TextDim, fontSize = 10.sp, maxLines = 1)
        }
        if (wide) Text(w7Date(e.modified), color = Win7.TextDim, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(112.dp))
        Text(
            if (e.isDir) "" else w7Size(e.size), color = Win7.TextDim, fontSize = 12.sp, maxLines = 1,
            textAlign = TextAlign.End, modifier = Modifier.width(if (wide) 76.dp else 68.dp)
        )
        if (checked != null && e.isDir && onEnter != null) {
            Box(
                Modifier.width(40.dp).heightIn(min = 32.dp).clickable { onEnter(e) },
                contentAlignment = Alignment.Center
            ) { Text("\u203A", color = Win7.NavHeader, fontSize = 22.sp) }
        }
    }
}

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
            W7Ico.PLAY -> {
                val tri = Path()
                tri.moveTo(5f * u, 3f * u); tri.lineTo(13f * u, 8f * u); tri.lineTo(5f * u, 13f * u); tri.close()
                drawPath(tri, c, style = Fill)
            }
            W7Ico.CODE -> {
                line(5.5f, 4f, 2f, 8f); line(2f, 8f, 5.5f, 12f)
                line(10.5f, 4f, 14f, 8f); line(14f, 8f, 10.5f, 12f)
                line(9f, 3f, 7f, 13f)
            }
            W7Ico.SCRIPT -> {
                box(1.5f, 2.5f, 13f, 11f)
                line(4.5f, 6f, 7f, 8.2f); line(7f, 8.2f, 4.5f, 10.5f); line(8.5f, 10.5f, 11.5f, 10.5f)
            }
            W7Ico.OPEN_WITH -> {
                line(2f, 4f, 2f, 13f); line(2f, 13f, 11f, 13f); line(11f, 13f, 11f, 9f); line(2f, 4f, 7f, 4f)
                line(8f, 8f, 14f, 2f); line(10f, 2f, 14f, 2f); line(14f, 2f, 14f, 6f)
            }
            W7Ico.SHARE -> {
                drawCircle(c, radius = 1.8f * u, center = Offset(12f * u, 3.5f * u), style = st)
                drawCircle(c, radius = 1.8f * u, center = Offset(4f * u, 8f * u), style = st)
                drawCircle(c, radius = 1.8f * u, center = Offset(12f * u, 12.5f * u), style = st)
                line(5.6f, 7f, 10.4f, 4.5f); line(5.6f, 9f, 10.4f, 11.5f)
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
internal fun W7Panel(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
internal fun W7SecretField( // [ARCHIVE2] internal: dipakai ArchiveCompressDialog
    value: String, onChange: (String) -> Unit, show: Boolean,
    modifier: Modifier, onDone: () -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = Win7.Ink, fontSize = 14.sp),
        cursorBrush = SolidColor(Win7.Ink),
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth().height(32.dp).background(Win7.Input)
            .border(1.dp, Win7.Field).padding(horizontal = 8.dp),
        decorationBox = { inner -> Box(Modifier.fillMaxSize(), Alignment.CenterStart) { inner() } }
    )
}

/** [ZIP-AES] Dialog kata sandi: [confirmField] meminta ketik dua kali (membuat ZIP), tanpa itu satu kali (mengekstrak). */
@Composable
private fun W7PasswordDialog(
    title: String, okLabel: String, confirmField: Boolean, note: String,
    onCancel: () -> Unit, onOk: (String) -> Unit
) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    val valid = if (confirmField) pw.length >= 4 && pw == pw2 else pw.isNotEmpty()
    val hint = when {
        !confirmField -> ""
        pw.isNotEmpty() && pw.length < 4 -> "Minimal 4 karakter."
        pw2.isNotEmpty() && pw != pw2 -> "Kata sandi tidak sama."
        else -> ""
    }
    W7Panel(title, onCancel) {
        Text(note, color = Win7.TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text("Kata sandi", color = Win7.Ink, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        W7SecretField(pw, { pw = it }, show, Modifier.focusRequester(focus)) { if (valid) onOk(pw) }
        if (confirmField) {
            Spacer(Modifier.height(8.dp))
            Text("Ulangi kata sandi", color = Win7.Ink, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            W7SecretField(pw2, { pw2 = it }, show, Modifier) { if (valid) onOk(pw) }
        }
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(hint, color = Color(0xFFFF8A80), fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clickable { show = !show }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (show) "[x]" else "[  ]", color = Win7.Ink, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text("Tampilkan kata sandi", color = Win7.TextDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Win7Button(okLabel, isDefault = true, enabled = valid) { onOk(pw) }
            Spacer(Modifier.width(8.dp))
            Win7Button("Batal", onClick = onCancel)
        }
    }
}

@Composable
private fun W7ConfirmDialog(files: List<File>, permanent: Boolean, onCancel: () -> Unit, onOk: () -> Unit) {
    val hasDir = files.any { it.isDirectory }
    val verb = if (permanent) "Hapus permanen" else "Pindahkan ke Recycle Bin"
    W7Panel(if (permanent) "Hapus permanen" else "Recycle Bin", onCancel) {
        Text(
            if (files.size == 1) verb + ": \"" + files[0].name + "\"" + (if (hasDir) " beserta seluruh isinya?" else "?")
            else verb + ": " + files.size + " item terpilih" + (if (hasDir) " beserta isi folder?" else "?"),
            color = Win7.Ink, fontSize = 13.sp
        )
        if (files.size > 1) {
            Spacer(Modifier.height(6.dp))
            files.take(4).forEach { Text("\u2022 " + it.name, color = Win7.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (files.size > 4) Text("\u2022 \u2026 dan " + (files.size - 4) + " lainnya", color = Win7.TextDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (permanent) Text("Penghapusan permanen tidak dapat dibatalkan.", color = Color(0xFFFF8A80), fontSize = 12.sp)
        else Text("Bisa dipulihkan dari Recycle Bin selama " + TrashBin.RETAIN_DAYS + " hari.", color = Win7.TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Win7Button(if (permanent) "Hapus permanen" else "Pindahkan", isDefault = true) { onOk() }
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
    val shaCtx = LocalContext.current
    val shaScope = rememberCoroutineScope()
    val shaCancel = remember(files) { AtomicBoolean(false) }
    var sha by remember(files) { mutableStateOf<String?>(null) }
    var shaBusy by remember(files) { mutableStateOf(false) }
    var shaPct by remember(files) { mutableStateOf(0f) }
    var shaErr by remember(files) { mutableStateOf("") }
    DisposableEffect(files) { onDispose { shaCancel.set(true) } }
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
                if (!p.isDir && !p.symlink && W7Thumb.hasThumb(first)) {
                    val media by produceState<List<Pair<String, String>>?>(null, first.path) { value = W7MediaInfo.read(first) }
                    media?.forEach { W7InfoRow(it.first, it.second) }
                }
                if (!p.isDir && !p.symlink && W7Thumb.isVideo(first)) {
                    val side by produceState<List<Pair<String, String>>?>(null, first.path) { value = W7MediaInfo.sidecar(first) }
                    side?.forEach { W7InfoRow(it.first, it.second) }
                }
                if (!p.isDir && !p.symlink) {
                    val h = sha
                    if (h != null) {
                        W7InfoRow("SHA-256", h)
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                            Win7Button("Salin hash") {
                                val cm = shaCtx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("sha256", h))
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            if (shaErr.isNotEmpty()) Text(shaErr, color = Color(0xFFFF8A80), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Win7Button(
                                if (shaBusy) "Menghitung " + (shaPct * 100).toInt() + "%" else "Hitung SHA-256",
                                enabled = !shaBusy
                            ) {
                                shaBusy = true
                                shaErr = ""
                                shaScope.launch {
                                    val r = withContext(Dispatchers.IO) {
                                        runCatching { FileOps.sha256(first, FileOps.Ctl(shaCancel) { pc -> shaPct = pc }) }
                                    }
                                    shaBusy = false
                                    r.onSuccess { sha = it }.onFailure { shaErr = it.message ?: "Gagal menghitung" }
                                }
                            }
                        }
                    }
                }
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

/**
 * [KB-TAP] Kolom teks tidak bisa menerima fokus otomatis (fokus awal jendela dialog); fokus baru
 * diizinkan setelah disentuh pengguna, dan dikunci lagi saat fokus lepas. Keyboard tidak muncul sendiri.
 */
private fun Modifier.tapToFocus(): Modifier = composed {
    var armed by remember { mutableStateOf(false) }
    this
        .focusProperties { canFocus = armed }
        .onFocusChanged { if (!it.isFocused) armed = false }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                armed = true
            }
        }
}

@Composable
private fun W7SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    Row(
        modifier.height(26.dp).background(Win7.Input).border(1.dp, Win7.Field).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Win7.Ink, fontSize = 12.sp),
            cursorBrush = SolidColor(Win7.Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f).tapToFocus(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), Alignment.CenterStart) {
                    if (value.isEmpty()) Text("Cari nama\u2026", color = Win7.TextDim, fontSize = 12.sp, maxLines = 1)
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
            Text("\u00D7", color = Win7.TextDim, fontSize = 14.sp, modifier = Modifier.clickable { onChange("") }.padding(start = 6.dp))
        }
    }
}

@Composable
private fun W7ChoiceItem(mark: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(mark, color = Win7.NavHeader, fontSize = 12.sp, modifier = Modifier.width(26.dp))
        Text(label, color = Win7.Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Jenis error + 8 baris tumpukan pertama, untuk Settings > Log (membantu mendiagnosis kegagalan di perangkat). */
private fun w7Trace(t: Throwable): String =
    t.javaClass.name + ": " + (t.message ?: "-") + "\n" + t.stackTrace.take(8).joinToString("\n") { "  at " + it }

/** [ZIP-VIEW] Isi ZIP tanpa mengekstrak; tombol Ekstrak memakai alur yang sama dengan menu Alat (termasuk kata sandi). */
private val W7_ZIP_IMG = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

/** Entri yang sedang dibuka dari ZIP: salinan sementara + cap waktu/ukuran saat dibuka untuk mendeteksi suntingan. */
private class W7ZipOpened(val file: File, var lm: Long, var len: Long)

/** Permintaan kata sandi tertunda: kind 0 = buka, 1 = buka di Terminal, 2 = simpan ke ZIP, 3 = tambah ke ZIP [ZIP-EDIT]. */
private class W7ZipAsk(val kind: Int, val name: String, val wrong: Boolean)

private class W7ZipChild(val full: String, val label: String, val isDir: Boolean, val size: Long)

/** Isi satu level folder di dalam ZIP (folder tanpa entri eksplisit tetap diturunkan dari path berkas). */
private fun w7ZipChildren(entries: List<FileOps.ZipEntryInfo>, cwd: String): List<W7ZipChild> {
    val dirs = LinkedHashSet<String>()
    val files = ArrayList<W7ZipChild>()
    for (en in entries) {
        val n = en.name.trimEnd('/')
        if (n.isEmpty() || !n.startsWith(cwd)) continue
        val rest = n.substring(cwd.length)
        if (rest.isEmpty()) continue
        val slash = rest.indexOf('/')
        if (slash >= 0) dirs.add(rest.substring(0, slash))
        else if (en.isDir) dirs.add(rest)
        else files.add(W7ZipChild(n, rest, false, en.size))
    }
    return dirs.sorted().map { W7ZipChild(cwd + it + "/", it, true, 0L) } + files.sortedBy { it.label.lowercase() }
}

@Composable
private fun W7ZipDialog(
    zip: File, onClose: () -> Unit, onOpenFile: (File) -> Unit,
    onOpenTerminal: ((File) -> Boolean)?, clipFiles: () -> List<File>, onExtract: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Folder files aplikasi (terlihat dari Debian bila /sdcard di-bind); cadangan: cache internal
    val cacheRoot = remember { ctx.getExternalFilesDir(null) ?: ctx.cacheDir }
    var listing by remember(zip) { mutableStateOf<FileOps.ZipListing?>(null) }
    var err by remember(zip) { mutableStateOf<String?>(null) }
    var cwd by remember(zip) { mutableStateOf("") }
    var picked by remember(zip) { mutableStateOf<String?>(null) }
    var busy by remember(zip) { mutableStateOf(false) }
    var status by remember(zip) { mutableStateOf("") }
    var statusOk by remember(zip) { mutableStateOf(true) }
    var ask by remember(zip) { mutableStateOf<W7ZipAsk?>(null) }
    var zipPw by remember(zip) { mutableStateOf<String?>(null) }
    var closeArmed by remember(zip) { mutableStateOf(false) }
    val opened = remember(zip) { mutableStateMapOf<String, W7ZipOpened>() }
    var dirty by remember(zip) { mutableStateOf<List<String>>(emptyList()) }
    var delArm by remember(zip) { mutableStateOf<String?>(null) } // [ZIP-EDIT] target hapus yang menunggu konfirmasi
    LaunchedEffect(zip) {
        val r = withContext(Dispatchers.IO) { runCatching { FileOps.listZip(zip) } }
        listing = r.getOrNull()
        err = r.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }
    }
    // Deteksi suntingan di salinan sementara (mis. dari Code) tiap detik selama dialog terbuka
    LaunchedEffect(zip) {
        while (true) {
            dirty = opened.entries.filter { it.value.file.lastModified() != it.value.lm || it.value.file.length() != it.value.len }.map { it.key }
            delay(1000)
        }
    }
    fun reload() {
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { FileOps.listZip(zip) } }
            r.getOrNull()?.let { listing = it }
        }
    }
    // [ZIP-OPEN] salin satu entri ke folder sementara lalu buka: media -> mpv, teks -> Code, atau Terminal
    fun openEntry(name: String, pw0: String?, toTerminal: Boolean = false) {
        if (busy) return
        val pw = pw0 ?: zipPw
        busy = true
        statusOk = true
        closeArmed = false
        status = "Menyiapkan " + name.substringAfterLast('/') + "\u2026"
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { FileOps.extractEntry(zip, name, cacheRoot, pw) { !isActive } } }
            busy = false
            val f = r.getOrNull()
            val ex = r.exceptionOrNull()
            when {
                f != null -> {
                    if (pw != null) zipPw = pw
                    if (opened[name] == null) opened[name] = W7ZipOpened(f, f.lastModified(), f.length())
                    if (toTerminal) {
                        val ok = onOpenTerminal?.invoke(f) ?: false
                        statusOk = ok
                        status = if (ok) "Terminal dibuka di folder salinan." else "Lokasi salinan tidak terlihat dari Debian."
                    } else {
                        status = "Dibuka dari salinan sementara. Setelah mengedit, tekan Simpan untuk menulis ke ZIP."
                        onOpenFile(f)
                    }
                }
                ex is FileOps.NeedPassword -> { status = ""; ask = W7ZipAsk(if (toTerminal) 1 else 0, name, false) }
                ex is FileOps.WrongPassword -> { status = ""; ask = W7ZipAsk(if (toTerminal) 1 else 0, name, true) }
                else -> { statusOk = false; status = ex?.message ?: "Gagal membuka entri" }
            }
        }
    }
    // [ZIP-SAVE] tulis kembali entri yang disunting ke ZIP (cadangan otomatis di folder zipbak)
    fun saveBack(pw0: String?) {
        if (busy) return
        val items = dirty.mapNotNull { n -> opened[n]?.let { Pair(n, it) } }
        if (items.isEmpty()) {
            statusOk = true
            status = "Tidak ada perubahan untuk disimpan."
            return
        }
        val pw = pw0 ?: zipPw
        busy = true
        statusOk = true
        closeArmed = false
        status = "Menyimpan " + items.size + " berkas ke ZIP\u2026"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { for (p in items) FileOps.replaceEntry(zip, p.first, p.second.file, pw, File(cacheRoot, "zipbak")) }
            }
            busy = false
            val ex = r.exceptionOrNull()
            when {
                ex == null -> {
                    if (pw != null) zipPw = pw
                    for (p in items) { p.second.lm = p.second.file.lastModified(); p.second.len = p.second.file.length() }
                    status = "Tersimpan ke ZIP (" + items.size + " berkas). Cadangan ZIP tersimpan di folder zipbak."
                    reload()
                }
                ex is FileOps.NeedPassword -> { status = ""; ask = W7ZipAsk(2, "", false) }
                ex is FileOps.WrongPassword -> { status = ""; ask = W7ZipAsk(2, "", true) }
                else -> { statusOk = false; status = ex?.message ?: "Gagal menyimpan ke ZIP" }
            }
        }
    }
    // [ZIP-EDIT] tempel berkas/folder dari klip Penjelajah ke folder ZIP yang sedang dibuka
    fun addFromClip(pw0: String?) {
        if (busy) return
        val src = clipFiles()
        if (src.isEmpty()) {
            statusOk = false
            status = "Klip kosong. Di Penjelajah, pilih berkas lalu Alat > Salin, kemudian buka ZIP ini."
            return
        }
        val pw = pw0 ?: zipPw
        val dest = cwd
        busy = true
        statusOk = true
        closeArmed = false
        delArm = null
        status = "Menambahkan " + src.size + " item ke ZIP\u2026"
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { FileOps.addEntries(zip, src, dest, pw, File(cacheRoot, "zipbak")) } }
            busy = false
            val ex = r.exceptionOrNull()
            when {
                ex == null -> {
                    if (pw != null) zipPw = pw
                    status = "Ditambahkan " + (r.getOrNull() ?: 0) + " item ke /" + dest + ". Cadangan ZIP ada di folder zipbak."
                    reload()
                }
                ex is FileOps.NeedPassword -> { status = ""; ask = W7ZipAsk(3, "", false) }
                ex is FileOps.WrongPassword -> { status = ""; ask = W7ZipAsk(3, "", true) }
                else -> { statusOk = false; status = ex?.message ?: "Gagal menambah ke ZIP" }
            }
        }
    }
    // [ZIP-EDIT] hapus berkas terpilih (atau folder yang sedang dibuka) dari ZIP; ketuk dua kali untuk konfirmasi
    fun deleteTarget() {
        if (busy) return
        val target = picked ?: cwd.takeIf { it.isNotEmpty() } ?: return
        if (delArm != target) {
            delArm = target
            statusOk = false
            status = "Hapus /" + target + " dari ZIP? Tekan Hapus sekali lagi untuk konfirmasi (cadangan dibuat otomatis)."
            return
        }
        delArm = null
        busy = true
        statusOk = true
        closeArmed = false
        status = "Menghapus dari ZIP\u2026"
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { FileOps.deleteEntries(zip, listOf(target), File(cacheRoot, "zipbak")) } }
            busy = false
            val ex = r.exceptionOrNull()
            if (ex == null) {
                status = "Dihapus " + (r.getOrNull() ?: 0) + " entri dari ZIP. Cadangan ada di folder zipbak."
                opened.keys.filter { it == target || (target.endsWith("/") && it.startsWith(target)) }.forEach { opened.remove(it) }
                if (target.endsWith("/")) cwd = target.trimEnd('/').substringBeforeLast('/', "").let { if (it.isEmpty()) "" else it + "/" }
                picked = null
                reload()
            } else { statusOk = false; status = ex.message ?: "Gagal menghapus dari ZIP" }
        }
    }
    fun tryClose() {
        if (dirty.isNotEmpty() && !closeArmed) {
            closeArmed = true
            statusOk = false
            status = "Ada perubahan belum disimpan ke ZIP. Tekan Tutup sekali lagi untuk membuangnya."
        } else onClose()
    }
    // [ZIP-PREVIEW] pratinjau gambar langsung dari ZIP, tanpa ekstrak ke disk
    val pk0 = picked
    val isImg = pk0 != null && pk0.substringAfterLast('.', "").lowercase() in W7_ZIP_IMG
    val preview: ImageBitmap? by produceState<ImageBitmap?>(null, pk0, zipPw) {
        value = null
        if (isImg && pk0 != null) {
            delay(200)
            value = W7Thumb.zipImage(zip, pk0, zipPw)?.asImageBitmap()
        }
    }
    var showHelp by remember { mutableStateOf(false) } // [ZIP-LAYOUT]
    W7Panel("ZIP: " + zip.name, { tryClose() }) {
        val l = listing
        val e = err
        when {
            e != null -> Text(e, color = Color(0xFFFF8A80), fontSize = 12.sp)
            l == null -> Text("Membaca isi ZIP\u2026", color = Win7.TextDim, fontSize = 12.sp)
            else -> {
                Text(
                    l.files.toString() + " berkas, " + l.folders + " folder \u2022 " + FileOps.readable(l.totalSize),
                    color = Win7.Ink, fontSize = 12.sp
                )
                if (l.encrypted) {
                    Spacer(Modifier.height(4.dp))
                    Text("Terenkripsi AES: kata sandi diminta saat membuka entri, menyimpan, atau mengekstrak.", color = Color(0xFFE0A800), fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text("/" + cwd, color = Win7.NavHeader, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                val children = remember(l, cwd) { w7ZipChildren(l.entries, cwd) }
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).background(Win7.Input)
                        .border(1.dp, Win7.Field).verticalScroll(rememberScrollState())
                ) {
                    if (cwd.isNotEmpty()) Row(
                        Modifier.fillMaxWidth().clickable(enabled = !busy) {
                            cwd = cwd.trimEnd('/').substringBeforeLast('/', "").let { if (it.isEmpty()) "" else it + "/" }
                            picked = null
                        }.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\u2190  ..", color = Win7.NavHeader, fontSize = 12.sp)
                    }
                    children.forEach { ch ->
                        val on = !ch.isDir && ch.full == picked
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (on) Modifier.background(Win7.SelFill) else Modifier)
                                .clickable(enabled = !busy) {
                                    delArm = null
                                    if (ch.isDir) { cwd = ch.full; picked = null }
                                    else if (picked == ch.full) openEntry(ch.full, null)
                                    else picked = ch.full
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (ch.isDir) Win7Folder(16.dp) else W7FileIcon(ch.label.substringAfterLast('.', ""), 18.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                ch.label, color = if (ch.isDir) Win7.NavHeader else Win7.Ink, fontSize = 12.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            if (opened[ch.full] != null && ch.full in dirty) Text("diubah", color = MolinaTheme.Gold, fontSize = 10.sp, modifier = Modifier.padding(start = 6.dp))
                            if (!ch.isDir) Text(FileOps.readable(ch.size), color = Win7.TextDim, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (children.isEmpty()) Text("Folder ini kosong.", color = Win7.TextDim, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                    if (l.truncated) Text(
                        "\u2026 hanya " + FileOps.ZIP_LIST_MAX + " entri pertama ditampilkan",
                        color = Win7.TextDim, fontSize = 11.sp, modifier = Modifier.padding(6.dp)
                    )
                }
                val pv = preview
                if (pv != null) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(110.dp).background(Win7.Input).border(1.dp, Win7.Field), Alignment.Center) {
                        Image(pv, null, Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { // [ZIP-LAYOUT]
                    Text("Ketuk berkas untuk memilih, ketuk lagi untuk membuka.", color = Win7.TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(
                        "Bantuan", color = MolinaTheme.Gold, fontSize = 12.sp,
                        modifier = Modifier.clickable { showHelp = true }.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
                    )
                }
            }
        }
        if (dirty.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Diubah: " + dirty.joinToString(", ") { it.substringAfterLast('/') } + ". Tekan Simpan untuk menulis ke ZIP.", color = MolinaTheme.Gold, fontSize = 11.sp)
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(status, color = if (statusOk) Win7.NavHeader else Color(0xFFFF8A80), fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        // [ZIP-LAYOUT-BTN] dua baris, tiap tombol berbobot sama: label tidak lagi terpotong
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Win7Button("Buka", Modifier.weight(1f), isDefault = true, enabled = picked != null && !busy, compact = true) { picked?.let { openEntry(it, null) } }
            Win7Button("Terminal", Modifier.weight(1f), enabled = picked != null && !busy && onOpenTerminal != null, compact = true) { picked?.let { openEntry(it, null, true) } }
            Win7Button("Simpan", Modifier.weight(1f), enabled = dirty.isNotEmpty() && !busy, accent = MolinaTheme.Gold, compact = true) { saveBack(null) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Win7Button("Tempel", Modifier.weight(1f), enabled = listing != null && !busy, compact = true) { addFromClip(null) }
            Win7Button(if (delArm != null) "Yakin?" else "Hapus", Modifier.weight(1f), enabled = listing != null && !busy && (picked != null || cwd.isNotEmpty()), accent = if (delArm != null) Color(0xFFFF8A80) else null, compact = true) { deleteTarget() }
            Win7Button("Ekstrak", Modifier.weight(1f), enabled = listing != null && !busy, compact = true) { onExtract() }
            Win7Button("Tutup", Modifier.weight(1f), compact = true) { tryClose() }
        }
    }
    if (showHelp) W7HelpPanel(
        "Cara memakai ZIP", "Telusuri, buka, dan ubah isi ZIP tanpa mengekstraknya lebih dulu.",
        listOf(
            "Folder" to "Ketuk untuk masuk.",
            "Berkas" to "Ketuk untuk memilih, ketuk lagi atau tekan Buka (media ke mpv, teks ke Code).",
            "Terminal" to "Membuka folder salinan berkas terpilih.",
            "Tempel" to "Menambah berkas yang disalin di Penjelajah ke folder ini.",
            "Hapus" to "Membuang berkas terpilih, atau folder ini bila tak ada yang dipilih.",
            "Ukuran" to "Yang tampil adalah ukuran tanpa kompresi."
        )
    ) { showHelp = false } // [HELP-PANEL]
    val a = ask
    if (a != null) W7PasswordDialog(
        title = if (a.kind == 3) "Tambah ke ZIP" else if (a.kind == 2) "Simpan ke ZIP" else "Buka dari ZIP",
        okLabel = if (a.kind == 3) "Tambah" else if (a.kind == 2) "Simpan" else "Buka", confirmField = false,
        note = if (a.wrong) "Kata sandi salah. Coba lagi." else "ZIP ini dilindungi kata sandi.",
        onCancel = { ask = null }
    ) { pw ->
        ask = null
        when (a.kind) {
            3 -> addFromClip(pw)
            2 -> saveBack(pw)
            1 -> openEntry(a.name, pw, true)
            else -> openEntry(a.name, pw)
        }
    }
}

private fun w7Norm(p: String): String =
    if (p.startsWith("/sdcard/")) "/storage/emulated/0/" + p.removePrefix("/sdcard/") else p

/** Chip riwayat putar: 0 = "Terakhir diputar" (aksen Cinema), lainnya = "Diputar" (redup). */
@Composable
private fun W7PlayedChip(rank: Int) {
    val latest = rank == 0
    val tone = if (latest) MolinaTheme.Cinema else Win7.TextDim
    Text(
        if (latest) "Terakhir diputar" else "Diputar", color = tone, fontSize = 9.sp, maxLines = 1,
        modifier = Modifier.padding(start = 6.dp)
            .border(1.dp, tone.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}
