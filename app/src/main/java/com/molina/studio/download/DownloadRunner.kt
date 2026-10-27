package com.molina.studio.download

import android.content.Context
import com.molina.code.preview.RunPlan
import com.molina.code.run.RunHandle
import com.molina.code.run.RunListener
import com.molina.code.run.RunRequest
import com.molina.code.run.RunResult
import com.molina.core.common.AppLog
import com.molina.core.common.DownloadCommand
import com.molina.core.common.DownloadFeed
import com.molina.core.common.DownloadRequest
import com.molina.core.common.DownloadRunState
import com.molina.core.common.LogLevel
import com.molina.studio.code.ProotScriptExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Menjalankan `molina_dl.py --json` di Debian PRoot dan mengubah keluarannya menjadi [state] untuk UI.
 * - Skrip yang dipakai selalu salinan bawaan di assets (protokol v1 dijamin cocok), bukan salinan di Scripts/
 *   yang mungkin sudah diedit pengguna.
 * - Satu sesi pada satu waktu. Berjalan di jalur DOWNLOAD executor: tidak memblokir editor Code dan tidak ikut
 *   dibatalkan Penyegaran sistem.
 * - Keluaran tidak dibatasi (progres menghasilkan banyak baris) tetapi tidak disimpan: hanya state hasil lipatan
 *   [DownloadFeed]. URL tidak pernah masuk log; log hanya memuat jumlah dan hasil.
 */
internal object DownloadRunner {
    private val _state = MutableStateFlow(DownloadRunState())
    val state: StateFlow<DownloadRunState> = _state.asStateFlow()

    private val lock = Any()
    private var feed: DownloadFeed? = null
    private var handle: RunHandle? = null

    fun isRunning(): Boolean = _state.value.running

    /** @return pesan galat siap tampil, atau null bila sesi unduhan dimulai. */
    fun start(ctx: Context, req: DownloadRequest): String? {
        DownloadCommand.validate(req)?.let { return it }
        val app = ctx.applicationContext
        val script = try {
            app.assets.open(DownloadCommand.SCRIPT_ASSET).use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            return "Skrip unduhan bawaan tidak ditemukan di aplikasi."
        }
        val f = DownloadFeed()
        synchronized(lock) {
            if (_state.value.running) return "Masih ada unduhan yang berjalan."
            feed = f
            handle = null
            _state.value = f.state
        }
        val t0 = System.nanoTime()
        AppLog.log("sistem", LogLevel.Info, "Unduhan", "mulai: ${DownloadCommand.clean(req.urls).size} URL, mode ${req.mode.arg}")
        val rr = RunRequest(
            name = "molina_dl",
            scriptText = script,
            plan = RunPlan("python3", "/usr/bin/python3", "py", true),
            hostDir = null,
            timeoutMs = DownloadCommand.TIMEOUT_MS,
            args = DownloadCommand.args(req),
            maxOutputChars = Long.MAX_VALUE,
        )
        val h = ProotScriptExecutor(app, ProotScriptExecutor.Lane.DOWNLOAD).start(rr, object : RunListener {
            override fun onOutput(text: String, isError: Boolean) {
                synchronized(lock) {
                    if (feed === f && f.push(text, isError)) _state.value = f.state
                }
            }

            override fun onFinished(result: RunResult) {
                val done = synchronized(lock) {
                    if (feed !== f) return
                    f.finish(result.cancelled, result.exitCode, result.startError)
                    _state.value = f.state
                    handle = null
                    f.state
                }
                val s = done.session
                val secs = (System.nanoTime() - t0) / 1_000_000_000L
                val level = if (result.startError != null || s.fatalError != null) LogLevel.Warn else LogLevel.Info
                AppLog.log(
                    "sistem", level, "Unduhan",
                    "selesai: ${s.ok} berhasil, ${s.failed} gagal" + (if (s.cancelled) " (dibatalkan)" else "") +
                        " dalam ${secs}dtk, kode ${result.exitCode}" + (if (result.timedOut) " (lewat waktu)" else ""),
                )
            }
        })
        // onFinished bisa datang sebelum start() kembali (mis. jalur sibuk): jangan simpan handle usang.
        synchronized(lock) { if (feed === f && _state.value.running) handle = h }
        if (_state.value.running) DownloadService.ensureRunning(app)
        return null
    }

    /** Batalkan sesi yang berjalan. Berkas .part tetap ada sehingga unduhan bisa dilanjutkan dengan URL yang sama. */
    fun cancel() {
        val h = synchronized(lock) { handle }
        h?.cancel()
    }

    /** Kosongkan hasil sesi yang sudah selesai (tidak berpengaruh saat masih berjalan). */
    fun clear() {
        synchronized(lock) {
            if (!_state.value.running) {
                feed = null
                _state.value = DownloadRunState()
            }
        }
    }
}
