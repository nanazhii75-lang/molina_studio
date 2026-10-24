package com.molina.studio.cinema

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.cinema.BackgroundPlaybackState
import com.molina.core.common.StreamError
import com.molina.core.common.StreamOutcome
import com.molina.core.common.StreamResolvers
import com.molina.core.common.StreamSearchItem
import com.molina.studio.ui.MolinaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HINT = "Ketuk ikon tautan di bawah untuk memutar atau mencari video di YouTube."
private const val FAIL = "Pemutar gagal dibuka."

private fun searchError(e: StreamError): String = when (e) {
    StreamError.NETWORK -> "Pencarian gagal: periksa koneksi internet."
    StreamError.PARSE -> "Pencarian gagal: format YouTube berubah, extractor perlu diperbarui."
    else -> "Pencarian gagal."
}

private fun duration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun CinemaHome(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<StreamSearchItem>>(emptyList()) }
    var showResults by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !CinemaActions.play(ctx, uri)) info = FAIL
    }

    fun search(q: String) {
        if (searching) return
        searching = true
        info = "Mencari\u2026"
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { StreamResolvers.get()?.search(q, 15) }
            searching = false
            when (outcome) {
                null -> info = "Mesin pencarian belum siap."
                is StreamOutcome.Failure -> info = searchError(outcome.error)
                is StreamOutcome.Success -> if (outcome.value.isEmpty()) {
                    info = "Tidak ada hasil untuk \"$q\"."
                } else {
                    info = ""
                    results = outcome.value
                    showResults = true
                }
            }
        }
    }

    Column(
        modifier.background(MolinaTheme.Background).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val live by BackgroundPlaybackState.active.collectAsState()
        if (live) {
            LiveBadge()
            Spacer(Modifier.height(8.dp))
        }
        Text("Pencarian YouTube", color = MolinaTheme.Muted, fontSize = 14.sp)
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CinemaLogo(info.ifEmpty { HINT }) }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            CinemaAction("\uD83D\uDCC4", "File") { pick.launch(arrayOf("video/*", "audio/*")) }
            CinemaAction("\uD83D\uDD17", "Tautan") { dialog = true }
            CinemaAction("\u2699", "Setelan") { if (!CinemaActions.openSettings(ctx)) info = "Setelan gagal dibuka." }
            CinemaAction("\u21BB", "Reset") { info = ""; input = "" }
        }
    }
    if (dialog) AlertDialog(
        onDismissRequest = { dialog = false },
        title = { Text("Tautan atau judul") },
        text = {
            OutlinedTextField(input, { input = it }, singleLine = true,
                placeholder = { Text("Tempel URL atau ketik judul video") })
        },
        confirmButton = {
            TextButton({
                dialog = false
                when (val r = CinemaActions.classify(input)) {
                    is CinemaActions.Input.Direct -> info = if (CinemaActions.play(ctx, r.uri)) "" else FAIL
                    CinemaActions.Input.Query -> search(input.trim())
                    CinemaActions.Input.Invalid -> info = "Tautan tidak didukung. Pakai http, https, rtsp, atau rtmp."
                    CinemaActions.Input.Empty -> Unit
                }
            }) { Text("Buka") }
        },
        dismissButton = { TextButton({ dialog = false }) { Text("Batal") } }
    )
    if (showResults) AlertDialog(
        onDismissRequest = { showResults = false },
        title = { Text("Hasil pencarian") },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(results) { r ->
                    Column(
                        Modifier.fillMaxWidth().clickable {
                            showResults = false
                            val d = CinemaActions.classify(r.pageUrl)
                            info = if (d is CinemaActions.Input.Direct && CinemaActions.playResults(ctx, results, r)) "" else FAIL
                        }.padding(vertical = 10.dp)
                    ) {
                        Text(r.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                        val meta = listOfNotNull(r.uploader, r.durationSec?.let { duration(it) }).joinToString(" \u00B7 ")
                        if (meta.isNotEmpty()) Text(meta, color = MolinaTheme.Muted, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = MolinaTheme.CardLine)
                }
            }
        },
        confirmButton = { TextButton({ showResults = false }) { Text("Tutup") } }
    )
}
