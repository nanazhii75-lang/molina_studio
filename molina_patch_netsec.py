import os, sys
root = os.getcwd()
P = lambda *a: os.path.join(root, *a)
rd = lambda p: open(p, encoding='utf-8').read()
def wr(p, s): open(p, 'w', encoding='utf-8').write(s)

def need(p):
    if not os.path.isfile(p):
        sys.exit("Jalankan dari root repo; tidak ditemukan: " + p)

def put(path, content, label):
    if os.path.isfile(path) and rd(path) == content:
        print("sama    :", label)
    else:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        wr(path, content); print("ditulis :", label)

def edit(path, old, new, label, marker):
    s = rd(path)
    if marker in s:
        print("sudah   :", label); return
    if s.count(old) != 1:
        sys.exit("pola tidak ditemukan/ganda: " + label)
    wr(path, s.replace(old, new)); print("diedit  :", label)
C = 'core/common/src/main/kotlin/com/molina/core/common/'
S = 'app/src/main/java/com/molina/studio/'
M = 'feature/mpv/src/main/java/com/molina/cinema/'
for f in (C+'YtdlpCommand.kt', C+'YtdlpFetch.kt', M+'MpvLoad.kt', M+'MPVView.kt', S+'MolinaApp.kt', S+'stream/StreamEnginePrefs.kt', S+'settings/SettingsScreen.kt', 'app/src/main/AndroidManifest.xml', 'progres.md'):
    need(P(f))
