package com.molina.studio.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.core.common.Assurance
import com.molina.core.common.BundledComponent
import com.molina.core.common.BundledComponents
import com.molina.core.common.ComponentRole
import com.molina.engine.newpipe.NewPipeInfo
import com.molina.studio.automation.AutoPrefs
import com.molina.studio.automation.AutoState
import com.molina.studio.packages.ToolCheck
import com.molina.studio.packages.ToolStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BundleBlue = Color(0xFF6CB4FF)
private val BundleGreen = Color(0xFF7BC77E)
private val BundleAmber = Color(0xFFE0B04A)

/** id komponen Debian -> id alat bawaan di Paket (BuiltinTools). */
private fun debianToolId(id: String): String? = when (id) {
    "python3" -> "b-python3"
    "git" -> "b-git"
    "ffmpeg" -> "b-ffmpeg"
    else -> null
}

private fun stampOf(at: Long): String = SimpleDateFormat("d MMM HH:mm", Locale("id", "ID")).format(Date(at))

private fun versionText(c: BundledComponent, s: AutoState, check: ToolCheck?): String = when {
    c.id == "ytdlp" -> s.ytdlpInstalled.ifEmpty { "belum terpasang" }
    c.version.isNotEmpty() -> c.version
    check != null && check.text.isNotBlank() -> check.text
    debianToolId(c.id) != null -> "versi belum diperiksa"
    else -> "versi mengikuti apt"
}

private fun updateNote(c: BundledComponent, s: AutoState): String? = when {
    c.id == "newpipe" && s.newpipeOutdated ->
        "Pembaruan tersedia: ${NewPipeInfo.VERSION} \u2192 ${s.newpipeLatest}. Perlu rilis Molina baru."
    c.id == "ytdlp" && s.ytdlpOutdated ->
        "Pembaruan tersedia: ${s.ytdlpInstalled} \u2192 ${s.ytdlpLatest}. Perbarui di halaman yt-dlp."
    else -> null
}

private fun trustColor(a: Assurance): Color = when {
    a.verified -> BundleGreen
    a == Assurance.Asset -> W7D.TextDim
    else -> BundleAmber
}

@Composable
private fun TrustDot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color, CircleShape))
}

@Composable
private fun RolePill(role: ComponentRole) {
    val shape = RoundedCornerShape(8.dp)
    val strong = role == ComponentRole.Wajib
    Text(
        role.label, color = if (strong) W7D.NavHeader else W7D.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(shape).background(if (strong) W7D.SelFill else W7D.Card, shape)
            .border(1.dp, if (strong) W7D.SelLine else W7D.Hairline, shape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun BundleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        label, color = if (selected) W7D.Ink else W7D.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(shape).background(if (selected) W7D.SelFill else W7D.Card, shape)
            .border(1.dp, if (selected) W7D.SelLine else W7D.Hairline, shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun BundleAction(label: String, onClick: () -> Unit) {
    Text(
        label, color = BundleBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 3.dp)) {
        Text(label, color = W7D.NavHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(84.dp))
        Text(value, color = W7D.Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

/**
 * Settings > Paket > Komponen bundel (gaya kartu Aero Gelap seperti kartu alat): ringkasan keamanan, saring menurut
 * peran, lalu satu kartu per komponen. Sumber data: [BundledComponents]; tidak ada koneksi jaringan di sini.
 */
@Composable
internal fun BundledComponentsSection(onOpenYtdlp: () -> Unit, onCheckVersion: (String) -> Unit) {
    val ctx = LocalContext.current
    remember { AutoPrefs.init(ctx) }
    val st by AutoPrefs.state.collectAsState()
    val checks by ToolStore.checks.collectAsState()
    val all = remember { BundledComponents.all(NewPipeInfo.VERSION) }
    val counts = remember { BundledComponents.roleCounts(all) }
    val report = remember { BundledComponents.securityReport(all) }
    var filter by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val role = ComponentRole.values().firstOrNull { it.name == filter }
    val shown = BundledComponents.filter(all, role)
    val shape = RoundedCornerShape(6.dp)

    Category("Komponen bundel")
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(W7D.SelFill, W7D.Card)), shape)
            .border(1.dp, W7D.Hairline, shape).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrustDot(if (report.requiredWithoutHash.isEmpty()) BundleGreen else BundleAmber)
            Spacer(Modifier.width(8.dp))
            Text("Ringkasan keamanan", color = W7D.NavHeader, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(report.headline(), color = W7D.Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Text(
            if (report.requiredWithoutHash.isEmpty()) "Semua komponen Wajib punya hash terverifikasi atau kode sumber di repo."
            else "Perlu ditinjau (Wajib tanpa hash): " + report.requiredWithoutHash.joinToString(", "),
            color = if (report.requiredWithoutHash.isEmpty()) W7D.TextDim else BundleAmber,
            fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "CI menggagalkan build bila klaim keamanan tidak punya pin nyata.",
            color = W7D.TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BundleChip("Semua ${all.size}", role == null) { filter = "" }
        ComponentRole.values().forEach { r ->
            BundleChip("${r.label} ${counts[r] ?: 0}", role == r) { filter = r.name }
        }
    }
    Spacer(Modifier.height(10.dp))

    shown.forEach { c ->
        val expanded = open == c.id
        val toolId = debianToolId(c.id)
        val check = toolId?.let { checks[it] }
        val update = updateNote(c, st)
        val sub = versionText(c, st, check) + " \u00b7 " + c.origin.label
        Column(
            Modifier.fillMaxWidth().clip(shape).background(W7D.Card, shape)
                .border(1.dp, if (expanded) W7D.SelLine else W7D.Hairline, shape)
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { open = if (expanded) null else c.id }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrustDot(trustColor(c.assurance))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(c.name, color = W7D.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sub, color = W7D.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (update != null) {
                    W7Badge("Baru", true)
                    Spacer(Modifier.width(6.dp))
                }
                RolePill(c.role)
                Spacer(Modifier.width(8.dp))
                Text(if (expanded) "\u25BE" else "\u25B8", color = W7D.NavHeader, fontSize = 16.sp)
            }
            if (expanded) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(W7D.Hairline))
                Column(Modifier.padding(12.dp)) {
                    if (update != null) {
                        Text(update, color = W7D.NavHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    DetailLine("Kegunaan", c.usedBy)
                    DetailLine("Asal", c.origin.label)
                    DetailLine("Sumber", c.source)
                    DetailLine("Lisensi", c.license)
                    DetailLine("Keamanan", c.assurance.label)
                    if (check != null && check.text.isNotBlank()) DetailLine("Dicek", stampOf(check.at))
                    Text(c.note, color = W7D.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    if (c.id == "ytdlp") BundleAction("BUKA HALAMAN YT-DLP", onOpenYtdlp)
                    if (toolId != null) BundleAction("PERIKSA VERSI") { onCheckVersion(toolId) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
