package com.molina.code.ui

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.molina.code.preview.PreviewKind
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.util.WeakHashMap

/**
 * Menampilkan editor milik tab aktif. Wadah FrameLayout dipakai agar pergantian tab hanya
 * menukar view anak (tanpa membuat ulang editor, jadi undo/redo dan kursor tetap).
 */
@Composable
internal fun EditorHost(session: CodeSession, modifier: Modifier = Modifier) {
    val tab = session.active ?: return
    AndroidView(
        modifier = modifier,
        factory = { ctx -> FrameLayout(ctx).apply { setBackgroundColor(CodeColors.EditorBg.toArgb()) } },
        update = { container ->
            val editor = tab.editorOrCreate(container.context)
            (editor as? MolinaCodeEditor)?.let { me -> // [CODE-PLAYGUTTER]
                me.onPlay = { session.playActive() }
                me.playRunning = session.run.running
                val en = tab.previewKind() == PreviewKind.SCRIPT
                if (me.playEnabled != en) me.playEnabled = en
                me.invalidate()
            }
            // membaca nama berkas di sini: ganti nama / Simpan sebagai ikut menyegarkan highlight
            syncSyntax(editor, tab.file?.name ?: tab.title)
            if (container.childCount != 1 || container.getChildAt(0) !== editor) {
                container.removeAllViews()
                (editor.parent as? ViewGroup)?.removeView(editor)
                container.addView(
                    editor,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        },
        onRelease = { it.removeAllViews() }
    )
}

internal fun EditorTab.editorOrCreate(context: Context): CodeEditor {
    editor?.let { return it }
    val ed = MolinaCodeEditor(context) // [CODE-GUTTER]
    ed.typefaceText = Typeface.MONOSPACE
    ed.setTextSize(CodeDimens.EditorTextSize.value)
    ed.setWordwrap(wordWrap) // blueprint v2 bagian 12: default AKTIF; diubah dari menu titik tiga
    ed.setEditable(!readOnly && !loading)
    applyMolinaColors(ed.colorScheme)
    // Isi hasil pemulihan draf dipasang sebelum langganan event agar tidak dihitung sebagai edit.
    pendingText?.let {
        ed.setText(it)
        pendingText = null
    }
    ed.subscribeEvent(ContentChangeEvent::class.java) { ev, _ ->
        // setText (pemulihan/muat) bukan edit pengguna: tidak boleh menandai dirty
        if (ev.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) {
            // Tandai dirty seketika, lalu bandingkan dengan isi tersimpan setelah edit berhenti:
            // undo sampai kembali ke isi awal menghapus titik emas (blueprint bagian 31.2).
            dirty = true
            onEdited?.invoke()
            scheduleDirtyCheck(ed)
        }
        // post: riwayat undo baru final setelah event selesai
        ed.post { revision++ }
        ChangeGutter.schedule(this)
    }
    // Penghitung find bar: hasil pencarian selesai atau kursor pindah (indeks hasil aktif)
    ed.subscribeEvent(PublishSearchResultEvent::class.java) { _, _ -> ed.post { findRevision++ } }
    ed.subscribeEvent(SelectionChangeEvent::class.java) { _, _ -> ed.post { findRevision++ } }
    // Teks dari Terminal yang diantrekan saat editor belum ada: disisipkan sebagai edit (menandai dirty + draf)
    queuedInsert?.let { q ->
        queuedInsert = null
        ed.post { runCatching { ed.insertText(q, q.length) } }
    }
    editor = ed
    ChangeGutter.schedule(this)
    return ed
}

private fun applyMolinaColors(s: EditorColorScheme) {
    ObsidianClaude.apply(s)
    s.setColor(EditorColorScheme.WHOLE_BACKGROUND, CodeColors.EditorBg.toArgb())
    s.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, CodeColors.GutterBg.toArgb())
    s.setColor(EditorColorScheme.LINE_NUMBER, CodeColors.LineNumber.toArgb())
    s.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, CodeColors.LineNumberCurrent.toArgb())
    s.setColor(EditorColorScheme.LINE_NUMBER_PANEL, CodeColors.LineNumberPanelBg.toArgb())
    s.setColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT, CodeColors.LineNumberFlash.toArgb())
    s.setColor(EditorColorScheme.LINE_DIVIDER, CodeColors.Hairline.toArgb())
    s.setColor(EditorColorScheme.CURRENT_LINE, CodeColors.ActiveLine.toArgb())
    s.setColor(EditorColorScheme.TEXT_NORMAL, CodeColors.TextPrimary.toArgb())
    s.setColor(EditorColorScheme.SELECTION_INSERT, CodeColors.Caret.toArgb())
    s.setColor(EditorColorScheme.SELECTION_HANDLE, CodeColors.Gold.toArgb())
    s.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, CodeColors.EditorSelection.toArgb())
    s.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, CodeColors.SearchMatch.toArgb())
}

private const val FLASH_MS = 1400L
private val flashRestore = WeakHashMap<CodeEditor, Runnable>()

/**
 * Pindah ke [line] (basis 0) dan, bila [column] > 0 (basis 1), ke kolom itu; lalu sorot nomor baris
 * dan latar baris tujuan sesaat. Kolom yang melebihi panjang baris dijepit ke ujung baris.
 */
internal fun CodeEditor.jumpToLineHighlighted(line: Int, column: Int = 0) {
    jumpToLine(line)
    if (column > 0) {
        val maxCol = text.getColumnCount(line)
        setSelection(line, minOf(column - 1, maxCol))
    }
    flashCurrentLine(FLASH_MS)
}

/** Nyalakan nomor dan latar baris kursor (putih-persik di atas terakota) selama [durationMs], lalu pulihkan. */
internal fun CodeEditor.flashCurrentLine(durationMs: Long = FLASH_MS) {
    val scheme = colorScheme
    scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, CodeColors.LineNumberFlash.toArgb())
    scheme.setColor(EditorColorScheme.CURRENT_LINE, CodeColors.ActiveLineFlash.toArgb())
    invalidate()
    flashRestore.remove(this)?.let { removeCallbacks(it) }
    val restore = Runnable {
        scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, CodeColors.LineNumberCurrent.toArgb())
        scheme.setColor(EditorColorScheme.CURRENT_LINE, CodeColors.ActiveLine.toArgb())
        invalidate()
        flashRestore.remove(this)
    }
    flashRestore[this] = restore
    postDelayed(restore, durationMs)
}

private val appliedSpec = java.util.WeakHashMap<CodeEditor, SyntaxSpec?>()

/** Pasang bahasa highlight sesuai nama berkas (default tema Obsidian Claude); hanya bila berubah. */
internal fun syncSyntax(ed: CodeEditor, fileName: String?) {
    val spec = SyntaxLanguages.specFor(fileName)
    if (appliedSpec.containsKey(ed) && appliedSpec[ed] === spec) return
    appliedSpec[ed] = spec
    ed.setEditorLanguage(if (spec == null) null else MolinaLanguage(spec))
}
