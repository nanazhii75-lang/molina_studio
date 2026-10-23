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
    val Muted = Color(0xFF9AA0A6)
    val CardBg = Color(0xFF14161B)
    val CardLine = Color(0xFF2A2D35)
}
