package com.molina.studio.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.molina.studio.log.LogScreen

private enum class Page { Home, Log }

/** Cangkang Settings Hub (fase 7 akan menambah isi); saat ini memuat halaman Log. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    BackHandler(enabled = page != Page.Home) { page = Page.Home }

    when (page) {
        Page.Log -> Column(modifier) {
            TextButton(onClick = { page = Page.Home }) { Text("\u2190 Settings") }
            LogScreen(Modifier.fillMaxSize())
        }
        Page.Home -> Column(modifier.padding(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary)
            SettingsItem("Log", "Log aktivitas mpv, Terminal, dan sistem") { page = Page.Log }
        }
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall)
    }
}
