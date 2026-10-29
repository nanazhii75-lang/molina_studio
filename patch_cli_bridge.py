import os, sys, stat

def find_root():
    for c in (os.getcwd(), "/storage/emulated/0/molina-studio", "/sdcard/molina-studio"):
        if os.path.isfile(os.path.join(c, "settings.gradle.kts")) and os.path.isdir(os.path.join(c, "app")):
            return c
    sys.exit("GAGAL: repo molina-studio tidak ditemukan. Jalankan dari dalam repo atau clone ke /storage/emulated/0/molina-studio")

ROOT = find_root()
EXEC = {"app/src/main/assets/molina/molina-cli.sh", "scripts/app-tests/run_command_protocol.sh"}

NEW_FILES = {
'app/src/main/java/com/molina/studio/cmd/CommandProtocol.kt': r'''package com.molina.studio.cmd

/**
 * Protokol CLI `molina` (Debian -> aplikasi). Kotlin murni agar bisa diuji di JVM.
 * Satu permintaan per koneksi: "verb[TAB]argumen\n". Balasan satu baris:
 *   "OK[ teks]\n" atau "ERR kode teks\n".
 */
object CommandProtocol {
    /** Abstract-namespace Unix socket (sama gaya dengan @molina.log). */
    const val SOCKET_NAME = "molina.cmd"
    const val VERSION = 1
    const val MAX_LINE = 4096

    enum class Verb(val wire: String) {
        PING("ping"), VERSION("version"), STATUS("status"), PLAY("play"), STOP("stop")
    }

    class Request(val verb: Verb, val arg: String)

    sealed interface Parsed {
        class Ok(val req: Request) : Parsed
        class Bad(val code: String, val message: String) : Parsed
    }

    fun parse(line: String): Parsed {
        val t = line.trimEnd('\r', '\n')
        if (t.isBlank()) return Parsed.Bad("empty", "perintah kosong")
        val tab = t.indexOf('\t')
        val word = (if (tab < 0) t else t.substring(0, tab)).trim().lowercase()
        val arg = if (tab < 0) "" else t.substring(tab + 1).trim()
        val verb = Verb.entries.firstOrNull { it.wire == word }
            ?: return Parsed.Bad("unknown", "perintah tidak dikenal: ${oneLine(word)}")
        if (verb == Verb.PLAY && arg.isEmpty()) return Parsed.Bad("usage", "play butuh path atau URL")
        return Parsed.Ok(Request(verb, arg))
    }

    fun ok(text: String = ""): String = if (text.isEmpty()) "OK\n" else "OK ${oneLine(text)}\n"

    fun err(code: String, text: String): String = "ERR $code ${oneLine(text)}\n"

    /** Satu baris, tanpa TAB/CR/LF, maks 300 karakter (balasan dan log tidak boleh memecah protokol). */
    fun oneLine(s: String): String = s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').take(300)

    /**
     * Path yang terlihat di Debian (/sdcard atau /storage/emulated/0) -> path Android di bawah
     * [storageRoot]. Null bila bukan path mutlak, mengandung NUL atau "..", atau di luar dua akar itu.
     */
    fun guestToHost(guest: String, storageRoot: String): String? {
        val g = guest.trim()
        if (!g.startsWith("/") || g.indexOf('\u0000') >= 0) return null
        val rest = when {
            g == "/sdcard" || g == "/storage/emulated/0" -> ""
            g.startsWith("/sdcard/") -> g.substring("/sdcard".length)
            g.startsWith("/storage/emulated/0/") -> g.substring("/storage/emulated/0".length)
            else -> return null
        }
        val parts = rest.split('/').filter { it.isNotEmpty() && it != "." }
        if (parts.any { it == ".." }) return null
        val root = storageRoot.trimEnd('/')
        return if (parts.isEmpty()) root else root + "/" + parts.joinToString("/")
    }
}
''',
'app/src/main/java/com/molina/studio/cmd/CommandServer.kt': r'''package com.molina.studio.cmd

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.Environment
import android.os.Process
import com.molina.cinema.MpvFolder
import com.molina.core.common.LogLevel
import com.molina.studio.cinema.CinemaActions
import com.molina.studio.log.LogBus
import com.molina.studio.system.MpvControl
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Server perintah CLI `molina` (Debian -> aplikasi): abstract Unix socket "@molina.cmd".
 * Hanya UID yang sama dengan aplikasi yang dilayani (peer credentials), seperti LogServer.
 * Perintah (lihat [CommandProtocol]): ping, version, status, play, stop. Tanpa izin atau host baru.
 */
object CommandServer {
    private const val MAX_CLIENTS = 8
    private const val READ_TIMEOUT_MS = 5000
    private const val TAG = "CommandServer"

    @Volatile private var server: LocalServerSocket? = null
    private val clients = AtomicInteger(0)

    @Synchronized
    fun start(ctx: Context) {
        if (server != null) return
        val app = ctx.applicationContext
        val s = try {
            LocalServerSocket(CommandProtocol.SOCKET_NAME)
        } catch (e: IOException) {
            LogBus.log("sistem", LogLevel.Error, TAG, "Gagal bind @${CommandProtocol.SOCKET_NAME}: ${e.message}")
            return
        }
        server = s
        LogBus.log("sistem", LogLevel.Info, TAG, "Mendengarkan @${CommandProtocol.SOCKET_NAME}")
        val t = Thread({ acceptLoop(s, app) }, "molina-cmd-accept")
        t.isDaemon = true
        t.start()
    }

    @Synchronized
    fun stop() {
        try { server?.close() } catch (e: IOException) { }
        server = null
    }

    private fun acceptLoop(s: LocalServerSocket, app: Context) {
        while (true) {
            val c = try { s.accept() } catch (e: IOException) { break }
            if (clients.incrementAndGet() > MAX_CLIENTS) {
                clients.decrementAndGet()
                closeQuietly(c)
                continue
            }
            val t = Thread({
                try {
                    handle(c, app)
                } catch (e: IOException) {
                    // peer menutup koneksi atau timeout baca
                } finally {
                    clients.decrementAndGet()
                    closeQuietly(c)
                }
            }, "molina-cmd-client")
            t.isDaemon = true
            t.start()
        }
        synchronized(this) { if (server === s) server = null }
    }

    private fun handle(c: LocalSocket, app: Context) {
        val uid = try { c.peerCredentials.uid } catch (e: IOException) { -1 }
        if (uid != Process.myUid()) {
            LogBus.log("sistem", LogLevel.Warn, TAG, "Koneksi ditolak (uid $uid)")
            return
        }
        c.soTimeout = READ_TIMEOUT_MS
        val line = readLine(c.inputStream)
        val reply = when {
            line == null -> return
            line.tooLong -> CommandProtocol.err("toolong", "perintah terlalu panjang")
            else -> when (val p = CommandProtocol.parse(line.text)) {
                is CommandProtocol.Parsed.Bad -> CommandProtocol.err(p.code, p.message)
                is CommandProtocol.Parsed.Ok -> execute(app, p.req)
            }
        }
        c.outputStream.write(reply.toByteArray(Charsets.UTF_8))
        c.outputStream.flush()
    }

    private class Line(val text: String, val tooLong: Boolean)

    /** Baca satu baris berbatas [CommandProtocol.MAX_LINE] byte; null bila koneksi tutup tanpa data. */
    private fun readLine(input: InputStream): Line? {
        val buf = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) return if (buf.size() == 0) null else Line(buf.toString("UTF-8"), false)
            if (b == '\n'.code) return Line(buf.toString("UTF-8"), false)
            if (buf.size() >= CommandProtocol.MAX_LINE) return Line("", true)
            buf.write(b)
        }
    }

    private fun execute(app: Context, r: CommandProtocol.Request): String = when (r.verb) {
        CommandProtocol.Verb.PING -> CommandProtocol.ok("pong")
        CommandProtocol.Verb.VERSION -> CommandProtocol.ok("molina-cmd ${CommandProtocol.VERSION}")
        CommandProtocol.Verb.STATUS ->
            CommandProtocol.ok(if (MpvControl.isActive()) "player=aktif" else "player=diam")
        CommandProtocol.Verb.STOP ->
            CommandProtocol.ok(if (MpvControl.shutdown(app)) "pemutar dihentikan" else "tidak ada pemutar aktif")
        CommandProtocol.Verb.PLAY -> play(NewTaskContext(app), r.arg)
    }

    private fun play(ctx: Context, arg: String): String {
        if (arg.contains("://")) {
            return when (val i = CinemaActions.classify(arg)) {
                is CinemaActions.Input.Direct ->
                    if (CinemaActions.play(ctx, i.uri)) CommandProtocol.ok("memutar URL")
                    else CommandProtocol.err("launch", "gagal membuka pemutar")
                else -> CommandProtocol.err("url", "URL tidak didukung (http, https, rtsp, rtmp, rtmps)")
            }
        }
        val root = Environment.getExternalStorageDirectory().absolutePath
        val host = CommandProtocol.guestToHost(arg, root)
            ?: return CommandProtocol.err("path", "path harus di bawah /sdcard atau /storage/emulated/0")
        val f = File(host)
        return when {
            f.isFile ->
                if (CinemaActions.playPath(ctx, host)) CommandProtocol.ok("memutar berkas")
                else CommandProtocol.err("launch", "gagal membuka pemutar")
            f.isDirectory -> {
                val first = MpvFolder.firstMediaIn(f)
                when {
                    first == null -> CommandProtocol.err("empty", "tidak ada media di folder")
                    CinemaActions.playFolder(ctx, first) -> CommandProtocol.ok("memutar folder")
                    else -> CommandProtocol.err("launch", "gagal membuka pemutar")
                }
            }
            else -> CommandProtocol.err("missing", "berkas tidak ditemukan")
        }
    }

    /** startActivity dari konteks Application wajib FLAG_ACTIVITY_NEW_TASK; CinemaActions tidak menambahkannya. */
    private class NewTaskContext(base: Context) : ContextWrapper(base) {
        override fun startActivity(intent: Intent) {
            super.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun closeQuietly(c: LocalSocket) {
        try { c.close() } catch (e: IOException) { }
    }
}
''',
'app/src/main/assets/molina/molina-cli.sh': r'''#!/bin/sh
# Molina CLI: jembatan Debian -> aplikasi Molina. Dibuat otomatis oleh aplikasi (jangan diedit).
if ! command -v python3 >/dev/null 2>&1; then
  echo "molina: python3 belum terpasang. Jalankan: apt install -y python3" >&2
  exit 127
fi
exec python3 /usr/local/lib/molina/molina_cli.py "$@"
''',
'app/src/main/assets/molina/molina-cli.py': r'''#!/usr/bin/env python3
# Molina CLI: kirim perintah dari Debian ke aplikasi Molina lewat abstract Unix socket @molina.cmd.
# Dibuat otomatis oleh aplikasi (jangan diedit). Hanya pustaka standar Python 3.
# Kode keluar: 0 berhasil, 1 ditolak aplikasi, 2 salah pakai, 3 aplikasi tidak terjangkau.
import os
import socket
import sys

CMD_NAME = os.environ.get("MOLINA_CMD_SOCKET", "molina.cmd")
LOG_NAME = os.environ.get("MOLINA_LOG_SOCKET", "molina.log")

HELP = """molina - kendalikan aplikasi Molina dari Debian

Pemakaian:
  molina ping                    cek koneksi ke aplikasi
  molina version                 versi protokol
  molina status                  status pemutar (aktif/diam)
  molina play <path|URL|->       putar berkas, folder, atau URL di Molina Cinema
                                 ("-" = baca satu baris dari stdin)
  molina stop                    hentikan pemutar
  molina log [-l D|I|W|E] [-t tag] pesan...
                                 kirim baris ke Log Molina (sumber "cli")

Path harus di bawah /sdcard atau /storage/emulated/0 (terlihat dari Debian).
Contoh:
  molina play /sdcard/Molina/Media/Video/film.mkv
  yt-dlp -f b -g "https://..." | molina play -
"""

LEVELS = ("D", "I", "W", "E")


def connect(name, timeout):
    s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    s.settimeout(timeout)
    s.connect("\0" + name)
    return s


def call(verb, arg="", timeout=10.0):
    """Kirim satu perintah, kembalikan satu baris balasan (tanpa newline)."""
    payload = verb + ("\t" + arg if arg else "") + "\n"
    s = connect(CMD_NAME, timeout)
    try:
        s.sendall(payload.encode("utf-8"))
        data = b""
        while b"\n" not in data:
            chunk = s.recv(4096)
            if not chunk:
                break
            data += chunk
    finally:
        s.close()
    return data.decode("utf-8", "replace").split("\n", 1)[0]


def run(verb, arg=""):
    try:
        reply = call(verb, arg)
    except (ConnectionRefusedError, FileNotFoundError):
        sys.stderr.write("molina: aplikasi Molina tidak berjalan atau belum siap (buka aplikasinya dulu)\n")
        return 3
    except (socket.timeout, OSError) as e:
        sys.stderr.write("molina: gagal menghubungi aplikasi: %s\n" % e)
        return 3
    if reply == "OK" or reply.startswith("OK "):
        text = reply[3:]
        if text:
            print(text)
        return 0
    if reply.startswith("ERR "):
        parts = reply.split(" ", 2)
        sys.stderr.write("molina: %s\n" % (parts[2] if len(parts) > 2 else parts[1]))
        return 1
    sys.stderr.write("molina: balasan tidak dikenal dari aplikasi\n")
    return 1


def clean(s):
    return s.replace("\t", " ").replace("\r", " ").replace("\n", " ")


def send_log(level, tag, message):
    line = "\t".join(["cli", level, clean(tag), clean(message)]) + "\n"
    try:
        s = connect(LOG_NAME, 5.0)
        try:
            s.sendall(line.encode("utf-8"))
        finally:
            s.close()
    except OSError:
        sys.stderr.write("molina: aplikasi Molina tidak berjalan atau belum siap\n")
        return 3
    return 0


def cmd_play(args):
    if len(args) != 1:
        sys.stderr.write("molina: play butuh tepat satu path, URL, atau -\n")
        return 2
    target = args[0]
    if target == "-":
        target = ""
        for line in sys.stdin:
            if line.strip():
                target = line.strip()
                break
        if not target:
            sys.stderr.write("molina: stdin kosong\n")
            return 2
    if "://" not in target:
        target = os.path.abspath(os.path.expanduser(target))
    return run("play", target)


def cmd_log(args):
    level, tag, words = "I", "molina", list(args)
    while words and words[0] in ("-l", "-t"):
        if len(words) < 2:
            sys.stderr.write("molina: opsi %s butuh nilai\n" % words[0])
            return 2
        if words[0] == "-l":
            level = words[1].upper()
            if level not in LEVELS:
                sys.stderr.write("molina: level harus D, I, W, atau E\n")
                return 2
        else:
            tag = words[1]
        words = words[2:]
    if not words:
        sys.stderr.write("molina: log butuh pesan\n")
        return 2
    return send_log(level, tag, " ".join(words))


def main(argv):
    if not argv or argv[0] in ("-h", "--help", "help"):
        sys.stdout.write(HELP)
        return 0 if argv else 2
    verb, rest = argv[0], argv[1:]
    if verb in ("ping", "version", "status", "stop"):
        if rest:
            sys.stderr.write("molina: %s tidak menerima argumen\n" % verb)
            return 2
        return run(verb)
    if verb == "play":
        return cmd_play(rest)
    if verb == "log":
        return cmd_log(rest)
    sys.stderr.write("molina: perintah tidak dikenal: %s (coba: molina help)\n" % verb)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
''',
'scripts/app-tests/CommandProtocolTests.kt': r'''import com.molina.studio.cmd.CommandProtocol
import com.molina.studio.cmd.CommandProtocol.Parsed
import com.molina.studio.cmd.CommandProtocol.Verb

private var fails = 0
private var runs = 0
private fun check(name: String, ok: Boolean) { runs++; if (!ok) { fails++; println("GAGAL: $name") } }
private fun <T> eq(name: String, got: T, want: T) { runs++; if (got != want) { fails++; println("GAGAL: $name -> $got (seharusnya $want)") } }

private fun okReq(line: String): CommandProtocol.Request? = (CommandProtocol.parse(line) as? Parsed.Ok)?.req
private fun badCode(line: String): String? = (CommandProtocol.parse(line) as? Parsed.Bad)?.code

fun main() {
    // parse: perintah tanpa argumen
    eq("ping", okReq("ping\n")?.verb, Verb.PING)
    eq("version", okReq("version")?.verb, Verb.VERSION)
    eq("status CRLF", okReq("status\r\n")?.verb, Verb.STATUS)
    eq("stop", okReq("stop\n")?.verb, Verb.STOP)
    eq("huruf besar", okReq("PING\n")?.verb, Verb.PING)
    eq("ping tanpa arg", okReq("ping\n")?.arg, "")

    // parse: play dengan argumen (spasi di dalam path tetap utuh)
    eq("play verb", okReq("play\t/sdcard/a b.mkv\n")?.verb, Verb.PLAY)
    eq("play arg spasi", okReq("play\t/sdcard/a b.mkv\n")?.arg, "/sdcard/a b.mkv")
    eq("play arg dipangkas", okReq("play\t  /sdcard/x.mp4  \n")?.arg, "/sdcard/x.mp4")
    eq("play url", okReq("play\thttps://example.org/v?a=1\n")?.arg, "https://example.org/v?a=1")
    eq("play TAB di arg dipertahankan", okReq("play\t/sdcard/a\tb\n")?.arg, "/sdcard/a\tb")

    // parse: galat
    eq("kosong", badCode(""), "empty")
    eq("hanya newline", badCode("\n"), "empty")
    eq("spasi saja", badCode("   \n"), "empty")
    eq("play tanpa arg", badCode("play\n"), "usage")
    eq("play arg kosong", badCode("play\t \n"), "usage")
    eq("tidak dikenal", badCode("joget\n"), "unknown")
    eq("pemisah spasi bukan TAB", badCode("play /sdcard/a.mp4\n"), "unknown")

    // balasan: satu baris, tanpa TAB/CR/LF, dibatasi
    eq("ok kosong", CommandProtocol.ok(), "OK\n")
    eq("ok teks", CommandProtocol.ok("pong"), "OK pong\n")
    eq("ok newline disapu", CommandProtocol.ok("a\nb\tc\rd"), "OK a b c d\n")
    eq("err", CommandProtocol.err("path", "tidak valid"), "ERR path tidak valid\n")
    check("err sebaris", CommandProtocol.err("x", "a\nb").count { it == '\n' } == 1)
    check("oneLine batas 300", CommandProtocol.oneLine("x".repeat(1000)).length == 300)

    // guestToHost: pemetaan sah
    val root = "/storage/emulated/0"
    eq("sdcard akar", CommandProtocol.guestToHost("/sdcard", root), root)
    eq("storage akar", CommandProtocol.guestToHost("/storage/emulated/0", root), root)
    eq("sdcard berkas", CommandProtocol.guestToHost("/sdcard/Molina/Media/Video/a.mkv", root), "$root/Molina/Media/Video/a.mkv")
    eq("storage berkas", CommandProtocol.guestToHost("/storage/emulated/0/DCIM/x.mp4", root), "$root/DCIM/x.mp4")
    eq("garis ganda dan titik", CommandProtocol.guestToHost("/sdcard//Molina/./a.mp4", root), "$root/Molina/a.mp4")
    eq("garis miring akhir", CommandProtocol.guestToHost("/sdcard/Molina/", root), "$root/Molina")
    eq("root berakhiran slash", CommandProtocol.guestToHost("/sdcard/a.mp4", "$root/"), "$root/a.mp4")
    eq("spasi di nama", CommandProtocol.guestToHost("/sdcard/Film Baru/a b.mkv", root), "$root/Film Baru/a b.mkv")
    eq("root alternatif", CommandProtocol.guestToHost("/sdcard/a.mp4", "/sdcard"), "/sdcard/a.mp4")

    // guestToHost: penolakan
    eq("relatif", CommandProtocol.guestToHost("film.mp4", root), null)
    eq("kosong", CommandProtocol.guestToHost("", root), null)
    eq("titik-titik di tengah", CommandProtocol.guestToHost("/sdcard/Molina/../DCIM/a.mp4", root), null)
    eq("titik-titik keluar", CommandProtocol.guestToHost("/sdcard/../data/data/x", root), null)
    eq("akhiran titik-titik", CommandProtocol.guestToHost("/sdcard/..", root), null)
    eq("root guest", CommandProtocol.guestToHost("/root/.bashrc", root), null)
    eq("etc", CommandProtocol.guestToHost("/etc/passwd", root), null)
    eq("data aplikasi", CommandProtocol.guestToHost("/data/data/com.molina.studio/files/x", root), null)
    eq("awalan mirip", CommandProtocol.guestToHost("/sdcardx/a.mp4", root), null)
    eq("awalan mirip 2", CommandProtocol.guestToHost("/storage/emulated/01/a.mp4", root), null)
    eq("kartu SD", CommandProtocol.guestToHost("/storage/1234-ABCD/a.mp4", root), null)
    eq("NUL", CommandProtocol.guestToHost("/sdcard/a\u0000.mp4", root), null)
    eq("nama berawalan titik tetap sah", CommandProtocol.guestToHost("/sdcard/.hidden/a.mp4", root), "$root/.hidden/a.mp4")
    eq("nama dengan dua titik di dalamnya sah", CommandProtocol.guestToHost("/sdcard/a..b/c.mp4", root), "$root/a..b/c.mp4")

    check("konstanta", CommandProtocol.SOCKET_NAME == "molina.cmd" && CommandProtocol.VERSION == 1)
    check("verb unik", Verb.entries.map { it.wire }.toSet().size == Verb.entries.size)

    println(if (fails == 0) "LULUS: $runs kasus" else "$fails dari $runs kasus GAGAL")
    if (fails != 0) System.exit(1)
}
''',
'scripts/app-tests/run_command_protocol.sh': r'''#!/usr/bin/env bash
# Uji JVM murni untuk CommandProtocol (parse, balasan satu baris, pemetaan path Debian -> Android) di :app.
# Butuh kotlinc (>= 2.1) di PATH atau KOTLINC=/jalur/kotlinc.
set -euo pipefail
cd "$(dirname "$0")/../.."
KC="${KOTLINC:-kotlinc}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$KC" -nowarn -d "$OUT" app/src/main/java/com/molina/studio/cmd/CommandProtocol.kt scripts/app-tests/CommandProtocolTests.kt
java -cp "$OUT:$(dirname "$(command -v "$KC")")/../lib/kotlin-stdlib.jar" CommandProtocolTestsKt
''',
'scripts/app-tests/test_molina_cli.py': r'''#!/usr/bin/env python3
"""Uji klien `molina` (molina_cli.py) terhadap server palsu di abstract socket. Hanya stdlib.
Jalankan: python3 scripts/app-tests/test_molina_cli.py"""
import os
import socket
import subprocess
import sys
import threading
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CLI = os.path.join(ROOT, "app/src/main/assets/molina/molina-cli.py")
SUF = str(os.getpid())
CMD = "molina.cmd.test" + SUF
LOG = "molina.log.test" + SUF


class FakeServer:
    """Terima satu koneksi per permintaan; catat baris; balas sesuai fungsi."""

    def __init__(self, name, reply):
        self.reply = reply
        self.lines = []
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.bind("\0" + name)
        self.sock.listen(4)
        self.sock.settimeout(0.2)
        self.stop = False
        self.t = threading.Thread(target=self.loop, daemon=True)
        self.t.start()

    def loop(self):
        while not self.stop:
            try:
                c, _ = self.sock.accept()
            except socket.timeout:
                continue
            except OSError:
                return
            data = b""
            while b"\n" not in data:
                chunk = c.recv(4096)
                if not chunk:
                    break
                data += chunk
            line = data.decode("utf-8").split("\n", 1)[0]
            self.lines.append(line)
            r = self.reply(line)
            if r:
                c.sendall(r.encode("utf-8"))
            c.close()

    def close(self):
        self.stop = True
        self.t.join()
        self.sock.close()


def run_cli(*args, stdin=None):
    env = dict(os.environ, MOLINA_CMD_SOCKET=CMD, MOLINA_LOG_SOCKET=LOG)
    p = subprocess.run([sys.executable, CLI, *args], input=stdin, capture_output=True,
                       text=True, env=env, timeout=20)
    return p.returncode, p.stdout, p.stderr


def echo_reply(line):
    verb = line.split("\t", 1)[0]
    if verb == "ping":
        return "OK pong\n"
    if verb == "version":
        return "OK molina-cmd 1\n"
    if verb == "status":
        return "OK player=diam\n"
    if verb == "stop":
        return "OK\n"
    if verb == "play":
        arg = line.split("\t", 1)[1]
        if arg.endswith("tidakada.mkv"):
            return "ERR missing berkas tidak ditemukan\n"
        return "OK memutar\n"
    return "ERR unknown perintah tidak dikenal\n"


class CliTests(unittest.TestCase):
    def setUp(self):
        self.srv = FakeServer(CMD, echo_reply)

    def tearDown(self):
        self.srv.close()

    def test_simple_verbs(self):
        self.assertEqual(run_cli("ping"), (0, "pong\n", ""))
        self.assertEqual(run_cli("version")[1], "molina-cmd 1\n")
        self.assertEqual(run_cli("status")[1], "player=diam\n")
        self.assertEqual(run_cli("stop")[0:2], (0, ""))
        self.assertEqual(self.srv.lines, ["ping", "version", "status", "stop"])

    def test_play_absolute_path_and_normalization(self):
        code, out, _ = run_cli("play", "/sdcard/Molina/../Molina/a b.mkv")
        self.assertEqual(code, 0)
        self.assertEqual(self.srv.lines[-1], "play\t/sdcard/Molina/a b.mkv")

    def test_play_relative_path_uses_cwd(self):
        env = dict(os.environ, MOLINA_CMD_SOCKET=CMD, MOLINA_LOG_SOCKET=LOG)
        p = subprocess.run([sys.executable, CLI, "play", "film.mp4"], cwd="/tmp",
                           capture_output=True, text=True, env=env, timeout=20)
        self.assertEqual(p.returncode, 0)
        self.assertEqual(self.srv.lines[-1], "play\t/tmp/film.mp4")

    def test_play_url_untouched(self):
        self.assertEqual(run_cli("play", "https://example.org/v.m3u8?a=1")[0], 0)
        self.assertEqual(self.srv.lines[-1], "play\thttps://example.org/v.m3u8?a=1")

    def test_play_stdin_first_nonblank_line(self):
        code, _, _ = run_cli("play", "-", stdin="\n  \nhttps://example.org/x.mp4\nhttps://lain\n")
        self.assertEqual(code, 0)
        self.assertEqual(self.srv.lines[-1], "play\thttps://example.org/x.mp4")

    def test_play_stdin_empty(self):
        code, _, err = run_cli("play", "-", stdin="\n\n")
        self.assertEqual(code, 2)
        self.assertIn("stdin kosong", err)

    def test_server_error_exit_1(self):
        code, out, err = run_cli("play", "/sdcard/tidakada.mkv")
        self.assertEqual(code, 1)
        self.assertEqual(out, "")
        self.assertEqual(err, "molina: berkas tidak ditemukan\n")

    def test_usage_errors_exit_2(self):
        self.assertEqual(run_cli()[0], 2)
        self.assertEqual(run_cli("play")[0], 2)
        self.assertEqual(run_cli("play", "a", "b")[0], 2)
        self.assertEqual(run_cli("ping", "x")[0], 2)
        self.assertEqual(run_cli("joget")[0], 2)
        self.assertEqual(run_cli("log")[0], 2)
        self.assertEqual(run_cli("log", "-l", "X", "pesan")[0], 2)
        self.assertEqual(run_cli("log", "-t")[0], 2)
        self.assertEqual(self.srv.lines, [])  # tidak ada yang sampai ke server

    def test_help(self):
        code, out, _ = run_cli("help")
        self.assertEqual(code, 0)
        self.assertIn("molina play", out)

    def test_log_line_format(self):
        logsrv = FakeServer(LOG, lambda line: "")
        try:
            code, _, _ = run_cli("log", "-l", "w", "-t", "uji", "halo", "dunia\tx")
            self.assertEqual(code, 0)
            import time
            for _ in range(20):
                if logsrv.lines:
                    break
                time.sleep(0.05)
            self.assertEqual(logsrv.lines, ["cli\tW\tuji\thalo dunia x"])
        finally:
            logsrv.close()


class UnreachableTests(unittest.TestCase):
    def test_no_server_exit_3(self):
        code, out, err = run_cli("ping")
        self.assertEqual(code, 3)
        self.assertIn("tidak berjalan", err)
        self.assertEqual(run_cli("play", "/sdcard/a.mp4")[0], 3)
        self.assertEqual(run_cli("log", "halo")[0], 3)


class GarbageReplyTests(unittest.TestCase):
    def test_unknown_reply_exit_1(self):
        srv = FakeServer(CMD, lambda line: "AMBURADUL\n")
        try:
            code, _, err = run_cli("ping")
            self.assertEqual(code, 1)
            self.assertIn("balasan tidak dikenal", err)
        finally:
            srv.close()

    def test_close_without_reply_exit_1(self):
        srv = FakeServer(CMD, lambda line: "")
        try:
            self.assertEqual(run_cli("ping")[0], 1)
        finally:
            srv.close()


if __name__ == "__main__":
    unittest.main(verbosity=2)
''',
}

