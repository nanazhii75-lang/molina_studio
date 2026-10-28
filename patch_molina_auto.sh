python3 << 'PYEOF'
import os, sys
ROOT = os.environ.get("MOLINA_ROOT", "/storage/emulated/0/molina-studio")
SRC = ROOT + "/app/src/main/java/com/molina/studio"
fails = []

def rd(p):
    return open(p, encoding="utf-8").read()

def write_new(rel, text):
    p = SRC + "/" + rel
    os.makedirs(os.path.dirname(p), exist_ok=True)
    if os.path.isfile(p) and rd(p) == text:
        print("SKIP  ", rel); return
    open(p, "w", encoding="utf-8").write(text)
    print("TULIS ", rel)

def sub(p, old, new, marker):
    s = rd(p)
    if marker in s:
        print("SKIP  ", os.path.basename(p), "[" + marker[:30] + "]"); return
    if s.count(old) != 1:
        print("GAGAL ", os.path.basename(p), "pola tidak unik/tidak ada:", old[:50].replace("\n", " "))
        fails.append(p); return
    open(p, "w", encoding="utf-8").write(s.replace(old, new))
    print("UBAH  ", os.path.basename(p), "[" + marker[:30] + "]")

# ---------------- berkas baru ----------------
write_new("automation/AutoPrefs.kt", r'''package com.molina.studio.automation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Status otomasi Molina (pembaruan otomatis dan cadangan harian). Dibaca UI lewat [AutoPrefs.state]. */
data class AutoState(
    val autoUpdate: Boolean = true,
    val autoBackup: Boolean = false,
    val lastCheck: Long = 0L,
    val lastBackup: Long = 0L,
    val ytdlpInstalled: String = "",
    val ytdlpLatest: String = ""
) {
    /** True bila rilis resmi yt-dlp lebih baru dari yang terpasang (keduanya diketahui). */
    val ytdlpOutdated: Boolean
        get() = ytdlpInstalled.isNotEmpty() && ytdlpLatest.isNotEmpty() && versionNewer(ytdlpLatest, ytdlpInstalled)
}

/** Bandingkan versi bertitik (mis. 2025.10.22): true bila [latest] lebih baru dari [installed]. */
internal fun versionNewer(latest: String, installed: String): Boolean {
    val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
    val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** SharedPreferences molina_auto. Hanya sakelar yang ikut Cadangan gabungan (lihat BackupManager). */
object AutoPrefs {
    private const val FILE = "molina_auto"
    private var sp: SharedPreferences? = null
    private val _state = MutableStateFlow(AutoState())
    val state: StateFlow<AutoState> = _state.asStateFlow()

    @Synchronized
    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp = p
        _state.value = AutoState(
            autoUpdate = p.getBoolean("auto_update", true),
            autoBackup = p.getBoolean("auto_backup", false),
            lastCheck = p.getLong("last_check", 0L),
            lastBackup = p.getLong("last_backup", 0L),
            ytdlpInstalled = p.getString("ytdlp_installed", "").orEmpty(),
            ytdlpLatest = p.getString("ytdlp_latest", "").orEmpty()
        )
    }

    @Synchronized
    private fun change(ctx: Context, write: (SharedPreferences.Editor) -> Unit, next: (AutoState) -> AutoState) {
        init(ctx)
        val ed = sp!!.edit()
        write(ed)
        ed.apply()
        _state.value = next(_state.value)
    }

    fun setAutoUpdate(ctx: Context, v: Boolean) =
        change(ctx, { it.putBoolean("auto_update", v) }, { it.copy(autoUpdate = v) })

    fun setAutoBackup(ctx: Context, v: Boolean) =
        change(ctx, { it.putBoolean("auto_backup", v) }, { it.copy(autoBackup = v) })

    fun markChecked(ctx: Context) {
        val now = System.currentTimeMillis()
        change(ctx, { it.putLong("last_check", now) }, { it.copy(lastCheck = now) })
    }

    fun markBackup(ctx: Context) {
        val now = System.currentTimeMillis()
        change(ctx, { it.putLong("last_backup", now) }, { it.copy(lastBackup = now) })
    }

    /** Versi terpasang (dari --version) dan rilis terbaru (dari GitHub); kosong = tidak diketahui. */
    fun recordYtdlp(ctx: Context, installed: String, latest: String) =
        change(
            ctx,
            { it.putString("ytdlp_installed", installed).putString("ytdlp_latest", latest) },
            { it.copy(ytdlpInstalled = installed, ytdlpLatest = latest) }
        )

    /** Dipanggil setelah pasang/perbarui sukses: versi terpasang = [v], lencana hilang bila sama dengan rilis terbaru. */
    fun setYtdlpInstalled(ctx: Context, v: String) =
        change(ctx, { it.putString("ytdlp_installed", v) }, { it.copy(ytdlpInstalled = v) })

    /** Dipanggil saat yt-dlp dihapus atau tidak ada: lencana dan status dibersihkan. */
    fun clearYtdlp(ctx: Context) = recordYtdlp(ctx, "", "")
}
''')

