package com.molina.studio

import kotlinx.coroutines.flow.MutableStateFlow

/** true selama Activity terlihat; pekerjaan periodik (statistik, dll.) berhenti saat false. */
object AppVisibility {
    val foreground = MutableStateFlow(true)
}
