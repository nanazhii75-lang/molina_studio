#!/usr/bin/env python3
"""Molina tahap D1 - perkecil permukaan serangan FFmpeg. Patch idempotent; jalankan dari akar repo."""
import os, sys

ROOT = os.getcwd()
PROFILE_PATH = "native/molina_ffmpeg_profile.sh"
AUDIT_PATH = "scripts/ffmpeg_components_audit.sh"
NATIVE = "scripts/native_mpv.sh"
PROGRES = "progres.md"

PROFILE = r'''# Profil FFmpeg Molina: PERMUKAAN SERANGAN SEMPIT (daftar putih) + pengerasan binari.
# Di-source oleh buildscripts/scripts/ffmpeg.sh (disisipkan scripts/native_mpv.sh) dan menghasilkan
# array molina_ffmpeg_args yang ditambahkan SETELAH flag upstream.
#
# Komponen yang tidak ada di daftar TIDAK dikompilasi sama sekali, jadi kodenya tidak ada di .so
# dan tidak bisa dieksploitasi lewat berkas atau stream jahat. Menambah format: edit daftar di
# bawah, catat di progres.md, jalankan workflow native-mpv.yml, lalu pin ulang native/prebuilt.env.
# Setelah build, scripts/ffmpeg_components_audit.sh memeriksa hasil configure yang sebenarnya.

# Protokol: hanya yang dipakai Molina (UrlPolicy mengizinkan http, https, rtmp, rtmps, rtsp, berkas lokal).
# httpproxy = tunnel CONNECT https lewat Molina Shield. crypto = AES-128 HLS. rtp+udp = isi rtsp.
molina_protocols=(file http https tcp tls crypto data httpproxy rtmp rtmps rtp udp)

# Demuxer: kontainer media umum, streaming, subtitle teks, dan gambar sampul/animasi.
molina_demuxers=(
  # kontainer video/audio
  matroska mov avi asf flv live_flv mpegts mpegps mpegvideo ogg wav mp3 flac aac ac3 eac3 dts dtshd truehd
  aiff caf ape wv tta dsf amr amrnb amrwb mpc mpc8 rm
  # bitstream mentah
  h264 hevc obu av1 ivf m4v mjpeg mpjpeg
  # streaming
  hls dash rtsp sdp
  # subtitle
  ass srt webvtt microdvd subviewer subviewer1 jacosub mpl2 mpsub pjs realtext sami stl vplayer aqtitle lrc sup
  # gambar
  image2 image2pipe image_jpeg_pipe image_png_pipe image_webp_pipe image_gif_pipe image_bmp_pipe gif apng webp_anim
)

molina_ffmpeg_args=(
  --disable-protocols --disable-demuxers
  --enable-protocol="$(IFS=,; echo "${molina_protocols[*]}")"
  --enable-demuxer="$(IFS=,; echo "${molina_demuxers[*]}")"
  # pengerasan: canary tumpukan, fortify, RELRO penuh, tumpukan tidak bisa dieksekusi
  "--extra-cflags=-fstack-protector-strong -D_FORTIFY_SOURCE=2"
  "--extra-ldflags=-Wl,-z,relro,-z,now,-z,noexecstack"
)
'''

