#!/usr/bin/env python3
"""Molina Shield tahap C2 - patch idempotent. Jalankan dari akar repo (molina-studio)."""
import os, sys

ROOT = os.getcwd()
CORE = "core/common/src/main/kotlin/com/molina/core/common"
APPD = "app/src/main/java/com/molina/studio"
MPVD = "feature/mpv/src/main/java/com/molina/cinema"
PROGRES = "progres.md"

SHIELD_PREFS = r"""package com.molina.studio.security

import android.content.Context
import android.content.SharedPreferences
import com.molina.core.common.ShieldProxy
import com.molina.core.common.TrackerList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pengaturan Molina Shield. Default: Shield NYALA, blokir pelacak NYALA, Referer diteruskan
 * (sebagian server IPTV mewajibkannya). Nilainya dicerminkan ke [ShieldProxy] dan [TrackerList].
 * Opsi `http_proxy` global mpv dibaca saat mpv diinisialisasi, jadi mematikan/menyalakan Shield
 * berlaku penuh setelah Refresh Pemutar atau membuka ulang pemutar.
 */
object ShieldPrefs {
    private const val FILE = "molina_shield"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TRACKERS = "block_trackers"
    private const val KEY_REFERER = "strip_referer"

    private var sp: SharedPreferences? = null
    private val _enabled = MutableStateFlow(true)
    private val _trackers = MutableStateFlow(true)
    private val _referer = MutableStateFlow(false)

    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    val blockTrackers: StateFlow<Boolean> = _trackers.asStateFlow()
    val stripReferer: StateFlow<Boolean> = _referer.asStateFlow()

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp = p
        _enabled.value = p.getBoolean(KEY_ENABLED, true)
        _trackers.value = p.getBoolean(KEY_TRACKERS, true)
        _referer.value = p.getBoolean(KEY_REFERER, false)
        apply()
    }

    fun setEnabled(v: Boolean) {
        _enabled.value = v
        sp?.edit()?.putBoolean(KEY_ENABLED, v)?.apply()
        apply()
    }

    fun setBlockTrackers(v: Boolean) {
        _trackers.value = v
        sp?.edit()?.putBoolean(KEY_TRACKERS, v)?.apply()
        apply()
    }

    fun setStripReferer(v: Boolean) {
        _referer.value = v
        sp?.edit()?.putBoolean(KEY_REFERER, v)?.apply()
        apply()
    }

    private fun apply() {
        ShieldProxy.enabled = _enabled.value
        ShieldProxy.stripReferer = _referer.value
        TrackerList.enabled = _trackers.value
    }
}
"""


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


if not os.path.exists(os.path.join(ROOT, CORE, "ShieldProxy.kt")):
    sys.exit("ShieldProxy.kt tidak ada. Jalankan dari akar repo setelah patch C1 terpasang.")

# 1. Inti: sakelar enabled / stripReferer / TrackerList.enabled
patch(CORE + "/ShieldProxy.kt", [
    ("    fun isRunning(): Boolean = server?.isClosed == false\n",
     "    /** Sakelar pengguna (ShieldPrefs). Mati: [proxyUrl] null sehingga mpv tidak memakai proxy. */\n"
     "    @Volatile var enabled: Boolean = true\n\n"
     "    /** Buang header Referer sebelum diteruskan ke server asal (default mati: sebagian IPTV memerlukannya). */\n"
     "    @Volatile var stripReferer: Boolean = false\n\n"
     "    fun isRunning(): Boolean = server?.isClosed == false\n"),
    ("        val s = server ?: return null\n        if (s.isClosed) return null\n",
     "        if (!enabled) return null\n        val s = server ?: return null\n        if (s.isClosed) return null\n"),
    ("                if (name in DROP_HEADERS) continue\n",
     '                if (name in DROP_HEADERS || (stripReferer && name == "referer")) continue\n'),
], "stripReferer")

patch(CORE + "/ShieldPolicy.kt", [
    ("    @Volatile\n    var allowed: Set<String> = emptySet()\n",
     "    @Volatile\n    var allowed: Set<String> = emptySet()\n\n"
     "    /** Sakelar pengguna: mati berarti tidak ada domain yang dianggap pelacak. */\n"
     "    @Volatile\n    var enabled: Boolean = true\n"),
    ("    fun isTracker(host: String): Boolean {\n",
     "    fun isTracker(host: String): Boolean {\n        if (!enabled) return false\n"),
], "var enabled: Boolean = true")

# 2. Aplikasi: ShieldPrefs + inisialisasi
put_file(APPD + "/security/ShieldPrefs.kt", SHIELD_PREFS)

