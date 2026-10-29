package com.molina.code.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.molina.code.history.DiffOp
import com.molina.code.history.DiffRow
import com.molina.code.history.HistoryFormat
import com.molina.code.history.HistoryStore
import com.molina.code.history.HistoryVersion
import com.molina.code.history.LineDiff
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [CODE-HISTORY] Hasil membaca versi dan membandingkannya dengan isi editor sekarang. */
private class HistDiffState(val old: String?, val result: LineDiff.Result?, val rows: List<DiffRow>)

/**
 * [CODE-HISTORY] Riwayat versi lokal: daftar snapshot berkas tab aktif (dibuat tiap simpan, plus isi disk sebelum
 * ditimpa). Ketuk versi = lihat perbedaan dengan isi editor sekarang (merah: ada di versi itu, hijau: ada di isi
 * sekarang); "Kembalikan" memasang versi itu ke editor sebagai SATU langkah undo, tanpa menyimpan ke disk.
 */
@Composable
internal fun HistoryDialog(session: CodeSession, tab: EditorTab, onDismiss: () -> Unit) {
    val store = session.history
    val file = tab.file
    var versions by remember { mutableStateOf<List<HistoryVersion>?>(null) }
    var picked by remember { mutableStateOf<HistoryVersion?>(null) }
    val now = remember { System.currentTimeMillis() }

    LaunchedEffect(file?.path) {
        versions = if (store == null || file == null) emptyList() else withContext(Dispatchers.IO) { store.list(file) }
    }

    val shape = RoundedCornerShape(CodeDimens.DialogCorner)
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    Dialog(
        onDismissRequest = { if (picked != null) picked = null else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).win7DialogFrame(shape)) {
            val v = picked
            if (store == null || file == null) {
                HistoryMessage("Riwayat versi tidak tersedia untuk tab ini (berkas belum disimpan).")
                HistoryButtons { Win7DialogButton("Tutup", onDismiss, isDefault = true) }
            } else if (v == null) {
                HistoryListPane(
                    store = store, file = file, versions = versions, now = now, maxH = maxH,
                    onPick = { picked = it }, onCleared = { versions = emptyList() }, onClose = onDismiss
                )
            } else {
                HistoryDiffPane(
                    session = session, tab = tab, store = store, file = file, v = v, now = now, maxH = maxH,
                    onBack = { picked = null }, onDone = onDismiss
                )
            }
        }
    }
}

@Composable
private fun HistoryMessage(text: String) {
    BasicText(
        text = text,
        modifier = Modifier.padding(16.dp),
        style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = 13.sp)
    )
}

@Composable
private fun HistoryButtons(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(CodeDimens.Hairline).background(CodeColors.Hairline))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun HistoryListPane(
    store: HistoryStore, file: File, versions: List<HistoryVersion>?, now: Long, maxH: Dp,
    onPick: (HistoryVersion) -> Unit, onCleared: () -> Unit, onClose: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BasicText(
        text = "Riwayat: " + file.name + (if (versions != null) " (" + versions.size + ")" else ""),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        style = TextStyle(color = CodeColors.IconDefault, fontSize = CodeDimens.DialogTitleSize)
    )
    when {
        versions == null -> HistoryMessage("Membaca riwayat\u2026")
        versions.isEmpty() -> HistoryMessage("Belum ada versi. Riwayat dibuat setiap kali berkas disimpan.")
        else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxH)) {
            items(versions) { v ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(v) }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = HistoryFormat.time(v.time, now),
                            maxLines = 1,
                            style = TextStyle(color = CodeColors.IconDefault, fontSize = 14.sp)
                        )
                        BasicText(
                            text = v.lines.toString() + " baris \u00B7 " + HistoryFormat.size(v.chars.toLong()),
                            maxLines = 1,
                            style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = 11.sp)
                        )
                    }
                    BasicText(
                        text = v.kind.label,
                        maxLines = 1,
                        style = TextStyle(color = CodeColors.Gold.copy(alpha = 0.75f), fontSize = 11.sp)
                    )
                }
            }
        }
    }
    BasicText(
        text = "Disimpan di penyimpanan privat aplikasi, maksimal " + HistoryStore.MAX_VERSIONS + " versi per berkas.",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = TextStyle(color = CodeColors.SecondaryChampagne.copy(alpha = 0.7f), fontSize = 11.sp)
    )
    HistoryButtons {
        if (!versions.isNullOrEmpty()) {
            Win7DialogButton(
                label = if (confirmClear) "Yakin hapus?" else "Hapus riwayat",
                onClick = {
                    if (confirmClear) {
                        scope.launch {
                            withContext(Dispatchers.IO) { store.clear(file) }
                            onCleared()
                            confirmClear = false
                        }
                    } else {
                        confirmClear = true
                    }
                },
                accent = CodeColors.Danger
            )
        }
        Win7DialogButton("Tutup", onClose, isDefault = true)
    }
}

