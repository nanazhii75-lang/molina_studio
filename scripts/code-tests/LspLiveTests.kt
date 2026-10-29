import com.molina.code.check.Diagnostic
import com.molina.code.lsp.LiveState
import com.molina.code.lsp.LspDecoder
import com.molina.code.lsp.LspFraming
import com.molina.code.lsp.LspJson
import com.molina.code.lsp.LspLive
import com.molina.code.lsp.LspLiveConfig
import com.molina.code.lsp.LspLiveEnv
import com.molina.code.lsp.LspLiveEvents
import com.molina.code.lsp.LspPathMap
import com.molina.code.lsp.LspServers
import com.molina.code.lsp.MemInfo
import com.molina.code.lsp.StreamExecutor
import com.molina.code.lsp.StreamHandle
import com.molina.code.lsp.StreamListener
import com.molina.code.lsp.StreamRequest
import com.molina.code.lsp.StreamResult
import com.molina.code.lsp.TextStamp

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

private const val ROOT = "/storage/emulated/0"
private const val FILE = "$ROOT/molina-studio/a.py"

/** Waktu palsu: jadwal berjalan hanya saat advance(). post() langsung jalan. */
private class FakeEnv : LspLiveEnv {
    var clock = 0L
    var mem = 2_000_000L
    private class Job(val at: Long, val task: () -> Unit) { var live = true }
    private val jobs = ArrayList<Job>()
    override fun schedule(delayMs: Long, task: () -> Unit): () -> Unit {
        val j = Job(clock + delayMs, task); jobs.add(j); return { j.live = false }
    }
    override fun post(task: () -> Unit) = task()
    override fun now() = clock
    override fun memAvailableKb() = mem
    fun advance(ms: Long) {
        val end = clock + ms
        while (true) {
            val next = jobs.filter { it.live && it.at <= end }.minByOrNull { it.at } ?: break
            clock = next.at; next.live = false; next.task()
        }
        clock = end
    }
    fun pending() = jobs.count { it.live }
}

private class FakeHandle : StreamHandle {
    val sent = ArrayList<Map<*, *>>()
    private val dec = LspDecoder()
    var cancels = 0
    override fun write(data: ByteArray): Boolean { dec.feed(data, 0, data.size) { sent.add(LspJson.parse(it) as Map<*, *>) }; return true }
    override fun closeInput() {}
    override fun cancel() { cancels++ }
    fun methods() = sent.map { it["method"] }
}

private class FakeExec(val syncExit: StreamResult? = null) : StreamExecutor {
    val handles = ArrayList<FakeHandle>()
    val listeners = ArrayList<StreamListener>()
    val requests = ArrayList<StreamRequest>()
    override fun startStream(request: StreamRequest, listener: StreamListener): StreamHandle {
        val h = FakeHandle(); handles.add(h); listeners.add(listener); requests.add(request)
        syncExit?.let { listener.onExit(it) }
        return h
    }
    val handle get() = handles.last()
    val listener get() = listeners.last()
    fun server(json: String) { val b = LspFraming.encode(json); listener.onStdout(b, b.size) }
    fun initOk() = server("""{"jsonrpc":"2.0","id":1,"result":{"capabilities":{}}}""")
    fun answerShutdown() { // id berikutnya setelah initialize = 2
        server("""{"jsonrpc":"2.0","id":2,"result":null}""")
    }
}

private class Ev : LspLiveEvents {
    val states = ArrayList<Pair<LiveState, String?>>()
    val notices = ArrayList<String>()
    val diags = ArrayList<Triple<Int, String, List<Diagnostic>>>()
    val stamps = ArrayList<TextStamp?>()
    override fun onState(state: LiveState, detail: String?) { states.add(state to detail) }
    override fun onNotice(message: String) { notices.add(message) }
    override fun onDiagnostics(tabId: Int, hostPath: String, items: List<Diagnostic>, stamp: TextStamp?) { diags.add(Triple(tabId, hostPath, items)); stamps.add(stamp) }
    fun last() = states.lastOrNull()?.first
}

