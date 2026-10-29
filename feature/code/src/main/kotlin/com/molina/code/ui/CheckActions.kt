package com.molina.code.ui

import com.molina.code.check.CheckRange
import com.molina.code.check.Checkers
import com.molina.code.check.Diagnostic
import com.molina.code.check.ExternalCheck
import com.molina.code.check.Severity
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer

private const val MAX_CHECK_CHARS = 300_000

/** [CODE-CHECK] Hasil pemeriksaan terakhir untuk satu tab (potret: tidak ikut berubah saat teks diedit). */
internal class ProblemList(val tabId: Int, val title: String, val label: String, val items: List<Diagnostic>)

/**
 * [CODE-CHECK] Periksa isi tab aktif sekali jalan (py: pyflakes, sh: shellcheck, json/xml: bawaan). Hasil jadi garis
 * bawah di editor (Sora DiagnosticsContainer; ketuk garis bawah untuk pesan) dan daftar masalah. Teks tidak pernah diubah.
 */
internal fun CodeSession.checkActive() {
    val tab = active ?: return
    if (tab.loading) return toast("Editor sedang memuat")
    if (tab.editor == null) return toast("Editor belum siap")
    if (checker.busy) return toast("Masih memeriksa\u2026")
    val text = tab.currentTextOrNull(MAX_CHECK_CHARS) ?: return toast("Berkas terlalu besar untuk diperiksa")
    if (text.isBlank()) {
        clearProblems(tab)
        return toast("Tidak ada teks untuk diperiksa")
    }
    val name = tab.file?.name ?: tab.title
    val plan = Checkers.of(name, tab.firstLineOrNull())
        ?: return toast("Belum ada pemeriksa untuk jenis ini (${Checkers.SUPPORTED_SUMMARY})")
    if (plan is ExternalCheck) toast("Memeriksa dengan ${plan.label}\u2026")
    checker.start(plan, name, text, tab.file?.parentFile?.path) { outcome ->
        when (outcome) {
            is CheckOutcome.Cancelled -> Unit
            is CheckOutcome.Failed -> toast(outcome.message)
            is CheckOutcome.Ok -> applyProblems(tab, text, plan.label, outcome.items)
        }
    }
}

/** Hapus garis bawah dan daftar masalah milik [tab]. */
internal fun CodeSession.clearProblems(tab: EditorTab) {
    try {
        tab.editor?.diagnostics = null
    } catch (e: Exception) {
        // editor sudah dilepas
    }
    if (problems?.tabId == tab.id) {
        problems = null
        problemsOpen = false
    }
}

private fun CodeSession.applyProblems(tab: EditorTab, sent: String, label: String, items: List<Diagnostic>) {
    val ed = tab.editor
    if (ed == null || tab !in tabs) return toast("Tab sudah ditutup, hasil periksa dibuang")
    val content = ed.text
    // Teks diedit saat memeriksa: posisi tidak lagi cocok, jangan menandai tempat yang salah.
    if (content.length != sent.length || content.toString() != sent) return toast("Teks berubah saat memeriksa, hasil dibuang")
    if (items.isEmpty()) {
        clearProblems(tab)
        return toast("Tidak ada masalah ($label)")
    }
    try {
        val container = DiagnosticsContainer()
        var id = 0L
        val regions = CheckRange.spans(sent, items).map { sp ->
            val d = sp.d
            val sev: Short = when (d.severity) {
                Severity.ERROR -> DiagnosticRegion.SEVERITY_ERROR
                Severity.WARNING -> DiagnosticRegion.SEVERITY_WARNING
                Severity.INFO -> DiagnosticRegion.SEVERITY_TYPO
            }
            DiagnosticRegion(sp.start, sp.end, sev, id++, DiagnosticDetail(d.message, (d.code?.let { "$it \u00B7 " } ?: "") + d.source))
        }
        container.addDiagnostics(regions)
        ed.diagnostics = container
    } catch (e: Exception) {
        toast("Gagal menandai masalah di editor")
    }
    problems = ProblemList(tab.id, tab.file?.name ?: tab.title, label, items)
    val errors = items.count { it.severity == Severity.ERROR }
    val warns = items.count { it.severity == Severity.WARNING }
    toast("${items.size} masalah ($label): $errors galat, $warns peringatan")
    problemsOpen = true
}
