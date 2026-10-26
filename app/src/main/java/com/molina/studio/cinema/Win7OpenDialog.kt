package com.molina.studio.cinema

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.molina.studio.terminal.TerminalSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    class Password(val zip: File, val wrong: Boolean) : W7Sheet()
    class Info(val files: List<File>) : W7Sheet()
    class Delete(val files: List<File>) : W7Sheet()
    object NewDir : W7Sheet()
}
private enum class W7Ico { PLAY, CODE, SCRIPT, COPY, CUT, PASTE, RENAME, ZIP, UNZIP, DELETE, NEWDIR, STAR, STAR_OUT, INFO }

private fun w7Names(l: List<File>): String = if (l.size == 1) l[0].name else l.size.toString() + " item"
private fun w7StemEnd(name: String, dir: Boolean): Int =
    if (dir) name.length else name.lastIndexOf('.').let { if (it > 0) it else name.length }

private val W7_ID = Locale("id", "ID")
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
    onCopyPath: ((File) -> Unit)? = null,
    onSendToTerminal: ((File) -> Unit)? = null,
    onRunInTerminal: ((File) -> Unit)? = null,
    onInsertPaths: ((List<File>) -> Unit)? = null,
    onChangeDir: ((File) -> Unit)? = null,
    sendEnter: Boolean = false,
    onSendEnterChange: ((Boolean) -> Unit)? = null,
    isVisibleInDebian: ((File) -> Boolean)? = null,
    pickFolder: Boolean = false,
    onPickFolder: (File) -> Unit = {},
    onPlay: (suspend (List<File>) -> Boolean)? = null,
    onOpenInCode: ((List<File>) -> Unit)? = null,
    onSendToScripts: ((File) -> Pair<Boolean, String>)? = null
) {
    val ctx = LocalContext.current
    var favTick by remember { mutableStateOf(0) } // [FILEOPS-1]
    val favs = remember(favTick) {
        val base = w7Favorites(browse)
        val mine = if (browse) ExplorerFavorites.load(ctx).filter { f -> base.none { it.dir.path == f.path } } else emptyList()
        base + mine.map { W7Place(it.name.ifEmpty { it.path }, it) }
    }
    val vols = remember { w7Drives() }
    var granted by remember { mutableStateOf(w7HasStorage(ctx)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var dir by remember { mutableStateOf(vols.first().dir) }
    var kind by remember { mutableStateOf(if (browse) W7Kind.ALL else W7Kind.MEDIA) }
    // [EXPLORER-SEARCH] pencarian nama + sortir yang diingat
    val savedSort = remember { ExplorerPrefs.load(ctx) }
    var key by remember { mutableStateOf(savedSort.key) }
    var asc by remember { mutableStateOf(savedSort.asc) }
    var foldersFirst by remember { mutableStateOf(savedSort.foldersFirst) }
    var showHidden by remember { mutableStateOf(savedSort.showHidden) }
    var deepSearch by remember { mutableStateOf(savedSort.deep) }
    var query by remember { mutableStateOf("") }
    var sortOpen by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }
    var entries by remember { mutableStateOf<List<W7Entry>?>(emptyList()) }
    var selected by remember { mutableStateOf<W7Entry?>(null) }
    var fileName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
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
    var picked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var progress by remember { mutableStateOf<Float?>(null) }
    val cancelFlag = remember { AtomicBoolean(false) }
    val guard = remember {
        vols.map { it.dir.absolutePath }.toSet() +
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
        say("Memproses\u2026", true)
        scope.launch {
            val ctl = FileOps.Ctl(cancelFlag) { p -> progress = p }
            val r = withContext(Dispatchers.IO) { runCatching { op(ctl) } }
            busy = false
            progress = null
            r.onSuccess { say(it, true); onDone(); selected = null; picked = emptySet(); fileName = ""; tick++ }
                .onFailure { say(it.message ?: "Operasi gagal."); tick++; onFail(it) }
        }
    }

    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
    LaunchedEffect(key, asc, foldersFirst, showHidden, deepSearch) {
        ExplorerPrefs.save(ctx, ExplorerPrefs.Data(key, asc, foldersFirst, showHidden, deepSearch))
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

    val roots = vols.map { it.dir.path }
    val canUp = dir.path !in roots && dir.parentFile != null
    fun go(to: File) { dir = to; selected = null; picked = emptySet(); fileName = ""; message = ""; query = "" }
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
            code != null && ExplorerList.looksText(f) -> code(listOf(f))
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
        is W7Sheet.Delete -> W7ConfirmDialog(s.files, onCancel = { sheet = null }) {
            sheet = null
            runOp(op = { ctl -> "Dihapus: " + FileOps.deleteAll(s.files, guard, ctl) + " item" })
        }
        is W7Sheet.Info -> W7InfoDialog(s.files, if (s.files.size == 1) isVisibleInDebian?.invoke(s.files[0]) else null) { sheet = null }
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
        null -> Unit
    }

    val frame = RoundedCornerShape(8.dp)
    Dialog(
        onDismissRequest = { if (multi) { multi = false; picked = emptySet() } else if (!goUp()) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
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
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 24.dp)
                .clip(frame)
                .background(Brush.verticalGradient(listOf(Win7.FrameTop, Win7.FrameBottom)), frame)
                .border(1.dp, Win7.FrameLine, frame)
                .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.Top) {
                Row(Modifier.weight(1f).padding(top = 5.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Win7Folder(16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(if (browse) "Penjelajah Molina" else if (pickFolder) "Buka folder" else "Buka", color = Win7.Ink, fontSize = 13.sp)
                }
                Win7Close(onDismiss)
            }
            Column(Modifier.weight(1f).fillMaxWidth().border(1.dp, Win7.FrameLine).background(Win7.Face)) {
                Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Win7BackOrb(canUp) { goUp() }
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
                    W7Cmd("Naik", canUp) { goUp() }
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
                                    W7MenuItem(
                                        W7Ico.UNZIP, "Ekstrak ZIP",
                                        one != null && one.isFile && one.extension.equals("zip", ignoreCase = true)
                                    ) {
                                        toolsOpen = false
                                        if (one != null) {
                                            val d = dir
                                            runOp(
                                                op = { ctl -> "Diekstrak ke: " + FileOps.unzip(one, d, ctl).name },
                                                onFail = { if (it is FileOps.NeedPassword) { say(""); sheet = W7Sheet.Password(one, false) } }
                                            )
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
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    W7NavPane(favs, vols, dir, Modifier.width(92.dp).fillMaxHeight()) { go(it) }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Win7.Hairline))
                    W7FileArea(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        granted = granted, entries = entries, key = key, asc = asc, selected = selected,
                        onSort = { k -> if (key == k) asc = !asc else { key = k; asc = true } },
                        multi = multi, picked = picked,
                        emptyNote = if (query.isNotBlank()) "Tidak ada hasil untuk \"" + query.trim() + "\"" else "Folder ini kosong.",
                        onOpen = { e ->
                            if (multi) picked = if (e.file.path in picked) picked - e.file.path else picked + e.file.path
                            else if (e.isDir) go(e.file)
                            else if (pickFolder) Unit
                            else if (selected?.file == e.file) { if (browse) smartOpen(e.file) else onPick(e.file) }
                            else { selected = e; fileName = e.name; message = "" }
                        },
                        onAsk = { ask.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
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
                    if (browse) {
                        val target = selected?.file ?: dir
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                target.path, color = Win7.TextDim, fontSize = 11.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            if (isVisibleInDebian != null) {
                                val vis = isVisibleInDebian.invoke(target)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (vis) "\u25CF Terlihat di Debian" else "\u25CB Di luar Debian",
                                    color = if (vis) Color(0xFF4CAF50) else Color(0xFFE0A800),
                                    fontSize = 10.sp, maxLines = 1
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Win7Button("Salin path", modifier = Modifier.weight(1f), enabled = onCopyPath != null, onClick = { onCopyPath?.invoke(target) })
                            val insertList = if (multi && picked.isNotEmpty()) picked.map { File(it) } else listOf(target)
                            if (target.isFile && !(multi && picked.size > 1)) {
                                Win7Button("Jalankan", modifier = Modifier.weight(1f), enabled = onRunInTerminal != null, onClick = { onRunInTerminal?.invoke(target) })
                            } else {
                                Win7Button(
                                    "Sisipkan" + (if (insertList.size > 1) " (" + insertList.size + ")" else ""),
                                    modifier = Modifier.weight(1f), enabled = onInsertPaths != null, onClick = { onInsertPaths?.invoke(insertList) }
                                )
                            }
                            Win7Button("cd", modifier = Modifier.weight(1f), enabled = onChangeDir != null && target.isDirectory, onClick = { onChangeDir?.invoke(target) })
                        }
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, color = if (messageOk) Color(0xFF4CAF50) else Color(0xFFFF8A80), fontSize = 11.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        Win7Button(
                            if (pickFolder) "Pilih folder" else "Buka", isDefault = true,
                            enabled = if (pickFolder) granted else fileName.isNotBlank(),
                            onClick = { if (pickFolder) onPickFolder(dir) else confirm() }
                        )
                        Spacer(Modifier.width(8.dp))
                        Win7Button("Batal", onClick = onDismiss)
                    }
                }
            }
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
private fun W7NavPane(favs: List<W7Place>, vols: List<W7Place>, dir: File, modifier: Modifier, onGo: (File) -> Unit) {
    Column(modifier.background(Win7.NavBg).verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        W7NavHeader("Favorit", true)
        favs.forEach { p -> W7NavItem(p.label, dir == p.dir, { if (p.molina) Win7MolinaFolder(16.dp) else Win7Folder(16.dp) }) { onGo(p.dir) } }
        Spacer(Modifier.height(8.dp))
        W7NavHeader("Komputer", false)
        vols.forEach { p -> W7NavItem(p.label, dir == p.dir, { Win7Drive(16.dp) }) { onGo(p.dir) } }
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
    emptyNote: String = "Folder ini kosong.",
    onSort: (ExplorerList.Key) -> Unit,
    onOpen: (W7Entry) -> Unit,
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
                        W7EntryRow(e, if (multi) on else e == selected, wide, onOpen, if (multi) on else null)
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
private fun W7EntryRow(e: W7Entry, selected: Boolean, wide: Boolean, onOpen: (W7Entry) -> Unit, checked: Boolean? = null) {
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
        if (e.isDir) Win7Folder(20.dp) else Win7File(video, 20.dp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, color = Win7.Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (e.rel != null) Text(e.rel, color = Win7.TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            else if (!wide) Text(w7Date(e.modified), color = Win7.TextDim, fontSize = 10.sp, maxLines = 1)
        }
        if (wide) Text(w7Date(e.modified), color = Win7.TextDim, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(112.dp))
        Text(
            if (e.isDir) "" else w7Size(e.size), color = Win7.TextDim, fontSize = 12.sp, maxLines = 1,
            textAlign = TextAlign.End, modifier = Modifier.width(if (wide) 76.dp else 68.dp)
        )
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
private fun W7SecretField(
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
