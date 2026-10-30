package com.molina.studio.cinema

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.core.common.ShelfCard
import com.molina.studio.ui.MolinaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val CardW = 104.dp
private val PosterShape = RoundedCornerShape(6.dp)

/**
 * Rak film Molina Cinema (SIDE-3): kartu poster 2:3 dari riwayat putar. Poster/metadata datang dari
 * pendamping `molina_dl --sidecar` (MediaSidecar); gambar dimuat W7Thumb (poster sidecar diprioritaskan,
 * kembali ke frame video). Model kartu dibangun di Dispatchers.IO supaya main thread tidak tersentuh berkas.
 */
@Composable
internal fun CinemaShelf(items: List<CinemaRecent.Item>, onPick: (CinemaRecent.Item) -> Unit) {
    if (items.isEmpty()) return
    val cards by produceState(initialValue = emptyList<ShelfCard.Card>(), items) {
        value = withContext(Dispatchers.IO) {
            ShelfCard.build(items.map { ShelfCard.Entry(it.ref, it.title, it.video) })
        }
    }
    if (cards.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Text("Rak film", color = Win7.NavHeader, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(Win7.Hairline))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cards, key = { it.ref }) { card ->
                ShelfCardView(card) { items.firstOrNull { it.ref == card.ref }?.let(onPick) }
            }
        }
    }
}

@Composable
private fun ShelfCardView(card: ShelfCard.Card, onClick: () -> Unit) {
    val thumb: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, card.thumbPath) {
        value = card.thumbPath?.let { W7Thumb.load(File(it)) }?.asImageBitmap()
    }
    Column(Modifier.width(CardW).clickable(onClick = onClick)) {
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
            card.duration?.let {
                Text(
                    it, color = Color.White, fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
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