write_new("automation/AutoWork.kt", r'''package com.molina.studio.automation

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Menjadwalkan (atau membatalkan) pekerjaan latar sesuai [AutoPrefs]. Aman dipanggil berulang. */
object AutoWork {
    private const val UPDATE = "molina-auto-update"
    private const val BACKUP = "molina-auto-backup"

    fun sync(ctx: Context) {
        val app = ctx.applicationContext
        AutoPrefs.init(app)
        val st = AutoPrefs.state.value
        val wm = WorkManager.getInstance(app)

        if (st.autoUpdate) {
            // Maks. sekali per 24 jam, hanya Wi-Fi (jaringan tidak berbayar), tidak saat baterai lemah.
            val c = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
            val req = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(c)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(UPDATE, ExistingPeriodicWorkPolicy.KEEP, req)
        } else {
            wm.cancelUniqueWork(UPDATE)
        }

        if (st.autoBackup) {
            val c = Constraints.Builder().setRequiresBatteryNotLow(true).build()
            val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(c)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(BACKUP, ExistingPeriodicWorkPolicy.KEEP, req)
        } else {
            wm.cancelUniqueWork(BACKUP)
        }
    }
}
''')

write_new("automation/UpdateChecker.kt", r'''package com.molina.studio.automation

import android.content.Context
import com.molina.code.preview.RunPlan
import com.molina.code.run.RunListener
import com.molina.code.run.RunRequest
import com.molina.code.run.RunResult
import com.molina.core.common.AppLog
import com.molina.core.common.LogLevel
import com.molina.studio.code.ProotScriptExecutor
import com.molina.studio.settings.AptHistory
import com.molina.studio.settings.AptJob
import com.molina.studio.stream.YtdlpLocator
import com.molina.studio.stream.YtdlpRunner
import com.molina.studio.stream.YtdlpStatus
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Pemeriksaan pembaruan tanpa dialog: jumlah paket apt yang bisa ditingkatkan (mengisi lencana ubin Sistem)
 * dan versi yt-dlp terbaru (mengisi lencana ubin Paket). Tidak memasang apa pun dan tidak menampilkan notifikasi.
 * Dipakai [UpdateCheckWorker] (otomatis) dan tombol "Periksa sekarang".
 */
object UpdateChecker {
    private const val TAG = "auto"
    private val TAG_RE = Regex("""\d{4}\.\d{2}\.\d{2}(\.\d+)?""")
    private val COUNT_RE = Regex("""^\d+$""")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Mulai pemeriksaan di latar (tidak terikat layar); abaikan bila sedang berjalan. */
    fun checkNow(ctx: Context) {
        if (_running.value) return
        val app = ctx.applicationContext
        scope.launch {
            try { checkAll(app) } catch (e: CancellationException) { throw e } catch (e: Exception) {
                AppLog.log("system", LogLevel.Warn, TAG, "periksa gagal: ${e.javaClass.simpleName}")
            }
        }
    }

    suspend fun checkAll(ctx: Context) {
        mutex.withLock {
            _running.value = true
            try { doCheck(ctx.applicationContext) } finally { _running.value = false }
        }
    }

    private suspend fun doCheck(ctx: Context) {
        AutoPrefs.init(ctx)
        AptHistory.init(ctx)
        val status = YtdlpLocator.status(ctx)
        if (status == YtdlpStatus.NoRootfs) {
            AppLog.log("system", LogLevel.Info, TAG, "dilewati: Debian belum dipasang")
            return
        }

        // 1) yt-dlp: versi terpasang (di-cache; --version memulai proot) vs rilis resmi terbaru.
        if (status is YtdlpStatus.Ready) {
            var installed = AutoPrefs.state.value.ytdlpInstalled
            if (installed.isEmpty()) {
                val o = YtdlpRunner.run(ctx, status.guestBinary, listOf("--version"), 60_000L)
                val first = o.stdout.trim().lineSequence().firstOrNull().orEmpty()
                if (o.exit == 0) installed = first
            }
            val latest = fetchLatestYtdlp()
            AutoPrefs.recordYtdlp(ctx, installed, latest)
            AppLog.log("system", LogLevel.Info, TAG, "yt-dlp terpasang=${installed.ifEmpty { "?" }} terbaru=${latest.ifEmpty { "?" }}")
        } else {
            AutoPrefs.clearYtdlp(ctx)
        }

        // 2) apt: perintah sama dengan "Periksa pembaruan paket"; jalur AUTO agar tidak memblokir editor/unduhan.
        val job = AptJob.CheckUpdates
        val (code, out) = runScript(ctx, job.command, job.timeoutMs)
        if (code == 0) {
            out.lineSequence().map { it.trim() }.lastOrNull { COUNT_RE.matches(it) }?.toIntOrNull()?.let {
                AptHistory.setPending(ctx, it)
                AppLog.log("system", LogLevel.Info, TAG, "apt: $it paket dapat ditingkatkan")
            }
        } else {
            AppLog.log("system", LogLevel.Info, TAG, "apt dilewati/gagal (kode $code), lencana tidak diubah")
        }
        AutoPrefs.markChecked(ctx)
    }

    /** Tag rilis terbaru yt-dlp dari GitHub (HTTPS); kosong bila gagal atau formatnya tak dikenal. */
    private fun fetchLatestYtdlp(): String {
        return try {
            val conn = URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest").openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "MolinaStudio")
            try {
                if (conn.responseCode != 200) "" else {
                    val body = conn.inputStream.use { it.readBytes() }
                    if (body.size > 1_000_000) "" else {
                        val tag = JSONObject(String(body, Charsets.UTF_8)).optString("tag_name").trim()
                        if (TAG_RE.matches(tag)) tag else ""
                    }
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            ""
        }
    }

    /** Jalankan skrip di Debian PRoot (jalur AUTO) dan tunggu; kode -1 bila gagal mulai/dibatalkan/lewat waktu. */
    private suspend fun runScript(ctx: Context, script: String, timeoutMs: Long): Pair<Int, String> =
        suspendCancellableCoroutine { cont ->
            val sb = StringBuilder()
            val req = RunRequest(
                name = "auto",
                scriptText = "set -e\n" + script.trim() + "\n",
                plan = RunPlan("sh", "/bin/sh", "sh", false),
                hostDir = null,
                timeoutMs = timeoutMs,
                maxOutputChars = 200_000L
            )
            val handle = ProotScriptExecutor(ctx, ProotScriptExecutor.Lane.AUTO).start(req, object : RunListener {
                override fun onOutput(text: String, isError: Boolean) {
                    synchronized(sb) { if (sb.length < 200_000) sb.append(text) }
                }

                override fun onFinished(result: RunResult) {
                    val code = if (result.startError != null || result.cancelled || result.timedOut) -1 else result.exitCode
                    val text = synchronized(sb) { sb.toString() }
                    if (cont.isActive) cont.resume(code to text)
                }
            })
            cont.invokeOnCancellation { handle.cancel() }
        }
}
''')

