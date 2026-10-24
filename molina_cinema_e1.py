#!/usr/bin/env python3
"""Molina Cinema E1 - pencarian judul di player (hasil sampai 50) + pagination NewPipe. Patch idempotent; jalankan dari akar repo."""
import os, sys

ROOT = os.getcwd()
MPVD = "feature/mpv/src/main/java/com/molina/cinema"
RES = "feature/mpv/src/main/res"
PROGRES = "progres.md"

LAYOUT = r'''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="?selectableItemBackground"
    android:minHeight="?listPreferredItemHeightSmall"
    android:orientation="vertical"
    android:paddingLeft="?attr/listPreferredItemPaddingLeft"
    android:paddingTop="8dp"
    android:paddingRight="?attr/listPreferredItemPaddingRight"
    android:paddingBottom="8dp">

    <TextView
        android:id="@+id/search_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:ellipsize="end"
        android:maxLines="2"
        android:textAppearance="?textAppearanceListItemSmall"
        tools:text="Judul video hasil pencarian" />

    <TextView
        android:id="@+id/search_meta"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:alpha="0.6"
        android:ellipsize="end"
        android:singleLine="true"
        android:textAppearance="?android:attr/textAppearanceSmall"
        tools:text="Nama kanal · 3:45" />

</LinearLayout>
'''

STRINGS = r'''    <string name="search_dialog_title">Open URL or search</string>
    <string name="search_query_hint">Paste link or type a title</string>
    <string name="search_searching">Searching…</string>
    <string name="search_unavailable">Search engine is not ready</string>
    <string name="search_no_results">No results for “%1$s”</string>
    <string name="search_results_title">Search results (%1$d)</string>
    <string name="search_close">Close</string>
    <string name="search_added">Added to queue: %1$s</string>
'''

KT_FUNCS = r'''    /** Cari judul lewat resolver (sampai [PlaylistPlan.MAX_ITEMS] hasil) lalu tampilkan daftar pilihan. */
    private fun searchAndShow(query: String, append: Boolean) {
        searchJob?.cancel()
        showToast(getString(R.string.search_searching), true)
        searchJob = streamScope.launch {
            val outcome = kotlinx.coroutines.withContext(Dispatchers.IO) {
                StreamResolvers.get()?.search(query, PlaylistPlan.MAX_ITEMS)
            }
            when (outcome) {
                null -> showToast(getString(R.string.search_unavailable), true)
                is StreamOutcome.Failure -> showToast(getString(StreamLoader.messageRes(outcome.error)), true)
                is StreamOutcome.Success ->
                    if (outcome.value.isEmpty()) showToast(getString(R.string.search_no_results, query), true)
                    else showSearchResults(outcome.value, append)
            }
        }
    }

    private fun searchMeta(item: StreamSearchItem): String {
        val parts = ArrayList<String>(2)
        item.uploader?.let { parts.add(it) }
        item.durationSec?.let { s ->
            val h = s / 3600
            val m = (s % 3600) / 60
            val sec = s % 60
            parts.add(
                if (h > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, sec)
                else String.format(java.util.Locale.ROOT, "%d:%02d", m, sec)
            )
        }
        return parts.joinToString(" \u00B7 ")
    }

    /**
     * Daftar hasil (maks. 50). Mode putar: ketuk = putar sekarang dan hasil lain jadi antrean bergulir
     * (sama seperti Cinema Home). Mode antrean: ketuk = tambahkan satu item, daftar tetap terbuka.
     */
    private fun showSearchResults(items: List<StreamSearchItem>, append: Boolean) {
        val restore = pauseForDialog()
        var chosen = false
        val adapter = object : ArrayAdapter<StreamSearchItem>(this, R.layout.dialog_search_item, R.id.search_title, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = super.getView(position, convertView, parent)
                val item = items[position]
                row.findViewById<TextView>(R.id.search_title).text = item.title
                val meta = row.findViewById<TextView>(R.id.search_meta)
                val line = searchMeta(item)
                meta.text = line
                meta.visibility = if (line.isEmpty()) View.GONE else View.VISIBLE
                return row
            }
        }
        val list = ListView(this)
        list.adapter = adapter
        lateinit var dialog: AlertDialog
        list.setOnItemClickListener { _, _, pos, _ ->
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
        }
        dialog = AlertDialog.Builder(this, R.style.AppTheme_PlayerDialog)
            .setTitle(getString(R.string.search_results_title, items.size))
            .setView(list)
            .setNegativeButton(R.string.search_close) { d, _ -> d.dismiss() }
            .setOnDismissListener {
                restore()
                if (chosen) player.paused = false
            }
            .create()
        dialog.show()
    }

    /** Putar hasil yang diketuk; hasil tonton YouTube lainnya (maks. 50) menjadi antrean bergulir. */
    private fun playSearchResult(items: List<StreamSearchItem>, pos: Int) {
        val picked = items[pos]
        val valid = items.filter { PlaylistPlan.isWatchUrl(it.pageUrl) }
            .distinctBy { it.pageUrl }
            .take(PlaylistPlan.MAX_ITEMS)
        val idx = valid.indexOfFirst { it.pageUrl == picked.pageUrl }
        val plan = if (idx >= 0) {
            runCatching {
                PlaylistPlan.parse(valid.map { it.pageUrl }, valid.map { it.title }, idx, picked.pageUrl)
            }.getOrNull()
        } else null
        rolling = plan?.let { RollingQueue(it) }
        plan?.let { PlaybackLog.onPlaylist(it.urls.size, it.index + 1) }
        PlaybackLog.onRequest(picked.pageUrl)
        loadWeb(picked.pageUrl, MpvLoad.REPLACE)
    }

'''