patch(APPD + "/MolinaApp.kt", [
    ("import com.molina.studio.security.PrivacyPrefs\n",
     "import com.molina.studio.security.PrivacyPrefs\nimport com.molina.studio.security.ShieldPrefs\n"),
    ("        ShieldProxy.start()\n",
     "        ShieldPrefs.init(this)\n        ShieldProxy.start()\n"),
], "ShieldPrefs.init")

# 3. Halaman Pengaturan > Keamanan jaringan
patch(APPD + "/settings/NetworkSecurityUi.kt", [
    ("import com.molina.studio.security.PrivacyPrefs\n",
     "import androidx.compose.runtime.remember\n"
     "import com.molina.core.common.ShieldStats\n"
     "import com.molina.studio.security.PrivacyPrefs\n"
     "import com.molina.studio.security.ShieldPrefs\n"),
    ("    val secure by PrivacyPrefs.secureScreen.collectAsState()\n",
     "    val secure by PrivacyPrefs.secureScreen.collectAsState()\n"
     "    val shieldOn by ShieldPrefs.enabled.collectAsState()\n"
     "    val trackersOn by ShieldPrefs.blockTrackers.collectAsState()\n"
     "    val refererOff by ShieldPrefs.stripReferer.collectAsState()\n"
     "    val blocked by ShieldStats.blockedCount.collectAsState()\n"
     "    val recent = remember(blocked) { ShieldStats.recent().takeLast(8).reversed() }\n"),
    ('    Category("Kebijakan URL")\n',
     '    Category("Molina Shield")\n'
     '    SwitchRow(\n'
     '        "Aktifkan Molina Shield",\n'
     '        "Semua lalu lintas http/https pemutar lewat filter lokal: redirect, segmen HLS, dan playlist ke alamat " +\n'
     '            "privat atau loopback diblokir, termasuk DNS yang diarahkan ke alamat privat. Perubahan berlaku penuh " +\n'
     '            "setelah Refresh Pemutar atau membuka ulang pemutar. Tidak mencakup rtmp/rtsp dan yt-dlp.",\n'
     '        shieldOn\n'
     '    ) { ShieldPrefs.setEnabled(it) }\n'
     '    SwitchRow(\n'
     '        "Blokir pelacak dan iklan",\n'
     '        "Menolak domain iklan, analitik, dan telemetri yang dipanggil oleh stream atau playlist.",\n'
     '        trackersOn\n'
     '    ) { ShieldPrefs.setBlockTrackers(it) }\n'
     '    SwitchRow(\n'
     '        "Hapus header Referer",\n'
     '        "Mencegah server mengetahui halaman asal. Matikan bila channel IPTV tertentu menolak diputar.",\n'
     '        refererOff\n'
     '    ) { ShieldPrefs.setStripReferer(it) }\n'
     '    SettingsItem(\n'
     '        "Diblokir sesi ini: $blocked",\n'
     '        if (recent.isEmpty()) "Belum ada permintaan yang diblokir. Hanya host dan alasan yang dicatat, tidak pernah URL lengkap."\n'
     '        else recent.joinToString("\\n") {\n'
     '            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it.atMs)) +\n'
     '                "  " + it.host + " - " + it.reason\n'
     '        }\n'
     '    ) {}\n'
     '    Category("Kebijakan URL")\n'),
], "ShieldPrefs.setEnabled")

# 4. Lencana Shield di bilah atas pemutar
LAYOUT = "feature/mpv/src/main/res/layout/player.xml"
patch(LAYOUT, [
    ('            <ImageButton\n                android:id="@+id/topSearchBtn"',
     '            <!-- Lencana Molina Shield: gembok (https), peringatan (tidak terenkripsi), jumlah blokir. -->\n'
     '            <TextView\n'
     '                android:id="@+id/shieldBadge"\n'
     '                android:layout_width="wrap_content"\n'
     '                android:layout_height="wrap_content"\n'
     '                android:layout_gravity="center_vertical"\n'
     '                android:minHeight="40dp"\n'
     '                android:gravity="center"\n'
     '                android:paddingLeft="8dp"\n'
     '                android:paddingRight="8dp"\n'
     '                android:textColor="@color/tint_normal"\n'
     '                android:textSize="14sp"\n'
     '                tools:text="\U0001F512 0" />\n\n'
     '            <ImageButton\n                android:id="@+id/topSearchBtn"'),
], 'android:id="@+id/shieldBadge"')

