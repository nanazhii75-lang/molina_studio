package com.molina.studio.terminal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.util.TypedValue
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.KeyHandler
import com.termux.view.TerminalView

@Composable
fun TerminalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
    var ready by remember {
        mutableStateOf(context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        ready = true // izin ditolak pun lanjut (cwd fallback ke filesDir)
    }
    LaunchedEffect(Unit) { if (!ready) launcher.launch(perm) }

    if (!ready) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Meminta izin penyimpanan...", color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        val mods = remember { TerminalModifiers() }
        Column(modifier) {
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx -> createTerminalView(ctx, mods) },
                onRelease = { v ->
                    if (TerminalSessions.client.view === v) TerminalSessions.client.view = null
                }
            )
            ExtraKeys(mods)
        }
    }
}

private fun createTerminalView(ctx: Context, mods: TerminalModifiers): TerminalView {
    val view = TerminalView(ctx, null)
    val session = TerminalSessions.main(ctx)
    TerminalSessions.client.view = view
    view.setTerminalViewClient(MolinaViewClient(view, mods))
    view.setTextSize(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 14f, ctx.resources.displayMetrics
        ).toInt()
    )
    view.setTypeface(Typeface.MONOSPACE)
    view.isFocusable = true
    view.isFocusableInTouchMode = true
    view.attachSession(session)
    view.requestFocus()
    return view
}

private fun send(s: String) {
    val b = s.toByteArray()
    TerminalSessions.currentSession?.write(b, 0, b.size)
}

private fun arrow(keyCode: Int) {
    val em = TerminalSessions.currentSession?.emulator ?: return
    KeyHandler.getCode(keyCode, 0, em.isCursorKeysApplicationMode, em.isKeypadApplicationMode)
        ?.let { send(it) }
}

@Composable
private fun ExtraKeys(mods: TerminalModifiers) {
    Row(Modifier.fillMaxWidth()) {
        Key("ESC") { send("\u001b") }
        Key("TAB") { send("\t") }
        Key("CTRL", active = mods.ctrl) { mods.ctrl = !mods.ctrl }
        Key("\u2191") { arrow(KeyEvent.KEYCODE_DPAD_UP) }
        Key("\u2193") { arrow(KeyEvent.KEYCODE_DPAD_DOWN) }
        Key("\u2190") { arrow(KeyEvent.KEYCODE_DPAD_LEFT) }
        Key("\u2192") { arrow(KeyEvent.KEYCODE_DPAD_RIGHT) }
        Key("/") { send("/") }
        Key("-") { send("-") }
    }
}

@Composable
private fun RowScope.Key(label: String, active: Boolean = false, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
