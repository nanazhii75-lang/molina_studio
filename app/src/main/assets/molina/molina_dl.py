#!/usr/bin/env python3
"""Molina DL v2 - pengunduh video/audio interaktif (backend: yt-dlp).

Instalasi dependensi di Debian (Molina Terminal):
    apt update && apt install -y python3 ffmpeg yt-dlp
Alternatif bila paket yt-dlp di apt terlalu lama:
    apt install -y pipx && pipx install yt-dlp && pipx ensurepath

Jalankan:  python3 molina_dl.py   (atau lewat Script Execution di Settings)

Fitur: indikator langsung gaya IDM (bar, persen, kecepatan, ETA, ukuran), antrian banyak URL,
ringkasan hasil, batal dengan Ctrl+C (unduhan sebagian dilanjutkan saat URL yang sama diunduh lagi).

Folder simpan (mengikuti struktur penyimpanan Molina):
    Video -> /sdcard/Molina/Media/Video
    Audio -> /sdcard/Molina/Media/Audio
Pengaturan lewat environment (opsional):
    MOLINA_ROOT      akar folder aplikasi (bawaan /sdcard/Molina)
    MOLINA_DL_DIR    satu folder untuk video DAN audio (menimpa semuanya)
    MOLINA_DL_ASCII  1 = bar dan tanda ASCII (bila font terminal tidak menampilkan blok)
    NO_COLOR         tanpa warna
Bila /sdcard/Molina tidak terlihat di Debian (belum di-bind), skrip memakai folder cadangan
/sdcard/molina-studio/unduhan/{Video,Audio}, atau ~/unduhan/{Video,Audio}, dan memberi peringatan.
"""
import json
import os
import re
import shutil
import subprocess
import sys
import time
from collections import deque

URL_RE = re.compile(r"^https?://[^\s/$.?#][^\s]*$", re.IGNORECASE)
VIDEO_EXTS = ("mp4", "mkv")
AUDIO_FORMATS = ("mp3", "m4a", "flac")
BITRATES = ("128k", "192k", "320k")
FALLBACK_HEIGHTS = (1080, 720, 480, 360)
DEFAULT_ROOT = "/sdcard/Molina"
LEGACY_BASE = "/sdcard/molina-studio/unduhan"
MIN_FREE_MB = 500

ASCII = os.environ.get("MOLINA_DL_ASCII") == "1"
BAR_FULL, BAR_EMPTY = ("#", "-") if ASCII else ("\u2588", "\u2591")
MARK_OK, MARK_ERR, MARK_STEP = ("OK", "X", ">>") if ASCII else ("\u2714", "\u2718", "\u00bb")
BRAND_SGR = os.environ.get("MOLINA_BRAND_SGR") or "1;33"  # emas Molina (dikirim aplikasi), cadangan kuning

# Keluaran progres terstruktur yt-dlp (field kosong = "NA"). Judul sengaja tidak dimasukkan (bisa memuat "|").
PROGRESS_ARGS = [
    "--quiet", "--no-warnings", "--progress", "--newline",
    "--progress-template",
    "download:MDL|%(progress.status)s|%(progress.downloaded_bytes)s|%(progress.total_bytes)s|"
    "%(progress.total_bytes_estimate)s|%(progress.speed)s|%(progress.eta)s|"
    "%(progress.fragment_index)s|%(progress.fragment_count)s|%(info.vcodec)s|%(info.acodec)s",
    "--progress-template",
    "postprocess:MDLPP|%(progress.status)s|%(progress.postprocessor)s",
]
PP_NAMES = {
    "Merger": "Menggabungkan video + audio",
    "ExtractAudio": "Mengonversi ke audio",
    "FFmpegMetadata": "Menyematkan metadata",
    "FFmpegVideoConvertor": "Mengonversi format video",
}


# ---------------------------------------------------------------- util tampilan

