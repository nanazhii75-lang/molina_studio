package com.molina.studio.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.molina.core.common.BundledComponent
import com.molina.core.common.BundledComponents
import com.molina.core.common.ComponentRole
import com.molina.engine.newpipe.NewPipeInfo
import com.molina.studio.automation.AutoPrefs
import com.molina.studio.automation.AutoState

private fun versionText(c: BundledComponent, s: AutoState): String = when (c.id) {
    "ytdlp" -> s.ytdlpInstalled.ifEmpty { "belum terpasang" }
    else -> c.version.ifEmpty { "versi mengikuti apt" }
}

private fun updateNote(c: BundledComponent, s: AutoState): String? = when {
    c.id == "newpipe" && s.newpipeOutdated -> "Pembaruan tersedia: ${NewPipeInfo.VERSION} \u2192 ${s.newpipeLatest} (perlu rilis Molina baru)"
    c.id == "ytdlp" && s.ytdlpOutdated -> "Pembaruan tersedia: ${s.ytdlpInstalled} \u2192 ${s.ytdlpLatest}"
    else -> null
}

private fun detailOf(c: BundledComponent, s: AutoState): String = buildString {
    updateNote(c, s)?.let { append(it).append("\n") }
    append("Dipakai oleh: ").append(c.usedBy).append("\n")
    append("Sumber: ").append(c.source).append("\n")
    append("Lisensi: ").append(c.license).append("\n")
    append("Keamanan: ").append(c.assurance.label).append("\n")
    append(c.note)
}

/** Settings > Paket: inventaris komponen pihak ketiga (versi, peran, asal, jaminan keamanan). Sumber: [BundledComponents]. */
@Composable
internal fun BundledComponentsSection() {
    val ctx = LocalContext.current
    remember { AutoPrefs.init(ctx) }
    val st by AutoPrefs.state.collectAsState()
    val items = remember { BundledComponents.all(NewPipeInfo.VERSION) }
    val counts = remember { BundledComponents.roleCounts(items) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }

    Category("Komponen bundel")
    Text(
        "Wajib ${counts[ComponentRole.Wajib]} \u00b7 Pendukung ${counts[ComponentRole.Pendukung]} \u00b7 " +
            "Opsional ${counts[ComponentRole.Opsional]}. Ketuk untuk melihat asal dan jaminan keamanan.",
        color = W7D.TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)
    )
    items.forEach { c ->
        val outdated = updateNote(c, st) != null
        BadgeOverlay(if (outdated) "Baru" else null, true) {
            SettingsItem(
                c.name + " \u00b7 " + c.role.label,
                versionText(c, st) + " \u00b7 " + c.origin.label
            ) { open = if (open == c.id) null else c.id }
        }
        if (open == c.id) {
            Text(
                detailOf(c, st),
                color = W7D.TextDim, fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}
