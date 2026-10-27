package com.molina.studio.terminal

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.molina.studio.ui.MolinaTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pilihan tampilan Terminal (SharedPreferences molina_ui): layar tetap menyala. */
internal object TerminalUi {
    private const val PREFS = "molina_ui"
    private const val KEY_KEEP_ON = "term_keep_on"
    private var loaded = false

    var keepScreenOn by mutableStateOf(false)
        private set

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        keepScreenOn = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_KEEP_ON, false)
    }

    fun setKeepScreenOn(ctx: Context, v: Boolean) {
        loaded = true
        keepScreenOn = v
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_KEEP_ON, v).apply()
    }
}

/**
 * Simpan seluruh transkrip (scrollback + layar) ke /sdcard/molina-studio/terminal-logs/.
 * Mengembalikan path versi Debian (/sdcard/...), atau null bila kosong/gagal.
 */
internal fun saveTranscript(): String? {
    val text = TerminalSessions.currentSession?.emulator?.screen?.transcriptText
    if (text.isNullOrBlank()) return null
    return try {
        val name = "terminal-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
        val dir = File(Environment.getExternalStorageDirectory(), TerminalSessions.WORKSPACE_DIR + "/terminal-logs")
        dir.mkdirs()
        File(dir, name).writeText(text.trimEnd() + "\n")
        "/sdcard/" + TerminalSessions.WORKSPACE_DIR + "/terminal-logs/" + name
    } catch (e: Exception) {
        null
    }
}

private val HdrTop = Color(0xFF3B6EA5)
private val HdrMid = Color(0xFF244A75)
private val HdrBottom = Color(0xFF14263F)
private val HdrDim = Color(0xFFBFD9F2)

/**
 * Header ala Windows 7 Aero di atas viewport Terminal: titik status sesi, nomor sesi, zoom font A-/A+,
 * saklar layar tetap menyala, dan menu utilitas (Ctrl+C, Ctrl+R, simpan transkrip, pengaturan).
 * Extra keys di bawah tidak diubah.
 */
@Composable
internal fun TerminalHeader(onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    remember(ctx) { TerminalUi.load(ctx) }
    val total = TerminalSessions.sessions.size
    val idx = TerminalSessions.currentIndex
    val label = TerminalSessions.labelOf(TerminalSessions.active)
    val fontSp = TerminalPrefs.fontSp
    val keepOn = TerminalUi.keepScreenOn
    var menu by remember { mutableStateOf(false) }
    var snippets by remember { mutableStateOf(false) }
    var toCode by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Brush.verticalGradient(listOf(HdrTop, HdrMid, HdrBottom)))
            .drawBehind {
                drawRect(Color.White.copy(alpha = 0.10f), Offset.Zero, Size(size.width, size.height * 0.5f))
                drawLine(Color(0x99BFE0FF), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MolinaTheme.Good))
        Spacer(Modifier.width(8.dp))
        Text("Debian", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(
            "[" + (idx + 1) + "/" + total + "]",
            color = HdrDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace
        )
        if (label != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                label, color = HdrDim, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(Modifier.weight(1f))
        HdrBtn("A\u2212", enabled = fontSp > TerminalPrefs.MIN_FONT_SP) { TerminalPrefs.setFontSp(fontSp - 1) }
        HdrBtn("A+", enabled = fontSp < TerminalPrefs.MAX_FONT_SP) { TerminalPrefs.setFontSp(fontSp + 1) }
        HdrBtn(if (keepOn) "\u2600" else "\u263E") {
            TerminalUi.setKeepScreenOn(ctx, !keepOn)
            Toast.makeText(
                ctx, if (!keepOn) "Layar tetap menyala: aktif" else "Layar tetap menyala: nonaktif",
                Toast.LENGTH_SHORT
            ).show()
        }
        Box {
            HdrBtn("\u22EE") { menu = true }
            if (snippets) SnippetDialog { snippets = false }
            if (toCode) SendToCodeDialog { toCode = false }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Hentikan proses (Ctrl+C)") },
                    onClick = { menu = false; TerminalActions.send("\u0003") }
                )
                DropdownMenuItem(
                    text = { Text("Cari riwayat perintah (Ctrl+R)") },
                    onClick = { menu = false; TerminalActions.send("\u0012") }
                )
                DropdownMenuItem(
                    text = { Text("Simpan transkrip ke berkas") },
                    onClick = {
                        menu = false
                        val path = saveTranscript()
                        Toast.makeText(
                            ctx, if (path != null) "Transkrip disimpan: $path" else "Tidak ada teks untuk disimpan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Snippet perintah") },
                    onClick = { menu = false; snippets = true }
                )
                DropdownMenuItem(
                    text = { Text("Sisip keluaran ke Code") },
                    onClick = { menu = false; toCode = true }
                )
                DropdownMenuItem(
                    text = { Text("Pengaturan") },
                    onClick = { menu = false; onOpenSettings() }
                )
            }
        }
    }
}

@Composable
private fun HdrBtn(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .padding(start = 4.dp)
            .size(width = 34.dp, height = 28.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A3A50), Color(0xFF111A28))), shape)
            .border(1.dp, Color(0x66BFE0FF), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = if (enabled) Color.White else Color(0x66FFFFFF))
    }
}
