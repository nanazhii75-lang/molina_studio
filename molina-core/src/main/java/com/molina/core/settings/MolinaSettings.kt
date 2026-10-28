package com.molina.core.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Aksen Home Theater: Cinema = amber hangat, Terminal = teal. */
enum class MolinaAccent(val argb: Long) {
    CINEMA(0xFFE8B04A),
    TERMINAL(0xFF2DD4BF)
}

data class MolinaSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: MolinaAccent = MolinaAccent.CINEMA,
    val language: String = "id",
    val workspacePath: String = "/storage/emulated/0/molina-studio",
    val terminalFontSize: Int = 14,
    val playerHwdec: String = "mediacodec-copy",
    val resumePlayback: Boolean = true
)
