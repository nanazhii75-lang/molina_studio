package com.molina.code.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** [CODE-SEARCH] Satu simbol di Struktur berkas. [line] basis 0, [depth] 0..4. */
internal class OutlineItem(val line: Int, val depth: Int, val kind: String, val name: String)

private class OSpec(val re: Regex, val nameGroup: Int, val kindGroup: Int, val fixedKind: String?)
private class OLang(val specs: List<OSpec>, val unit: Int)

/** Pengurai struktur ringan berbasis regex per ekstensi (tanpa parser penuh): kelas, fungsi, dan judul Markdown. */
internal object Outline {
    private const val MODS_KT = "public|private|protected|internal|open|abstract|override|suspend|inline|data|sealed|enum|annotation|companion|const|lateinit|operator|infix|tailrec|external|actual|expect"
    private val KT = OLang(listOf(OSpec(Regex("""^(\s*)(?:(?:$MODS_KT)\s+)*?(class|interface|object|fun)\s+(?:<[^>]*>\s*)?([A-Za-z_][\w.]*)"""), 3, 2, null)), 4)
    private val PY = OLang(listOf(OSpec(Regex("""^(\s*)(class|async def|def)\s+(\w+)"""), 3, 2, null)), 4)
    private val JS = OLang(listOf(OSpec(Regex("""^(\s*)(?:export\s+)?(?:default\s+)?(?:async\s+)?(function\*?|class)\s+(\w+)"""), 3, 2, null)), 2)
    private val JAVA = OLang(
        listOf(
            OSpec(Regex("""^(\s*)(?:(?:public|private|protected|static|final|abstract|sealed)\s+)*(class|interface|enum|record)\s+(\w+)"""), 3, 2, null),
            OSpec(Regex("""^(\s*)(?:(?:public|private|protected|static|final|abstract|synchronized|default)\s+)+(?:<[^>]+>\s+)?[\w<>\[\],.?]+\s+(\w+)\s*\("""), 2, 0, "fun")
        ), 4
    )
    private val SH = OLang(
        listOf(
            OSpec(Regex("""^(\s*)function\s+([A-Za-z_][\w-]*)"""), 2, 0, "fun"),
            OSpec(Regex("""^(\s*)([A-Za-z_][\w-]*)\s*\(\s*\)\s*\{?"""), 2, 0, "fun")
        ), 2
    )
    private val MD_RE = Regex("""^(#{1,6})\s+(.+?)\s*#*\s*$""")
    private val LANGS = mapOf(
        "kt" to KT, "kts" to KT, "py" to PY, "js" to JS, "jsx" to JS, "ts" to JS, "tsx" to JS, "mjs" to JS,
        "java" to JAVA, "sh" to SH, "bash" to SH
    )

    /** Null bila jenis berkas belum didukung. */
    fun extract(name: String?, text: String): List<OutlineItem>? {
        val ext = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val md = ext == "md" || ext == "markdown"
        val lang = if (md) null else LANGS[ext]
        if (!md && lang == null) return null
        val specs = lang?.specs ?: emptyList()
        val unit = lang?.unit ?: 4
        val out = ArrayList<OutlineItem>()
        var inFence = false
        var n = 0
        for (raw in text.lineSequence()) {
            val i = n++
            if (n > 200_000 || out.size >= 800) break
            if (md) {
                if (raw.trimStart().startsWith("```")) { inFence = !inFence; continue }
                if (inFence) continue
                val m = MD_RE.find(raw) ?: continue
                out.add(OutlineItem(i, m.groupValues[1].length - 1, "#", m.groupValues[2].trim()))
            } else {
                val line = if (raw.length > 300) raw.substring(0, 300) else raw
                for (sp in specs) {
                    val m = sp.re.find(line) ?: continue
                    val indent = m.groupValues[1].replace("\t", "    ").length
                    val kind = sp.fixedKind ?: m.groupValues[sp.kindGroup].let { if (it.startsWith("async")) "def" else it.removeSuffix("*") }
                    out.add(OutlineItem(i, minOf(indent / unit, 4), kind, m.groupValues[sp.nameGroup]))
                    break
                }
            }
        }
        return out
    }
}

/** Struktur berkas (pola panel Structure di Android Studio): daftar simbol tab aktif; ketuk untuk melompat ke barisnya. */
@Composable
internal fun OutlineDialog(tab: EditorTab, onDismiss: () -> Unit) {
    val label = tab.file?.name ?: tab.title
    val symbols = remember(tab.id) { Outline.extract(label, tab.currentTextOrNull(2_000_000) ?: "") }
    val shape = RoundedCornerShape(CodeDimens.DialogCorner)
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().win7DialogFrame(shape)) {
            BasicText(
                text = "Struktur: " + label + (if (symbols != null) " (" + symbols.size + ")" else ""),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                style = TextStyle(color = CodeColors.IconDefault, fontSize = CodeDimens.DialogTitleSize)
            )
            when {
                symbols == null -> BasicText(
                    text = "Jenis berkas ini belum didukung. Didukung: kt, kts, java, py, js/ts, sh, md.",
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = 13.sp)
                )
                symbols.isEmpty() -> BasicText(
                    text = "Tidak ada simbol ditemukan.",
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = 13.sp)
                )
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxH)) {
                    items(symbols) { s ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    tab.editor?.jumpToLineHighlighted(s.line)
                                    onDismiss()
                                }
                                .padding(start = (16 + s.depth * 14).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = s.kind,
                                modifier = Modifier.width(44.dp),
                                maxLines = 1,
                                style = TextStyle(color = CodeColors.Gold.copy(alpha = 0.75f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            )
                            BasicText(
                                text = s.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(color = CodeColors.IconDefault, fontSize = 14.sp)
                            )
                            BasicText(
                                text = (s.line + 1).toString(),
                                style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
