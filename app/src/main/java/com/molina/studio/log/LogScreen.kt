package com.molina.studio.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.molina.core.common.LogLevel
import com.molina.studio.ui.MolinaTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FILTERS = listOf(
    "" to "Semua",
    "cinema" to "Cinema",
    "terminal" to "Terminal",
    "sistem" to "Sistem",
    "logcat" to "Logcat",
    "crash" to "Crash"
)

// Log pemutar (MpvLog memakai sumber "mpv") ditampilkan di filter Cinema.
private val CINEMA_SOURCES = setOf("cinema", "mpv")

private fun matchesFilter(source: String, filter: String): Boolean =
    if (filter == "cinema") source in CINEMA_SOURCES else source == filter

private val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val entries by LogBus.entries.collectAsState()
    var filter by rememberSaveable { mutableStateOf("") }
    val shown = remember(entries, filter) {
        if (filter.isEmpty()) entries else entries.filter { matchesFilter(it.source, filter) }
    }
    val listState = rememberLazyListState()
    val ctx = LocalContext.current
    val lastCount = shown.lastOrNull()?.count ?: 0

    // Ikuti entri terbaru hanya bila pengguna sudah berada di dasar daftar.
    LaunchedEffect(shown.size, lastCount) {
        if (shown.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (lastVisible >= info.totalItemsCount - 2) listState.scrollToItem(shown.lastIndex)
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            FILTERS.forEach { (key, label) ->
                LogButton(label, filter == key) { filter = key }
            }
            LogButton("Hapus", false) { LogBus.clear() }
            LogButton("Uji", false) { sendTest() }
        }
        Row(Modifier.fillMaxWidth()) {
            LogButton("Salin", false) { copyLogs(ctx, shown) }
            LogButton("Bagikan", false) { shareLogs(ctx, shown) }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Belum ada log.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            ) {
                items(shown, key = { it.id }) { e ->
                    Text(
                        rowText(e, levelColor(e.level)),
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = {},
                            onLongClick = { copyText(ctx, format(e), "1 baris") }
                        ),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.LogButton(label: String, active: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.Error -> MolinaTheme.Bad    // merah lembut: mis. "Galat mpv"
    LogLevel.Warn -> MolinaTheme.Warn    // kuning: mis. "Berakhir sebelum dimuat"
    LogLevel.Info -> MaterialTheme.colorScheme.onSurface
    LogLevel.Debug -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
}

/** Baris log; baris dari Cinema diberi ikon putar ungu merek di depannya. */
private fun rowText(e: LogEntry, color: Color) = buildAnnotatedString {
    if (e.source in CINEMA_SOURCES) {
        withStyle(SpanStyle(color = MolinaTheme.Cinema)) { append("\u25B6 ") }
    }
    withStyle(SpanStyle(color = color)) { append(format(e)) }
}

private fun format(e: LogEntry): String {
    val n = if (e.count > 1) "  \u00D7${e.count}" else ""
    return "${TIME_FMT.format(Instant.ofEpochMilli(e.time))} ${e.level.code} " +
        "${e.source}/${e.tag}: ${e.message}$n"
}

private const val MAX_COPY_LINES = 2000
private const val MAX_SHARE_LINES = 800

private fun joinLogs(list: List<LogEntry>, max: Int): String =
    list.takeLast(max).joinToString("\n") { format(it) }

private fun copyText(ctx: Context, text: String, what: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Molina Log", text))
    Toast.makeText(ctx, "Tersalin: $what", Toast.LENGTH_SHORT).show()
}

private fun copyLogs(ctx: Context, list: List<LogEntry>) {
    if (list.isEmpty()) {
        Toast.makeText(ctx, "Belum ada log", Toast.LENGTH_SHORT).show()
        return
    }
    copyText(ctx, joinLogs(list, MAX_COPY_LINES), "${minOf(list.size, MAX_COPY_LINES)} baris")
}

private fun shareLogs(ctx: Context, list: List<LogEntry>) {
    if (list.isEmpty()) {
        Toast.makeText(ctx, "Belum ada log", Toast.LENGTH_SHORT).show()
        return
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, joinLogs(list, MAX_SHARE_LINES))
    }
    ctx.startActivity(Intent.createChooser(send, "Bagikan log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Kirim satu baris uji lewat socket IPC (jalur penuh klien -> server -> bus -> UI). */
private fun sendTest() {
    Thread({
        val ok = LogClient.send(
            "terminal", LogLevel.Info, "uji",
            "Pesan uji lewat socket IPC @${LogServer.SOCKET_NAME}"
        )
        if (!ok) LogBus.log("sistem", LogLevel.Error, "LogClient", "Gagal mengirim lewat socket")
    }, "molina-log-test").start()
}
