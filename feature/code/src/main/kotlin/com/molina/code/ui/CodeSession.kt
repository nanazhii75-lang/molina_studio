package com.molina.code.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

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

    /** Berkas di disk. Null = belum pernah disimpan (Simpan membuka dialog Simpan sebagai). */
    var file by mutableStateOf<File?>(null)
        internal set

    /** True selama isi tab hasil pemulihan draf masih dibaca; editor dikunci agar tidak tertimpa. */
    var loading by mutableStateOf(false)
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

    /** Jumlah edit pengguna (bukan setText). Simpan async hanya membersihkan dirty bila angka ini tidak berubah. */
    internal var edits = 0

    /** Teks hasil pemulihan yang menunggu editor dibuat. */
    internal var pendingText: String? = null

    /** Dipasang sesi: dipanggil tiap edit pengguna untuk menjadwalkan autosave draf. */
    internal var onEdited: (() -> Unit)? = null

    /** Isi tab saat ini; null bila lebih panjang dari [maxChars] atau belum ada. */
    internal fun currentTextOrNull(maxChars: Int): String? {
        val ed = editor
        if (ed != null) {
            val content = ed.text
            return if (content.length > maxChars) null else content.toString()
        }
        val p = pendingText ?: return null
        return if (p.length > maxChars) null else p
    }

    /** Pasang isi hasil pemulihan. Aman dipanggil sebelum maupun sesudah editor dibuat. */
    internal fun applyLoaded(text: String, asDirty: Boolean) {
        val ed = editor
        if (ed == null) pendingText = text else ed.setText(text)
        dirty = asDirty
        loading = false
        ed?.setEditable(!readOnly)
    }

    internal fun disposeEditor() {
        editor?.release()
        editor = null
    }
}

/** Permintaan dialog Simpan sebagai. [after] dipanggil setelah berkas benar-benar tersimpan. */
class SaveAsRequest internal constructor(val tab: EditorTab, val after: (() -> Unit)?)

/**
 * Status layar Code: daftar tab dan tab aktif. Dibuat SATU kali di tingkat Activity supaya tab
 * tidak hilang saat pengguna pindah ke tab bawah lain.
 *
 * Bila [draftDir] diberikan, tab beserta isi yang belum disimpan ditulis ke folder itu (autosave
 * berjeda 1,5 detik, plus [flushDrafts] saat Activity berhenti) dan dipulihkan saat sesi dibuat
 * lagi, jadi tab "Baru" tidak hilang ketika proses dimatikan atau layar diputar.
 */
class CodeSession(val workDir: String, draftDir: File? = null) {
    val tabs = mutableStateListOf<EditorTab>()

    val find = FindState()

    /** Dialog Pergi ke baris terbuka. */
    var gotoOpen by mutableStateOf(false)

    /** Dialog Simpan sebagai yang sedang terbuka (null = tertutup). */
    var saveAs by mutableStateOf<SaveAsRequest?>(null)
        private set

    var activeId by mutableIntStateOf(-1)
        private set

    /** Pesan singkat untuk pengguna (Toast). Dipasang oleh CodeScreen. */
    var toast: (String) -> Unit = {}

    private var nextId = 1
    private var lastSaveDir: File? = null

