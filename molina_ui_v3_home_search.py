#!/usr/bin/env python3
"""Molina Cinema v3: dialog 'Hasil pencarian' di Home (Compose) disamakan dengan playlist ala channel TV. Idempotent."""
import os, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
APP = os.path.join(ROOT, "app/src/main")
CIN = os.path.join(APP, "java/com/molina/studio/cinema")
HOME = os.path.join(CIN, "CinemaHome.kt")
if not os.path.exists(HOME):
    sys.exit("Bukan root repo molina-studio: %s" % ROOT)

def rd(p): return open(p, encoding="utf-8").read()
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path) and rd(path) == text:
        print("skip  ", os.path.relpath(path, ROOT)); return
    open(path, "w", encoding="utf-8").write(text); print("tulis ", os.path.relpath(path, ROOT))

def vec(d):
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp"\n'
            '    android:viewportWidth="24" android:viewportHeight="24">\n'
            f'  <path android:fillColor="#FFFFFFFF" android:pathData="{d}"/>\n</vector>\n')

for n, d in {
 "ic_tv_search": "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
 "ic_tv_info": "M11,7h2v2h-2zM11,11h2v6h-2zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z",
 "ic_tv_close": "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
}.items():
    write(os.path.join(APP, "res/drawable", n + ".xml"), vec(d))

KOTLIN = r'''package com.molina.studio.cinema

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.molina.core.common.StreamSearchItem
import com.molina.studio.R
import com.molina.studio.ui.MolinaTheme
import kotlinx.coroutines.delay
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

    fun pickFromQuery() {
        val t = if (numeric) visible.firstOrNull { it.number == q.toIntOrNull() } else visible.singleOrNull()
        if (t != null) onPick(t.item)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 640.dp),
            shape = RoundedCornerShape(28.dp),
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
                            onClick = { onPick(e.item) })
                    }
                }

                HorizontalDivider(color = Color(0x1FFFFFFF))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TvKey(R.drawable.ic_tv_search, "Cari",
                        if (searchOpen) Accent else Color.White, Modifier.weight(1f)) {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
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
private fun ChannelRow(e: ResultEntry, isSelected: Boolean, onPress: () -> Unit, onClick: () -> Unit) {
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
            val meta = listOfNotNull(e.item.uploader, e.item.durationSec?.let { duration(it) })
                .joinToString(" \u00B7 ")
            if (meta.isNotEmpty()) Text(meta, fontSize = 12.sp,
                color = if (isSelected) Color(0xCCFFFFFF) else MolinaTheme.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
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
'''
write(os.path.join(CIN, "CinemaResults.kt"), KOTLIN)

# ---- patch CinemaHome.kt ----
h = rd(HOME); changed = False
if "private fun duration(" in h:
    h = h.replace("private fun duration(", "internal fun duration(", 1); changed = True; print("patch  duration -> internal")
else: print("skip   duration")

if "SearchResultsDialog(" in h: print("skip   dialog hasil")
else:
    start = h.find("    if (showResults) AlertDialog(")
    end_marker = "        confirmButton = { TextButton({ showResults = false }) { Text(\"Tutup\") } }\n    )\n"
    end = h.find(end_marker, start)
    if start < 0 or end < 0: sys.exit("Blok dialog hasil pencarian tidak ditemukan di CinemaHome.kt")
    end += len(end_marker)
    new = '''    if (showResults) SearchResultsDialog(
        results = results,
        onDismiss = { showResults = false },
        onPick = { r ->
            showResults = false
            val d = CinemaActions.classify(r.pageUrl)
            info = if (d is CinemaActions.Input.Direct && CinemaActions.playResults(ctx, results, r)) "" else FAIL
        }
    )
'''
    h = h[:start] + new + h[end:]; changed = True; print("patch  CinemaHome.kt dialog")
if changed: open(HOME, "w", encoding="utf-8").write(h)

pg = os.path.join(ROOT, "progres.md")
if os.path.exists(pg):
    p = rd(pg)
    if "Cinema UI v3" not in p:
        open(pg, "w", encoding="utf-8").write(p.rstrip("\n") + """

## 2026-10-25 - Cinema UI v3 (hasil pencarian Home ala playlist TV)
- [x] feat(home-ui): dialog Hasil pencarian (Compose) diganti SearchResultsDialog di CinemaResults.kt: baris bernomor 001.., baris terpilih kotak biru (tekan = sorot lalu putar), penghitung 003/050 + jam, Cari (teks = saring, angka = lompat, Enter = putar), Info, Tutup; logika putar tetap CinemaActions.playResults
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: keyboard saat Cari, jumlah baris 50, scroll, ketuk baris memutar + antrean berlanjut
- [ ] dialog hasil pencarian di dalam player (MPVActivity, ArrayAdapter 'Search results') belum diseragamkan
""")
        print("patch  progres.md")
    else: print("skip   progres.md")
print("selesai.")
