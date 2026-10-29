#!/usr/bin/env python3
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
  molina open <berkas>...        buka berkas (maks 10) di editor Code
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


def cmd_open(args):
    if not args:
        sys.stderr.write("molina: open butuh minimal satu berkas\n")
        return 2
    if len(args) > 10:
        sys.stderr.write("molina: open maksimal 10 berkas sekaligus\n")
        return 2
    paths = []
    for a in args:
        p = os.path.abspath(os.path.expanduser(a))
        if "\t" in p or "\n" in p or "\r" in p:
            sys.stderr.write("molina: nama berkas mengandung TAB atau baris baru (tidak didukung)\n")
            return 2
        paths.append(p)
    return run("open", "\t".join(paths))


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
    if verb == "open":
        return cmd_open(rest)
    if verb == "log":
        return cmd_log(rest)
    sys.stderr.write("molina: perintah tidak dikenal: %s (coba: molina help)\n" % verb)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