put(P(C+'UrlPolicy.kt'), r'''package com.molina.core.common

/** Pengaturan kebijakan jaringan yang dibaca lintas modul. :app mengisinya saat start. */
object NetworkPolicy {
    /** Izinkan alamat jaringan lokal (NAS, DLNA, Jellyfin). Default mati: jaringan dianggap tidak tepercaya. */
    @Volatile
    var allowLan: Boolean = false
}

/**
 * Kebijakan URL untuk semua yang akan dibuka mpv atau yt-dlp. Jaringan dianggap rawan: URL dari
 * tautan, daftar putar, atau hasil resolver bisa diracik untuk membaca berkas lokal (file://,
 * concat:, edl://, fd://), memindai jaringan rumah (SSRF), atau menyamar lewat userinfo.
 * Daftar putih skema, bukan daftar hitam. Murni, tanpa Android.
 *
 * Batas yang diketahui: nama host publik yang DNS-nya mengarah ke alamat privat (DNS rebinding)
 * tidak terdeteksi di sini karena tidak ada resolusi DNS.
 */
object UrlPolicy {
    const val MAX_URL_LENGTH = 4096

    enum class Reason(val message: String) {
        EMPTY("URL kosong"),
        TOO_LONG("URL terlalu panjang"),
        CONTROL_CHARS("URL memuat karakter terlarang"),
        BAD_SCHEME("skema URL tidak diizinkan"),
        MALFORMED("URL tidak valid"),
        USERINFO("URL memuat nama pengguna/kata sandi"),
        NO_HOST("URL tanpa host"),
        BAD_PORT("port tidak valid"),
        NON_ASCII_HOST("host non-ASCII ditolak (rawan penyamaran)"),
        LOOPBACK("alamat lokal perangkat atau metadata ditolak"),
        PRIVATE_ADDRESS("alamat jaringan lokal ditolak (aktifkan di Keamanan jaringan)"),
    }

    sealed interface Verdict {
        object Allowed : Verdict
        data class Blocked(val reason: Reason) : Verdict
    }

    private val REMOTE_SCHEMES = setOf("http", "https", "rtmp", "rtmps", "rtsp")
    private val CLEARTEXT_SCHEMES = setOf("http", "rtmp", "rtsp")
    private val LOCAL_SCHEMES = setOf("file", "content")
    private val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")
    private val NUMERIC_LABEL = Regex("^(0[xX][0-9a-fA-F]*|[0-9]+)$")
    private val LAN_SUFFIXES = listOf(".local", ".internal", ".lan", ".home.arpa", ".localdomain", ".intranet", ".corp", ".home")

    fun scheme(url: String): String? = SCHEME.find(url.trim())?.groupValues?.get(1)?.lowercase()

    /** True bila URL ini dikirim tanpa enkripsi (http, rtmp, rtsp): bagus untuk peringatan di UI. */
    fun isCleartext(url: String): Boolean = scheme(url) in CLEARTEXT_SCHEMES

    fun check(input: String, allowLan: Boolean = NetworkPolicy.allowLan): Verdict {
        val u = input.trim()
        if (u.isEmpty()) return blocked(Reason.EMPTY)
        if (u.length > MAX_URL_LENGTH) return blocked(Reason.TOO_LONG)
        if (u.any { it.isISOControl() }) return blocked(Reason.CONTROL_CHARS)
        // Jalur berkas lokal absolut (pemilih berkas) boleh memuat spasi dan unicode.
        if (u.startsWith("/")) return Verdict.Allowed
        if (u.any { it.isWhitespace() }) return blocked(Reason.CONTROL_CHARS)

        val scheme = SCHEME.find(u)?.groupValues?.get(1)?.lowercase() ?: return blocked(Reason.BAD_SCHEME)
        if (scheme in LOCAL_SCHEMES) return Verdict.Allowed
        if (scheme !in REMOTE_SCHEMES) return blocked(Reason.BAD_SCHEME)

        val rest = u.substring(scheme.length + 1)
        if (!rest.startsWith("//")) return blocked(Reason.MALFORMED)
        val authority = rest.substring(2).takeWhile { it != '/' && it != '?' && it != '#' }
        if (authority.isEmpty()) return blocked(Reason.NO_HOST)
        if (authority.contains('\\')) return blocked(Reason.MALFORMED)
        if (authority.contains('@')) return blocked(Reason.USERINFO)

        val host: String
        val port: String
        if (authority.startsWith("[")) {
            val end = authority.indexOf(']')
            if (end < 0) return blocked(Reason.MALFORMED)
            host = authority.substring(1, end)
            val after = authority.substring(end + 1)
            if (after.isNotEmpty() && !after.startsWith(":")) return blocked(Reason.MALFORMED)
            port = after.removePrefix(":")
        } else {
            val c = authority.indexOf(':')
            if (c >= 0) {
                if (authority.indexOf(':', c + 1) >= 0) return blocked(Reason.MALFORMED)
                host = authority.substring(0, c)
                port = authority.substring(c + 1)
            } else {
                host = authority
                port = ""
            }
        }
        if (host.isEmpty()) return blocked(Reason.NO_HOST)
        if (port.isNotEmpty()) {
            if (port.length > 5 || !port.all { it in '0'..'9' }) return blocked(Reason.BAD_PORT)
            if (port.toInt() !in 1..65535) return blocked(Reason.BAD_PORT)
        }
        return if (authority.startsWith("[")) checkV6(host, allowLan) else checkName(host, allowLan)
    }

    // ---------- nama host dan IPv4 ----------

    private fun checkName(raw: String, allowLan: Boolean): Verdict {
        if (raw.any { it.code > 127 }) return blocked(Reason.NON_ASCII_HOST)
        val h = raw.lowercase().trimEnd('.')
        if (h.isEmpty()) return blocked(Reason.NO_HOST)
        if (!h.all { it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '.' || it == '_' }) {
            return blocked(Reason.MALFORMED)
        }
        val labels = h.split('.')
        if (labels.any { it.isEmpty() }) return blocked(Reason.MALFORMED)

        if (NUMERIC_LABEL.matches(labels.last())) {
            val addr = parseLooseV4(labels) ?: return blocked(Reason.MALFORMED)
            return classifyV4(addr, allowLan)
        }
        if (h == "localhost" || h.endsWith(".localhost")) return blocked(Reason.LOOPBACK)
        val lanName = !h.contains('.') || LAN_SUFFIXES.any { h.endsWith(it) }
        if (lanName && !allowLan) return blocked(Reason.PRIVATE_ADDRESS)
        return Verdict.Allowed
    }

    /** Pembacaan longgar ala inet_aton (desimal, 0x heks, 0 oktal, 1-4 bagian); null bila tidak sah. */
    private fun parseLooseV4(parts: List<String>): Long? {
        if (parts.size !in 1..4) return null
        val v = LongArray(parts.size)
        for ((i, p) in parts.withIndex()) {
            val n: Long = when {
                p.startsWith("0x") || p.startsWith("0X") ->
                    if (p.length == 2) 0L else p.substring(2).toLongOrNull(16) ?: return null
                p.length > 1 && p.startsWith("0") ->
                    if (p.all { it in '0'..'7' }) p.toLongOrNull(8) ?: return null else return null
                else -> p.toLongOrNull() ?: return null
            }
            if (n < 0) return null
            v[i] = n
        }
        for (i in 0 until v.size - 1) if (v[i] > 255) return null
        val lastMax = 1L shl (8 * (5 - parts.size))
        if (v.last() >= lastMax) return null
        var addr = 0L
        for (i in 0 until v.size - 1) addr = addr or (v[i] shl (8 * (3 - i)))
        return addr or v.last()
    }

    private fun classifyV4(a: Long, allowLan: Boolean): Verdict {
        val b1 = ((a shr 24) and 255).toInt()
        val b2 = ((a shr 16) and 255).toInt()
        val b3 = ((a shr 8) and 255).toInt()
        if (b1 == 0 || b1 == 127) return blocked(Reason.LOOPBACK)
        if (b1 == 169 && b2 == 254) return blocked(Reason.LOOPBACK) // link-local + metadata cloud
        if (b1 >= 224) return blocked(Reason.LOOPBACK) // multicast dan cadangan
        val lan = b1 == 10 ||
            (b1 == 172 && b2 in 16..31) ||
            (b1 == 192 && b2 == 168) ||
            (b1 == 100 && b2 in 64..127) ||
            (b1 == 192 && b2 == 0 && b3 == 0) ||
            (b1 == 198 && (b2 == 18 || b2 == 19))
        if (lan && !allowLan) return blocked(Reason.PRIVATE_ADDRESS)
        return Verdict.Allowed
    }

    // ---------- IPv6 ----------

    private fun checkV6(raw: String, allowLan: Boolean): Verdict {
        val h = raw.lowercase()
        if (h.isEmpty() || h.contains('%') || !h.all { it in '0'..'9' || it in 'a'..'f' || it == ':' || it == '.' }) {
            return blocked(Reason.MALFORMED)
        }
        var text = h
        var tail: Long? = null // IPv4 tertanam di dua hextet terakhir
        if (h.contains('.')) {
            val i = h.lastIndexOf(':')
            if (i < 0) return blocked(Reason.MALFORMED)
            val dotted = h.substring(i + 1).split('.')
            if (dotted.size != 4 || dotted.any { it.isEmpty() || it.length > 3 || !it.all { c -> c in '0'..'9' } }) {
                return blocked(Reason.MALFORMED)
            }
            val bytes = dotted.map { it.toInt() }
            if (bytes.any { it > 255 }) return blocked(Reason.MALFORMED)
            val a = (bytes[0].toLong() shl 24) or (bytes[1].toLong() shl 16) or (bytes[2].toLong() shl 8) or bytes[3].toLong()
            tail = a
            text = h.substring(0, i + 1) + "0:0"
        }
        val hx = expandV6(text) ?: return blocked(Reason.MALFORMED)
        if (tail != null) { hx[6] = (tail shr 16).toInt(); hx[7] = (tail and 0xffff).toInt() }

        fun embedded(hi: Int, lo: Int): Verdict = classifyV4(((hi.toLong() and 0xffff) shl 16) or (lo.toLong() and 0xffff), allowLan)

        if (hx.all { it == 0 }) return blocked(Reason.LOOPBACK)
        if (hx.take(7).all { it == 0 } && hx[7] == 1) return blocked(Reason.LOOPBACK)
        // ::ffff:a.b.c.d (dipetakan IPv4) dan ::a.b.c.d (kompatibel)
        if (hx.take(5).all { it == 0 } && (hx[5] == 0xffff || hx[5] == 0)) return embedded(hx[6], hx[7])
        // NAT64 64:ff9b::/96
        if (hx[0] == 0x64 && hx[1] == 0xff9b && hx.slice(2..5).all { it == 0 }) return embedded(hx[6], hx[7])
        // 6to4 2002::/16 menyimpan IPv4 di hextet 1-2
        if (hx[0] == 0x2002) return embedded(hx[1], hx[2])
        val first = hx[0]
        if (first in 0xfe80..0xfebf || first in 0xfec0..0xfeff) return blocked(Reason.LOOPBACK) // link-local, site-local
        if (first >= 0xff00) return blocked(Reason.LOOPBACK) // multicast
        if (first in 0xfc00..0xfdff) return if (allowLan) Verdict.Allowed else blocked(Reason.PRIVATE_ADDRESS)
        if ((first and 0xe000) == 0x2000) return Verdict.Allowed // unicast global 2000::/3
        return blocked(Reason.LOOPBACK) // rentang lain belum dialokasikan
    }

    private fun expandV6(s: String): IntArray? {
        val dbl = s.indexOf("::")
        if (dbl >= 0 && s.indexOf("::", dbl + 1) >= 0) return null
        fun groups(t: String): List<Int>? {
            if (t.isEmpty()) return emptyList()
            return t.split(':').map { g ->
                if (g.isEmpty() || g.length > 4) return null
                g.toIntOrNull(16) ?: return null
            }
        }
        val out = IntArray(8)
        if (dbl < 0) {
            val g = groups(s) ?: return null
            if (g.size != 8) return null
            g.forEachIndexed { i, v -> out[i] = v }
        } else {
            val l = groups(s.substring(0, dbl)) ?: return null
            val r = groups(s.substring(dbl + 2)) ?: return null
            if (l.size + r.size > 7) return null
            l.forEachIndexed { i, v -> out[i] = v }
            r.forEachIndexed { i, v -> out[8 - r.size + i] = v }
        }
        return out
    }

    private fun blocked(r: Reason): Verdict = Verdict.Blocked(r)
}
''', 'UrlPolicy.kt')
put(P(C+'GuardedStreamResolver.kt'), r'''package com.molina.core.common

/**
 * Pembungkus resolver yang menerapkan [UrlPolicy] pada dua sisi: URL halaman yang masuk, dan URL
 * stream yang dikembalikan engine. Respons server (NewPipe, yt-dlp) tidak dipercaya: URL hasil
 * resolusi yang mengarah ke skema atau alamat terlarang ditolak sebelum sampai ke mpv.
 */
class GuardedStreamResolver(
    private val inner: StreamResolver,
    private val allowLan: () -> Boolean = { NetworkPolicy.allowLan },
) : StreamResolver {
    override val id: String get() = inner.id

    override fun canResolve(url: String): Boolean =
        UrlPolicy.check(url, allowLan()) is UrlPolicy.Verdict.Allowed && inner.canResolve(url)

    override suspend fun resolve(pageUrl: String): StreamOutcome<ResolvedStream> {
        val pre = UrlPolicy.check(pageUrl, allowLan())
        if (pre is UrlPolicy.Verdict.Blocked) {
            return StreamOutcome.Failure(StreamError.UNSUPPORTED, "URL ditolak: ${pre.reason.message}")
        }
        val r = inner.resolve(pageUrl)
        if (r is StreamOutcome.Success) {
            val post = UrlPolicy.check(r.value.url, allowLan())
            if (post is UrlPolicy.Verdict.Blocked) {
                return StreamOutcome.Failure(StreamError.UNSUPPORTED, "URL hasil resolusi ditolak: ${post.reason.message}")
            }
        }
        return r
    }

    override suspend fun search(query: String, limit: Int): StreamOutcome<List<StreamSearchItem>> =
        inner.search(query, limit)
}
''', 'GuardedStreamResolver.kt')
put(P(S+'settings/NetworkSecurityUi.kt'), r'''package com.molina.studio.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.molina.studio.stream.StreamEnginePrefs

/** Halaman "Keamanan jaringan": kebijakan URL untuk mpv dan yt-dlp. Jaringan dianggap tidak tepercaya. */
@Composable
internal fun NetworkSecurityPage() {
    val lan by StreamEnginePrefs.allowLan.collectAsState()
    Category("Kebijakan URL")
    SettingsItem(
        "Daftar putih skema",
        "Hanya http, https, rtmp(s), rtsp, dan berkas lokal. concat:, edl://, fd://, data: dan sejenisnya " +
            "selalu ditolak, begitu pula alamat perangkat sendiri (127.x) dan metadata cloud (169.254.x)."
    ) {}
    SwitchRow(
        "Izinkan jaringan lokal (LAN)",
        "Untuk NAS, Jellyfin, DLNA (192.168.x.x, 10.x.x.x, nama .local). Default mati supaya tautan " +
            "jahat tidak bisa memakai aplikasi untuk memindai jaringan rumah Anda.",
        lan
    ) { StreamEnginePrefs.enableLan(it) }
    Category("Catatan")
    SettingsItem(
        "Tautan http dan rtmp tidak terenkripsi",
        "Isinya bisa dibaca atau diubah siapa pun di jaringan yang sama. Gunakan https bila tersedia."
    ) {}
}
''', 'NetworkSecurityUi.kt')
put(P('app/src/main/res/xml/network_security_config.xml'), r'''<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Lalu lintas Java/Kotlin (NewPipe, pemasang yt-dlp): tanpa cleartext, hanya CA sistem.
         Catatan: mpv memakai jaringan native, tidak tunduk pada berkas ini; lihat UrlPolicy. -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
''', 'network_security_config.xml')

