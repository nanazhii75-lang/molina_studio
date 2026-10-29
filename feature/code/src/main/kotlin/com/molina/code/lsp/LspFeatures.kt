package com.molina.code.lsp

// [CODE-LSP4] Completion dan definition (pylsp + Jedi). Kotlin murni. Semua posisi di berkas ini memakai
// konvensi LSP/Sora: baris dan kolom basis 0, kolom dalam UTF-16 (sama dengan String Kotlin).

/** Penggantian teks yang diminta server; rentang basis 0, akhir eksklusif. */
data class LspTextEdit(val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int, val newText: String)

data class LspCompletionItem(
    val label: String,
    /** Teks yang disisipkan (textEdit.newText > insertText > label). Selalu teks polos: snippet tidak diumumkan ke server. */
    val insertText: String,
    val kind: Int?,
    val detail: String?,
    val sortKey: String,
    val edit: LspTextEdit?,
)

/** Lokasi hasil definition (basis 0). [uri] milik guest Debian; terjemahkan lewat LspPathMap. */
data class LspLocation(val uri: String, val line: Int, val column: Int)

object LspCompletions {
    const val MAX_ITEMS = 200
    private const val MAX_TEXT = 300

    /** Terima `CompletionItem[]`, `CompletionList`, atau null. Item tanpa label dibuang; hasil diurutkan stabil menurut sortText. */
    fun convert(result: Any?): List<LspCompletionItem> {
        val raw: List<*> = when (result) {
            is List<*> -> result
            is Map<*, *> -> result["items"] as? List<*> ?: return emptyList()
            else -> return emptyList()
        }
        val out = ArrayList<LspCompletionItem>(minOf(raw.size, MAX_ITEMS))
        for (o in raw) {
            val m = o as? Map<*, *> ?: continue
            val label = (m["label"] as? String)?.take(MAX_TEXT)?.ifBlank { null } ?: continue
            val edit = edit(m["textEdit"])
            val insert = (edit?.newText ?: m["insertText"] as? String ?: label).take(MAX_TEXT * 4)
            out.add(
                LspCompletionItem(
                    label = label,
                    insertText = insert,
                    kind = (m["kind"] as? Double)?.toInt(),
                    detail = (m["detail"] as? String)?.take(MAX_TEXT)?.ifBlank { null },
                    sortKey = (m["sortText"] as? String) ?: label,
                    edit = edit,
                ),
            )
        }
        out.sortBy { it.sortKey } // sortBy stabil
        return if (out.size > MAX_ITEMS) ArrayList(out.subList(0, MAX_ITEMS)) else out
    }

    /** `TextEdit` atau `InsertReplaceEdit` (dipakai bagian `replace`). */
    private fun edit(o: Any?): LspTextEdit? {
        val m = o as? Map<*, *> ?: return null
        val text = m["newText"] as? String ?: return null
        val range = (m["range"] ?: m["replace"]) as? Map<*, *> ?: return null
        val s = pos(range["start"]) ?: return null
        val e = pos(range["end"]) ?: return null
        return LspTextEdit(s.first, s.second, e.first, e.second, text.take(MAX_TEXT * 4))
    }

    internal fun pos(o: Any?): Pair<Int, Int>? {
        val m = o as? Map<*, *> ?: return null
        val l = (m["line"] as? Double)?.toInt() ?: return null
        val c = (m["character"] as? Double)?.toInt() ?: return null
        return Pair(l.coerceAtLeast(0), c.coerceAtLeast(0))
    }
}

object LspDefinitions {
    const val MAX_ITEMS = 50

    /** Terima `Location`, `Location[]`, `LocationLink[]`, atau null. Duplikat (uri+posisi sama) dibuang. */
    fun convert(result: Any?): List<LspLocation> {
        val raw: List<*> = when (result) {
            is List<*> -> result
            is Map<*, *> -> listOf(result)
            else -> return emptyList()
        }
        val out = LinkedHashSet<LspLocation>()
        for (o in raw) {
            if (out.size >= MAX_ITEMS) break
            val m = o as? Map<*, *> ?: continue
            val uri = (m["uri"] ?: m["targetUri"]) as? String ?: continue
            val range = (m["range"] ?: m["targetSelectionRange"] ?: m["targetRange"]) as? Map<*, *> ?: continue
            val (l, c) = LspCompletions.pos(range["start"]) ?: continue
            out.add(LspLocation(uri, l, c))
        }
        return out.toList()
    }
}
