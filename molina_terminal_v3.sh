python3 << 'PYEOF'
import os, re, sys

ROOT = os.environ.get("MOLINA_ROOT")
if not ROOT:
    for cand in ("/storage/emulated/0/molina-studio", "/sdcard/molina-studio", os.getcwd()):
        if os.path.isdir(os.path.join(cand, "app", "src", "main")):
            ROOT = cand
            break
if not ROOT or not os.path.isdir(os.path.join(ROOT, "app", "src", "main")):
    sys.exit("ERROR: repo molina-studio tidak ditemukan. Set MOLINA_ROOT=/path/ke/repo")
J = "app/src/main/java/com/molina/studio/"
changed = []

def rd(rel):
    with open(os.path.join(ROOT, rel), encoding="utf-8") as f:
        return f.read()

def wr(rel, text):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    changed.append(rel)

def write_full(rel, text):
    p = os.path.join(ROOT, rel)
    if os.path.isfile(p) and rd(rel) == text:
        return
    wr(rel, text)

def patch(rel, marker, steps):
    """steps: [(old, new)]; dilewati bila marker sudah ada (idempotent)."""
    s = rd(rel)
    if marker in s:
        return
    for old, new in steps:
        if old not in s:
            sys.exit("ERROR: pola tidak ditemukan di %s:\n%s" % (rel, old[:120]))
        s = s.replace(old, new, 1)
    wr(rel, s)
