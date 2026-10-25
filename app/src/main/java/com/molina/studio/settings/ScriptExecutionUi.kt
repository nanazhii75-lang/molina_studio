package com.molina.studio.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.code.preview.ScriptLaunch
import com.molina.studio.code.ScriptItem
import com.molina.studio.code.ScriptRunner
import com.molina.studio.code.ScriptStore
import com.molina.studio.terminal.TerminalSessions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF6CB4FF)
private val Red = Color(0xFFE8776B)
private const val PREVIEW_LINES = 30

/** Formulir tambah/ubah skrip. [id] null = skrip baru. */
private class Draft(val id: String?, title: String, note: String, body: String) {
    var title by mutableStateOf(title)
    var note by mutableStateOf(note)
    var body by mutableStateOf(body)
}

/**
 * Halaman "Script execution": skrip yang dikirim dari editor Code (menu titik tiga > Kirim ke Script
 * Execution) tampil sebagai accordion dengan judul, catatan, dan isi (maks 3.000 baris). Tambah, ubah,
 * hapus, salin, dan jalankan dengan satu ketukan di sesi Molina Terminal.
 */
@Composable
internal fun ScriptsPage() {
    val ctx = LocalContext.current
    val items by ScriptStore.items.collectAsState()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf<Draft?>(null) }
    var toDelete by remember { mutableStateOf<ScriptItem?>(null) }
    var toRun by remember { mutableStateOf<ScriptItem?>(null) }

    Text(
        "Kirim skrip dari editor Code lewat menu titik tiga > Kirim ke Script Execution. Skrip yang sama " +
            "diperbarui, bukan digandakan. Tombol JALANKAN menulis skrip ke Debian lalu mengetiknya di Molina " +
            "Terminal (chmod +x dan Enter otomatis).",
        color = W7D.TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Skrip tersimpan: ${items.size} / ${ScriptStore.MAX_SCRIPTS}",
            color = W7D.NavHeader, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
        )
        ActionText("+ TAMBAH", Blue) {
            expandedId = null
            draft = Draft(null, "", "", "")
        }
    }
    Spacer(Modifier.height(8.dp))

    draft?.takeIf { it.id == null }?.let { d ->
        ScriptEditorCard(d, ctx, onCancel = { draft = null }, onSaved = { id -> draft = null; expandedId = id })
        Spacer(Modifier.height(8.dp))
    }
    if (items.isEmpty() && draft == null) {
        Text(
            "Belum ada skrip. Tekan + TAMBAH atau kirim dari editor Code.",
            color = W7D.TextDim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp)
        )
    }
    items.forEach { item ->
        key(item.id) {
            ScriptAccordion(
                item = item,
                expanded = expandedId == item.id,
                draft = draft?.takeIf { it.id == item.id },
                ctx = ctx,
                onToggle = {
                    expandedId = if (expandedId == item.id) null else item.id
                    if (draft?.id == item.id && expandedId != item.id) draft = null
                },
                onEdit = { draft = Draft(item.id, item.title, item.note, item.body) },
                onCancelEdit = { draft = null },
                onSaved = { draft = null },
                onRun = { toRun = item },
                onDelete = { toDelete = item }
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    toRun?.let { item ->
        val canNew = TerminalSessions.sessions.isNotEmpty() && TerminalSessions.canAdd()
        fun go(newSession: Boolean) {
            toRun = null
            val err = ScriptRunner.run(ctx, item, newSession)
            if (err != null) toast(ctx, err)
        }
        AlertDialog(
            onDismissRequest = { toRun = null },
            title = { Text("Jalankan di Molina Terminal?") },
            text = {
                Text(
                    "\"${item.title}\" (${ScriptLaunch.countLines(item.body)} baris) ditulis ke Debian, lalu diketik " +
                        "ke terminal dengan chmod +x dan Enter, persis seperti Anda mengetiknya sendiri.\n\n" +
                        "Periksa isinya bila bukan skrip buatan Anda. Pilih SESI BARU bila sesi aktif sedang " +
                        "menjalankan sesuatu."
                )
            },
            confirmButton = {
                Row {
                    if (canNew) TextButton(onClick = { go(true) }) { Text("SESI BARU") }
                    TextButton(onClick = { go(false) }) { Text("SESI AKTIF") }
                }
            },
            dismissButton = { TextButton(onClick = { toRun = null }) { Text("BATAL") } }
        )
    }
    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus skrip?") },
            text = { Text("\"${item.title}\" akan dihapus dari Script Execution. Berkas di editor Code tidak berubah.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    if (draft?.id == item.id) draft = null
                    if (expandedId == item.id) expandedId = null
                    ScriptStore.delete(item.id)
                    ScriptRunner.cleanup(ctx, item.id)
                    toast(ctx, "\"${item.title}\" dihapus")
                }) { Text("HAPUS", color = Red) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("BATAL") } }
        )
    }
}

