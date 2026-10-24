#!/usr/bin/env python3
"""Molina Cinema v2: menu atas ikon-saja + playlist (cari/lompat nomor, jam, penghitung). Idempotent.
Prasyarat: molina_ui_menu_playlist.py sudah dijalankan."""
import os, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
MOD = os.path.join(ROOT, "feature/mpv/src/main")
RES = os.path.join(MOD, "res")
KT = os.path.join(MOD, "java/com/molina/cinema")
if not os.path.exists(os.path.join(RES, "drawable/bg_channel_selected.xml")):
    sys.exit("Jalankan molina_ui_menu_playlist.py terlebih dahulu (root repo: %s)" % ROOT)

def rd(p): return open(p, encoding="utf-8").read()
def write(path, text):
    if os.path.exists(path) and rd(path) == text:
        print("skip  ", os.path.relpath(path, ROOT)); return
    open(path, "w", encoding="utf-8").write(text); print("tulis ", os.path.relpath(path, ROOT))

def vec(d):
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp"\n'
            '    android:viewportWidth="24" android:viewportHeight="24">\n'
            f'  <path android:fillColor="#FFFFFFFF" android:pathData="{d}"/>\n</vector>\n')

ICONS = {
 "ic_menu_prev": "M6,6h2v12H6zM9.5,12l8.5,6V6z",
 "ic_menu_next": "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z",
 "ic_menu_search": "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
}
for n, d in ICONS.items():
    write(os.path.join(RES, "drawable", n + ".xml"), vec(d))

# ---- style ikon-saja ----
sp = os.path.join(RES, "values/molina_menu_styles.xml")
s = rd(sp)
if "Widget.Molina.MenuIcon" not in s:
    s = s.replace("</resources>", '''
    <!-- Menu atas: hanya ikon (tekan lama = tooltip) -->
    <style name="Widget.Molina.MenuIcon" parent="Widget.AppCompat.ImageButton">
        <item name="android:background">?attr/selectableItemBackgroundBorderless</item>
        <item name="android:tint">@color/player_dialog_text</item>
        <item name="android:scaleType">center</item>
        <item name="android:minWidth">56dp</item>
        <item name="android:minHeight">56dp</item>
    </style>
</resources>''')
    open(sp, "w", encoding="utf-8").write(s); print("patch  molina_menu_styles.xml")
else: print("skip   molina_menu_styles.xml")

# ---- string ----
stp = os.path.join(RES, "values/strings.xml")
s = rd(stp)
if "tv_key_search" not in s:
    s = s.replace("</resources>",
      '    <string name="tv_key_search" translatable="false">Cari</string>\n'
      '    <string name="tv_search_hint" translatable="false">Cari nama atau ketik nomor channel</string>\n</resources>')
    open(stp, "w", encoding="utf-8").write(s); print("patch  strings.xml")
else: print("skip   strings.xml")

# ---- menu atas: ikon saja ----
def ib(id_, desc, icon):
    return (f'<ImageButton\n            android:id="@+id/{id_}"\n            style="@style/Widget.Molina.MenuIcon"\n'
            f'            android:layout_width="0dp"\n            android:layout_height="56dp"\n            android:layout_weight="1"\n'
            f'            android:contentDescription="@string/{desc}"\n            android:tooltipText="@string/{desc}"\n'
            f'            android:src="@drawable/{icon}" />')
def row(id_, *items):
    rid = f'\n        android:id="@+id/{id_}"' if id_ else ""
    return (f'<LinearLayout{rid}\n        android:layout_width="match_parent"\n        android:layout_height="wrap_content"\n'
            f'        android:orientation="horizontal">\n\n        ' + "\n\n        ".join(items) + '\n\n    </LinearLayout>')

top = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Molina: menu atas hanya ikon. ID tombol tetap (MPVActivity.openTopMenu). Tekan lama = tooltip. -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:paddingHorizontal="8dp"
    android:paddingVertical="8dp">

    <!-- Since this dialog does not scroll, don't add too many buttons here! -->

    {row(None,
         ib("audioBtn","open_external_audio","ic_menu_audio_file"),
         ib("subBtn","open_external_sub","ic_menu_subtitles"),
         ib("playlistBtn","action_playlist","ic_menu_playlist"),
         ib("advancedBtn","advanced_menu","ic_menu_tune"))}

    {row("rowMode",
         ib("backgroundBtn","resume_bg_playback","ic_menu_headset"),
         ib("audioOnlyBtn","audio_only_on","ic_menu_audiotrack"),
         ib("orientationBtn","switch_orientation","ic_menu_rotate"))}

    {row("rowChapter",
         ib("chapterPrev","dialog_prev","ic_menu_prev"),
         ib("chapterBtn","chapter_button","ic_menu_chapters"),
         ib("chapterNext","dialog_next","ic_menu_next"))}

</LinearLayout>
'''
write(os.path.join(RES, "layout/dialog_top_menu.xml"), top)

# ---- layout playlist ----
def key(id_, text, icon):
    return (f'<Button\n            android:id="@+id/{id_}"\n            style="@style/Widget.Molina.TvKey"\n'
            f'            android:layout_width="0dp"\n            android:layout_height="wrap_content"\n'
            f'            android:layout_weight="1"\n            android:drawableTop="@drawable/{icon}"\n'
            f'            android:text="@string/{text}" />')
def wbtn(id_, text, icon):
    return (f'<Button\n            android:id="@+id/{id_}"\n            style="@style/Widget.AppCompat.Button.Borderless.Colored"\n'
            f'            android:layout_width="0dp"\n            android:layout_height="wrap_content"\n'
            f'            android:layout_weight="1"\n            android:drawableStart="@drawable/ic_menu_{icon}"\n'
            f'            android:drawablePadding="8dp"\n            android:drawableTint="@color/player_playlist_active"\n'
            f'            android:text="@string/{text}" />')

pl = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Molina: playlist bergaya daftar channel TV digital -->
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
            android:text="@string/action_playlist"
            android:textAppearance="@style/TextAppearance.AppCompat.Title" />

        <TextView
            android:id="@+id/countText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:fontFamily="monospace"
            android:textColor="@color/player_dialog_text_secondary"
            android:textSize="13sp"
            tools:text="003/012" />

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

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/list"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipToPadding="false"
            android:padding="8dp"
            app:layoutManager="LinearLayoutManager"
            tools:listitem="@layout/dialog_playlist_item" />

    </FrameLayout>

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="?android:attr/listDivider" />

    <!-- Action bar ala panel TV -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="8dp"
        android:layout_marginTop="6dp"
        android:orientation="horizontal">

        {key("shuffleBtn","tv_key_shuffle","ic_shuffle_24dp")}

        {key("repeatBtn","tv_key_repeat","ic_repeat_24dp")}

        {key("searchBtn","tv_key_search","ic_menu_search")}

        {key("infoBtn","tv_key_info","ic_menu_info")}

        {key("menuBtn","tv_key_menu","ic_menu_grid")}

        {key("exitBtn","tv_key_exit","ic_menu_close")}

    </LinearLayout>

    <TableRow
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        {wbtn("fileBtn","action_pick_file","file_open")}

        {wbtn("urlBtn","action_open_url","link")}

    </TableRow>

</LinearLayout>
'''
write(os.path.join(RES, "layout/dialog_playlist.xml"), pl)