def ask(prompt):
    """input() yang mengembalikan None saat Ctrl+D (EOF) atau Ctrl+C."""
    try:
        return input(prompt).strip()
    except (EOFError, KeyboardInterrupt):
        print()
        return None


def choose(title, options, default=1):
    """Tampilkan daftar bernomor, kembalikan indeks (0-based) atau None bila dibatalkan."""
    print(f"\n{title}")
    for i, label in enumerate(options, 1):
        print(f"  {i}. {label}")
    while True:
        raw = ask(f"Pilih [1-{len(options)}] (Enter = {default}, 0 = batal): ")
        if raw is None or raw == "0":
            return None
        if raw == "":
            return default - 1
        if raw.isdigit() and 1 <= int(raw) <= len(options):
            return int(raw) - 1
        print("Pilihan tidak valid.")


def human_size(n):
    n = float(n)
    for unit in ("B", "KB", "MB", "GB"):
        if n < 1024 or unit == "GB":
            return f"{n:.0f} {unit}" if unit == "B" else f"{n:.1f} {unit}"
        n /= 1024


def human_eta(sec):
    sec = max(0, int(sec))
    h, rest = divmod(sec, 3600)
    m, s = divmod(rest, 60)
    return f"{h}:{m:02d}:{s:02d}" if h else f"{m:02d}:{s:02d}"


def clip(text, width):
    return text if len(text) <= width else text[: max(1, width - 1)] + "\u2026"


def term_cols():
    return max(30, shutil.get_terminal_size((60, 24)).columns - 1)


# ---------------------------------------------------------------- progres

def _num(s, cast=float):
    if s in (None, "", "NA", "None"):
        return None
    try:
        return cast(s)
    except ValueError:
        return None


def parse_event(line):
    """Baris keluaran yt-dlp -> dict peristiwa, atau None bila bukan baris progres."""
    line = line.rstrip("\r\n")
    if line.startswith("MDL|"):
        p = line.split("|")
        if len(p) < 11:
            return None
        total = _num(p[3], lambda x: int(float(x)))
        if total is None:
            total = _num(p[4], lambda x: int(float(x)))
        return {
            "kind": "dl", "status": p[1],
            "done": _num(p[2], lambda x: int(float(x))) or 0,
            "total": total,
            "speed": _num(p[5]),
            "eta": _num(p[6], lambda x: int(float(x))),
            "frag": _num(p[7], int), "frags": _num(p[8], int),
            "vcodec": p[9], "acodec": p[10],
        }
    if line.startswith("MDLPP|"):
        p = line.split("|")
        if len(p) < 3:
            return None
        return {"kind": "pp", "status": p[1], "name": p[2]}
    return None


def stream_label(vcodec, acodec):
    has_v = vcodec not in ("NA", "none", "None", "")
    has_a = acodec not in ("NA", "none", "None", "")
    if has_v:
        return "Video"
    if has_a:
        return "Audio"
    return "Berkas"


