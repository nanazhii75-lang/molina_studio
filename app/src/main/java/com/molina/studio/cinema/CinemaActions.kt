package com.molina.studio.cinema

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Peluncur Molina Cinema (MPVActivity) dan validasi input. Input pengguna dianggap tidak tepercaya. */
internal object CinemaActions {
    private const val PLAYER = "com.molina.cinema.MPVActivity"
    private const val SETTINGS = "com.molina.cinema.preferences.PreferenceActivity"
    private val SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtmps")

    sealed interface Input {
        class Direct(val uri: Uri) : Input
        object NeedsExtractor : Input
        object Invalid : Input
    }

    fun classify(raw: String): Input {
        val u = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return Input.Invalid
        val scheme = u.scheme?.lowercase() ?: return Input.Invalid
        val host = u.host?.lowercase().orEmpty()
        if (scheme !in SCHEMES || host.isEmpty()) return Input.Invalid
        // YouTube dan situs lain sama-sama diteruskan ke MPVActivity; StreamLoader/NewPipe yang
        // memutuskan apakah perlu resolve (5d-2a). NeedsExtractor tidak lagi dihasilkan.
        return Input.Direct(u)
    }

    fun play(ctx: Context, uri: Uri): Boolean = start(
        ctx,
        Intent(Intent.ACTION_VIEW, uri)
            .setClassName(ctx.packageName, PLAYER)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )

    fun openSettings(ctx: Context): Boolean =
        start(ctx, Intent().setClassName(ctx.packageName, SETTINGS))

    private fun start(ctx: Context, i: Intent): Boolean =
        try { ctx.startActivity(i); true } catch (e: Exception) { false }
}
