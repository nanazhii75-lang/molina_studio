#!/usr/bin/env python3
"""Gerbang daftar komponen bundel: klaim keamanan di BundledComponents.kt harus punya bukti nyata di repo
(pin hash, versi sama dengan Gradle). Gagal bila daftar 'berbohong'. Ubah SECARA SADAR."""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
def rd(p):
    with open(os.path.join(ROOT, p), encoding="utf-8") as f: return f.read()

REG = "core/common/src/main/kotlin/com/molina/core/common/BundledComponents.kt"
SHA_OK = {"newpipe", "libmpv", "rootfs", "ytdlp"}  # klaim Sha256 baru WAJIB ditambah di sini setelah pin-nya ada
HEX = re.compile(r"^[0-9a-f]{64}$")
problems = []

reg = rd(REG)
chunks = reg.split("BundledComponent(\n")[1:]
entries = {}
for c in chunks:
    m = re.match(r'\s*"([a-z0-9]+)"', c)
    if m: entries[m.group(1)] = c
if len(entries) < 8:
    problems.append("daftar komponen terlalu sedikit/format berubah: %d entri" % len(entries))

for cid, c in entries.items():
    if "Assurance.Sha256" in c and cid not in SHA_OK:
        problems.append("klaim Sha256 tanpa pin yang dikenal gerbang: %s" % cid)
    if "Assurance.Sha256" in c and "pinRef" not in c and "PIN_" not in c.split("Assurance.Sha256", 1)[1]:
        problems.append("Sha256 tanpa pinRef: %s" % cid)
    if "://" in c:
        problems.append("URL bersekema di daftar komponen (pakai teks tanpa skema): %s" % cid)

# NewPipe: Gradle == NewPipeInfo == pins.sha256
gr = rd("engine/newpipe/build.gradle.kts")
gv = re.search(r'val newpipeVersion = "([^"]+)"', gr)
info = re.search(r'VERSION = "([^"]+)"', rd("engine/newpipe/src/main/kotlin/com/molina/engine/newpipe/NewPipeInfo.kt"))
if not gv or not info or gv.group(1) != info.group(1):
    problems.append("versi NewPipe: Gradle (%s) != NewPipeInfo (%s)" % (gv and gv.group(1), info and info.group(1)))
else:
    pins = rd("scripts/newpipe/pins.sha256")
    if not re.search(r"^[0-9a-f]{64}\s+NewPipeExtractor-%s\.jar$" % re.escape(gv.group(1)), pins, re.M):
        problems.append("pins.sha256 tidak memuat NewPipeExtractor-%s.jar dengan hash valid" % gv.group(1))

# libmpv: tag + hash 64 hex
env = rd("native/prebuilt.env")
tag = re.search(r"^NATIVE_TAG=(\S+)", env, re.M)
sha = re.search(r"^NATIVE_SHA256=(\S+)", env, re.M)
if not tag or not sha or not HEX.match(sha.group(1)):
    problems.append("native/prebuilt.env: NATIVE_TAG/NATIVE_SHA256 tidak valid")

# Debian rootfs: hash 64 hex di Rootfs.kt
rf = rd("app/src/main/java/com/molina/studio/terminal/Rootfs.kt")
m = re.search(r'ROOTFS_SHA256 =\s*"([0-9a-f]+)"', rf)
if not m or not HEX.match(m.group(1)):
    problems.append("Rootfs.kt: ROOTFS_SHA256 tidak valid")

# yt-dlp: installer harus memverifikasi SHA-256
yi = rd("app/src/main/java/com/molina/studio/stream/YtdlpInstaller.kt")
if "SHA-256" not in yi or "expectedSha" not in yi:
    problems.append("YtdlpInstaller.kt tidak lagi memverifikasi SHA-256")

# Versi statis di daftar harus sama dengan Gradle
def must(text, pat, label):
    if pat not in text: problems.append("versi %s di daftar tidak sama dengan Gradle (harap %s)" % (label, pat))
sora = re.search(r"sora-editor:editor:([\d.]+)", rd("feature/code/build.gradle.kts"))
if sora: must(reg, '"%s"' % sora.group(1), "sora-editor")
app = rd("app/build.gradle.kts")
for key, rx in (("commons-compress", r"commons-compress:([\d.]+)"), ("zip4j", r"zip4j:([\d.]+)"), ("xz", r"org\.tukaani:xz:([\d.]+)")):
    v = re.search(rx, app)
    if v: must(reg, v.group(1), key)

if problems:
    print("GERBANG KOMPONEN GAGAL:")
    for x in sorted(set(problems)): print("  - " + x)
    sys.exit(1)
print("Gerbang komponen lulus: %d komponen, klaim keamanan punya bukti pin." % len(entries))
