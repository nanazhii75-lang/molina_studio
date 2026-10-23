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
- 2026-10-23: Fix UI: drawer sesi dipersempit 300dp -> 240dp (konstanta `DrawerWidth`), tombol KEYBOARD/NEW SESSION 108dp -> 100dp dan teks 10sp -> 8sp. Menunggu CI + uji perangkat.
- 2026-10-23: Signing: kunci tetap (PKCS12) via GitHub Secrets MOLINA_KEYSTORE_B64 + MOLINA_KEYSTORE_PASSWORD; Gradle pakai signingConfig `molina` bila env ada (fallback debug bawaan); CI cetak SHA-256 sertifikat APK; langkah di docs/SIGNING.md. Setelah secrets diisi: uninstall sekali terakhir. Backup molina.p12 di luar repo.
- 2026-10-23: Docs: roadmap prioritas ditambahkan, status fase di docs/BLUEPRINT.md disinkronkan (Phase 2 COMPLETED, Phase 3-7 dirinci), checklist uji perangkat di docs/UJI_PERANGKAT.md.
- 2026-10-23: Gate lulus (kunci tetap aktif, sidik jari APK dicek, uji perangkat dilakukan). Fase 6 langkah 1: modul kosong `:core:common` (Kotlin JVM murni) dan `:core:storage` (Android library, api ke core:common) ditambahkan + dipasang di :app; folder kerja tetap `/sdcard/molina-studio` (`/sdcard/Molina` di handoff = salah tulis). Menunggu CI.
- 2026-10-23: Run 22 hijau (scaffold core). Blueprint 5.1 diperbarui: modul `:feature:{terminal,mpv,code,settings}` + `:engine:newpipe`; kontrak `SettingsSection` dan `StreamResolver` di core:common; urutan migrasi code > settings > mpv+engine > terminal terakhir.
- 2026-10-23: Fase 6 langkah 2: kontrak `ExitHook` + registri `ExitHooks` (register/unregister/runAll) dipindah ke `:core:common` (`com.molina.core.common`); `AppExit` kini memanggil `ExitHooks.runAll()` dan tidak lagi menyimpan hook. Perilaku keluar tidak berubah. Menunggu CI.
- 2026-10-23: Run 23 hijau. Fase 6 langkah 2b: `LogLevel`, `LogEvent`, `LogSink` dan pintu `AppLog` (install/log) dipindah ke `:core:common`; `LogBus` kini mengimplementasi `LogSink` dan dipasang lewat `AppLog.install(LogBus)` di MainActivity. Producer lama di :app tetap memanggil `LogBus.log` (perilaku tidak berubah). `MolinaLog` di terminal itu logger android.util.Log terikat TerminalPrefs: tidak masuk core, ikut :feature:terminal. Menunggu CI.
- 2026-10-23: Docs: Blueprint disinkron ulang: Fase 3 dan 4 COMPLETED (uji perangkat dilaporkan lulus pengguna; checklist docs/UJI_PERANGKAT.md belum dicentang item per item), blok Fase 5/6 mengikuti `ExitHooks`/`AppLog`, daftar kandidat core diperbarui (sisa: `AppVisibility`; `MolinaLog` ikut :feature:terminal).

## Roadmap Prioritas (disusun 2026-10-23)
0. Gate: uji perangkat penuh (docs/UJI_PERANGKAT.md) sebelum fase baru. (Selesai 2026-10-23.)
1. Fase 6 Code Editor (Rosemoe) lebih dulu dari Cinema: risiko rendah, nilai harian, membuka `molina open`. (Menyimpang dari urutan blueprint.)
2. Jembatan CLI `molina` (open/play/log) lewat IPC + paste aman (bracketed paste, kirim per potongan, konfirmasi multi-baris).
3. Fase 5 Cinema bertahap: 5a spike libmpv arm64 di CI (+ lisensi di THIRD_PARTY_NOTICES.md), 5b putar lokal + AppExit + MpvPrefs, 5c event ke LogBus, 5d NewPipe terakhir (sering rusak, perlu update).
4. Fase 7 Settings Hub (tombol Keluar, hot-reload mpv.conf), selipkan kapan saja.
Ide pembeda: chip status CI GitHub Actions + `molina ci` (token di Keystore); prompt dengan branch git + exit code; paket setup Dev/Media (SHA-256); token desain `MolinaTheme` (emas #D4AF37), ikon adaptif + splash; janji tanpa telemetri/donasi/offline-first di About.

## Backlog Lintas Fase
- Fase 5 (Cinema/mpv): daftarkan `ExitHooks.register("mpv") { MPVLib.destroy() }`.
- Fase 6 (Code Editor): daftarkan hook simpan `ExitHooks.register("editor") { simpanSemuaBerkas() }` (hook jalan sebelum proses dibunuh).
- Fase 7 (Settings Hub): tombol "Keluar" yang memanggil `AppExit.exit()`.
- Catatan: bila kelak memakai sharedUserId, sapuan /proc di AppExit harus dipersempit.
- Fase 5 (Cinema/mpv): buat `MpvSettings` + `MpvPrefs` dengan pola `TerminalPrefs` (data class snapshot, `PrefKeys`, satu fungsi `update {}`, DataStore + `StateFlow`, tulis disk async & conflated). Ekstrak base class generik HANYA bila sudah ada 3 store atau lebih; jangan lebih awal.
- Semua modul (Cinema/Code/Settings): gestur geser horizontal wajib lewat `Modifier.onHorizontalSwipe` (`com.molina.studio.ui`); jangan menulis `pointerInput` + `detectHorizontalDragGestures` manual lagi.
- Modul fitur: pindahkan Terminal ke `:feature:terminal` paling akhir (cek TerminalService di manifest, assets molina/bashrc, jniLibs + useLegacyPackaging tetap di :app).
- Log: pasang `AppLog.install(LogBus)` di kelas Application (butuh `android:name` di manifest) agar log sebelum Activity tidak hilang; producer terminal pindah ke `AppLog.log` saat `:feature:terminal` dibuat.
