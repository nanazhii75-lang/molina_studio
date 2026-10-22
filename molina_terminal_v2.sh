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
write_full('app/src/main/java/com/molina/studio/terminal/TerminalPrefs.kt', r'''package com.molina.studio.terminal

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.io.File

/** Tema warna prompt. SGR dipakai bashrc; Color dipakai pratinjau di Settings. */
enum class PromptTheme(
    val id: String,
    val label: String,
    val homeSgr: String,
    val otherSgr: String,
    val homeColor: Color,
    val otherColor: Color
) {
    CinemaGold(
        "cinema_gold", "Cinema Gold",
        "1;38;2;212;175;55", "1;38;2;196;69;54",
        Color(0xFFD4AF37), Color(0xFFC44536)
    ),
    NeonGreen(
        "neon_green", "Neon Green",
        "1;38;2;57;255;20", "1;38;2;255;49;99",
        Color(0xFF39FF14), Color(0xFFFF3163)
    ),
    Classic(
        "classic", "Molina Classic",
        "1;32", "1;31",
        Color(0xFF00CD00), Color(0xFFCD0000)
    );

    companion object {
        fun fromId(id: String?): PromptTheme = entries.firstOrNull { it.id == id } ?: CinemaGold
    }
}

/** Preferensi terminal Molina (SharedPreferences; state-nya observable oleh Compose). */
object TerminalPrefs {
    const val MIN_FONT_SP = 8
    const val MAX_FONT_SP = 14
    const val DEFAULT_FONT_SP = 9

    const val LOG_OFF = 0
    const val LOG_NORMAL = 1
    const val LOG_DEBUG = 2
    const val LOG_VERBOSE = 3

    private const val FILE = "molina_terminal"
    private var sp: SharedPreferences? = null

    var fontSp by mutableIntStateOf(DEFAULT_FONT_SP); private set
    var promptTheme by mutableStateOf(PromptTheme.CinemaGold); private set
    var logLevel by mutableIntStateOf(LOG_NORMAL); private set
    var keyLogging by mutableStateOf(false); private set
    var pluginErrorNotifications by mutableStateOf(true); private set
    var crashNotifications by mutableStateOf(true); private set
    var softKeyboard by mutableStateOf(true); private set
    var softKeyboardOnlyIfNoHardware by mutableStateOf(false); private set
    var marginAdjustment by mutableStateOf(true); private set
    var logcatReporter by mutableStateOf(false); private set

    /** Aman dipanggil berulang; hanya memuat sekali. */
    @Synchronized
    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp = p
        fontSp = p.getInt("font_sp", DEFAULT_FONT_SP).coerceIn(MIN_FONT_SP, MAX_FONT_SP)
        promptTheme = PromptTheme.fromId(p.getString("prompt_theme", null))
        logLevel = p.getInt("log_level", LOG_NORMAL).coerceIn(LOG_OFF, LOG_VERBOSE)
        keyLogging = p.getBoolean("key_logging", false)
        pluginErrorNotifications = p.getBoolean("plugin_error_notifications", true)
        crashNotifications = p.getBoolean("crash_notifications", true)
        softKeyboard = p.getBoolean("soft_keyboard", true)
        softKeyboardOnlyIfNoHardware = p.getBoolean("soft_keyboard_only_if_no_hw", false)
        marginAdjustment = p.getBoolean("margin_adjustment", true)
        logcatReporter = p.getBoolean("logcat_reporter", false)
    }

    private fun putInt(k: String, v: Int) { sp?.edit()?.putInt(k, v)?.apply() }
    private fun putBool(k: String, v: Boolean) { sp?.edit()?.putBoolean(k, v)?.apply() }

    fun setFontSp(v: Int) {
        val c = v.coerceIn(MIN_FONT_SP, MAX_FONT_SP)
        if (c == fontSp) return
        fontSp = c
        putInt("font_sp", c)
    }

    /** Simpan tema, lalu tulis ulang prompt.env agar prompt berikutnya langsung berwarna baru. */
    fun setPromptTheme(ctx: Context, t: PromptTheme) {
        promptTheme = t
        sp?.edit()?.putString("prompt_theme", t.id)?.apply()
        val app = ctx.applicationContext
        Thread {
            runCatching {
                val dir: File = RootfsManager.rootfsDir(app)
                if (dir.isDirectory) MolinaShell.writePromptEnv(dir, t)
            }
        }.apply { isDaemon = true; name = "molina-prompt-theme" }.start()
    }

    fun setLogLevel(v: Int) { logLevel = v.coerceIn(LOG_OFF, LOG_VERBOSE); putInt("log_level", logLevel) }
    fun setKeyLogging(v: Boolean) { keyLogging = v; putBool("key_logging", v) }
    fun setPluginErrorNotifications(v: Boolean) { pluginErrorNotifications = v; putBool("plugin_error_notifications", v) }
    fun setCrashNotifications(v: Boolean) { crashNotifications = v; putBool("crash_notifications", v) }
    fun setSoftKeyboard(v: Boolean) { softKeyboard = v; putBool("soft_keyboard", v) }
    fun setSoftKeyboardOnlyIfNoHardware(v: Boolean) {
        softKeyboardOnlyIfNoHardware = v; putBool("soft_keyboard_only_if_no_hw", v)
    }
    fun setMarginAdjustment(v: Boolean) { marginAdjustment = v; putBool("margin_adjustment", v) }
    fun setLogcatReporter(v: Boolean) { logcatReporter = v; putBool("logcat_reporter", v) }

    /** Keyboard layar boleh tampil? (Soft Keyboard Enabled + Only If No Hardware). */
    fun softKeyboardAllowed(ctx: Context): Boolean {
        if (!softKeyboard) return false
        if (softKeyboardOnlyIfNoHardware) {
            val hasHw = ctx.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
            if (hasHw) return false
        }
        return true
    }
}

/** Logger yang menghormati Log Level di Settings (Off / Normal / Debug / Verbose), seperti Termux. */
object MolinaLog {
    private fun on(min: Int) = TerminalPrefs.logLevel >= min

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (!on(TerminalPrefs.LOG_NORMAL)) return
        if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
    }
    fun w(tag: String, msg: String) { if (on(TerminalPrefs.LOG_NORMAL)) Log.w(tag, msg) }
    fun i(tag: String, msg: String) { if (on(TerminalPrefs.LOG_NORMAL)) Log.i(tag, msg) }
    fun d(tag: String, msg: String) { if (on(TerminalPrefs.LOG_DEBUG)) Log.d(tag, msg) }
    fun v(tag: String, msg: String) { if (on(TerminalPrefs.LOG_VERBOSE)) Log.v(tag, msg) }

    /** Log tombol; hanya bila "Terminal View Key Logging" aktif (dan Log Level bukan Off). */
    fun key(tag: String, msg: String) {
        if (TerminalPrefs.keyLogging && on(TerminalPrefs.LOG_NORMAL)) Log.d(tag, msg)
    }
}
''')
write_full('app/src/main/java/com/molina/studio/terminal/CrashReporter.kt', r'''package com.molina.studio.terminal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.molina.studio.MainActivity
import com.molina.studio.R
import com.molina.studio.log.LogBus
import com.molina.studio.log.LogLevel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crash reporter Molina: menangkap exception yang tidak tertangani, menulis laporan ke
 * files/crash/, lalu (bila "Crash Report Notifications" aktif) menampilkan notifikasi saat
 * aplikasi dibuka lagi. Penanganan crash bawaan sistem tetap dijalankan setelahnya.
 */
object CrashReporter {
    private const val DIR = "crash"
    private const val PENDING = ".pending"
    private const val CHANNEL = "molina_crash"
    private const val NOTIF_ID = 4101

    @Volatile private var installed = false

    fun install(ctx: Context) {
        if (installed) return
        installed = true
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun dir(ctx: Context) = File(ctx.filesDir, DIR).apply { mkdirs() }

    private fun write(ctx: Context, thread: Thread, error: Throwable) {
        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now)
        val d = dir(ctx)
        val version = runCatching {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        }.getOrNull() ?: "?"
        val recent = LogBus.entries.value.takeLast(40).joinToString("\n") {
            "${it.level.code}/${it.source}/${it.tag}: ${it.message}"
        }
        val body = buildString {
            appendLine("Molina crash report")
            appendLine("Waktu     : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(now)}")
            appendLine("Versi     : $version")
            appendLine("Perangkat : ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread    : ${thread.name}")
            appendLine()
            appendLine(Log.getStackTraceString(error))
            appendLine()
            appendLine("--- 40 entri log terakhir ---")
            appendLine(recent)
        }
        File(d, "crash-$stamp.txt").writeText(body)
        File(d, PENDING).writeText(stamp)
        LogBus.log("crash", LogLevel.Error, "uncaught", "${error::class.java.simpleName}: ${error.message}")
    }

    /** Dipanggil saat aplikasi mulai: tampilkan notifikasi untuk crash sebelumnya. */
    fun notifyPending(ctx: Context) {
        val marker = File(dir(ctx), PENDING)
        if (!marker.exists()) return
        marker.delete()
        if (!TerminalPrefs.crashNotifications) return
        runCatching {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Crash Report", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val open = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_molina)
                .setContentTitle("Molina berhenti tak terduga")
                .setContentText("Laporan crash tersimpan: Settings > Terminal > Debugging.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIF_ID, n)
        }
    }

    fun list(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.isFile && f.name.startsWith("crash-") }
            ?.sortedByDescending { it.name } ?: emptyList()

    fun read(f: File): String = runCatching { f.readText() }.getOrDefault("(tidak dapat dibaca)")

    fun delete(f: File) { f.delete() }

    fun clearAll(ctx: Context) { list(ctx).forEach { it.delete() } }

    /** crash-20261023-101500.txt -> 2026-10-23 10:15:00 */
    fun title(f: File): String {
        val m = Regex("""crash-(\d{4})(\d{2})(\d{2})-(\d{2})(\d{2})(\d{2})""").find(f.name)
            ?: return f.name
        val g = m.groupValues
        return "${g[1]}-${g[2]}-${g[3]} ${g[4]}:${g[5]}:${g[6]}"
    }
}
''')
write_full('app/src/main/java/com/molina/studio/terminal/LogcatReporter.kt', r'''package com.molina.studio.terminal

import com.molina.studio.log.LogBus
import com.molina.studio.log.LogLevel

/**
 * Logcat reporter: membaca logcat milik proses aplikasi sendiri (tanpa izin tambahan)
 * dan menyalurkannya ke LogBus (sumber "logcat") sehingga ikut digabung oleh LogGrouping.
 * Default mati; dinyalakan lewat Settings > Terminal > Debugging.
 */
object LogcatReporter {
    private val LINE = Regex("""^\S+\s+\S+\s+\d+\s+\d+\s+([VDIWEFA])\s+(.+?)\s*:\s?(.*)$""")

    @Volatile private var proc: Process? = null

    @Synchronized
    fun setEnabled(on: Boolean) { if (on) start() else stop() }

    @Synchronized
    private fun start() {
        if (proc != null) return
        val p = try {
            ProcessBuilder(
                "logcat", "-v", "threadtime", "-T", "200",
                "--pid=${android.os.Process.myPid()}"
            ).redirectErrorStream(true).start()
        } catch (e: Exception) {
            LogBus.log("logcat", LogLevel.Warn, "reporter", "Gagal memulai logcat: ${e.message}")
            return
        }
        proc = p
        Thread({
            try {
                p.inputStream.bufferedReader().forEachLine { line -> handle(line) }
            } catch (e: Exception) {
                // proses dihentikan (destroy) -> abaikan
            }
        }, "molina-logcat").apply { isDaemon = true }.start()
        LogBus.log("logcat", LogLevel.Info, "reporter", "Logcat reporter aktif")
    }

    @Synchronized
    private fun stop() {
        proc?.destroy()
        proc = null
    }

    private fun handle(line: String) {
        if (line.startsWith("---")) return
        val m = LINE.find(line) ?: return
        val (lv, tag, msg) = m.destructured
        val level = when (lv[0]) {
            'V', 'D' -> LogLevel.Debug
            'I' -> LogLevel.Info
            'W' -> LogLevel.Warn
            else -> LogLevel.Error
        }
        LogBus.log("logcat", level, tag, msg)
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
# ---- Clients.kt: Log level, modifier lock, pinch-zoom, soft keyboard, key logging ----
C = J + "terminal/Clients.kt"
s = rd(C)
if "MolinaLog" not in s:
    s = re.sub(r"(?<![\w.])Log\.([ewidv])\(", r"MolinaLog.\1(", s)
    wr(C, s)
patch(C, "toggleCtrlLock", [
 ("""    var ctrl by mutableStateOf(false)
    var alt by mutableStateOf(false)
}""", """    var ctrl by mutableStateOf(false)
    var alt by mutableStateOf(false)
    var ctrlLocked by mutableStateOf(false)
    var altLocked by mutableStateOf(false)

    /** Tap: sekali pakai; bila sedang dikunci, tap membuka kunci. */
    fun tapCtrl() { if (ctrlLocked) { ctrlLocked = false; ctrl = false } else ctrl = !ctrl }
    fun tapAlt() { if (altLocked) { altLocked = false; alt = false } else alt = !alt }

    /** Long-press: kunci / buka kunci modifier. */
    fun toggleCtrlLock() { ctrlLocked = !ctrlLocked; ctrl = ctrlLocked }
    fun toggleAltLock() { altLocked = !altLocked; alt = altLocked }
}"""),
 ("""        if (mods.ctrl) mods.ctrl = false // CTRL virtual sekali pakai
        if (mods.alt) mods.alt = false   // ALT virtual sekali pakai""",
  """        MolinaLog.key(TAG, "onCodePoint(codePoint=$codePoint, ctrlDown=$ctrlDown)")
        if (mods.ctrl && !mods.ctrlLocked) mods.ctrl = false // sekali pakai kecuali dikunci
        if (mods.alt && !mods.altLocked) mods.alt = false"""),
 ("""    override fun onScale(scale: Float): Float = 1.0f""",
  """    /** Pinch-to-zoom: tiap lonjakan skala menggeser font 1sp (rentang 8-14sp, disimpan). */
    override fun onScale(scale: Float): Float {
        if (scale < 0.9f || scale > 1.1f) {
            TerminalPrefs.setFontSp(TerminalPrefs.fontSp + if (scale > 1f) 1 else -1)
            return 1.0f
        }
        return scale
    }"""),
 ("""    override fun onSingleTapUp(e: MotionEvent) {
        val imm""", """    override fun onSingleTapUp(e: MotionEvent) {
        if (!TerminalPrefs.softKeyboardAllowed(view.context)) return
        val imm"""),
 ("""    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false""",
  """    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
        MolinaLog.key(TAG, "onKeyDown(keyCode=$keyCode, action=${e.action})")
        return false
    }
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean {
        MolinaLog.key(TAG, "onKeyUp(keyCode=$keyCode)")
        return false
    }"""),
])

# ---- MolinaShell.kt: prompt.env ----
patch(J + "terminal/MolinaShell.kt", "writePromptEnv", [
 ("""    /** Salin bashrc Molina dari assets""", """    /** Tulis warna prompt aktif; dibaca bashrc di setiap prompt (perubahan tema langsung berlaku). */
    fun writePromptEnv(rootfsDir: File, theme: PromptTheme) {
        val f = File(rootfsDir, "etc/molina/prompt.env")
        val text = "MOLINA_PS_HOME='${theme.homeSgr}'\\nMOLINA_PS_OTHER='${theme.otherSgr}'\\n"
        f.parentFile?.mkdirs()
        if (!f.isFile || f.readText() != text) f.writeText(text)
    }

    /** Salin bashrc Molina dari assets"""),
])

# ---- TerminalSessions.kt: home ~, tema prompt ----
patch(J + "terminal/TerminalSessions.kt", "TerminalPrefs.init", [
 ("""    private fun debianShell(ctx: Context): TerminalSession {
        val nativeDir""", """    private fun debianShell(ctx: Context): TerminalSession {
        TerminalPrefs.init(ctx)
        val nativeDir"""),
 ("""        val work = workDir()
        val guestCwd = if (work.isDirectory && work.canRead()) MolinaShell.HOME_DIR else "/root"
""", """        val guestCwd = "/root" // home default: prompt tampil sebagai user@localhost:~
"""),
 ("""        MolinaShell.installRc(ctx, File(rootfsDir, "etc/molina/bashrc"))
""", """        MolinaShell.installRc(ctx, File(rootfsDir, "etc/molina/bashrc"))
        MolinaShell.writePromptEnv(rootfsDir, TerminalPrefs.promptTheme)
"""),
 ('"MOLINA_PS_HOME=${MolinaShell.PS_HOME_SGR}"', '"MOLINA_PS_HOME=${TerminalPrefs.promptTheme.homeSgr}"'),
 ('"MOLINA_PS_OTHER=${MolinaShell.PS_OTHER_SGR}"', '"MOLINA_PS_OTHER=${TerminalPrefs.promptTheme.otherSgr}"'),
])

# ---- assets/molina/bashrc: baca prompt.env tiap prompt ----
patch("app/src/main/assets/molina/bashrc", "prompt.env", [
 ("__molina_ps1() {\n", "__molina_ps1() {\n  [ -r /etc/molina/prompt.env ] && . /etc/molina/prompt.env\n"),
])

# ---- MainActivity.kt: init prefs, crash reporter, logcat reporter ----
patch(J + "MainActivity.kt", "CrashReporter.install", [
 ("        LogServer.start()\n", """        LogServer.start()
        com.molina.studio.terminal.TerminalPrefs.init(applicationContext)
        com.molina.studio.terminal.CrashReporter.install(applicationContext)
        com.molina.studio.terminal.CrashReporter.notifyPending(applicationContext)
        com.molina.studio.terminal.LogcatReporter.setEnabled(
            com.molina.studio.terminal.TerminalPrefs.logcatReporter
        )
"""),
])

# ---- LogScreen.kt: filter logcat & crash ----
patch(J + "log/LogScreen.kt", '"logcat" to', [
 ('    "sistem" to "Sistem"\n)', '    "sistem" to "Sistem",\n    "logcat" to "Logcat",\n    "crash" to "Crash"\n)'),
])

# ---- progres.md ----
PM = "progres.md"
entry = ("- 2026-10-23: Terminal v2: home default prompt `user@localhost:~` (cwd /root), font 8-14sp "
 "(pinch-to-zoom + slider Settings, disimpan), long-press CTRL/ALT mengunci modifier (latar menyala + bingkai), "
 "tema prompt Cinema Gold / Neon Green / Classic (prompt.env, berlaku di prompt berikutnya), "
 "Settings > Terminal 1-1 Termux v0.118.0 (Debugging, Terminal I/O, Terminal View, About; tanpa Donate) "
 "plus Crash Reporter dan Logcat Reporter. Menunggu CI + uji perangkat.\n")
p = os.path.join(ROOT, PM)
if os.path.isfile(p):
    s = rd(PM)
    if "Terminal v2:" not in s:
        k = "\n## Backlog Lintas Fase"
        s = s.replace(k, entry.rstrip("\n") + "\n" + k, 1) if k in s else s.rstrip("\n") + "\n" + entry
        wr(PM, s)

print("Selesai. File berubah:" if changed else "Tidak ada perubahan (sudah terpasang).")
for c in changed:
    print(" -", c)
PYEOF
