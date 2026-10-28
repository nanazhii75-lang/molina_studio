package com.molina.code.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.molina.code.preview.PythonTerminalRun

private enum class PyRunMode { HEREDOC, FILE }

private const val MAX_HEREDOC_CHARS = 20_000

/**
 * [CODE-PYRUN] Dialog "Jalankan Python di Terminal". Opsi 1: bungkus isi dengan python3 << 'PYEOF' (tanpa berkas).
 * Opsi 2: python3 nama.py; pengguna cukup mengisi nama, ekstensi .py dideteksi/ditambah otomatis.
 * Pratinjau perintah selalu tampil; penulisan berkas bisa diurungkan dari menu titik tiga.
 */
@Composable
internal fun PythonRunDialog(session: CodeSession, tab: EditorTab, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(PyRunMode.HEREDOC) }
    var name by remember { mutableStateOf(PythonTerminalRun.suggestStem(tab.file?.name ?: tab.title)) }
    val focus = remember { FocusRequester() }
    val shape = RoundedCornerShape(CodeDimens.DialogCorner)
    val hasSelection = remember { session.pythonHasSelection() }
    val source = remember { session.pythonSourceText() }

    val parsed = PythonTerminalRun.normalizeName(name)
    val heredoc = PythonTerminalRun.heredoc(source)
    val target = session.pythonTarget(tab, parsed.name)
    val overwrite = mode == PyRunMode.FILE && target != null && target.exists() && target != tab.file

    val tooLong = mode == PyRunMode.HEREDOC && heredoc != null && heredoc.length > MAX_HEREDOC_CHARS
    val canRun = when (mode) {
        PyRunMode.HEREDOC -> heredoc != null && !tooLong
        PyRunMode.FILE -> parsed.name != null
    }
    val preview = when (mode) {
        PyRunMode.HEREDOC -> heredoc?.let { PythonTerminalRun.preview(it) } ?: "(isi kosong)"
        PyRunMode.FILE -> "python3 " + (parsed.name ?: "nama.py")
    }

    fun run() {
        if (!canRun) return
        if (mode == PyRunMode.HEREDOC) session.runPythonHeredoc() else session.runPythonFile(name)
        onDismiss()
    }

    LaunchedEffect(mode) { if (mode == PyRunMode.FILE) runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .widthIn(max = 340.dp)
                .win7DialogFrame(shape)
                .padding(20.dp)
        ) {
            BasicText("Jalankan Python di Terminal", style = TextStyle(color = CodeColors.Gold, fontSize = CodeDimens.DialogTitleSize))
            Spacer(Modifier.height(8.dp))

            PyOption(
                selected = mode == PyRunMode.HEREDOC,
                title = "Heredoc: python3 << 'PYEOF'",
                subtitle = (if (hasSelection) "Hanya seleksi" else "Seluruh isi tab") + " dikirim apa adanya; tanpa berkas. input() tidak berfungsi.",
                onClick = { mode = PyRunMode.HEREDOC }
            )
            PyOption(
                selected = mode == PyRunMode.FILE,
                title = "Berkas: python3 nama.py",
                subtitle = "Isi tab ditulis ke berkas lalu dijalankan. Berkas lama dicadangkan dan bisa diurungkan.",
                onClick = { mode = PyRunMode.FILE }
            )

            if (mode == PyRunMode.FILE) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(PythonTerminalRun.MAX_NAME + 3) },
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        singleLine = true,
                        textStyle = TextStyle(color = CodeColors.TextPrimary, fontSize = CodeDimens.FindTextSize),
                        cursorBrush = SolidColor(CodeColors.Gold),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { run() })
                    )
                    BasicText(
                        text = if (name.endsWith(".py", ignoreCase = true)) "" else ".py",
                        style = TextStyle(color = CodeColors.IconDim, fontSize = CodeDimens.FindTextSize)
                    )
                }
                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(CodeDimens.FindUnderline).background(CodeColors.Gold))
                if (hasSelection) PyNote("Seleksi diabaikan; seluruh isi tab ditulis ke berkas.", CodeColors.LabelChampagne)
                parsed.error?.let { if (name.isNotBlank()) PyNote(it, CodeColors.Danger) }
                if (overwrite) PyNote(parsed.name + " sudah ada dan akan ditimpa (bisa diurungkan).", CodeColors.Danger)
            }
            if (tooLong) PyNote("Isi terlalu panjang untuk heredoc (maks. 20.000 karakter). Pakai opsi Berkas.", CodeColors.Danger)

            Spacer(Modifier.height(10.dp))
            BasicText(
                text = preview,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeColors.IconDim.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                maxLines = 5,
                style = TextStyle(color = CodeColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            )

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BasicText(
                    text = "BATAL",
                    modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp),
                    style = TextStyle(color = CodeColors.LabelChampagne, fontSize = 14.sp)
                )
                BasicText(
                    text = if (overwrite) "TIMPA & JALANKAN" else "JALANKAN",
                    modifier = Modifier.clickable(enabled = canRun, onClick = { run() }).padding(12.dp),
                    style = TextStyle(color = if (canRun) CodeColors.Gold else CodeColors.IconDim, fontSize = 14.sp)
                )
            }
        }
    }
}

@Composable
private fun PyOption(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.padding(top = 2.dp).size(18.dp).border(1.5.dp, if (selected) CodeColors.Gold else CodeColors.IconDim, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(9.dp).background(CodeColors.Gold, CircleShape))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = TextStyle(color = CodeColors.TextPrimary, fontSize = 14.sp))
            BasicText(subtitle, style = TextStyle(color = CodeColors.LabelChampagne, fontSize = 12.sp))
        }
    }
}

@Composable
private fun PyNote(text: String, color: androidx.compose.ui.graphics.Color) {
    BasicText(text, modifier = Modifier.padding(top = 6.dp), style = TextStyle(color = color, fontSize = 12.sp))
}