# ---- PlaylistDialog.kt ----
KOTLIN = r'''package com.molina.cinema

import com.molina.cinema.databinding.DialogPlaylistBinding
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
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

/** Baris item hasil pencarian yang belum di-resolve (belum ada di antrean mpv). */
internal data class PendingRow(val planIndex: Int, val title: String)

/** Satu baris "channel": nomor tetap (tidak berubah saat difilter). */
internal class PlaylistRow(
    val number: Int,
    val text: String,
    val selected: Boolean,
    val item: MPVView.PlaylistItem?,
    val planIndex: Int,
)

internal class PlaylistDialog(
    private val player: MPVView,
    /** URL stream -> judul; menggantikan nama berkas mentah. */
    private val titles: () -> Map<String, String> = { emptyMap() },
    private val pending: () -> List<PendingRow> = { emptyList() },
) {
    private lateinit var binding: DialogPlaylistBinding

    private var playlist = listOf<MPVView.PlaylistItem>()
    private var pendingRows = listOf<PendingRow>()
    private var selectedIndex = -1
    private var allRows = listOf<PlaylistRow>()
    private var visibleRows = listOf<PlaylistRow>()
    private var query = ""

    interface Listeners {
        fun pickFile()
        fun openUrl()
        fun onItemPicked(item: MPVView.PlaylistItem)
        fun onPendingPicked(planIndex: Int) {}
        /** Tombol Exit pada action bar. */
        fun onExit() {}
        /** Tombol Menu pada action bar (buka menu atas). */
        fun onMenu() {}
    }

    var listeners: Listeners? = null

    fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogPlaylistBinding.inflate(layoutInflater)

        binding.list.adapter = CustomAdapter(this)
        binding.list.setHasFixedSize(true)
        refresh()

        binding.fileBtn.setOnClickListener { listeners?.pickFile() }
        binding.urlBtn.setOnClickListener { listeners?.openUrl() }
        binding.exitBtn.setOnClickListener { listeners?.onExit() }
        binding.menuBtn.setOnClickListener { listeners?.onMenu() }
        binding.infoBtn.setOnClickListener { showInfo() }
        binding.searchBtn.setOnClickListener { toggleSearch() }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                applyFilter()
            }
        })
        // Enter/Done: angka = putar channel itu; teks = putar bila hasil tepat satu
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val q = query.trim()
                val target = if (q.isNotEmpty() && q.all { it.isDigit() })
                    visibleRows.firstOrNull { it.number == q.toIntOrNull() }
                else visibleRows.singleOrNull()
                if (target != null) pick(target)
                true
            } else false
        }

        binding.shuffleBtn.setOnClickListener {
            player.changeShuffle(true)
            refresh()
        }
        binding.repeatBtn.setOnClickListener {
            player.cycleRepeat()
            refresh()
        }

        Utils.handleInsetsAsPadding(binding.root)
        return binding.root
    }

    fun refresh() {
        selectedIndex = MPVLib.getPropertyInt("playlist-pos") ?: -1
        playlist = player.loadPlaylist()
        pendingRows = pending()
        MpvLog.v(TAG, "PlaylistDialog: loaded ${playlist.size} items")

        val titleMap = titles()
        val rows = ArrayList<PlaylistRow>(playlist.size + pendingRows.size)
        playlist.forEachIndexed { i, item ->
            val text = titleMap[item.filename] ?: item.title ?: Utils.fileBasename(item.filename)
            rows.add(PlaylistRow(i + 1, text, item.index == selectedIndex, item, -1))
        }
        pendingRows.forEachIndexed { i, p ->
            rows.add(PlaylistRow(playlist.size + i + 1, p.title, false, null, p.planIndex))
        }
        allRows = rows
        applyFilter(scrollToPlaying = true)

        /*
         * At least on api 33 there is in some cases a (reproducible) bug, where the space below the
         * recycler view for the buttons is not taken into account and they go out-of-bounds of the
         * alert dialog. This fixes it.
         */
        binding.list.post {
            binding.list.parent.requestLayout()
        }

        val ctx = binding.root.context
        val accent = ContextCompat.getColor(ctx, R.color.player_playlist_active)
        val normal = ContextCompat.getColor(ctx, R.color.player_dialog_text)
        val disabled = ContextCompat.getColor(ctx, R.color.alpha_disabled)

        val playingPos = rows.indexOfFirst { it.selected } + 1
        binding.countText.text = String.format("%03d/%03d", playingPos, rows.size)

        binding.shuffleBtn.apply {
            isEnabled = playlist.size > 1
            val c = if (!isEnabled) disabled else if (player.getShuffle()) accent else normal
            setTextColor(c)
            compoundDrawableTintList = ColorStateList.valueOf(c)
        }
        val repeatState = player.getRepeat()
        binding.repeatBtn.apply {
            val c = if (repeatState > 0) accent else normal
            setTextColor(c)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, if (repeatState == 2) R.drawable.ic_repeat_one_24dp else R.drawable.ic_repeat_24dp, 0, 0)
            compoundDrawableTintList = ColorStateList.valueOf(c)
        }
        updateSearchTint()
    }

    private fun updateSearchTint() {
        val ctx = binding.root.context
        val c = ContextCompat.getColor(ctx,
            if (binding.searchInput.isVisible) R.color.player_playlist_active else R.color.player_dialog_text)
        binding.searchBtn.setTextColor(c)
        binding.searchBtn.compoundDrawableTintList = ColorStateList.valueOf(c)
    }

    private fun toggleSearch() {
        val show = !binding.searchInput.isVisible
        val imm = binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            binding.searchInput.isVisible = true
            binding.searchInput.requestFocus()
            binding.searchInput.post { imm.showSoftInput(binding.searchInput, InputMethodManager.SHOW_IMPLICIT) }
        } else {
            binding.searchInput.setText("")
            binding.searchInput.isVisible = false
            imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        }
        updateSearchTint()
    }

    /**
     * Teks biasa = saring berdasarkan nama. Hanya angka = lompat ke nomor channel tersebut
     * (daftar tidak disaring, supaya nomor tetap terbaca seperti di TV).
     */
    private fun applyFilter(scrollToPlaying: Boolean = false) {
        val q = query.trim()
        val numeric = q.isNotEmpty() && q.all { it.isDigit() }
        visibleRows = if (q.isEmpty() || numeric) allRows
            else allRows.filter { it.text.contains(q, ignoreCase = true) }
        binding.list.adapter!!.notifyDataSetChanged()

        val lm = binding.list.layoutManager as LinearLayoutManager
        when {
            numeric -> {
                val idx = visibleRows.indexOfFirst { it.number == q.toIntOrNull() }
                if (idx >= 0) lm.scrollToPositionWithOffset(idx, 0)
            }
            scrollToPlaying && q.isEmpty() -> {
                val idx = visibleRows.indexOfFirst { it.selected }
                if (idx >= 0) binding.list.scrollToPosition(idx)
            }
        }
    }

    private fun showInfo() {
        val ctx = binding.root.context
        val pos = allRows.indexOfFirst { it.selected } + 1
        val on = "ON"
        val off = "OFF"
        val rep = when (player.getRepeat()) { 2 -> "1"; 1 -> on; else -> off }
        Toast.makeText(ctx, ctx.getString(R.string.tv_info_format, pos.coerceAtLeast(1), allRows.size,
            if (player.getShuffle()) on else off, rep), Toast.LENGTH_LONG).show()
    }

    private fun pick(row: PlaylistRow) {
        val item = row.item
        if (item != null) listeners?.onItemPicked(item)
        else listeners?.onPendingPicked(row.planIndex)
    }

    private fun clickItem(position: Int) {
        if (position < 0) return
        visibleRows.getOrNull(position)?.let { pick(it) }
    }

    class CustomAdapter(private val parent: PlaylistDialog) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        class ViewHolder(private val parent: PlaylistDialog, view: View) :
            RecyclerView.ViewHolder(view) {
            private val textView: TextView = view.findViewById(android.R.id.text1)
            private val numberView: TextView = view.findViewById(R.id.channelNumber)

            init {
                view.setOnClickListener {
                    parent.clickItem(bindingAdapterPosition)
                }
            }

            fun bind(row: PlaylistRow) {
                val ctx = textView.context
                val dim = row.item == null
                numberView.text = String.format("%03d", row.number)
                textView.text = row.text
                itemView.alpha = if (dim) 0.5f else 1f
                // Baris terpilih: kotak biru ala menu TV; lainnya: baris gelap
                itemView.setBackgroundResource(
                    if (row.selected) R.drawable.bg_channel_selected else R.drawable.bg_channel_row)
                textView.setTypeface(null, if (row.selected) Typeface.BOLD else Typeface.NORMAL)
                val textColor = ContextCompat.getColor(ctx, R.color.player_dialog_text)
                textView.setTextColor(textColor)
                numberView.setTextColor(if (row.selected) textColor else 0xFFC8E600.toInt())
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.dialog_playlist_item, viewGroup, false)
            return ViewHolder(parent, view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.bind(parent.visibleRows[position])
        }

        override fun getItemCount() = parent.visibleRows.size
    }

    companion object {
        private const val TAG = "mpv"
    }
}
'''
write(os.path.join(KT, "PlaylistDialog.kt"), KOTLIN)