# --- yt-dlp: jangan baca config di rootfs ---
edit(P(C+'YtdlpCommand.kt'), '"-J", "--no-playlist",', '"--ignore-config", "-J", "--no-playlist",', 'yt-dlp resolve: --ignore-config', '"--ignore-config", "-J", "--no-playlist"')
edit(P(C+'YtdlpCommand.kt'), '"--flat-playlist", "-j",', '"--ignore-config", "--flat-playlist", "-j",', 'yt-dlp search: --ignore-config', '"--ignore-config", "--flat-playlist"')
edit(P(C+'YtdlpFetch.kt'), '"-J", "--no-simulate",', '"--ignore-config", "-J", "--no-simulate",', 'yt-dlp fetch: --ignore-config', '"--ignore-config", "-J", "--no-simulate"')

# --- gerbang URL tunggal untuk semua loadfile lewat MpvLoad ---
edit(P(M+'MpvLoad.kt'), 'import com.molina.core.common.StreamHeaders\n',
     'import com.molina.core.common.AppLog\nimport com.molina.core.common.LogLevel\nimport com.molina.core.common.StreamHeaders\nimport com.molina.core.common.UrlPolicy\n',
     'MpvLoad: impor', 'import com.molina.core.common.UrlPolicy')
edit(P(M+'MpvLoad.kt'),
     '    fun loadfileArgs(url: String, flag: String, headers: Map<String, String>, title: String? = null): Array<String> {\n',
     '    fun loadfileArgs(url: String, flag: String, headers: Map<String, String>, title: String? = null): Array<String> {\n'
     '        // Jaringan dianggap tidak tepercaya: URL yang melanggar kebijakan tidak pernah sampai ke mpv.\n'
     '        val verdict = UrlPolicy.check(url)\n'
     '        if (verdict is UrlPolicy.Verdict.Blocked) {\n'
     '            AppLog.log("net", LogLevel.Warn, "policy", "ditolak: ${verdict.reason.name}")\n'
     '            return arrayOf("show-text", "Diblokir: ${verdict.reason.message}", "5000")\n'
     '        }\n',
     'MpvLoad: gerbang UrlPolicy', 'UrlPolicy.check(url)')

