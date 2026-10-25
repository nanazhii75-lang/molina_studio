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

/** Token Windows 7 (Aero) untuk pemilih file lokal Molina Cinema. */
internal object Win7 {
    val FrameTop = Color(0xFFC9DCF0)
    val FrameBottom = Color(0xFF9FBFE3)
    val FrameLine = Color(0xFF5E7FA6)
    val Face = Color(0xFFF0F0F0)
    val NavBg = Color(0xFFF5F8FC)
    val NavHeader = Color(0xFF1E395B)
    val Ink = Color(0xFF1E1E1E)
    val TextDim = Color(0xFF6D6D6D)
    val Line = Color(0xFF8A8A8A)
    val Field = Color(0xFFABADB3)
    val Hairline = Color(0xFFD5DFE5)
    val SelFill = Color(0xFFD9EBF9)
    val SelLine = Color(0xFF7DA2CE)
    val Folder = Color(0xFFF3CF6E)
    val FolderDark = Color(0xFFD9A93B)
    val CloseTop = Color(0xFFE8A598)
    val CloseBottom = Color(0xFFC4392B)
    val BtnTop = Color(0xFFF2F2F2)
    val BtnBottom = Color(0xFFCFCFCF)
    val BtnLine = Color(0xFF707070)
    val DefaultLine = Color(0xFF3C7FB1)
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

@Composable
internal fun Win7Folder(box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Win7.FolderDark, Offset(w * 0.04f, h * 0.14f), Size(w * 0.42f, h * 0.2f), CornerRadius(w * 0.05f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Win7.Folder, Win7.FolderDark)),
            Offset(w * 0.04f, h * 0.26f), Size(w * 0.92f, h * 0.62f), CornerRadius(w * 0.07f)
        )
        drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(w * 0.08f, h * 0.32f), Size(w * 0.84f, h * 0.12f), CornerRadius(w * 0.04f))
    }
}

@Composable
internal fun Win7File(video: Boolean, box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        val page = Path().apply {
            moveTo(w * 0.18f, h * 0.06f); lineTo(w * 0.62f, h * 0.06f); lineTo(w * 0.84f, h * 0.28f)
            lineTo(w * 0.84f, h * 0.94f); lineTo(w * 0.18f, h * 0.94f); close()
        }
        drawPath(page, Color.White)
        drawPath(page, Win7.Line, style = Stroke(width = 1.2f))
        val fold = Path().apply {
            moveTo(w * 0.62f, h * 0.06f); lineTo(w * 0.62f, h * 0.28f); lineTo(w * 0.84f, h * 0.28f); close()
        }
        drawPath(fold, Win7.Hairline)
        val badge = Path().apply {
            moveTo(w * 0.36f, h * 0.46f); lineTo(w * 0.36f, h * 0.80f); lineTo(w * 0.68f, h * 0.63f); close()
        }
        drawPath(badge, if (video) Color(0xFF3C7FB1) else Color(0xFF6A9A3A))
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
