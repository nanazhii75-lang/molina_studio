#!/usr/bin/env python3
"""Molina Cinema: menu atas berikon + Playlist ala daftar channel TV. Idempotent."""
import os, re, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
MOD = os.path.join(ROOT, "feature/mpv/src/main")
RES = os.path.join(MOD, "res")
KT = os.path.join(MOD, "java/com/molina/cinema")
if not os.path.isdir(RES):
    sys.exit(f"Bukan root repo molina-studio: {ROOT}")

def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path) and open(path, encoding="utf-8").read() == text:
        print("skip  ", os.path.relpath(path, ROOT)); return
    open(path, "w", encoding="utf-8").write(text)
    print("tulis ", os.path.relpath(path, ROOT))

def vec(path_data):
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp"\n'
            '    android:viewportWidth="24" android:viewportHeight="24">\n'
            f'  <path android:fillColor="#FFFFFFFF" android:pathData="{path_data}"/>\n</vector>\n')

ICONS = {
 "ic_menu_audio_file": "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z",
 "ic_menu_subtitles": "M20,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2zM4,12h4v2H4v-2zM14,18H4v-2h10v2zM20,18h-4v-2h4v2zM20,14H10v-2h10v2z",
 "ic_menu_playlist": "M3,13h2v-2H3v2zM3,17h2v-2H3v2zM3,9h2V7H3v2zM7,13h14v-2H7v2zM7,17h14v-2H7v2zM7,7v2h14V7H7z",
 "ic_menu_headset": "M12,1c-4.97,0 -9,4.03 -9,9v7c0,1.66 1.34,3 3,3h3v-8H5v-2c0,-3.87 3.13,-7 7,-7s7,3.13 7,7v2h-4v8h3c1.66,0 3,-1.34 3,-3v-7c0,-4.97 -4.03,-9 -9,-9z",
 "ic_menu_audiotrack": "M12,3v9.28c-0.47,-0.17 -0.97,-0.28 -1.5,-0.28C8.01,12 6,14.01 6,16.5S8.01,21 10.5,21c2.31,0 4.2,-1.75 4.45,-4H15V6h4V3h-7z",
 "ic_menu_chapters": "M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z",
 "ic_menu_tune": "M3,17v2h6v-2H3zM3,5v2h10V5H3zM13,21v-2h8v-2h-8v-2h-2v6h2zM7,9v2H3v2h4v2h2V9H7zM21,13v-2H11v2h10zM15,9h2V7h4V5h-4V3h-2v6z",
 "ic_menu_rotate": "M16.48,2.52c3.27,1.55 5.61,4.72 5.97,8.48h1.5C23.44,4.84 18.29,0 12,0l-0.66,0.03 3.81,3.81 1.33,-1.32zM10.23,1.75c-0.59,-0.59 -1.54,-0.59 -2.12,0L1.75,8.11c-0.59,0.59 -0.59,1.54 0,2.12l12.02,12.02c0.59,0.59 1.54,0.59 2.12,0l6.36,-6.36c0.59,-0.59 0.59,-1.54 0,-2.12L10.23,1.75zM14.83,21.19L2.81,9.17l6.36,-6.36 12.02,12.02 -6.36,6.36zM7.52,21.48C4.25,19.94 1.91,16.76 1.55,13H0.05C0.56,19.16 5.71,24 12,24l0.66,-0.03 -3.81,-3.81 -1.33,1.32z",
 "ic_menu_close": "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
 "ic_menu_info": "M11,7h2v2h-2zM11,11h2v6h-2zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z",
 "ic_menu_grid": "M3,3v8h8V3H3zM9,9H5V5h4v4zM3,13v8h8v-8H3zM9,19H5v-4h4v4zM13,3v8h8V3h-8zM19,9h-4V5h4v4zM13,13v8h8v-8h-8zM19,19h-4v-4h4v4z",
 "ic_menu_file_open": "M6,2c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6H6zM13,9V3.5L18.5,9H13z",
 "ic_menu_link": "M3.9,12c0,-1.71 1.39,-3.1 3.1,-3.1h4V7H7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5h4v-1.9H7c-1.71,0 -3.1,-1.39 -3.1,-3.1zM8,13h8v-2H8v2zM17,7h-4v1.9h4c1.71,0 3.1,1.39 3.1,3.1s-1.39,3.1 -3.1,3.1h-4V17h4c2.76,0 5,-2.24 5,-5s-2.24,-5 -5,-5z",
}
for name, d in ICONS.items():
    write(os.path.join(RES, "drawable", name + ".xml"), vec(d))

