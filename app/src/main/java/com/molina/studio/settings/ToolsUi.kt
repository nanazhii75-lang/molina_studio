package com.molina.studio.settings

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.molina.studio.packages.BuiltinTools
import com.molina.studio.packages.ToolCheck
import com.molina.studio.packages.ToolItem
import com.molina.studio.packages.ToolPurge
import com.molina.studio.packages.ToolStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF6CB4FF)
private val Red = Color(0xFFE8776B)

private enum class ToolAction(val label: String, val timeoutMs: Long, val confirm: Boolean) {
    Install("Pasang", 30 * 60_000L, true),
    Version("Versi", 2 * 60_000L, false),
    Remove("Hapus", 15 * 60_000L, true)
}

private class PendingRun(val tool: ToolItem, val action: ToolAction, val backup: Boolean = false, val previewed: Boolean = false)

private class ToolDraft(val id: String?, name: String, note: String, install: String, version: String, remove: String) {
    var name by mutableStateOf(name)
    var note by mutableStateOf(note)
    var install by mutableStateOf(install)
    var version by mutableStateOf(version)
    var remove by mutableStateOf(remove)
}

private fun scriptOf(t: ToolItem, a: ToolAction): String = when (a) {
    ToolAction.Install -> t.install
    ToolAction.Version -> t.version
    ToolAction.Remove -> t.remove
}

/**
 * Halaman "Paket": alat khusus (pasang, versi, hapus). yt-dlp punya halaman sendiri; alat Debian bawaan
 * (Python 3, Git, FFmpeg) dan alat buatan pengguna memakai tiga skrip yang dijalankan di Debian PRoot.
 * Skrip umum ada di Script execution.
 */
