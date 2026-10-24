package com.molina.studio.stream

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pilihan mesin stream. yt-dlp (cadangan untuk TikTok, Facebook, dll.) DEFAULT MATI karena satu
 * resolusi memulai proot + python.
 *
 * Satu sumber kebenaran: [ytdlpEnabled]. UI mengamatinya lewat `collectAsState()`, thread latar
 * cukup membaca `ytdlpEnabled.value` (aman lintas thread).
 */
object StreamEnginePrefs {
    private const val FILE = "molina_stream"
    private const val KEY_YTDLP = "ytdlp_enabled"

    private var sp: SharedPreferences? = null
    private val _ytdlpEnabled = MutableStateFlow(false)

    val ytdlpEnabled: StateFlow<Boolean> = _ytdlpEnabled.asStateFlow()

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp = p
        _ytdlpEnabled.value = p.getBoolean(KEY_YTDLP, false)
    }

    fun enableYtdlp(v: Boolean) {
        _ytdlpEnabled.value = v
        sp?.edit()?.putBoolean(KEY_YTDLP, v)?.apply()
    }
}
