package com.molina.code.ui

import com.molina.code.format.ExternalPlan
import com.molina.code.format.Formatters

private const val MAX_FORMAT_CHARS = 500_000

/**
 * [CODE-FORMAT] Format seluruh isi tab aktif. Pemformat dipilih dari nama berkas (atau shebang):
 * JSON dan XML bawaan; Python, shell, JS/TS/CSS/HTML/Markdown/YAML lewat formatter di Debian.
 * Hasil dipasang sebagai SATU langkah undo (Urungkan mengembalikan teks semula). Bila gagal, teks
 * tidak berubah dan pesan galat tampil di toast.
 */
internal fun CodeSession.formatActive() {
    val tab = active ?: return
    if (tab.loading) { toast("Editor sedang memuat"); return }
    if (tab.readOnly) { toast("Tab ini hanya-baca"); return }
    if (tab.editor == null) { toast("Editor belum siap"); return }
    if (format.busy) { toast("Masih memformat\u2026"); return }
    val text = tab.currentTextOrNull(MAX_FORMAT_CHARS) ?: run { toast("Berkas terlalu besar untuk diformat"); return }
    if (text.isBlank()) { toast("Tidak ada teks untuk diformat"); return }
    val name = tab.file?.name ?: tab.title
    val plan = Formatters.of(name, tab.firstLineOrNull())
    if (plan == null) {
        toast("Belum ada pemformat untuk jenis ini (${Formatters.SUPPORTED_SUMMARY})")
        return
    }
    if (plan is ExternalPlan) toast("Memformat dengan ${plan.label}\u2026")
    format.start(plan, name, text, tab.file?.parentFile?.path) { outcome ->
        applyFormat(tab, text, plan.label, outcome)
    }
}

private fun CodeSession.applyFormat(tab: EditorTab, original: String, label: String, outcome: FormatOutcome) {
    when (outcome) {
        is FormatOutcome.Cancelled -> Unit
        is FormatOutcome.Failed -> toast(outcome.message)
        is FormatOutcome.Ok -> {
            val ed = tab.editor
            if (ed == null || tab !in tabs) { toast("Tab sudah ditutup, format dibatalkan"); return }
            val content = ed.text
            // Teks diedit saat formatter jalan: jangan menimpa ketikan pengguna.
            if (content.length != original.length || content.toString() != original) {
                toast("Teks berubah saat memformat, dibatalkan")
                return
            }
            if (outcome.text == original) { toast("Sudah rapi ($label)"); return }
            val line = ed.cursor.leftLine
            val col = ed.cursor.leftColumn
            try {
                content.replace(0, content.length, outcome.text)
                val l = line.coerceIn(0, ed.text.lineCount - 1)
                ed.setSelection(l, col.coerceAtMost(ed.text.getColumnCount(l)))
                toast("Diformat dengan $label")
            } catch (e: Exception) {
                toast("Gagal memasang hasil format")
            }
        }
    }
}
