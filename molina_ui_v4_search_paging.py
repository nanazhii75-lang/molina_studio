#!/usr/bin/env python3
"""Molina Cinema v4: Prev/Next halaman hasil pencarian di Home, dan dialog hasil pencarian di PLAYER
disamakan dengan Home (gaya channel TV, cari/lompat nomor, Prev/Next, jam, penghitung). Idempotent.
Prasyarat: v1, v2, v3 sudah dijalankan."""
import os, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
APP = os.path.join(ROOT, "app/src/main")
MOD = os.path.join(ROOT, "feature/mpv/src/main")
RES = os.path.join(MOD, "res")
KT = os.path.join(MOD, "java/com/molina/cinema")
CIN = os.path.join(APP, "java/com/molina/studio/cinema")
RESULTS = os.path.join(CIN, "CinemaResults.kt")
if not (os.path.exists(os.path.join(RES, "drawable/bg_channel_selected.xml")) and os.path.exists(RESULTS)):
    sys.exit("Jalankan v1, v2, dan v3 terlebih dahulu (root repo: %s)" % ROOT)

def rd(p): return open(p, encoding="utf-8").read()
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path) and rd(path) == text:
        print("skip  ", os.path.relpath(path, ROOT)); return
    open(path, "w", encoding="utf-8").write(text); print("tulis ", os.path.relpath(path, ROOT))
def vec(d):
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp"\n'
            '    android:viewportWidth="24" android:viewportHeight="24">\n'
            f'  <path android:fillColor="#FFFFFFFF" android:pathData="{d}"/>\n</vector>\n')

PREV = "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z"
NEXT = "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z"
write(os.path.join(APP, "res/drawable/ic_tv_page_prev.xml"), vec(PREV))
write(os.path.join(APP, "res/drawable/ic_tv_page_next.xml"), vec(NEXT))
write(os.path.join(RES, "drawable/ic_page_prev.xml"), vec(PREV))
write(os.path.join(RES, "drawable/ic_page_next.xml"), vec(NEXT))

# ================= HOME (Compose): Prev / Next =================
c = rd(RESULTS)
if "ic_tv_page_prev" in c:
    print("skip   CinemaResults.kt")
else:
    def sub(old, new):
        global c
        if c.count(old) != 1: sys.exit("Pola tidak ditemukan/ganda di CinemaResults.kt:\n" + old)
        c = c.replace(old, new)
    sub("import androidx.compose.runtime.remember\n",
        "import androidx.compose.runtime.remember\nimport androidx.compose.runtime.rememberCoroutineScope\n")
    sub("import kotlinx.coroutines.delay\n", "import kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch\n")
    sub("private val Accent = Color(0xFF4DA3FF)\n",
        "private val Accent = Color(0xFF4DA3FF)\n\n/** Prev/Next memindahkan sorotan satu 'halaman' (seperti CH+/CH- cepat di remote TV). */\nprivate const val PAGE_SIZE = 10\n")
    sub("    val focus = remember { FocusRequester() }\n",
        "    val focus = remember { FocusRequester() }\n    val scope = rememberCoroutineScope()\n")
    sub("    fun pickFromQuery() {",
        """    fun page(delta: Int) {
        if (visible.isEmpty()) return
        val cur = visible.indexOfFirst { it.number == selected }.coerceAtLeast(0)
        val idx = (cur + delta).coerceIn(0, visible.lastIndex)
        selected = visible[idx].number
        scope.launch { listState.scrollToItem(idx) }
    }

    fun pickFromQuery() {""")
    sub('                    TvKey(R.drawable.ic_tv_search, "Cari",',
        '''                    TvKey(R.drawable.ic_tv_page_prev, "Prev", Color.White, Modifier.weight(1f)) { page(-PAGE_SIZE) }
                    TvKey(R.drawable.ic_tv_page_next, "Next", Color.White, Modifier.weight(1f)) { page(PAGE_SIZE) }
                    TvKey(R.drawable.ic_tv_search, "Cari",''')
    open(RESULTS, "w", encoding="utf-8").write(c); print("patch  CinemaResults.kt (Prev/Next)")

