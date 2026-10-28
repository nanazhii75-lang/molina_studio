package com.molina.code.ui

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.molina.code.preview.PreviewKind
import com.molina.code.preview.RunPlans
import com.molina.code.run.ScriptExecutor
import com.molina.code.run.ScriptShare
import com.molina.code.run.ScriptSink
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

/**
 * Satu tab editor. Tiap tab punya instance editor sendiri (dibuat malas) sehingga isi, posisi
 * kursor, dan riwayat undo/redo terpisah per tab (blueprint bagian 7).
 *
 * Penanda dirty dibandingkan dengan isi tersimpan (baseline berupa sidik jari teks), jadi undo
 * sampai kembali ke isi awal menghapus titik emas (blueprint bagian 31.2).
 */
class EditorTab internal constructor(val id: Int, initialTitle: String) {
    var title by mutableStateOf(initialTitle)
        internal set

    /** True bila isi berbeda dari yang tersimpan/dibuka. Menjadi titik emas pada judul tab. */
    var dirty by mutableStateOf(false)
        internal set

    /** Berkas di disk. Null = belum pernah disimpan (Simpan membuka dialog Simpan sebagai). */
    var file by mutableStateOf<File?>(null)
        internal set

    /** True selama isi tab masih dibaca dari disk/draf; editor dikunci agar tidak tertimpa. */
    var loading by mutableStateOf(false)
        internal set

    /** Naik setiap isi berubah (setelah event selesai). Memicu hitung ulang state Undo/Redo. */
    var revision by mutableIntStateOf(0)
        internal set

    /** Pembungkus kata per tab (default aktif, blueprint v2 bagian 12). Diubah dari menu titik tiga. */
    var wordWrap by mutableStateOf(true)

    /** Hanya baca per tab. Diubah dari menu titik tiga. */
    var readOnly by mutableStateOf(false)

    /** Kunci entri Script Execution bila tab ini hasil EDIT dari Settings; dipakai saat Kirim ke Script Execution. */
    internal var scriptKey: String? = null

    /** Naik saat hasil pencarian atau seleksi berubah; memicu hitung ulang penghitung find bar. */
    var findRevision by mutableIntStateOf(0)
        internal set

    /** Akhir baris yang dipakai saat menulis ke disk. Editor sendiri selalu memegang teks LF. */
    internal var eol by mutableStateOf(Eol.LF)

    /** State agar find bar dan toolbar ikut diperbarui begitu editor tab selesai dibuat. */
    internal var editor by mutableStateOf<CodeEditor?>(null)

    /** Sidik jari isi tersimpan/dibuka (pembanding dirty). Tab "Baru" = sidik jari teks kosong. */
    internal var baseFp: Long = FileStore.EMPTY_FP

    /** Akhir baris pada isi tersimpan/dibuka. Beda dari [eol] = dirty (hanya akhir baris yang berubah). */
    internal var baseEol: Eol = Eol.LF

    /** Teks hasil pemulihan yang menunggu editor dibuat. */
    internal var pendingText: String? = null

    /** Teks dari Terminal yang menunggu editor dibuat (lihat CodeSession.insertAtCursor). */
    internal var queuedInsert: String? = null

    /** Dipasang sesi: dipanggil tiap perubahan status isi untuk menjadwalkan autosave draf. */
    internal var onEdited: (() -> Unit)? = null

    private val checkTask = Runnable { recomputeDirty() }

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

    /** Tab kosong yang belum pernah disentuh; boleh dipakai ulang saat membuka berkas. */
    internal fun isPristine(): Boolean {
        if (file != null || dirty || loading || queuedInsert != null) return false
        val len = editor?.text?.length ?: (pendingText?.length ?: 0)
        return len == 0
    }