@Composable
internal fun PackagesPage(onOpenYtdlp: () -> Unit) {
    val ctx = LocalContext.current
    remember { ToolStore.init(ctx) }
    val custom by ToolStore.custom.collectAsState()
    val checks by ToolStore.checks.collectAsState()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingRun?>(null) }
    var draft by remember { mutableStateOf<ToolDraft?>(null) }
    var toDrop by remember { mutableStateOf<ToolItem?>(null) }
    var preview by remember { mutableStateOf<ToolItem?>(null) }

    Text(
        "Paket berisi alat khusus: pasang, cek versi, dan hapus. Skrip umum ada di menu Script execution.",
        color = W7D.TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)
    )
    Category("Bawaan Molina")
    W7Link({ W7IconYtdlp() }, "Mesin cadangan (yt-dlp)", "TikTok, Facebook, dll. lewat Debian (default mati)", onOpenYtdlp)
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Alat Debian", color = W7D.NavHeader, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        ActionText("+ TAMBAH ALAT", Blue) { draft = ToolDraft(null, "", "", "", "", "") }
    }
    Spacer(Modifier.height(8.dp))
    (BuiltinTools.all + custom).forEach { tool ->
        key(tool.id) {
            ToolCard(
                tool = tool,
                check = checks[tool.id],
                expanded = expandedId == tool.id,
                onToggle = { expandedId = if (expandedId == tool.id) null else tool.id },
                onAction = { a ->
                    if (scriptOf(tool, a).isBlank()) {
                        toast(ctx, "Skrip ${a.label.lowercase()} belum diisi. Ubah alat untuk mengisinya.")
                    } else {
                        if (a == ToolAction.Remove && tool.builtin && tool.purgePkgs.isNotEmpty()) preview = tool
                        else pending = PendingRun(tool, a)
                    }
                },
                onEdit = { draft = ToolDraft(tool.id, tool.name, tool.note, tool.install, tool.version, tool.remove) },
                onDrop = { toDrop = tool }
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    draft?.let { d ->
        ToolEditorDialog(d, ctx, onCancel = { draft = null }, onSaved = { draft = null })
    }
    preview?.let { t ->
        PurgePreviewDialog(
            t,
            onProceed = { backup ->
                preview = null
                pending = PendingRun(t, ToolAction.Remove, backup, true)
            },
            onClose = { preview = null }
        )
    }
    pending?.let { pr ->
        val note = when (pr.action) {
            ToolAction.Install -> "Memasang ${pr.tool.name} di Debian. Butuh internet."
            ToolAction.Version -> "Memeriksa versi ${pr.tool.name}."
            ToolAction.Remove ->
                if (pr.tool.builtin) {
                    "Menghapus ${pr.tool.name} dari Debian sampai tuntas: purge paket, dependensi yatim, konfigurasi, " +
                        "cache, dan sisa berkas, lalu diverifikasi. Pengaturan pengguna ikut hilang (mis. ~/.gitconfig " +
                        "untuk Git). Aplikasi lain yang bergantung padanya bisa berhenti bekerja."
                } else {
                    "Menjalankan skrip hapus ${pr.tool.name}. Pastikan skripnya membersihkan sisa berkas sendiri."
                }
        }
        ToolRunDialog(
            ToolRun(
                "${pr.tool.name}: ${pr.action.label}",
                (if (pr.backup) ToolPurge.backupScript(pr.tool.name, pr.tool.backupPaths) else "") + scriptOf(pr.tool, pr.action),
                pr.action.timeoutMs,
                note + if (pr.backup) " Konfigurasi dicadangkan dulu ke /sdcard/molina-studio/.molina-backup." else "",
                pr.action.confirm && !pr.previewed
            ),
            onClose = { pending = null }
        ) { code, out ->
            when {
                code == -1 -> {}
                pr.action == ToolAction.Version ->
                    ToolStore.setCheck(pr.tool.id, if (code == 0) firstLine(out) else "Tidak terdeteksi (kode $code)")
                code == 0 -> ToolStore.setCheck(pr.tool.id, "")
            }
        }
    }
    toDrop?.let { t ->
        AlertDialog(
            onDismissRequest = { toDrop = null },
            title = { Text("Buang alat dari daftar?") },
            text = { Text("\"${t.name}\" dibuang dari daftar. Alat yang sudah terpasang di Debian tidak ikut dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    toDrop = null
                    if (expandedId == t.id) expandedId = null
                    ToolStore.delete(t.id)
                    toast(ctx, "\"${t.name}\" dibuang")
                }) { Text("BUANG", color = Red) }
            },
            dismissButton = { TextButton(onClick = { toDrop = null }) { Text("BATAL") } }
        )
    }
}

@Composable
private fun ToolCard(
    tool: ToolItem,
    check: ToolCheck?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAction: (ToolAction) -> Unit,
    onEdit: () -> Unit,
    onDrop: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val sub = check?.takeIf { it.text.isNotBlank() }?.let { "${it.text} \u00B7 ${stamp(it.at)}" }
        ?: tool.note.ifBlank { "Pasang, versi, hapus" }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(W7D.Card, shape)
            .border(1.dp, if (expanded) W7D.SelLine else W7D.Hairline, shape)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(tool.name, color = W7D.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, color = W7D.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (expanded) "\u25BE" else "\u25B8", color = W7D.NavHeader, fontSize = 16.sp)
        }
        if (!expanded) return@Column
        Box(Modifier.fillMaxWidth().height(1.dp).background(W7D.Hairline))
        Column(Modifier.padding(12.dp)) {
            if (tool.note.isNotBlank()) {
                Text(tool.note, color = W7D.Ink, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionText("PASANG", Blue) { onAction(ToolAction.Install) }
                ActionText("VERSI", W7D.Ink) { onAction(ToolAction.Version) }
                ActionText("HAPUS", Red) { onAction(ToolAction.Remove) }
            }
            if (!tool.builtin) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ActionText("UBAH", W7D.Ink, onClick = onEdit)
                    Spacer(Modifier.weight(1f))
                    ActionText("BUANG", W7D.TextDim, onClick = onDrop)
                }
            }
        }
    }
}

@Composable
private fun ToolEditorDialog(d: ToolDraft, ctx: Context, onCancel: () -> Unit, onSaved: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (d.id == null) "Alat baru" else "Ubah alat") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FieldLabel("Nama alat")
                ToolField(d.name, { d.name = it.take(ToolStore.MAX_NAME) }, singleLine = true)
                FieldLabel("Catatan (opsional)")
                ToolField(d.note, { d.note = it.take(ToolStore.MAX_NOTE) }, singleLine = true)
                FieldLabel("Skrip pasang")
                ToolField(d.install, { d.install = it }, Modifier.height(90.dp), mono = true)
                FieldLabel("Skrip versi")
                ToolField(d.version, { d.version = it }, Modifier.height(60.dp), mono = true)
                FieldLabel("Skrip hapus")
                ToolField(d.remove, { d.remove = it }, Modifier.height(90.dp), mono = true)
                Text(
                    "Dijalankan di Debian dengan sh. Contoh pasang: apt install -y htop. " +
                        "Skrip panjang lebih baik dibuat di Script execution.",
                    color = W7D.TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = d.name.isNotBlank(), onClick = {
                val r = ToolStore.save(d.id, d.name, d.note, d.install, d.version, d.remove)
                toast(ctx, r.message)
                if (r.ok) onSaved()
            }) { Text("SIMPAN") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("BATAL") } }
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = W7D.NavHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun ToolField(
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

private fun firstLine(out: String): String =
    out.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }?.take(80) ?: "OK"

private fun stamp(at: Long): String = SimpleDateFormat("d MMM HH:mm", Locale("id", "ID")).format(Date(at))
