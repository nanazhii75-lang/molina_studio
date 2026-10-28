package com.molina.studio

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.molina.studio.code.ScriptItem

/**
 * Pintasan skrip di layar utama (pinned shortcut), bukan shortcut dinamis: kuota launcher sudah terpakai
 * Cinema, Terminal, dan shortcut dinamis. Intent HANYA membawa id skrip (isi/judul tidak ikut karena shortcut
 * dapat dibaca launcher), dan MainActivity selalu menampilkan dialog konfirmasi sebelum menjalankan.
 */
internal object ScriptShortcuts {
    const val EXTRA_RUN = "molina_run_script"
    private fun shortcutId(id: String) = "molina_script_$id"

    /** Pesan siap tampil sebagai Toast. */
    fun pin(ctx: Context, item: ScriptItem): String {
        val app = ctx.applicationContext
        val sm = app.getSystemService(ShortcutManager::class.java) ?: return "Pintasan tidak didukung perangkat ini"
        if (!sm.isRequestPinShortcutSupported) return "Launcher tidak mendukung pintasan di layar utama"
        val info = ShortcutInfo.Builder(app, shortcutId(item.id))
            .setShortLabel(item.title.take(12))
            .setLongLabel(item.title.take(40))
            .setIcon(Icon.createWithResource(app, R.mipmap.ic_shortcut_code))
            .setIntent(Intent(app, MainActivity::class.java).setAction(Intent.ACTION_MAIN).putExtra(EXTRA_RUN, item.id))
            .build()
        return if (runCatching { sm.requestPinShortcut(info, null) }.getOrDefault(false)) {
            "Konfirmasi pintasan \"${item.title}\" di layar utama"
        } else {
            "Gagal meminta pintasan"
        }
    }

    /** Nonaktifkan pintasan yang sudah disematkan saat skrip dihapus. */
    fun onDeleted(ctx: Context, id: String) {
        runCatching {
            ctx.applicationContext.getSystemService(ShortcutManager::class.java)
                ?.disableShortcuts(listOf(shortcutId(id)), "Skrip sudah dihapus")
        }
    }
}