    /** Bandingkan isi sekarang dengan baseline. Dipanggil setelah edit (ditunda) dan setelah simpan/muat. */
    internal fun recomputeDirty() {
        if (loading) return
        val ed = editor
        val text: String? = if (ed != null) {
            val c = ed.text
            if (c.length > MAX_COMPARE_CHARS) null else c.toString()
        } else {
            pendingText ?: ""
        }
        val now = text == null || FileStore.fingerprint(text) != baseFp || eol != baseEol
        if (now != dirty) {
            dirty = now
            onEdited?.invoke()
        }
    }

    /** Tunda hitung ulang dirty sampai edit berhenti (trailing debounce), supaya mengetik tetap ringan. */
    internal fun scheduleDirtyCheck(view: View) {
        view.removeCallbacks(checkTask)
        view.postDelayed(checkTask, DIRTY_CHECK_MS)
    }

    /** Isi [text] kini tersimpan di disk dengan akhir baris [savedEol]. */
    internal fun markSaved(text: String, savedEol: Eol) {
        baseFp = FileStore.fingerprint(text)
        baseEol = savedEol
        recomputeDirty()
    }

    /** Pasang isi bersih dari disk (buka atau muat ulang). Teks sudah LF. */
    internal fun applyDiskText(text: String, fileEol: Eol) {
        setBody(text)
        eol = fileEol
        baseEol = fileEol
        baseFp = FileStore.fingerprint(text)
        dirty = false
        loading = false
        editor?.setEditable(!readOnly)
    }

    /** Pasang isi dari draf: baseline dan akhir baris dikembalikan, lalu dirty dihitung ulang. */
    internal fun applyDraft(text: String, draftEol: Eol, base: Long, baseEolSaved: Eol) {
        setBody(text)
        eol = draftEol
        baseFp = base
        baseEol = baseEolSaved
        loading = false
        editor?.setEditable(!readOnly)
        dirty = true
        recomputeDirty()
    }

    /** Berkas tab dihapus dari disk: isi tinggal di editor, jadi tab ditandai belum tersimpan. */
    internal fun markOrphaned() {
        file = null
        baseFp = FileStore.ORPHAN_FP
        dirty = true
        onEdited?.invoke()
    }

    private fun setBody(text: String) {
        val ed = editor
        if (ed == null) pendingText = text else ed.setText(text)
    }

    internal fun disposeEditor() {
        editor?.release()
        editor = null
    }

    companion object {
        private const val DIRTY_CHECK_MS = 150L
        private const val MAX_COMPARE_CHARS = 2_000_000
    }
}

/** Permintaan dialog Simpan sebagai. [after] dipanggil setelah berkas benar-benar tersimpan. */
class SaveAsRequest internal constructor(val tab: EditorTab, val after: (() -> Unit)?)

