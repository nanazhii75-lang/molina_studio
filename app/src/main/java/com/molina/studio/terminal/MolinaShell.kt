package com.molina.studio.terminal

import android.content.Context
import java.io.File

/** Identitas visual shell Molina: satu sumber kebenaran untuk banner dan warna prompt. */
object MolinaShell {
    const val BRAND = "Welcome Molina Terminal"
    const val BRAND_SGR = "1;38;2;212;175;55" // kuning emas
    const val PS_HOME_SGR = "1;32"            // hijau
    const val PS_OTHER_SGR = "1;31"           // merah
    const val HOME_DIR = "/storage/emulated/0/molina-studio"
    const val REDRAW_SEQ = "\u001b[99~"       // dipetakan ke redraw-current-line di bashrc

    val banner: String get() = "\u001b[${BRAND_SGR}m$BRAND\u001b[0m"

    /** Salin bashrc Molina dari assets ke rootfs; hanya menulis bila isinya berbeda. */
    fun installRc(ctx: Context, target: File) {
        val text = ctx.assets.open("molina/bashrc").bufferedReader().use { it.readText() }
        target.parentFile?.mkdirs()
        if (!target.isFile || target.readText() != text) target.writeText(text)
    }
}