ACT = MPVD + "/MPVActivity.kt"
patch(ACT, [
    ("import com.molina.core.common.UrlPolicy\n",
     "import com.molina.core.common.ShieldProxy\nimport com.molina.core.common.ShieldStats\nimport com.molina.core.common.UrlPolicy\n"),
    ("    private val streamScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)\n",
     "    private val streamScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)\n\n"
     "    /** Path entri yang sedang diputar, disimpan agar lencana Shield tidak memanggil mpv dari thread UI. */\n"
     "    @Volatile private var shieldPath: String = \"\"\n"),
    ("            topLockBtn.setOnClickListener { lockUI() }\n",
     "            topLockBtn.setOnClickListener { lockUI() }\n            shieldBadge.setOnClickListener { showShieldInfo() }\n"),
    ("        initListeners()\n\n        gestures = TouchGestures(this)\n",
     "        initListeners()\n"
     "        streamScope.launch { ShieldStats.blockedCount.collect { updateShieldBadge() } }\n\n"
     "        gestures = TouchGestures(this)\n"),
    ("    private fun showToast(msg: String, cancel: Boolean = false) {\n",
     "    /** Lencana Molina Shield: gembok (https), peringatan (http/rtmp/rtsp), perisai (lokal) + jumlah blokir. */\n"
     "    private fun updateShieldBadge() {\n"
     "        val p = shieldPath\n"
     "        val icon = when {\n"
     "            UrlPolicy.scheme(p) == \"https\" -> \"\\uD83D\\uDD12\"\n"
     "            UrlPolicy.isCleartext(p) -> \"\\u26A0\"\n"
     "            else -> \"\\uD83D\\uDEE1\"\n"
     "        }\n"
     "        val n = ShieldStats.blockedCount.value\n"
     "        binding.shieldBadge.text = if (n > 0) \"$icon $n\" else icon\n"
     "    }\n\n"
     "    private fun showShieldInfo() {\n"
     "        val p = shieldPath\n"
     "        val line = when {\n"
     "            UrlPolicy.scheme(p) == \"https\" -> \"Terenkripsi (https).\"\n"
     "            UrlPolicy.isCleartext(p) -> \"TIDAK terenkripsi: isinya bisa dibaca atau diubah di jaringan.\"\n"
     "            else -> \"Sumber lokal.\"\n"
     "        }\n"
     "        val shield = if (ShieldProxy.proxyUrl() != null) \"Molina Shield aktif\" else \"Molina Shield mati\"\n"
     "        showToast(\"$line $shield. Diblokir: ${ShieldStats.blockedCount.value}\", true)\n"
     "    }\n\n"
     "    private fun showToast(msg: String, cancel: Boolean = false) {\n"),
    ("        if (eventId == MpvEvent.MPV_EVENT_FILE_LOADED) {\n            PlaybackLog.onLoaded()\n",
     "        if (eventId == MpvEvent.MPV_EVENT_START_FILE || eventId == MpvEvent.MPV_EVENT_FILE_LOADED) {\n"
     "            shieldPath = MPVLib.getPropertyString(\"path\") ?: \"\"\n"
     "            runOnUiThread { updateShieldBadge() }\n"
     "        }\n\n"
     "        if (eventId == MpvEvent.MPV_EVENT_FILE_LOADED) {\n            PlaybackLog.onLoaded()\n"),
], "updateShieldBadge")

NOTE = """
## 2026-10-25 - Molina Shield C2 (UI dan pengaturan)
- [x] C1 hijau dan lolos uji perangkat (IPTV, pemutaran normal)
- [x] feat(security): ShieldPrefs (sakelar Shield, blokir pelacak, hapus Referer) + bagian Molina Shield di Pengaturan > Keamanan jaringan (jumlah blokir + 8 kejadian terakhir: host dan alasan, tanpa URL) + lencana gembok/peringatan/perisai di bilah atas pemutar - menunggu CI hijau
- [ ] uji perangkat: lencana muncul (https=gembok, http=peringatan), ketuk lencana = info, sakelar Shield mati lalu Refresh Pemutar = pemutaran tetap jalan, blokir 127.0.0.1 tercatat di daftar
- [ ] berikutnya: daftar host LAN tepercaya per host (C3), lalu D (perkecil build FFmpeg + audit native)
"""
s = rd(PROGRES)
if "Molina Shield C2" in s:
    print("sudah dicatat, lewati:", PROGRES)
else:
    wr(PROGRES, s.rstrip("\n") + "\n" + NOTE)
    print("dicatat:", PROGRES)
print("SELESAI")