    private val main = Handler(Looper.getMainLooper())
    private val io: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "molina-code-io").apply { isDaemon = true }
    }
    private val store: DraftStore? = draftDir?.let { DraftStore(it) }
    private val draftTask = Runnable { persistDrafts() }

    val active: EditorTab?
        get() = tabs.firstOrNull { it.id == activeId }

    init {
        if (!restoreDrafts()) newTab()
    }

    fun newTab(): EditorTab {
        val tab = EditorTab(nextId++, nextUntitledTitle())
        tab.onEdited = { scheduleDraft() }
        tabs.add(tab)
        activeId = tab.id
        scheduleDraft()
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
        scheduleDraft()
    }

    // ---------------------------------------------------------------- Simpan (F3e)

    /**
     * Simpan tab. Tab yang sudah punya berkas ditulis langsung; tab baru membuka dialog Simpan
     * sebagai. [after] dipanggil setelah tersimpan dan tanpa edit baru selama penulisan.
     */
    fun save(tab: EditorTab, after: (() -> Unit)? = null) {
        if (tab.loading) return
        val target = tab.file
        if (target == null) saveAs = SaveAsRequest(tab, after) else writeTab(tab, target, after)
    }

    /** Menu titik tiga "Simpan sebagai": selalu membuka dialog untuk tab aktif. */
    fun saveAsActive() {
        val tab = active ?: return
        if (!tab.loading) saveAs = SaveAsRequest(tab, null)
    }

    fun cancelSaveAs() {
        saveAs = null
    }

    /** Folder awal dialog Simpan sebagai: folder berkas tab, lalu folder simpan terakhir, lalu folder kerja. */
    internal fun defaultSaveDir(tab: EditorTab): File =
        tab.file?.parentFile ?: lastSaveDir ?: File(workDir)

    internal fun commitSaveAs(req: SaveAsRequest, target: File) {
        saveAs = null
        lastSaveDir = target.parentFile
        writeTab(req.tab, target, req.after)
    }

    private fun writeTab(tab: EditorTab, target: File, after: (() -> Unit)?) {
        val text = tab.currentTextOrNull(Int.MAX_VALUE) ?: ""
        val mark = tab.edits
        runIo {
            val err = try {
                FileStore.writeUtf8Atomic(target, text)
                null
            } catch (e: Exception) {
                e
            }
            main.post {
                if (err != null) {
                    toast("Gagal menyimpan: ${err.message ?: err.javaClass.simpleName}")
                } else {
                    tab.file = target
                    tab.title = target.name
                    val unchanged = tab.edits == mark
                    if (unchanged) tab.dirty = false
                    scheduleDraft()
                    toast("Tersimpan: ${target.name}")
                    if (unchanged) {
                        after?.invoke()
                    } else if (after != null) {
                        toast("Ada perubahan baru, tab tidak ditutup")
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- Draf (F3e)

    /** Jadwalkan tulis draf 1,5 detik setelah edit terakhir (debounce). */
    internal fun scheduleDraft() {
        if (store == null) return
        main.removeCallbacks(draftTask)
        main.postDelayed(draftTask, DRAFT_DELAY_MS)
    }

    private fun persistDrafts() {
        val st = store ?: return
        val snap = snapshot() ?: return
        runIo { st.write(snap) }
    }

    /** Tulis draf sekarang dan tunggu selesai (maks 2 detik). Dipanggil dari Activity.onStop. */
    fun flushDrafts() {
        val st = store ?: return
        main.removeCallbacks(draftTask)
        val snap = snapshot() ?: return
        try {
            io.submit(Runnable { st.write(snap) }).get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // best-effort
        }
    }

    private fun snapshot(): DraftSnapshot? {
        if (store == null) return null
        val entries = tabs.map { tab ->
            val text = if (tab.dirty && !tab.loading) tab.currentTextOrNull(MAX_DRAFT_CHARS) else null
            DraftEntry(tab.id, tab.title, tab.file?.path, tab.dirty, tab.wordWrap, tab.readOnly, text)
        }
        return DraftSnapshot(activeId, nextId, entries)
    }

    /** Pulihkan daftar tab dari draf. Isi dibaca di thread IO; tab dikunci (loading) sampai selesai. */
    private fun restoreDrafts(): Boolean {
        val st = store ?: return false
        val snap = st.load() ?: return false
        if (snap.tabs.isEmpty()) return false
        for (t in snap.tabs) {
            val tab = EditorTab(t.id, t.title)
            tab.onEdited = { scheduleDraft() }
            tab.file = t.path?.let { File(it) }
            tab.wordWrap = t.wrap
            tab.readOnly = t.readOnly
            tab.dirty = t.dirty
            tabs.add(tab)
            if (!t.dirty && tab.file == null) continue // tab kosong: tidak ada yang dibaca
            tab.loading = true
            runIo {
                var text: String? = if (t.dirty) st.readDraft(t.id) else null
                var asDirty = t.dirty
                if (text == null) {
                    text = tab.file?.let { FileStore.readUtf8OrNull(it) }
                    asDirty = false
                }
                val loaded = text
                val dirtyFinal = asDirty
                main.post {
                    if (loaded == null) {
                        toast("Gagal memuat ${tab.title}")
                        close(tab.id)
                    } else {
                        tab.applyLoaded(loaded, dirtyFinal)
                    }
                }
            }
        }
        nextId = maxOf(snap.next, (snap.tabs.maxOfOrNull { it.id } ?: 0) + 1)
        activeId = if (tabs.any { it.id == snap.active }) snap.active else tabs.first().id
        return true
    }

    private fun runIo(block: () -> Unit) {
        try {
            io.execute(block)
        } catch (e: RejectedExecutionException) {
            // sesi sudah dilepas
        }
    }

    fun releaseAll() {
        main.removeCallbacks(draftTask)
        tabs.forEach { it.disposeEditor() }
        io.shutdown()
    }

    companion object {
        const val UNTITLED = "Baru"
        private const val DRAFT_DELAY_MS = 1500L
        private const val MAX_DRAFT_CHARS = 2_000_000
    }
}
