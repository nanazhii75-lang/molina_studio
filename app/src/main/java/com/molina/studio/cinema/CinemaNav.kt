package com.molina.studio.cinema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Permintaan pindah ke tab Cinema (ikon launcher Molina Cinema); dikonsumsi MainActivity. */
internal object CinemaNav {
    var openTab by mutableStateOf(false)
}
