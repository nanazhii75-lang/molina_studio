package com.molina.studio.log

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val entries by LogBus.entries.collectAsState()
    var filter by rememberSaveable { mutableStateOf("") }
    val shown = remember(entries, filter) {
        if (filter.isEmpty()) entries else entries.filter { it.source == filter }
    }
    val listState = rememberLazyListState()
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
                        format(e),
                        color = levelColor(e.level),
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
    LogLevel.Error -> MaterialTheme.colorScheme.error
    LogLevel.Warn -> MaterialTheme.colorScheme.primary
    LogLevel.Info -> MaterialTheme.colorScheme.onSurface
    LogLevel.Debug -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
}

private fun format(e: LogEntry): String {
    val n = if (e.count > 1) "  \u00D7${e.count}" else ""
    return "${TIME_FMT.format(Instant.ofEpochMilli(e.time))} ${e.level.code} " +
        "${e.source}/${e.tag}: ${e.message}$n"
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
