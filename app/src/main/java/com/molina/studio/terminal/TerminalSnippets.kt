package com.molina.studio.terminal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.molina.studio.cinema.Win7
import com.molina.studio.cinema.Win7Button
import com.molina.studio.settings.Win7GlassWindow
import org.json.JSONArray
import org.json.JSONObject

/** Snippet perintah Terminal (SharedPreferences molina_ui/term_snippets). Bawaan dipakai sampai pengguna mengubah daftar. */
internal object TerminalSnippets {
    class Snippet(val name: String, val cmd: String)

    private const val PREFS = "molina_ui"
    private const val KEY = "term_snippets"
    private var loaded = false

    val items = mutableStateListOf<Snippet>()

    private fun defaults() = listOf(
        Snippet("Perbarui paket", "apt update && apt upgrade -y"),
        Snippet("Status git", "git status -sb"),
        Snippet("Log git ringkas", "git log --oneline -10"),
        Snippet("Ukuran folder", "du -sh * | sort -h"),
        Snippet("Port terbuka", "ss -tulpn")
    )

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val raw = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        val list = if (raw == null) defaults() else try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Snippet(o.getString("n"), o.getString("c"))
            }
        } catch (e: Exception) {
            defaults()
        }
        items.clear()
        items.addAll(list)
    }

    private fun save(ctx: Context) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("n", it.name).put("c", it.cmd)) }
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, name: String, cmd: String) {
        items.add(Snippet(name.trim(), cmd.trim()))
        save(ctx)
    }

    fun removeAt(ctx: Context, index: Int) {
        if (index in items.indices) {
            items.removeAt(index)
            save(ctx)
        }
    }
}

@Composable
private fun SnipBtn(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier.padding(start = 4.dp).height(28.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Win7.BtnTop, Win7.BtnBottom)), shape)
            .border(1.dp, Win7.BtnLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        Alignment.Center
    ) { Text(label, color = Win7.Ink, fontSize = 12.sp) }
}

/**
 * Dialog snippet: "Sisip" menulis perintah ke prompt tanpa Enter (bisa diedit dulu), "Jalan" menulis lalu Enter.
 * Daftar bisa ditambah dan dihapus (tombol x).
 */
@Composable
internal fun SnippetDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    remember(ctx) { TerminalSnippets.load(ctx) }
    var name by remember { mutableStateOf("") }
    var cmd by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Win7GlassWindow(
                title = "Snippet perintah",
                icon = { Text("\u276F", color = Color.White, fontSize = 12.sp) }
            ) {
                Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    TerminalSnippets.items.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, color = Win7.Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    s.cmd, color = Win7.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            }
                            SnipBtn("Sisip") { TerminalActions.send(s.cmd); onDismiss() }
                            SnipBtn("Jalan") { TerminalActions.send(s.cmd + "\r"); onDismiss() }
                            Text(
                                "\u2715", color = Win7.TextDim, fontSize = 13.sp,
                                modifier = Modifier.clickable { TerminalSnippets.removeAt(ctx, i) }.padding(8.dp)
                            )
                        }
                    }
                }
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Win7.Ink,
                    unfocusedTextColor = Win7.Ink,
                    focusedBorderColor = Win7.DefaultLine,
                    unfocusedBorderColor = Win7.Line,
                    cursorColor = Win7.DefaultLine,
                    focusedPlaceholderColor = Win7.TextDim,
                    unfocusedPlaceholderColor = Win7.TextDim,
                    focusedContainerColor = Win7.Input,
                    unfocusedContainerColor = Win7.Input
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(3.dp),
                    placeholder = { Text("Nama snippet", fontSize = 13.sp) }, colors = fieldColors
                )
                OutlinedTextField(
                    value = cmd, onValueChange = { cmd = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(3.dp),
                    placeholder = { Text("Perintah", fontSize = 13.sp) }, colors = fieldColors
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    Win7Button("Tutup", onClick = onDismiss)
                    Win7Button("Tambah", isDefault = true, enabled = name.isNotBlank() && cmd.isNotBlank()) {
                        TerminalSnippets.add(ctx, name, cmd)
                        name = ""
                        cmd = ""
                    }
                }
            }
        }
    }
}
