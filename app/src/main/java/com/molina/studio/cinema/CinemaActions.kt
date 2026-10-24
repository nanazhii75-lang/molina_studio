package com.molina.studio.cinema

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.molina.core.common.PlaylistExtras
import com.molina.core.common.PlaylistPlan
import com.molina.core.common.StreamSearchItem

/** Peluncur Molina Cinema (MPVActivity) dan validasi input. Input pengguna dianggap tidak tepercaya. */
internal object CinemaActions {
    private const val PLAYER = "com.molina.cinema.MPVActivity"
    private const val SETTINGS = "com.molina.cinema.preferences.PreferenceActivity"
    private val SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtmps")
    private val BARE_HOSTS = listOf("www.", "youtu.be/", "youtube.com/", "m.youtube.com/", "music.youtube.com/")

    sealed interface Input {
        class Direct(val uri: Uri) : Input
        /** Bukan URL: diperlakukan sebagai kata kunci pencarian judul. */
        object Query : Input
        /** Mengandung "://" tetapi skema/host tidak didukung. */
        object Invalid : Input
        object Empty : Input
    }

    /** Tambahkan https:// pada tautan tanpa skema yang jelas berupa host web (www., youtu.be, dst.). */
    fun normalize(raw: String): String {
        val t = raw.trim()
        if (t.contains("://")) return t
        return if (BARE_HOSTS.any { t.startsWith(it, ignoreCase = true) }) "https://$t" else t
    }

    fun classify(raw: String): Input {
        val t = normalize(raw)
        if (t.isEmpty()) return Input.Empty
        val u = runCatching { Uri.parse(t) }.getOrNull()
        val scheme = u?.scheme?.lowercase()
        val host = u?.host?.lowercase().orEmpty()
        if (u != null && scheme in SCHEMES && host.isNotEmpty()) return Input.Direct(u)
        return if (t.contains("://")) Input.Invalid else Input.Query
    }

    fun play(ctx: Context, uri: Uri): Boolean = start(
        ctx,
        Intent(Intent.ACTION_VIEW, uri)
            .setClassName(ctx.packageName, PLAYER)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )

    /**
     * Putar hasil yang diketuk dan jadikan hasil lain sebagai playlist (urutan sama dengan daftar).
     * Bila kurang dari 2 hasil valid, jatuh ke pemutaran tunggal.
     */
    fun playResults(ctx: Context, items: List<StreamSearchItem>, picked: StreamSearchItem): Boolean {
        val list = items.filter { PlaylistPlan.isWatchUrl(it.pageUrl) }.take(PlaylistPlan.MAX_ITEMS)
        val idx = list.indexOfFirst { it.pageUrl == picked.pageUrl }
        val uri = Uri.parse(picked.pageUrl)
        if (idx < 0 || list.size < 2) return play(ctx, uri)
        return start(
            ctx,
            Intent(Intent.ACTION_VIEW, uri)
                .setClassName(ctx.packageName, PLAYER)
                .putStringArrayListExtra(PlaylistExtras.URLS, ArrayList(list.map { it.pageUrl }))
                .putStringArrayListExtra(PlaylistExtras.TITLES, ArrayList(list.map { it.title }))
                .putExtra(PlaylistExtras.INDEX, idx)
        )
    }

    fun openSettings(ctx: Context): Boolean =
        start(ctx, Intent().setClassName(ctx.packageName, SETTINGS))

    private fun start(ctx: Context, i: Intent): Boolean =
        try { ctx.startActivity(i); true } catch (e: Exception) { false }
}