@Composable
private fun HistoryDiffPane(
    session: CodeSession, tab: EditorTab, store: HistoryStore, file: File, v: HistoryVersion, now: Long, maxH: Dp,
    onBack: () -> Unit, onDone: () -> Unit
) {
    // Teks editor diambil di thread utama (Sora bukan thread-safe); membaca snapshot dan diff di thread latar.
    val cur = remember(v) { tab.currentTextOrNull(HistoryStore.MAX_CHARS) }
    val state by produceState<HistDiffState?>(null, v) {
        value = withContext(Dispatchers.IO) {
            val old = store.read(file, v)
            if (old == null || cur == null) {
                HistDiffState(old, null, emptyList())
            } else {
                val r = LineDiff.diff(old, cur)
                HistDiffState(old, r, LineDiff.compact(r, 2))
            }
        }
    }
    val label = HistoryFormat.time(v.time, now)
    BasicText(
        text = label + " \u00B7 " + v.kind.label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        style = TextStyle(color = CodeColors.IconDefault, fontSize = CodeDimens.DialogTitleSize)
    )
    val st = state
    val summary = when {
        st == null -> "Membaca versi\u2026"
        st.old == null -> "Versi ini tidak bisa dibaca (berkas snapshot hilang atau rusak)."
        st.result == null -> "Isi sekarang terlalu besar untuk dibandingkan."
        !st.result.changed -> if (st.old == cur) "Sama dengan isi sekarang." else "Hanya beda baris kosong di akhir berkas."
        else -> "\u2212" + st.result.removed + " +" + st.result.added + " baris dibanding isi sekarang" +
            (if (st.result.coarse) " (berkas besar: diff ringkas)" else "")
    }
    BasicText(
        text = summary,
        modifier = Modifier.padding(horizontal = 16.dp),
        style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = 12.sp)
    )
    BasicText(
        text = "Merah: ada di versi ini. Hijau: ada di isi sekarang.",
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        style = TextStyle(color = CodeColors.SecondaryChampagne.copy(alpha = 0.7f), fontSize = 11.sp)
    )
    Box(Modifier.fillMaxWidth().height(CodeDimens.Hairline).background(CodeColors.Hairline))
    if (st != null && st.rows.isNotEmpty()) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxH)) {
            items(st.rows) { row ->
                val ln = row.line
                if (ln == null) {
                    BasicText(
                        text = "\u22EF " + row.skipped + " baris sama",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                        style = TextStyle(color = CodeColors.SecondaryChampagne.copy(alpha = 0.6f), fontSize = 11.sp)
                    )
                } else {
                    val bg = when (ln.op) {
                        DiffOp.ADD -> CodeColors.Clean.copy(alpha = 0.16f)
                        DiffOp.DEL -> CodeColors.Danger.copy(alpha = 0.18f)
                        DiffOp.SAME -> androidx.compose.ui.graphics.Color.Transparent
                    }
                    val sign = when (ln.op) { DiffOp.ADD -> "+"; DiffOp.DEL -> "\u2212"; DiffOp.SAME -> " " }
                    val no = if (ln.op == DiffOp.DEL) ln.oldNo else ln.newNo
                    Row(Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 1.dp)) {
                        BasicText(
                            text = no.toString(),
                            modifier = Modifier.width(40.dp),
                            maxLines = 1,
                            style = TextStyle(color = CodeColors.SecondaryChampagne, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        )
                        BasicText(
                            text = sign + " " + ln.text,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(color = CodeColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        }
    } else {
        Spacer(Modifier.height(4.dp))
    }
    val old = st?.old
    val canRestore = old != null && old != cur && !tab.readOnly && !tab.loading && tab.editor != null
    HistoryButtons {
        Win7DialogButton("Kembali", onBack)
        if (canRestore && old != null) {
            Win7DialogButton("Kembalikan", { session.restoreHistoryText(tab, old, label); onDone() }, isDefault = true)
        }
    }
}

/**
 * [CODE-HISTORY] Pasang [text] (versi lama) ke editor sebagai satu langkah undo. Tidak menyimpan ke disk:
 * tab menjadi "diubah" dan Urungkan membatalkan pengembalian. Kursor dijaga di baris yang sama bila masih ada.
 */
internal fun CodeSession.restoreHistoryText(tab: EditorTab, text: String, label: String) {
    val ed = tab.editor
    if (ed == null || tab.loading || tab.readOnly || tab !in tabs) {
        toast("Editor belum siap atau tab hanya-baca")
        return
    }
    try {
        val line = ed.cursor.leftLine
        val col = ed.cursor.leftColumn
        val content = ed.text
        content.replace(0, content.length, text)
        val l = line.coerceIn(0, ed.text.lineCount - 1)
        ed.setSelection(l, col.coerceAtMost(ed.text.getColumnCount(l)))
        toast("Versi $label dikembalikan. Simpan untuk menerapkan, Urungkan untuk membatalkan")
    } catch (e: Exception) {
        toast("Gagal mengembalikan versi")
    }
}