write_new("automation/AutoWorkers.kt", r'''package com.molina.studio.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.molina.core.common.AppLog
import com.molina.core.common.LogLevel
import com.molina.core.common.MolinaDir
import com.molina.studio.AppPaths
import com.molina.studio.settings.BackupManager
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DAY_MS = 23 * 3_600_000L // sedikit di bawah 24 jam agar jadwal periodik tidak terlewat

/** Periksa pembaruan apt + yt-dlp (Wi-Fi saja, maks. 1x/24 jam). Hasil hanya mengubah lencana. */
class UpdateCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext
        AutoPrefs.init(app)
        val st = AutoPrefs.state.value
        if (!st.autoUpdate) return Result.success()
        if (System.currentTimeMillis() - st.lastCheck < DAY_MS) return Result.success()
        try {
            UpdateChecker.checkAll(app)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.log("system", LogLevel.Warn, "auto", "periksa otomatis gagal: ${e.javaClass.simpleName}")
        }
        return Result.success()
    }
}

/**
 * Cadangan gabungan harian (BackupManager). Pengaman: salinan sebelumnya disimpan sebagai molina-backup.prev.json,
 * dan bila hasil baru jauh lebih kecil (mis. data aplikasi baru dihapus) berkas lama dipulihkan, bukan ditimpa.
 */
class AutoBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext
        AutoPrefs.init(app)
        val st = AutoPrefs.state.value
        if (!st.autoBackup) return Result.success()
        if (System.currentTimeMillis() - st.lastBackup < DAY_MS) return Result.success()
        withContext(Dispatchers.IO) {
            try {
                val dir = AppPaths.get(app).dir(MolinaDir.Backup)
                val f = File(dir, BackupManager.FILE_NAME)
                val prev = File(dir, "molina-backup.prev.json")
                val oldSize = if (f.isFile) f.length() else 0L
                if (oldSize > 0L) f.copyTo(prev, overwrite = true)
                BackupManager.backupAll(app)
                if (oldSize > 2048L && f.length() < oldSize / 2) {
                    prev.copyTo(f, overwrite = true)
                    AppLog.log("system", LogLevel.Warn, "auto", "cadangan baru jauh lebih kecil; berkas lama dipertahankan")
                } else {
                    AutoPrefs.markBackup(app)
                    AppLog.log("system", LogLevel.Info, "auto", "cadangan harian selesai")
                }
            } catch (e: Exception) {
                AppLog.log("system", LogLevel.Warn, "auto", "cadangan harian gagal: ${e.javaClass.simpleName}")
            }
        }
        return Result.success()
    }
}
''')

