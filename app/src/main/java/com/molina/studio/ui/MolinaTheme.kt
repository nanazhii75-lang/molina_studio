package com.molina.studio.ui

import androidx.compose.ui.graphics.Color

/** Token desain Molina: satu sumber warna merek dan status untuk seluruh UI dan shell. */
object MolinaTheme {
    // Emas merek, acuan visual #E6B450. Nilai ARGB dan SGR (ANSI truecolor) harus tetap sama.
    const val GOLD_ARGB = 0xFFE6B450.toInt()
    const val GOLD_SGR = "38;2;230;180;80"

    val Gold = Color(GOLD_ARGB)
    val Background = Color(0xFF0A0A0F)
    val OnBackground = Color(0xFFEDEDED)
    val Good = Color(0xFF69F0AE)
    val Warn = Color(0xFFFFD54F)
    val Bad = Color(0xFFFF8A80)
    /** Aksen Molina Cinema (ikon putar, penanda baris log mpv). */
    val Cinema = Color(0xFFB388FF)
    val Muted = Color(0xFF9AA0A6)
    val CardBg = Color(0xFF14161B)
    val CardLine = Color(0xFF2A2D35)

    /** Palet Penjelajah Molina Cinema (dulu token Win7). Garis tombol utama memakai emas merek. */
    object Explorer {
        val FrameTop = Color(0xFF1A2230)
        val FrameBottom = Color(0xFF0A0F16)
        val FrameLine = Color(0xFF2E4A68)
        val Face = Color(0xFF000000)
        val NavBg = Color(0xFF07090D)
        val NavHeader = Color(0xFF8FC4EE)
        val Ink = Color(0xFFEAEFF5)
        val TextDim = Color(0xFF9AA5B1)
        val Line = Color(0xFF5A6675)
        val Field = Color(0xFF3A4654)
        val Hairline = Color(0xFF26303B)
        val SelFill = Color(0xFF12263A)
        val SelLine = Color(0xFF3C7FB1)
        val Folder = Color(0xFFF3CF6E)
        val FolderDark = Color(0xFFD9A93B)
        val CloseTop = Color(0xFFE8A598)
        val CloseBottom = Color(0xFFC4392B)
        val BtnTop = Color(0xFF1E252E)
        val BtnBottom = Color(0xFF10151B)
        val BtnLine = Color(0xFF4A5666)
        val DefaultLine = MolinaTheme.Gold
        val Input = Color(0xFF0F1318)
        val BarTop = Color(0xFF14181E)
        val BarBottom = Color(0xFF05070A)
    }
}
