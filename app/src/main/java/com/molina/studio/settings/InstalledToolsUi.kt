package com.molina.studio.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.molina.studio.packages.BuiltinTools
import com.molina.studio.packages.ToolStore

private val KEY_LINES = Regex("""^\[(ada|tidak ada|tanpa skrip)\]""")
private val ENTRY = Regex("""^\[(ada|tidak ada|tanpa skrip)\] (.*)$""")
private const val YTDLP_NAME = "yt-dlp"
private const val YTDLP_VERSION = "export PATH=/usr/local/bin:/root/.local/bin:/usr/bin:/bin:/usr/sbin:/sbin\nyt-dlp --version"

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

private fun summarize(lines: List<String>): String? =
    if (lines.isEmpty()) null else "Terpasang ${lines.count { it.startsWith("[ada]") }} dari ${lines.size} alat."

/**
 * Menu Debian > "Daftar tool terpasang": mendeteksi alat bawaan (Python 3, Git, FFmpeg), alat buatan
 * pengguna, dan yt-dlp lewat skrip versi masing-masing, lalu menyimpan hasilnya ke kartu Paket.
 */
@Composable
internal fun InstalledToolsDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    remember { ToolStore.init(ctx) }
    val custom by ToolStore.custom.collectAsState()
    val tools = remember { BuiltinTools.all + custom }
    val entries = remember { tools.map { it.name to it.version } + (YTDLP_NAME to YTDLP_VERSION) }
    val run = remember {
        ToolRun(
            "Debian: Daftar tool terpasang", detectScript(entries), 3 * 60_000L,
            "Mendeteksi alat Molina di Debian beserta versinya.", false, KEY_LINES, ::summarize
        )
    }
    ToolRunDialog(run, onClose = onClose) { code, out ->
        if (code == 0) {
            val found = out.lineSequence().map { it.trim() }.mapNotNull { ENTRY.find(it) }.toList()
            if (found.size == entries.size) {
                tools.forEachIndexed { i, t ->
                    val m = found[i]
                    when (m.groupValues[1]) {
                        "ada" -> ToolStore.setCheck(t.id, m.groupValues[2].removePrefix(t.name + " : ").ifBlank { "Terpasang" })
                        "tidak ada" -> ToolStore.setCheck(t.id, "Tidak terdeteksi")
                    }
                }
            }
        }
    }
}