write_new("settings/AutomationUi.kt", r'''package com.molina.studio.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.molina.studio.automation.AutoPrefs
import com.molina.studio.automation.AutoState
import com.molina.studio.automation.AutoWork
import com.molina.studio.automation.UpdateChecker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Menumpuk lencana di tepi kanan sebuah baris (mis. W7Link) tanpa mengubah tanda tangan W7Link. */
@Composable
internal fun BadgeOverlay(badge: String?, aero: Boolean, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        content()
        if (badge != null) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) { W7Badge(badge, aero) }
        }
    }
}

private fun whenText(at: Long, never: String): String =
    if (at <= 0L) never else SimpleDateFormat("d MMM HH.mm", Locale("id", "ID")).format(Date(at))

private fun ytdlpText(s: AutoState): String = when {
    s.ytdlpInstalled.isEmpty() && s.ytdlpLatest.isEmpty() -> "Belum diperiksa, atau yt-dlp belum terpasang"
    s.ytdlpOutdated -> "Pembaruan tersedia: ${s.ytdlpInstalled} \u2192 ${s.ytdlpLatest}"
    s.ytdlpInstalled.isNotEmpty() -> "Terbaru (${s.ytdlpInstalled})"
    else -> "Rilis terbaru: ${s.ytdlpLatest}"
}

/** Settings > Sistem > Otomasi dan pembaruan: pemeriksaan pembaruan otomatis dan cadangan harian. */
@Composable
internal fun AutomationPage() {
    val ctx = LocalContext.current
    remember { AutoPrefs.init(ctx); AptHistory.init(ctx) }
    val st by AutoPrefs.state.collectAsState()
    val pending by AptHistory.pending.collectAsState()
    val running by UpdateChecker.running.collectAsState()

    Category("Pembaruan otomatis")
    SwitchRow(
        "Periksa pembaruan otomatis",
        "Maksimal sekali per 24 jam dan hanya saat Wi-Fi. Menyegarkan lencana paket Debian (ubin Sistem) dan " +
            "yt-dlp (ubin Paket) tanpa membuka dialog. Tanpa notifikasi dan tidak memasang apa pun.",
        st.autoUpdate
    ) {
        AutoPrefs.setAutoUpdate(ctx, it)
        AutoWork.sync(ctx)
    }
    SettingsItem(
        if (running) "Memeriksa\u2026" else "Periksa sekarang",
        "Terakhir: " + whenText(st.lastCheck, "belum pernah") + ". Berjalan di latar dan bisa memakai data seluler."
    ) { if (!running) UpdateChecker.checkNow(ctx) }
    SettingsItem("Paket Debian", AptHistory.checkSubtitle(pending)) {}
    SettingsItem("yt-dlp", ytdlpText(st)) {}

    Category("Cadangan otomatis")
    SwitchRow(
        "Cadangan gabungan harian",
        "Menyimpan penanda video, snippet Terminal, dan setelan ke folder Backup sekali per hari. " +
            "Salinan sebelumnya dijaga sebagai molina-backup.prev.json.",
        st.autoBackup
    ) {
        AutoPrefs.setAutoBackup(ctx, it)
        AutoWork.sync(ctx)
    }
    SettingsItem("Cadangan otomatis terakhir", whenText(st.lastBackup, "belum pernah")) {}
}
''')

