package com.molina.studio.stream

import android.content.Context
import android.content.SharedPreferences
import com.molina.core.common.NetworkPolicy
import com.molina.core.common.SearchLang
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
    private const val KEY_LAN = "allow_lan"
    private const val KEY_LOCK_ID = "lock_id"

    private var sp: SharedPreferences? = null
    private val _ytdlpEnabled = MutableStateFlow(false)

    val ytdlpEnabled: StateFlow<Boolean> = _ytdlpEnabled.asStateFlow()

    private val _allowLan = MutableStateFlow(false)

    /** Izinkan alamat jaringan lokal (default mati). Nilainya juga dicerminkan ke [NetworkPolicy]. */
    val allowLan: StateFlow<Boolean> = _allowLan.asStateFlow()

    private val _lockIndonesian = MutableStateFlow(true)

    /** Kunci hasil pencarian ke bahasa Indonesia (default nyala). Dicerminkan ke [SearchLang.enabled]. */
    val lockIndonesian: StateFlow<Boolean> = _lockIndonesian.asStateFlow()

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp = p
        _ytdlpEnabled.value = p.getBoolean(KEY_YTDLP, false)
        _allowLan.value = p.getBoolean(KEY_LAN, false)
        NetworkPolicy.allowLan = _allowLan.value
        _lockIndonesian.value = p.getBoolean(KEY_LOCK_ID, true)
        SearchLang.enabled = _lockIndonesian.value
    }

    fun enableLan(v: Boolean) {
        _allowLan.value = v
        NetworkPolicy.allowLan = v
        sp?.edit()?.putBoolean(KEY_LAN, v)?.apply()
    }

    fun enableYtdlp(v: Boolean) {
        _ytdlpEnabled.value = v
        sp?.edit()?.putBoolean(KEY_YTDLP, v)?.apply()
    }

    fun setLockIndonesian(v: Boolean) {
        _lockIndonesian.value = v
        SearchLang.enabled = v
        sp?.edit()?.putBoolean(KEY_LOCK_ID, v)?.apply()
    }
}
