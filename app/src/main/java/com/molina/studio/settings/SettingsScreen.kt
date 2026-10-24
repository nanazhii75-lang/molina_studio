package com.molina.studio.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.studio.log.LogScreen
import com.molina.studio.terminal.CrashReporter
import com.molina.studio.terminal.LogcatReporter
import com.molina.studio.terminal.PromptTheme
import com.molina.studio.terminal.TerminalPrefs
import java.io.File
import kotlin.math.roundToInt

/** Permintaan navigasi dari luar Settings (mis. tombol ⚙ di drawer sesi terminal). */
object SettingsNav {
    var openTerminal by mutableStateOf(false)
}

private enum class Page {
    Home, Terminal, Debugging, TerminalIO, TerminalView, Tampilan, CrashReports, About, Log, Stream, Network
}

private fun parentOf(p: Page): Page? = when (p) {
    Page.Home -> null
    Page.Debugging, Page.TerminalIO, Page.TerminalView, Page.Tampilan -> Page.Terminal
    Page.CrashReports -> Page.Debugging
    else -> Page.Home
}

private fun titleOf(p: Page): String = when (p) {
    Page.Home -> "Settings"
    Page.Terminal -> "Molina Terminal"
    Page.Debugging -> "Debugging"
    Page.TerminalIO -> "Terminal I/O"
    Page.TerminalView -> "Terminal View"
    Page.Tampilan -> "Tampilan Molina"
    Page.CrashReports -> "Crash Reporter"
    Page.About -> "About"
    Page.Log -> "Log"
    Page.Stream -> "Mesin cadangan (yt-dlp)"
    Page.Network -> "Keamanan dan privasi"
}