# ---------------- ubah berkas yang ada ----------------
gr = ROOT + "/app/build.gradle.kts"
sub(gr, '    implementation("androidx.datastore:datastore-preferences:1.1.1")\n',
    '    implementation("androidx.datastore:datastore-preferences:1.1.1")\n    implementation("androidx.work:work-runtime-ktx:2.9.1")\n',
    "work-runtime-ktx")

fr = SRC + "/settings/Win7FrameUi.kt"
sub(fr, "internal fun W7Badge(text: String) {", "internal fun W7Badge(text: String, aero: Boolean = false) {", "aero: Boolean")
sub(fr, ".background(Brush.verticalGradient(listOf(Color(0xFFFF8A80), Color(0xFFC4392B))), shape)",
    ".background(\n                Brush.verticalGradient(\n                    if (aero) listOf(Color(0xFF7FC4FF), Color(0xFF1B6FC2)) else listOf(Color(0xFFFF8A80), Color(0xFFC4392B))\n                ),\n                shape\n            )",
    "if (aero) listOf")

ws = SRC + "/settings/Win7SettingsUi.kt"
sub(ws, "    badge: String? = null\n) {", "    badge: String? = null,\n    badgeAero: Boolean = false\n) {", "badgeAero: Boolean")
sub(ws, "if (badge != null) W7Badge(badge)", "if (badge != null) W7Badge(badge, badgeAero)", "W7Badge(badge, badgeAero)")