# --- opsi mpv ---
edit(P(M+'MPVView.kt'), '        MPVLib.setOptionString("ytdl", "no")\n',
     '        MPVLib.setOptionString("ytdl", "no")\n'
     '        // Daftar putar dari jaringan tidak boleh menyisipkan berkas lokal atau protokol lain.\n'
     '        MPVLib.setOptionString("load-unsafe-playlists", "no")\n',
     'MPVView: load-unsafe-playlists=no', 'load-unsafe-playlists')

# --- resolver dibungkus gerbang ---
edit(P(S+'MolinaApp.kt'), 'import com.molina.core.common.CompositeStreamResolver\n',
     'import com.molina.core.common.CompositeStreamResolver\nimport com.molina.core.common.GuardedStreamResolver\n',
     'MolinaApp: impor', 'import com.molina.core.common.GuardedStreamResolver')
edit(P(S+'MolinaApp.kt'),
     '            CompositeStreamResolver(\n                primary = NewPipeStreamResolver(),\n                fallback = YtdlpStreamResolver(this, { StreamEnginePrefs.ytdlpEnabled.value }),\n            )\n',
     '            GuardedStreamResolver(\n                CompositeStreamResolver(\n                    primary = NewPipeStreamResolver(),\n                    fallback = YtdlpStreamResolver(this, { StreamEnginePrefs.ytdlpEnabled.value }),\n                )\n            )\n',
     'MolinaApp: GuardedStreamResolver', 'GuardedStreamResolver(\n')