/**
 * Settings Hub. Halaman Terminal meniru struktur Settings Termux (Debugging, Terminal I/O,
 * Terminal View, About) dan menambah "Tampilan Molina" (font, tema prompt), Crash Reporter,
 * serta Logcat Reporter. Menu Donate dan aplikasi pendamping Termux sengaja tidak ada.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    var showRefresh by remember { mutableStateOf(false) }
    BackHandler(enabled = page != Page.Home) { page = parentOf(page) ?: Page.Home }
    LaunchedEffect(SettingsNav.openTerminal) {
        if (SettingsNav.openTerminal) {
            page = Page.Terminal
            SettingsNav.openTerminal = false
        }
    }

    if (page == Page.Log) {
        Column(modifier) {
            TextButton(onClick = { page = Page.Home }) { Text("\u2190 Settings") }
            LogScreen(Modifier.fillMaxSize())
        }
        return
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        val up = parentOf(page)
        if (up != null) {
            TextButton(onClick = { page = up }) { Text("\u2190 ${titleOf(up)}") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                titleOf(page), style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
            )
            if (page == Page.Home) RefreshIconButton { showRefresh = true }
        }
        if (showRefresh) SystemRefreshDialog { showRefresh = false }
        when (page) {
            Page.Home -> {
                SettingsItem("Terminal", "Molina Terminal: debugging, I/O, tampilan") { page = Page.Terminal }
                SettingsItem("Log", "Log aktivitas mpv, Terminal, dan sistem") { page = Page.Log }
                SettingsItem("Mesin cadangan (yt-dlp)", "TikTok, Facebook, dll. lewat Debian (default mati)") { page = Page.Stream }
                SettingsItem("Keamanan dan privasi", "Kebijakan URL, jaringan lokal (LAN), layar privat") { page = Page.Network }
                SettingsItem("About", "Versi dan lisensi") { page = Page.About }
            }
            Page.Terminal -> {
                SettingsItem("Debugging", "Preferences for debugging") { page = Page.Debugging }
                SettingsItem("Terminal I/O", "Preferences for terminal I/O") { page = Page.TerminalIO }
                SettingsItem("Terminal View", "Preferences for terminal view") { page = Page.TerminalView }
                SettingsItem("Tampilan Molina", "Ukuran font dan tema warna prompt") { page = Page.Tampilan }
            }
            Page.Debugging -> DebuggingPage(
                onOpenCrash = { page = Page.CrashReports },
                onOpenLog = { page = Page.Log }
            )
            Page.TerminalIO -> TerminalIoPage()
            Page.TerminalView -> TerminalViewPage()
            Page.Tampilan -> TampilanPage()
            Page.CrashReports -> CrashReportsPage()
            Page.About -> AboutPage()
            Page.Log -> {}
            Page.Stream -> StreamEnginePage()
            Page.Network -> NetworkSecurityPage()
        }
    }
}

// ---------- Debugging ----------

private val LOG_LEVELS = listOf("Off", "Normal", "Debug", "Verbose")

@Composable
private fun DebuggingPage(onOpenCrash: () -> Unit, onOpenLog: () -> Unit) {
    var showLevel by remember { mutableStateOf(false) }
    Category("Logging")
    SettingsItem("Log Level", LOG_LEVELS[TerminalPrefs.logLevel]) { showLevel = true }
    SwitchRow(
        "Terminal View Key Logging",
        if (TerminalPrefs.keyLogging) "Logcat logs will have entries for terminal view keys."
        else "Logs will not have entries for terminal view keys. (Default)",
        TerminalPrefs.keyLogging
    ) { TerminalPrefs.setKeyLogging(it) }
    SwitchRow(
        "Plugin Error Notifications",
        "Molina belum memiliki plugin; nilai disimpan untuk fase mpv dan Code.",
        TerminalPrefs.pluginErrorNotifications
    ) { TerminalPrefs.setPluginErrorNotifications(it) }
    SwitchRow(
        "Crash Report Notifications",
        if (TerminalPrefs.crashNotifications) "Show notifications for crash reports. (Default)"
        else "Disable notifications for crash reports.",
        TerminalPrefs.crashNotifications
    ) { TerminalPrefs.setCrashNotifications(it) }

    Category("Molina Reporter")
    SettingsItem("Crash Reporter", "Lihat, salin, atau hapus laporan crash", onOpenCrash)
    SwitchRow(
        "Logcat Reporter",
        "Salurkan logcat aplikasi ke halaman Log (sumber: logcat), digabung oleh LogGrouping.",
        TerminalPrefs.logcatReporter
    ) {
        TerminalPrefs.setLogcatReporter(it)
        LogcatReporter.setEnabled(it)
    }
    SettingsItem("Buka Log", "Tampilkan log terkumpul", onOpenLog)

    if (showLevel) {
        AlertDialog(
            onDismissRequest = { showLevel = false },
            confirmButton = { TextButton(onClick = { showLevel = false }) { Text("Tutup") } },
            title = { Text("Log Level") },
            text = {
                Column {
                    LOG_LEVELS.forEachIndexed { i, name ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                TerminalPrefs.setLogLevel(i); showLevel = false
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = TerminalPrefs.logLevel == i, onClick = {
                                TerminalPrefs.setLogLevel(i); showLevel = false
                            })
                            Text(name)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun CrashReportsPage() {
    val ctx = LocalContext.current
    var files by remember { mutableStateOf(CrashReporter.list(ctx)) }
    var open by remember { mutableStateOf<File?>(null) }

    if (files.isEmpty()) {
        Text(
            "Belum ada laporan crash.", modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
    files.forEach { f ->
        SettingsItem(CrashReporter.title(f), "${f.length()} byte") { open = f }
    }
    if (files.isNotEmpty()) {
        TextButton(onClick = { CrashReporter.clearAll(ctx); files = emptyList() }) {
            Text("Hapus semua")
        }
    }

    open?.let { f ->
        val text = remember(f) { CrashReporter.read(f) }
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(CrashReporter.title(f)) },
            text = {
                Box(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(text, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { copyText(ctx, text) }) { Text("Salin") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        CrashReporter.delete(f); files = CrashReporter.list(ctx); open = null
                    }) { Text("Hapus") }
                    TextButton(onClick = { open = null }) { Text("Tutup") }
                }
            }
        )
    }
}

private fun copyText(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("molina-crash", text))
    Toast.makeText(ctx, "Laporan disalin", Toast.LENGTH_SHORT).show()
}

// ---------- Terminal I/O ----------

@Composable
private fun TerminalIoPage() {
    Category("Keyboard")
    SwitchRow(
        "Soft Keyboard Enabled",
        if (TerminalPrefs.softKeyboard) "Soft keyboard will be enabled. (Default)"
        else "Soft keyboard will be disabled.",
        TerminalPrefs.softKeyboard
    ) { TerminalPrefs.setSoftKeyboard(it) }
    SwitchRow(
        "Soft Keyboard Only If No Hardware",
        if (TerminalPrefs.softKeyboardOnlyIfNoHardware)
            "Soft keyboard will be enabled only if no hardware keyboard is connected."
        else "Soft keyboard will be enabled even if hardware keyboard is connected. (Default)",
        TerminalPrefs.softKeyboardOnlyIfNoHardware
    ) { TerminalPrefs.setSoftKeyboardOnlyIfNoHardware(it) }

    Category("Penyimpanan")
    SwitchRow(
        "Akses penyimpanan penuh",
        if (TerminalPrefs.fullStorageAccess)
            "Seluruh /sdcard terlihat di dalam Debian. Berlaku di sesi baru."
        else "Hanya /sdcard/molina-studio yang terlihat di dalam Debian. (Default) Berlaku di sesi baru.",
        TerminalPrefs.fullStorageAccess
    ) { TerminalPrefs.setFullStorageAccess(it) }
}

// ---------- Terminal View ----------

@Composable
private fun TerminalViewPage() {
    Category("View")
    SwitchRow(
        "Terminal Margin Adjustment",
        if (TerminalPrefs.marginAdjustment) "Terminal margin adjustment will be enabled. (Default)"
        else "Terminal margin adjustment will be disabled.",
        TerminalPrefs.marginAdjustment
    ) { TerminalPrefs.setMarginAdjustment(it) }
}

// ---------- Tampilan Molina ----------

@Composable
private fun TampilanPage() {
    val ctx = LocalContext.current
    val theme = TerminalPrefs.promptTheme
    val sp = TerminalPrefs.fontSp

    Category("Ukuran font")
    Text(
        "${sp}sp  (${TerminalPrefs.MIN_FONT_SP}\u2013${TerminalPrefs.MAX_FONT_SP}sp)",
        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium
    )
    Slider(
        value = sp.toFloat(),
        onValueChange = { TerminalPrefs.setFontSp(it.roundToInt()) },
        valueRange = TerminalPrefs.MIN_FONT_SP.toFloat()..TerminalPrefs.MAX_FONT_SP.toFloat(),
        steps = TerminalPrefs.MAX_FONT_SP - TerminalPrefs.MIN_FONT_SP - 1
    )
    Text(
        "user@localhost:~ $", fontFamily = FontFamily.Monospace, fontSize = sp.sp,
        fontWeight = FontWeight.Bold, color = theme.homeColor
    )
    Text(
        "Di terminal, cubit dua jari (pinch) juga mengubah ukuran ini, per 1sp.",
        modifier = Modifier.padding(top = 6.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(Modifier.height(12.dp))
    Category("Tema warna prompt")
    PromptTheme.entries.forEach { t ->
        Row(
            Modifier.fillMaxWidth().clickable { TerminalPrefs.setPromptTheme(ctx, t) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = t == theme, onClick = { TerminalPrefs.setPromptTheme(ctx, t) })
            Column {
                Text(t.label, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "user@localhost:~ $", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = t.homeColor
                    )
                    Text(
                        "user@localhost:/etc $", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = t.otherColor
                    )
                }
            }
        }
    }
    Text(
        "Warna baru tampil pada prompt berikutnya (tekan Enter di terminal).",
        modifier = Modifier.padding(top = 6.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodySmall
    )
}

// ---------- About ----------

@Composable
private fun AboutPage() {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    SettingsItem("Molina Studio", "Versi $version") {}
    SettingsItem(
        "Terminal emulator",
        "Berbasis Termux v0.118.0 (terminal-emulator, terminal-view; GPLv3). Lihat THIRD_PARTY_NOTICES.md."
    ) {}
}

// ---------- Komponen bersama ----------

@Composable
internal fun Category(title: String) {
    Text(
        title, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge
    )
}

@Composable
internal fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)) {
        Text(
            title, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun SwitchRow(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title, color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                summary, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
