package com.molina.studio.download

import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import com.molina.core.common.DownloadCommand
import com.molina.core.common.DownloadFormat
import com.molina.core.common.DownloadPaths
import com.molina.core.common.DownloadUrls
import com.molina.core.common.DownloadItemState
import com.molina.core.common.DownloadItemStatus
import com.molina.core.common.DownloadMode
import com.molina.core.common.DownloadReducer
import com.molina.core.common.DownloadRequest
import com.molina.core.common.LogRedact
import com.molina.studio.cinema.CinemaActions
import com.molina.studio.cinema.Win7OpenDialog
import com.molina.studio.settings.MolinaExplorer
import com.molina.studio.ui.MolinaTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Tab Unduhan: tempel URL (satu ketukan dari clipboard), pilih mode video/audio, pantau progres per item
 * (bar emas), lalu putar di Molina Player atau buka folder hasil di Penjelajah. Sumber kebenaran: [DownloadRunner].
 * Hasil sesi terakhir tetap tampil sampai dikosongkan.
 */
@Composable
fun UnduhanScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val run by DownloadRunner.state.collectAsState()
    val sess = run.session

    // Formulir ada di DownloadForm (bukan rememberSaveable): tab hanya dikomposisi saat dipilih.
    var urls by remember { DownloadForm.urls }
    var video by remember { DownloadForm.video }
    var height by remember { DownloadForm.height }
    var videoExt by remember { DownloadForm.videoExt }
    var audioFmt by remember { DownloadForm.audioFmt }
    var bitrate by remember { DownloadForm.bitrate }
    var error by remember { mutableStateOf<String?>(null) }
    var openDir by remember { mutableStateOf<File?>(null) }

    // Riwayat sesi lama + Ulangi. Riwayat baru terbuka bila diketuk; satu kartu terbuka pada satu waktu.
    val history by DownloadHistoryStore.entries.collectAsState()
    var historyOpen by remember { mutableStateOf(false) }
    var openAt by remember { mutableStateOf<Long?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val retryUrls = if (run.running) emptyList<String>() else DownloadRunner.retryUrls()

    /** Hasil start/ulangi: galat -> Toast; sukses -> gulir ke atas supaya progres sesi baru terlihat. */
    fun afterStart(msg: String?) {
        if (msg != null) Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        else scope.launch { listState.animateScrollToItem(0) }
    }

    val doneFiles = sess.items.flatMap { it.files }
    val hasResult = run.running || sess.items.isNotEmpty() || sess.fatalError != null || run.exitCode != null
    val count = DownloadUrls.split(urls).size

    // URL kiriman dari layar lain (kartu hasil pencarian Cinema): digabung ke kolom, duplikat dilewati.
    val pending = DownloadNav.pendingUrls
    LaunchedEffect(pending) {
        if (pending.isNotEmpty()) {
            val r = DownloadUrls.merge(urls, pending)
            urls = r.text
            error = null
            DownloadNav.pendingUrls = emptyList()
            val note = buildString {
                append(if (r.added > 0) "${r.added} URL ditambahkan" else "URL sudah ada di daftar")
                if (r.skipped > 0 && r.added > 0) append(", ${r.skipped} sudah ada")
                if (r.dropped > 0) append(", ${r.dropped} melewati batas ${DownloadCommand.MAX_URLS}")
                if (DownloadRunner.isRunning()) append(". Unduhan masih berjalan; mulai setelah selesai.")
            }
            Toast.makeText(ctx, note, Toast.LENGTH_SHORT).show()
        }
    }

    openDir?.let { dir ->
        Win7OpenDialog(
            onDismiss = { openDir = null },
            browse = true,
            startDir = dir,
            onCopyPath = { f -> MolinaExplorer.copyPath(ctx, f) },
            onShare = { files -> MolinaExplorer.share(ctx, files) },
            onInsertPaths = { fs -> MolinaExplorer.insertPaths(ctx, fs) },
            isVisibleInDebian = { f -> MolinaExplorer.visibleInDebian(f) },
            onPlay = { files -> MolinaExplorer.play(ctx, files) },
        )
    }

    if (confirmClear) {
        ClearHistoryDialog(
            onConfirm = { DownloadHistoryStore.clear(); openAt = null; confirmClear = false },
            onDismiss = { confirmClear = false },
        )
    }

    LazyColumn(
        modifier.fillMaxSize().background(MolinaTheme.Background).padding(horizontal = 16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text("Unduhan", color = MolinaTheme.OnBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.padding(top = 6.dp).fillMaxWidth().height(2.dp).background(
                        Brush.horizontalGradient(listOf(MolinaTheme.Gold, Color.Transparent))
                    )
                )
            }
        }
        item {
            OutlinedTextField(
                value = urls,
                onValueChange = { urls = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                enabled = !run.running,
                label = { Text("URL (satu per baris)") },
                minLines = 2,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MolinaTheme.Gold,
                    unfocusedBorderColor = MolinaTheme.CardLine,
                    focusedLabelColor = MolinaTheme.Gold,
                    cursorColor = MolinaTheme.Gold,
                ),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val clip = pasteText(ctx)
                        if (clip.isBlank()) {
                            Toast.makeText(ctx, "Clipboard kosong.", Toast.LENGTH_SHORT).show()
                        } else {
                            urls = if (urls.isBlank()) clip.trim() else urls.trimEnd() + "\n" + clip.trim()
                            error = null
                        }
                    },
                    enabled = !run.running,
                    border = BorderStroke(1.dp, MolinaTheme.Gold),
                ) { Text("Tempel", color = MolinaTheme.Gold) }
                if (urls.isNotEmpty() && !run.running) {
                    TextButton(onClick = { urls = ""; error = null }) { Text("Hapus", color = MolinaTheme.Muted) }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Video", video, !run.running) { video = true }
                    Chip("Audio", !video, !run.running) { video = false }
                }
                if (video) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Terbaik", 1080 to "1080p", 720 to "720p", 480 to "480p").forEach { (h, l) ->
                            Chip(l, height == h, !run.running) { height = h }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DownloadCommand.VIDEO_EXTS.sorted().forEach { e ->
                            Chip(e.uppercase(Locale.ROOT), videoExt == e, !run.running) { videoExt = e }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DownloadCommand.AUDIO_FORMATS.sorted().forEach { f ->
                            Chip(f.uppercase(Locale.ROOT), audioFmt == f, !run.running) { audioFmt = f }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DownloadCommand.BITRATES.sortedBy { it.dropLast(1).toInt() }.forEach { b ->
                            Chip(b, bitrate == b, !run.running) { bitrate = b }
                        }
                    }
                }
            }
        }
        item {
            error?.let { Text(it, color = MolinaTheme.Bad, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (run.running) {
                    Button(
                        onClick = { DownloadRunner.cancel() },
                        colors = ButtonDefaults.buttonColors(containerColor = MolinaTheme.Bad, contentColor = Color.Black),
                    ) { Text("Batalkan") }
                } else {
                    Button(
                        onClick = {
                            val req = DownloadRequest(
                                urls = urls.split(Regex("\\s+")),
                                mode = if (video) DownloadMode.VIDEO else DownloadMode.AUDIO,
                                height = height,
                                videoExt = videoExt,
                                audioFormat = audioFmt,
                                bitrate = bitrate,
                            )
                            val msg = DownloadRunner.start(ctx.applicationContext, req)
                            error = msg
                            if (msg == null) {
                                urls = ""
                                DownloadPrefs.save(ctx.applicationContext, DownloadForm.choices())
                            }
                        },
                        enabled = count > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MolinaTheme.Gold, contentColor = Color.Black),
                    ) { Text(if (count > 0) "Mulai unduh ($count)" else "Mulai unduh") }
                }
                if (!run.running && hasResult) {
                    TextButton(onClick = { DownloadRunner.clear(); error = null }) {
                        Text("Kosongkan hasil", color = MolinaTheme.Muted)
                    }
                }
            }
        }
        if (hasResult) {
            item { SessionSummary(run.running, sess.ok, sess.failed, sess.cancelled, sess.finished, sess.items.size) }
            sess.fatalError?.let { fe -> item { Text(fe, color = MolinaTheme.Bad, fontSize = 13.sp) } }
            if (sess.fatalError == null && !sess.finished && !run.running && run.exitCode != null && run.exitCode != 0) {
                items(run.stderrTail.takeLast(5)) { l ->
                    Text(l, color = MolinaTheme.Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            items(sess.warnings.takeLast(3)) { w -> Text(w, color = MolinaTheme.Warn, fontSize = 12.sp) }
            items(sess.items, key = { it.index }) { item ->
                ItemCard(
                    ctx, item,
                    onRetry = if (!run.running && item.status == DownloadItemStatus.FAILED && item.url.isNotEmpty()) {
                        { afterStart(DownloadRunner.retry(ctx.applicationContext, item.url)) }
                    } else null,
                )
            }
            if (!run.running && doneFiles.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = {
                            val dir = folderOf(doneFiles.first().path)
                            if (dir == null) Toast.makeText(ctx, "Folder hasil ada di dalam Debian dan belum bisa dibuka dari sini.", Toast.LENGTH_LONG).show()
                            else openDir = dir
                        },
                        border = BorderStroke(1.dp, MolinaTheme.Gold),
                    ) { Text("Buka folder", color = MolinaTheme.Gold) }
                }
            }
            if (!run.running && retryUrls.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { afterStart(DownloadRunner.retry(ctx.applicationContext)) },
                        border = BorderStroke(1.dp, MolinaTheme.Gold),
                    ) { Text("Ulangi yang gagal (${retryUrls.size})", color = MolinaTheme.Gold) }
                }
            }
        }
        // Sesi terbaru sudah tampil di atas (hasil sesi) bila hasilnya belum dikosongkan: jangan digandakan.
        historySection(
            entries = if (hasResult && !run.running) history.drop(1) else history,
            running = run.running,
            open = historyOpen,
            onToggle = { historyOpen = !historyOpen },
            openAt = openAt,
            onToggleEntry = { at -> openAt = if (openAt == at) null else at },
            onRetry = { e -> afterStart(DownloadRunner.start(ctx.applicationContext, e.retryRequest())) },
            onDelete = { e ->
                if (openAt == e.at) openAt = null
                DownloadHistoryStore.remove(e.at)
            },
            onClearAll = { confirmClear = true },
        )
        item { Box(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MolinaTheme.Gold.copy(alpha = 0.22f),
            selectedLabelColor = MolinaTheme.Gold,
            labelColor = MolinaTheme.Muted,
        ),
    )
}

