package com.molina.studio.terminal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.Toast
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import kotlin.math.floor

private fun spToPx(ctx: Context, sp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), ctx.resources.displayMetrics).toInt()

/** Lebar satu sel monospace pada ukuran font tertentu (sama seperti TerminalRenderer). */
private fun cellWidthPx(ctx: Context, sp: Int): Float =
    Paint().apply { typeface = Typeface.MONOSPACE; textSize = spToPx(ctx, sp).toFloat() }.measureText("X")

@Composable
fun TerminalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
    var granted by remember {
        mutableStateOf(context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = true // ditolak pun lanjut (cwd fallback)
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(perm) }

    if (!granted) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Meminta izin penyimpanan...", color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        TerminalContent(modifier)
    }
}

@Composable
private fun TerminalContent(modifier: Modifier) {
    val context = LocalContext.current
    val mods = remember { TerminalModifiers() }
    remember(context) { RootfsManager.refresh(context) }
    val rootfsState by RootfsManager.state.collectAsState()

    Column(modifier) {
        StatusBar()
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (rootfsState !is RootfsState.Ready) {
                InstallPanel(rootfsState) { RootfsManager.install(context) }
            } else {
                val fontSp = TerminalPrefs.fontSp
                val density = LocalDensity.current
                // Terminal Margin Adjustment: pusatkan kolom penuh, sisa lebar dibagi dua sisi.
                val cell = remember(fontSp) { cellWidthPx(context, fontSp) }
                val marginDp = if (TerminalPrefs.marginAdjustment && cell > 0f) {
                    val w = constraints.maxWidth.toFloat()
                    val rest = w - floor(w / cell) * cell
                    with(density) { (rest / 2f).toDp() }
                } else 0.dp
                AndroidView(
                    modifier = Modifier.fillMaxSize().padding(horizontal = marginDp),
                    factory = { ctx -> createTerminalView(ctx, mods) },
                    update = { v ->
                        val sp = TerminalPrefs.fontSp
                        if (v.tag != sp) {
                            v.tag = sp
                            v.setTextSize(spToPx(v.context, sp))
                        }
                    },
                    onRelease = { v ->
                        if (TerminalSessions.client.view === v) TerminalSessions.client.view = null
                    }
                )
            }
        }
        ExtraKeys(mods)
    }
}

private val KeyRed = Color(0xFFFF8A80)    // ESC, CLEAR
private val KeyBlue = Color(0xFF82B1FF)   // HOME, panah
private val KeyGreen = Color(0xFF69F0AE)  // COPY, PASTE
private val KeyAmber = Color(0xFFFFD54F)  // CTRL, ALT, TAB, menu
private val DownloadColor = Color(0xFF81D4FA) // biru muda
private val UploadColor = Color(0xFFEF5350)   // merah
private val NormalColor = Color.White
private val WarnColor = Color(0xFFFFD54F)     // kuning: lonjakan
private val HighColor = Color(0xFFFF5252)     // merah: posisi tinggi

private fun levelStyle(level: Level): SpanStyle = when (level) {
    Level.Normal -> SpanStyle(color = NormalColor)
    Level.Warn -> SpanStyle(color = WarnColor, fontWeight = FontWeight.Bold)
    Level.High -> SpanStyle(color = HighColor, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatusBar() {
    val ctx = LocalContext.current
    val stats by remember(ctx) { SystemStats.flow(ctx) }.collectAsState(StatsSnapshot())
    val text = remember(stats) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = DownloadColor)) { append("\u2193 ${stats.rxText}") }
            append("    ")
            withStyle(SpanStyle(color = UploadColor)) { append("\u2191 ${stats.txText}") }
            append("    ")
            withStyle(levelStyle(stats.cpuLevel)) { append("CPU ${stats.cpuPercent}%") }
            append("    ")
            withStyle(levelStyle(stats.ramLevel)) {
                append("RAM app ${stats.ramMb} MB/${stats.totalGbText}GB")
            }
        }
    }
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        color = NormalColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
private fun InstallPanel(state: RootfsState, onInstall: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Debian trixie (arm64)", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        when (state) {
            is RootfsState.Downloading -> {
                Text("Mengunduh... ${state.percent}%", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { state.percent / 100f })
            }
            is RootfsState.Verifying ->
                Text("Memverifikasi SHA-256...", color = MaterialTheme.colorScheme.onSurface)
            is RootfsState.Extracting -> {
                Text("Mengekstrak... ${state.percent}%", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { state.percent / 100f })
            }
            is RootfsState.Failed -> {
                Text("Gagal: ${state.message}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onInstall) { Text("Coba lagi") }
            }
            else -> {
                Text("Belum terpasang. Unduhan sekitar 30 MB, ruang akhir sekitar 105 MB.",
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onInstall) { Text("Pasang Debian") }
            }
        }
    }
}

