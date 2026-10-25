package com.molina.studio

import com.molina.studio.ui.MolinaTheme
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.view.ViewTreeObserver
import com.molina.core.common.AppVisibility
import com.molina.studio.log.LogServer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private enum class Tab(val label: String, val glyph: String, val color: Color) {
    Mpv("mpv", "\u25B6", Color(0xFFB9A7E8)),          // lavender redup: bioskop
    Terminal("Terminal", ">_", Color(0xFF8FCBA6)),      // hijau sage: terminal
    Code("Code", "< >", Color(0xFFFFB300)),             // emas: editor
    Settings("Settings", "\u2699", Color(0xFFCDBFA3))  // champagne: pengaturan
}

/** True saat keyboard layar tampil. Dipakai agar nav bawah tidak makan ruang saat mengetik. */
@Composable
private fun rememberKeyboardVisible(): Boolean {
    val view = LocalView.current
    var visible by remember { mutableStateOf(false) }
    DisposableEffect(view) {
        val rect = Rect()
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            view.getWindowVisibleDisplayFrame(rect)
            val screenH = view.rootView.height
            visible = screenH > 0 && (screenH - rect.bottom) > screenH * 0.15f
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
    return visible
}

private val MolinaColors = darkColorScheme(
    background = MolinaTheme.Background,
    surface = MolinaTheme.Background,
    primary = MolinaTheme.Gold,
    onBackground = MolinaTheme.OnBackground,
    onSurface = MolinaTheme.OnBackground
)

class MainActivity : ComponentActivity() {
    /** Izin storage diminta sekali di awal agar AppPaths siap untuk semua fitur (mpv, editor), bukan hanya Terminal. */
    private val storagePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            AppPaths.ensureIfPermitted(applicationContext)
        }

    override fun onStart() {
        super.onStart()
        AppVisibility.setForeground(true)
    }

    override fun onResume() {
        super.onResume()
        AppPaths.ensureIfPermitted(applicationContext)
    }

    /** FLAG_SECURE + filter sentuhan sesuai sakelar Pengaturan > Keamanan dan privasi. */
    private fun applyPrivacy() {
        val on = com.molina.studio.security.PrivacyPrefs.secureScreen.value
        if (on) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.filterTouchesWhenObscured = on
    }

    override fun onDestroy() {
        com.molina.studio.security.PrivacyPrefs.onChange = null
        super.onDestroy()
    }

    /** Sesi editor Code; dipegang agar draf bisa ditulis sinkron saat Activity berhenti (F3e). */
    private var codeSessionRef: com.molina.code.ui.CodeSession? = null

    override fun onStop() {
        codeSessionRef?.flushDrafts()
        AppVisibility.setForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        LogServer.start()
        applyPrivacy()
        com.molina.studio.security.PrivacyPrefs.onChange = { runOnUiThread { applyPrivacy() } }
        com.molina.studio.terminal.TerminalPrefs.init(applicationContext)
        com.molina.studio.terminal.CrashReporter.install(applicationContext)
        com.molina.studio.terminal.CrashReporter.notifyPending(applicationContext)
        com.molina.studio.terminal.LogcatReporter.setEnabled(
            com.molina.studio.terminal.TerminalPrefs.logcatReporter
        )
        AppExit.attach(this)
        setContent {
            MaterialTheme(colorScheme = MolinaColors) {
                var selected by rememberSaveable { mutableIntStateOf(0) }
                val codeSession = remember {
                    com.molina.code.ui.CodeSession(
                        AppPaths.get(this@MainActivity).workspace.path,
                        java.io.File(filesDir, "code-drafts")
                    ).also {
                        codeSessionRef = it
                        it.scriptExecutor = com.molina.studio.code.ProotScriptExecutor(applicationContext)
                    }
                }
                DisposableEffect(codeSession) { onDispose { codeSession.releaseAll() } }
                val keyboardVisible = rememberKeyboardVisible()
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (!keyboardVisible && !(selected == Tab.Code.ordinal && codeSession.previewFullScreen)) {
                            Column {
                                Box(
                                    Modifier.fillMaxWidth().height(1.dp).background(
                                        Brush.horizontalGradient(
                                            // rail emas, selaras dengan rail toolbar Code
                                            listOf(Color.Transparent, Color(0xFFFFB300).copy(alpha = 0.40f), Color.Transparent)
                                        )
                                    )
                                )
                                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                    Tab.entries.forEachIndexed { i, tab ->
                                        NavigationBarItem(
                                            selected = selected == i,
                                            onClick = { selected = i },
                                            icon = {
                                                Text(tab.glyph, fontWeight = FontWeight.Bold)
                                            },
                                            label = { Text(tab.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = tab.color,
                                                selectedTextColor = tab.color,
                                                indicatorColor = tab.color.copy(alpha = 0.18f),
                                                unselectedIconColor = tab.color.copy(alpha = 0.55f),
                                                unselectedTextColor = tab.color.copy(alpha = 0.55f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { pad ->
                    when (Tab.entries[selected]) {
                        Tab.Terminal -> {
                            com.molina.studio.terminal.TerminalScreen(
                                Modifier.fillMaxSize().padding(pad),
                                onOpenSettings = {
                                    com.molina.studio.settings.SettingsNav.openTerminal = true
                                    selected = Tab.Settings.ordinal
                                }
                            )
                            return@Scaffold
                        }
                        Tab.Mpv -> {
                            com.molina.studio.cinema.CinemaHome(Modifier.fillMaxSize().padding(pad))
                            return@Scaffold
                        }
                        Tab.Settings -> {
                            com.molina.studio.settings.SettingsScreen(Modifier.fillMaxSize().padding(pad))
                            return@Scaffold
                        }
                        Tab.Code -> {
                            com.molina.code.ui.CodeScreen(codeSession, Modifier.fillMaxSize().padding(pad))
                            return@Scaffold
                        }
                        else -> {}
                    }
                    Box(
                        Modifier.fillMaxSize().padding(pad),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Molina ${Tab.entries[selected].label}",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