# ---- MPVActivity.kt ----
ma = os.path.join(KT, "MPVActivity.kt")
m = rd(ma); changed = False
def rep(old, new, tag):
    global m, changed
    if new in m: print("skip  ", tag); return
    if old not in m: sys.exit("Pola tidak ditemukan (%s) di MPVActivity.kt" % tag)
    m = m.replace(old, new, 1); changed = True; print("patch ", tag)

rep("val buttonView = dialogView.findViewById<Button>(button.idRes)",
    "val buttonView = dialogView.findViewById<View>(button.idRes)", "genericMenu View")
rep("""            v.findViewById<Button>(R.id.audioOnlyBtn)
                .setText(if (forcedAudioOnly) R.string.audio_only_off else R.string.audio_only_on)""",
"""            v.findViewById<android.widget.ImageButton>(R.id.audioOnlyBtn).apply {
                val label = getString(if (forcedAudioOnly) R.string.audio_only_off else R.string.audio_only_on)
                contentDescription = label
                tooltipText = label
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context,
                    if (forcedAudioOnly) R.color.player_playlist_active else R.color.player_dialog_text))
            }""", "audioOnly ikon")
rep("""            setView(impl.buildView(inflater))
            setOnDismissListener { restore() }
            create()
        }
        dialog.show()""",
"""            setView(impl.buildView(inflater))
            setOnDismissListener { restore() }
            create()
        }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()""", "soft input playlist")
if changed: open(ma, "w", encoding="utf-8").write(m)

# ---- progres.md ----
pg = os.path.join(ROOT, "progres.md")
if os.path.exists(pg):
    p = rd(pg)
    if "Cinema UI v2" not in p:
        open(pg, "w", encoding="utf-8").write(p.rstrip("\n") + """

## 2026-10-25 - Cinema UI v2 (menu ikon-saja + pencarian playlist)
- [x] feat(player-ui): menu atas hanya ikon (ImageButton, tooltip tekan-lama, ID tetap); Audio only aktif berwarna biru; chapter prev/next berikon
- [x] feat(player-ui): playlist: tombol Cari (saring nama; ketik angka = lompat ke nomor channel, Enter = putar), penghitung 003/012 dan jam HH:mm di header; nomor channel tetap saat disaring
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: keyboard + resize dialog, filter, lompat nomor, Enter memutar, ikon menu terbaca, audio-only toggle
- [ ] belum dikerjakan: nama grup (header Semua/kiri-kanan) karena entri playlist belum membawa group-title M3U
""")
        print("patch  progres.md")
    else: print("skip   progres.md")
print("selesai.")