# ================= PLAYER (View): dialog hasil pencarian =================
sp = os.path.join(RES, "values/strings.xml")
s = rd(sp)
if "tv_key_prev" not in s:
    s = s.replace("</resources>",
      '    <string name="tv_key_prev" translatable="false">Prev</string>\n'
      '    <string name="tv_key_next" translatable="false">Next</string>\n'
      '    <string name="tv_info_results" translatable="false">Hasil %1$03d dari %2$03d</string>\n</resources>')
    open(sp, "w", encoding="utf-8").write(s); print("patch  strings.xml")
else: print("skip   strings.xml")

def key(id_, text, icon):
    return (f'<Button\n            android:id="@+id/{id_}"\n            style="@style/Widget.Molina.TvKey"\n'
            f'            android:layout_width="0dp"\n            android:layout_height="wrap_content"\n'
            f'            android:layout_weight="1"\n            android:drawableTop="@drawable/{icon}"\n'
            f'            android:text="@string/{text}" />')

LAYOUT = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Molina: hasil pencarian di player, gaya sama dengan playlist / Home -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="24dp"
        android:layout_marginTop="16dp"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/headerTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textAppearance="@style/TextAppearance.AppCompat.Title"
            tools:text="Search results (50)" />

        <TextView
            android:id="@+id/countText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:fontFamily="monospace"
            android:textColor="@color/player_dialog_text_secondary"
            android:textSize="13sp"
            tools:text="001/050" />

        <TextClock
            android:id="@+id/clockText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:format24Hour="HH:mm"
            android:format12Hour="HH:mm"
            android:textColor="@color/player_dialog_text"
            android:textSize="15sp"
            android:textStyle="bold" />

    </LinearLayout>

    <EditText
        android:id="@+id/searchInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="20dp"
        android:layout_marginTop="4dp"
        android:hint="@string/tv_search_hint"
        android:imeOptions="actionDone"
        android:importantForAutofill="no"
        android:inputType="text"
        android:maxLines="1"
        android:textSize="15sp"
        android:visibility="gone" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:padding="8dp"
        app:layoutManager="LinearLayoutManager"
        tools:listitem="@layout/dialog_search_result_row" />

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="?android:attr/listDivider" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="8dp"
        android:layout_marginVertical="6dp"
        android:orientation="horizontal">

        KEYS

    </LinearLayout>

</LinearLayout>
'''.replace("KEYS", "\n\n        ".join([
    key("prevBtn", "tv_key_prev", "ic_page_prev"),
    key("nextBtn", "tv_key_next", "ic_page_next"),
    key("searchBtn", "tv_key_search", "ic_menu_search"),
    key("infoBtn", "tv_key_info", "ic_menu_info"),
    key("closeBtn", "tv_key_exit", "ic_menu_close"),
]))
write(os.path.join(RES, "layout/dialog_search_results.xml"), LAYOUT)

ROW = '''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/channelRow"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="2dp"
    android:background="@drawable/bg_channel_row"
    android:gravity="center_vertical"
    android:minHeight="52dp"
    android:orientation="horizontal"
    android:paddingStart="10dp"
    android:paddingTop="8dp"
    android:paddingEnd="10dp"
    android:paddingBottom="8dp">

    <TextView
        android:id="@+id/channelNumber"
        android:layout_width="44dp"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:textSize="14sp"
        android:textStyle="bold"
        tools:text="001" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/resultTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="end"
            android:maxLines="2"
            android:textSize="15sp"
            tools:text="Judul video hasil pencarian" />

        <TextView
            android:id="@+id/resultMeta"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:alpha="0.7"
            android:ellipsize="end"
            android:singleLine="true"
            android:textSize="12sp"
            tools:text="Nama kanal · 3:45" />

    </LinearLayout>

</LinearLayout>
'''
write(os.path.join(RES, "layout/dialog_search_result_row.xml"), ROW)

KOTLIN = r'''package com.molina.cinema

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.molina.cinema.databinding.DialogSearchResultsBinding
import com.molina.core.common.StreamSearchItem

/** Satu baris hasil: nomor tetap (tidak berubah saat disaring). */
internal class SearchEntry(val number: Int, val item: StreamSearchItem)

/**
 * Daftar hasil pencarian di player, gaya sama dengan playlist dan Cinema Home:
 * nomor 001.., baris terpilih kotak biru, Prev/Next (satu halaman), Cari (teks = saring,
 * angka = lompat ke nomor, Enter = pilih), Info, Exit, penghitung + jam.
 */
