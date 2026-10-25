package com.molina.code.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Panel bawah editor (tombol Play): pratinjau native Markdown/JSON yang ikut berubah saat mengetik,
 * atau keluaran skrip. Semua digambar dengan Compose (tanpa WebView). [onJump] melompat ke
 * (baris basis 1, kolom basis 1) di editor aktif, dipakai oleh galat JSON.
 */
@Composable
internal fun PreviewPane(session: CodeSession, onJump: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    val preview = session.preview
    Column(modifier.fillMaxWidth().background(CodeColors.PreviewBg)) {
        PaneHeader(session)
        Box(Modifier.fillMaxWidth().height(CodeDimens.Hairline).background(CodeColors.Hairline))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (preview.mode) {
                PaneMode.Output -> OutputBody(session.run)
                PaneMode.Preview -> PreviewBody(preview.content, onJump)
            }
        }
    }
}

/**
 * Halaman Tinjauan penuh (blueprint bagian 21): appbar hitam dengan tombol kembali, judul
 * "Tinjauan", subjudul path berkas, lalu isi pratinjau native. Tombol Back sistem menutupnya.
 * Isi dibangun ulang otomatis bila berkas berubah (lihat efek di CodeScreen).
 */
@Composable
internal fun PreviewPage(session: CodeSession, onJump: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    val preview = session.preview
    val tab = session.active
    val ctx = LocalContext.current
    BackHandler { preview.open = false }
    Column(
        modifier
            .background(CodeColors.PreviewBg)
            // menyerap sentuhan agar tidak tembus ke editor di bawahnya
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).background(CodeColors.PreviewBg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(56.dp).fillMaxHeight().clickable { preview.open = false },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(24.dp)) {
                    val k = size.minDimension / 24f
                    val c = CodeColors.IconDefault
                    val w = 2f * k
                    drawLine(c, Offset(20f * k, 12f * k), Offset(5f * k, 12f * k), strokeWidth = w, cap = StrokeCap.Round)
                    drawLine(c, Offset(11f * k, 5f * k), Offset(4f * k, 12f * k), strokeWidth = w, cap = StrokeCap.Round)
                    drawLine(c, Offset(11f * k, 19f * k), Offset(4f * k, 12f * k), strokeWidth = w, cap = StrokeCap.Round)
                }
            }
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                BasicText(
                    text = "Tinjauan",
                    maxLines = 1,
                    style = TextStyle(color = CodeColors.IconDefault, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
                BasicText(
                    text = tab?.file?.path ?: tab?.title.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = CodeDimens.PreviewSubtitleSize)
                )
            }
            PaneAction("BAGIKAN", CodeColors.Gold) {
                val text = tab?.currentTextOrNull(200_000)
                if (text == null) session.toast("Teks terlalu besar untuk dibagikan") else shareText(ctx, text)
            }
        }
        Box(Modifier.fillMaxWidth().height(CodeDimens.Hairline).background(CodeColors.Hairline))
        Box(Modifier.weight(1f).fillMaxWidth()) { PreviewBody(preview.content, onJump, preview::takeAnchor) }
    }
}

@Composable
private fun PaneHeader(session: CodeSession) {
    val preview = session.preview
    val run = session.run
    val output = preview.mode == PaneMode.Output
    val label = when {
        output -> "Keluaran \u00B7 ${run.interpreter}"
        preview.label.isNotEmpty() -> "Pratinjau \u00B7 ${preview.label}"
        else -> "Pratinjau"
    }
    Row(
        Modifier.fillMaxWidth().height(CodeDimens.PaneHeaderHeight).background(CodeColors.Surface1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            maxLines = 1,
            style = TextStyle(color = CodeColors.GoldSoft, fontSize = CodeDimens.PaneLabelSize)
        )
        if (output && run.summary.isNotEmpty()) {
            val dot = when (run.status) {
                RunStatus.Running -> CodeColors.Gold
                RunStatus.Done -> CodeColors.OutputOk
                RunStatus.Failed, RunStatus.TimedOut -> CodeColors.Danger
                else -> CodeColors.IconDim
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(CodeDimens.DirtyDot).background(dot, CircleShape))
            Spacer(Modifier.width(5.dp))
            BasicText(
                text = run.summary,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = CodeColors.TextSecondary, fontSize = CodeDimens.StatusTextSize)
            )
        }
        Spacer(Modifier.weight(1f))
        if (output) {
            if (run.running) {
                PaneAction("BERHENTI", CodeColors.Danger) { run.stop() }
            } else {
                PaneAction("BERSIHKAN", CodeColors.IconDefault) { run.clear() }
            }
        }
        PaneAction("\u2715", CodeColors.IconDefault) { preview.open = false }
    }
}