class Meter:
    """Indikator unduhan satu baris gaya IDM: bar, persen, kecepatan, ETA, ukuran."""

    def __init__(self):
        self.tty = sys.stdout.isatty()
        self.color = self.tty and not os.environ.get("NO_COLOR")
        self.label = None
        self.line_open = False
        self.step = -1
        self.pp_shown = set()
        self.t0 = time.time()

    def paint(self, text, sgr=BRAND_SGR):
        return f"\x1b[{sgr}m{text}\x1b[0m" if self.color else text

    def _emit(self, text):
        sys.stdout.write("\r\x1b[2K" + text)
        sys.stdout.flush()
        self.line_open = True

    def close(self):
        if self.line_open:
            sys.stdout.write("\n")
            sys.stdout.flush()
            self.line_open = False

    def _line(self, label, pct, done, total, speed, eta):
        cols = term_cols()
        parts = []
        if pct is not None:
            parts.append(f"{pct:5.1f}%")
        if speed:
            parts.append(f"{human_size(speed)}/s")
        if eta is not None:
            parts.append(f"ETA {human_eta(eta)}")
        if total:
            parts.append(f"{human_size(done)} / {human_size(total)}")
        elif done:
            parts.append(human_size(done))
        while True:
            tail = " ".join(parts)
            width = cols - len(label) - len(tail) - 4
            if width >= 10 or len(parts) <= 1:
                break
            parts.pop()  # buang yang paling akhir dulu (ukuran, lalu ETA, ...)
        width = max(8, width)
        if pct is None:  # ukuran tak diketahui: penanda bergerak
            pos = int(time.time() * 6) % (width - 2)
            bar = BAR_EMPTY * pos + BAR_FULL * 3 + BAR_EMPTY * (width - pos - 3)
            bar = bar[:width]
        else:
            filled = int(width * max(0.0, min(100.0, pct)) / 100)
            bar = BAR_FULL * filled + BAR_EMPTY * (width - filled)
        return f"{self.paint(label)} [{self.paint(bar)}] {tail}"

    def handle(self, ev):
        if ev["kind"] == "pp":
            self._handle_pp(ev)
            return
        label = stream_label(ev["vcodec"], ev["acodec"])
        if label != self.label:
            self.close()
            self.label, self.step = label, -1
        done, total = ev["done"], ev["total"]
        pct = None
        if total:
            pct = min(100.0, done * 100.0 / total)
        elif ev["frags"]:
            pct = min(100.0, (ev["frag"] or 0) * 100.0 / ev["frags"])
        if ev["status"] == "finished":
            text = f"{self.paint(MARK_OK)} {label} selesai ({human_size(done)})"
            if self.tty:
                self._emit(text)
                self.close()
            else:
                print(text)
            self.label = None
            return
        if ev["status"] != "downloading":
            return
        if self.tty:
            self._emit(self._line(label, pct, done, total, ev["speed"], ev["eta"]))
        else:  # bukan terminal (dialihkan ke berkas): satu baris tiap 10%
            step = int((pct or 0) // 10)
            if step != self.step:
                self.step = step
                print(f"{label} {pct or 0:.0f}% {human_size(done)}")

    def _handle_pp(self, ev):
        text = PP_NAMES.get(ev["name"])
        if ev["status"] != "started" or not text or ev["name"] in self.pp_shown:
            return
        self.pp_shown.add(ev["name"])
        self.close()
        print(f"{self.paint(MARK_STEP)} {text}...")


# ---------------------------------------------------------------- lingkungan

def find_ytdlp():
    """Perintah dasar yt-dlp: binary di PATH, atau modul python bila terpasang lewat pip."""
    exe = shutil.which("yt-dlp")
    if exe:
        return [exe]
    try:
        import yt_dlp  # noqa: F401
        return [sys.executable, "-m", "yt_dlp"]
    except ImportError:
        return None


def ensure_dir(path):
    try:
        os.makedirs(path, exist_ok=True)
        return os.access(path, os.W_OK)
    except OSError:
        return False


def resolve_dirs():
    """Kembalikan (folder_video, folder_audio, peringatan atau None).

    Akar /sdcard/Molina dibuat oleh aplikasi, jadi TIDAK dibuat di sini: bila belum ada berarti
    belum ter-bind ke Debian, dan membuatnya hanya menghasilkan folder tersembunyi di dalam rootfs.
    """
    custom = os.environ.get("MOLINA_DL_DIR")
    if custom:
        base = os.path.expanduser(custom)
        if ensure_dir(base):
            return base, base, None

    root = os.environ.get("MOLINA_ROOT", DEFAULT_ROOT)
    if os.path.isdir(root):
        video = os.path.join(root, "Media", "Video")
        audio = os.path.join(root, "Media", "Audio")
        if ensure_dir(video) and ensure_dir(audio):
            return video, audio, None

    if os.path.isdir(os.path.dirname(LEGACY_BASE)):
        base, where = LEGACY_BASE, "folder cadangan molina-studio/unduhan"
    else:
        base, where = os.path.expanduser("~/unduhan"), "folder cadangan ~/unduhan (tidak terlihat di Android)"
    video, audio = os.path.join(base, "Video"), os.path.join(base, "Audio")
    ensure_dir(video)
    ensure_dir(audio)
    warn = (f"{root} tidak terlihat dari Debian, jadi unduhan masuk ke {where}.\n"
            "  Aktifkan 'Akses penyimpanan penuh' di Setelan Terminal lalu buka sesi baru.")
    return video, audio, warn


def free_space_low(path):
    try:
        return shutil.disk_usage(path).free < MIN_FREE_MB * 1024 * 1024
    except OSError:
        return False


# ---------------------------------------------------------------- yt-dlp

def fetch_info(ytdlp, url):
    """Metadata video. Mengembalikan (info, None) atau (None, pesan_galat). Sekaligus memvalidasi URL."""
    try:
        res = subprocess.run(
            ytdlp + ["-J", "--no-playlist", "--no-warnings", url],
            capture_output=True, text=True, timeout=90,
        )
    except subprocess.TimeoutExpired:
        return None, "Waktu habis saat mengambil info. Periksa koneksi internet."
    except KeyboardInterrupt:
        return None, "Dibatalkan."
    if res.returncode != 0:
        lines = [ln for ln in res.stderr.strip().splitlines() if ln.strip()]
        reason = lines[-1] if lines else "URL tidak didukung atau koneksi bermasalah."
        return None, reason.replace("ERROR: ", "")
    try:
        info = json.loads(res.stdout)
    except json.JSONDecodeError:
        return None, "Respons yt-dlp tidak bisa dibaca."
    if info.get("_type") in ("playlist", "multi_video"):
        return None, "URL ini berupa playlist/kanal. Tempel URL satu video saja."
    return info, None


def available_heights(info):
    heights = {
        f["height"] for f in info.get("formats", [])
        if f.get("height") and f.get("vcodec") not in (None, "none")
    }
    return sorted(heights, reverse=True)[:8] or list(FALLBACK_HEIGHTS)


def video_format(height, ext, has_ffmpeg):
    """Selector format yt-dlp. Tanpa ffmpeg hanya format gabungan (satu berkas) yang aman."""
    cap = f"[height<={height}]" if height else ""
    if not has_ffmpeg:
        return f"b{cap}/b"
    if ext == "mp4":
        return f"bv*{cap}[ext=mp4]+ba[ext=m4a]/bv*{cap}+ba/b{cap}"
    return f"bv*{cap}+ba/b{cap}"


def common_args(outdir):
    return PROGRESS_ARGS + [
        "--no-playlist", "--embed-metadata",
        "--no-mtime",             # waktu berkas = waktu unduh, bukan tanggal unggah (terurut di file manager)
        "--windows-filenames",    # nama aman untuk storage bersama Android
        "--retries", "10", "--fragment-retries", "10", "--concurrent-fragments", "4",
        "-o", os.path.join(outdir, "%(title).150B [%(id)s].%(ext)s"),
    ]


def build_video_cmd(ytdlp, url, outdir, height, ext, has_ffmpeg):
    cmd = ytdlp + common_args(outdir) + ["-f", video_format(height, ext, has_ffmpeg)]
    if has_ffmpeg:
        cmd += ["--merge-output-format", ext]
    return cmd + [url]


def build_audio_cmd(ytdlp, url, outdir, fmt, bitrate):
    cmd = ytdlp + common_args(outdir) + ["-f", "bestaudio/best", "-x", "--audio-format", fmt]
    if bitrate:  # FLAC lossless: tanpa bitrate
        cmd += ["--audio-quality", bitrate.upper()]
    return cmd + [url]


def snapshot(folder):
    try:
        return {e.name for e in os.scandir(folder) if e.is_file()}
    except OSError:
        return set()


def stop_process(proc):
    if proc is None or proc.poll() is not None:
        return
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except subprocess.TimeoutExpired:
        proc.kill()
        proc.wait()


def run_download(cmd, outdir):
    """Jalankan yt-dlp dengan indikator langsung. Mengembalikan dict hasil:
    ok, cancelled, files [(nama, ukuran)], error, secs."""
    if free_space_low(outdir):
        print(f"Peringatan: ruang kosong kurang dari {MIN_FREE_MB} MB, unduhan bisa gagal.")
    before = snapshot(outdir)
    meter = Meter()
    last = deque(maxlen=6)
    t0 = time.time()
    proc = None
    result = {"ok": False, "cancelled": False, "files": [], "error": "", "secs": 0}
    try:
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, errors="replace", bufsize=1)
        for line in proc.stdout:
            ev = parse_event(line)
            if ev:
                meter.handle(ev)
            elif line.strip():
                last.append(line.strip())
        rc = proc.wait()
    except KeyboardInterrupt:
        meter.close()
        stop_process(proc)
        result.update(cancelled=True, error="dibatalkan", secs=time.time() - t0)
        print("Dibatalkan. Bagian yang sudah terunduh dilanjutkan bila URL yang sama diunduh lagi.")
        return result
    except OSError as e:
        meter.close()
        result.update(error=f"gagal menjalankan yt-dlp: {e}", secs=time.time() - t0)
        return result
    finally:
        stop_process(proc)
    meter.close()
    result["secs"] = time.time() - t0
    if rc != 0:
        errs = [ln for ln in last if ln.startswith("ERROR")]
        reason = (errs[-1] if errs else (last[-1] if last else f"kode {rc}")).replace("ERROR: ", "")
        result["error"] = reason
        return result
    fresh = [n for n in sorted(snapshot(outdir) - before)
             if ".part" not in n and not n.endswith((".ytdl", ".temp"))]
    for name in fresh:
        try:
            size = os.path.getsize(os.path.join(outdir, name))
        except OSError:
            size = 0
        result["files"].append((name, size))
    result["ok"] = True
    return result