@Composable
private fun SessionSummary(running: Boolean, ok: Int, failed: Int, cancelled: Boolean, finished: Boolean, total: Int) {
    val text = when {
        running && total == 0 -> "Menyiapkan unduhan…"
        running -> "Berjalan: $total item"
        cancelled -> "Dibatalkan: $ok berhasil, $failed gagal. Berkas .part tetap ada; URL yang sama dilanjutkan."
        finished -> "Selesai: $ok berhasil, $failed gagal"
        else -> "Sesi berhenti"
    }
    Text(text, color = MolinaTheme.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun ItemCard(ctx: Context, item: DownloadItemState, onRetry: (() -> Unit)? = null) {
    val pct = DownloadReducer.overallPercent(item)
    val title = item.title ?: LogRedact.target(item.url).ifEmpty { "Item ${item.index}" }.take(70)
    Column(
        Modifier.fillMaxWidth()
            .background(MolinaTheme.CardBg, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "${item.index}. $title", color = MolinaTheme.OnBackground, fontSize = 14.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        val color = when (item.status) {
            DownloadItemStatus.DONE -> MolinaTheme.Good
            DownloadItemStatus.FAILED -> MolinaTheme.Bad
            else -> MolinaTheme.Gold
        }
        if (item.status != DownloadItemStatus.FAILED) {
            if (pct == null || item.status == DownloadItemStatus.PROCESSING) {
                LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp), color = color, trackColor = MolinaTheme.CardLine)
            } else {
                LinearProgressIndicator(
                    progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = color,
                    trackColor = MolinaTheme.CardLine,
                )
            }
        }
        Text(statusLine(item, pct), color = if (item.status == DownloadItemStatus.FAILED) MolinaTheme.Bad else MolinaTheme.Muted, fontSize = 12.sp)
        if (item.status == DownloadItemStatus.FAILED && onRetry != null) {
            TextButton(onClick = onRetry) { Text("Ulangi", color = MolinaTheme.Gold) }
        }
        if (item.status == DownloadItemStatus.DONE) {
            item.files.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        f.name + "  " + fmtSize(f.sizeBytes), color = MolinaTheme.OnBackground, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { playFile(ctx, f.path) }) { Text("Putar", color = MolinaTheme.Cinema) }
                }
            }
        }
    }
}

