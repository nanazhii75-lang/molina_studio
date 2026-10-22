# MOLINA STUDIO - Progres

- Repo: https://github.com/nanazhii75-lang/molina_studio.git
- Target: Android 8.0 (API 26), arm64-v8a only
- Build: hanya via GitHub Actions CI

## Log Progres
- 2026-10-22: Inisialisasi repo, blueprint tersimpan di docs/, .gitignore dan progres.md dibuat.
- 2026-10-22: Skeleton Gradle (AGP 8.7.3, Kotlin 2.0.21, Compose), shell 4 tab placeholder, dan workflow GitHub Actions ditambahkan.
- 2026-10-22: Build CI pertama sukses, APK debug terverifikasi berjalan. Fase 1 (fondasi repo + CI) selesai.
- 2026-10-22: Integrasi emulator Termux (terminal-view 0.118.0 via JitPack), sesi shell lokal /system/bin/sh di tab Terminal, baris tombol ESC/TAB/CTRL/panah.
- 2026-10-22: JitPack gagal resolve terminal-view; modul terminal-emulator & terminal-view di-vendor dari Termux v0.118.0 (tanpa -Werror pada build JNI).
- 2026-10-22: Langkah 3a: unduh+verifikasi SHA-256+ekstrak Debian trixie-slim arm64, proot via jniLibs, tab Terminal punya sesi Debian dan Android sh.
- 2026-10-22: Langkah 3a terverifikasi di perangkat (Debian trixie via PRoot berjalan, apt/git dapat dipakai). Fase 3 (Terminal) hampir selesai, sisa 3b.
- 2026-10-22: Langkah 3b: TerminalService (foreground service + notifikasi persisten Buka/Keluar, WAKE_LOCK), izin FOREGROUND_SERVICE/WAKE_LOCK, aksi Keluar mematikan semua sesi. Menunggu CI + uji perangkat.
- 2026-10-22: Langkah 3c: AppExit (aksi Keluar membunuh semua proses ber-UID aplikasi, hook pembersihan per modul untuk mpv/editor, Activity dihapus dari recents). Menunggu CI + uji perangkat.
- 2026-10-22: Backlog lintas fase (hook AppExit mpv/editor, tombol Keluar di Settings) dicatat.
- 2026-10-22: Langkah 4: LogGrouping (bus log + penggabungan baris identik), IPC LocalSocket abstract @molina.log dengan cek UID, tab Log di Terminal (filter, hapus, uji). Menunggu CI + uji perangkat.

## Backlog Lintas Fase
- Fase 5 (Cinema/mpv): daftarkan `AppExit.register("mpv") { MPVLib.destroy() }`.
- Fase 6 (Code Editor): daftarkan hook simpan `AppExit.register("editor") { simpanSemuaBerkas() }` (hook jalan sebelum proses dibunuh).
- Fase 7 (Settings Hub): tombol "Keluar" yang memanggil `AppExit.exit()`.
- Catatan: bila kelak memakai sharedUserId, sapuan /proc di AppExit harus dipersempit.