INSTALL_CLI_FN = r"""    /**
     * Pasang CLI `molina` di Debian (jembatan Debian -> aplikasi, lihat cmd/CommandServer).
     * Hanya menulis bila isinya berbeda; gagal tulis dicatat, tidak menghentikan sesi.
     */
    fun installCli(ctx: Context, rootfsDir: File) {
        val files = listOf(
            Triple("molina/molina-cli.sh", "usr/local/bin/molina", true),
            Triple("molina/molina-cli.py", "usr/local/lib/molina/molina_cli.py", false),
        )
        for ((asset, rel, exec) in files) {
            runCatching {
                val data = ctx.assets.open(asset).use { it.readBytes() }
                val f = File(rootfsDir, rel)
                f.parentFile?.mkdirs()
                if (!f.isFile || !f.readBytes().contentEquals(data)) f.writeBytes(data)
                if (exec && !f.canExecute()) f.setExecutable(true, false)
            }.onFailure {
                LogBus.log("sistem", LogLevel.Warn, "Shell", "Gagal memasang CLI molina ($rel): ${it.message}")
            }
        }
    }

"""

# (berkas, penanda idempoten, jangkar, teks sisip, "before"/"after")
PATCHES = [
    ("app/src/main/java/com/molina/studio/MolinaApp.kt", "CommandServer.start",
     "        MpvControl.install(this)\n",
     "        com.molina.studio.cmd.CommandServer.start(this)\n", "after"),
    ("app/src/main/java/com/molina/studio/terminal/TerminalSessions.kt", "MolinaShell.installCli",
     "        MolinaShell.installAptConf(rootfsDir)\n",
     "        MolinaShell.installCli(ctx, rootfsDir)\n", "after"),
    ("app/src/main/java/com/molina/studio/terminal/MolinaShell.kt", "fun installCli",
     '    private const val APT_CONF = """',
     INSTALL_CLI_FN, "before"),
]

