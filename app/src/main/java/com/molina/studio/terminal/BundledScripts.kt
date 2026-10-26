package com.molina.studio.terminal

import android.content.Context
import com.molina.core.common.LogLevel
import com.molina.core.common.MolinaDir
import com.molina.studio.AppPaths
import com.molina.studio.log.LogBus
import java.io.File

/**
 * Skrip bawaan di assets/molina/ yang disalin ke /sdcard/Molina/Scripts agar terlihat di Debian.
 * Tidak pernah menimpa: berkas yang sudah ada (mungkin diedit pengguna) dibiarkan.
 */
internal object BundledScripts {
    private val NAMES = listOf("molina_dl.py")

    /** Operasi disk: panggil di luar main thread, setelah izin storage ada. Gagal tidak melempar. */
    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val dir = AppPaths.get(app).dir(MolinaDir.Scripts)
        if (!dir.isDirectory && !dir.mkdirs()) return
        for (name in NAMES) {
            val target = File(dir, name)
            if (target.exists()) continue
            val tmp = File(dir, "$name.tmp")
            val ok = runCatching {
                app.assets.open("molina/$name").use { input -> tmp.outputStream().use { input.copyTo(it) } }
                tmp.renameTo(target)
            }.getOrDefault(false)
            if (ok) {
                LogBus.log("sistem", LogLevel.Info, "Skrip", "Skrip bawaan disalin: Scripts/$name")
            } else {
                tmp.delete()
                LogBus.log("sistem", LogLevel.Warn, "Skrip", "Skrip bawaan tidak bisa disalin: $name")
            }
        }
    }
}
