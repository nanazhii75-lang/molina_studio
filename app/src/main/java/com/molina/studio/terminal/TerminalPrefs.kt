package com.molina.studio.terminal

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
