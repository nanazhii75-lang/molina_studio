package com.molina.code.ui

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

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
    val ed = CodeEditor(context)
    ed.typefaceText = Typeface.MONOSPACE
    ed.setTextSize(CodeDimens.EditorTextSize.value)
    ed.setWordwrap(true) // blueprint v2 bagian 12: word wrap default AKTIF
    applyMolinaColors(ed.colorScheme)
    ed.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
        dirty = true
        // post: riwayat undo baru final setelah event selesai
        ed.post { revision++ }
    }
    editor = ed
    return ed
}

private fun applyMolinaColors(s: EditorColorScheme) {
    s.setColor(EditorColorScheme.WHOLE_BACKGROUND, CodeColors.EditorBg.toArgb())
    s.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, CodeColors.GutterBg.toArgb())
    s.setColor(EditorColorScheme.LINE_NUMBER, CodeColors.LineNumber.toArgb())
    s.setColor(EditorColorScheme.LINE_DIVIDER, CodeColors.Hairline.toArgb())
    s.setColor(EditorColorScheme.CURRENT_LINE, CodeColors.ActiveLine.toArgb())
    s.setColor(EditorColorScheme.TEXT_NORMAL, CodeColors.TextPrimary.toArgb())
    s.setColor(EditorColorScheme.SELECTION_INSERT, CodeColors.Caret.toArgb())
    s.setColor(EditorColorScheme.SELECTION_HANDLE, CodeColors.Gold.toArgb())
    s.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, CodeColors.EditorSelection.toArgb())
}
