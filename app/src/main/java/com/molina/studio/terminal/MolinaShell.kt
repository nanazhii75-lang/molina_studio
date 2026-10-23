package com.molina.studio.terminal

import com.molina.core.common.LogLevel
import com.molina.studio.log.LogBus
import com.molina.studio.ui.MolinaTheme
import android.content.Context
import java.io.File

/** Identitas visual shell Molina: banner di sini; warna prompt bersumber dari [PromptTheme]. */
object MolinaShell {
    const val BRAND = "Welcome Molina Terminal"
    const val BRAND_SGR = "1;${MolinaTheme.GOLD_SGR}" // emas Molina
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

    private const val APT_CONF = """// Molina: default apt/dpkg yang aman dan hemat (dibuat otomatis, ditimpa tiap sesi).
Acquire::AllowInsecureRepositories "false";
Acquire::AllowDowngradeToInsecureRepositories "false";
Acquire::Languages "none";
APT::Install-Recommends "false";
APT::Install-Suggests "false";
Dpkg::Options {
  "--force-confdef";
  "--force-confold";
};
"""

    /**
     * Default apt/dpkg guest. Aman: repo tak bertanda tangan ditolak, konfigurasi lokal tidak
     * ditimpa paket. Hemat: tanpa Recommends/Suggests dan tanpa indeks Translation.
     * Hanya menulis bila isinya berbeda; gagal tulis dicatat, tidak menghentikan sesi.
     */
    fun installAptConf(rootfsDir: File) {
        val f = File(rootfsDir, "etc/apt/apt.conf.d/02-molina-defaults")
        runCatching {
            f.parentFile?.mkdirs()
            if (!f.isFile || f.readText() != APT_CONF) f.writeText(APT_CONF)
        }.onFailure {
            LogBus.log("sistem", LogLevel.Warn, "Shell", "Gagal menulis apt.conf Molina: ${it.message}")
        }
    }
}