ss = SRC + "/settings/SettingsScreen.kt"
sub(ss, "Files, Downloads, Storage, Monitor\n}", "Files, Downloads, Storage, Monitor, Automation\n}", "Monitor, Automation")
sub(ss, '    Page.Monitor -> "Monitor Sistem"\n}', '    Page.Monitor -> "Monitor Sistem"\n    Page.Automation -> "Otomasi dan pembaruan"\n}', "Page.Automation ->\u0020\"Otomasi")
sub(ss, '    Hit("About", "Versi dan lisensi", Page.About)',
    '    Hit("Otomasi dan pembaruan", "Pemeriksaan pembaruan otomatis (Wi-Fi, 24 jam) dan cadangan harian", Page.Automation),\n'
    '    Hit("Pembaruan otomatis", "Perbarui lencana paket Debian dan yt-dlp lewat WorkManager", Page.Automation),\n'
    '    Hit("Cadangan otomatis", "Cadangan gabungan harian ke folder Backup", Page.Automation),\n'
    '    Hit("About", "Versi dan lisensi", Page.About)', 'Hit("Otomasi dan pembaruan"')
sub(ss, "    val aptPending by AptHistory.pending.collectAsState()\n",
    "    val aptPending by AptHistory.pending.collectAsState()\n    remember { com.molina.studio.automation.AutoPrefs.init(aptCtx) }\n    val autoSt by com.molina.studio.automation.AutoPrefs.state.collectAsState()\n",
    "val autoSt by")
sub(ss, "selected = lastTile == Page.Packages,\n",
    "selected = lastTile == Page.Packages,\n                            badge = if (autoSt.ytdlpOutdated) \"!\" else null,\n                            badgeAero = true,\n",
    "badgeAero = true")
sub(ss, '                        Category("Debian")\n',
    '                        W7Link({ W7IconAptSync() }, "Otomasi dan pembaruan", "Pembaruan otomatis (Wi-Fi), cadangan harian, lencana yt-dlp") { open(Page.Automation) }\n'
    '                        Category("Debian")\n', '"Otomasi dan pembaruan", "Pembaruan otomatis (Wi-Fi)')
sub(ss, "                    Page.Monitor -> SystemMonitorPage()\n",
    "                    Page.Monitor -> SystemMonitorPage()\n                    Page.Automation -> AutomationPage()\n", "Page.Automation -> AutomationPage()")

tu = SRC + "/settings/ToolsUi.kt"
sub(tu, '    W7Link({ W7IconYtdlp() }, "Mesin cadangan (yt-dlp)", "TikTok, Facebook, dll. lewat Debian (default mati)", onOpenYtdlp)\n',
    '    remember { com.molina.studio.automation.AutoPrefs.init(ctx) }\n'
    '    val autoSt by com.molina.studio.automation.AutoPrefs.state.collectAsState()\n'
    '    BadgeOverlay(if (autoSt.ytdlpOutdated) "Baru" else null, true) {\n'
    '        W7Link(\n'
    '            { W7IconYtdlp() }, "Mesin cadangan (yt-dlp)",\n'
    '            if (autoSt.ytdlpOutdated) "Pembaruan tersedia: ${autoSt.ytdlpInstalled} \\u2192 ${autoSt.ytdlpLatest}"\n'
    '            else "TikTok, Facebook, dll. lewat Debian (default mati)",\n'
    '            onOpenYtdlp\n'
    '        )\n'
    '    }\n', "BadgeOverlay(")

yi = SRC + "/stream/YtdlpInstaller.kt"
sub(yi, "                _state.value = doInstall(app)\n",
    "                val res = doInstall(app)\n                _state.value = res\n"
    "                if (res is YtdlpInstallState.Done) res.version?.let { com.molina.studio.automation.AutoPrefs.setYtdlpInstalled(app, it) }\n",
    "AutoPrefs.setYtdlpInstalled")

sr = SRC + "/settings/SystemRefreshUi.kt"
sub(sr, '            if (code != -1 && r.title.endsWith("Hapus")) tick++\n',
    '            if (code != -1 && r.title.endsWith("Hapus")) {\n                tick++\n                com.molina.studio.automation.AutoPrefs.clearYtdlp(ctx)\n            }\n',
    "AutoPrefs.clearYtdlp")

