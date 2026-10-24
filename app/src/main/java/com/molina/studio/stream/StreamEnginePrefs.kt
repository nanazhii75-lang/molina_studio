package com.molina.studio.stream

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Pilihan mesin stream. yt-dlp (cadangan untuk TikTok, Facebook, dll.) DEFAULT MATI karena satu
 * resolusi memulai proot + python. Dibaca dari thread latar lewat [isYtdlpEnabled] (volatile);
 * [ytdlpEnabled] adalah cermin state Compose untuk UI.
 */
object StreamEnginePrefs {
    private const val FILE = "molina_stream"
    private const val KEY_YTDLP = "ytdlp_enabled"

    private var sp: SharedPreferences? = null
    @Volatile private var flag = false

    var ytdlpEnabled by mutableStateOf(false)
        private set

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp = p
        flag = p.getBoolean(KEY_YTDLP, false)
        ytdlpEnabled = flag
    }

    fun isYtdlpEnabled(): Boolean = flag

    fun setYtdlpEnabled(v: Boolean) {
        flag = v
        ytdlpEnabled = v
        sp?.edit()?.putBoolean(KEY_YTDLP, v)?.apply()
    }
}
