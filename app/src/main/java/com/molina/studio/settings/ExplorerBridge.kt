package com.molina.studio.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.molina.studio.terminal.TerminalActions
import com.molina.studio.terminal.TerminalPrefs
import com.molina.studio.terminal.TerminalSessions
import com.molina.cinema.MpvFolder
import com.molina.code.run.ScriptShare
import com.molina.studio.cinema.CinemaActions
import com.molina.studio.code.CodeNav
import com.molina.studio.code.ScriptStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * selain itu hanya molina-studio dan folder aplikasi Molina (sesuai bind PRoot). Kartu SD/USB tidak pernah terlihat.
     */
    fun visibleInDebian(f: File): Boolean {
        // Bind PRoot tetap selama sesi hidup: ikuti sesi aktif, bukan hanya setelan terbaru
        val full = TerminalSessions.fullAccessOf(TerminalSessions.currentSession) ?: TerminalPrefs.fullStorageAccess
        val roots = if (full) listOf(base()) else listOf(
            base() + "/" + TerminalSessions.WORKSPACE_DIR,
            base() + "/" + com.molina.core.common.MolinaPaths.APP_FOLDER,
        )
        val p = f.absolutePath
        return roots.any { p == it || p.startsWith("$it/") }
    }

    fun quote(path: String): String = "'" + path.replace("'", "'\\''") + "'"

    /**
     * Putar di Molina Player (mpv): satu berkas, beberapa berkas (playlist memory://), atau satu folder
     * (playlist folder). Pemindaian folder berjalan di IO; peluncuran activity di thread pemanggil.
     */
    suspend fun play(ctx: Context, files: List<File>): Boolean {
        val dir = files.singleOrNull()?.takeIf { it.isDirectory }
        return when {
            dir != null -> {
                val first = withContext(Dispatchers.IO) { MpvFolder.firstMediaIn(dir) } ?: return false
                CinemaActions.playFolder(ctx, first)
            }
            files.size == 1 -> CinemaActions.playPath(ctx, files[0].absolutePath)
            files.isNotEmpty() -> {
                val uri = withContext(Dispatchers.IO) { MpvFolder.memoryPlaylist(files.map { it.absolutePath }) } ?: return false
                CinemaActions.playTree(ctx, uri)
            }
            else -> false
        }
    }

    /** Buka berkas teks di editor Code (tab Code): dikonsumsi MainActivity lewat [CodeNav.pendingFiles]. */
    fun openInCode(ctx: Context, files: List<File>) {
        val ok = files.filter { it.isFile }.take(10)
        if (ok.isEmpty()) {
            Toast.makeText(ctx, "Pilih berkas teks untuk dibuka di Code.", Toast.LENGTH_SHORT).show()
            return
        }
        CodeNav.pendingFiles = ok
    }

    /** Kirim isi berkas teks ke Script execution. Sumber = path berkas, sama dengan kiriman dari editor Code (tidak menggandakan). */
    fun sendToScripts(f: File): Pair<Boolean, String> {
        if (!f.isFile) return Pair(false, "Pilih satu berkas teks")
        if (f.length() > 1_000_000L) return Pair(false, "Berkas terlalu besar untuk Script execution")
        val text = try {
            f.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            return Pair(false, "Gagal membaca " + f.name)
        }
        if (text.indexOf('\u0000') >= 0) return Pair(false, "Berkas biner tidak bisa dijadikan skrip")
        val r = ScriptStore.upsertFromEditor(ScriptShare(f.name, f.path, text))
        return Pair(r.ok, r.message)
    }

    fun copyPath(ctx: Context, f: File) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("molina-path", debianPath(f)))
        Toast.makeText(ctx, "Path disalin: " + debianPath(f), Toast.LENGTH_SHORT).show()
    }

    /** Tempel path (sudah di-quote) ke input shell sesi aktif, tanpa menekan Enter. */
    fun sendToTerminal(ctx: Context, f: File, enter: Boolean = false) {
        val em = TerminalSessions.currentSession?.emulator
        if (em == null) {
            Toast.makeText(ctx, "Belum ada sesi terminal aktif. Buka Terminal terlebih dahulu.", Toast.LENGTH_LONG).show()
            return
        }
        if (enter && f.isDirectory) {
            changeDir(ctx, f) // folder + Enter = cd
            return
        }
        if (enter) TerminalActions.send(quote(debianPath(f)) + "\r")
        else em.paste(quote(debianPath(f)) + " ")
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
                if (TerminalPrefs.fullStorageAccess && TerminalSessions.fullAccessOf(TerminalSessions.currentSession) == false)
                    "Sesi terminal ini dibuat sebelum Akses penyimpanan penuh aktif. Buka sesi baru (NEW SESSION)."
                else
                    "Folder ini tidak terlihat di Debian. Aktifkan \"Akses penyimpanan penuh\" di Setelan Terminal (berlaku di sesi baru), atau pindahkan ke folder Molina atau molina-studio.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        TerminalActions.send("cd " + quote(debianPath(dir)) + "\r")
        Toast.makeText(ctx, "cd " + debianPath(dir), Toast.LENGTH_SHORT).show()
    }
}
