package com.molina.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.molinaStore: DataStore<Preferences> by preferencesDataStore(name = "molina_settings")

class MolinaSettingsRepository(context: Context) {

    private val store = context.applicationContext.molinaStore

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val LANGUAGE = stringPreferencesKey("language")
        val WORKSPACE = stringPreferencesKey("workspace_path")
        val TERM_FONT = intPreferencesKey("terminal_font_size")
        val HWDEC = stringPreferencesKey("player_hwdec")
        val RESUME = booleanPreferencesKey("resume_playback")
    }

    val settings: Flow<MolinaSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    suspend fun update(transform: (MolinaSettings) -> MolinaSettings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.THEME] = s.themeMode.name
            prefs[Keys.ACCENT] = s.accent.name
            prefs[Keys.LANGUAGE] = s.language
            prefs[Keys.WORKSPACE] = s.workspacePath
            prefs[Keys.TERM_FONT] = s.terminalFontSize
            prefs[Keys.HWDEC] = s.playerHwdec
            prefs[Keys.RESUME] = s.resumePlayback
        }
    }

    private fun Preferences.toSettings(): MolinaSettings {
        val d = MolinaSettings()
        return MolinaSettings(
            themeMode = runCatching { ThemeMode.valueOf(this[Keys.THEME] ?: "") }.getOrDefault(d.themeMode),
            accent = runCatching { MolinaAccent.valueOf(this[Keys.ACCENT] ?: "") }.getOrDefault(d.accent),
            language = this[Keys.LANGUAGE] ?: d.language,
            workspacePath = this[Keys.WORKSPACE] ?: d.workspacePath,
            terminalFontSize = this[Keys.TERM_FONT] ?: d.terminalFontSize,
            playerHwdec = this[Keys.HWDEC] ?: d.playerHwdec,
            resumePlayback = this[Keys.RESUME] ?: d.resumePlayback
        )
    }
}