def print_result(res, outdir):
    if res["cancelled"]:
        return
    if not res["ok"]:
        print(f"{MARK_ERR} Gagal: {res['error']}")
        print("  Penyebab umum: koneksi terputus, video privat/dibatasi wilayah, atau yt-dlp perlu diperbarui.")
        return
    if not res["files"]:
        print(f"{MARK_OK} Berkas ini sudah ada di {outdir}, tidak diunduh ulang.")
        return
    cols = term_cols()
    for name, size in res["files"]:
        print(f"{MARK_OK} {clip(name, cols - 14)} ({human_size(size)})")
    print(f"  Folder: {outdir}  |  {human_eta(res['secs'])}")


def show_summary(results):
    cols = term_cols()
    print("\n=== Ringkasan ===")
    ok_count, total_size, total_secs = 0, 0, 0
    for title, res in results:
        total_secs += res["secs"]
        if res["ok"]:
            ok_count += 1
            size = sum(s for _, s in res["files"])
            total_size += size
            note = human_size(size) if res["files"] else "sudah ada"
            print(f"{MARK_OK} {clip(title, cols - 14)} ({note})")
        else:
            print(f"{MARK_ERR} {clip(title, cols - 4)}")
            print(f"    {clip(res['error'], cols - 4)}")
    print(f"{ok_count}/{len(results)} berhasil | {human_size(total_size)} | {human_eta(total_secs)}")


