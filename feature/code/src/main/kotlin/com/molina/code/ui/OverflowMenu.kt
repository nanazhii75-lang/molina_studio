package com.molina.code.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Satu baris menu titik tiga. [enabled] false = tampil redup (alfa 38%) dan tidak bisa diketuk.
 * [checked] non-null = baris berkotak centang (emas bila aktif).
 */
internal class MenuEntry(
    val label: String,
    val enabled: Boolean = false,
    val checked: Boolean? = null,
    val onClick: () -> Unit = {}
)

private const val MAX_SHARE_CHARS = 200_000

/**
 * Menu titik tiga (F3b, blueprint bagian 14): 5 grup dipisah hairline, baris 44dp, tinggi maks 72%
 * layar, di-anchor kanan atas. Item yang fiturnya belum ada tampil redup dan menyala bertahap
 * sesuai fase (Buka, Muat ulang, Aksi file aktif sejak F4).
 */
@Composable
internal fun OverflowMenu(session: CodeSession, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val tab = session.active
    val rev = tab?.revision ?: 0
    val ed = tab?.editor
    val hasText = rev >= 0 && (ed?.text?.length ?: 0) > 0

    val groups = listOf(
        listOf(
            MenuEntry("Baru", enabled = true) { session.newTab(); onDismiss() },
            MenuEntry("Simpan sebagai", enabled = tab != null && !tab.loading) {
                session.saveAsActive()
                onDismiss()
            },
            MenuEntry("Buka", enabled = true) {
                session.picker = PickerRequest(PickMode.File, session.defaultOpenDir(), "Buka berkas") { session.openFile(it) }
                onDismiss()
            },
            MenuEntry("Muat ulang", enabled = tab?.file != null && !tab.loading) {
                session.reloadActive()
                session.dir.refresh()
                onDismiss()
            },
            MenuEntry("Aksi file", enabled = tab?.file != null) {
                tab?.file?.let { session.fileDialog = FileDialog.Menu(it) }
                onDismiss()
            }
        ),
        listOf(
            MenuEntry("Cari", enabled = tab != null) { session.openFind(); onDismiss() },
            MenuEntry("Pergi ke baris", enabled = tab != null) { session.gotoOpen = true; onDismiss() }
        ),
        listOf(
            MenuEntry("Bagikan", enabled = hasText) {
                val text = ed?.text?.toString().orEmpty()
                if (text.length > MAX_SHARE_CHARS) {
                    Toast.makeText(ctx, "Teks terlalu besar untuk dibagikan", Toast.LENGTH_SHORT).show()
                } else {
                    shareText(ctx, text)
                }
                onDismiss()
            },
            MenuEntry("Kirim ke Script Execution", enabled = hasText && session.scriptSink != null) {
                session.sendToScripts()
                onDismiss()
            },
            MenuEntry("Sisipkan ke Terminal", enabled = hasText && session.terminalSink != null) {
                session.sendToTerminal(false)
                onDismiss()
            },
            MenuEntry("Jalankan di Terminal", enabled = hasText && session.terminalSink != null) {
                session.sendToTerminal(true)
                onDismiss()
            },
            MenuEntry("Ekspor"),
            MenuEntry("Konversi berkas...")
        ),
        listOf(
            MenuEntry("Syntax"),
            MenuEntry("Enkode: UTF-8 (terkunci)"),
            MenuEntry("Akhir baris: ${tab?.eol?.label ?: "LF"}", enabled = tab != null && !tab.loading) {
                tab?.let {
                    it.eol = it.eol.other()
                    it.recomputeDirty()
                    session.scheduleDraft()
                }
            },
            MenuEntry("Gaya Visual"),
            MenuEntry("Pembungkus kata", enabled = tab != null, checked = tab?.wordWrap ?: true) {
                tab?.let {
                    it.wordWrap = !it.wordWrap
                    it.editor?.setWordwrap(it.wordWrap)
                }
            },
            MenuEntry("Hanya baca", enabled = tab != null, checked = tab?.readOnly ?: false) {
                tab?.let {
                    it.readOnly = !it.readOnly
                    it.editor?.setEditable(!it.readOnly && !it.loading)
                }
            }
        ),
        listOf(
            MenuEntry("Statistik", enabled = ed != null) {
                ed?.let {
                    Toast.makeText(
                        ctx, "${it.text.lineCount} baris, ${it.text.length} karakter", Toast.LENGTH_SHORT
                    ).show()
                }
                onDismiss()
            },
            MenuEntry("Lakukan")
        )
    )

    val offsetY = with(LocalDensity.current) { CodeDimens.HeaderHeight.roundToPx() }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * CodeDimens.MenuMaxHeightFraction).dp
    val shape = RoundedCornerShape(CodeDimens.MenuCorner)

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(Modifier.padding(8.dp)) {
            Column(
                Modifier
                    .shadow(CodeDimens.MenuElevation, shape)
                    .clip(shape)
                    .background(CodeColors.SurfaceMenu)
                    .border(CodeDimens.Hairline, CodeColors.Hairline, shape)
                    .widthIn(min = CodeDimens.MenuMinWidth, max = CodeDimens.MenuMaxWidth)
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(CodeDimens.Hairline)
                                .background(CodeColors.Hairline)
                        )
                    }
                    group.forEach { MenuRow(it) }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(e: MenuEntry) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(CodeDimens.MenuRowHeight)
            .clickable(enabled = e.enabled, onClick = e.onClick)
            .padding(horizontal = 16.dp)
            .alpha(if (e.enabled) 1f else CodeColors.DisabledAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = e.label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = CodeColors.IconDefault, fontSize = CodeDimens.MenuTextSize)
        )
        e.checked?.let {
            Spacer(Modifier.width(12.dp))
            CheckMark(it)
        }
    }
}

/** Kotak centang: terisi emas dengan centang hitam bila aktif; border champagne 50% bila tidak. */
@Composable
internal fun CheckMark(on: Boolean) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier
            .size(CodeDimens.MenuIconSize)
            .background(if (on) CodeColors.Gold else Color.Transparent, shape)
            .border(
                1.5.dp,
                if (on) CodeColors.Gold else CodeColors.IconDefault.copy(alpha = 0.5f),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (on) BasicText("\u2713", style = TextStyle(color = Color.Black, fontSize = 12.sp))
    }
}

internal fun shareText(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, "Bagikan").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
