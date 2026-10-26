package com.molina.studio.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.molina.studio.terminal.TerminalActions
import com.molina.studio.terminal.TerminalPrefs
import com.molina.studio.terminal.TerminalSessions
import java.io.File

/** Jembatan Penjelajah Molina: path versi Debian, salin ke clipboard, kirim ke terminal. */
internal object MolinaExplorer {
    private const val SDCARD = "/sdcard"

    private fun base(): String = Environment.getExternalStorageDirectory().absolutePath

    /** Path di dalam Debian PRoot; penyimpanan internal di-bind ke /sdcard. */
    fun debianPath(f: File): String {
        val b = base()
        val p = f.absolutePath
        return when {
            p == b -> SDCARD
            p.startsWith("$b/") -> SDCARD + p.substring(b.length)
            else -> p
        }
    }

    /**
     * Terlihat di Debian: seluruh penyimpanan internal bila "Akses penyimpanan penuh" aktif,
     * selain itu hanya folder kerja molina-studio (sesuai bind PRoot). Kartu SD/USB tidak pernah terlihat.
     */
    fun visibleInDebian(f: File): Boolean {
        val root = if (TerminalPrefs.fullStorageAccess) base() else base() + "/" + TerminalSessions.WORKSPACE_DIR
        val p = f.absolutePath
        return p == root || p.startsWith("$root/")
    }

    fun quote(path: String): String = "'" + path.replace("'", "'\\''") + "'"

    fun copyPath(ctx: Context, f: File) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("molina-path", debianPath(f)))
        Toast.makeText(ctx, "Path disalin: " + debianPath(f), Toast.LENGTH_SHORT).show()
    }

    /** Tempel path (sudah di-quote) ke input shell sesi aktif, tanpa menekan Enter. */
    fun sendToTerminal(ctx: Context, f: File) {
        val em = TerminalSessions.currentSession?.emulator
        if (em == null) {
            Toast.makeText(ctx, "Belum ada sesi terminal aktif. Buka Terminal terlebih dahulu.", Toast.LENGTH_LONG).show()
            return
        }
        em.paste(quote(debianPath(f)) + " ")
        val note = if (visibleInDebian(f)) "Path dikirim ke Terminal" else "Path dikirim, tetapi lokasi ini tidak terlihat di Debian"
        Toast.makeText(ctx, note, Toast.LENGTH_SHORT).show()
    }

    /** Jalankan `cd` ke folder di sesi aktif (langsung dieksekusi). Hanya untuk folder yang terlihat di Debian. */
    fun changeDir(ctx: Context, dir: File) {
        if (TerminalSessions.currentSession?.emulator == null) {
            Toast.makeText(ctx, "Belum ada sesi terminal aktif. Buka Terminal terlebih dahulu.", Toast.LENGTH_LONG).show()
            return
        }
        if (!dir.isDirectory) {
            Toast.makeText(ctx, "Pilih sebuah folder untuk cd.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!visibleInDebian(dir)) {
            Toast.makeText(
                ctx,
                "Folder ini tidak terlihat di Debian. Aktifkan \"Akses penyimpanan penuh\" di Setelan Terminal (berlaku di sesi baru), atau pindahkan ke molina-studio.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        TerminalActions.send("cd " + quote(debianPath(dir)) + "\r")
        Toast.makeText(ctx, "cd " + debianPath(dir), Toast.LENGTH_SHORT).show()
    }
}
