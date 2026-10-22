package com.molina.studio.terminal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.util.TypedValue
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.molina.studio.log.LogScreen
import com.molina.studio.log.LogTabButton
import com.molina.studio.log.LogUi
import com.termux.view.TerminalView

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
    val kind = TerminalSessions.selected

    Column(modifier) {
        KindTabs(kind)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (LogUi.show) {
                LogScreen(Modifier.fillMaxSize())
            } else if (kind == SessionKind.Debian && rootfsState !is RootfsState.Ready) {
                InstallPanel(rootfsState) { RootfsManager.install(context) }
            } else {
                key(kind) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx -> createTerminalView(ctx, mods, kind) },
                        onRelease = { v ->
                            if (TerminalSessions.client.view === v) TerminalSessions.client.view = null
                        }
                    )
                }
            }
        }
        if (!LogUi.show) ExtraKeys(mods)
    }
}

@Composable
private fun KindTabs(selected: SessionKind) {
    Row(Modifier.fillMaxWidth()) {
        SessionKind.entries.forEach { k ->
            TextButton(onClick = { TerminalSessions.selected = k; LogUi.show = false }) {
                Text(
                    k.label,
                    fontWeight = if (k == selected && !LogUi.show) FontWeight.Bold else FontWeight.Normal,
                    color = if (k == selected && !LogUi.show) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        LogTabButton()
    }
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

private fun createTerminalView(ctx: Context, mods: TerminalModifiers, kind: SessionKind): TerminalView {
    val view = TerminalView(ctx, null)
    val session = TerminalSessions.session(ctx, kind)
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
