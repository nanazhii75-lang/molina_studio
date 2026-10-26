package com.molina.studio.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.code.run.RunHandle
import com.molina.core.common.AppLog
import com.molina.core.common.LogLevel
import com.molina.studio.packages.BuiltinTools
import com.molina.studio.packages.ToolStore

private val ENTRY = Regex("""^\[(ada|tidak ada|tanpa skrip)\] (.*)$""")
private const val YTDLP_NAME = "yt-dlp"
private const val YTDLP_VERSION = "export PATH=/usr/local/bin:/root/.local/bin:/usr/bin:/bin:/usr/sbin:/sbin\nyt-dlp --version"

private val DotOk = Color(0xFF4CAF50)
private val DotMissing = Color(0xFF8A8F98)
private val DotNoScript = Color(0xFFE8A35F)

private class ToolRow(val name: String, val state: String, val version: String)

private fun shq(s: String): String = "'" + s.replace("'", "'\\''") + "'"

/** Satu skrip sh: info Debian lalu satu baris per alat ("[ada] Nama : versi" / "[tidak ada] Nama"). */
private fun detectScript(entries: List<Pair<String, String>>): String = buildString {
    append("printf '== Debian ==\\n'\n")
    append("printf 'Rilis : %s\\n' \"`cat /etc/debian_version 2>/dev/null || echo ?`\"\n")
    append("printf 'Paket apt terpasang : %s\\n' \"`dpkg -l 2>/dev/null | grep -c '^ii' || true`\"\n")
    append("printf '\\n== Alat Molina ==\\n'\n")
    entries.forEachIndexed { i, (name, script) ->
        val n = shq(name)
        if (script.isBlank()) {
            append("printf '[tanpa skrip] %s\\n' $n\n")
        } else {
            append("cat > /tmp/.molina_v$i.sh << 'MOLINA_EOF'\n")
            append(script.trim()).append("\nMOLINA_EOF\n")
            append("if sh -e /tmp/.molina_v$i.sh > /tmp/.molina_v.out 2>&1; then\n")
            append("  printf '[ada] %s : %s\\n' $n \"`grep -m1 . /tmp/.molina_v.out | cut -c1-80`\"\n")
            append("else\n  printf '[tidak ada] %s\\n' $n\nfi\n")
        }
    }
    append("rm -f /tmp/.molina_v*.sh /tmp/.molina_v.out\n")
}

/**
 * Menu Debian > "Daftar tool terpasang": mendeteksi alat bawaan (Python 3, Git, FFmpeg), alat buatan
 * pengguna, dan yt-dlp, lalu menampilkannya sebagai daftar berwarna (hijau = terpasang, abu-abu = tidak ada,
 * kuning = skrip versi belum diisi) dan menyimpan versinya ke kartu Paket.
 */
@Composable
internal fun InstalledToolsDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    remember { ToolStore.init(ctx) }
    val custom by ToolStore.custom.collectAsState()
    val tools = remember { BuiltinTools.all + custom }
    val entries = remember { tools.map { it.name to it.version } + (YTDLP_NAME to YTDLP_VERSION) }
    var running by remember { mutableStateOf(true) }
    var output by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<ToolRow>>(emptyList()) }
    var handle by remember { mutableStateOf<RunHandle?>(null) }
    val run = remember { ToolRun("Debian: Daftar tool terpasang", detectScript(entries), 3 * 60_000L, "", false) }

    LaunchedEffect(Unit) {
        handle = startShell(
            ctx, run,
            onChunk = { output = (output + it.replace("\r", "")).takeLast(20_000) },
            onDone = { r ->
                running = false
                val ok = r.startError == null && !r.cancelled && !r.timedOut && r.exitCode == 0
                val found = output.lineSequence().map { it.trim() }.mapNotNull { ENTRY.find(it) }.toList()
                if (!ok) {
                    failure = describe(r)
                } else if (found.size != entries.size) {
                    failure = "Keluaran deteksi tidak lengkap."
                } else {
                    rows = found.mapIndexed { i, m ->
                        val name = entries[i].first
                        ToolRow(name, m.groupValues[1], m.groupValues[2].removePrefix("$name : "))
                    }
                    tools.forEachIndexed { i, t ->
                        val row = rows[i]
                        when (row.state) {
                            "ada" -> ToolStore.setCheck(t.id, row.version.ifBlank { "Terpasang" })
                            "tidak ada" -> ToolStore.setCheck(t.id, "Tidak terdeteksi")
                        }
                    }
                    AppLog.log("system", LogLevel.Info, "tool", "Daftar tool: ${rows.count { it.state == "ada" }} dari ${rows.size} terpasang")
                }
            }
        )
    }

    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val info = output.lineSequence().map { it.trim() }
        .filter { it.startsWith("Rilis :") || it.startsWith("Paket apt terpasang :") }.toList()
    AlertDialog(
        onDismissRequest = { if (!running) onClose() },
        title = { Text("Daftar tool terpasang") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    running -> Text("Mendeteksi alat di Debian\u2026", color = dim, fontSize = 13.sp)
                    failure != null -> {
                        Text(failure.orEmpty(), color = DotNoScript, fontSize = 13.sp)
                        if (output.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(output.takeLast(600), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = dim)
                        }
                    }
                    else -> {
                        info.forEach { Text(it, color = dim, fontSize = 12.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Terpasang ${rows.count { it.state == "ada" }} dari ${rows.size} alat",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        rows.forEach { ToolStateRow(it, dim) }
                    }
                }
            }
        },
        confirmButton = {
            if (running) TextButton(onClick = { handle?.cancel() }) { Text("Berhenti") }
            else TextButton(onClick = onClose) { Text("Tutup") }
        },
        dismissButton = {
            TextButton(enabled = !running, onClick = {
                val body = rows.joinToString("\n") {
                    "[${it.state}] ${it.name}" + if (it.version.isNotBlank()) " : ${it.version}" else ""
                }
                copyOutput(ctx, (info + body).filter { it.isNotBlank() }.joinToString("\n"))
            }) { Text("Salin") }
        }
    )
}

@Composable
private fun ToolStateRow(row: ToolRow, dim: Color) {
    val color = when (row.state) {
        "ada" -> DotOk
        "tidak ada" -> DotMissing
        else -> DotNoScript
    }
    val sub = when (row.state) {
        "ada" -> row.version.ifBlank { "Terpasang" }
        "tidak ada" -> "Tidak terpasang"
        else -> "Skrip versi belum diisi"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = dim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
