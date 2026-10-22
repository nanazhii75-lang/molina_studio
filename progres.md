# MOLINA STUDIO - Progres

- Repo: https://github.com/nanazhii75-lang/molina_studio.git
- Target: Android 8.0 (API 26), arm64-v8a only
- Build: hanya via GitHub Actions CI

## Log Progres
- 2026-10-22: Inisialisasi repo, blueprint tersimpan di docs/, .gitignore dan progres.md dibuat.
\n- 2026-10-22: Skeleton Gradle (AGP 8.7.3, Kotlin 2.0.21, Compose), shell 4 tab placeholder, dan workflow GitHub Actions ditambahkan.\n
- 2026-10-22: Build CI pertama sukses, APK debug terverifikasi berjalan. Fase 1 (fondasi repo + CI) selesai.
- 2026-10-22: Integrasi emulator Termux (terminal-view 0.118.0 via JitPack), sesi shell lokal /system/bin/sh di tab Terminal, baris tombol ESC/TAB/CTRL/panah.
- 2026-10-22: JitPack gagal resolve terminal-view; modul terminal-emulator & terminal-view di-vendor dari Termux v0.118.0 (tanpa -Werror pada build JNI).
- 2026-10-22: Langkah 3a: unduh+verifikasi SHA-256+ekstrak Debian trixie-slim arm64, proot via jniLibs, tab Terminal punya sesi Debian dan Android sh.
- 2026-10-22: Langkah 3a terverifikasi di perangkat (Debian trixie via PRoot berjalan, apt/git dapat dipakai). Fase 3 (Terminal) hampir selesai, sisa 3b.