# --- preferensi LAN ---
PR = P(S+'stream/StreamEnginePrefs.kt')
edit(PR, 'import android.content.SharedPreferences\n', 'import android.content.SharedPreferences\nimport com.molina.core.common.NetworkPolicy\n', 'Prefs: impor', 'import com.molina.core.common.NetworkPolicy')
edit(PR, '    private const val KEY_YTDLP = "ytdlp_enabled"\n', '    private const val KEY_YTDLP = "ytdlp_enabled"\n    private const val KEY_LAN = "allow_lan"\n', 'Prefs: kunci', 'KEY_LAN')
edit(PR, '    val ytdlpEnabled: StateFlow<Boolean> = _ytdlpEnabled.asStateFlow()\n',
     '    val ytdlpEnabled: StateFlow<Boolean> = _ytdlpEnabled.asStateFlow()\n\n    private val _allowLan = MutableStateFlow(false)\n\n    /** Izinkan alamat jaringan lokal (default mati). Nilainya juga dicerminkan ke [NetworkPolicy]. */\n    val allowLan: StateFlow<Boolean> = _allowLan.asStateFlow()\n',
     'Prefs: flow', 'val allowLan: StateFlow<Boolean>')
edit(PR, '        _ytdlpEnabled.value = p.getBoolean(KEY_YTDLP, false)\n',
     '        _ytdlpEnabled.value = p.getBoolean(KEY_YTDLP, false)\n        _allowLan.value = p.getBoolean(KEY_LAN, false)\n        NetworkPolicy.allowLan = _allowLan.value\n',
     'Prefs: init', 'NetworkPolicy.allowLan = _allowLan.value')
