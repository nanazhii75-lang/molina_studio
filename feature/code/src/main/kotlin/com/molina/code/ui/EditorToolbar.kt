package com.molina.code.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Toolbar editor (F3, Opsi A): flat penuh lebar di Surface2 + rail emas. Grup:
 * [Simpan] | [Tempel Undo Redo] | [Cari Kiri Kanan] | [PilihSemuaSalin Play] | [Tutup].
 * Simpan, Cari, dan Play sudah tampil tetapi aksinya menyusul (F3e, F3c, fase Play).
 */
@Composable
internal fun EditorToolbar(
    tab: EditorTab,
    onPaste: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onSelectAllCopy: () -> Unit,
    onClose: () -> Unit
) {
    // Membaca revision memicu hitung ulang Undo/Redo setiap isi berubah.
    val rev = tab.revision
    val ed = tab.editor
    val canUndo = rev >= 0 && ed?.canUndo() == true
    val canRedo = rev >= 0 && ed?.canRedo() == true
    val hasText = rev >= 0 && (ed?.text?.length ?: 0) > 0

    val ctx = LocalContext.current
    var hasClip by remember { mutableStateOf(false) }
    DisposableEffect(ctx) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val listener = ClipboardManager.OnPrimaryClipChangedListener { hasClip = cm.hasPrimaryClip() }
        hasClip = cm.hasPrimaryClip()
        cm.addPrimaryClipChangedListener(listener)
        onDispose { cm.removePrimaryClipChangedListener(listener) }
    }

    Column(Modifier.fillMaxWidth().background(CodeColors.Surface2)) {
        // Rail emas: gradient transparan -> gold 40% -> transparan
        Box(
            Modifier
                .fillMaxWidth()
                .height(CodeDimens.RailHeight)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, CodeColors.RailPeak, Color.Transparent)
                    )
                )
        )
        Row(
            Modifier.fillMaxWidth().height(CodeDimens.ToolbarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simpan: aksi menyusul di F3e; titik emas menandai ada perubahan
            ToolbarButton(
                ToolbarIcon.Save,
                if (tab.dirty) CodeColors.Gold else CodeColors.IconDim,
                enabled = false, dot = tab.dirty, onClick = {}
            )
            GroupDivider()
            ToolbarButton(
                ToolbarIcon.Paste,
                if (hasClip) CodeColors.IconDefault else CodeColors.IconDisabled,
                enabled = hasClip, onClick = onPaste
            )
            ToolbarButton(
                ToolbarIcon.Undo,
                if (canUndo) CodeColors.IconDefault else CodeColors.IconDisabled,
                enabled = canUndo, onClick = onUndo
            )
            ToolbarButton(
                ToolbarIcon.Redo,
                if (canRedo) CodeColors.IconDefault else CodeColors.IconDisabled,
                enabled = canRedo, onClick = onRedo
            )
            GroupDivider()
            // Cari: find bar menyusul di F3c
            ToolbarButton(ToolbarIcon.Find, CodeColors.IconDisabled, enabled = false, onClick = {})
            ToolbarButton(ToolbarIcon.Left, CodeColors.IconDefault, enabled = true, onClick = onLeft)
            ToolbarButton(ToolbarIcon.Right, CodeColors.IconDefault, enabled = true, onClick = onRight)
            GroupDivider()
            // Pilih semua lalu salin otomatis (keputusan pemilik; menggantikan "Manajemen tab")
            ToolbarButton(
                ToolbarIcon.SelectAll,
                if (hasText) CodeColors.IconDefault else CodeColors.IconDisabled,
                enabled = hasText, onClick = onSelectAllCopy
            )
            // Play: file belum bisa dijalankan
            ToolbarButton(ToolbarIcon.Play, CodeColors.IconDisabled, enabled = false, onClick = {})
            GroupDivider()
            ToolbarButton(ToolbarIcon.Close, CodeColors.Danger, enabled = true, onClick = onClose)
        }
    }
}

@Composable
private fun GroupDivider() {
    Box(
        Modifier
            .width(CodeDimens.Hairline)
            .height(CodeDimens.ToolbarGroupDividerHeight)
            .background(CodeColors.Hairline)
    )
}

/** Satu tombol toolbar. Efek tekan: isi gold 16% (menggantikan ripple abu bawaan). */
@Composable
private fun RowScope.ToolbarButton(
    icon: ToolbarIcon,
    color: Color,
    enabled: Boolean,
    dot: Boolean = false,
    badge: Int? = null,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (pressed && enabled) CodeColors.Ripple else Color.Transparent)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(CodeDimens.ToolbarIconSize)) { drawToolbarIcon(icon, color, dot) }
        if (badge != null) {
            BasicText(
                text = badge.toString(),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 3.dp),
                style = TextStyle(color = CodeColors.GoldSoft, fontSize = CodeDimens.TabBadgeTextSize)
            )
        }
    }
}
