package com.molina.studio.cinema

import android.content.Context
import android.content.ContextWrapper
import android.os.Environment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.core.common.MolinaDir
import com.molina.core.common.ResumeMark
import com.molina.core.common.ShelfCard
import com.molina.core.common.TvGuide
import com.molina.studio.AppPaths
import com.molina.studio.ui.MolinaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val TvBg = Color(0xFF0E0B10)
private val TileShape = RoundedCornerShape(6.dp)

private tailrec fun Context.lifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.lifecycleOwner()
    else -> null
}

private class TvData(val rows: List<TvGuide.Row>, val cards: Map<String, ShelfCard.Card>)

private fun tvPlaces(ctx: android.content.Context): List<Pair<String, File>> {
    val p = AppPaths.get(ctx)
    return listOf(
        "Unduhan" to p.dir(MolinaDir.Downloads),
        "Video" to p.dir(MolinaDir.Video),
        "Audio" to p.dir(MolinaDir.Audio),
        "IPTV" to p.dir(MolinaDir.Iptv),
        "Molina" to p.appRoot,
        "Film" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        "Musik" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
    )
}

/**
 * Mode TV penjelajah berkas Molina Cinema (SIDE-5, tahap 2), dirancang untuk ponsel potret layar sentuh:
 * bilah status (jam, ruang bebas, tutup), chip saluran bernomor, baris-baris kartu yang bisa digeser, dan
 * panel info ala EPG. Ketuk = buka (folder masuk, video/audio putar); tekan lama = tampilkan info di panel.
 * Model dari TvGuide/ShelfCard (teruji di :core:common); semua pembacaan berkas di Dispatchers.IO.
 * Pemanggil memberi [onPlay] (mis. CinemaActions.playPath) dan [onClose]; sakelar/titik masuk menyusul (tahap 3).
 */
@Composable
internal fun TvBrowser(onPlay: (File) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val channels = remember { TvGuide.channels(tvPlaces(ctx)) }
    val resumeDirs = remember {
        listOf(File(ctx.filesDir, "watch_later"), File(File(ctx.filesDir, "state"), "watch_later"))
    }
    var idx by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(channels.firstOrNull()?.dir) }
    var focus by remember { mutableStateOf<TvGuide.Item?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Muat ulang setelah layar kembali dari pemutar (ON_PAUSE lalu ON_RESUME), agar garis "dilanjutkan" mutakhir.
    val owner = remember { ctx.lifecycleOwner() }
    DisposableEffect(owner) {
        var armed = false
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) {
                armed = true
            } else if (e == Lifecycle.Event.ON_RESUME && armed) {
                armed = false
                tick++
            }
        }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }

    val clock by produceState("") {
        while (true) {
            value = TvGuide.clock(System.currentTimeMillis())
            delay(30_000L)
        }
    }
    val free by produceState<String?>(null, dir) { value = dir?.let { d -> withContext(Dispatchers.IO) { TvGuide.freeLabel(d) } } }
    val data by produceState<TvData?>(null, dir, tick) {
        val d = dir
        value = if (d == null) null else withContext(Dispatchers.IO) {
            val rows = TvGuide.rows(d)
            val vids = rows.filter { it.title == "Video" }.flatMap { it.items }
                .map { ShelfCard.Entry(it.file.path, it.name, true) }
            TvData(rows, ShelfCard.build(vids, max = vids.size, resumeDirs = resumeDirs).associateBy { it.ref })
        }
    }

    Column(Modifier.fillMaxSize().background(TvBg)) {
        // ---- bilah status
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Molina Cinema", color = MolinaTheme.Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            free?.let { Text(it, color = Win7.TextDim, fontSize = 11.sp, modifier = Modifier.padding(end = 10.dp)) }
            Text(clock, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "Tutup", color = Win7.TextDim, fontSize = 12.sp,
                modifier = Modifier.padding(start = 14.dp).clickable(onClick = onClose).padding(vertical = 4.dp)
            )
        }
        if (channels.isEmpty()) {
            Text("Belum ada folder media. Buat folder di /sdcard/Molina atau beri izin penyimpanan.", color = Win7.TextDim, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            return@Column
        }
        // ---- chip saluran
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(channels, key = { it.number }) { ch ->
                val on = ch.number - 1 == idx
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(if (on) Color(0x33FFFFFF) else Color(0x14FFFFFF))
                        .border(1.dp, if (on) MolinaTheme.Gold else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable {
                            if (on) tick++ else { idx = ch.number - 1; dir = ch.dir; focus = null }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ch.number.toString(), color = MolinaTheme.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(ch.label, color = if (on) Color.White else Win7.TextDim, fontSize = 13.sp)
                }
            }
        }
        // ---- jejak folder + tombol naik
        val root = channels.getOrNull(idx)?.dir
        val cur = dir
        if (cur != null && root != null && cur.path != root.path) {
            Text(
                "\u2039  " + cur.name,
                color = MolinaTheme.Gold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
                    .clickable { cur.parentFile?.let { dir = it; focus = null } }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }
        // ---- baris kartu
        val d = data
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (d == null) {
                Text("Memuat\u2026", color = Win7.TextDim, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
            } else if (d.rows.isEmpty()) {
                Text("Folder ini kosong.", color = Win7.TextDim, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(d.rows, key = { it.title }) { row ->
                        Column {
                            Text(
                                row.title + " \u00B7 " + row.total, color = Color.White, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(row.items, key = { it.file.path }) { item ->
                                    val open: () -> Unit = {
                                        when (item.kind) {
                                            TvGuide.Kind.Folder -> { dir = item.file; focus = null }
                                            TvGuide.Kind.Video, TvGuide.Kind.Audio -> onPlay(item.file)
                                            TvGuide.Kind.Other -> focus = item
                                        }
                                    }
                                    val card = d.cards[item.file.path]
                                    if (card != null) ShelfCardView(card, onLong = { focus = item }) { open() }
                                    else TvTile(item, onLong = { focus = item }) { open() }
                                }
                            }
                        }
                    }
                }
            }
        }
        // ---- panel info (EPG)
        TvInfoPanel(
            item = focus,
            extra = focus?.let { f -> data?.cards?.get(f.file.path)?.let { c -> listOfNotNull(c.duration, c.subtitle.takeIf { it.isNotEmpty() }).joinToString(" \u00B7 ") } },
            fallback = cur?.path.orEmpty() + (data?.let { dd -> "  \u00B7  " + dd.rows.joinToString(", ") { it.total.toString() + " " + it.title.lowercase() } } ?: ""),
            resumeSec = focus?.let { f -> data?.cards?.get(f.file.path)?.resumeSec },
            onReset = { f ->
                scope.launch {
                    withContext(Dispatchers.IO) { ResumeMark.clear(resumeDirs, f.file.path) }
                    tick++
                }
            },
            onOpen = { f ->
                when (f.kind) {
                    TvGuide.Kind.Folder -> { dir = f.file; focus = null }
                    TvGuide.Kind.Video, TvGuide.Kind.Audio -> onPlay(f.file)
                    TvGuide.Kind.Other -> {}
                }
            },
            onClear = { focus = null }
        )
    }
}

