package com.molina.studio.terminal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.Toast
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

    Column(modifier) {
        StatusBar()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (rootfsState !is RootfsState.Ready) {
                InstallPanel(rootfsState) { RootfsManager.install(context) }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> createTerminalView(ctx, mods) },
                    onRelease = { v ->
                        if (TerminalSessions.client.view === v) TerminalSessions.client.view = null
                    }
                )
            }
        }
        ExtraKeys(mods)
    }
}

@Composable
private fun StatusBar() {
    val ctx = LocalContext.current
    val stats by remember(ctx) { SystemStats.flow(ctx) }.collectAsState(StatsSnapshot())
    Text(
        stats.format(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
            Key("ESC") { TerminalActions.send("\u001b") }
            MenuKey("\u2261", menu)
            Key("\u2014") { TerminalActions.send("-") }
            Key("HOME") { TerminalActions.sendKey(KeyEvent.KEYCODE_MOVE_HOME) }
            Key("\u2191") { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_UP) }
            Key("CLEAR") { TerminalActions.clear() }
            Key("COPY") {
                val ok = TerminalActions.copyAll(ctx)
                Toast.makeText(
                    ctx, if (ok) "Seluruh teks terminal disalin" else "Tidak ada teks untuk disalin",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Key("\u21C6") { TerminalActions.send("\t") }
            Key("CTRL", active = mods.ctrl) { mods.ctrl = !mods.ctrl }
            Key("ALT", active = mods.alt) { mods.alt = !mods.alt }
            Key("\u2190") { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_LEFT) }
            Key("\u2193") { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_DOWN) }
            Key("\u2192") { TerminalActions.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT) }
            Key("PASTE") { TerminalActions.paste(ctx) }
        }
    }
}

@Composable
private fun RowScope.Key(label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.weight(1f)) { KeyButton(label, active, onClick) }
}

@Composable
private fun RowScope.MenuKey(label: String, items: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.weight(1f)) {
        KeyButton(label, false) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() })
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, active: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            maxLines = 1,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