internal class SearchResultsDialog(
    private val items: List<StreamSearchItem>,
    private val metaOf: (StreamSearchItem) -> String,
    /** Dipanggil dengan indeks item pada daftar asli. */
    private val onPick: (Int) -> Unit,
    private val onClose: () -> Unit,
) {
    private lateinit var b: DialogSearchResultsBinding
    private val entries = items.mapIndexed { i, r -> SearchEntry(i + 1, r) }
    private var visible = entries
    private var query = ""
    private var selected = 1

    fun buildView(inflater: LayoutInflater): View {
        b = DialogSearchResultsBinding.inflate(inflater)
        val ctx = b.root.context
        b.headerTitle.text = ctx.getString(R.string.search_results_title, entries.size)
        b.list.adapter = CustomAdapter()
        updateCount()

        b.prevBtn.setOnClickListener { page(-PAGE_SIZE) }
        b.nextBtn.setOnClickListener { page(PAGE_SIZE) }
        b.searchBtn.setOnClickListener { toggleSearch() }
        b.infoBtn.setOnClickListener {
            Toast.makeText(ctx, ctx.getString(R.string.tv_info_results, selected, entries.size),
                Toast.LENGTH_SHORT).show()
        }
        b.closeBtn.setOnClickListener { onClose() }

        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                applyFilter()
            }
        })
        b.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val q = query.trim()
                val target = if (q.isNotEmpty() && q.all { it.isDigit() })
                    visible.firstOrNull { it.number == q.toIntOrNull() }
                else visible.singleOrNull()
                if (target != null) onPick(target.number - 1)
                true
            } else false
        }

        Utils.handleInsetsAsPadding(b.root)
        return b.root
    }

    private fun lm() = b.list.layoutManager as LinearLayoutManager

    private fun updateCount() {
        b.countText.text = String.format("%03d/%03d", selected, entries.size)
    }

    private fun select(number: Int) {
        if (number == selected) return
        val old = visible.indexOfFirst { it.number == selected }
        val new = visible.indexOfFirst { it.number == number }
        selected = number
        if (old >= 0) b.list.adapter!!.notifyItemChanged(old)
        if (new >= 0) b.list.adapter!!.notifyItemChanged(new)
        updateCount()
    }

    /** Prev/Next: geser sorotan satu halaman dan gulir ke sana. */
    private fun page(delta: Int) {
        if (visible.isEmpty()) return
        val cur = visible.indexOfFirst { it.number == selected }.coerceAtLeast(0)
        val idx = (cur + delta).coerceIn(0, visible.lastIndex)
        select(visible[idx].number)
        lm().scrollToPositionWithOffset(idx, 0)
    }

    private fun toggleSearch() {
        val show = !b.searchInput.isVisible
        val imm = b.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            b.searchInput.isVisible = true
            b.searchInput.requestFocus()
            b.searchInput.post { imm.showSoftInput(b.searchInput, InputMethodManager.SHOW_IMPLICIT) }
        } else {
            b.searchInput.setText("")
            b.searchInput.isVisible = false
            imm.hideSoftInputFromWindow(b.searchInput.windowToken, 0)
        }
        val ctx = b.root.context
        val c = ContextCompat.getColor(ctx,
            if (show) R.color.player_playlist_active else R.color.player_dialog_text)
        b.searchBtn.setTextColor(c)
        b.searchBtn.compoundDrawableTintList = ColorStateList.valueOf(c)
    }

    private fun applyFilter() {
        val q = query.trim()
        val numeric = q.isNotEmpty() && q.all { it.isDigit() }
        visible = if (q.isEmpty() || numeric) entries
            else entries.filter { it.item.title.contains(q, ignoreCase = true) }
        var jumpTo = -1
        if (numeric) {
            jumpTo = visible.indexOfFirst { it.number == q.toIntOrNull() }
            if (jumpTo >= 0) selected = visible[jumpTo].number
        }
        b.list.adapter!!.notifyDataSetChanged()
        updateCount()
        if (jumpTo >= 0) lm().scrollToPositionWithOffset(jumpTo, 0)
    }

    private inner class CustomAdapter : RecyclerView.Adapter<CustomAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            private val numberView: TextView = view.findViewById(R.id.channelNumber)
            private val titleView: TextView = view.findViewById(R.id.resultTitle)
            private val metaView: TextView = view.findViewById(R.id.resultMeta)

            init {
                // Sorot saat disentuh (seperti menggeser kursor di TV), lalu klik memilih.
                view.setOnTouchListener { _, ev ->
                    if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                        val pos = bindingAdapterPosition
                        if (pos >= 0) select(visible[pos].number)
                    }
                    false
                }
                view.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos >= 0) onPick(visible[pos].number - 1)
                }
            }

            fun bind(e: SearchEntry) {
                val ctx = titleView.context
                val sel = e.number == selected
                val textColor = ContextCompat.getColor(ctx, R.color.player_dialog_text)
                numberView.text = String.format("%03d", e.number)
                numberView.setTextColor(if (sel) textColor else 0xFFC8E600.toInt())
                titleView.text = e.item.title
                titleView.setTextColor(textColor)
                titleView.setTypeface(null, if (sel) Typeface.BOLD else Typeface.NORMAL)
                val meta = metaOf(e.item)
                metaView.text = meta
                metaView.setTextColor(textColor)
                metaView.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE
                itemView.setBackgroundResource(
                    if (sel) R.drawable.bg_channel_selected else R.drawable.bg_channel_row)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.dialog_search_result_row, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(visible[position])

        override fun getItemCount() = visible.size
    }

    companion object {
        /** Jumlah baris per Prev/Next. */
        private const val PAGE_SIZE = 10
    }
}
'''
write(os.path.join(KT, "SearchResultsDialog.kt"), KOTLIN)

# ---- MPVActivity.showSearchResults ----
ma = os.path.join(KT, "MPVActivity.kt")
m = rd(ma)
if "SearchResultsDialog(" in m:
    print("skip   MPVActivity.kt")
else:
    start = m.find("    private fun showSearchResults(")
    end = m.find("    /** Putar hasil yang diketuk;", start)
    if start < 0 or end < 0: sys.exit("showSearchResults tidak ditemukan di MPVActivity.kt")
    new = '''    private fun showSearchResults(items: List<StreamSearchItem>, append: Boolean) {
        val restore = pauseForDialog()
        var chosen = false
        lateinit var dialog: AlertDialog
        val impl = SearchResultsDialog(items, { searchMeta(it) }, onPick = { pos ->
            val picked = items[pos]
            if (append) {
                PlaybackLog.onRequest(picked.pageUrl)
                loadWeb(picked.pageUrl, MpvLoad.APPEND) {
                    showToast(getString(R.string.search_added, picked.title), true)
                }
            } else {
                chosen = true
                dialog.dismiss()
                playSearchResult(items, pos)
            }
        }, onClose = { dialog.dismiss() })
        val builder = AlertDialog.Builder(this, R.style.AppTheme_PlayerDialog)
        builder.setView(impl.buildView(LayoutInflater.from(builder.context)))
        builder.setOnDismissListener {
            restore()
            if (chosen) player.paused = false
        }
        dialog = builder.create()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
    }