private fun createTerminalView(ctx: Context, mods: TerminalModifiers): TerminalView {
    val view = TerminalView(ctx, null)
    val session = TerminalSessions.session(ctx)
    TerminalSessions.client.view = view
    view.setTerminalViewClient(MolinaViewClient(view, mods))
    view.tag = TerminalPrefs.fontSp
    view.setTextSize(spToPx(ctx, TerminalPrefs.fontSp))
    view.setTypeface(Typeface.MONOSPACE)
    view.isFocusable = true
    view.isFocusableInTouchMode = true
    view.attachSession(session)
    view.requestFocus()
    return view
}

@Composable
private fun ExtraKeys(mods: TerminalModifiers) {
    val ctx = LocalContext.current
    val menu = remember {
        listOf("/", "|", "~", "\\", "_", "&").map { sym -> sym to { TerminalActions.send(sym) } } +
            listOf(
                "END" to { TerminalActions.sendKey(KeyEvent.KEYCODE_MOVE_END) },
                "PGUP" to { TerminalActions.sendKey(KeyEvent.KEYCODE_PAGE_UP) },
                "PGDN" to { TerminalActions.sendKey(KeyEvent.KEYCODE_PAGE_DOWN) }
            )
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Key("ESC", color = KeyRed) { TerminalActions.send("\u001b") }
            MenuKey("\u2261", menu, KeyAmber)
            Key("\u2014", color = KeyAmber) { TerminalActions.send("-") }
            Key("HOME", color = KeyBlue) { TerminalActions.sendKey(KeyEvent.KEYCODE_MOVE_HOME) }
            Key("\u2191", color = KeyBlue) { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_UP) }
            Key("CLEAR", color = KeyRed) { TerminalActions.clear() }
            Key("COPY", color = KeyGreen) {
                val ok = TerminalActions.copyAll(ctx)
                Toast.makeText(
                    ctx, if (ok) "Seluruh teks terminal disalin" else "Tidak ada teks untuk disalin",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Key("\u21C6", color = KeyAmber) { TerminalActions.send("\t") }
            Key("CTRL", active = mods.ctrl, locked = mods.ctrlLocked, color = KeyAmber,
                onLongClick = { mods.toggleCtrlLock() }) { mods.tapCtrl() }
            Key("ALT", active = mods.alt, locked = mods.altLocked, color = KeyAmber,
                onLongClick = { mods.toggleAltLock() }) { mods.tapAlt() }
            Key("\u2190", color = KeyBlue) { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_LEFT) }
            Key("\u2193", color = KeyBlue) { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_DOWN) }
            Key("\u2192", color = KeyBlue) { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT) }
            Key("PASTE", color = KeyGreen) { TerminalActions.paste(ctx) }
        }
    }
}

@Composable
private fun RowScope.Key(
    label: String,
    active: Boolean = false,
    locked: Boolean = false,
    color: Color = Color.Unspecified,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(Modifier.weight(1f)) { KeyButton(label, active, locked, color, onLongClick, onClick) }
}

@Composable
private fun RowScope.MenuKey(
    label: String,
    items: List<Pair<String, () -> Unit>>,
    color: Color = Color.Unspecified
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.weight(1f)) {
        KeyButton(label, false, false, color, null) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() })
            }
        }
    }
}

/**
 * Tombol ekstra. active = modifier aktif sekali pakai (latar redup);
 * locked = modifier dikunci lewat long-press (latar menyala + bingkai).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyButton(
    label: String,
    active: Boolean,
    locked: Boolean,
    tint: Color,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val base = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    val shape = RoundedCornerShape(8.dp)
    val bg = when {
        locked -> base.copy(alpha = 0.40f)
        active -> base.copy(alpha = 0.22f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(bg, shape)
            .then(if (locked) Modifier.border(1.5.dp, base, shape) else Modifier)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let { lc ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lc()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            maxLines = 1,
            fontWeight = if (active || locked) FontWeight.Bold else FontWeight.Medium,
            color = if (active || locked) base else base.copy(alpha = 0.85f)
        )
    }
}
