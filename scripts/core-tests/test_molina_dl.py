#!/usr/bin/env python3
# Uji molina_dl.py (v5). Jalankan: python3 -m unittest -v scripts/core-tests/test_molina_dl.py
# Unit murni selalu jalan. Uji integrasi memakai yt-dlp + ffmpeg asli terhadap server HTTP lokal
# (tanpa internet) dan dilewati otomatis bila alatnya tidak ada.
import functools
import importlib.util
import json
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import unittest
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.abspath(os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "molina", "molina_dl.py"))
_spec = importlib.util.spec_from_file_location("molina_dl", SCRIPT)
dl = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(dl)

HAVE_TOOLS = bool(dl.find_ytdlp()) and bool(shutil.which("ffmpeg")) and bool(shutil.which("ffprobe"))


class Recorder:
    """Pelapor palsu: mengumpulkan event."""
    interactive = False

    def __init__(self):
        self.events = []

    def update(self, status, **f):
        self.events.append(("update", status, f))

    def doctor(self, data):
        self.events.append(("doctor", data))

    def warn(self, code, msg):
        self.events.append(("warn", code))

    def hello(self):
        pass

    def statuses(self):
        return [e[1] for e in self.events if e[0] == "update"]


class ParseTests(unittest.TestCase):
    def test_parse_subs(self):
        self.assertEqual(dl.parse_subs("id,en"), (["id", "en"], None))
        self.assertEqual(dl.parse_subs(" id , id ,pt-BR"), (["id", "pt-BR"], None))
        self.assertEqual(dl.parse_subs(""), ([], None))
        for bad in ("id;rm", "../x", "en.*", "a" * 30, "--x", "i"):
            self.assertTrue(dl.parse_subs(bad)[1], bad)
        self.assertTrue(dl.parse_subs("id,en,fr,de,es,ja")[1])

    def test_rate_regex(self):
        for ok in ("500K", "2M", "1.5M", "300", "2m"):
            self.assertTrue(dl.RATE_RE.match(ok), ok)
        for bad in ("0", "-1", "2MB", "abc", "1 M", ""):
            self.assertFalse(dl.RATE_RE.match(bad), bad)

    def test_stage_map(self):
        self.assertEqual(dl.PP_STAGES["EmbedSubtitle"], "subs")
        self.assertEqual(dl.PP_STAGES["EmbedThumbnail"], "cover")

    def test_build_cmd_subs_cover_rate(self):
        base = {"mode": "video", "ext": "mp4", "height": None}
        c = dl.build_cmd(["yt-dlp"], "http://x/1", "/o", base, True)
        for flag in ("--write-subs", "--embed-subs", "--limit-rate", "--embed-thumbnail"):
            self.assertNotIn(flag, c)
        c = dl.build_cmd(["yt-dlp"], "http://x/1", "/o", dict(base, subs=["id", "en"], limit="2M"), True)
        self.assertEqual(c[c.index("--sub-langs") + 1], "id,en")
        self.assertIn("--embed-subs", c)
        self.assertIn("no-keep-subs", c)  # supaya berkas subtitle tidak tersisa setelah disematkan
        self.assertEqual(c[c.index("--limit-rate") + 1], "2M")
        c = dl.build_cmd(["yt-dlp"], "http://x/1", "/o", dict(base, subs=["id"], subs_mode="file"), True)
        self.assertNotIn("--embed-subs", c)
        self.assertIn("--write-subs", c)
        c = dl.build_cmd(["yt-dlp"], "http://x/1", "/o", dict(base, subs=["id"]), False)  # tanpa ffmpeg
        self.assertNotIn("--embed-subs", c)
        self.assertNotIn("--convert-subs", c)
        self.assertIn("--write-subs", c)
        self.assertEqual(c[-1], "http://x/1")

    def test_build_cmd_cover(self):
        a = {"mode": "audio", "afmt": "mp3", "bitrate": "192k"}
        self.assertIn("--embed-thumbnail", dl.build_cmd(["y"], "http://x/1", "/o", a, True))
        self.assertNotIn("--embed-thumbnail", dl.build_cmd(["y"], "http://x/1", "/o", dict(a, cover=False), True))
        self.assertNotIn("--embed-thumbnail", dl.build_cmd(["y"], "http://x/1", "/o", a, False))
        self.assertNotIn("--write-subs", dl.build_cmd(["y"], "http://x/1", "/o", dict(a, subs=["id"]), True))

    def test_cover_supported(self):
        self.assertTrue(dl.cover_supported("mp3"))
        orig_which, orig_mut = dl.shutil.which, dl.has_mutagen
        try:
            dl.shutil.which = lambda n: None
            dl.has_mutagen = lambda: False
            self.assertFalse(dl.cover_supported("m4a"))
            self.assertFalse(dl.cover_supported("flac"))
            dl.has_mutagen = lambda: True
            self.assertTrue(dl.cover_supported("flac"))
        finally:
            dl.shutil.which, dl.has_mutagen = orig_which, orig_mut

    def test_origin(self):
        self.assertIsNone(dl.ytdlp_origin(None))
        self.assertEqual(dl.ytdlp_origin(["python3", "-m", "yt_dlp"]), "pip")
        self.assertEqual(dl.ytdlp_origin(["/usr/local/bin/yt-dlp"]), "pip")