private fun statusLine(item: DownloadItemState, pct: Double?): String = when (item.status) {
    DownloadItemStatus.QUEUED -> "Antre"
    DownloadItemStatus.RUNNING -> {
        val p = item.streams.values.firstOrNull { it.speedBps != null }
        buildString {
            append(pct?.let { String.format(Locale.ROOT, "%.0f%%", it) } ?: "Mengunduh")
            p?.speedBps?.let { append(" · ").append(fmtSize(it)).append("/dtk") }
            p?.etaSec?.let { append(" · sisa ").append(fmtEta(it)) }
        }
    }
    DownloadItemStatus.PROCESSING -> item.stage?.let { "Memproses: $it" } ?: "Memproses"
    DownloadItemStatus.DONE -> if (item.existed) "Selesai (berkas sudah ada)" else "Selesai"
    DownloadItemStatus.FAILED -> item.errorMsg ?: "Gagal"
}

private fun fmtSize(b: Long): String = DownloadFormat.size(b)

private fun fmtEta(s: Long): String = DownloadFormat.eta(s)

private fun pasteText(ctx: Context): String {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = cm.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(ctx)?.toString().orEmpty()
}

/** Path Debian (`/sdcard/...`) -> File Android. null bila di luar /sdcard (mis. cadangan di dalam rootfs Debian). */
private fun androidFile(debianPath: String): File? {
    val base = Environment.getExternalStorageDirectory().absolutePath
    return DownloadPaths.toAndroid(debianPath, base)?.let { File(it) }
}

private fun folderOf(debianPath: String): File? =
    androidFile(debianPath)?.parentFile?.takeIf { it.isDirectory }

private fun playFile(ctx: Context, debianPath: String) {
    val f = androidFile(debianPath)
    when {
        f == null -> Toast.makeText(ctx, "Berkas ada di dalam Debian dan belum bisa diputar dari sini.", Toast.LENGTH_LONG).show()
        !f.isFile -> Toast.makeText(ctx, "Berkas tidak ditemukan (mungkin sudah dipindah atau dihapus).", Toast.LENGTH_LONG).show()
        else -> if (!CinemaActions.playPath(ctx, f.absolutePath)) {
            Toast.makeText(ctx, "Molina Player tidak bisa dibuka.", Toast.LENGTH_SHORT).show()
        }
    }
}