@Composable
private fun PaneAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxHeight()
            .widthIn(min = CodeDimens.PaneActionWidth)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            maxLines = 1,
            style = TextStyle(color = color, fontSize = CodeDimens.PaneLabelSize)
        )
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(CodeDimens.PanePadding), contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            style = TextStyle(color = CodeColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        )
    }
}

// ------------------------------------------------------------------ Pratinjau

@Composable
private fun PreviewBody(content: PreviewContent, onJump: (Int, Int) -> Unit, anchor: () -> Int = { -1 }) {
    when (content) {
        PreviewContent.Empty -> Unit
        is PreviewContent.Message -> CenterMessage(content.text)
        is PreviewContent.Markdown -> MarkdownBody(content.blocks, content.startLines, anchor)
        is PreviewContent.Json -> MonoBody(content.text)
        is PreviewContent.JsonError -> JsonErrorBody(content, onJump)
    }
}

@Composable
private fun MarkdownBody(blocks: List<MdUi>, startLines: List<Int>, anchor: () -> Int) {
    val uri = LocalUriHandler.current
    val state = rememberLazyListState()
    // Sekali per pembukaan: gulir ke blok tempat kursor editor berada.
    LaunchedEffect(blocks) {
        if (blocks.isEmpty()) return@LaunchedEffect
        val line = anchor()
        if (line < 0) return@LaunchedEffect
        val idx = startLines.indexOfLast { it <= line }
        if (idx > 0) state.scrollToItem(idx)
    }
    if (blocks.isEmpty()) {
        CenterMessage("Dokumen kosong.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(CodeDimens.PanePadding)
    ) {
        items(blocks.size) { i -> MdBlockView(blocks[i], uri) }
    }
}

private fun bodyStyle(bold: Boolean = false) = TextStyle(
    color = CodeColors.PreviewText,
    fontSize = CodeDimens.PaneBodySize,
    fontWeight = if (bold) FontWeight.Bold else null
)

@Composable
private fun MdBlockView(b: MdUi, uri: UriHandler) {
    when (b) {
        is MdUi.Heading -> {
            val size = when (b.level) {
                1 -> 24.sp
                2 -> 20.sp
                3 -> 17.sp
                else -> 15.sp
            }
            Column(Modifier.fillMaxWidth().padding(top = if (b.level <= 2) 14.dp else 10.dp, bottom = 6.dp)) {
                LinkText(
                    b.text,
                    TextStyle(color = CodeColors.PreviewHeading, fontSize = size, fontWeight = FontWeight.Bold),
                    uri
                )
                if (b.level <= 2) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 4.dp).height(CodeDimens.Hairline)
                            .background(CodeColors.PreviewHairline)
                    )
                }
            }
        }
        is MdUi.Para -> LinkText(b.text, bodyStyle(), uri, Modifier.padding(vertical = 5.dp))
        is MdUi.Code -> {
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(CodeColors.PreviewCodeBg, shape)
                    .border(CodeDimens.Hairline, CodeColors.PreviewHairline, shape)
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                BasicText(
                    text = b.text,
                    softWrap = false,
                    style = TextStyle(
                        color = CodeColors.PreviewText,
                        fontSize = CodeDimens.PaneMonoSize,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
        is MdUi.Quote -> Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .background(CodeColors.PreviewQuoteBg)
                .height(IntrinsicSize.Min)
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(CodeColors.PreviewQuoteBar))
            LinkText(
                b.text,
                TextStyle(color = CodeColors.PreviewQuoteText, fontSize = CodeDimens.PaneBodySize),
                uri,
                Modifier.padding(10.dp)
            )
        }
        is MdUi.Item -> Row(
            Modifier.fillMaxWidth().padding(start = (b.depth * 16).dp, top = 2.dp, bottom = 2.dp)
        ) {
            val marker = when (b.checked) {
                true -> "\u2611"
                false -> "\u2610"
                null -> b.marker
            }
            BasicText(
                text = marker,
                modifier = Modifier.widthIn(min = 24.dp),
                style = TextStyle(color = CodeColors.IconDefault, fontSize = CodeDimens.PaneBodySize)
            )
            LinkText(b.text, bodyStyle(), uri, Modifier.weight(1f))
        }
        MdUi.Rule -> Box(
            Modifier.fillMaxWidth().padding(vertical = 10.dp).height(CodeDimens.Hairline)
                .background(CodeColors.PreviewHairline)
        )
        is MdUi.Table -> Column(
            Modifier
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState())
                .border(CodeDimens.Hairline, CodeColors.PreviewHairline)
        ) {
            TableRowView(b.header, true, uri)
            b.rows.forEach { TableRowView(it, false, uri) }
        }
    }
}