class SidecarUnitTests(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp(prefix="mdl-side-")

    def tearDown(self):
        shutil.rmtree(self.d, ignore_errors=True)

    def put(self, name, text="x"):
        p = os.path.join(self.d, name)
        with open(p, "w", encoding="utf-8") as f:
            f.write(text)
        return p

    def test_build_cmd_sidecar_flags(self):
        base = {"mode": "video", "ext": "mp4", "height": None}
        c = dl.build_cmd(["y"], "http://x/1", "/o", base, True)
        for flag in ("--write-thumbnail", "--write-info-json"):
            self.assertNotIn(flag, c)  # bawaan mati: folder unduhan tetap bersih
        c = dl.build_cmd(["y"], "http://x/1", "/o", dict(base, sidecar=True), True)
        self.assertIn("--write-thumbnail", c)
        self.assertIn("--write-info-json", c)
        self.assertEqual(c[c.index("--convert-thumbnails") + 1], "jpg")
        c = dl.build_cmd(["y"], "http://x/1", "/o", dict(base, sidecar=True), False)  # tanpa ffmpeg
        self.assertIn("--write-thumbnail", c)
        self.assertNotIn("--convert-thumbnails", c)
        a = {"mode": "audio", "afmt": "mp3", "bitrate": "192k", "sidecar": True}
        c = dl.build_cmd(["y"], "http://x/1", "/o", a, True)
        self.assertNotIn("--write-info-json", c)
        self.assertNotIn("--write-thumbnail", c)

    def test_slim_info_json(self):
        big = {"id": "abc", "title": "Pelajaran – Bab 1", "uploader": "Kanal", "duration": 61, "upload_date": "20240131",
               "description": "d" * 5000, "tags": ["t%d" % i for i in range(50)] + [1],
               "formats": [{"url": "http://x/%d" % i, "format_id": str(i)} for i in range(2000)],
               "http_headers": {"User-Agent": "x"}, "view_count": 0, "like_count": None, "categories": []}
        p = self.put("a.info.json", json.dumps(big))
        before = os.path.getsize(p)
        self.assertTrue(dl.slim_info_json(p))
        with open(p, encoding="utf-8") as f:
            slim = json.load(f)
        self.assertLess(os.path.getsize(p), min(before // 10, 6000))
        for gone in ("formats", "http_headers", "like_count", "categories"):
            self.assertNotIn(gone, slim)
        self.assertEqual(slim["title"], "Pelajaran – Bab 1")
        self.assertEqual(len(slim["description"]), dl.INFO_DESC_MAX)
        self.assertEqual(len(slim["tags"]), dl.INFO_TAGS_MAX)
        self.assertEqual(slim["view_count"], 0)  # nol tetap disimpan
        self.assertEqual(slim["_molina_slim"], 1)
        with open(p, "rb") as f:
            first = f.read()
        self.assertIn("Pelajaran – Bab 1".encode("utf-8"), first)  # unicode tidak di-escape
        self.assertTrue(dl.slim_info_json(p))
        with open(p, "rb") as f:
            self.assertEqual(f.read(), first)  # idempoten
        self.assertFalse(os.path.exists(p + ".tmp"))

    def test_slim_info_json_bad_input(self):
        p = self.put("b.info.json", "{rusak")
        self.assertFalse(dl.slim_info_json(p))
        with open(p, encoding="utf-8") as f:
            self.assertEqual(f.read(), "{rusak")  # berkas tidak disentuh
        self.assertFalse(dl.slim_info_json(self.put("c.info.json", "[1]")))
        self.assertFalse(dl.slim_info_json(os.path.join(self.d, "tidak-ada.info.json")))

    def test_find_sidecars(self):
        v = self.put("Mr. Film [abc].mp4")
        self.assertEqual(dl.find_sidecars(v), [])
        self.put("Mr. Film [abc].webp")
        self.put("Mr. Film [abc].info.json", "{}")
        self.assertEqual([n for n, _p, _s in dl.find_sidecars(v)], ["Mr. Film [abc].webp", "Mr. Film [abc].info.json"])
        self.put("Mr. Film [abc].jpg")
        names = [n for n, _p, _s in dl.find_sidecars(v)]
        self.assertEqual(names, ["Mr. Film [abc].jpg", "Mr. Film [abc].info.json"])  # satu poster saja, jpg lebih dulu
        self.put("Lain [zzz].jpg")  # milik video lain tidak ikut
        self.assertEqual(len(dl.find_sidecars(v)), 2)

    def test_collect_sidecars_slims_and_skips_non_video(self):
        v = self.put("V [a].mp4")
        srt = self.put("V [a].id.srt")
        self.put("V [a].jpg")
        info = self.put("V [a].info.json", json.dumps({"id": "a", "title": "V", "formats": [{"x": "y" * 3000}]}))
        result = [(os.path.basename(p), p, 1) for p in (v, srt)]
        got = dl.collect_sidecars(result)
        self.assertEqual([n for n, _p, _s in got], ["V [a].jpg", "V [a].info.json"])
        self.assertEqual(got[1][2], os.path.getsize(info))  # ukuran yang dilaporkan = setelah diringkas
        self.assertLess(got[1][2], 300)

    def test_json_done_sidecars_only_when_present(self):
        import contextlib
        import io
        rep = dl.JsonReporter()
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            rep.done(1, "/o", [("a.mp4", "/o/a.mp4", 5)], False)
            rep.done(2, "/o", [("a.mp4", "/o/a.mp4", 5)], False, [("a.jpg", "/o/a.jpg", 3)])
        first, second = [json.loads(ln) for ln in buf.getvalue().splitlines()]
        self.assertNotIn("sidecars", first)
        self.assertEqual(second["sidecars"], [{"name": "a.jpg", "path": "/o/a.jpg", "size": 3}])
        self.assertEqual(len(second["files"]), 1)


class UpdateTests(unittest.TestCase):
    def setUp(self):
        self.saved = (dl.run_capture, dl.pip_available, dl.find_ytdlp, dl.ytdlp_version, dl.ytdlp_origin)
        self.calls = []

    def tearDown(self):
        dl.run_capture, dl.pip_available, dl.find_ytdlp, dl.ytdlp_version, dl.ytdlp_origin = self.saved

    def stub(self, versions, outputs, pip=True):
        vs, outs = list(versions), list(outputs)
        dl.pip_available = lambda: pip
        dl.find_ytdlp = lambda: ["yt-dlp"]
        dl.ytdlp_version = lambda y: vs.pop(0)
        dl.ytdlp_origin = lambda y: "apt"

        def cap(cmd, timeout):
            self.calls.append(cmd)
            return outs.pop(0)
        dl.run_capture = cap

    def test_ok(self):
        self.stub(["2026.01.01", "2026.10.26"], [(0, "Successfully installed yt-dlp-2026.10.26")])
        rep = Recorder()
        self.assertEqual(dl.run_update(rep, ["yt-dlp"]), 0)
        self.assertEqual(rep.statuses(), ["start", "ok"])
        self.assertIn("--break-system-packages", self.calls[0])

    def test_latest(self):
        self.stub(["2026.10.26", "2026.10.26"], [(0, "Requirement already satisfied: yt-dlp")])
        rep = Recorder()
        self.assertEqual(dl.run_update(rep, ["yt-dlp"]), 0)
        self.assertEqual(rep.statuses(), ["start", "latest"])

    def test_shadowed_by_old_path(self):
        self.stub(["2026.01.01", "2026.01.01"], [(0, "Successfully installed yt-dlp-2026.10.26")])
        rep = Recorder()
        self.assertEqual(dl.run_update(rep, ["yt-dlp"]), 1)
        self.assertEqual(rep.statuses(), ["start", "failed"])

    def test_fail_uses_last_line(self):
        self.stub(["2026.01.01"], [(1, "x\nERROR: Could not find a version\n")])
        rep = Recorder()
        self.assertEqual(dl.run_update(rep, ["yt-dlp"]), 1)
        self.assertEqual(rep.events[-1][2]["msg"], "ERROR: Could not find a version")

    def test_old_pip_retries_without_flag(self):
        self.stub(["2026.01.01", "2026.10.26"], [(2, "no such option: --break-system-packages"), (0, "Successfully installed")])
        rep = Recorder()
        self.assertEqual(dl.run_update(rep, ["yt-dlp"]), 0)
        self.assertEqual(len(self.calls), 2)
        self.assertNotIn("--break-system-packages", self.calls[1])

    def test_no_pip(self):
        self.stub(["2026.01.01"], [], pip=False)
        rep = Recorder()
        self.assertEqual(dl.run_update(rep, ["yt-dlp"]), 1)
        self.assertEqual(rep.statuses(), ["failed"])
        self.assertEqual(self.calls, [])

    def test_maintenance_doctor_without_ytdlp_fails(self):
        class A:
            json = True
            update_ytdlp = False
            doctor = True
            dir = None
        saved_rep = dl.JsonReporter
        rec = Recorder()
        dl.JsonReporter = lambda: rec
        dl.find_ytdlp = lambda: None
        try:
            self.assertEqual(dl.run_maintenance(A, None), 1)
        finally:
            dl.JsonReporter = saved_rep
        data = [e for e in rec.events if e[0] == "doctor"][0][1]
        self.assertIn("no_ytdlp", data["issues"])
        self.assertIsNone(data["ytdlp"])


def run_script(*args, cwd=None, env=None):
    e = dict(os.environ, NO_COLOR="1")
    e.update(env or {})
    res = subprocess.run([sys.executable, SCRIPT, *args], capture_output=True, text=True, cwd=cwd, env=e, timeout=180)
    events = []
    for line in res.stdout.splitlines():
        if line.startswith("{"):
            events.append(json.loads(line))
    return res.returncode, events


def ev(events, name):
    return [e for e in events if e["ev"] == name]


class CliTests(unittest.TestCase):
    @unittest.skipUnless(dl.find_ytdlp(), "yt-dlp tidak ada")
    def test_bad_args_exit_2(self):
        rc, events = run_script("--json", "--subs", "id;rm", "http://127.0.0.1:1/x")
        self.assertEqual((rc, ev(events, "error")[0]["code"]), (2, "bad_subs"))
        rc, events = run_script("--json", "--limit-rate", "2MB", "http://127.0.0.1:1/x")
        self.assertEqual((rc, ev(events, "error")[0]["code"]), (2, "bad_rate"))

    @unittest.skipUnless(dl.find_ytdlp(), "yt-dlp tidak ada")
    def test_doctor_json(self):
        rc, events = run_script("--json", "--doctor")
        self.assertEqual(rc, 0)
        d = ev(events, "doctor")[0]
        for key in ("ytdlp", "ytdlp_method", "ffmpeg", "ffprobe", "mutagen", "python", "video_dir", "audio_dir",
                    "root_bound", "free_mb", "issues"):
            self.assertIn(key, d)
        self.assertEqual(events[0]["ev"], "hello")
        self.assertEqual(events[0]["version"], dl.SCRIPT_VERSION)

    def test_version(self):
        res = subprocess.run([sys.executable, SCRIPT, "--version"], capture_output=True, text=True)
        self.assertEqual(res.stdout.strip(), f"molina_dl {dl.SCRIPT_VERSION} (protokol 1)")


@unittest.skipUnless(HAVE_TOOLS, "butuh yt-dlp, ffmpeg, ffprobe")
class IntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.www = tempfile.mkdtemp(prefix="mdl-www-")
        cls.out = tempfile.mkdtemp(prefix="mdl-out-")

        def ff(*a):
            subprocess.run(["ffmpeg", "-y", "-v", "error", *a], check=True)
        w = cls.www
        ff("-f", "lavfi", "-i", "testsrc=size=640x360:rate=10", "-f", "lavfi", "-i", "sine=frequency=440", "-t", "2",
           "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", os.path.join(w, "clip.mp4"))
        ff("-f", "lavfi", "-i", "testsrc=size=640x360:rate=10", "-f", "lavfi", "-i", "sine=frequency=440", "-t", "2",
           "-c:v", "mpeg4", "-c:a", "mp3", os.path.join(w, "clip_old.mp4"))
        ff("-f", "lavfi", "-i", "testsrc=size=800x450", "-frames:v", "1", os.path.join(w, "thumb.jpg"))
        for lang, text in (("id", "Halo dunia"), ("en", "Hello world")):
            with open(os.path.join(w, f"sub_{lang}.vtt"), "w") as f:
                f.write(f"WEBVTT\n\n00:00:00.000 --> 00:00:01.500\n{text}\n")
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), functools.partial(SimpleHTTPRequestHandler, directory=w))
        cls.server.RequestHandlerClass.log_message = lambda *a, **k: None
        cls.base = f"http://127.0.0.1:{cls.server.server_address[1]}"
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()

        def page(name, clip, subs=True):
            tracks = "".join(f'<track kind="subtitles" srclang="{l}" src="{cls.base}/sub_{l}.vtt">' for l in ("id", "en")) if subs else ""
            with open(os.path.join(w, name), "w") as f:
                f.write(f'<html><head><title>Uji</title><meta property="og:image" content="{cls.base}/thumb.jpg"></head>'
                        f'<body><video controls><source src="{cls.base}/{clip}" type="video/mp4">{tracks}</video></body></html>')
        page("subs.html", "clip.mp4")
        page("old.html", "clip_old.mp4")
        page("nosubs.html", "clip.mp4", subs=False)

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        shutil.rmtree(cls.www, ignore_errors=True)
        shutil.rmtree(cls.out, ignore_errors=True)

    def outdir(self, name):
        d = os.path.join(self.out, name)
        shutil.rmtree(d, ignore_errors=True)
        return d

    def streams(self, path, kind=None):
        sel = ["-select_streams", kind] if kind else []
        res = subprocess.run(["ffprobe", "-v", "error", *sel, "-show_entries", "stream=codec_name,codec_type:stream_tags=language",
                              "-of", "json", path], capture_output=True, text=True)
        return json.loads(res.stdout)["streams"]

    def test_embed_subs_mp4(self):
        d = self.outdir("embed")
        rc, events = run_script("--json", "--subs", "id,en", "--dir", d, f"{self.base}/subs.html")
        self.assertEqual(rc, 0)
        self.assertIn("subs", [e["stage"] for e in ev(events, "stage")])
        path = ev(events, "done")[0]["files"][0]["path"]
        subs = self.streams(path, "s")
        self.assertEqual([s["codec_name"] for s in subs], ["mov_text", "mov_text"])
        self.assertEqual(os.listdir(d), [os.path.basename(path)])  # tanpa sisa berkas subtitle

    def test_file_mode_lists_sidecar(self):
        d = self.outdir("file")
        rc, events = run_script("--json", "--subs", "id", "--subs-mode", "file", "--dir", d, f"{self.base}/subs.html")
        self.assertEqual(rc, 0)
        names = [f["name"] for f in ev(events, "done")[0]["files"]]
        self.assertTrue(any(n.endswith(".mp4") for n in names))
        self.assertTrue(any(n.endswith(".id.srt") for n in names), names)
        self.assertEqual(self.streams(os.path.join(d, [n for n in names if n.endswith(".mp4")][0]), "s"), [])

    def test_recode_keeps_subtitles(self):
        d = self.outdir("recode")
        rc, events = run_script("--json", "--subs", "en", "--dir", d, f"{self.base}/old.html")
        self.assertEqual(rc, 0)
        self.assertIn("recode", [e["stage"] for e in ev(events, "stage")])
        path = ev(events, "done")[0]["files"][0]["path"]
        self.assertEqual([s["codec_name"] for s in self.streams(path)], ["h264", "aac", "mov_text"])

    def test_missing_subtitles_warn(self):
        d = self.outdir("nosubs")
        rc, events = run_script("--json", "--subs", "id", "--dir", d, f"{self.base}/nosubs.html")
        self.assertEqual(rc, 0)
        self.assertIn("no_subs", [w["code"] for w in ev(events, "warn")])

    def test_audio_mp3_has_cover_and_title(self):
        d = self.outdir("mp3")
        rc, events = run_script("--json", "--mode", "audio", "--audio-format", "mp3", "--dir", d, f"{self.base}/subs.html")
        self.assertEqual(rc, 0)
        self.assertIn("cover", [e["stage"] for e in ev(events, "stage")])
        path = ev(events, "done")[0]["files"][0]["path"]
        kinds = [(s["codec_type"], s["codec_name"]) for s in self.streams(path)]
        self.assertIn(("video", "mjpeg"), kinds)  # sampul
        tags = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format_tags=title", "-of", "csv=p=0", path],
                              capture_output=True, text=True).stdout.strip()
        self.assertTrue(tags.startswith("Uji"), tags)

    def test_no_cover_flag(self):
        d = self.outdir("nocover")
        rc, events = run_script("--json", "--mode", "audio", "--no-cover", "--dir", d, f"{self.base}/subs.html")
        self.assertEqual(rc, 0)
        self.assertNotIn("cover", [e["stage"] for e in ev(events, "stage")])
        path = ev(events, "done")[0]["files"][0]["path"]
        self.assertEqual([s["codec_type"] for s in self.streams(path)], ["audio"])

    def test_m4a_without_mutagen_warns_but_downloads(self):
        if dl.has_mutagen() or shutil.which("AtomicParsley"):
            self.skipTest("mutagen/AtomicParsley tersedia")
        d = self.outdir("m4a")
        rc, events = run_script("--json", "--mode", "audio", "--audio-format", "m4a", "--dir", d, f"{self.base}/subs.html")
        self.assertEqual(rc, 0)
        self.assertIn("no_cover", [w["code"] for w in ev(events, "warn")])
        self.assertEqual(len(ev(events, "done")[0]["files"]), 1)

    def test_limit_rate_accepted(self):
        d = self.outdir("rate")
        rc, events = run_script("--json", "--limit-rate", "5M", "--dir", d, f"{self.base}/nosubs.html")
        self.assertEqual(rc, 0)
        self.assertEqual(ev(events, "summary")[0]["ok"], 1)

    def test_subs_ignored_for_audio(self):
        d = self.outdir("audiosubs")
        rc, events = run_script("--json", "--mode", "audio", "--subs", "id", "--dir", d, f"{self.base}/subs.html")
        self.assertEqual(rc, 0)
        self.assertIn("subs_ignored", [w["code"] for w in ev(events, "warn")])

    def test_default_has_no_sidecar_files(self):
        d = self.outdir("nosidecar")
        rc, events = run_script("--json", "--dir", d, f"{self.base}/nosubs.html")
        self.assertEqual(rc, 0)
        done = ev(events, "done")[0]
        self.assertNotIn("sidecars", done)
        self.assertEqual(os.listdir(d), [done["files"][0]["name"]])  # kontrak lama: folder tanpa sisa berkas

    def test_sidecar_writes_poster_and_slim_info(self):
        d = self.outdir("sidecar")
        rc, events = run_script("--json", "--sidecar", "--dir", d, f"{self.base}/nosubs.html")
        self.assertEqual(rc, 0)
        done = ev(events, "done")[0]
        self.assertEqual(len(done["files"]), 1)  # poster/metadata bukan hasil unduhan
        mp4 = done["files"][0]["name"]
        base = os.path.splitext(mp4)[0]
        self.assertEqual(sorted(s["name"] for s in done["sidecars"]), sorted([base + ".jpg", base + ".info.json"]))
        self.assertEqual(sorted(os.listdir(d)), sorted([mp4, base + ".jpg", base + ".info.json"]))
        with open(os.path.join(d, base + ".info.json"), encoding="utf-8") as f:
            info = json.load(f)
        self.assertEqual(info["_molina_slim"], 1)
        self.assertNotIn("formats", info)
        sizes = {s["name"]: s["size"] for s in done["sidecars"]}
        self.assertEqual(sizes[base + ".info.json"], os.path.getsize(os.path.join(d, base + ".info.json")))
        self.assertNotIn("no_poster", [w["code"] for w in ev(events, "warn")])

    def test_sidecar_survives_recode(self):
        d = self.outdir("sidecarrecode")
        rc, events = run_script("--json", "--sidecar", "--dir", d, f"{self.base}/old.html")
        self.assertEqual(rc, 0)
        done = ev(events, "done")[0]
        self.assertTrue(os.path.isfile(done["files"][0]["path"]))
        self.assertEqual(len(done["sidecars"]), 2)
        for s in done["sidecars"]:
            self.assertTrue(os.path.isfile(s["path"]), s)

    def test_sidecar_ignored_for_audio(self):
        d = self.outdir("audiosidecar")
        rc, events = run_script("--json", "--mode", "audio", "--sidecar", "--dir", d, f"{self.base}/nosubs.html")
        self.assertEqual(rc, 0)
        self.assertIn("sidecar_ignored", [w["code"] for w in ev(events, "warn")])
        self.assertNotIn("sidecars", ev(events, "done")[0])
        self.assertFalse([n for n in os.listdir(d) if n.endswith(".info.json")])


if __name__ == "__main__":
    unittest.main(verbosity=2)
