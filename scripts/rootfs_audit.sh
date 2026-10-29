#!/usr/bin/env bash
# Audit rootfs Debian Molina untuk Periksa kode dan kandidat language server (6c).
# Hanya membaca: tidak memasang apa pun. Hasil: docs/rootfs_audit.txt
OUT="$(dirname "$0")/../docs/rootfs_audit.txt"
{
echo "== Waktu: $(date -Is)"
echo "== Arsitektur: $(uname -m) | $(. /etc/os-release; echo "$PRETTY_NAME")"
echo "== RAM (MB):"; grep -E 'MemTotal|MemAvailable|SwapTotal' /proc/meminfo
echo "== Disk bebas rootfs:"; df -h / | tail -1
echo
echo "== Pemeriksa terpasang"
for c in shellcheck pyflakes3 python3 node npm clangd; do
  if command -v "$c" >/dev/null 2>&1; then echo "ADA   $c: $("$c" --version 2>&1 | head -1)"; else echo "TIDAK $c"; fi
done
echo
echo "== Paket apt tersedia (kandidat)"
for k in shellcheck pyflakes3 python3-pylsp python3-jedi-language-server clangd bash-language-server shfmt black nodejs npm; do
  v="$(apt-cache policy "$k" 2>/dev/null | awk '/Candidate:/{print $2}')"
  echo "$k: ${v:-tidak ada}"
done
echo
echo "== Waktu & puncak RAM shellcheck/pyflakes (sampel 300 baris)"
T="$(mktemp -d)"
for i in $(seq 1 300); do echo "echo \$v$i; cp \$a \$b"; done > "$T/s.sh"
for i in $(seq 1 300); do echo "import os$i"; done | sed 's/os\([0-9]*\)/os/' > "$T/p.py"
run() {
  local n="$1"; shift
  if command -v "$1" >/dev/null 2>&1; then
    if [ -x /usr/bin/time ]; then
      /usr/bin/time -f "$n: %e dtk, puncak RAM %M KB" "$@" >/dev/null 2>"$T/e"; tail -1 "$T/e"
    else
      s=$(date +%s.%N); "$@" >/dev/null 2>&1; e=$(date +%s.%N)
      echo "$n: $(echo "$e - $s" | bc 2>/dev/null || echo ?) dtk (RAM tidak terukur: apt install -y time)"
    fi
  else echo "$n: dilewati (belum terpasang)"; fi
}
run shellcheck shellcheck -f json -s bash - < "$T/s.sh"
run pyflakes3 pyflakes3 < "$T/p.py"
rm -rf "$T"
} 2>&1 | tee "$OUT"
echo; echo "Tersimpan: $OUT"
