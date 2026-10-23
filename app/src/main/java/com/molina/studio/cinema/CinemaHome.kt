package com.molina.studio.cinema

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.cinema.BackgroundPlaybackState
import com.molina.studio.ui.MolinaTheme

private const val HINT = "Ketuk ikon tautan di bawah untuk mencari video di YouTube."
private const val FAIL = "Pemutar gagal dibuka."

@Composable
fun CinemaHome(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var info by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !CinemaActions.play(ctx, uri)) info = FAIL
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
                info = when (val r = CinemaActions.classify(input)) {
                    is CinemaActions.Input.Direct -> if (CinemaActions.play(ctx, r.uri)) "" else FAIL
                    CinemaActions.Input.NeedsExtractor -> "Tautan YouTube perlu extractor (fase 5d), belum tersedia."
                    CinemaActions.Input.Invalid -> "Pencarian judul belum tersedia (fase 5d). Tempel URL media langsung."
                }
            }) { Text("Buka") }
        },
        dismissButton = { TextButton({ dialog = false }) { Text("Batal") } }
    )
}