def rd(p):
    return open(os.path.join(ROOT, p), encoding="utf-8").read()


def wr(p, s):
    full = os.path.join(ROOT, p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    open(full, "w", encoding="utf-8").write(s)


def put_file(p, content):
    full = os.path.join(ROOT, p)
    if os.path.exists(full) and rd(p) == content:
        print("sudah ada, lewati:", p)
        return
    wr(p, content)
    print("ditulis:", p)


def patch(p, edits, marker):
    s = rd(p)
    if marker in s:
        print("sudah dipatch, lewati:", p)
        return
    for old, new in edits:
        if s.count(old) != 1:
            sys.exit("GAGAL: pola harus tepat 1 kali di %s (ditemukan %d):\n%s" % (p, s.count(old), old))
        s = s.replace(old, new, 1)
    wr(p, s)
    print("dipatch:", p)


if not os.path.exists(os.path.join(ROOT, MPVD, "MPVActivity.kt")):
    sys.exit("Jalankan dari akar repo molina-studio.")

# 1. Layout baris hasil + string
put_file(RES + "/layout/dialog_search_item.xml", LAYOUT)
patch(RES + "/values/strings.xml", [
    ('    <string name="search_url_queue">Add to queue</string>\n',
     '    <string name="search_url_queue">Add to queue</string>\n' + STRINGS),
], 'name="search_dialog_title"')

# 2. StreamLoader: bedakan kata kunci pencarian dari URL
patch(MPVD + "/StreamLoader.kt", [
    ("    /** [onResolving] dipanggil di thread utama hanya bila resolver benar-benar akan dipakai. */\n",
     "    private val BARE_HOSTS = listOf(\"www.\", \"youtu.be/\", \"youtube.com/\", \"m.youtube.com/\", \"music.youtube.com/\")\n\n"
     "    /** Teks bebas (bukan URL, bukan path berkas, bukan host tanpa skema) dianggap kata kunci pencarian judul. */\n"
     "    fun isQuery(raw: String): Boolean {\n"
     "        val t = raw.trim()\n"
     "        return t.isNotEmpty() && !t.contains(\"://\") && !t.startsWith(\"/\") &&\n"
     "            BARE_HOSTS.none { t.startsWith(it, ignoreCase = true) }\n"
     "    }\n\n"
     "    /** [onResolving] dipanggil di thread utama hanya bila resolver benar-benar akan dipakai. */\n"),
], "fun isQuery(")

# 3. Dialog URL: mode pencarian (opt-in; pemakai lain tidak berubah)
patch(MPVD + "/Utils.kt", [
    ("    class OpenUrlDialog(context: Context, themeRes: Int = 0) {\n",
     "    class OpenUrlDialog(context: Context, themeRes: Int = 0, private val allowQuery: Boolean = false) {\n"),
    ("            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI\n",
     "            editText.inputType = if (allowQuery) InputType.TYPE_CLASS_TEXT\n"
     "                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI\n"),
    ("            editText.hint = context.getString(R.string.search_url_hint)\n",
     "            editText.hint = context.getString(if (allowQuery) R.string.search_query_hint else R.string.search_url_hint)\n"),
    ("                setTitle(R.string.action_open_url)\n",
     "                setTitle(if (allowQuery) R.string.search_dialog_title else R.string.action_open_url)\n"),
    ("        private fun validate(text: String): Boolean {\n",
     "        private fun validate(text: String): Boolean {\n"
     "            if (allowQuery && StreamLoader.isQuery(text)) return true\n"),
], "allowQuery")

# 4. Player: alur cari -> daftar hasil -> putar/antre
patch(MPVD + "/MPVActivity.kt", [
    ("import android.widget.Button\n",
     "import android.widget.ArrayAdapter\nimport android.widget.Button\nimport android.widget.ListView\nimport android.widget.TextView\n"),
    ("import com.molina.core.common.StreamHeaders\n",
     "import com.molina.core.common.StreamHeaders\nimport com.molina.core.common.StreamOutcome\n"
     "import com.molina.core.common.StreamResolvers\nimport com.molina.core.common.StreamSearchItem\n"),
    ("    private var rolling: RollingQueue? = null\n",
     "    private var rolling: RollingQueue? = null\n\n    private var searchJob: kotlinx.coroutines.Job? = null\n"),
    ("        val helper = Utils.OpenUrlDialog(this, R.style.AppTheme_PlayerDialog)\n",
     "        val helper = Utils.OpenUrlDialog(this, R.style.AppTheme_PlayerDialog, allowQuery = true)\n"),
    ("            builder.setPositiveButton(R.string.search_url_play) { _, _ ->\n"
     "                playNow = true\n"
     "                PlaybackLog.onRequest(helper.text)\n"
     "                loadWeb(helper.text, MpvLoad.REPLACE)\n"
     "            }\n",
     "            builder.setPositiveButton(R.string.search_url_play) { _, _ ->\n"
     "                if (StreamLoader.isQuery(helper.text)) {\n"
     "                    searchAndShow(helper.text, append = false)\n"
     "                } else {\n"
     "                    playNow = true\n"
     "                    PlaybackLog.onRequest(helper.text)\n"
     "                    loadWeb(helper.text, MpvLoad.REPLACE)\n"
     "                }\n"
     "            }\n"),
    ("            builder.setNeutralButton(R.string.search_url_queue) { _, _ ->\n"
     "                PlaybackLog.onRequest(helper.text)\n"
     "                loadWeb(helper.text, MpvLoad.APPEND) {\n"
     "                    showToast(getString(R.string.notice_file_appended), true)\n"
     "                }\n"
     "            }\n",
     "            builder.setNeutralButton(R.string.search_url_queue) { _, _ ->\n"
     "                if (StreamLoader.isQuery(helper.text)) {\n"
     "                    searchAndShow(helper.text, append = true)\n"
     "                } else {\n"
     "                    PlaybackLog.onRequest(helper.text)\n"
     "                    loadWeb(helper.text, MpvLoad.APPEND) {\n"
     "                        showToast(getString(R.string.notice_file_appended), true)\n"
     "                    }\n"
     "                }\n"
     "            }\n"),
    ("    private fun openPlaylistMenu(restore: StateRestoreCallback) {\n",
     KT_FUNCS + "    private fun openPlaylistMenu(restore: StateRestoreCallback) {\n"),
], "fun searchAndShow(")

# 5. NewPipe: ambil beberapa halaman hasil sampai [limit] video
NP = "engine/newpipe/src/main/kotlin/com/molina/engine/newpipe/NewPipeStreamResolver.kt"
patch(NP, [
    ("import org.schabi.newpipe.extractor.NewPipe\n",
     "import org.schabi.newpipe.extractor.InfoItem\nimport org.schabi.newpipe.extractor.NewPipe\n"),
    ("                SearchInfo.getInfo(service, handler).relatedItems.orEmpty()\n",
     "                val first = SearchInfo.getInfo(service, handler)\n"
     "                val all = ArrayList<InfoItem>(first.relatedItems.orEmpty())\n"
     "                var page = first.nextPage\n"
     "                var pages = 1\n"
     "                // Satu halaman YouTube hanya ~20 entri campuran; lanjutkan sampai [limit] video terkumpul.\n"
     "                try {\n"
     "                    while (page != null && pages < 6 && all.count { it is StreamInfoItem } < limit) {\n"
     "                        val more = SearchInfo.getMoreItems(service, handler, page)\n"
     "                        all.addAll(more.items.orEmpty())\n"
     "                        page = more.nextPage\n"
     "                        pages++\n"
     "                    }\n"
     "                } catch (e: InterruptedException) {\n"
     "                    throw e\n"
     "                } catch (e: Exception) {\n"
     "                    // halaman lanjutan gagal: pakai hasil yang sudah terkumpul\n"
     "                }\n"
     "                all\n"),
    ("                .filterIsInstance<StreamInfoItem>()\n                .map { i ->\n",
     "                .filterIsInstance<StreamInfoItem>()\n                .distinctBy { it.url }\n                .map { i ->\n"),
], "getMoreItems")

# 6. Cinema Home: 15 -> 50 hasil, jumlah di judul
HOME = "app/src/main/java/com/molina/studio/cinema/CinemaHome.kt"
patch(HOME, [
    ("import com.molina.core.common.StreamError\n",
     "import com.molina.core.common.PlaylistPlan\nimport com.molina.core.common.StreamError\n"),
    ("StreamResolvers.get()?.search(q, 15)", "StreamResolvers.get()?.search(q, PlaylistPlan.MAX_ITEMS)"),
    ('title = { Text("Hasil pencarian") },', 'title = { Text("Hasil pencarian (${results.size})") },'),
], "PlaylistPlan.MAX_ITEMS")

NOTE = """
## 2026-10-25 - Cinema E1 (pencarian judul di player, hasil sampai 50)
- [x] D1 di-push (commit 8f2cf73); pin native baru menyusul setelah workflow Native mpv dijalankan
- [x] feat(player): ikon cari di player menerima judul (bukan hanya URL): Play = daftar hasil lalu putar yang diketuk dan sisanya jadi antrean bergulir; Add to queue = tambahkan item yang diketuk (daftar tetap terbuka). Daftar menampilkan judul, kanal, durasi, sampai 50 hasil
- [x] fix(search): NewPipe hanya mengambil 1 halaman (~10-20 video); kini lanjut halaman berikutnya (maks. 6) sampai 50 video, hasil tanpa duplikat; Cinema Home memakai batas yang sama (sebelumnya 15)
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: ketik judul di dialog player, cek 50 hasil, ketuk = putar + antrean berlanjut, Add to queue, URL biasa tetap jalan, Open URL di layar utama/pemilih berkas tidak berubah
- [ ] batas: antrean bergulir hanya untuk URL tonton YouTube; pencarian lewat yt-dlp (ytsearchN) tidak diubah
"""
t = rd(PROGRES)
if "Cinema E1" in t:
    print("sudah dicatat, lewati:", PROGRES)
else:
    wr(PROGRES, t.rstrip("\n") + "\n" + NOTE)
    print("dicatat:", PROGRES)
print("SELESAI")