write_full('app/src/main/java/com/molina/studio/terminal/SessionDrawer.kt', r'''package com.molina.studio.terminal

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFE6B450)
private val CloseRed = Color(0xFFFF8A80)

/**
 * Drawer sesi ala Termux: daftar sesi [1] [2] ... (maks. TerminalSessions.MAX_SESSIONS),
 * tombol KEYBOARD dan NEW SESSION, tombol pengaturan, serta tombol ✕ per sesi
 * untuk menutup (kill) tab. Dibuka lewat tombol ≡ (tap atau swipe); ditutup dengan
 * tap latar gelap, swipe ke kiri, atau tombol Back.
 */
@Composable
fun SessionDrawer(open: Boolean, onClose: () -> Unit, onOpenSettings: () -> Unit) {
    if (!open) return
    val ctx = LocalContext.current
    var confirm by remember { mutableStateOf<Int?>(null) }
    val swallow = remember { MutableInteractionSource() }
    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize()) {
        // Latar gelap: tap menutup drawer.
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onClose)
        )
        Column(
            Modifier.fillMaxHeight().width(300.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(interactionSource = swallow, indication = null, onClick = {})
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onHorizontalDrag = { change, dx ->
                            total += dx
                            if (total < -60f) {
                                change.consume()
                                total = 0f
                                onClose()
                            }
                        }
                    )
                }
        ) {
            // Header: pengaturan + penghitung sesi.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clickable { onClose(); onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) { Text("\u2699", fontSize = 22.sp, color = Gold) }
                Spacer(Modifier.weight(1f))
                Text(
                    "${TerminalSessions.sessions.size}/${TerminalSessions.MAX_SESSIONS}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Daftar sesi.
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val tick = TerminalSessions.titleTick
                TerminalSessions.sessions.forEachIndexed { i, s ->
                    val selected = i == TerminalSessions.currentIndex
                    val title = remember(tick, s) {
                        s.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Debian"
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (selected) Gold.copy(alpha = 0.16f) else Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "[${i + 1}] $title",
                            modifier = Modifier.weight(1f)
                                .clickable { TerminalSessions.select(i); onClose() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            color = if (selected) Gold else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            Modifier.size(48.dp).clickable { confirm = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\u2715", color = CloseRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tombol bawah: KEYBOARD dan NEW SESSION.
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrawerButton("KEYBOARD", true, Modifier.weight(1f)) {
                    onClose()
                    toggleKeyboard(ctx)
                }
                DrawerButton("NEW SESSION", TerminalSessions.canAdd(), Modifier.weight(1f)) {
                    when {
                        RootfsManager.state.value !is RootfsState.Ready ->
                            toast(ctx, "Debian belum terpasang")
                        !TerminalSessions.canAdd() ->
                            toast(ctx, "Maksimal ${TerminalSessions.MAX_SESSIONS} sesi")
                        else -> { TerminalSessions.newSession(ctx); onClose() }
                    }
                }
            }
        }
    }

    confirm?.let { i ->
        val last = TerminalSessions.sessions.size <= 1
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Tutup sesi [${i + 1}]?") },
            text = {
                Text(
                    if (last) "Ini sesi terakhir: proses di dalamnya dihentikan dan Molina keluar."
                    else "Proses yang berjalan di sesi ini akan dihentikan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    TerminalSessions.closeSession(i)
                }) { Text("Tutup", color = CloseRed) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun DrawerButton(label: String, enabledLook: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val a = if (enabledLook) 1f else 0.4f
    Box(
        modifier
            .clip(shape)
            .background(Gold.copy(alpha = 0.12f * a), shape)
            .border(1.dp, Gold.copy(alpha = 0.6f * a), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Gold.copy(alpha = a), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

/** Tampilkan atau sembunyikan keyboard layar untuk terminal aktif. */
private fun toggleKeyboard(ctx: Context) {
    val v = TerminalSessions.client.view ?: return
    if (!TerminalPrefs.softKeyboardAllowed(ctx)) {
        toast(ctx, "Soft keyboard dinonaktifkan di Settings")
        return
    }
    v.requestFocus()
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
}
''')
write_full('app/src/main/java/com/molina/studio/terminal/TerminalSessions.kt', r'''package com.molina.studio.terminal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.molina.studio.AppExit
import android.os.Environment
import android.system.Os
import android.system.OsConstants
import com.molina.studio.log.LogBus
import com.molina.studio.log.LogLevel
import com.termux.terminal.TerminalSession
import java.io.File
import kotlin.math.min

/**
 * Daftar sesi Debian (PRoot), maksimal [MAX_SESSIONS]. Sesi tetap hidup walau tab aplikasi
 * berpindah (dijaga TerminalService). Seluruh mutasi daftar terjadi di main thread,
 * kecuali killAll() saat aplikasi keluar.
 */
object TerminalSessions {
    const val MAX_SESSIONS = 5

    /** true: bila sesi terakhir berakhir, matikan semua proses lalu tutup aplikasi. */
    private const val EXIT_APP_ON_SESSION_END = true

    val client = MolinaSessionClient()

    /** Daftar sesi (observable oleh Compose). */
    val sessions = mutableStateListOf<TerminalSession>()

    var currentIndex by mutableIntStateOf(0); private set

    /** Naik tiap judul sesi berubah, agar daftar di drawer dihitung ulang. */
    var titleTick by mutableIntStateOf(0); private set

    /** Sesi aktif sebagai state (dipakai TerminalScreen untuk berpindah tab). */
    val active: TerminalSession? get() = sessions.getOrNull(currentIndex)

    @Volatile var currentSession: TerminalSession? = null
        private set

    @Volatile private var appContext: Context? = null

    fun canAdd(): Boolean = sessions.size < MAX_SESSIONS

    /** Pastikan minimal ada satu sesi; kembalikan sesi aktif. */
    @Synchronized
    fun ensureSession(context: Context): TerminalSession {
        val app = context.applicationContext
        appContext = app
        sessions.removeAll { !it.isRunning }
        if (sessions.isEmpty()) {
            sessions.add(debianShell(app))
            currentIndex = 0
            LogBus.log("terminal", LogLevel.Info, "session", "Sesi Debian [1] dimulai")
        }
        currentIndex = currentIndex.coerceIn(0, sessions.lastIndex)
        val s = sessions[currentIndex]
        currentSession = s
        TerminalService.ensureRunning(app)
        return s
    }

    /** Sesi baru (NEW SESSION). null bila sudah mencapai batas [MAX_SESSIONS]. */
    @Synchronized
    fun newSession(context: Context): TerminalSession? {
        if (!canAdd()) return null
        val app = context.applicationContext
        appContext = app
        val s = debianShell(app)
        sessions.add(s)
        select(sessions.lastIndex)
        LogBus.log("terminal", LogLevel.Info, "session", "Sesi Debian [${sessions.size}] dimulai")
        TerminalService.ensureRunning(app)
        return s
    }

    fun select(index: Int) {
        if (index !in sessions.indices) return
        currentIndex = index
        currentSession = sessions[index]
    }

    fun titleChanged() { titleTick++ }

    /** Tombol ✕ di drawer: bunuh proses sesi (grup proses) lalu tutup tabnya. */
    fun closeSession(index: Int) {
        val s = sessions.getOrNull(index) ?: return
        LogBus.log("terminal", LogLevel.Info, "session", "Sesi [${index + 1}] ditutup pengguna")
        kill(s)
        remove(s, exitDelayMs = 0L)
    }

    /** Dipanggil client saat sebuah sesi berakhir (mis. perintah exit). */
    fun onSessionFinished(finished: TerminalSession) {
        LogBus.log("terminal", LogLevel.Info, "session", "Sesi terminal berakhir")
        remove(finished, exitDelayMs = 800L)
    }

    private fun remove(s: TerminalSession, exitDelayMs: Long) {
        val idx = sessions.indexOf(s)
        if (idx < 0) return // sudah dihapus (mis. lewat tombol ✕)
        sessions.removeAt(idx)
        if (sessions.isEmpty()) {
            currentSession = null
            currentIndex = 0
            onAllSessionsEnded(exitDelayMs)
            return
        }
        currentIndex = when {
            idx < currentIndex -> currentIndex - 1
            idx == currentIndex -> min(idx, sessions.lastIndex)
            else -> currentIndex
        }
        currentSession = sessions[currentIndex]
    }

    private fun onAllSessionsEnded(exitDelayMs: Long) {
        if (EXIT_APP_ON_SESSION_END) {
            AppExit.exit(delayMs = exitDelayMs)
            return
        }
        appContext?.let { TerminalService.requestStop(it) }
    }

    private fun kill(s: TerminalSession) {
        if (!s.isRunning) return
        val pid = s.pid
        if (pid > 1) {
            try { Os.kill(-pid, OsConstants.SIGKILL) } catch (e: Exception) {}
        }
        s.finishIfRunning()
    }

    /** Aksi "Keluar": matikan semua sesi (grup proses + proses utama) dan kosongkan state. */
    @Synchronized
    fun killAll() {
        sessions.toList().forEach { kill(it) }
        sessions.clear()
        currentSession = null
        currentIndex = 0
    }

    private fun debianShell(ctx: Context): TerminalSession {
        TerminalPrefs.init(ctx)
        val nativeDir = ctx.applicationInfo.nativeLibraryDir
        val rootfsDir = RootfsManager.rootfsDir(ctx)
        val rootfs = rootfsDir.absolutePath
        val tmp = File(ctx.cacheDir, "proot-tmp").apply { mkdirs() }
        val guestCwd = "/root" // home default: prompt tampil sebagai user@localhost:~
        val storage = Environment.getExternalStorageDirectory().absolutePath

        // bashrc Molina (prompt berwarna + banner) dan titik mount /storage/emulated/0 di guest.
        MolinaShell.installRc(ctx, File(rootfsDir, "etc/molina/bashrc"))
        MolinaShell.writePromptEnv(rootfsDir, TerminalPrefs.promptTheme)
        File(rootfsDir, "storage/emulated/0").mkdirs()

        val args = arrayOf(
            "proot", "--link2symlink", "--kill-on-exit", "-0",
            "--rootfs=$rootfs", "--cwd=$guestCwd",
            "--bind=/dev", "--bind=/proc", "--bind=/sys",
            "--bind=$storage:/sdcard",
            "--bind=$storage:/storage/emulated/0",
            "/usr/bin/env", "-i",
            "HOME=/root", "TERM=xterm-256color", "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "MOLINA_HOME=${MolinaShell.HOME_DIR}",
            "MOLINA_BRAND=${MolinaShell.BRAND}",
            "MOLINA_BRAND_SGR=${MolinaShell.BRAND_SGR}",
            "MOLINA_PS_HOME=${TerminalPrefs.promptTheme.homeSgr}",
            "MOLINA_PS_OTHER=${TerminalPrefs.promptTheme.otherSgr}",
            "/bin/bash", "--rcfile", "/etc/molina/bashrc", "-i"
        )
        val env = buildList {
            add("PROOT_LOADER=$nativeDir/libproot_loader.so")
            add("PROOT_TMP_DIR=${tmp.absolutePath}")
            add("TMPDIR=${tmp.absolutePath}")
            add("PATH=/system/bin")
            add("HOME=${ctx.filesDir.absolutePath}")
            System.getenv("ANDROID_ROOT")?.let { add("ANDROID_ROOT=$it") }
            System.getenv("ANDROID_DATA")?.let { add("ANDROID_DATA=$it") }
        }.toTypedArray()
        return TerminalSession(
            "$nativeDir/libproot.so", ctx.filesDir.absolutePath, args, env, 2000, client
        )
    }
}
''')
write_full('app/src/main/java/com/molina/studio/terminal/TerminalScreen.kt', r'''package com.molina.studio.terminal

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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointerInput
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
import kotlin.math.abs
import kotlin.math.floor

private fun spToPx(ctx: Context, sp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), ctx.resources.displayMetrics).toInt()

/** Lebar satu sel monospace pada ukuran font tertentu (sama seperti TerminalRenderer). */
private fun cellWidthPx(ctx: Context, sp: Int): Float =
    Paint().apply { typeface = Typeface.MONOSPACE; textSize = spToPx(ctx, sp).toFloat() }.measureText("X")

@Composable
fun TerminalScreen(modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {}) {
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
        TerminalContent(modifier, onOpenSettings)
    }
}

@Composable
private fun TerminalContent(modifier: Modifier, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val mods = remember { TerminalModifiers() }
    remember(context) { RootfsManager.refresh(context) }
    val rootfsState by RootfsManager.state.collectAsState()
    var drawerOpen by remember { mutableStateOf(false) }

    Box(modifier) {
    Column(Modifier.fillMaxSize()) {
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
                        // Pindah sesi (tab) dari drawer.
                        val target = TerminalSessions.active
                        if (target != null && v.currentSession !== target) {
                            v.attachSession(target)
                            v.requestFocus()
                            v.onScreenUpdated()
                        }
                    },
                    onRelease = { v ->
                        if (TerminalSessions.client.view === v) TerminalSessions.client.view = null
                    }
                )
            }
        }
        ExtraKeys(mods) { drawerOpen = true }
    }
    SessionDrawer(
        open = drawerOpen,
        onClose = { drawerOpen = false },
        onOpenSettings = onOpenSettings
    )
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
    val session = TerminalSessions.ensureSession(ctx)
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
private fun ExtraKeys(mods: TerminalModifiers, onOpenDrawer: () -> Unit) {
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
            DrawerKey("\u2261", menu, KeyAmber, onOpenDrawer)
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

/**
 * Tombol ≡: tap atau geser (swipe) membuka drawer sesi; tekan lama membuka menu simbol.
 */
@Composable
private fun RowScope.DrawerKey(
    label: String,
    items: List<Pair<String, () -> Unit>>,
    color: Color,
    onOpenDrawer: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(
        Modifier.weight(1f).pointerInput(Unit) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onHorizontalDrag = { change, dx ->
                    total += dx
                    if (abs(total) > 48f) {
                        change.consume()
                        total = 0f
                        onOpenDrawer()
                    }
                }
            )
        }
    ) {
        KeyButton(label, false, false, color, { open = true }) { onOpenDrawer() }
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
''')
write_full('app/src/main/java/com/molina/studio/settings/SettingsScreen.kt', r'''package com.molina.studio.settings

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
    Home, Terminal, Debugging, TerminalIO, TerminalView, Tampilan, CrashReports, About, Log
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
}

/**
 * Settings Hub. Halaman Terminal meniru struktur Settings Termux (Debugging, Terminal I/O,
 * Terminal View, About) dan menambah "Tampilan Molina" (font, tema prompt), Crash Reporter,
 * serta Logcat Reporter. Menu Donate dan aplikasi pendamping Termux sengaja tidak ada.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(Page.Home) }
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
        Text(
            titleOf(page), style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        when (page) {
            Page.Home -> {
                SettingsItem("Terminal", "Molina Terminal: debugging, I/O, tampilan") { page = Page.Terminal }
                SettingsItem("Log", "Log aktivitas mpv, Terminal, dan sistem") { page = Page.Log }
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
private fun Category(title: String) {
    Text(
        title, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
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
private fun SwitchRow(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
''')

