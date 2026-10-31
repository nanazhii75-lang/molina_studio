#!/usr/bin/env python3
"""[DOMAIN-SCAN] Gagal bila ada domain di source produksi yang tidak ada di allowed-domains.txt."""
import os, re, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ROOTS = ["app", "core", "feature", "engine", "terminal-emulator", "terminal-view", "native", "scripts"]
EXT = {".kt", ".kts", ".xml", ".gradle", ".py", ".sh", ".yml", ".yaml", ".toml", ".properties", ".cpp", ".c", ".h", ".json"}
SKIP_DIRS = {".git", "build", ".gradle", "node_modules"}
URL = re.compile(r"https?://([A-Za-z0-9._-]+\.[A-Za-z]{2,})", re.I)

def allowed():
    p = os.path.join(os.path.dirname(__file__), "allowed-domains.txt")
    return {l.strip().lower() for l in open(p, encoding="utf-8") if l.strip() and not l.startswith("#")}

def main():
    ok = allowed()
    bad = {}
    for r in ROOTS:
        base = os.path.join(ROOT, r)
        for d, ds, fs in os.walk(base):
            ds[:] = [x for x in ds if x not in SKIP_DIRS and not x.endswith("-tests")]
            for f in fs:
                if os.path.splitext(f)[1].lower() not in EXT:
                    continue
                p = os.path.join(d, f)
                try:
                    t = open(p, encoding="utf-8", errors="ignore").read()
                except OSError:
                    continue
                for m in URL.finditer(t):
                    h = m.group(1).lower().rstrip(".")
                    if h not in ok:
                        bad.setdefault(h, set()).add(os.path.relpath(p, ROOT))
    if bad:
        print("DOMAIN ASING TERDETEKSI (tinjau lalu tambahkan ke allowed-domains.txt bila sah):")
        for h in sorted(bad):
            print("  -", h, "->", ", ".join(sorted(bad[h])[:3]))
        return 1
    print("OK: semua domain ada di allowlist")
    return 0

if __name__ == "__main__":
    sys.exit(main())
