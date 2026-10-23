package com.molina.studio.terminal

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private const val CHIP_TEXT_SP = 9
private val ChipDownload = Color(0xFF81D4FA) // biru muda
private val ChipUpload = Color(0xFFEF5350)   // merah
private val ChipNormal = Color.White
private val ChipWarn = Color(0xFFFFD54F)     // kuning: lonjakan
private val ChipHigh = Color(0xFFFF5252)     // merah: posisi tinggi
private val ChipLocale: Locale = Locale("id", "ID")

/** Bar status 4 chip berbingkai: unduh, unggah, CPU, RAM. Data dari [SystemStats]. */
@Composable
fun StatusChips() {
    val ctx = LocalContext.current
    val stats by remember(ctx) { SystemStats.flow(ctx) }.collectAsState(StatsSnapshot())
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Chip("\u2193 ${compactSpeed(stats.rxBps)}", ChipDownload, Level.Normal, 1.0f)
        Chip("\u2191 ${compactSpeed(stats.txBps)}", ChipUpload, Level.Normal, 1.0f)
        Chip("CPU ${stats.cpuPercent}%", ChipNormal, stats.cpuLevel, 0.9f)
        Chip(compactRam(stats), ChipNormal, stats.ramLevel, 1.5f)
    }
}

@Composable
private fun RowScope.Chip(text: String, color: Color, level: Level, weight: Float) {
    val tint = when (level) {
        Level.Normal -> color
        Level.Warn -> ChipWarn
        Level.High -> ChipHigh
    }
    // Normal: bingkai tipis transparan. Warn/High: bingkai solid, level terbaca dari bentuk.
    val frame = if (level == Level.Normal) tint.copy(alpha = 0.35f) else tint
    Box(
        Modifier
            .weight(weight)
            .border(1.dp, frame, RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = tint,
            fontFamily = FontFamily.Monospace,
            fontSize = CHIP_TEXT_SP.sp,
            fontWeight = if (level == Level.Normal) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

private fun compactSpeed(bps: Long): String {
    val kb = bps / 1024.0
    return if (kb < 1000) String.format(ChipLocale, "%.0fKB/s", kb)
    else String.format(ChipLocale, "%.1fMB/s", kb / 1024.0)
}

private fun compactRam(s: StatsSnapshot): String {
    val used = if (s.ramMb >= 1024) String.format(ChipLocale, "%.1fGB", s.ramMb / 1024.0)
    else "${s.ramMb}MB"
    return "RAM $used/${s.totalGbText}G"
}
