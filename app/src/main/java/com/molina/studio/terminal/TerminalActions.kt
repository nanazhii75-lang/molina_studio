package com.molina.studio.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.molina.terminal.KeyHandler

/** Aksi tombol ekstra; semua dipanggil dari main thread. */
object TerminalActions {
    fun send(s: String) {
        val b = s.toByteArray()
        TerminalSessions.currentSession?.write(b, 0, b.size)
    }

    /** Tulis teks ke prompt tanpa Enter. Teks banyak baris lewat paste agar tidak langsung dijalankan. */
    fun insert(text: String) {
        if ('\n' in text) TerminalSessions.currentSession?.emulator?.paste(text) else send(text)
    }

    /** Keluaran terminal: [maxLines] baris terakhir (null = semua); baris kosong di ujung dibuang. */
    fun outputText(maxLines: Int?): String {
        val all = TerminalSessions.currentSession?.emulator?.screen?.transcriptText ?: return ""
        val lines = all.trimEnd().lines()
        val picked = if (maxLines == null || lines.size <= maxLines) lines else lines.takeLast(maxLines)
        return picked.joinToString("\n")
    }

    fun sendKey(keyCode: Int) {
        val em = TerminalSessions.currentSession?.emulator ?: return
        KeyHandler.getCode(keyCode, 0, em.isCursorKeysApplicationMode, em.isKeypadApplicationMode)
            ?.let { send(it) }
    }

    /** Salin seluruh transkrip (scrollback + layar, termasuk prompt). */
    fun copyAll(ctx: Context): Boolean {
        val text = TerminalSessions.currentSession?.emulator?.screen?.transcriptText
        if (text.isNullOrEmpty()) return false
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("molina-terminal", text))
        return true
    }

    /** Tempel ke input shell, tepat setelah prompt (bracketed paste ditangani emulator). */
    fun paste(ctx: Context) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(ctx)?.toString()
        if (!text.isNullOrEmpty()) TerminalSessions.currentSession?.emulator?.paste(text)
    }

    /**
     * Hapus seluruh scrollback dan layar, cetak ulang banner Molina, lalu minta bash
     * menggambar ulang prompt (beserta ketikan yang belum dieksekusi).
     * Dilewati bila program layar penuh (vim, htop, ...) sedang aktif.
     */
    fun clear(): Boolean {
        val session = TerminalSessions.currentSession ?: return false
        val em = session.emulator ?: return false
        if (em.isAlternateBufferActive) return false
        val seq = ("\u001b[3J\u001b[H\u001b[2J" + MolinaShell.banner + "\r\n").toByteArray()
        em.append(seq, seq.size)
        send(MolinaShell.REDRAW_SEQ)
        TerminalSessions.client.view?.onScreenUpdated()
        return true
    }
}
