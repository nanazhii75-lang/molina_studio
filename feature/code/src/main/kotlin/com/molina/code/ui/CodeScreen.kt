package com.molina.code.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import io.github.rosemoe.sora.widget.SelectionMovement

/**
 * Layar tab Code (F2, F3): header path, tab bar, editor, toolbar. Panel direktori (F4) dan
 * menu titik tiga (F3b) belum ada. Semua warna/ukuran dari [CodeColors] dan [CodeDimens].
 */
@Composable
fun CodeScreen(session: CodeSession, modifier: Modifier = Modifier) {
    var pendingClose by remember { mutableStateOf<EditorTab?>(null) }
    val ctx = LocalContext.current

    Column(modifier.fillMaxWidth().background(CodeColors.EditorBg)) {
        PathHeader(session.workDir)
        EditorTabBar(session) { tab ->
            if (tab.dirty) pendingClose = tab else session.close(tab.id)
        }
        EditorHost(session, Modifier.weight(1f).fillMaxWidth())
        session.active?.let { tab ->
            EditorToolbar(
                tab = tab,
                onPaste = { tab.editor?.pasteText() },
                onUndo = { tab.editor?.undo() },
                onRedo = { tab.editor?.redo() },
                onLeft = { tab.editor?.moveSelection(SelectionMovement.LEFT) },
                onRight = { tab.editor?.moveSelection(SelectionMovement.RIGHT) },
                onSelectAllCopy = {
                    tab.editor?.let { ed ->
                        if (ed.text.length > 0) {
                            ed.selectAll()
                            ed.copyText()
                            Toast.makeText(ctx, "Semua teks disalin", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onClose = { if (tab.dirty) pendingClose = tab else session.close(tab.id) }
            )
        }
    }

    pendingClose?.let { tab ->
        ConfirmCloseDialog(
            title = tab.title,
            onDiscard = {
                session.close(tab.id)
                pendingClose = null
            },
            onCancel = { pendingClose = null }
        )
    }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(CodeDimens.Hairline).background(CodeColors.Hairline))
}

/** Header path folder kerja. Titik tiga baru visual; menunya dikerjakan di F3b. */
@Composable
private fun PathHeader(path: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(CodeDimens.HeaderHeight)
            .background(CodeColors.Surface1)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = path,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = CodeColors.TextSecondary, fontSize = CodeDimens.PathTextSize)
        )
        Box(
            Modifier.width(CodeDimens.HeaderHeight).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "\u22EE",
                style = TextStyle(color = CodeColors.IconDefault, fontSize = 20.sp)
            )
        }
    }
    Hairline()
}

@Composable
private fun EditorTabBar(session: CodeSession, onRequestClose: (EditorTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(CodeDimens.TabBarHeight)
            .background(CodeColors.Surface1)
            .horizontalScroll(rememberScrollState())
    ) {
        session.tabs.forEach { tab ->
            key(tab.id) {
                TabItem(
                    tab = tab,
                    active = tab.id == session.activeId,
                    onSelect = { session.select(tab.id) },
                    onClose = { onRequestClose(tab) }
                )
            }
        }
        Box(
            Modifier
                .width(CodeDimens.TabBarHeight)
                .fillMaxHeight()
                .clickable { session.newTab() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "+",
                style = TextStyle(color = CodeColors.IconDefault, fontSize = 20.sp)
            )
        }
    }
    Hairline()
}

/** Tab aktif: garis emas di ATAS, latar lebih gelap. Titik emas = belum disimpan. */
@Composable
private fun TabItem(tab: EditorTab, active: Boolean, onSelect: () -> Unit, onClose: () -> Unit) {
    Box(
        Modifier
            .height(CodeDimens.TabBarHeight)
            .widthIn(min = CodeDimens.TabMinWidth, max = CodeDimens.TabMaxWidth)
            .background(if (active) CodeColors.EditorBg else CodeColors.Surface1)
            .clickable(onClick = onSelect)
    ) {
        if (active) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(CodeDimens.TabTopLine)
                    .background(CodeColors.Gold)
            )
        }
        Row(
            Modifier.fillMaxHeight().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tab.dirty) {
                Box(Modifier.size(CodeDimens.DirtyDot).background(CodeColors.Gold, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            BasicText(
                text = tab.title,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (active) CodeColors.TextPrimary else CodeColors.TextSecondary,
                    fontSize = CodeDimens.TabTitleSize
                )
            )
            Box(
                Modifier.width(CodeDimens.TabCloseWidth).fillMaxHeight().clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "\u2715",
                    style = TextStyle(color = CodeColors.IconDefault, fontSize = 12.sp)
                )
            }
        }
    }
}

/** Konfirmasi tutup tab dirty. Simpan menyusul di F3; sementara hanya Buang atau Batal. */
@Composable
private fun ConfirmCloseDialog(title: String, onDiscard: () -> Unit, onCancel: () -> Unit) {
    val shape = RoundedCornerShape(CodeDimens.DialogCorner)
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .widthIn(max = 320.dp)
                .background(CodeColors.SurfaceDialog, shape)
                .border(CodeDimens.Hairline, CodeColors.Hairline, shape)
                .padding(20.dp)
        ) {
            BasicText(
                text = "Tutup tab?",
                style = TextStyle(color = CodeColors.Gold, fontSize = CodeDimens.DialogTitleSize)
            )
            Spacer(Modifier.height(12.dp))
            BasicText(
                text = "\"$title\" belum disimpan. Perubahan akan hilang.",
                style = TextStyle(color = CodeColors.TextPrimary, fontSize = 14.sp)
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                BasicText(
                    text = "BATAL",
                    modifier = Modifier.clickable(onClick = onCancel).padding(12.dp),
                    style = TextStyle(color = CodeColors.LabelChampagne, fontSize = 14.sp)
                )
                BasicText(
                    text = "BUANG",
                    modifier = Modifier.clickable(onClick = onDiscard).padding(12.dp),
                    style = TextStyle(color = CodeColors.Danger, fontSize = 14.sp)
                )
            }
        }
    }
}