@Composable
private fun TvTile(item: TvGuide.Item, onLong: () -> Unit, onClick: () -> Unit) {
    val folder = item.kind == TvGuide.Kind.Folder
    val label = when (item.kind) {
        TvGuide.Kind.Folder -> "FOLDER"
        TvGuide.Kind.Audio -> "AUDIO"
        else -> "BERKAS"
    }
    Column(
        Modifier.width(104.dp).pointerInput(item.file.path) { detectTapGestures(onLongPress = { onLong() }, onTap = { onClick() }) }
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(TileShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF241B2B), Color(0xFF0E0B10))), TileShape)
                .border(1.dp, if (folder) MolinaTheme.Gold else Win7.Line, TileShape),
            Alignment.Center
        ) {
            Text(label, color = Win7.TextDim, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
            Text(ShelfCard.monogram(item.title), color = MolinaTheme.Gold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(item.title, color = Win7.Ink, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TvInfoPanel(
    item: TvGuide.Item?,
    extra: String?,
    fallback: String,
    resumeSec: Int?,
    onReset: (TvGuide.Item) -> Unit,
    onOpen: (TvGuide.Item) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF17121B)).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (item == null) {
            Text(fallback, color = Win7.TextDim, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Tekan lama kartu untuk melihat info.", color = Win7.TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        } else {
            Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(TvGuide.describe(item, extra), color = Win7.TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            if (resumeSec != null) {
                Text(
                    "Dilanjutkan di " + ShelfCard.clock(resumeSec), color = MolinaTheme.Gold, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.kind != TvGuide.Kind.Other) {
                    Text(
                        if (item.kind == TvGuide.Kind.Folder) "Buka" else "Putar",
                        color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MolinaTheme.Gold)
                            .clickable { onOpen(item) }.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                if (resumeSec != null) {
                    Text(
                        "Mulai dari awal", color = MolinaTheme.Gold, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, MolinaTheme.Gold, RoundedCornerShape(12.dp))
                            .clickable { onReset(item) }.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Text(
                    "Tutup info", color = Win7.TextDim, fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, Win7.Line, RoundedCornerShape(12.dp))
                        .clickable(onClick = onClear).padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