# ---------------------------------------------------------------- alur menu

def read_batch(ytdlp):
    """Kumpulkan satu atau banyak URL (antrian). Mengembalikan [(url, info)], atau None bila dibatalkan."""
    items, seen = [], set()
    print("\nTempel URL video (boleh banyak, satu per baris atau dipisah spasi).")
    while True:
        hint = "mulai" if items else "kembali"
        raw = ask(f"URL #{len(items) + 1} (kosong = {hint}): ")
        if raw is None:
            return None
        if not raw:
            return items
        for url in raw.split():
            if url in seen:
                print("  Sudah ada di antrian.")
                continue
            if not URL_RE.match(url):
                print("  URL tidak valid. Awali dengan http:// atau https://")
                continue
            print("  Memeriksa URL...")
            info, err = fetch_info(ytdlp, url)
            if err:
                print(f"  Galat: {err}")
                continue
            seen.add(url)
            items.append((url, info))
            print(f"  + {clip(info.get('title', '(tanpa judul)'), term_cols() - 4)}  [antrian: {len(items)}]")


def run_queue(items, outdir, builder):
    """Unduh satu per satu dengan penanda [i/n]; berhenti bila dibatalkan. builder(url) -> perintah."""
    results = []
    for i, (url, info) in enumerate(items, 1):
        title = info.get("title", "(tanpa judul)")
        print(f"\n[{i}/{len(items)}] {clip(title, term_cols() - 8)}")
        res = run_download(builder(url), outdir)
        print_result(res, outdir)
        results.append((title, res))
        if res["cancelled"]:
            break
    if len(results) > 1:
        show_summary(results)