AUDIT = r'''#!/usr/bin/env bash
# Periksa komponen FFmpeg yang BENAR-BENAR terkompilasi (dari config_components.h hasil configure).
# Pakai: bash scripts/ffmpeg_components_audit.sh <config_components.h> [keluaran.txt]
# GAGAL (exit 1) bila ada protokol/demuxer terlarang aktif atau jumlah demuxer melebihi batas.
set -euo pipefail
H="${1:?config_components.h wajib}"; OUT="${2:-}"
[ -f "$H" ] || { echo "tidak ada: $H" >&2; exit 2; }
MAX_DEMUXERS="${MAX_DEMUXERS:-110}"; MAX_PROTOCOLS="${MAX_PROTOCOLS:-20}"
enabled() { sed -n "s/^#define CONFIG_\\(.*\\)_$1 1\$/\\1/p" "$H" | tr 'A-Z' 'a-z' | sort; }
protos="$(enabled PROTOCOL)"; demux="$(enabled DEMUXER)"
np=$(printf '%s\n' "$protos" | grep -c . || true); nd=$(printf '%s\n' "$demux" | grep -c . || true)
echo "protokol aktif: $np | demuxer aktif: $nd"
bad=0
for p in ftp concat concatf subfile pipe fd unix tee md5 gopher gophers sftp android_content async cache srtp udplite \
         rtmpt rtmpe rtmpte rtmpts mmsh mmst icecast sctp prompeg dtls bluray ipfs_gateway ipns_gateway; do
  if printf '%s\n' "$protos" | grep -qx "$p"; then echo "GAGAL: protokol terlarang aktif: $p" >&2; bad=1; fi
done
for d in concat ffmetadata data sap rtp; do
  if printf '%s\n' "$demux" | grep -qx "$d"; then echo "GAGAL: demuxer terlarang aktif: $d" >&2; bad=1; fi
done
[ "$np" -le "$MAX_PROTOCOLS" ] || { echo "GAGAL: protokol $np > batas $MAX_PROTOCOLS" >&2; bad=1; }
[ "$nd" -le "$MAX_DEMUXERS" ] || { echo "GAGAL: demuxer $nd > batas $MAX_DEMUXERS" >&2; bad=1; }
if [ -n "$OUT" ]; then { echo "# protokol ($np)"; printf '%s\n' "$protos"; echo "# demuxer ($nd)"; printf '%s\n' "$demux"; } > "$OUT"; fi
[ "$bad" -eq 0 ] && echo "OK: permukaan FFmpeg sesuai profil"
exit "$bad"
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


if not os.path.exists(os.path.join(ROOT, NATIVE)):
    sys.exit("scripts/native_mpv.sh tidak ditemukan. Jalankan dari akar repo molina-studio.")

put_file(PROFILE_PATH, PROFILE)
put_file(AUDIT_PATH, AUDIT)

s = rd(NATIVE)
if "molina_ffmpeg_profile.sh" in s:
    print("sudah dipatch, lewati:", NATIVE)
else:
    def sub(old, new):
        global s
        if s.count(old) != 1:
            sys.exit("GAGAL: pola harus tepat 1 kali di %s (ditemukan %d):\n%s" % (NATIVE, s.count(old), old))
        s = s.replace(old, new, 1)

    # A. sisipkan profil ke ffmpeg.sh upstream (setelah rename JNI, sebelum masuk ke buildscripts)
    sub('cd "$UP/buildscripts"\n',
        'msg "Profil FFmpeg Molina (daftar putih demuxer/protokol + pengerasan)"\n'
        'FFSH="$UP/buildscripts/scripts/ffmpeg.sh"\n'
        'cp "$ROOT/native/molina_ffmpeg_profile.sh" "$UP/buildscripts/scripts/molina_ffmpeg_profile.sh"\n'
        'if ! grep -q molina_ffmpeg_args "$FFSH"; then\n'
        '  python3 - "$FFSH" << \'PYEOF\'\n'
        'import sys\n'
        'p = sys.argv[1]\n'
        's = open(p).read()\n'
        'a = "args=(\\n"\n'
        'b = "\\n)\\n../configure \\"${args[@]}\\""\n'
        'assert s.count(a) == 1 and s.count(b) == 1, "ffmpeg.sh upstream berubah, sesuaikan patch"\n'
        's = s.replace(a, ". ../../scripts/molina_ffmpeg_profile.sh\\n\\n" + a, 1)\n'
        's = s.replace(b, "\\n)\\nargs+=(\\"${molina_ffmpeg_args[@]}\\")\\n../configure \\"${args[@]}\\"", 1)\n'
        'open(p, "w").write(s)\n'
        'PYEOF\n'
        'fi\n'
        'grep -q molina_ffmpeg_args "$FFSH" && grep -q molina_ffmpeg_profile.sh "$FFSH"\n\n'
        'cd "$UP/buildscripts"\n')

    # B. audit komponen sesudah FFmpeg dikompilasi (dependensi mpv), sebelum libmpv
    sub('msg "Kompilasi dependensi (arm64)"\n./buildall.sh --only-deps mpv --arch arm64\n',
        'msg "Kompilasi dependensi (arm64)"\n./buildall.sh --only-deps mpv --arch arm64\n'
        'msg "Audit komponen FFmpeg yang terkompilasi"\n'
        'bash "$ROOT/scripts/ffmpeg_components_audit.sh" deps/ffmpeg/_build_arm64/config_components.h "$OUT/ffmpeg-components.txt"\n')

    # C. audit pengerasan .so (informasi dulu; jadikan fatal setelah build pertama bersih)
    sub('"$BIN/llvm-strip" --strip-unneeded "$D"/*.so\n',
        '"$BIN/llvm-strip" --strip-unneeded "$D"/*.so\n'
        'msg "Audit pengerasan .so (informasi; belum fatal)"\n'
        'bash "$ROOT/scripts/native_audit.sh" "$D" | tee "$OUT/hardening-audit.txt" || echo "PERINGATAN: native_audit melaporkan GAGAL (lihat hardening-audit.txt)"\n')

    # D. catat hasil audit di BUILD-INFO.txt (ikut terbit di Release)
    sub('echo "libass=$LIBASS_SHA"; echo "libplacebo=$LIBPLACEBO_SHA"; echo "ndk=$v_ndk_n"; } > "$OUT/BUILD-INFO.txt"\n',
        'echo "libass=$LIBASS_SHA"; echo "libplacebo=$LIBPLACEBO_SHA"; echo "ndk=$v_ndk_n"; } > "$OUT/BUILD-INFO.txt"\n'
        '{ echo "--- komponen FFmpeg (profil Molina)"; cat "$OUT/ffmpeg-components.txt"\n'
        '  echo "--- audit pengerasan"; cat "$OUT/hardening-audit.txt"; } >> "$OUT/BUILD-INFO.txt"\n')

    wr(NATIVE, s)
    print("dipatch:", NATIVE)

NOTE = """
## 2026-10-25 - Molina Shield D1 (perkecil permukaan serangan FFmpeg)
- [x] C2 hijau dan diuji di perangkat
- [x] feat(security): native/molina_ffmpeg_profile.sh = daftar putih FFmpeg (12 protokol, ~70 demuxer; default upstream 41 protokol dan 361 demuxer) + flag pengerasan (stack-protector-strong, FORTIFY, RELRO penuh, noexecstack); disisipkan ke ffmpeg.sh upstream oleh scripts/native_mpv.sh; scripts/ffmpeg_components_audit.sh memeriksa config_components.h hasil build (GAGAL bila ada ftp/concat/pipe/dst. atau jumlah melebihi batas); hasil audit komponen dan pengerasan ikut BUILD-INFO.txt
- [x] terverifikasi: configure FFmpeg SHA dipin dengan mbedtls+libdav1d+libxml2 lolos; hitungan 41->12 protokol, 361->70 demuxer; audit lulus pada profil dan GAGAL pada build default (uji negatif)
- [ ] BELUM terbukti: kompilasi penuh (sandbox 1 core), build Android arm64, dan pemutaran. Jalankan workflow Native mpv (manual), lalu isi NATIVE_TAG dan NATIVE_SHA256 baru di native/prebuilt.env, lalu CI APK
- [ ] uji perangkat setelah pin baru: mkv/mp4/mp3/ts, IPTV id.m3u, HLS, DASH, rtsp/rtmp bila punya, subtitle .srt/.ass, gambar. Format di luar daftar (mxf, wtv, swf, nut, dll.) memang tidak akan terbuka
- [ ] D2 (berikutnya): daftar putih decoder setelah D1 terbukti; native_audit dijadikan fatal bila build pertama bersih
"""
t = rd(PROGRES)
if "Molina Shield D1" in t:
    print("sudah dicatat, lewati:", PROGRES)
else:
    wr(PROGRES, t.rstrip("\n") + "\n" + NOTE)
    print("dicatat:", PROGRES)
print("SELESAI")