def read(p):
    with open(os.path.join(ROOT, p), encoding="utf-8", newline="") as fh:
        return fh.read()

# 1) periksa semua jangkar dulu: gagal = tidak ada yang ditulis
plan = []
for path, marker, anchor, text, where in PATCHES:
    src = read(path)
    if marker in src:
        plan.append((path, None, "sudah ada"))
        continue
    if src.count(anchor) != 1:
        sys.exit("GAGAL: jangkar tidak ditemukan tepat satu kali di %s (kode berubah?). Tidak ada yang ditulis." % path)
    new = src.replace(anchor, anchor + text) if where == "after" else src.replace(anchor, text + anchor)
    plan.append((path, new, "dipatch"))

# 2) tulis
for rel, content in NEW_FILES.items():
    p = os.path.join(ROOT, rel)
    old = open(p, encoding="utf-8", newline="").read() if os.path.isfile(p) else None
    if old == content:
        print("lewati (sama):", rel)
    else:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8", newline="") as fh:
            fh.write(content)
        print("ditulis:", rel)
    if rel in EXEC:
        os.chmod(p, os.stat(p).st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
for path, new, status in plan:
    if new is not None:
        with open(os.path.join(ROOT, path), "w", encoding="utf-8", newline="") as fh:
            fh.write(new)
    print("%s: %s" % (status, path))
print("SELESAI. Commit: feat(cmd): jembatan CLI molina (Debian -> aplikasi)")