def flow_video(ytdlp, has_ffmpeg, outdir):
    items = read_batch(ytdlp)
    if not items:
        return
    heights = available_heights(items[0][1])  # untuk banyak URL: daftar milik URL pertama
    labels = ["Kualitas terbaik"] + [f"{h}p" for h in heights]
    idx = choose("Resolusi:", labels)
    if idx is None:
        return
    height = None if idx == 0 else heights[idx - 1]
    e = choose("Format video:", [x.upper() for x in VIDEO_EXTS])
    if e is None:
        return
    if not has_ffmpeg:
        print("Peringatan: ffmpeg tidak ditemukan, hanya format gabungan yang bisa diunduh "
              "(resolusi tinggi bisa tidak tersedia). Pasang: apt install -y ffmpeg")
    ext = VIDEO_EXTS[e]
    run_queue(items, outdir, lambda u: build_video_cmd(ytdlp, u, outdir, height, ext, has_ffmpeg))


def flow_audio(ytdlp, has_ffmpeg, outdir):
    if not has_ffmpeg:
        print("\nUnduh audio butuh ffmpeg untuk konversi. Pasang: apt install -y ffmpeg")
        return
    items = read_batch(ytdlp)
    if not items:
        return
    f = choose("Format audio:", ["MP3", "M4A", "FLAC (lossless)"])
    if f is None:
        return
    fmt = AUDIO_FORMATS[f]
    bitrate = None
    if fmt != "flac":
        b = choose("Kualitas (bitrate):", list(BITRATES), default=2)
        if b is None:
            return
        bitrate = BITRATES[b]
    run_queue(items, outdir, lambda u: build_audio_cmd(ytdlp, u, outdir, fmt, bitrate))


def main():
    ytdlp = find_ytdlp()
    if not ytdlp:
        print("yt-dlp tidak ditemukan. Pasang dengan: apt update && apt install -y yt-dlp ffmpeg")
        return 1
    has_ffmpeg = shutil.which("ffmpeg") is not None
    video_dir, audio_dir, warn = resolve_dirs()
    while True:
        print("\n=== Molina DL ===")
        print("1. Unduh Video")
        print("2. Unduh Audio Saja")
        print("3. Keluar")
        print(f"Video -> {video_dir}")
        print(f"Audio -> {audio_dir}")
        if warn:
            print(f"Peringatan: {warn}")
        if not has_ffmpeg:
            print("Peringatan: ffmpeg tidak ada (audio dan video resolusi tinggi dibatasi)")
        choice = ask("Pilih [1-3]: ")
        if choice is None or choice == "3":
            print("Sampai jumpa.")
            return 0
        if choice == "1":
            flow_video(ytdlp, has_ffmpeg, video_dir)
        elif choice == "2":
            flow_audio(ytdlp, has_ffmpeg, audio_dir)
        else:
            print("Pilihan tidak valid.")


if __name__ == "__main__":
    sys.exit(main())
