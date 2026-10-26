package com.molina.studio.cinema

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.molina.studio.ui.MolinaTheme

/** Pilihan awal tombol File di Cinema Home: buka satu berkas, atau buka folder sebagai playlist. */
@Composable
internal fun CinemaOpenChooser(onFile: () -> Unit, onFolder: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp), Alignment.Center) {
            Column(
                Modifier.widthIn(max = 380.dp).fillMaxWidth()
                    .shadow(16.dp, shape)
                    .clip(shape)
                    .background(MolinaTheme.CardBg)
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(MolinaTheme.Gold.copy(alpha = 0.6f), MolinaTheme.CardLine)),
                        shape
                    )
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Text("Buka dari perangkat", color = MolinaTheme.OnBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(32.dp).height(2.dp).background(MolinaTheme.Gold, RoundedCornerShape(1.dp)))
                Spacer(Modifier.height(14.dp))
                ChooserRow("\uD83D\uDCC4", "Buka berkas", "Putar satu video atau audio", onFile)
                Spacer(Modifier.height(8.dp))
                ChooserRow("\uD83D\uDCC1", "Buka folder", "Semua media di folder jadi playlist", onFolder)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    TextButton(onDismiss) { Text("Batal", color = MolinaTheme.Muted) }
                }
            }
        }
    }
}

@Composable
private fun ChooserRow(icon: String, title: String, sub: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .border(1.dp, MolinaTheme.CardLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = MolinaTheme.OnBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = MolinaTheme.Muted, fontSize = 12.sp)
        }
    }
}
