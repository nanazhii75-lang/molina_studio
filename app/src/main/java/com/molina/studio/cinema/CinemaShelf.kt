package com.molina.studio.cinema

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.core.common.MolinaDir
import com.molina.core.common.ShelfCard
import com.molina.core.common.ShelfScan
import com.molina.studio.AppPaths
import com.molina.studio.ui.MolinaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val CardW = 104.dp
private val PosterShape = RoundedCornerShape(6.dp)

/** Sumber kartu rak: riwayat putar terakhir, atau video terbaru di folder Unduhan (/sdcard/Molina/Downloads). */
private enum class ShelfTab(val label: String) { Recent("Terakhir"), Downloads("Unduhan") }

/**
 * Rak film Molina Cinema (SIDE-3): kartu poster 2:3. Poster/metadata datang dari pendamping
 * `molina_dl --sidecar` (MediaSidecar); gambar dimuat W7Thumb (poster sidecar diprioritaskan, kembali ke
 * frame video). Kartu yang pernah dilanjutkan mpv diberi garis emas tipis di tepi bawah poster (panjangnya
 * = posisi/durasi). Tab "Unduhan" memindai folder Unduhan setiap kali dibuka atau diketuk ulang, sehingga
 * film yang baru selesai diunduh langsung muncul. Semua kerja berkas di Dispatchers.IO.
 */
@Composable
internal fun CinemaShelf(items: List<CinemaRecent.Item>, onPick: (CinemaRecent.Item) -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(ShelfTab.Recent) }
    var tick by remember { mutableIntStateOf(0) }
    val dlDir = remember { AppPaths.get(ctx).dir(MolinaDir.Downloads) }
    // mpv menaruh watch_later di bawah config-dir (= filesDir); `state/` dicek juga karena bergantung versi mpv.
    val resumeDirs = remember {
        listOf(File(ctx.filesDir, "watch_later"), File(File(ctx.filesDir, "state"), "watch_later"))
    }
    val cards by produceState(initialValue = emptyList<ShelfCard.Card>(), tab, tick, items) {
        value = withContext(Dispatchers.IO) {
            val entries = if (tab == ShelfTab.Recent) items.map { ShelfCard.Entry(it.ref, it.title, it.video) } else ShelfScan.scan(dlDir)
            ShelfCard.build(entries, resumeDirs = resumeDirs)
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Rak film", color = Win7.NavHeader, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            ShelfTab.entries.forEach { t ->
                val on = t == tab
                Text(
                    t.label,
                    color = if (on) MolinaTheme.Gold else Win7.TextDim,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.clickable { if (on) tick++ else tab = t }.padding(vertical = 2.dp)
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(Win7.Hairline))
        Spacer(Modifier.height(8.dp))
        if (cards.isEmpty()) {
            Text(
                if (tab == ShelfTab.Recent) "Belum ada yang diputar." else "Belum ada video di Unduhan.",
                color = Win7.TextDim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(cards, key = { it.ref }) { card ->
                    ShelfCardView(card) {
                        val hit = items.firstOrNull { it.ref == card.ref } ?: CinemaRecent.Item(card.ref, card.title, card.video)
                        onPick(hit)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ShelfCardView(card: ShelfCard.Card, onLong: () -> Unit = {}, onClick: () -> Unit) {
    val thumb: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, card.thumbPath) {
        value = card.thumbPath?.let { W7Thumb.load(File(it)) }?.asImageBitmap()
    }
    Column(
        Modifier.width(CardW).pointerInput(card.ref) { detectTapGestures(onLongPress = { onLong() }, onTap = { onClick() }) }
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(PosterShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF241B2B), Color(0xFF0E0B10))), PosterShape)
                .border(1.dp, if (card.hasPoster) MolinaTheme.Gold else Win7.Line, PosterShape),
            Alignment.Center
        ) {
            val img = thumb
            if (img != null) {
                Image(img, contentDescription = card.title, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else {
                Text(card.monogram, color = MolinaTheme.Gold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            if (card.resumeSec != null) {
                // Aksen "sudah pernah dilanjutkan": jalur redup + garis emas sepanjang posisi/durasi.
                val f = card.resumeFraction
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x66000000)))
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth(f ?: 1f).height(3.dp)
                        .background(MolinaTheme.Gold.copy(alpha = if (f == null) 0.55f else 1f))
                )
            }
            card.duration?.let {
                Text(
                    it, color = Color.White, fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = if (card.resumeSec != null) 7.dp else 4.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(card.title, color = Win7.Ink, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (card.subtitle.isNotEmpty()) {
            Text(card.subtitle, color = Win7.TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
