package com.molina.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private enum class Tab(val label: String, val glyph: String) {
    Cinema("Cinema", "\u25B6"),
    Terminal("Terminal", "\u203A_"),
    Editor("Editor", "\u270E"),
    Settings("Setelan", "\u2699")
}

private val MolinaColors = darkColorScheme(
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF0A0A0F),
    primary = Color(0xFFE6B450),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = MolinaColors) {
                var selected by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            Tab.entries.forEachIndexed { i, tab ->
                                NavigationBarItem(
                                    selected = selected == i,
                                    onClick = { selected = i },
                                    icon = { Text(tab.glyph) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                ) { pad ->
                    if (Tab.entries[selected] == Tab.Terminal) {
                        com.molina.studio.terminal.TerminalScreen(Modifier.fillMaxSize().padding(pad))
                        return@Scaffold
                    }
                    Box(
                        Modifier.fillMaxSize().padding(pad),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Molina ${Tab.entries[selected].label}",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