private class Rig(val cfg: LspLiveConfig = LspLiveConfig(), val exec: FakeExec = FakeExec()) {
    val env = FakeEnv()
    val ev = Ev()
    val live = LspLive(env, ev, { exec }, { LspPathMap(ROOT, "molina-studio", false, emptyList()) }, cfg)
    var text = "import os\n"
    fun touch(tab: Int = 1, path: String = FILE) = live.touch(tab, path, LspServers.PYTHON) { text }
    fun ready() { touch(); env.advance(cfg.debounceMs); exec.initOk() }
}

private fun diag(src: String, msg: String = "m") = """{"range":{"start":{"line":0,"character":0},"end":{"line":0,"character":1}},"severity":2,"source":"$src","message":"$msg"}"""
private fun publish(path: String, vararg items: String) =
    """{"jsonrpc":"2.0","method":"textDocument/publishDiagnostics","params":{"uri":"$path","diagnostics":[${items.joinToString(",")}]}}"""

fun main() {
    // ---- ServerSpec / MemInfo
    check("py = Python", LspServers.of("a.py", null) === LspServers.PYTHON && LspServers.of("A.PYW", null) === LspServers.PYTHON)
    check("shebang python tanpa ekstensi", LspServers.of("skrip", "#!/usr/bin/env python3") === LspServers.PYTHON)
    check("sh bukan Python", LspServers.of("a.sh", "#!/bin/bash") == null && LspServers.of("a.txt", null) == null)
    check("shebang python pada .sh tidak dianggap", LspServers.of("a.sh", "#!/usr/bin/python") == null)
    check("MemInfo MemAvailable", MemInfo.availableKb("MemTotal: 3700000 kB\nMemAvailable:    1300000 kB\n") == 1_300_000L)
    check("MemInfo tanpa MemAvailable = -1", MemInfo.availableKb("MemTotal: 1 kB\n") == -1L && MemInfo.availableKb("") == -1L)
    check("TextStamp", TextStamp(3, "abc".hashCode()).matches("abc") && !TextStamp(3, "abc".hashCode()).matches("abd"))

    run { // mati secara bawaan
        val r = Rig(); r.touch(); r.env.advance(5000)
        check("saklar mati: tidak ada proses", r.exec.handles.isEmpty() && r.live.state == LiveState.OFF)
    }
    run { // debounce
        val r = Rig(); r.live.setEnabled(true)
        check("dinyalakan: IDLE tanpa proses", r.live.state == LiveState.IDLE && r.exec.handles.isEmpty())
        r.touch(); r.env.advance(300); r.touch(); r.env.advance(300); r.touch()
        check("sebelum debounce selesai: belum ada proses", r.exec.handles.isEmpty())
        r.env.advance(699)
        check("699 ms sejak sentuhan terakhir: masih belum", r.exec.handles.isEmpty())
        r.env.advance(1)
        check("debounce selesai: SATU proses, STARTING", r.exec.handles.size == 1 && r.live.state == LiveState.STARTING)
        check("argv dan cwd benar", r.exec.requests[0].guestArgv == listOf("pylsp") && r.exec.requests[0].hostDir == "$ROOT/molina-studio" && r.exec.requests[0].missingHint!!.contains("python3-pylsp"))
        check("initialize memakai rootUri guest", ((r.exec.handle.sent[0]["params"] as Map<*, *>)["rootUri"]) == "file:///sdcard/molina-studio")
        r.exec.initOk()
        check("READY", r.live.state == LiveState.READY)
        check("didOpen berisi teks saat debounce selesai", r.exec.handle.methods().contains("textDocument/didOpen"))
        r.text = "import sys\n"; r.touch(); r.env.advance(700)
        check("edit berikutnya: didChange, proses tetap satu", r.exec.handle.methods().last() == "textDocument/didChange" && r.exec.handles.size == 1)
        r.touch(); r.env.advance(700)
        check("teks sama: tidak ada didChange tambahan", r.exec.handle.methods().count { it == "textDocument/didChange" } == 1)
    }
    run { // diagnostik: terjemah path, sembunyikan pycodestyle, stempel teks
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.exec.server(publish("file:///sdcard/molina-studio/a.py", diag("pyflakes", "x"), diag("pycodestyle", "gaya")))
        check("diagnostik sampai dengan tabId dan path Android", r.ev.diags.size == 1 && r.ev.diags[0].first == 1 && r.ev.diags[0].second == FILE)
        check("pycodestyle disembunyikan", r.ev.diags[0].third.size == 1 && r.ev.diags[0].third[0].source == "pyflakes")
        check("stempel cocok dengan teks yang dikirim", r.ev.stamps[0]!!.matches("import os\n") && !r.ev.stamps[0]!!.matches("import sys\n"))
    }
    run { // idle
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.env.advance(299_000)
        check("belum idle di 299 dtk", r.live.state == LiveState.READY)
        r.env.advance(1_000)
        check("idle 5 menit: shutdown dikirim", r.exec.handle.methods().last() == "shutdown")
        r.exec.answerShutdown()
        check("server menjawab: SLEEPING dan proses dimatikan", r.live.state == LiveState.SLEEPING && r.exec.handle.cancels >= 1)
        r.touch(); r.env.advance(700)
        check("edit lagi: sesi BARU (proses kedua)", r.exec.handles.size == 2 && r.live.state == LiveState.STARTING)
        r.exec.initOk()
        check("sesi baru membuka ulang dokumen", r.exec.handle.methods().contains("textDocument/didOpen"))
    }
    run { // idle: edit menunda
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.env.advance(200_000); r.text = "x = 1\n"; r.touch(); r.env.advance(700)
        r.env.advance(200_000)
        check("edit menggeser jadwal idle", r.live.state == LiveState.READY)
    }
    run { // server tidak menjawab shutdown -> kill
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.env.advance(300_000)
        check("shutdown terkirim, belum di-kill", r.exec.handle.methods().last() == "shutdown" && r.exec.handle.cancels == 0)
        r.env.advance(3_000)
        check("lewat masa tenggang: kill", r.exec.handle.cancels >= 1)
        r.exec.listener.onExit(StreamResult(-1, true, 1))
        check("setelah proses mati: SLEEPING", r.live.state == LiveState.SLEEPING)
    }
    run { // RAM tipis saat mulai
        val r = Rig(); r.live.setEnabled(true); r.env.mem = 300_000
        r.touch(); r.env.advance(700)
        check("RAM < 600 MB: tidak mulai, ada notice", r.exec.handles.isEmpty() && r.ev.notices.size == 1 && r.ev.states.last().second == "RAM rendah")
        r.touch(); r.env.advance(700)
        check("dalam masa tunda: tidak mencoba dan tidak mengulang notice", r.exec.handles.isEmpty() && r.ev.notices.size == 1)
        r.env.mem = 1_500_000; r.env.advance(60_000); r.touch(); r.env.advance(700)
        check("RAM pulih dan masa tunda lewat: mulai", r.exec.handles.size == 1)
    }
    run { // RAM tipis saat berjalan
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.env.mem = 200_000
        r.env.advance(20_000)
        check("RAM < 250 MB: sesi dihentikan dengan notice", r.exec.handle.methods().last() == "shutdown" && r.ev.notices.any { it.contains("dihentikan") })
        r.exec.answerShutdown()
        check("setelah RAM stop: IDLE 'RAM rendah' dan tidak langsung bangun", r.live.state == LiveState.IDLE && r.ev.states.last().second == "RAM rendah")
        r.touch(); r.env.advance(700)
        check("masa tunda RAM mencegah restart", r.exec.handles.size == 1)
    }
    run { // RAM tidak diketahui -> dilewati
        val r = Rig(); r.live.setEnabled(true); r.env.mem = -1
        r.touch(); r.env.advance(700)
        check("RAM tak diketahui: tetap mulai", r.exec.handles.size == 1)
    }
    run { // proses mati sendiri -> ERROR, cooldown, menyerah
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.exec.listener.onExit(StreamResult(139, false, 5))
        check("proses mati: ERROR + satu notice", r.live.state == LiveState.ERROR && r.ev.notices.size == 1)
        r.touch(); r.env.advance(700)
        check("dalam cooldown 10 dtk: tidak restart", r.exec.handles.size == 1)
        r.env.advance(10_000); r.text = "a\n"; r.touch(); r.env.advance(700)
        check("setelah cooldown: restart", r.exec.handles.size == 2)
        r.exec.listener.onExit(StreamResult(139, false, 5))
        r.env.advance(10_000); r.text = "b\n"; r.touch(); r.env.advance(700)
        check("gagal kedua: restart ketiga", r.exec.handles.size == 3)
        r.exec.listener.onExit(StreamResult(139, false, 5))
        check("gagal ketiga: menyerah dengan notice", r.live.state == LiveState.ERROR && r.ev.notices.last().contains("dimatikan"))
        r.env.advance(60_000); r.text = "c\n"; r.touch(); r.env.advance(700)
        check("sesudah menyerah: tidak mencoba lagi", r.exec.handles.size == 3)
        r.live.setEnabled(false); r.live.setEnabled(true); r.touch(); r.env.advance(700)
        check("saklar dinyalakan ulang: mencoba lagi", r.exec.handles.size == 4)
    }
    run { // pylsp belum terpasang (gagal sinkron)
        val ex = FakeExec(syncExit = StreamResult(-1, false, 0, "pylsp belum ada di Debian."))
        val r = Rig(exec = ex); r.live.setEnabled(true); r.touch(); r.env.advance(700)
        check("gagal start sinkron: ERROR dengan pesan, satu proses", r.live.state == LiveState.ERROR && r.ev.states.last().second == "pylsp belum ada di Debian." && ex.handles.size == 1)
        check("gagal start: notice memuat pesan", r.ev.notices.size == 1 && r.ev.notices[0].contains("pylsp belum ada"))
    }
    run { // executor tidak ada
        val env = FakeEnv(); val ev = Ev()
        val live = LspLive(env, ev, { null }, { LspPathMap(ROOT, "molina-studio", false) })
        live.setEnabled(true); live.touch(1, FILE, LspServers.PYTHON) { "x" }; env.advance(700); live.touch(1, FILE, LspServers.PYTHON) { "y" }; env.advance(700)
        check("tanpa executor: satu notice, tidak melempar", ev.notices.size == 1)
    }
    run { // matikan saklar
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.live.setEnabled(false)
        check("saklar mati: shutdown terkirim", r.exec.handle.methods().last() == "shutdown")
        r.exec.answerShutdown()
        check("saklar mati: OFF, proses mati, tidak ada jadwal tersisa", r.live.state == LiveState.OFF && r.exec.handle.cancels >= 1 && r.env.pending() == 0)
        r.touch(); r.env.advance(2000)
        check("saklar mati: sentuhan diabaikan", r.exec.handles.size == 1)
    }
    run { // Simpan sebagai (path berubah) dan tutup tab
        val r = Rig(); r.live.setEnabled(true); r.ready()
        val newPath = "$ROOT/molina-studio/b.py"
        r.touch(1, newPath); r.env.advance(700)
        val m = r.exec.handle.methods()
        check("path berubah: didClose path lama lalu didOpen path baru", m.takeLast(2) == listOf("textDocument/didClose", "textDocument/didOpen"))
        r.live.release(1)
        check("release: didClose terkirim", r.exec.handle.methods().last() == "textDocument/didClose")
        r.exec.server(publish("file:///sdcard/molina-studio/b.py", diag("pyflakes")))
        check("diagnostik untuk tab yang sudah dilepas diabaikan", r.ev.diags.isEmpty())
    }
    run { // berkas di luar lingkup (akses penuh mati)
        val r = Rig(); r.live.setEnabled(true)
        r.touch(1, "$ROOT/DCIM/z.py"); r.env.advance(700); r.exec.initOk()
        check("berkas tak terlihat: tidak dikirim ke server", !r.exec.handle.methods().contains("textDocument/didOpen"))
    }
    run { // teks terlalu besar / null
        val r = Rig(LspLiveConfig(maxDocChars = 5)); r.live.setEnabled(true); r.ready()
        check("teks > batas: tidak dibuka", !r.exec.handle.methods().contains("textDocument/didOpen"))
        val r2 = Rig(); r2.live.setEnabled(true); r2.live.touch(1, FILE, LspServers.PYTHON) { null }; r2.env.advance(700); r2.exec.initOk()
        check("teks null: tidak dibuka", !r2.exec.handle.methods().contains("textDocument/didOpen"))
    }
    run { // shutdown total
        val r = Rig(); r.live.setEnabled(true); r.ready()
        r.live.shutdown()
        check("shutdown(): proses dimatikan paksa", r.exec.handle.cancels >= 1)
        r.exec.answerShutdown()
        check("shutdown(): OFF", r.live.state == LiveState.OFF)
    }
    println(if (fails == 0) "SEMUA LULUS" else "$fails GAGAL")
    if (fails > 0) System.exit(1)
}