# ---------- warna, gaya, string ----------
write(os.path.join(RES, "drawable/bg_channel_selected.xml"),
'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="270" android:startColor="#3D8BFF" android:endColor="#0F4FC4" />
    <corners android:radius="4dp" />
    <stroke android:width="1dp" android:color="#8DBBFF" />
</shape>
''')
write(os.path.join(RES, "drawable/bg_channel_row.xml"),
'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="270" android:startColor="#1C2430" android:endColor="#121821" />
    <corners android:radius="4dp" />
    <stroke android:width="1dp" android:color="#26FFFFFF" />
</shape>
''')
write(os.path.join(RES, "drawable/bg_tv_action_key.xml"),
'''<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true"><shape><solid android:color="#33FFFFFF" /><corners android:radius="6dp" /></shape></item>
    <item android:state_focused="true"><shape><solid android:color="#264DA3FF" /><corners android:radius="6dp" /></shape></item>
    <item><shape><solid android:color="#00000000" /><corners android:radius="6dp" /></shape></item>
</selector>
''')
write(os.path.join(RES, "values/molina_menu_styles.xml"),
'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Item menu atas: ikon di kiri teks, rata kiri, ringkas -->
    <style name="Widget.Molina.MenuItem" parent="Widget.AppCompat.Button.Borderless">
        <item name="android:textAllCaps">false</item>
        <item name="android:gravity">start|center_vertical</item>
        <item name="android:drawablePadding">12dp</item>
        <item name="android:drawableTint">@color/player_dialog_text</item>
        <item name="android:textColor">@color/player_dialog_text</item>
        <item name="android:textSize">14sp</item>
        <item name="android:minHeight">48dp</item>
        <item name="android:paddingStart">16dp</item>
        <item name="android:paddingEnd">12dp</item>
        <item name="android:maxLines">1</item>
        <item name="android:ellipsize">end</item>
    </style>

    <!-- Tombol aksi ala remote TV: ikon di atas, label kecil di bawah -->
    <style name="Widget.Molina.TvKey" parent="Widget.AppCompat.Button.Borderless">
        <item name="android:background">@drawable/bg_tv_action_key</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:textSize">10sp</item>
        <item name="android:textColor">@color/player_dialog_text</item>
        <item name="android:drawableTint">@color/player_dialog_text</item>
        <item name="android:drawablePadding">2dp</item>
        <item name="android:minWidth">0dp</item>
        <item name="android:minHeight">0dp</item>
        <item name="android:paddingTop">6dp</item>
        <item name="android:paddingBottom">6dp</item>
        <item name="android:maxLines">1</item>
    </style>
</resources>
''')
strings = os.path.join(RES, "values/strings.xml")
s = open(strings, encoding="utf-8").read()
if "tv_key_exit" not in s:
    add = ('    <string name="tv_key_exit" translatable="false">Exit</string>\n'
           '    <string name="tv_key_info" translatable="false">Info</string>\n'
           '    <string name="tv_key_menu" translatable="false">Menu</string>\n'
           '    <string name="tv_key_shuffle" translatable="false">Shuffle</string>\n'
           '    <string name="tv_key_repeat" translatable="false">Repeat</string>\n'
           '    <string name="tv_info_format" translatable="false">Channel %1$03d dari %2$03d  |  Shuffle: %3$s  |  Repeat: %4$s</string>\n')
    s = s.replace("</resources>", add + "</resources>")
    open(strings, "w", encoding="utf-8").write(s); print("patch ", "values/strings.xml")
else:
    print("skip   values/strings.xml")

# ---------- 1) Menu atas ----------
def btn(id_, text, icon, extra=""):
    return (f'<Button\n        android:id="@+id/{id_}"\n        style="@style/Widget.Molina.MenuItem"\n'
            f'        android:layout_width="{extra or "match_parent"}"\n        android:layout_height="wrap_content"\n'
            f'        android:drawableStart="@drawable/{icon}"\n        android:text="@string/{text}" />')
def row(id_, *items):
    inner = "\n\n".join(
        b.replace('android:layout_width="match_parent"', 'android:layout_width="0dp"\n        android:layout_weight="1"')
        for b in items)
    rid = f'\n        android:id="@+id/{id_}"' if id_ else ""
    return (f'<LinearLayout{rid}\n        android:layout_width="match_parent"\n        android:layout_height="wrap_content"\n'
            f'        android:orientation="horizontal">\n\n{inner}\n\n    </LinearLayout>')

def chap_small(id_, text):
    return (f'<Button\n            android:id="@+id/{id_}"\n            style="@style/Widget.Molina.MenuItem"\n'
            f'            android:layout_width="0dp"\n            android:layout_height="wrap_content"\n'
            f'            android:layout_weight="1"\n            android:gravity="center"\n            android:paddingStart="8dp"\n'
            f'            android:paddingEnd="8dp"\n            android:background="@drawable/alpha_lighten"\n'
            f'            android:text="@string/{text}" />')

top = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Molina: menu atas ringkas dengan ikon. ID tombol sama seperti sebelumnya (MPVActivity.openTopMenu). -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:paddingTop="8dp"
    android:paddingBottom="8dp">

    <!-- Since this dialog does not scroll, don't add too many buttons here! -->

    {row(None, btn("audioBtn","open_external_audio","ic_menu_audio_file"), btn("subBtn","open_external_sub","ic_menu_subtitles"))}

    {btn("playlistBtn","action_playlist","ic_menu_playlist")}

    {row("rowMode", btn("backgroundBtn","resume_bg_playback","ic_menu_headset"), btn("audioOnlyBtn","audio_only_on","ic_menu_audiotrack"))}

    <LinearLayout
        android:id="@+id/rowChapter"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <Button
            android:id="@+id/chapterBtn"
            style="@style/Widget.Molina.MenuItem"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="3"
            android:drawableStart="@drawable/ic_menu_chapters"
            android:text="@string/chapter_button" />

        {chap_small("chapterPrev","dialog_prev")}

        {chap_small("chapterNext","dialog_next")}

    </LinearLayout>

    {row(None, btn("advancedBtn","advanced_menu","ic_menu_tune"), btn("orientationBtn","switch_orientation","ic_menu_rotate"))}

</LinearLayout>
'''
write(os.path.join(RES, "layout/dialog_top_menu.xml"), top)

# ---------- 2) Playlist ala TV ----------
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

    <TextView
        android:id="@+id/headerTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="24dp"
        android:layout_marginTop="16dp"
        android:text="@string/action_playlist"
        android:textAppearance="@style/TextAppearance.AppCompat.Title" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:padding="8dp"
        app:layoutManager="LinearLayoutManager"
        tools:listitem="@layout/dialog_playlist_item" />

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

item = '''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/channelRow"
    android:layout_width="match_parent"
    android:layout_height="44dp"
    android:layout_marginVertical="2dp"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="10dp"
    android:paddingEnd="10dp"
    android:background="@drawable/bg_channel_row">

    <TextView
        android:id="@+id/channelNumber"
        android:layout_width="44dp"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:textSize="14sp"
        android:textStyle="bold"
        tools:text="001" />

    <TextView
        android:id="@android:id/text1"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:ellipsize="middle"
        android:singleLine="true"
        android:textSize="15sp"
        tools:text="TRANS7 DIGITAL" />

</LinearLayout>
'''
write(os.path.join(RES, "layout/dialog_playlist_item.xml"), item)

pd = os.path.join(KT, "PlaylistDialog.kt")
src = open(pd, encoding="utf-8").read()
if "channelNumber" in src:
    print("skip   PlaylistDialog.kt")
else:
    def sub(old, new, cnt=1):
        global src
        if old not in src: sys.exit("Pola tidak ditemukan di PlaylistDialog.kt:\n" + old)
        src = src.replace(old, new, cnt)
    sub("import android.view.ViewGroup\nimport android.widget.TextView\n",
        "import android.view.ViewGroup\nimport android.widget.TextView\nimport android.widget.Toast\n")
    sub("        fun onPendingPicked(planIndex: Int) {}\n",
        "        fun onPendingPicked(planIndex: Int) {}\n        /** Tombol Exit pada action bar. */\n        fun onExit() {}\n        /** Tombol Menu pada action bar (buka menu atas). */\n        fun onMenu() {}\n")
    sub("        binding.shuffleBtn.setOnClickListener {",
        "        binding.exitBtn.setOnClickListener { listeners?.onExit() }\n        binding.menuBtn.setOnClickListener { listeners?.onMenu() }\n        binding.infoBtn.setOnClickListener { showInfo() }\n\n        binding.shuffleBtn.setOnClickListener {")
    sub("""        binding.shuffleBtn.apply {
            isEnabled = playlist.size > 1
            imageTintList = if (isEnabled)
                ColorStateList.valueOf(if (shuffleState) accent else normal)
            else
                ColorStateList.valueOf(disabled)
        }""", """        binding.shuffleBtn.apply {
            isEnabled = playlist.size > 1
            val c = if (!isEnabled) disabled else if (shuffleState) accent else normal
            setTextColor(c)
            compoundDrawableTintList = ColorStateList.valueOf(c)
        }""")
    sub("""        binding.repeatBtn.apply {
            imageTintList = ColorStateList.valueOf(if (repeatState > 0) accent else normal)
            setImageResource(if (repeatState == 2) R.drawable.ic_repeat_one_24dp else R.drawable.ic_repeat_24dp)
        }""", """        binding.repeatBtn.apply {
            val c = if (repeatState > 0) accent else normal
            setTextColor(c)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, if (repeatState == 2) R.drawable.ic_repeat_one_24dp else R.drawable.ic_repeat_24dp, 0, 0)
            compoundDrawableTintList = ColorStateList.valueOf(c)
        }""")
    sub("    private fun clickItem(position: Int) {",
        """    private fun showInfo() {
        val ctx = binding.root.context
        val total = playlist.size + pendingRows.size
        val pos = playlist.indexOfFirst { it.index == selectedIndex } + 1
        val on = "ON"
        val off = "OFF"
        val rep = when (player.getRepeat()) { 2 -> "1"; 1 -> on; else -> off }
        Toast.makeText(ctx, ctx.getString(R.string.tv_info_format, pos.coerceAtLeast(1), total,
            if (player.getShuffle()) on else off, rep), Toast.LENGTH_LONG).show()
    }

    private fun clickItem(position: Int) {""")
    sub("""            private val textView: TextView

            init {
                textView = view.findViewById(android.R.id.text1)""",
        """            private val textView: TextView = view.findViewById(android.R.id.text1)
            private val numberView: TextView = view.findViewById(R.id.channelNumber)

            init {""")
    sub("""            fun bind(text: String, selected: Boolean, dim: Boolean = false) {
                textView.text = text
                textView.alpha = if (dim) 0.5f else 1f
                textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
                textView.setTextColor(ContextCompat.getColor(textView.context,
                    if (selected) R.color.player_playlist_active else R.color.player_dialog_text))
            }""", """            fun bind(number: Int, text: String, selected: Boolean, dim: Boolean = false) {
                val ctx = textView.context
                numberView.text = String.format("%03d", number)
                textView.text = text
                itemView.alpha = if (dim) 0.5f else 1f
                // Baris terpilih: kotak biru ala menu TV; lainnya: baris gelap
                itemView.setBackgroundResource(
                    if (selected) R.drawable.bg_channel_selected else R.drawable.bg_channel_row)
                textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
                val textColor = ContextCompat.getColor(ctx, R.color.player_dialog_text)
                textView.setTextColor(textColor)
                // Nomor: kuning hijau ala daftar channel; saat terpilih jadi putih
                numberView.setTextColor(if (selected) textColor else 0xFFC8E600.toInt())
            }""")
    sub("viewHolder.bind(text, item.index == parent.selectedIndex)",
        "viewHolder.bind(position + 1, text, item.index == parent.selectedIndex)")
    sub("viewHolder.bind(parent.pendingRows[position - parent.playlist.size].title, false, true)",
        "viewHolder.bind(position + 1, parent.pendingRows[position - parent.playlist.size].title, false, true)")
    open(pd, "w", encoding="utf-8").write(src); print("patch  PlaylistDialog.kt")

ma = os.path.join(KT, "MPVActivity.kt")
m = open(ma, encoding="utf-8").read()
if "override fun onMenu()" in m:
    print("skip   MPVActivity.kt")
else:
    old = """            override fun onPendingPicked(planIndex: Int) {
                dialog.dismiss()
                jumpToPending(planIndex)
            }
"""
    if old not in m: sys.exit("Pola onPendingPicked tidak ditemukan di MPVActivity.kt")
    new = old + """
            override fun onExit() = dialog.dismiss()

            override fun onMenu() {
                dialog.dismiss()
                openTopMenu()
            }
"""
    open(ma, "w", encoding="utf-8").write(m.replace(old, new, 1)); print("patch  MPVActivity.kt")

# ---------- progres.md ----------
pg = os.path.join(ROOT, "progres.md")
if os.path.exists(pg):
    p = open(pg, encoding="utf-8").read()
    if "Cinema UI menu atas + playlist TV" not in p:
        p = p.rstrip("\n") + """

## 2026-10-25 - Cinema UI menu atas + playlist TV
- [x] feat(player-ui): dialog_top_menu kini berikon (ikon di kiri teks, 2 kolom ringkas, ID tombol tetap); style Widget.Molina.MenuItem dan Widget.Molina.TvKey
- [x] feat(player-ui): Playlist ala daftar channel TV: nomor 001/002/..., baris terpilih kotak biru bergradasi, Shuffle dan Repeat pindah ke footer, action bar Shuffle/Repeat/Info/Menu/Exit, PICK FILE dan OPEN URL berikon tetap ada
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: tampil di API 26, Shuffle/Repeat berubah warna, Info (toast), Menu membuka menu atas, Exit menutup, klik item = putar, antrean pending tampil redup
"""
        open(pg, "w", encoding="utf-8").write(p); print("patch  progres.md")
    else:
        print("skip   progres.md")
print("selesai.")