'''
    open(ma, "w", encoding="utf-8").write(m[:start] + new + m[end:]); print("patch  MPVActivity.kt")

pg = os.path.join(ROOT, "progres.md")
if os.path.exists(pg):
    p = rd(pg)
    if "Cinema UI v4" not in p:
        open(pg, "w", encoding="utf-8").write(p.rstrip("\n") + """

## 2026-10-25 - Cinema UI v4 (Prev/Next hasil pencarian + pencarian player disamakan)
- [x] feat(home-ui): SearchResultsDialog Home: tombol Prev/Next (geser sorotan satu halaman = 10 baris)
- [x] feat(player-ui): hasil pencarian di player (MPVActivity.showSearchResults) kini memakai SearchResultsDialog (View) bergaya sama dengan Home: nomor, kotak biru, Prev/Next, Cari/lompat nomor, Info, Exit, jam + penghitung; mode Play (putar + antrean bergulir) dan Add to queue (daftar tetap terbuka) tidak berubah; user mencari dan memutar dari player tanpa kembali ke Home
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: Prev/Next di Home dan player, Add to queue (daftar tetap terbuka, sorotan pindah), keyboard + resize, lompat nomor
- [ ] dialog_search_item.xml (tata letak lama) kini tidak terpakai; hapus setelah CI hijau
""")
        print("patch  progres.md")
    else: print("skip   progres.md")
print("selesai.")
