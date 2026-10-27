package com.molina.studio.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.studio.cinema.Win7
import com.molina.studio.terminal.StatsSnapshot
import com.molina.studio.terminal.SystemStats
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.ln
import com.molina.studio.terminal.Level as LoadLevel

private val MonLocale: Locale = Locale("id", "ID")

private val RgbColors = listOf(
    Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676),
    Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFFD500F9), Color(0xFFFF1744)
)

// Kaca Aero (varian gelap): biru kaca di sisi tepi, gelap di bawah.
private val GlassTop = Color(0xFF4A7DB5)
private val GlassMid = Color(0xFF244A75)
private val GlassBottom = Color(0xFF0F1F34)

/** Bingkai RGB berputar (sweep gradient) setebal 2dp; hanya menggambar ulang, tanpa recomposition. */
@Composable
private fun RgbFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart)
    )
    Box(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .drawBehind {
                val d = hypot(size.width, size.height)
                rotate(angle) {
                    drawRect(
                        brush = Brush.sweepGradient(RgbColors, center),
                        topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f),
                        size = Size(d, d)
                    )
                }
            }
            .padding(2.dp)
    ) { content() }
}

/** Jendela ala Windows 7 Aero: tepi kaca berkilap, bilah judul, dan area klien hitam. */
@Composable
private fun Win7GlassWindow(title: String, content: @Composable ColumnScope.() -> Unit) {
    val outer = RoundedCornerShape(9.dp)
    val inner = RoundedCornerShape(3.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(outer)
            .background(Brush.verticalGradient(listOf(GlassTop, GlassMid, GlassBottom)), outer)
            .drawBehind {
                drawRect(Color.White.copy(alpha = 0.10f), Offset.Zero, Size(size.width, size.height * 0.45f))
            }
            .border(1.dp, Color(0x99BFE0FF), outer)
            .padding(start = 7.dp, end = 7.dp, bottom = 7.dp, top = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            W7IconMonitor(18.dp)
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 13.sp)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(inner)
                .background(Win7.Face)
                .border(1.dp, Win7.FrameLine, inner)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Satu baris meter: label, nilai, dan bilah progres hijau-kaca ala Windows 7. */
@Composable
private fun MonitorMeter(label: String, value: String, fraction: Float, top: Color, bottom: Color) {
    val f by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f))
    val bar = RoundedCornerShape(3.dp)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Win7.Ink, fontSize = 13.sp)
            Text(value, color = Win7.NavHeader, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(bar)
                .background(Brush.verticalGradient(listOf(Win7.BarBottom, Win7.BarTop)), bar)
                .border(1.dp, Win7.Line, bar)
                .padding(1.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .fillMaxHeight()
                    .clip(bar)
                    .background(Brush.verticalGradient(listOf(top, bottom)), bar)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .background(Color.White.copy(alpha = 0.28f))
                )
            }
        }
    }
}

private fun levelColors(l: LoadLevel): Pair<Color, Color> = when (l) {
    LoadLevel.Normal -> Color(0xFF7BE87A) to Color(0xFF1E9E1A)
    LoadLevel.Warn -> Color(0xFFFFE680) to Color(0xFFE0A800)
    LoadLevel.High -> Color(0xFFFF8A80) to Color(0xFFC4392B)
}

/** Skala logaritmik: 1 KB/s ~ 6%, 1 MB/s ~ 60%, 100 MB/s = penuh. */
private fun netFraction(bps: Long): Float =
    (ln(1.0 + bps.coerceAtLeast(0L) / 1024.0) / ln(1.0 + 102400.0)).toFloat().coerceIn(0f, 1f)

private fun ramText(s: StatsSnapshot): String {
    val used = if (s.ramMb >= 1024) String.format(MonLocale, "%.1f GB", s.ramMb / 1024.0) else "${s.ramMb} MB"
    val pct = if (s.totalRamMb > 0) s.ramMb * 100 / s.totalRamMb else 0
    return "$used / ${s.totalGbText} GB ($pct%)"
}

/** Halaman Settings > Sistem > Monitor Sistem: unduh, unggah, CPU, RAM (data dari [SystemStats]). */
@Composable
internal fun SystemMonitorPage() {
    val ctx = LocalContext.current
    val stats by remember(ctx) { SystemStats.flow(ctx) }.collectAsState(StatsSnapshot())
    val cpuC = levelColors(stats.cpuLevel)
    val ramC = levelColors(stats.ramLevel)
    val ramFraction = if (stats.totalRamMb > 0) stats.ramMb.toFloat() / stats.totalRamMb else 0f

    Spacer(Modifier.height(4.dp))
    RgbFrame(Modifier.fillMaxWidth()) {
        Win7GlassWindow("Monitor Sistem") {
            MonitorMeter("\u2193 Unduh", stats.rxText, netFraction(stats.rxBps), Color(0xFF8FC4EE), Color(0xFF2D6DB5))
            MonitorMeter("\u2191 Unggah", stats.txText, netFraction(stats.txBps), Color(0xFFE8776B), Color(0xFFB3261E))
            MonitorMeter("CPU", "${stats.cpuPercent}%", stats.cpuPercent / 100f, cpuC.first, cpuC.second)
            MonitorMeter("RAM", ramText(stats), ramFraction, ramC.first, ramC.second)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Mengukur seluruh proses aplikasi (termasuk Debian PRoot). Berhenti saat aplikasi di latar belakang. " +
            "Bilah jaringan memakai skala logaritmik. Kuning = lonjakan, merah = tinggi.",
        color = Win7.TextDim,
        fontSize = 12.sp
    )
}
