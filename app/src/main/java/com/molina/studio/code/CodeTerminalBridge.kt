package com.molina.studio.code

import com.molina.studio.terminal.TerminalActions
import com.molina.studio.terminal.TerminalSessions

/**
 * Jembatan Code -> Terminal. Teks ditempel lewat emulator (bracketed paste bila shell mendukung),
 * sehingga teks banyak baris tidak langsung dieksekusi. [execute] true = tekan Enter setelahnya.
 * Mengembalikan pesan galat, atau null bila berhasil (sekaligus pindah ke tab Terminal).
 */
internal object CodeTerminalBridge {
    private const val MAX_CHARS = 20_000

    fun deliver(text: String, execute: Boolean): String? {
        if (text.length > MAX_CHARS) return "Teks terlalu panjang untuk dikirim ke Terminal"
        val em = TerminalSessions.currentSession?.emulator
            ?: return "Terminal belum siap. Buka tab Terminal sekali dulu."
        if (em.isAlternateBufferActive) return "Terminal sedang menjalankan program layar penuh"
        em.paste(text)
        if (execute) TerminalActions.send("\r")
        ScriptNav.openTerminal = true
        return null
    }
}
