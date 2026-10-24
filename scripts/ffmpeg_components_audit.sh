#!/usr/bin/env bash
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
