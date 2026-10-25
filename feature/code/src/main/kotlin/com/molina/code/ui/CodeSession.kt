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

    /** Pembungkus kata per tab (default aktif, blueprint v2 bagian 12). Diubah dari menu titik tiga. */
    var wordWrap by mutableStateOf(true)

    /** Hanya baca per tab. Diubah dari menu titik tiga. */
    var readOnly by mutableStateOf(false)

    /** Naik saat hasil pencarian atau seleksi berubah; memicu hitung ulang penghitung find bar. */
    var findRevision by mutableIntStateOf(0)
        internal set

    /** State agar find bar dan toolbar ikut diperbarui begitu editor tab selesai dibuat. */
    internal var editor by mutableStateOf<CodeEditor?>(null)

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

    val find = FindState()

    /** Dialog Pergi ke baris terbuka. */
    var gotoOpen by mutableStateOf(false)

    var activeId by mutableIntStateOf(-1)
        private set

    private var nextId = 1

    val active: EditorTab?
        get() = tabs.firstOrNull { it.id == activeId }

    init {
        newTab()
    }

    fun newTab(): EditorTab {
        val tab = EditorTab(nextId++, nextUntitledTitle())
        tabs.add(tab)
        activeId = tab.id
        return tab
    }

    /** Nomor terkecil yang belum dipakai tab lain, jadi menutup tab lalu membuka baru tidak menaikkan angka. */
    private fun nextUntitledTitle(): String {
        val used = tabs.map { it.title }.toSet()
        var n = 1
        while (true) {
            val t = if (n == 1) UNTITLED else "$UNTITLED $n"
            if (t !in used) return t
            n++
        }
    }

    /** Buka find bar; seleksi satu baris pendek (maks 64 karakter) menjadi kata cari awal. */
    fun openFind() {
        val ed = active?.editor
        if (ed != null) {
            val c = ed.cursor
            if (c.isSelected && c.right - c.left in 1..64) {
                val sel = ed.text.subSequence(c.left, c.right).toString()
                if ('\n' !in sel) find.query = sel
            }
        }
        find.open = true
        find.focusTick++
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
        const val UNTITLED = "Baru"
    }
}
