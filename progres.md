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
- 2026-10-23: Redesain UI Terminal: tab Android sh dihapus, Log pindah ke Settings, navigasi mpv/Terminal/Code/Settings, bar status (net/CPU/RAM), extra keys 2 baris (CLEAR/COPY/PASTE/ALT), prompt hijau/merah + banner emas via bashrc Molina. Menunggu CI + uji perangkat.
- 2026-10-23: Font terminal 14sp -> 11sp (konstanta TERMINAL_TEXT_SP), bar status berwarna (download biru muda, upload merah, CPU/RAM putih, kuning saat lonjakan, merah saat tinggi), optimasi SystemStats (pause saat background via AppVisibility, cache PID 3 dtk, PSS tiap 5 dtk). Menunggu CI + uji perangkat.
- 2026-10-23: Nav bawah disembunyikan saat keyboard aktif (hanya extra key yang tersisa di atas keyboard), font terminal 11sp -> 9sp, nav 4 tab berwarna per tab + garis gradien Molina, extra key berwarna per kategori + haptic tick + latar menyala untuk CTRL/ALT aktif. Menunggu CI + uji perangkat.
- 2026-10-23: Terminal v2: home default prompt `user@localhost:~` (cwd /root), font 8-14sp (pinch-to-zoom + slider Settings, disimpan), long-press CTRL/ALT mengunci modifier (latar menyala + bingkai), tema prompt Cinema Gold / Neon Green / Classic (prompt.env, berlaku di prompt berikutnya), Settings > Terminal 1-1 Termux v0.118.0 (Debugging, Terminal I/O, Terminal View, About; tanpa Donate) plus Crash Reporter dan Logcat Reporter. Menunggu CI + uji perangkat.
- 2026-10-23: Terminal v3: multi-session maksimal 5 (TerminalSessions berbasis daftar, sesi berakhir/di-kill menutup tab, sesi terakhir = keluar app), drawer sesi ala Termux (daftar [n], KEYBOARD, NEW SESSION, gear ke Settings > Terminal) dengan tombol ✕ konfirmasi untuk kill tab, tombol ≡ tap/swipe membuka drawer (tekan lama = menu simbol). Menunggu CI + uji perangkat.
- 2026-10-23: Fix build CI: import `pointerInput` di SessionDrawer.kt & TerminalScreen.kt dikoreksi ke `androidx.compose.ui.input.pointer.pointerInput` (error `detectHorizontalDragGestures` adalah efek turunan). Menunggu CI + uji perangkat.
- 2026-10-23: Fix build CI: Platform declaration clash di TerminalPrefs.kt (property `private set` vs `fun setX()` eksplisit) diselesaikan dengan `@JvmName("applyX")` pada 9 setter; API Kotlin & call site tidak berubah. Menunggu CI + uji perangkat.
- 2026-10-23: Refactor TerminalPrefs: SharedPreferences -> DataStore (migrasi otomatis dari file lama `molina_terminal`) + `StateFlow<TerminalSettings>` (`TerminalPrefs.settings`); setter tanpa I/O di thread pemanggil, tulis disk async & conflated di Dispatchers.IO. Sekaligus menyelesaikan Platform declaration clash (properti kini `val` tanpa setter). API/call site tidak berubah. Menunggu CI + uji perangkat.
- 2026-10-23: Refactor gestur: `Modifier.onHorizontalSwipe(threshold: Dp, direction, onTrigger)` di `com.molina.studio.ui` menggantikan blok `pointerInput`+`detectHorizontalDragGestures` duplikat di SessionDrawer & TerminalScreen (ambang px mentah -> dp: 60px->22dp tutup drawer, 48px->18dp buka drawer; lambda tidak basi). Menunggu CI + uji perangkat.
- 2026-10-23: Fix UI: tema prompt default Cinema Gold -> Molina Classic (home hijau, selain home merah; banner tetap emas); kunci tersimpan diganti `prompt_theme_v2` agar nilai lama (cinema_gold yang ikut tertulis oleh refactor DataStore) tidak menimpa default; tombol KEYBOARD/NEW SESSION di drawer: teks 12sp -> 10sp, lebar 108dp (rata tengah). Menunggu CI + uji perangkat.
- 2026-10-23: Cleanup: konstanta `MolinaShell.PS_HOME_SGR` & `PS_OTHER_SGR` (tidak terpakai) dihapus; `PromptTheme` kini satu-satunya sumber warna prompt. Diverifikasi: CI hijau sebelum perubahan, tidak ada pemakaian di kode/assets/gradle. Menunggu CI.

## Backlog Lintas Fase
- Fase 5 (Cinema/mpv): daftarkan `AppExit.register("mpv") { MPVLib.destroy() }`.
- Fase 6 (Code Editor): daftarkan hook simpan `AppExit.register("editor") { simpanSemuaBerkas() }` (hook jalan sebelum proses dibunuh).
- Fase 7 (Settings Hub): tombol "Keluar" yang memanggil `AppExit.exit()`.
- Catatan: bila kelak memakai sharedUserId, sapuan /proc di AppExit harus dipersempit.
- Fase 5 (Cinema/mpv): buat `MpvSettings` + `MpvPrefs` dengan pola `TerminalPrefs` (data class snapshot, `PrefKeys`, satu fungsi `update {}`, DataStore + `StateFlow`, tulis disk async & conflated). Ekstrak base class generik HANYA bila sudah ada 3 store atau lebih; jangan lebih awal.
- Semua modul (Cinema/Code/Settings): gestur geser horizontal wajib lewat `Modifier.onHorizontalSwipe` (`com.molina.studio.ui`); jangan menulis `pointerInput` + `detectHorizontalDragGestures` manual lagi.