edit(PR, '    fun enableYtdlp(v: Boolean) {\n',
     '    fun enableLan(v: Boolean) {\n        _allowLan.value = v\n        NetworkPolicy.allowLan = v\n        sp?.edit()?.putBoolean(KEY_LAN, v)?.apply()\n    }\n\n    fun enableYtdlp(v: Boolean) {\n',
     'Prefs: enableLan', 'fun enableLan')

# --- halaman Pengaturan ---
SC = P(S+'settings/SettingsScreen.kt')
edit(SC, 'CrashReports, About, Log, Stream\n', 'CrashReports, About, Log, Stream, Network\n', 'Settings: enum', 'Log, Stream, Network')
edit(SC, '    Page.Stream -> "Mesin cadangan (yt-dlp)"\n', '    Page.Stream -> "Mesin cadangan (yt-dlp)"\n    Page.Network -> "Keamanan jaringan"\n', 'Settings: judul', 'Page.Network -> "Keamanan jaringan"')
edit(SC, '{ page = Page.Stream }\n', '{ page = Page.Stream }\n                SettingsItem("Keamanan jaringan", "Kebijakan URL dan jaringan lokal (LAN)") { page = Page.Network }\n', 'Settings: menu', 'page = Page.Network')
edit(SC, '            Page.Stream -> StreamEnginePage()\n', '            Page.Stream -> StreamEnginePage()\n            Page.Network -> NetworkSecurityPage()\n', 'Settings: rute', 'Page.Network -> NetworkSecurityPage()')

# --- Manifest ---
edit(P('app/src/main/AndroidManifest.xml'), '        android:allowBackup="false"\n',
     '        android:allowBackup="false"\n        android:networkSecurityConfig="@xml/network_security_config"\n', 'Manifest: networkSecurityConfig', 'networkSecurityConfig')

note = "- [x] feat(security): UrlPolicy (daftar putih skema, blok loopback/metadata/SSRF, LAN opt-in), GuardedStreamResolver, gerbang di MpvLoad, mpv load-unsafe-playlists=no, yt-dlp --ignore-config, NetworkSecurityConfig, halaman Keamanan jaringan - menunggu CI hijau"
p = rd(P('progres.md'))
if note in p: print("progres : sudah ada")
else:
    if not p.endswith("\n"): p += "\n"
    wr(P('progres.md'), p + "\n## 2026-10-24 - Keamanan jaringan\n" + note + "\n- [ ] uji perangkat: putar https biasa, TikTok, URL http://127.0.0.1 (harus ditolak), LAN dengan sakelar mati/hidup\n")
    print("progres : diperbarui")
