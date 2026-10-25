package com.molina.studio.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.code.preview.RunPlan
import com.molina.code.run.RunHandle
import com.molina.code.run.RunListener
import com.molina.code.run.RunRequest
import com.molina.code.run.RunResult
import com.molina.core.common.AppLog
import com.molina.core.common.LogLevel
import com.molina.studio.code.ProotScriptExecutor

/** Pintasan perawatan Debian di Settings > Sistem. Dijalankan di Debian PRoot yang sama dengan Terminal. */
internal enum class AptJob(val title: String, val command: String, val timeoutMs: Long, val note: String) {
    Update(
        "apt update", "apt update -o APT::Sandbox::User=root", 10 * 60_000L,
        "Memperbarui daftar paket Debian. Butuh internet."
    ),
    Upgrade(
        "apt upgrade -y",
        "apt upgrade -y -o APT::Sandbox::User=root -o Dpkg::Options::=--force-confold -o Dpkg::Options::=--force-confdef",
        40 * 60_000L,
        "Meningkatkan semua paket Debian tanpa bertanya. Bisa mengunduh banyak data dan memakan beberapa menit. " +
            "Jalankan apt update lebih dulu."
    )
}

private const val MAX_UI_CHARS = 60_000

@Composable
internal fun DebianAptDialog(job: AptJob, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var started by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf<RunHandle?>(null) }
    val scroll = rememberScrollState()
    LaunchedEffect(output) { scroll.scrollTo(scroll.maxValue) }

    if (!started) {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Debian: ${job.title}") },
            text = { Text(job.note) },
            confirmButton = {
                TextButton(onClick = {
                    started = true
                    running = true
                    status = "Berjalan\u2026"
                    handle = startApt(
                        ctx, job,
                        onChunk = { output = (output + it.replace("\r", "")).takeLast(MAX_UI_CHARS) },
                        onDone = { r ->
                            running = false
                            status = describe(r)
                            AppLog.log("system", LogLevel.Info, "apt", "${job.title} selesai exit=${r.exitCode} ${r.millis}ms")
                        }
                    )
                }) { Text("Jalankan") }
            },
            dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = { if (!running) onClose() },
            title = { Text("Debian: ${job.title}") },
            text = {
                Column {
                    Text(
                        status, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.heightIn(max = 320.dp).verticalScroll(scroll)) {
                        Text(output, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                if (running) TextButton(onClick = { handle?.cancel() }) { Text("Berhenti") }
                else TextButton(onClick = onClose) { Text("Tutup") }
            },
            dismissButton = { TextButton(onClick = { copyOutput(ctx, output) }) { Text("Salin") } }
        )
    }
}

private fun startApt(ctx: Context, job: AptJob, onChunk: (String) -> Unit, onDone: (RunResult) -> Unit): RunHandle {
    val main = Handler(Looper.getMainLooper())
    val req = RunRequest(
        name = "apt",
        scriptText = "echo '\$ ${job.title}'\n${job.command}\n",
        plan = RunPlan("sh", "/bin/sh", "sh", false),
        hostDir = null,
        timeoutMs = job.timeoutMs
    )
    return ProotScriptExecutor(ctx.applicationContext).start(req, object : RunListener {
        override fun onOutput(text: String, isError: Boolean) { main.post { onChunk(text) } }
        override fun onFinished(result: RunResult) { main.post { onDone(result) } }
    })
}

private fun describe(r: RunResult): String {
    val err = r.startError
    return when {
        err != null -> err
        r.cancelled -> "Dihentikan. Bila apt terputus di tengah jalan, jalankan dpkg --configure -a di Terminal."
        r.timedOut -> "Lewat batas waktu."
        r.exitCode == 0 -> "Selesai dalam ${r.millis / 1000} dtk."
        else -> "Gagal (kode ${r.exitCode})."
    }
}

private fun copyOutput(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("molina-apt", text))
    Toast.makeText(ctx, "Keluaran disalin", Toast.LENGTH_SHORT).show()
}