# ---- Prasyarat: v2 harus sudah terpasang ----
if "toggleCtrlLock" not in rd(J + "terminal/Clients.kt"):
    sys.exit("ERROR: jalankan molina_terminal_v2.sh dulu (prasyarat v3).")

# ---- Clients.kt: client multi-sesi ----
patch(J + "terminal/Clients.kt", "TerminalSessions.titleChanged", [
 ("override fun onTextChanged(changedSession: TerminalSession) { view?.onScreenUpdated() }",
  "override fun onTextChanged(changedSession: TerminalSession) {\n        if (changedSession === TerminalSessions.currentSession) view?.onScreenUpdated()\n    }"),
 ("override fun onTitleChanged(changedSession: TerminalSession) {}",
  "override fun onTitleChanged(changedSession: TerminalSession) { TerminalSessions.titleChanged() }"),
 ("TerminalSessions.onSessionFinished()", "TerminalSessions.onSessionFinished(finishedSession)"),
])

# ---- MainActivity.kt: tombol gear di drawer -> Settings > Terminal ----
patch(J + "MainActivity.kt", "SettingsNav.openTerminal", [
 ("com.molina.studio.terminal.TerminalScreen(Modifier.fillMaxSize().padding(pad))",
  "com.molina.studio.terminal.TerminalScreen(\n                                Modifier.fillMaxSize().padding(pad),\n                                onOpenSettings = {\n                                    com.molina.studio.settings.SettingsNav.openTerminal = true\n                                    selected = Tab.Settings.ordinal\n                                }\n                            )"),
])

# ---- progres.md ----
PM = "progres.md"
entry = ("- 2026-10-23: Terminal v3: multi-session maksimal 5 (TerminalSessions berbasis daftar, sesi berakhir/di-kill menutup tab, "
 "sesi terakhir = keluar app), drawer sesi ala Termux (daftar [n], KEYBOARD, NEW SESSION, gear ke Settings > Terminal) "
 "dengan tombol ✕ konfirmasi untuk kill tab, tombol ≡ tap/swipe membuka drawer (tekan lama = menu simbol). Menunggu CI + uji perangkat.\n")
p = os.path.join(ROOT, PM)
if os.path.isfile(p):
    s = rd(PM)
    if "Terminal v3:" not in s:
        k = "\n## Backlog Lintas Fase"
        s = s.replace(k, entry.rstrip("\n") + "\n" + k, 1) if k in s else s.rstrip("\n") + "\n" + entry
        wr(PM, s)

print("Selesai. File berubah:" if changed else "Tidak ada perubahan (sudah terpasang).")
for c in changed:
    print(" -", c)
PYEOF