@Composable
private fun TableRowView(cells: List<AnnotatedString>, header: Boolean, uri: UriHandler) {
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .background(if (header) CodeColors.PreviewTableHeadBg else Color.Transparent)
    ) {
        cells.forEach { c ->
            Box(
                Modifier
                    .width(CodeDimens.PaneTableCellWidth)
                    .fillMaxHeight()
                    .border(CodeDimens.Hairline, CodeColors.PreviewHairline)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                LinkText(c, bodyStyle(bold = header), uri)
            }
        }
    }
}

/** Teks dengan tautan yang bisa diketuk; teks tanpa tautan memakai BasicText biasa. */
@Composable
private fun LinkText(text: AnnotatedString, style: TextStyle, uri: UriHandler, modifier: Modifier = Modifier) {
    val hasLink = remember(text) { text.getStringAnnotations(LINK_TAG, 0, text.length).isNotEmpty() }
    if (!hasLink) {
        BasicText(text, modifier, style)
        return
    }
    ClickableText(text, modifier, style) { offset ->
        text.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()?.let { openLink(uri, it.item) }
    }
}

/** Hanya skema web/email yang dibuka; tautan relatif atau skema lain diabaikan. */
private fun openLink(uri: UriHandler, url: String) {
    val ok = url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:")
    if (ok) runCatching { uri.openUri(url) }
}

@Composable
private fun MonoBody(text: AnnotatedString) {
    val v = rememberScrollState()
    val h = rememberScrollState()
    Box(
        Modifier.fillMaxSize().verticalScroll(v).horizontalScroll(h).padding(CodeDimens.PanePadding)
    ) {
        BasicText(
            text = text,
            softWrap = false,
            style = TextStyle(
                color = CodeColors.PreviewText,
                fontSize = CodeDimens.PaneMonoSize,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
private fun JsonErrorBody(e: PreviewContent.JsonError, onJump: (Int, Int) -> Unit) {
    val caretAt = (e.column - 1).coerceIn(0, e.snippet.length)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CodeDimens.PanePadding)) {
        BasicText(
            text = "JSON tidak valid",
            style = TextStyle(color = CodeColors.Danger, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(6.dp))
        BasicText(text = e.message, style = bodyStyle())
        Spacer(Modifier.height(10.dp))
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            BasicText(
                text = e.snippet + "\n" + " ".repeat(caretAt) + "^",
                softWrap = false,
                style = TextStyle(
                    color = CodeColors.PreviewText,
                    fontSize = CodeDimens.PaneMonoSize,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = "Lompat ke baris ${e.line}, kolom ${e.column}",
            modifier = Modifier.clickable { onJump(e.line, e.column) }.padding(vertical = 8.dp),
            style = TextStyle(color = CodeColors.Gold, fontSize = 13.sp)
        )
    }
}

// ------------------------------------------------------------------ Keluaran skrip

@Composable
private fun OutputBody(run: RunController) {
    val scroll = rememberScrollState()
    val text = remember(run.version) { PreviewRender.output(run.lines) }
    LaunchedEffect(run.version) {
        // Ikuti keluaran terbaru selama skrip berjalan; setelah selesai pengguna bebas menggulir.
        if (run.running) {
            withFrameNanos { }
            scroll.scrollTo(scroll.maxValue)
        }
    }
    if (run.lines.isEmpty()) {
        CenterMessage(
            when (run.status) {
                RunStatus.Running -> "Menjalankan\u2026"
                RunStatus.Idle -> "Belum ada keluaran. Buka berkas .sh atau .py lalu tekan Play."
                else -> "Selesai tanpa keluaran."
            }
        )
        return
    }
    Box(Modifier.fillMaxSize().verticalScroll(scroll).padding(CodeDimens.PanePadding)) {
        BasicText(
            text = text,
            style = TextStyle(
                color = CodeColors.OutputText,
                fontSize = CodeDimens.PaneMonoSize,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}