bm = SRC + "/settings/BackupManager.kt"
sub(bm, '        Spec("molina_stream"),\n',
    '        Spec("molina_stream"),\n        Spec("molina_auto", only = setOf("auto_update", "auto_backup")),\n', 'Spec("molina_auto"')

pe = SRC + "/code/ProotScriptExecutor.kt"
sub(pe, "enum class Lane { CODE, DOWNLOAD }", "enum class Lane { CODE, DOWNLOAD, AUTO }", "DOWNLOAD, AUTO")

ma = SRC + "/MolinaApp.kt"
sub(ma, "        SystemRefresh.install(this)\n",
    "        SystemRefresh.install(this)\n        com.molina.studio.automation.AutoPrefs.init(this)\n        com.molina.studio.automation.AutoWork.sync(this)\n",
    "AutoWork.sync")

pg = ROOT + "/progres.md"
entry = '''
## 2026-10-28 - Otomasi: periksa pembaruan 24 jam/Wi-Fi, lencana yt-dlp, cadangan harian [AUTO-UPDATE-BACKUP]
- [x] fix(settings): hapus import matchParentSize di Win7FrameUi.kt (member BoxScope)
- [x] feat(automation): `AutoPrefs`, `AutoWork`, `UpdateCheckWorker` (WorkManager periodik 24 jam, hanya Wi-Fi/UNMETERED, baterai tidak lemah, tunda awal 15 menit). Tanpa notifikasi: hasil hanya mengubah lencana
- [x] feat(automation): `UpdateChecker` (apt: perintah CheckUpdates di jalur Lane.AUTO; yt-dlp: --version terpasang vs tag rilis GitHub), `ProotScriptExecutor.Lane.AUTO` agar tidak memblokir editor/unduhan
- [x] feat(settings): lencana biru Aero (`W7Badge(aero)`) di ubin Paket dan baris yt-dlp bila ada rilis lebih baru; hilang setelah `YtdlpInstaller` sukses (`AutoPrefs.setYtdlpInstalled`) atau yt-dlp dihapus
- [x] feat(settings): halaman Settings > Sistem > Otomasi dan pembaruan (`AutomationPage`): sakelar pembaruan otomatis, Periksa sekarang, status apt/yt-dlp, sakelar cadangan harian
- [x] feat(backup): `AutoBackupWorker` (harian, simpan molina-backup.prev.json, pulihkan berkas lama bila hasil baru < 50% ukuran). Sakelar otomasi ikut `BackupManager` (molina_auto)
- [x] Dampak: Gradle (androidx.work:work-runtime-ktx:2.9.1), AndroidManifest hasil merge menambah RECEIVE_BOOT_COMPLETED dari WorkManager. JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (`BadgeOverlay` memakai `Modifier.align` di dalam Box, `suspendCancellableCoroutine` + RunListener, WorkManager 2.9.1 vs compileSdk 35)
- [ ] uji perangkat (Android 8): Sistem > Otomasi dan pembaruan > Periksa sekarang > status apt/yt-dlp terisi, lencana merah di Sistem bila ada paket, lencana biru di Paket bila yt-dlp usang; Perbarui yt-dlp > lencana hilang; matikan sakelar > pekerjaan dibatalkan; nyalakan cadangan harian > cek molina-backup.json dan .prev.json
- [ ] LANGKAH BERIKUT: pilih bagian yang dipulihkan, ukuran unduhan di fase Mengunduh paket, ubin Settings dengan ikon status
'''
if os.path.isfile(pg):
    s = rd(pg)
    if "[AUTO-UPDATE-BACKUP]" in s:
        print("SKIP   progres.md")
    else:
        open(pg, "a", encoding="utf-8").write(("" if s.endswith("\n") else "\n") + entry)
        print("UBAH   progres.md")
else:
    print("GAGAL  progres.md tidak ada"); fails.append(pg)

print("\nSELESAI dengan", len(fails), "kegagalan")
sys.exit(1 if fails else 0)
PYEOF
