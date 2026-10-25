package com.molina.code.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Satu tab editor. Tiap tab punya instance editor sendiri (dibuat malas) sehingga isi, posisi
 * kursor, dan riwayat undo/redo terpisah per tab (blueprint bagian 7).
 */
class EditorTab internal constructor(val id: Int, initialTitle: String) {
    var title by mutableStateOf(initialTitle)
        internal set

    /** True bila isi berubah sejak dibuka/disimpan. Menjadi titik emas pada judul tab. */
    var dirty by mutableStateOf(false)
        internal set

    /** Naik setiap isi berubah (setelah event selesai). Memicu hitung ulang state Undo/Redo. */
    var revision by mutableIntStateOf(0)
        internal set

    internal var editor: CodeEditor? = null

    internal fun disposeEditor() {
        editor?.release()
        editor = null
    }
}

/**
 * Status layar Code: daftar tab dan tab aktif. Dibuat SATU kali di tingkat Activity supaya tab
 * tidak hilang saat pengguna pindah ke tab bawah lain. Belum bertahan saat rotasi layar atau
 * proses dimatikan (autosave draf menyusul).
 */
class CodeSession(val workDir: String) {
    val tabs = mutableStateListOf<EditorTab>()

    var activeId by mutableIntStateOf(-1)
        private set

    private var nextId = 1
    private var untitledCount = 0

    val active: EditorTab?
        get() = tabs.firstOrNull { it.id == activeId }

    init {
        newTab()
    }

    fun newTab(): EditorTab {
        untitledCount++
        val title = if (untitledCount == 1) UNTITLED else "$UNTITLED $untitledCount"
        val tab = EditorTab(nextId++, title)
        tabs.add(tab)
        activeId = tab.id
        return tab
    }

    fun select(id: Int) {
        if (tabs.any { it.id == id }) activeId = id
    }

    /** Menutup tab. Selalu menyisakan satu tab kosong. Konfirmasi tab dirty ada di layar. */
    fun close(id: Int) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs.removeAt(index).disposeEditor()
        if (tabs.isEmpty()) {
            newTab()
        } else if (activeId == id) {
            activeId = tabs[minOf(index, tabs.size - 1)].id
        }
    }

    fun releaseAll() {
        tabs.forEach { it.disposeEditor() }
    }

    companion object {
        const val UNTITLED = "Tidak berjudul"
    }
}