@Composable
private fun ScriptAccordion(
    item: ScriptItem,
    expanded: Boolean,
    draft: Draft?,
    ctx: Context,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaved: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val lines = remember(item.body) { ScriptLaunch.countLines(item.body) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(W7D.Card, shape)
            .border(1.dp, if (expanded) W7D.SelLine else W7D.Hairline, shape)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(item.title, color = W7D.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    item.note.ifBlank { firstLine(item.body) },
                    color = W7D.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(meta(item, lines), color = W7D.TextDim.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Text(if (expanded) "\u25BE" else "\u25B8", color = W7D.NavHeader, fontSize = 16.sp)
        }
        if (!expanded) return@Column
        Box(Modifier.fillMaxWidth().height(1.dp).background(W7D.Hairline))
        if (draft != null) {
            ScriptEditorCard(draft, ctx, onCancel = onCancelEdit, onSaved = { onSaved() }, bare = true)
            return@Column
        }
        Column(Modifier.padding(12.dp)) {
            if (item.note.isNotBlank()) {
                Text(item.note, color = W7D.Ink, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (item.sourceKey != null) {
                val from = if (item.sourceKey.startsWith("tab:")) "tab editor Code" else item.sourceKey
                Text("Dari: $from", color = W7D.TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 8.dp))
            }
            val code = remember(item.body) { codePreview(item.body) }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(W7D.Field)
                    .border(1.dp, W7D.Hairline, RoundedCornerShape(5.dp))
                    .horizontalScroll(rememberScrollState()).padding(10.dp)
            ) {
                Text(code, color = W7D.Ink, fontSize = 11.5.sp, lineHeight = 16.sp, fontFamily = FontFamily.Monospace, softWrap = false)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionText("JALANKAN", Blue, onClick = onRun)
                ActionText("EDIT", W7D.Ink, onClick = onEdit)
                ActionText("SALIN", W7D.Ink) {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(item.title, item.body))
                    toast(ctx, "Isi skrip disalin")
                }
                Spacer(Modifier.weight(1f))
                ActionText("HAPUS", Red, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun ScriptEditorCard(d: Draft, ctx: Context, onCancel: () -> Unit, onSaved: (String?) -> Unit, bare: Boolean = false) {
    val lines = remember(d.body) { ScriptLaunch.countLines(d.body) }
    val over = lines > ScriptLaunch.MAX_LINES
    val canSave = !over && d.title.isNotBlank() && d.body.isNotBlank()
    val shape = RoundedCornerShape(6.dp)
    val base = if (bare) Modifier.fillMaxWidth() else
        Modifier.fillMaxWidth().clip(shape).background(W7D.Card, shape).border(1.dp, W7D.SelLine, shape)
    Column(base.padding(12.dp)) {
        FieldLabel(if (d.id == null) "Skrip baru" else "Ubah skrip")
        FieldLabel("Judul skrip")
        W7Field(d.title, { d.title = it.take(ScriptLaunch.MAX_TITLE) }, singleLine = true)
        FieldLabel("Catatan (fungsi singkat skrip)")
        W7Field(d.note, { d.note = it.take(ScriptLaunch.MAX_NOTE) }, Modifier.height(72.dp))
        FieldLabel("Isi skrip")
        W7Field(d.body, { d.body = it }, Modifier.height(280.dp), mono = true)
        Text(
            "$lines / ${ScriptLaunch.MAX_LINES} baris" + if (over) " \u00B7 melebihi batas, kurangi dulu" else "",
            color = if (over) Red else W7D.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionText("SIMPAN", Blue, enabled = canSave) {
                val r = ScriptStore.save(d.id, d.title, d.note, d.body)
                toast(ctx, r.message)
                if (r.ok) onSaved(r.id)
            }
            ActionText("BATAL", W7D.Ink, onClick = onCancel)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = W7D.NavHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun W7Field(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    mono: Boolean = false
) {
    val shape = RoundedCornerShape(5.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = TextStyle(
            color = W7D.Ink,
            fontSize = if (mono) 12.sp else 14.sp,
            lineHeight = if (mono) 17.sp else 20.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        ),
        cursorBrush = SolidColor(W7D.NavHeader),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
        modifier = modifier.fillMaxWidth().clip(shape).background(W7D.Field, shape).border(1.dp, W7D.Line, shape).padding(10.dp)
    )
}

@Composable
private fun ActionText(label: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(4.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

private fun firstLine(body: String): String =
    body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#!") }.orEmpty()

/** 30 baris pertama; sisanya diringkas supaya accordion tetap ringan untuk skrip ribuan baris. */
private fun codePreview(body: String): String {
    val all = body.lines()
    val shown = all.take(PREVIEW_LINES).joinToString("\n") { if (it.length > 200) it.take(200) + "\u2026" else it }
    val more = all.size - PREVIEW_LINES
    return if (more > 0) "$shown\n\u2026 (+$more baris lagi, tekan EDIT untuk melihat semua)" else shown
}

private fun meta(item: ScriptItem, lines: Int): String {
    val f = SimpleDateFormat("d MMM yyyy HH:mm", Locale("id", "ID"))
    val ran = if (item.lastRunAt > 0) " \u00B7 dijalankan ${f.format(Date(item.lastRunAt))}" else ""
    return "$lines baris \u00B7 diperbarui ${f.format(Date(item.updatedAt))}$ran"
}