/**
 * Status layar Code: daftar tab, tab aktif, dan panel direktori. Dibuat SATU kali di tingkat
 * Activity supaya tab tidak hilang saat pengguna pindah ke tab bawah lain.
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

    /** [CODE-PALETTE] Palet Perintah terbuka. */
    var paletteOpen by mutableStateOf(false)

    /** [CODE-SEARCH] Dialog Cari di berkas dan Struktur berkas terbuka. */
    var findFilesOpen by mutableStateOf(false)
    var outlineOpen by mutableStateOf(false)

    /** Dialog Simpan sebagai yang sedang terbuka (null = tertutup). */
    var saveAs by mutableStateOf<SaveAsRequest?>(null)
        private set

    /** Dialog aksi berkas (menu, ganti nama, hapus, baru). Dirender oleh [FileDialogs]. */
    internal var fileDialog by mutableStateOf<FileDialog?>(null)

    /** Pemilih berkas/folder yang sedang terbuka (null = tertutup). */
    internal var picker by mutableStateOf<PickerRequest?>(null)

    /** Tab dirty yang menunggu konfirmasi Muat ulang. */
    internal var reloadConfirm by mutableStateOf<EditorTab?>(null)
        private set

    var activeId by mutableIntStateOf(-1)
        private set

    /** Pesan singkat untuk pengguna (Toast). Dipasang oleh CodeScreen. */
    var toast: (String) -> Unit = {}

    private var nextId = 1
    private var lastSaveDir: File? = null

    internal val mainHandler = Handler(Looper.getMainLooper())
    private val io: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "molina-code-io").apply { isDaemon = true }
    }
    private val store: DraftStore? = draftDir?.let { DraftStore(it) }
    private val draftTask = Runnable { persistDrafts() }

    /** Panel direktori (F4). Folder utama awal = folder kerja. */
    internal val dir = DirectoryState(this, File(workDir))

    /** Panel pratinjau langsung / keluaran skrip di bawah editor (tombol Play). */
    internal val preview = PreviewController()

    /** True saat halaman penuh terbuka (Tinjauan Markdown/JSON/HTML atau Keluaran skrip); :app menyembunyikan bottom nav. */
    val previewFullScreen: Boolean
        get() = preview.open

    /** Opsi Settings "Pakai WebView untuk HTML"; dipasang :app. */
    var htmlWebView: Boolean
        get() = preview.htmlWebView
        set(value) { preview.htmlWebView = value }

    /** Status eksekusi skrip untuk panel Keluaran. */
    internal val run = RunController(mainHandler)

    /** Dipasang :app (Debian PRoot). Null = tombol Play untuk skrip memberi tahu bahwa belum tersedia. */
    var scriptExecutor: ScriptExecutor?
        get() = run.executor
        set(value) { run.executor = value }

    /** Dipasang :app. Null = menu \"Kirim ke Script Execution\" redup. */
    var scriptSink: ScriptSink? = null

    /** Dipasang :app. (teks, jalankan) -> pesan galat atau null. Null = menu Terminal redup. */
    var terminalSink: ((String, Boolean) -> String?)? = null

    /** Teks seleksi aktif, atau baris kursor bila tidak ada seleksi. Kosong bila tidak ada editor. */
    internal fun selectionOrLine(): String {
        val ed = active?.editor ?: return ""
        return try {
            val c = ed.cursor
            if (c.isSelected) ed.text.substring(c.left, c.right) else ed.text.getLineString(c.leftLine)
        } catch (e: Exception) {
            ""
        }
    }

    /** Kirim seleksi (atau baris kursor) ke Terminal; [execute] true = langsung Enter. */
    fun sendToTerminal(execute: Boolean) {
        val sink = terminalSink
        if (sink == null) { toast("Terminal belum tersedia"); return }
        val text = selectionOrLine().trimEnd('\n', '\r')
        if (text.isBlank()) { toast("Tidak ada teks untuk dikirim"); return }
        sink(text, execute)?.let { toast(it) }
    }

    /** Dipasang :app. (nama, isi) -> pesan hasil untuk toast. Null = menu Simpan sebagai snippet redup. */
    var snippetSink: ((String, String) -> String)? = null

    /** Simpan seleksi (atau baris kursor) sebagai snippet Terminal. Nama = baris pertama yang tidak kosong. */
    fun saveSelectionAsSnippet() {
        val sink = snippetSink
        if (sink == null) { toast("Snippet belum tersedia"); return }
        val text = selectionOrLine().trimEnd('\n', '\r')
        if (text.isBlank()) { toast("Tidak ada teks untuk disimpan"); return }
        if (text.length > 4000) { toast("Teks terlalu panjang untuk snippet (maks. 4000 karakter)"); return }
        val name = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty().take(40)
        toast(sink(name, text))
    }

    /**
     * Sisipkan teks di posisi kursor tab aktif (mengganti seleksi bila ada). Mengembalikan pesan galat atau null.
     * Bila editor tab belum dibuat (tab Code belum pernah tampil), teks diantrekan dan disisipkan saat editor dibuat.
     */
    fun insertAtCursor(text: String): String? {
        val tab = active ?: return "Tidak ada tab editor"
        if (tab.loading) return "Editor sedang memuat"
        if (tab.readOnly) return "Tab ini hanya-baca"
        val clean = text.replace("\r\n", "\n").replace('\r', '\n')
        val ed = tab.editor
        if (ed == null) {
            tab.queuedInsert = (tab.queuedInsert ?: "") + clean
            return null
        }
        return try {
            ed.insertText(clean, clean.length)
            null
        } catch (e: Exception) {
            "Gagal menyisipkan teks"
        }
    }

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

    // ---------------------------------------------------------------- Buka berkas (F4)

    /** Tab yang sedang memegang [file] (dibandingkan lewat path kanonik), atau null. */
    internal fun tabFor(file: File): EditorTab? {
        val key = FileStore.keyOf(file)
        return tabs.firstOrNull { t -> t.file?.let { FileStore.keyOf(it) } == key }
    }

    /** True bila ada tab yang berkasnya sama dengan atau berada di bawah [file] (folder). */
    internal fun hasTabUnder(file: File): Boolean {
        val key = FileStore.keyOf(file)
        return tabs.any { t ->
            val k = t.file?.let { FileStore.keyOf(it) } ?: return@any false
            k == key || k.startsWith("$key/")
        }
    }

    /** Folder awal pemilih Buka: folder berkas tab aktif, lalu folder utama panel. */
    internal fun defaultOpenDir(): File = active?.file?.parentFile ?: dir.root

    /**
     * Buka berkas di tab. Berkas yang sudah terbuka hanya dipilih (tidak ada tab ganda). Tab "Baru"
     * yang masih kosong dipakai ulang. Isi dibaca di thread IO; tab dikunci selama dibaca.
     */
    fun openFile(file: File) {
        dir.closePanel()
        val existing = tabFor(file)
        if (existing != null) {
            activeId = existing.id
            return
        }
        val reuse = active?.takeIf { it.isPristine() }
        val tab = reuse ?: newTab()
        tab.file = file
        tab.title = file.name
        tab.loading = true
        tab.editor?.setEditable(false)
        dir.selected = file.path
        scheduleDraft()
        runIo {
            val r = FileStore.readForOpen(file)
            mainHandler.post {
                if (tabs.none { it === tab }) return@post
                val text = r.text
                if (text == null) {
                    toast(r.error ?: "Gagal membuka ${file.name}")
                    if (reuse != null) {
                        tab.file = null
                        tab.title = nextUntitledTitle()
                        tab.loading = false
                        tab.editor?.setEditable(!tab.readOnly)
                    } else {
                        close(tab.id)
                    }
                } else {
                    tab.applyDiskText(text, r.eol)
                    if (r.hadBom) toast("BOM dibuang saat berkas disimpan")
                }
                scheduleDraft()
            }
        }
    }

    // ---------------------------------------------------------------- Muat ulang (F4)

    /** Muat ulang tab aktif dari disk. Tab dirty meminta konfirmasi dulu (buang perubahan). */
    fun reloadActive() {
        val tab = active ?: return
        if (tab.file == null) {
            toast("Berkas belum disimpan")
            return
        }
        if (tab.loading) return
        if (tab.dirty) reloadConfirm = tab else doReload(tab)
    }

    internal fun cancelReload() {
        reloadConfirm = null
    }

    internal fun doReload(tab: EditorTab) {
        reloadConfirm = null
        val f = tab.file ?: return
        tab.loading = true
        tab.editor?.setEditable(false)
        runIo {
            val r = FileStore.readForOpen(f)
            mainHandler.post {
                if (tabs.none { it === tab }) return@post
                val text = r.text
                if (text == null) {
                    toast(r.error ?: "Gagal memuat ulang ${f.name}")
                    tab.loading = false
                    tab.editor?.setEditable(!tab.readOnly)
                } else {
                    tab.applyDiskText(text, r.eol)
                    toast("Dimuat ulang: ${f.name}")
                }
                scheduleDraft()
            }
        }
    }

    // ---------------------------------------------------------------- Sinkron tab dengan operasi berkas (F4)

    /** Berkas/folder [old] diganti nama jadi [new]: arahkan tab yang menunjuk ke sana. */
    internal fun onRenamed(old: File, new: File) {
        val oldKey = FileStore.keyOf(old)
        for (t in tabs) {
            val f = t.file ?: continue
            val k = FileStore.keyOf(f)
            if (k == oldKey) {
                t.file = new
                t.title = new.name
            } else if (k.startsWith("$oldKey/")) {
                t.file = File(new, k.substring(oldKey.length + 1))
            }
        }
        scheduleDraft()
    }

    /** Berkas/folder [gone] dihapus: tab di dalamnya tetap terbuka sebagai belum tersimpan. */
    internal fun onDeleted(gone: File) {
        val key = FileStore.keyOf(gone)
        var orphaned = 0
        for (t in tabs) {
            val f = t.file ?: continue
            val k = FileStore.keyOf(f)
            if (k == key || k.startsWith("$key/")) {
                t.markOrphaned()
                orphaned++
            }
        }
        if (orphaned > 0) toast("Berkas dihapus dari disk. Tab tetap terbuka (belum disimpan)")
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

    /** Folder awal dialog Simpan sebagai: folder berkas tab, lalu folder simpan terakhir, lalu folder utama panel. */
    internal fun defaultSaveDir(tab: EditorTab): File =
        tab.file?.parentFile ?: lastSaveDir ?: dir.root

    internal fun commitSaveAs(req: SaveAsRequest, target: File) {
        saveAs = null
        lastSaveDir = target.parentFile
        writeTab(req.tab, target, req.after)
    }

    private fun writeTab(tab: EditorTab, target: File, after: (() -> Unit)?) {
        val text = tab.currentTextOrNull(Int.MAX_VALUE) ?: ""
        val eol = tab.eol
        runIo {
            val err = try {
                FileStore.writeUtf8Atomic(target, FileStore.applyEol(text, eol))
                null
            } catch (e: Exception) {
                e
            }
            mainHandler.post {
                if (err != null) {
                    toast("Gagal menyimpan: ${err.message ?: err.javaClass.simpleName}")
                } else {
                    tab.file = target
                    tab.title = target.name
                    // Baseline = isi yang barusan ditulis. Bila pengguna mengetik selama penulisan,
                    // recomputeDirty membiarkan tab tetap dirty dan "after" tidak dijalankan.
                    tab.markSaved(text, eol)
                    dir.selected = target.path
                    dir.refreshParentOf(target)
                    scheduleDraft()
                    toast("Tersimpan: ${target.name}")
                    if (!tab.dirty) {
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
        mainHandler.removeCallbacks(draftTask)
        mainHandler.postDelayed(draftTask, DRAFT_DELAY_MS)
    }

    private fun persistDrafts() {
        val st = store ?: return
        val snap = snapshot() ?: return
        runIo { st.write(snap) }
    }

    /** Tulis draf sekarang dan tunggu selesai (maks 2 detik). Dipanggil dari Activity.onStop. */
    fun flushDrafts() {
        val st = store ?: return
        mainHandler.removeCallbacks(draftTask)
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
            DraftEntry(
                tab.id, tab.title, tab.file?.path, tab.dirty, tab.wordWrap, tab.readOnly, text,
                tab.eol, tab.baseEol, tab.baseFp
            )
        }
        return DraftSnapshot(activeId, nextId, entries, dir.root.path)
    }

    /** Pulihkan daftar tab dari draf. Isi dibaca di thread IO; tab dikunci (loading) sampai selesai. */
    private fun restoreDrafts(): Boolean {
        val st = store ?: return false
        val snap = st.load() ?: return false
        if (snap.tabs.isEmpty()) return false
        snap.root?.let { dir.restoreRoot(File(it)) }
        for (t in snap.tabs) {
            val tab = EditorTab(t.id, t.title)
            tab.onEdited = { scheduleDraft() }
            tab.file = t.path?.let { File(it) }
            tab.wordWrap = t.wrap
            tab.readOnly = t.readOnly
            tab.eol = t.eol
            tab.baseEol = t.baseEol
            tab.baseFp = t.baseFp
            tab.dirty = t.dirty
            tabs.add(tab)
            if (!t.dirty && tab.file == null) continue // tab kosong: tidak ada yang dibaca
            tab.loading = true
            runIo {
                val draft: String? = if (t.dirty) st.readDraft(t.id) else null
                val disk: OpenResult? = if (draft == null) tab.file?.let { FileStore.readForOpen(it) } else null
                mainHandler.post {
                    val diskText = disk?.text
                    if (draft != null) {
                        tab.applyDraft(draft, t.eol, t.baseFp, t.baseEol)
                    } else if (disk != null && diskText != null) {
                        tab.applyDiskText(diskText, disk.eol)
                    } else {
                        toast("Gagal memuat ${tab.title}")
                        close(tab.id)
                    }
                }
            }
        }
        nextId = maxOf(snap.next, (snap.tabs.maxOfOrNull { it.id } ?: 0) + 1)
        activeId = if (tabs.any { it.id == snap.active }) snap.active else tabs.first().id
        return true
    }

    /** Jalankan [block] di thread IO tunggal sesi. Aman dipanggil setelah sesi dilepas (diabaikan). */
    internal fun runIo(block: () -> Unit) {
        try {
            io.execute(block)
        } catch (e: RejectedExecutionException) {
            // sesi sudah dilepas
        }
    }

    /**
     * Tombol Play. Skrip berjalan: hentikan. Markdown/JSON: buka/tutup pratinjau langsung.
     * .sh/.py: jalankan isi editor sekarang (tidak perlu disimpan) di Debian PRoot.
     */
    internal fun playActive() {
        if (run.running) {
            run.stop()
            return
        }
        val tab = active ?: return
        if (tab.loading) return
        when (tab.previewKind()) {
            PreviewKind.MARKDOWN, PreviewKind.JSON, PreviewKind.HTML -> {
                if (preview.open && preview.mode == PaneMode.Preview) {
                    preview.open = false
                } else {
                    preview.invalidate()
                    preview.anchorLine = tab.editor?.cursor?.leftLine ?: -1
                    preview.mode = PaneMode.Preview
                    preview.open = true
                }
            }
            PreviewKind.SCRIPT -> runScript(tab)
            PreviewKind.NONE -> toast("Play mendukung berkas .sh, .py, .md, .json, dan .html")
        }
    }

    /** Menu titik tiga: kirim isi tab aktif ke Script Execution (Settings). Berkas sama = entri diperbarui. */
    internal fun sendToScripts() {
        val sink = scriptSink ?: run { toast("Script Execution belum tersedia"); return }
        val tab = active ?: return
        if (tab.loading) return
        val text = tab.currentTextOrNull(MAX_RUN_CHARS) ?: run { toast("Skrip terlalu besar untuk dikirim"); return }
        if (text.isBlank()) { toast("Editor kosong, tidak ada yang dikirim"); return }
        val title = tab.file?.name ?: tab.title
        val key = tab.scriptKey ?: tab.file?.path ?: "tab:${tab.title}"
        val res = sink.send(ScriptShare(title, key, text))
        toast(res.message)
        // Tab hasil EDIT dari Script Execution: kiriman berhasil = tersimpan, penanda belum-disimpan padam.
        if (res.ok && tab.scriptKey != null && tab.file == null) tab.markSaved(text, tab.eol)
    }

    /**
     * Buka isi skrip dari Script Execution di tab editor (tombol EDIT di Settings). Tab dengan [key] yang sama
     * hanya dipilih; tab kosong yang belum disentuh dipakai ulang. Isi dianggap belum tersimpan sampai
     * dikirim balik lewat menu titik tiga > Kirim ke Script Execution.
     */
    fun openScript(title: String, key: String, text: String) {
        dir.closePanel()
        val existing = tabs.firstOrNull { it.scriptKey == key }
        if (existing != null) {
            activeId = existing.id
            return
        }
        val tab = active?.takeIf { it.isPristine() } ?: newTab()
        tab.title = title
        tab.scriptKey = key
        tab.applyDraft(text.replace("\r\n", "\n").replace('\r', '\n'), Eol.LF, FileStore.EMPTY_FP, Eol.LF)
        activeId = tab.id
        scheduleDraft()
    }

    private fun runScript(tab: EditorTab) {
        val text = tab.currentTextOrNull(MAX_RUN_CHARS)
        if (text == null) {
            toast("Skrip terlalu besar untuk dijalankan")
            return
        }
        val plan = RunPlans.of(tab.file?.name, tab.firstLineOrNull())
        if (plan == null) {
            toast("Interpreter skrip tidak dikenal")
            return
        }
        if (run.executor == null) {
            toast("Eksekutor skrip belum terpasang")
            return
        }
        preview.mode = PaneMode.Output
        preview.open = true
        run.start(tab.title, text, plan, tab.file?.parentFile?.path)
    }

    // [CODE-PALETTE] buka berkas:baris dari tautan error di panel Keluaran
    internal fun openLocation(path: String, line: Int, col: Int) {
        val f = resolveLocation(path)
        val base = path.substringAfterLast('/')
        val tab: EditorTab? = if (f != null) {
            openFile(f)
            tabFor(f)
        } else {
            tabs.firstOrNull { it.file?.name == base || it.title == base }
                ?: tabs.firstOrNull { it.title == run.title }
        }
        if (tab == null) {
            toast("Berkas tidak ditemukan: $path")
            return
        }
        preview.open = false
        activeId = tab.id
        jumpWhenReady(tab, line - 1, col, 40)
    }

    private fun resolveLocation(raw: String): File? {
        var p = raw.removePrefix("file://")
        if (p.startsWith("/sdcard/")) p = "/storage/emulated/0/" + p.removePrefix("/sdcard/")
        val cands = ArrayList<File>()
        if (p.startsWith("/")) {
            cands.add(File(p))
        } else {
            active?.file?.parentFile?.let { cands.add(File(it, p)) }
            cands.add(File(workDir, p))
            cands.add(File(dir.root, p))
        }
        return cands.firstOrNull { it.isFile }
    }

    /** Lompat ke [line] (basis 0) begitu editor tab siap dan teksnya selesai dimuat; mencoba ulang tiap 100 ms. */
    private fun jumpWhenReady(tab: EditorTab, line: Int, col: Int, tries: Int) {
        val ed = tab.editor
        val total = ed?.text?.lineCount ?: 0
        if (ed != null && !tab.loading && total > 0 && (line < total || tries <= 0)) {
            ed.jumpToLineHighlighted(line.coerceIn(0, total - 1), col)
            return
        }
        if (tries <= 0) return
        mainHandler.postDelayed({ jumpWhenReady(tab, line, col, tries - 1) }, 100L)
    }

    fun releaseAll() {
        run.release()
        mainHandler.removeCallbacks(draftTask)
        tabs.forEach { it.disposeEditor() }
        io.shutdown()
    }

    companion object {
        const val UNTITLED = "Baru"
        private const val DRAFT_DELAY_MS = 1500L
        private const val MAX_DRAFT_CHARS = 2_000_000
        private const val MAX_RUN_CHARS = 500_000
    }
}
