package com.molina.studio.terminal

import android.content.Context
import java.io.File

/** Identitas visual shell Molina: banner di sini; warna prompt bersumber dari [PromptTheme]. */
object MolinaShell {
    const val BRAND = "Welcome Molina Terminal"
    const val BRAND_SGR = "1;38;2;212;175;55" // kuning emas
    const val HOME_DIR = "/storage/emulated/0/molina-studio"
    const val REDRAW_SEQ = "\u001b[99~"       // dipetakan ke redraw-current-line di bashrc

    val banner: String get() = "\u001b[${BRAND_SGR}m$BRAND\u001b[0m"

    /** Tulis warna prompt aktif; dibaca bashrc di setiap prompt (perubahan tema langsung berlaku). */
    fun writePromptEnv(rootfsDir: File, theme: PromptTheme) {
        val f = File(rootfsDir, "etc/molina/prompt.env")
        val text = "MOLINA_PS_HOME='${theme.homeSgr}'\nMOLINA_PS_OTHER='${theme.otherSgr}'\n"
        f.parentFile?.mkdirs()
        if (!f.isFile || f.readText() != text) f.writeText(text)
    }

    /** Salin bashrc Molina dari assets ke rootfs; hanya menulis bila isinya berbeda. */
    fun installRc(ctx: Context, target: File) {
        val text = ctx.assets.open("molina/bashrc").bufferedReader().use { it.readText() }
        target.parentFile?.mkdirs()
        if (!target.isFile || target.readText() != text) target.writeText(text)
    }
}
