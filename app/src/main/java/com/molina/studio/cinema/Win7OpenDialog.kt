package com.molina.studio.cinema

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
import androidx.core.content.ContextCompat
import com.molina.studio.terminal.TerminalSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class W7Entry(val file: File, val isDir: Boolean, val size: Long, val modified: Long) {
    val name: String get() = file.name
}

private enum class W7Sort { NAME, DATE, SIZE }
private enum class W7Kind(val label: String) { MEDIA("Media (video dan audio)"), ALL("Semua file") }
private class W7Place(val label: String, val dir: File)

private val W7_ID = Locale("id", "ID")
private val W7_AUDIO = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "wma", "ac3", "dts", "mka")
private val W7_MEDIA = W7_AUDIO + setOf(
    "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts", "m2ts", "mts", "flv", "wmv",
    "mpg", "mpeg", "ogv", "vob", "rmvb", "m3u", "m3u8", "pls", "cue"
)

private fun w7HasStorage(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

private fun w7Favorites(browse: Boolean = false): List<W7Place> = listOfNotNull(
    if (browse) W7Place("Molina Studio", File(Environment.getExternalStorageDirectory(), TerminalSessions.WORKSPACE_DIR)) else null,
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

private fun w7Load(dir: File, kind: W7Kind, key: W7Sort, asc: Boolean): List<W7Entry>? {
    val all = dir.listFiles() ?: return null
    val items = all.filter { !it.name.startsWith(".") }
        .filter { it.isDirectory || kind == W7Kind.ALL || it.extension.lowercase() in W7_MEDIA }
        .map { W7Entry(it, it.isDirectory, if (it.isDirectory) 0L else it.length(), it.lastModified()) }
    fun sorted(l: List<W7Entry>, k: W7Sort): List<W7Entry> = when (k) {
        W7Sort.NAME -> l.sortedBy { it.name.lowercase() }
        W7Sort.DATE -> l.sortedBy { it.modified }
        W7Sort.SIZE -> l.sortedBy { it.size }
    }
    val dirs = sorted(items.filter { it.isDir }, if (key == W7Sort.SIZE) W7Sort.NAME else key)
    val files = sorted(items.filter { !it.isDir }, key)
    return (if (asc) dirs else dirs.reversed()) + (if (asc) files else files.reversed())
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
    onChangeDir: ((File) -> Unit)? = null,
    isVisibleInDebian: ((File) -> Boolean)? = null,
    pickFolder: Boolean = false,
    onPickFolder: (File) -> Unit = {}
) {
    val ctx = LocalContext.current
    val favs = remember { w7Favorites(browse) }
    val vols = remember { w7Drives() }
    var granted by remember { mutableStateOf(w7HasStorage(ctx)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var dir by remember { mutableStateOf(vols.first().dir) }
    var kind by remember { mutableStateOf(if (browse) W7Kind.ALL else W7Kind.MEDIA) }
    var key by remember { mutableStateOf(W7Sort.NAME) }
    var asc by remember { mutableStateOf(true) }
    var tick by remember { mutableStateOf(0) }
    var entries by remember { mutableStateOf<List<W7Entry>?>(emptyList()) }
    var selected by remember { mutableStateOf<W7Entry?>(null) }
    var fileName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var comboOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
    LaunchedEffect(dir, kind, key, asc, tick, granted) {
        if (!granted) return@LaunchedEffect
        entries = withContext(Dispatchers.IO) { w7Load(dir, kind, key, asc) }
    }

    val roots = vols.map { it.dir.path }
    val canUp = dir.path !in roots && dir.parentFile != null
    fun go(to: File) { dir = to; selected = null; fileName = ""; message = "" }
    fun goUp(): Boolean {
        val parent = dir.parentFile
        if (!canUp || parent == null) return false
        go(parent)
        return true
    }
    fun confirm() {
        val typed = fileName.trim()
        if (typed.isEmpty()) return
        val target = selected?.takeIf { it.name == typed }?.file
            ?: File(if (typed.startsWith("/")) typed else dir.path + "/" + typed)
        when {
            target.isDirectory -> go(target)
            target.isFile -> onPick(target)
            else -> message = "Berkas tidak ditemukan. Periksa nama berkas lalu coba lagi."
        }
    }

    val frame = RoundedCornerShape(8.dp)
    Dialog(
        onDismissRequest = { if (!goUp()) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
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
                }
                Row(
                    Modifier.fillMaxWidth().height(30.dp)
                        .background(Brush.verticalGradient(listOf(Win7.BarTop, Win7.BarBottom))),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    W7Cmd("Naik", canUp) { goUp() }
                    W7Cmd("Segarkan", true) { tick++ }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    W7NavPane(favs, vols, dir, Modifier.width(92.dp).fillMaxHeight()) { go(it) }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Win7.Hairline))
                    W7FileArea(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        granted = granted, entries = entries, key = key, asc = asc, selected = selected,
                        onSort = { k -> if (key == k) asc = !asc else { key = k; asc = true } },
                        onOpen = { e ->
                            if (e.isDir) go(e.file)
                            else if (pickFolder) Unit
                            else if (selected?.file == e.file) onPick(e.file)
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
                            modifier = Modifier.weight(1f).height(26.dp).background(Win7.Input)
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
                            Win7Button("Ke Terminal", modifier = Modifier.weight(1f), enabled = onSendToTerminal != null, onClick = { onSendToTerminal?.invoke(target) })
                            Win7Button("cd", modifier = Modifier.weight(1f), enabled = onChangeDir != null && target.isDirectory, onClick = { onChangeDir?.invoke(target) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, color = Color(0xFFFF8A80), fontSize = 11.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
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
        favs.forEach { p -> W7NavItem(p.label, dir == p.dir, { Win7Folder(16.dp) }) { onGo(p.dir) } }
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
    key: W7Sort,
    asc: Boolean,
    selected: W7Entry?,
    onSort: (W7Sort) -> Unit,
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
                W7HeadCell("Nama", W7Sort.NAME, key, asc, Modifier.weight(1f), onSort)
                if (wide) W7HeadCell("Diubah", W7Sort.DATE, key, asc, Modifier.width(112.dp), onSort)
                W7HeadCell("Ukuran", W7Sort.SIZE, key, asc, Modifier.width(76.dp), onSort)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Win7.Hairline))
            when {
                !granted -> W7Note("Izin penyimpanan diperlukan untuk menjelajah berkas. Bila tombol tidak merespons, aktifkan izin Penyimpanan di Setelan Android.") {
                    Spacer(Modifier.height(10.dp))
                    Win7Button("Izinkan akses", onClick = onAsk)
                }
                entries == null -> W7Note("Folder ini tidak dapat dibuka.") {}
                entries.isEmpty() -> W7Note("Folder ini kosong.") {}
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(entries, key = { it.file.path }) { e -> W7EntryRow(e, e == selected, wide, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun W7HeadCell(label: String, me: W7Sort, key: W7Sort, asc: Boolean, modifier: Modifier, onSort: (W7Sort) -> Unit) {
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
private fun W7EntryRow(e: W7Entry, selected: Boolean, wide: Boolean, onOpen: (W7Entry) -> Unit) {
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
        if (e.isDir) Win7Folder(20.dp) else Win7File(video, 20.dp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, color = Win7.Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!wide) Text(w7Date(e.modified), color = Win7.TextDim, fontSize = 10.sp, maxLines = 1)
        }
        if (wide) Text(w7Date(e.modified), color = Win7.TextDim, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(112.dp))
        Text(
            if (e.isDir) "" else w7Size(e.size), color = Win7.TextDim, fontSize = 12.sp, maxLines = 1,
            textAlign = TextAlign.End, modifier = Modifier.width(if (wide) 76.dp else 68.dp)
        )
    }
}
