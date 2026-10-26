package com.molina.studio.cinema

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.studio.ui.MolinaTheme

/** Token Windows 7 (Aero) untuk pemilih file lokal Molina Cinema. */
internal object Win7 {
    val FrameTop = Color(0xFF1A2230)
    val FrameBottom = Color(0xFF0A0F16)
    val FrameLine = Color(0xFF2E4A68)
    val Face = Color(0xFF000000)
    val NavBg = Color(0xFF07090D)
    val NavHeader = Color(0xFF8FC4EE)
    val Ink = Color(0xFFEAEFF5)
    val TextDim = Color(0xFF9AA5B1)
    val Line = Color(0xFF5A6675)
    val Field = Color(0xFF3A4654)
    val Hairline = Color(0xFF26303B)
    val SelFill = Color(0xFF12263A)
    val SelLine = Color(0xFF3C7FB1)
    val Folder = Color(0xFFF3CF6E)
    val FolderDark = Color(0xFFD9A93B)
    val CloseTop = Color(0xFFE8A598)
    val CloseBottom = Color(0xFFC4392B)
    val BtnTop = Color(0xFF1E252E)
    val BtnBottom = Color(0xFF10151B)
    val BtnLine = Color(0xFF4A5666)
    val DefaultLine = Color(0xFF4DA3FF)
    val Input = Color(0xFF0F1318)
    val BarTop = Color(0xFF14181E)
    val BarBottom = Color(0xFF05070A)
}

@Composable
internal fun Win7Button(
    label: String,
    modifier: Modifier = Modifier,
    isDefault: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier.height(28.dp).widthIn(min = 76.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Win7.BtnTop, Win7.BtnBottom)), shape)
            .border(1.dp, if (isDefault && enabled) Win7.DefaultLine else Win7.BtnLine, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        Alignment.Center
    ) { Text(label, color = if (enabled) Win7.Ink else Win7.TextDim, fontSize = 13.sp) }
}

@Composable
internal fun Win7BackOrb(enabled: Boolean, onClick: () -> Unit) {
    val top = if (enabled) Color(0xFF8FC4EE) else Color(0xFFD7DDE4)
    val bottom = if (enabled) Color(0xFF2D6DB5) else Color(0xFFB5BDC7)
    Box(
        Modifier.size(26.dp).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(top, bottom)), CircleShape)
            .border(1.dp, if (enabled) Color(0xFF1F4E85) else Color(0xFF9AA3AD), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        Alignment.Center
    ) {
        Canvas(Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w * 0.05f, h * 0.5f); lineTo(w * 0.5f, h * 0.08f); lineTo(w * 0.5f, h * 0.32f)
                lineTo(w * 0.95f, h * 0.32f); lineTo(w * 0.95f, h * 0.68f); lineTo(w * 0.5f, h * 0.68f)
                lineTo(w * 0.5f, h * 0.92f); close()
            }
            drawPath(p, Color.White)
        }
    }
}

@Composable
internal fun Win7Close(onClick: () -> Unit) {
    val shape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)
    Box(
        Modifier.width(44.dp).height(20.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Win7.CloseTop, Win7.CloseBottom)), shape)
            .border(1.dp, Color(0xFF6E1F18), shape)
            .clickable(onClick = onClick),
        Alignment.Center
    ) {
        Canvas(Modifier.size(9.dp)) {
            val s = 1.8.dp.toPx()
            drawLine(Color.White, Offset(0f, 0f), Offset(size.width, size.height), s, StrokeCap.Round)
            drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), s, StrokeCap.Round)
        }
    }
}

/** [GOLD-ICONS] Folder emas Molina: badan gradasi emas merek, tab lebih gelap, kilap tipis. */
@Composable
internal fun Win7Folder(box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF8A6418), Offset(w * 0.04f, h * 0.14f), Size(w * 0.42f, h * 0.2f), CornerRadius(w * 0.05f))
        drawRoundRect(
            Brush.verticalGradient(listOf(MolinaTheme.Gold, Color(0xFFB07F22))),
            Offset(w * 0.04f, h * 0.26f), Size(w * 0.92f, h * 0.62f), CornerRadius(w * 0.07f)
        )
        drawRoundRect(Color.White.copy(alpha = 0.30f), Offset(w * 0.08f, h * 0.32f), Size(w * 0.84f, h * 0.12f), CornerRadius(w * 0.04f))
    }
}

/** Folder akar Molina (/sdcard/Molina): emas lebih terang dengan bintang kecil sebagai pembeda. */
@Composable
internal fun Win7MolinaFolder(box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF9A6F10), Offset(w * 0.04f, h * 0.14f), Size(w * 0.42f, h * 0.2f), CornerRadius(w * 0.05f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFFFDC85), MolinaTheme.Gold)),
            Offset(w * 0.04f, h * 0.26f), Size(w * 0.92f, h * 0.62f), CornerRadius(w * 0.07f)
        )
        val cx = w * 0.5f
        val cy = h * 0.58f
        val r = w * 0.17f
        val star = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val ang = Math.toRadians((-90.0 + i * 36.0))
            val x = cx + (rad * Math.cos(ang)).toFloat()
            val y = cy + (rad * Math.sin(ang)).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, Color(0xFF5A3F06))
    }
}

/** Berkas bergaya Home Theater: halaman gelap, garis tepi emas, lencana emas (video = putar, audio = not). */
@Composable
internal fun Win7File(video: Boolean, box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        val gold = MolinaTheme.Gold
        val page = Path().apply {
            moveTo(w * 0.18f, h * 0.06f); lineTo(w * 0.62f, h * 0.06f); lineTo(w * 0.84f, h * 0.28f)
            lineTo(w * 0.84f, h * 0.94f); lineTo(w * 0.18f, h * 0.94f); close()
        }
        drawPath(page, Color(0xFF1B1E26))
        drawPath(page, gold.copy(alpha = 0.85f), style = Stroke(width = 1.4f))
        val fold = Path().apply {
            moveTo(w * 0.62f, h * 0.06f); lineTo(w * 0.62f, h * 0.28f); lineTo(w * 0.84f, h * 0.28f); close()
        }
        drawPath(fold, gold.copy(alpha = 0.45f))
        if (video) {
            val badge = Path().apply {
                moveTo(w * 0.36f, h * 0.46f); lineTo(w * 0.36f, h * 0.80f); lineTo(w * 0.68f, h * 0.63f); close()
            }
            drawPath(badge, gold)
        } else {
            drawCircle(gold, w * 0.09f, Offset(w * 0.40f, h * 0.76f))
            drawRect(gold, Offset(w * 0.46f, h * 0.44f), Size(w * 0.05f, h * 0.34f))
            drawRect(gold, Offset(w * 0.46f, h * 0.44f), Size(w * 0.20f, h * 0.07f))
        }
    }
}

@Composable
internal fun Win7Drive(box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFE6E6E6), Color(0xFF9A9A9A))),
            Offset(w * 0.06f, h * 0.3f), Size(w * 0.88f, h * 0.42f), CornerRadius(w * 0.06f)
        )
        drawCircle(Color(0xFF3CC13C), w * 0.05f, Offset(w * 0.78f, h * 0.52f))
    }
}
