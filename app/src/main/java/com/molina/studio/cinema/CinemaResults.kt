package com.molina.studio.cinema

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.molina.core.common.DownloadCommand
import com.molina.core.common.RelativeTime
import com.molina.core.common.StreamSearchItem
import com.molina.studio.R
import com.molina.studio.download.DownloadNav
import com.molina.studio.ui.MolinaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Palet yang sama dengan playlist player (Molina Cinema)
private val DialogBg = Color(0xFF0F0F0F)
private val ChannelNumber = Color(0xFFC8E600)
private val SelectedTop = Color(0xFF3D8BFF)
private val SelectedBottom = Color(0xFF0F4FC4)
private val SelectedLine = Color(0xFF8DBBFF)
private val RowTop = Color(0xFF1C2430)
private val RowBottom = Color(0xFF121821)
private val RowLine = Color(0x26FFFFFF)
private val Accent = Color(0xFF4DA3FF)

/** Prev/Next memindahkan sorotan satu 'halaman' (seperti CH+/CH- cepat di remote TV). */
private const val PAGE_SIZE = 10

private class ResultEntry(val number: Int, val item: StreamSearchItem)

private fun clockNow(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

/**
 * Dialog hasil pencarian bergaya daftar channel TV digital: nomor 001.., baris terpilih kotak biru,
 * penghitung + jam di header, Cari (teks = saring, angka = lompat ke nomor, Enter = putar), Info, Tutup.
 */
@Composable
internal fun SearchResultsDialog(
    results: List<StreamSearchItem>,
    onPick: (StreamSearchItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val entries = remember(results) { results.mapIndexed { i, r -> ResultEntry(i + 1, r) } }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(1) }
    var clock by remember { mutableStateOf(clockNow()) }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) { clock = clockNow(); delay(15_000) }
    }

    val q = query.trim()
    val numeric = q.isNotEmpty() && q.all { it.isDigit() }
    val visible = if (q.isEmpty() || numeric) entries
        else entries.filter { it.item.title.contains(q, ignoreCase = true) }

    LaunchedEffect(q) {
        if (numeric) {
            val idx = visible.indexOfFirst { it.number == q.toIntOrNull() }
            if (idx >= 0) { selected = visible[idx].number; listState.scrollToItem(idx) }
        }
    }
    LaunchedEffect(searchOpen) { if (searchOpen) focus.requestFocus() }

    fun page(delta: Int) {
        if (visible.isEmpty()) return
        val cur = visible.indexOfFirst { it.number == selected }.coerceAtLeast(0)
        val idx = (cur + delta).coerceIn(0, visible.lastIndex)
        selected = visible[idx].number
        scope.launch { listState.scrollToItem(idx) }
    }

    fun pickFromQuery() {
        val t = if (numeric) visible.firstOrNull { it.number == q.toIntOrNull() } else visible.singleOrNull()
        if (t != null) onPick(t.item)
    }

    // Kirim URL ke tab Unduhan (tidak langsung mengunduh: mode/kualitas dipilih di sana).
    fun download(items: List<StreamSearchItem>) {
        val all = items.map { it.pageUrl }.filter { it.startsWith("http://") || it.startsWith("https://") }
        if (all.isEmpty()) {
            Toast.makeText(ctx, "Tidak ada tautan yang bisa diunduh.", Toast.LENGTH_SHORT).show()
            return
        }
        val urls = all.take(DownloadCommand.MAX_URLS)
        DownloadNav.request(urls)
        val msg = if (urls.size == 1) "Dikirim ke Unduhan" else "${urls.size} tautan dikirim ke Unduhan" +
            (if (all.size > urls.size) " (maks ${DownloadCommand.MAX_URLS})" else "")
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // [CINEMA-RESULTS-FRAME] margin tepi 24dp, lebar maks 480dp, bingkai tipis bergradasi emas
        val frame = RoundedCornerShape(20.dp)
        Surface(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 24.dp)
                .heightIn(max = 640.dp)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(MolinaTheme.Gold.copy(alpha = 0.6f), MolinaTheme.CardLine)),
                    frame,
                ),
            shape = frame,
            color = DialogBg,
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hasil pencarian", color = Color.White, fontSize = 20.sp,
                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("%03d/%03d".format(selected, entries.size), color = MolinaTheme.Muted,
                        fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.width(12.dp))
                    Text(clock, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                if (searchOpen) OutlinedTextField(
                    query, { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                        .focusRequester(focus),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MolinaTheme.OnBackground,
                        unfocusedTextColor = MolinaTheme.OnBackground,
                        focusedBorderColor = MolinaTheme.Gold,
                        unfocusedBorderColor = MolinaTheme.CardLine,
                        cursorColor = MolinaTheme.Gold,
                        focusedPlaceholderColor = MolinaTheme.Muted,
                        unfocusedPlaceholderColor = MolinaTheme.Muted,
                    ),
                    placeholder = { Text("Cari nama atau ketik nomor channel", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { pickFromQuery() }),
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                ) {
                    items(visible, key = { it.number }) { e ->
                        ChannelRow(e, e.number == selected,
                            onPress = { selected = e.number },
                            onClick = { onPick(e.item) },
                            onDownload = { download(listOf(e.item)) })
                    }
                }

                HorizontalDivider(color = Color(0x1FFFFFFF))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TvKey(R.drawable.ic_tv_page_prev, "Prev", Color.White, Modifier.weight(1f)) { page(-PAGE_SIZE) }
                    TvKey(R.drawable.ic_tv_page_next, "Next", Color.White, Modifier.weight(1f)) { page(PAGE_SIZE) }
                    TvKey(R.drawable.ic_tv_search, "Cari",
                        if (searchOpen) Accent else Color.White, Modifier.weight(1f)) {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    }
                    TvKey(R.drawable.ic_tv_download, "Unduh semua", MolinaTheme.Gold, Modifier.weight(1f)) {
                        download(visible.map { it.item })
                    }
                    TvKey(R.drawable.ic_tv_info, "Info", Color.White, Modifier.weight(1f)) {
                        Toast.makeText(ctx,
                            "Hasil %03d dari %03d".format(selected, entries.size),
                            Toast.LENGTH_SHORT).show()
                    }
                    TvKey(R.drawable.ic_tv_close, "Tutup", Color.White, Modifier.weight(1f), onDismiss)
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    e: ResultEntry, isSelected: Boolean,
    onPress: () -> Unit, onClick: () -> Unit, onDownload: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    val brush = if (isSelected) Brush.verticalGradient(listOf(SelectedTop, SelectedBottom))
        else Brush.verticalGradient(listOf(RowTop, RowBottom))
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .clip(shape).background(brush)
            .border(1.dp, if (isSelected) SelectedLine else RowLine, shape)
            .pointerInput(e.number) {
                detectTapGestures(onPress = { onPress(); tryAwaitRelease() }, onTap = { onClick() })
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("%03d".format(e.number), color = if (isSelected) Color.White else ChannelNumber,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Text(e.item.title, color = Color.White, fontSize = 15.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            val meta = listOfNotNull(e.item.uploader, e.item.durationSec?.let { duration(it) }, RelativeTime.label(e.item))
                .joinToString(" \u00B7 ")
            if (meta.isNotEmpty()) Text(meta, fontSize = 12.sp,
                color = if (isSelected) Color(0xCCFFFFFF) else MolinaTheme.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.padding(start = 8.dp).size(36.dp).clip(RoundedCornerShape(18.dp))
                .border(1.dp, MolinaTheme.Gold.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                .clickable(onClick = onDownload),
            contentAlignment = Alignment.Center,
        ) { Text("\u2193", color = MolinaTheme.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TvKey(icon: Int, label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(icon), contentDescription = label,
            colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(24.dp))
        Text(label.uppercase(), color = tint, fontSize = 10.sp, maxLines = 1)
    }
}
