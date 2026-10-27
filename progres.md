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
- 2026-10-23: Run 24 hijau. Fase 6 langkah 2c: `AppVisibility` dipindah ke `:core:common` (`StateFlow` read-only + `setForeground`, hanya MainActivity yang menulis); `:core:common` kini memakai `kotlinx-coroutines-core:1.9.0` (api). Prasyarat tersisa: `MolinaPaths` + folder, kelas Application. Menunggu CI.
- 2026-10-23: Fase 6 langkah 3a: kontrak `MolinaPaths` + enum `MolinaDir` (downloads/export/media/backup) ditambahkan di `:core:common`. Keputusan: data bersama di folder tersembunyi `/sdcard/.molina-data` (di luar working tree git, opsi b); rahasia tetap di `filesDir`/Keystore. Belum ada implementasi. Menunggu CI.
- 2026-10-23: Fase 6 langkah 3b: `AndroidMolinaPaths` (implementasi `MolinaPaths`) di `:core:storage`: workspace=ExternalStorage/molina-studio, dataRoot=ExternalStorage/.molina-data, privateRoot=filesDir; `ensureDirs()` membuat subfolder (off-main-thread, butuh izin storage). Belum dirakit di :app. Menunggu CI.
- 2026-10-23: Fase 6 langkah 3c: `AppPaths` (singleton di `:app`) dirakit; `MainActivity.onResume` memanggil `ensureIfPermitted` (jalan di IO, hanya bila izin storage ada, dicoba ulang tiap resume sampai berhasil; hasil dicatat ke AppLog). Perilaku UI tidak berubah. Menunggu CI.
- 2026-10-23: docs: catat ide snapshot otomatis berkas yang sedang diedit ke `MolinaDir.Backup` (lihat Backlog, Fase 6).
- 2026-10-23: Fase 6 langkah 4: `MolinaApp` (Application) memasang `AppLog.install(LogBus)` di `onCreate` proses; dihapus dari `MainActivity`; manifest `android:name=".MolinaApp"`. Log sebelum Activity (mis. TerminalService) kini tertangkap. Menunggu CI.
- 2026-10-23: Fase 6 pra-langkah 5, rebranding terminal: `com.termux.terminal` -> `com.molina.terminal`, `com.termux.view` -> `com.molina.terminal.view`, JNI `Java_com_molina_terminal_JNI_*`, lib native `libtermux` -> `libmolina_pty` (`molina_pty.c`), namespace Gradle disesuaikan, impor 4 berkas app. Atribusi Termux (NOTICES, About, URL upstream) sengaja dipertahankan. Menunggu CI + UJI PERANGKAT (JNI tidak terbukti oleh CI).
- 2026-10-23: Review terminal pra-mpv (audit baca-saja): tidak ada telemetri/donasi/URL dev lama fungsional. Temuan P0: uji perangkat JNI, DNS hardcode, bind /sdcard penuh, hardening symlink ekstraksi, gerbang telemetri CI, LICENSE+source di About. P1: mirror rootfs, release+R8, resume unduhan.

- 2026-10-23: Hardening terminal P0: DNS guest mengikuti DNS sistem (MolinaDns, izin ACCESS_NETWORK_STATE), bind storage default hanya /sdcard/molina-studio (toggle Settings > Terminal I/O > Akses penyimpanan penuh), ekstraksi rootfs tahan symlink (cek path kanonik), gerbang telemetri CI (scripts/telemetry_gate.py: host/izin/dependency). Menunggu CI + uji perangkat.
- 2026-10-23: UI Terminal: panel pemasangan Debian baru (`SetupPanel.kt`: monogram Molina, pill status, kartu info disk/jaringan/SHA-256, langkah Unduh/Verifikasi/Ekstrak, galat ramah + Salin pesan galat) dan bar status 4 chip berbingkai (`StatusChips.kt`, 9sp, bingkai solid saat Warn/High) menggantikan `InstallPanel`/`StatusBar` di TerminalScreen.kt. Font dikecilkan (judul 18sp, isi 11-12sp, tombol 14sp). Tombol Batal ditunda (perlu ubah Rootfs.kt). Menunggu CI + uji perangkat.
- 2026-10-23: Token desain: `MolinaTheme` (`ui/MolinaTheme.kt`) jadi satu sumber warna (emas #E6B450 + Good/Warn/Bad/Muted/Card); MainActivity, SessionDrawer, TerminalService, SetupPanel, StatusChips memakainya; emas tema prompt Cinema Gold dan banner `MolinaShell.BRAND_SGR` disatukan dari #D4AF37 ke #E6B450 (SGR 230;180;80); tombol Pasang/Coba lagi dipersempit (176dp x 40dp). Menunggu CI + uji perangkat.
- 2026-10-23: Rootfs: unduhan bisa dilanjutkan (HTTP Range ke .part di cacheDir; 206 = lanjut, 200 = ulang dari awal; Accept-Encoding identity; arsip utuh dipakai ulang), `RootfsManager.cancel()` + tombol Batal (batalkan unduh/verifikasi/ekstrak, bersihkan debian.tmp, state kembali Missing, sisa unduhan dipertahankan); bila ada sisa, tombol jadi Lanjutkan + baris Tersimpan. Range diuji manual ke URL rootfs (206, Content-Range total 30200478). Menunggu CI + uji perangkat.
- 2026-10-23: Keputusan pemilik: Fase 5 (mpv) sebelum Fase 6 (Code Editor); lisensi kode Molina GPL-3.0-or-later (FFmpeg GPL + NewPipeExtractor GPLv3).
- 2026-10-23: Fase 5a (spike native): workflow manual `native-mpv.yml` + `scripts/native_mpv.sh` membangun libmpv/libplayer arm64 dari mpv-android commit 62013d4; mpv/dav1d/libass/libplacebo/FFmpeg dipin ke commit di `native/pins.env` (upstream mengambang di HEAD); unduhan lewat `scripts/native_wget.sh` (HTTPS saja, NDK http->https, SHA-256 dicatat dan diverifikasi bila `native/pins.sha256` terisi); nama kelas JNI diubah ke `com.molina.cinema.MPVLib` saat build. Menunggu run CI + tinjau hash.
- 2026-10-23: Catatan gerbang telemetri: baris komentar penuh dan folder `scripts/` tidak dipindai; saat `buildscripts/*.sh` upstream masuk repo (5b-0) perlu daftar host build-time terpisah. Belum diubah.
- 2026-10-23: Fase 5a run #1 hijau: artefak molina-native-arm64 (libmpv, libplayer, libc++_shared, libav*/libsw*; cek NEEDED lulus). SHA-256: libmpv.so e90028a3..., libplayer.so a857127e..., tgz bec3da1f.... 10 hash unduhan dikunci di `native/pins.sha256` (mode verifikasi aktif mulai run #2; unduhan baru wajib dipin sadar). Catatan: arsip gitlab (fontconfig, libxml2) dan gas-preprocessor (master) bisa berubah di sisi server; bila run gagal 'SHA-256 TIDAK COCOK', tinjau dulu sebelum memperbarui pin. Belum dilakukan: uji System.loadLibrary di perangkat (dikerjakan di 5b-0).
- 2026-10-23: Fase 5b-0 (kode): impor mpv-android 62013d4 ke `feature/mpv` (modul library, namespace com.molina.cinema), paket is.xyz.mpv/filepicker -> com.molina.cinema[.filepicker], nama app 'Molina Cinema', JNI tidak diimpor (dibangun `native-mpv.yml`). Manifest library: tanpa atribut <application>, MainActivity upstream bukan launcher, skema mms/mmst/mmsh/tcp/udp/rtp/ftp dibuang. Gerbang telemetri diubah SADAR: izin FOREGROUND_SERVICE_MEDIA_PLAYBACK (latar belakang) + host teks lisensi mozilla.org/www.gnu.org. Belum: pustaka .so belum masuk APK (menunggu langkah publikasi artefak native + build.yml), CI hijau belum diverifikasi, uji putar di perangkat belum.
- 2026-10-23: Fase 5b-0 CI hijau (Run 38, commit 2eaac19): :feature:mpv terkompilasi dengan AGP 8.7.3/Kotlin 2.0.21, dependensi diturunkan lolos, gerbang telemetri lulus. Catatan perangkat: di PRoot storage bersama git butuh `git config core.createObject rename` (hardlink tidak andal). Belum: .so belum di APK (5b-0b), uji putar di perangkat belum.
- 2026-10-23: Fase 5b-0b (CI): `native-mpv.yml` kini punya job `publish` (hak tulis hanya di job itu) yang menerbitkan molina-native-arm64.tgz ke GitHub Release bertag `native-mpv-<sha7>-run<N>`; `build.yml` menjalankan `scripts/fetch_native.sh` (gh release download + cek SHA-256 + validasi isi tgz) ke feature/mpv/src/main/jniLibs, pin di `native/prebuilt.env` (kosong = APK tanpa .so, hanya peringatan); izin build.yml dipersempit ke contents: read. Belum: jalankan native-mpv sekali lagi (uji verifikasi pins run #2), isi pin, uji System.loadLibrary di perangkat.

## Roadmap Prioritas (disusun 2026-10-23)
0. Gate: uji perangkat penuh (docs/UJI_PERANGKAT.md) sebelum fase baru. (Selesai 2026-10-23.)
1. Fase 5 Cinema (mpv) lebih dulu, lalu Fase 6 Code Editor (Rosemoe) (keputusan pemilik 2026-10-23, menggantikan urutan lama).
2. Jembatan CLI `molina` (open/play/log) lewat IPC + paste aman (bracketed paste, kirim per potongan, konfirmasi multi-baris).
3. Fase 5 Cinema bertahap: 5a spike libmpv arm64 di CI (+ lisensi di THIRD_PARTY_NOTICES.md), 5b putar lokal + AppExit + MpvPrefs, 5c event ke LogBus, 5d NewPipe terakhir (sering rusak, perlu update).
4. Fase 7 Settings Hub (tombol Keluar, hot-reload mpv.conf), selipkan kapan saja.
Ide pembeda: chip status CI GitHub Actions + `molina ci` (token di Keystore); prompt dengan branch git + exit code; paket setup Dev/Media (SHA-256); token desain `MolinaTheme` (emas #E6B450), ikon adaptif + splash; janji tanpa telemetri/donasi/offline-first di About.

## Backlog Lintas Fase
- Fase 5 (Cinema/mpv): daftarkan `ExitHooks.register("mpv") { MPVLib.destroy() }`.
- Fase 6 (Code Editor): daftarkan hook simpan `ExitHooks.register("editor") { simpanSemuaBerkas() }` (hook jalan sebelum proses dibunuh).
- Fase 7 (Settings Hub): tombol "Keluar" yang memanggil `AppExit.exit()`.
- Catatan: bila kelak memakai sharedUserId, sapuan /proc di AppExit harus dipersempit.
- Fase 5 (Cinema/mpv): buat `MpvSettings` + `MpvPrefs` dengan pola `TerminalPrefs` (data class snapshot, `PrefKeys`, satu fungsi `update {}`, DataStore + `StateFlow`, tulis disk async & conflated). Ekstrak base class generik HANYA bila sudah ada 3 store atau lebih; jangan lebih awal.
- Semua modul (Cinema/Code/Settings): gestur geser horizontal wajib lewat `Modifier.onHorizontalSwipe` (`com.molina.studio.ui`); jangan menulis `pointerInput` + `detectHorizontalDragGestures` manual lagi.
- Modul fitur: pindahkan Terminal ke `:feature:terminal` paling akhir (cek TerminalService di manifest, assets molina/bashrc, jniLibs + useLegacyPackaging tetap di :app).
- Log: producer terminal pindah ke `AppLog.log` saat `:feature:terminal` dibuat.
- Fase 6 langkah 5: `:feature:code` (Rosemoe), hanya bergantung ke core; `ExitHooks.register("editor")`; kontrak `SettingsSection` di core; `:app` merakit
- Fase 6 ide (`:feature:code`): snapshot otomatis berkas yang sedang diedit ke `MolinaDir.Backup` (`/sdcard/.molina-data/backup`) SEBELUM `ExitHooks.register("editor") { simpanSemuaBerkas() }` menulis ke berkas asli, agar ada salinan terakhir bila proses diterminasi paksa. Detail (rotasi N salinan, penamaan) diputuskan saat implementasi.
- Optimasi RAM (kandidat, belum dikerjakan): ukur baseline dulu; transcript rows dapat diatur (default 2000), batas buffer log; proses Debian/PRoot di dalam sesi umumnya dominan.
- 2026-10-23: Branding: logo Molina (jendela terminal + prompt chevron emas #E6B450 + kursor, latar #0A0A0F). Ikon adaptif di app/src/main/res, manifest memakai @mipmap/ic_launcher + roundIcon, master SVG + PNG di docs/branding/. Menunggu CI + uji perangkat.
- 2026-10-23: Fix terminal: peringatan debconf (Dialog/Readline/tcsetattr) di apt PRoot dihilangkan via DEBIAN_FRONTEND=noninteractive di env guest; default apt aman (tolak repo tak bertanda tangan, --force-confold) dan hemat (tanpa Recommends/Translation) lewat etc/apt/apt.conf.d/02-molina-defaults; MALLOC_ARENA_MAX=2 untuk menekan RAM proses guest. Menunggu CI + uji perangkat.
- 2026-10-23: Prasyarat inti Fase 5 diverifikasi lunas (AppPaths.ensureIfPermitted, MolinaApp memasang AppLog.install, ExitHooks, AppVisibility); blueprint disinkronkan dan urutan fase diluruskan: mpv (Fase 5) sebelum Code Editor (Fase 6). Utang tersisa ditutup: izin storage kini diminta sekali di MainActivity (sebelumnya hanya saat tab Terminal dibuka, sehingga AppPaths tidak siap bila pengguna langsung memakai tab mpv). Kontrak core yang masih dibuat saat dibutuhkan: MolinaCommand/CommandBus, SettingsSection, StreamResolver. Menunggu CI + uji perangkat.
- 2026-10-23: Pin native diisi SADAR (native/prebuilt.env): tag native-mpv-af13289-run2, SHA-256 tgz f1195d5a7e1145fbaf513b5f284a9781f4c93f5b6de1371108e60e4065d19ad3. Sumber: mpv 6a262d1, ffmpeg 946fcce, libass f61db56, libplacebo 285d1cc, dav1d 4c84431, NDK 30.0.16248370. APK kini memasang libmpv/libplayer; belum diuji di perangkat (System.loadLibrary). Menunggu CI.
- 2026-10-23: Cinema Home 5b-1: tab mpv berisi layar Molina Cinema (File via SAF, Tautan, Setelan mpv bawaan, Reset). Tanpa switch Remember choice (dibuang atas keputusan pemilik). URL hanya http/https/rtsp/rtmp(s); tautan YouTube dan pencarian judul menunggu NewPipe 5d. Belum diuji di perangkat. Menunggu CI.
- 2026-10-23: Perbaikan gerbang telemetri 5b-1: placeholder dialog Cinema Home berisi pola URL (host tidak sah) diganti teks biasa. Daftar allowlist gerbang TIDAK diubah. Menunggu CI.
- 2026-10-23: Hasil tes pemilik atas Cinema Home: tampilan tampil, ikon aksi terlalu besar. ikon aksi Cinema Home diperkecil (lingkaran 56 ke 40 dp, glyph 22 ke 16 sp, label 12 ke 10 sp). Disepakati: yt-dlp (Debian PRoot) jadi cadangan setelah NewPipe, opt-in. Menunggu CI.
- 2026-10-23: Spike 5d-0 disiapkan: workflow spike-newpipe.yml (manual) me-resolve NewPipeExtractor v0.26.5 (commit f9e6bb8, rilis 2026-08-15) dari JitPack, hanya untuk grup com.github.*; hash dan verification-metadata keluar sebagai artefak. Host build-time jitpack.io hanya di scripts/spike-newpipe (folder yang dilewati gerbang); ALLOWED_HOSTS TIDAK diubah. Belum dijalankan di Actions. Koordinat belum pasti (dua kandidat dicoba).
- 2026-10-23: build.yml dipin ke SHA commit: checkout v4.4.0, setup-java v4.9.1, setup-gradle v4.4.4, upload-artifact v4.6.2 (SHA dicocokkan ke tag lewat git ls-remote). Semua workflow kini tanpa tag mengambang. Perilaku build tidak berubah. Menunggu CI.
- 2026-10-23: Rencana: URL/video, keamanan, optimasi disepakati; menunggu hasil recon Manifest/mpv sebelum patch.
- 2026-10-23: hardening intent MPVActivity (exported): extra 'filepath' divalidasi (skema allowlist, tolak path data privat), skema 'lavf' dihapus dari resolveUri. Dukungan URL/video sudah ada (intent http/https/rtmp/rtsp, share, dialog Buka URL). Menunggu CI.
- 2026-10-23: normalisasi input URL di dialog Buka URL (trim, skema huruf kecil, www. -> https://) dan 'lavf' dihapus dari Utils.PROTOCOLS agar konsisten dengan MPVActivity.resolveUri. yt-dlp dicatat sebagai cadangan fitur 'play universal'. Menunggu CI.
- 2026-10-23: network-timeout mpv 20 dtk dan user-agent MolinaCinema/<versi> di MPVView. Jika ada server menolak (HTTP 403), hapus opsi user-agent. Menunggu CI.
- 2026-10-23: LogRedact (core:common): samarkan URL (hanya skema+host), token/kredensial, dan path lokal di log. Fungsi murni, diuji lokal dengan kotlinc (19 kasus lulus). Belum dipakai modul lain. Menunggu CI.
- 2026-10-23: MpvLog (feature/mpv) menggantikan android.util.Log: pesan disamarkan lewat LogRedact lalu dikirim ke logcat dan AppLog (LogGrouping, sumber mpv). Menutup kebocoran URL/token lewat onNewIntent. feature/mpv kini bergantung pada :core:common. Menunggu CI.
- 2026-10-23: PlaybackLog (feature/mpv): catat Permintaan, Mulai, Dimuat (durasi, judul) dan Selesai (posisi) ke tab Log (sumber mpv, tag playback), semua lewat LogRedact. 4 titik panggil di MPVActivity (START_FILE, FILE_LOADED, END_FILE, onNewIntent). Batasan: alasan end-file tidak tersedia di JNI. Menunggu CI + uji perangkat.
- 2026-10-23: Audit pelepasan sumber daya mpv: MPVActivity.onDestroy kini melepas observer lebih dulu (menutup race receiver becomingNoisy yang bisa terdaftar ulang) dan membersihkan eventUiHandler/fadeHandler/stopServiceHandler; AboutActivity melepas log observer di onDestroy (sebelumnya Activity bisa tertahan di daftar statis MPVLib). Menunggu CI + uji perangkat.
- 2026-10-23: Cek tipe PlaybackLog vs MPVLib.kt/LogRedact/MpvLog/PlaybackState: semua cocok. PlaybackLog.onEnd diperbaiki: tidak mencatat bila tanpa START_FILE (startedAt 0) dan mereset state setelah dicatat, sehingga END_FILE ganda tidak menghasilkan log Selesai ganda atau elapsed salah. Menunggu CI.
- 2026-10-23: Pelacakan aktivitas pemutaran (poin 2): PlaybackLog kini punya LogObserver khusus yang menangkap log mpv level ERROR/FATAL (disamarkan LogRedact, dedupe, maks 20 per file) ke AppLog, dan melampirkan galat terakhir sebagai penyebab pada log 'Berakhir sebelum dimuat'. attach() sebelum player.initialize(), detach() di onDestroy. Menunggu CI + uji perangkat.
- 2026-10-24: Tab Log: Error memakai MolinaTheme.Bad (merah lembut), Warn memakai MolinaTheme.Warn (kuning), baris sumber mpv diberi ikon putar ungu (MolinaTheme.Cinema, CinemaPurple jadi alias); filter Cinema kini mencakup sumber 'mpv' (sebelumnya log pemutar tidak pernah muncul di filter itu). ExitHooks mpv: MpvLifecycle (markCreated/destroyIfAlive) menjaga MPVLib.destroy() agar tidak ganda atau tanpa create; hook 'mpv' dipasang di MolinaApp dengan batas waktu 1,5 dtk. Menunggu CI + uji perangkat.
- 2026-10-24: Fase 5b MpvSettings + MpvPrefs: fasad bertipe di atas SharedPreferences bawaan (layar setelan XML menulis ke berkas yang sama, jadi bukan DataStore; kunci tidak berubah). MPVView.initOptions, MPVActivity.readSettings/writeSettings, dan TouchGestures.syncSettings kini membaca lewat MpvPrefs (snapshot + StateFlow, tahan ClassCastException). Sisa pembacaan langsung: status layar FilePicker/MainScreenFragment dan material_you_theming (ditunda ke Fase 7). Menunggu CI + uji perangkat.
- 2026-10-24: Fase 5c-2 (event ke AppLog): AudioLog menerjemahkan audio focus (GAIN/LOSS/LOSS_TRANSIENT/LOSS_CAN_DUCK, sumber sistem/output dicabut/permintaan, penanda diabaikan; focus ditolak jadi peringatan); BackgroundPlaybackService mencatat mulai/berhenti/mpv berhenti/jeda-lanjut; MPVActivity mencatat pemutaran latar dan kegagalan memulai layanan. Hanya log, tanpa perubahan perilaku. Menunggu CI + uji perangkat; centang 5c di BLUEPRINT.md setelah lulus.
- 2026-10-24: docs: tambah seksi G (Cinema/mpv, Fase 5) di docs/UJI_PERANGKAT.md — 14 item mencakup putar lokal/tautan, gestur, persistensi MpvPrefs, reset, exit hook mpv, AppExit saat main, tab Log (PlaybackLog + filter Cinema), error mpv, pemutaran latar, audio focus, simpan posisi, ingat kecerahan, peringatan keluar playlist. Prasyarat sebelum putaran uji gabungan Fase 5 (5b/5c sudah selesai kode, menunggu CI + uji ini sebelum dicentang di BLUEPRINT.md).
- 2026-10-24: feat(mpv): badge LIVE ungu (MolinaTheme.Cinema) di Cinema Home. BackgroundPlaybackState (StateFlow<Boolean> baru di feature/mpv) ditulis BackgroundPlaybackService.onStartCommand/onDestroy, dibaca CinemaHome lewat collectAsState; LiveBadge (CinemaParts.kt) dipakai saat service aktif. Tanpa ubah Gradle/Manifest/JNI. docs: tambah 2 item ke seksi D (notifikasi+exit mpv) dan 1 item ke seksi F (konsistensi warna) serta seksi G (14 item Cinema/mpv) di docs/UJI_PERANGKAT.md. Menunggu CI + uji perangkat.
- 2026-10-24: Riset 5d-0 (tanpa run CI): koordinat JitPack NewPipeExtractor yang benar adalah kandidat pertama `com.github.TeamNewPipe:NewPipeExtractor:$VER` (sesuai README resmi TeamNewPipe/NewPipeExtractor dan halaman JitPack); kandidat kedua di run.sh aman dibiarkan sebagai fallback tapi tak akan terpakai. minSdk proyek (app & feature/mpv) = 26, sama dengan syarat minimum NewPipeExtractor untuk desugaring ('minSdkVersion below 26 requires API desugaring') -> TIDAK perlu core library desugaring. Spike CI (workflow_dispatch spike-newpipe.yml) belum dijalankan -> perlu dipicu manual dari tab Actions oleh pemilik repo sebelum lanjut ke integrasi modul newpipe sungguhan.
- 2026-10-24: UI player mpv: (1) grup ikon top_controls (Lock/PiP/Settings) dipindah dari pojok kanan atas ke tepat di atas teks end time (layout_above + alignRight @id/controls, marginRight 48dp = lebar nextBtn, marginBottom 4dp); controls marginTop 60dp jadi 0dp karena RelativeLayout memasukkan margin atas anchor ke posisi layout_above. Show/hide/fade tetap lewat showControls/hideControls, tanpa ubah Kotlin. (2) Dialog Settings (genericMenu: menu atas + Advanced) memakai tema AppTheme.PlayerDialog: latar #0F0F0F (@color/player_dialog_bg), teks putih; alpha_darken diganti alpha_lighten di dialog_top_menu/dialog_advanced_menu agar tombol Prev/Next dan grup Advanced tetap terlihat. Dialog picker lain (track, playlist, speed) belum disentuh. Menunggu CI + uji perangkat.
- 2026-10-24: UI player mpv: grup ikon top_controls kini berlatar pill (drawable bg_pill_controls, #bf000000, radius 28dp) dan dialog Settings memakai dialogCornerRadius 28dp agar selaras. Fitur baru "Audio only" di menu Settings: toggle property mpv vid (no/auto) lewat toggleAudioOnly(); UI audio dipicu observer current-tracks/video/image yang sudah ada; tombol berbagi baris rowMode dengan "Play in background" supaya tinggi dialog (tidak scroll) tidak bertambah; tombol disembunyikan bila tak ada video+audio dan mode belum aktif. genericMenu mendapat parameter onViewReady (default no-op). Tanpa ubah Gradle/Manifest/JNI. Mini player ditunda: menunggu konfirmasi definisi (pop-up overlay vs PiP). Menunggu CI + uji perangkat (cek UI audio benar-benar berpindah setelah vid=no).
- 2026-10-24: Player mpv: ikon cari/URL di dalam player (topSearchBtn, awal grup pill) membuka dialog URL bertema gelap tanpa keluar dari player: Play (loadfile replace, lanjut main walau sebelumnya jeda) atau Add to queue (append), tetap lewat Utils.normalizeUrl + validasi skema + PlaybackLog.onRequest. Utils.OpenUrlDialog: parameter tema opsional, hint, tombol netral ikut aktif/nonaktif (pemanggil lama tidak berubah). Pencarian JUDUL belum ada: menunggu NewPipe (5d); saat siap, kolom yang sama menerima teks biasa dan menampilkan daftar hasil lewat kontrak StreamResolver. Mini player ditunda/diganti fitur ini. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Catatan CI: CI tidak terpicu karena HEAD push berisi commit docs dengan tag skip-ci, sehingga seluruh push (termasuk commit kode di dalamnya) dilewati GitHub. Terdampak: 3 push hari ini (layout end time, dialog gelap, pill, Audio only, ikon cari/URL). Build dipicu manual (workflow_dispatch) dari main. Aturan alur: push commit KODE dulu (CI jalan), baru commit docs dengan tag skip-ci di push terpisah; jangan gabungkan keduanya dalam satu push. Menunggu hasil CI gabungan + uji perangkat.
- 2026-10-24: Playlist mpv: dialog kini AppTheme.PlayerDialog (latar #0F0F0F, teks putih; ikon shuffle/repeat di-tint putih agar terlihat). Item yang sedang diputar berwarna biru (@color/player_playlist_active #4DA3FF) dan tebal; shuffle/repeat aktif juga biru. Tombol prev/next PiP dan notifikasi latar kini dibangun ulang saat playlist-count berubah (sebelumnya notifikasi hanya membaca sekali saat layanan mulai). Dialog track belum disentuh. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Open lokal mpv: berkas yang dipilih memuat seluruh media satu folder sebagai playlist (urut alami, mulai dari berkas terpilih; berkas tersembunyi dan cue/m3u diabaikan; subfolder tidak ikut). Utils.folderPlaylist, BaseMPVView.playFolder (loadfile replace + append + playlist-move), extra folder_playlist dari MainScreenFragment divalidasi isAllowedCustomPath di MPVActivity. Hanya pemilih berkas biasa (path nyata), bukan SAF content://. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Sakelar "Play entire folder" (kunci play_folder, default aktif) di Settings > UI lewat MpvSettings.playFolder dan MpvPrefs. MainScreenFragment membaca nilainya sebelum mengirim folder_playlist ke MPVActivity; bila dimatikan, berkas lokal diputar tunggal seperti semula. Teks baru hanya bahasa Inggris (locale lain memakai bawaan). Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Folder playlist untuk Cinema Home: tombol File di Cinema Home memakai SAF (ACTION_VIEW), bukan pemilih lama, jadi MPVActivity kini memuat folder untuk ACTION_VIEW bila play_folder aktif dan URI terselesaikan ke path nyata (findRealPath). Bila tidak, tercatat di Log 'Playlist folder tidak dipakai'. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Tombol prev/next player digeser ke dalam: baris seekbar (controls_seekbar_group) diberi padding kiri/kanan 16dp; margin kanan top_controls 48dp ke 64dp agar tetap rata dengan teks durasi. Hanya XML, mode audio tidak berubah. Menunggu CI + uji perangkat.
- 2026-10-24: Fix mpv: `isAllowedCustomPath` menolak `memory://` (skema tidak ada di allowlist), sehingga playlist multi-pilih berujung "No file given". Kini playlist `memory://#EXTM3U` diterima hanya bila 1-1000 entri dan tiap entri lolos validasi entri tunggal (skema diizinkan atau path non-privat; `memory://` bersarang ditolak). Kotlin saja, tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat (multi-pilih berkas).
- 2026-10-24: docs: `docs/HANDOFF.md` ditulis ulang (versi lama rusak: 101 byte NUL, tidak bisa dipulihkan dari git). Isi: aturan kerja termasuk aturan push (kode dulu, docs `[skip ci]` terpisah), arsitektur, status fase, utang sebelum 5d, jebakan, langkah berikutnya. Klaim `TODO: headers` dan `StreamResolver belum ada di kode` diverifikasi di source; status CI commit terbaru belum terverifikasi.
- 2026-10-24: Fase 5c-hdr (kontrak + header): `core:common` kini punya kontrak `StreamResolver` (`ResolvedStream`, `StreamSearchItem`, `StreamOutcome`, `StreamError`) dan `StreamHeaders` (saring header: nama token valid, tanpa CR/LF/kontrol, blokir Host/Content-Length/Transfer-Encoding/Connection/Upgrade, maks 16 header, nilai maks 2048). `feature/mpv`: `MpvLoad` menyusun `loadfile <url> <flag> -1 http-header-fields=...` (panjang-tetap `%N%` berlapis, byte UTF-8) sehingga header menempel per entri playlist; `BaseMPVView.playFile(path, headers)`; `MPVActivity` membaca extra intent `headers` (String[] berpasangan, null aman, maks 64 elemen) di onCreate dan onNewIntent; log hanya jumlah dipakai/ditolak. `tls_verify` dari intent sengaja tidak didukung. Bagian murni diuji dengan kotlinc 2.0.21 (14 pemeriksaan lulus); kode Android BELUM dikompilasi (menunggu CI) dan perilaku loadfile+opsi BELUM diuji di perangkat. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: CI #62 hijau untuk commit f7c64ba (kontrak StreamResolver + header per entri); belum diuji di perangkat. docs: seksi G `UJI_PERANGKAT.md` dilengkapi 7 item (kontrol pill, dialog gelap/playlist biru, Audio only, ikon cari/URL, folder playlist + sakelar, notifikasi prev/next, multi-pilih memory://); BLUEPRINT Fase 5 dirinci per sub-fase: 5a dicentang (artefak native dipin dan terverifikasi), 5b/5c/5d sengaja BELUM dicentang sampai uji perangkat. Langkah berikut: pemilik menjalankan `spike-newpipe.yml` (Actions > Run workflow) dan menguji seksi G.
- 2026-10-24: Spike NewPipe v0.26.5 HIJAU: 7 artefak ter-resolve dengan SHA-256 (NewPipeExtractor, jsoup 1.22.2, jsr305 3.0.2, nanojson, protobuf-javalite 4.35.1, rhino 1.8.1, rhino-engine 1.8.1). Repo diverifikasi di HEAD b5f17da: StreamResolver dan seksi G sesuai. Catatan 5d: perlu repositori JitPack terbatas (includeGroup TeamNewPipe) di settings.gradle.kts dan keputusan verification-metadata; keduanya menunggu persetujuan. Dokumen HANDOFF/BLUEPRINT masih menulis spike belum dijalankan, diperbarui di langkah berikut. Uji perangkat APK CI #62 (G-memory, Header stream) masih menunggu.
- 2026-10-24: 5d-1 rancangan :engine:newpipe disusun (belum ada kode): modul JVM murni, MolinaDownloader dengan allowlist host runtime, StreamResolverRegistry di core:common; tahap d1 Gradle+gate, d2 resolve, d3 pencarian. Temuan: gate memblokir jitpack.io di .kts (perlu BUILD_ONLY_HOSTS). Hash 7 jar spike disimpan di scripts/newpipe/pins.sha256; HANDOFF/BLUEPRINT dikoreksi. Menunggu keputusan pemilik: JitPack+gate, pin A/B, cakupan iterasi 1, registry.
- 2026-10-24: Utang kecil: onNewIntent kini memuat folder sebagai playlist (sebelumnya hanya onCreate). Logika diekstrak ke MPVActivity.resolveFolderPlaylist dan loadReplacing; BaseMPVView.loadFolder jadi publik. Mode antre latar (append) tidak memuat folder. Validasi isAllowedCustomPath dan sakelar play_folder tidak berubah. Kotlin saja, tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Dialog player disamakan ke tema gelap: 8 AlertDialog di MPVActivity (track, subtitle track, hwdec, chapter, aspek, picker speed/delay audio, delay subtitle, konfirmasi keluar playlist) kini memakai AppTheme.PlayerDialog (#0F0F0F, sudut 28dp). Kotlin saja, tanpa ubah Gradle/Manifest/JNI. Dialog Cinema Home dan layar preferensi belum disentuh. Menunggu CI + uji perangkat.
- 2026-10-24: Notifikasi latar: ikon aksi khusus (ic_notif_play/pause/prev/next) hitam di drawable dan putih di drawable-night agar terlihat di notifikasi gelap; setColor aksen merek #4DA3FF (cover tetap menimpa). Ikon bersama UI player/PiP tidak diubah. Varian gelap tak bisa diuji di Android 8.0. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Fitur baru Sleep timer (Settings > Advanced): 15/30/45/60/90 menit, fade-out volume mpv 10 detik terakhir lalu jeda, volume dipulihkan; label menampilkan sisa waktu; dibatalkan di onDestroy. Logika murni di SleepTimer.kt, diuji JVM dengan kotlinc (20 pemeriksaan lulus: alur normal, batal, jeda manual, mulai ulang, volume null). Teks baru hanya bahasa Inggris. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.

- 2026-10-24: Audit 5d: NewPipe belum diimplementasikan. Ada: kontrak StreamResolver (core:common), spike 5d-0 HIJAU (7 hash di scripts/newpipe/pins.sha256, NewPipeExtractor v0.26.5). Belum: modul :engine:newpipe, implementasi resolver, wiring ke dialog URL player. Koreksi baris riset 5d-0: spike sudah dijalankan. Urutan: uji perangkat seksi G, lalu :engine:newpipe. Docs saja.

- 2026-10-24: Fase 5d-1 (engine NewPipe): modul `:engine:newpipe` (Kotlin/JVM) ditambahkan: NewPipeStreamResolver (canResolve lewat LinkType.STREAM, resolve, search YouTube; galat dipetakan ke StreamError, detail hanya nama kelas, pembatalan diteruskan lewat runInterruptible), MolinaDownloader (HttpURLConnection, HTTPS saja, gzip, batas 8 MiB, 429 -> ReCaptchaException), StreamPicker (live -> HLS; selain itu video progresif ber-audio tertinggi; lalu audio; expire diparse dari URL). Rantai pasok: JitPack dibatasi grup com.github.TeamNewPipe; tugas verifyNewPipePins mencocokkan 7 jar di runtimeClasspath dengan scripts/newpipe/pins.sha256 sebelum kompilasi. Gerbang telemetri: host jitpack.io diizinkan SADAR (build-time). :app bergantung ke engine (validasi dexing), belum dipanggil UI. Batas v1: tanpa video-only+audio (kualitas mengikuti stream progresif YouTube). Menunggu CI (hasil menentukan 5d-2: wiring dialog URL/pencarian).

- 2026-10-24: Run #67 hijau (5d-1: modul :engine:newpipe, pin terverifikasi, dexing lolos). Fase 5d-2a (wiring NewPipe ke player): core:common.StreamResolvers (pintu resolver aktif, pembuat malas), MolinaApp memasang NewPipeStreamResolver; feature:mpv.StreamLoader (resolve di IO, header disaring StreamHeaders, galat jadi toast per StreamError); MPVActivity.loadWeb dipakai di dialog URL player (putar/antre), antrean playlist, dan onNewIntent; startup (Cinema Home/share) lewat playWhenReady (muat langsung bila surface sudah ada, jika belum antre ke surfaceCreated); MpvLoad.loadfileArgs menerima judul (force-media-title per-entri). Galat saat startup: toast lalu keluar. Batas: pencarian judul (5d-2b) dan video-only+audio (5d-3) belum. Menunggu CI + uji perangkat.

- 2026-10-24: Perbaikan CI 5d-2a: :app gagal kompilasi (Cannot access Localization/ContentCountry) karena konstruktor NewPipeStreamResolver memakai tipe NewPipe di parameter default sedangkan NewPipeExtractor `implementation` di :engine:newpipe. Diganti parameter String (languageCode/countryCode); tipe NewPipe dibuat di dalam engine, API publik engine bebas tipe NewPipe. Menunggu CI.

- 2026-10-24: Backlog prioritas 5d (dicatat agar tidak lupa): (1) uji perangkat 5d-2a (URL YouTube dari Cinema Home, dialog URL player, antrean, galat) + ukur memori (RAM bar status: idle, saat putar, setelah tutup) + cek 403 googlevideo (hapus opsi user-agent mpv bila perlu); (2) centang 5b/5c di docs/BLUEPRINT.md (uji perangkat seksi G dilaporkan lulus); (3) 5d-2b pencarian judul (kolom URL menerima teks, daftar hasil dengan thumbnail/judul/uploader/durasi/LIVE, lewat StreamResolver.search); (4) 5d-3 gabung video-only + audio (tambah audioUrl di ResolvedStream, audio-add di mpv) agar tidak mentok 360p; (5) fallback yt-dlp di Debian PRoot, opt-in, untuk TikTok/Facebook/Instagram (NewPipeExtractor v0.26.5 hanya YouTube, SoundCloud, media.ccc.de, PeerTube, Bandcamp; build mpv tanpa hook ytdl); rapuh, Facebook privat butuh login; (6) bump NewPipe berkala: jalankan workflow spike-newpipe dengan versi baru, perbarui scripts/newpipe/pins.sha256, naikkan versi di engine/newpipe/build.gradle.kts; siapkan aturan R8 rhino/jsoup/protobuf sebelum rilis. Memori: libmpv sudah dihancurkan di onDestroy dan cache demuxer dibatasi (32 MB maju + 32 MB mundur di API 26); sisa memori NewPipe/rhino tidak bisa di-unload, ukur dulu, opsi lanjutan player di proses terpisah (refactor LogBus/Prefs/StateFlow lintas proses). Docs saja.
- 2026-10-24: Audit repo (HEAD a6a5d5b): pencarian judul YouTube belum bisa karena 5d-2b belum dikerjakan. StreamResolver.search() ada di :engine:newpipe tetapi belum dipanggil UI; dialog URL masih memakai Utils.normalizeUrl. Link YouTube lengkap (5d-2a) sudah terpasang, menunggu uji perangkat. Docs saja.
- 2026-10-24: CI hijau untuk a16343f: core library desugaring (desugar_jdk_libs_nio 2.1.5) aktif di :app, mengikuti molina_suite (NewPipeExtractor v0.26.5, minSdk 26). Koreksi riset 5d-0: minSdk 26 TIDAK cukup tanpa desugaring. Hasil uji perangkat link YouTube: <isi setelah tes>.
- 2026-10-24: Fix Cinema Home: CinemaActions.classify tidak lagi menolak tautan YouTube (NeedsExtractor); semua http(s) diteruskan ke MPVActivity yang resolve lewat StreamLoader/NewPipe. Koreksi catatan 5d-2a: sisi Cinema Home sebelumnya belum tersambung. Menunggu CI + uji perangkat.
- 2026-10-24: 5d-2b (versi awal): pencarian judul di Cinema Home. CinemaActions.classify kini Direct/Query/Invalid/Empty (NeedsExtractor dihapus) dan menormalkan tautan tanpa skema (www., youtu.be). Judul dicari lewat StreamResolvers.get().search di IO, hasil tampil sebagai daftar (judul, uploader, durasi), ketuk untuk memutar lewat 5d-2a. Belum ada thumbnail (referensi: ThumbnailLoader di molina_suite). Menunggu CI + uji perangkat.
- 2026-10-24: Fase 5d-2c: playlist hasil pencarian (jendela bergulir). core:common: `PlaylistPlan` (validasi extra Intent: hanya URL watch YouTube https, 2-50 item, tanpa duplikat, indeks cocok dengan URL yang dibuka), `RollingQueue` (satu item di depan, item gagal dilewati) dan `PlaylistExtras`. :app: `CinemaActions.playResults` mengirim hasil yang diketuk beserta daftar hasil valid; `CinemaHome` memakainya (validasi `classify` tetap). :feature:mpv: `MPVActivity` membaca extra (onCreate dan loadReplacing), dan saat FILE_LOADED meresolve item berikutnya di latar lalu `loadfile append` bila antrean mpv tak punya item sesudah posisi kini; hasil resolve yang datang setelah playlist diganti dibuang; `PlaybackLog` mencatat playlist, item ditambahkan, dan item dilewati (tanpa URL). Item di atas hasil yang diketuk tidak diantrekan; Prev hanya ke item yang sudah diputar. Batas: shuffle/repeat mpv hanya mencakup item yang sudah di-resolve; menekan Next sebelum item berikutnya siap tidak berpindah. Logika murni diuji kotlinc 2.0.21 (37 pemeriksaan lulus); fungsi baru di MPVActivity lolos kompilasi dengan stub; kode Android BELUM dikompilasi penuh (menunggu CI) dan BELUM diuji di perangkat. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat (hasil pencarian jadi playlist, Prev/Next, notifikasi, item gagal).
- 2026-10-24: Fase 5d-2d: dialog playlist penuh + Reset segarkan memori. Dialog playlist player kini menampilkan seluruh hasil pencarian: item yang sudah di-resolve memakai judul dari peta URL stream->judul (`MPVActivity.streamTitles`, menggantikan nama berkas stream), sisanya baris redup dari `RollingQueue.pendingIndices()` (`PendingRow`). Mengetuk baris redup me-resolve SEMUA item dari kursor sampai item itu berurutan (`RollingQueue.beginForced`, urutan terjaga, item gagal dilewati), lalu melompat ke yang terakhir berhasil; ditolak (toast) bila prefetch sedang berjalan. Tombol Reset di Cinema Home kini membersihkan hasil pencarian dan menyegarkan memori ringan: `StreamResolvers.release()` (bila tidak ada video di latar), GC, lalu menampilkan RSS proses sebelum/sesudah (`MemoryRefresh`, baca /proc/self/status). Batas: tidak bisa mengembalikan memori native libmpv yang masih dipegang alokator (API 26 tanpa trim), jadi RAM bisa tetap tinggi; angka RSS dipakai untuk memutuskan pemisahan player ke proses sendiri (android:process, ubah Manifest + lintas proses LogBus/Prefs/BackgroundPlaybackState) sebagai fase tersendiri. Mulai ulang proses sengaja TIDAK dipakai karena AppExit mematikan sesi Terminal. Uji kotlinc: RollingQueue 49 pemeriksaan, MemoryRefresh 11 pemeriksaan lulus; fungsi baru MPVActivity lolos kompilasi dengan stub; PlaylistDialog dan CinemaHome BELUM dikompilasi (menunggu CI) dan BELUM diuji di perangkat. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI + uji perangkat.
- 2026-10-24: Fase 5d-fallback (adopsi mpv dari molina_suite, butir 1+2): :core:common kini punya MiniJson (+JsonCursor, pengurai JSON objek tanpa dependensi; maks 4 juta karakter, kedalaman 32, rusak -> null), YtdlpParse (FORMAT: H.264 dulu, tanpa AV1, maks 1080p; single() mengubah keluaran yt-dlp -J menjadi ResolvedStream, hanya stream gabungan, header disaring StreamHeaders, kedaluwarsa dari URL) dan YtdlpSearch (baris --dump-json menjadi StreamSearchItem, terbaru ke terlama, tanpa duplikat, tautan media kedaluwarsa tidak dipakai). Belum ada pemanggil: resolver yt-dlp opt-in di Debian PRoot belum ditulis. Dari Suite hanya selektor format dan logika baca hasil pencarian; ytdl_hook, NewPipeYtdlServer, dan log mpv verbose permanen sengaja tidak diadopsi. Diuji kotlinc 2.0.21 di sandbox (49 pemeriksaan lulus) dan gerbang telemetri lulus; BELUM dikompilasi Gradle (menunggu CI) dan BELUM diuji di perangkat. Tanpa ubah Gradle/Manifest/JNI. Menunggu CI.

## 2026-10-24 - Persiapan yt-dlp resolver & Refresh Sistem
- [x] #75 parser JSON mini, selektor H.264, pembaca keluaran yt-dlp (CI hijau)
- [x] Cek rootfs: python3 3.13.5 ada; yt-dlp & pip3 belum terpasang
- [ ] Pasang yt-dlp zipapp ke /usr/local/bin (dijalankan user di Terminal)
- [ ] core:common YtdlUrls.needsYtdl + CompositeStreamResolver
- [ ] core:common Refreshable + ProcessReaper (SIGTERM lalu SIGKILL grup proses)
- [ ] app YtdlpRunner + YtdlpStreamResolver (sakelar default mati)
- [ ] app ikon Refresh Sistem di Pengaturan (mpv / terminal / code nanti / semua)

## 2026-10-24 - yt-dlp terpasang & langkah 1 resolver
- [x] yt-dlp 2026.10.21 terpasang di rootfs Debian (zipapp, /root/.local/bin dan /usr/local/bin)
- [x] core:common YtdlUrls.needsYtdl + CompositeStreamResolver (menunggu CI)
- [ ] app YtdlpRunner + YtdlpStreamResolver (sakelar default mati)
- [ ] core:common Refreshable + ProcessReaper; ikon Refresh Sistem di Pengaturan

## 2026-10-24 - Refresh Sistem + yt-dlp resolver (langkah 2 dan 3)
- [x] core:common Refreshable/Refreshers, ProcessReaper (SIGTERM, tunggu, SIGKILL, verifikasi; lindungi diri, init, logcat), ProcFs, YtdlpCommand (argv proot, klasifikasi galat), YtdlpInstall (SHA2-256SUMS)
- [x] app YtdlpRunner (satu proses sekali jalan, batas waktu/ukuran, pohon proses dimatikan), YtdlpStreamResolver (sakelar default mati), YtdlpInstaller (unduh + verifikasi SHA-256 ke rootfs), dipasang di MolinaApp lewat CompositeStreamResolver
- [x] app ikon Refresh di Pengaturan (mpv, Terminal, Code disiapkan) + halaman Mesin cadangan (yt-dlp)
- [ ] menunggu CI hijau, lalu uji perangkat

## 2026-10-24 - Perbaikan gerbang telemetri
- [x] CI: telemetry_gate mengizinkan github.com (unduh yt-dlp opt-in) - menunggu CI hijau

## 2026-10-24 - Refactor StreamEnginePrefs
- [x] refactor(app): StreamEnginePrefs jadi satu StateFlow<Boolean> (hapus isYtdlpEnabled, cermin Compose) - menunggu CI hijau

## 2026-10-24 - CI hijau
- [x] CI hijau: Refresh Sistem, yt-dlp resolver/pemasang, StreamEnginePrefs StateFlow (uji perangkat belum)
- [ ] uji perangkat 1-5 (ikon Refresh, Mesin cadangan, TikTok/Facebook)
- [ ] 5d-3: video terpisah (stream video + audio)

## 2026-10-24 - TikTok 403 (unduh ke cache)
- [x] fix(stream): TikTok 403 di mpv -> mode unduh-ke-cache (yt-dlp mengunduh, mpv memutar berkas lokal), StreamCache (maks 5 berkas/300 MB), mpv ytdl=no - menunggu CI hijau
- [ ] uji perangkat: link TikTok, cek log 'unduh ke cache selesai'
- [x] verifikasi Terminal: `yt-dlp -J --no-simulate` memuat requested_downloads[].filepath (853074 byte, TikTok) - asumsi parser terbukti
- [x] feat(system): Refresh Pemutar ikut mengosongkan cache unduhan yt-dlp (/tmp/molina-stream) setelah mpv berhenti - menunggu CI hijau

## 2026-10-24 - Keamanan jaringan
- [x] feat(security): UrlPolicy (daftar putih skema, blok loopback/metadata/SSRF, LAN opt-in), GuardedStreamResolver, gerbang di MpvLoad, mpv load-unsafe-playlists=no, yt-dlp --ignore-config, NetworkSecurityConfig, halaman Keamanan jaringan - menunggu CI hijau
- [ ] uji perangkat: putar https biasa, TikTok, URL http://127.0.0.1 (harus ditolak), LAN dengan sakelar mati/hidup

## 2026-10-24 - Pengerasan tahap 2
- [x] feat(security): tahap 2 - gerbang URL untuk subtitle intent, judul intent dibersihkan, BROWSABLE dicabut dari filter berkas lokal, layar privat (FLAG_SECURE + filter sentuhan) dengan sakelar, scripts/native_audit.sh - menunggu CI hijau
- [ ] jalankan `bash scripts/native_audit.sh` pada pustaka native hasil CI; catat peringatan RELRO/BIND_NOW
- [ ] uji perangkat: screenshot Terminal (harus hitam), Recents, sakelar Layar privat, TikTok tetap jalan

- Audit jaringan tahap 3 (hanya baca): redirect/segmen/playlist belum dijaga UrlPolicy; protocol_whitelist belum ada; memory:// terblokir CONTROL_CHARS. Rencana: A (memory+skema), B (whitelist per entri), C (Molina Shield).

## 2026-10-24 - Jaringan tahap 3 (A+B)
- [x] fix(security): playlist memory:// diperiksa per baris lewat UrlPolicy (sebelumnya seluruh string ditolak CONTROL_CHARS), daftar skema intent disamakan dengan UrlPolicy, path privat dicek juga untuk file:// - menunggu CI hijau
- [x] feat(security): ffmpeg protocol_blacklist global (concat, subfile, ftp, sftp, unix, md5, gopher, pipe, fd, tee) dan protocol_whitelist ketat per entri http/https (http,https,tls,tcp,crypto,data) - menunggu CI hijau
- [ ] uji perangkat: https biasa, HLS (m3u8) jarak jauh, TikTok, bagikan 2+ berkas ke Molina (memory://), video lokal dari /sdcard, rtmp/rtsp bila ada
- [ ] belum dijaga: redirect/segmen ke alamat privat (butuh Molina Shield, tahap C); m3u8 lokal belum dibatasi whitelist
- 2026-10-24: Hasil uji IPTV Studio: URL yang diuji adalah stream langsung (bukan M3U), playlist-count=1 sehingga wajar. Dugaan whitelist/load-unsafe-playlists belum terbukti (belum ada uji dengan file .m3u). Studio belum punya fitur daftar channel IPTV; sumber daftar di Suite masih dicari. Catatan terpisah: h264_mediacodec "surface and native_window are NULL" (jatuh ke decode software).
- 2026-10-24: Diagnosis akar masalah daftar IPTV: Suite memakai mpv ytdl_hook (yt-dlp -J) sehingga URL halaman diperluas jadi playlist; Studio memakai yt-dlp default mati, --no-playlist, dan YtdlpParse.single menolak playlist. Patch whitelist .m3u sebelumnya tidak relevan untuk kasus ini. Rencana: dukungan playlist yt-dlp di resolver Studio (menunggu persetujuan).
- 2026-10-24: Fix daftar IPTV: yt-dlp kini boleh mengembalikan playlist (-J --flat-playlist, tanpa --no-playlist bila URL tak memuat list=/playlist=). YtdlpParse.resolve (maks 200 entri), ResolvedStream.entries, GuardedStreamResolver menyaring entri lewat UrlPolicy, MPVActivity.appendResolved menambah sisa entri setelah item pertama (BaseMPVView.playWhenReady punya callback after). yt-dlp "Unsupported URL" diteruskan mentah ke mpv. Bagian murni diuji kotlinc 2.0.21 (14 pemeriksaan lulus); kode Android BELUM dikompilasi (menunggu CI) dan BELUM diuji di perangkat. Prasyarat uji: aktifkan Mesin cadangan (yt-dlp). Tanpa ubah Gradle/Manifest/JNI.

## 2026-10-24 - Daftar IPTV hanya 1 item
- [x] fix(stream): .m3u/.pls masuk DIRECT_EXT sehingga tidak lewat yt-dlp (yt-dlp membaca .m3u sebagai 1 stream HLS); mpv memecah daftar sendiri - menunggu CI hijau
- [ ] uji perangkat: https://iptv-org.github.io/iptv/countries/id.m3u -> playlist-count harus >1, daftar di PlaylistDialog terisi
- [ ] jika masih 1: kirim log `playlist-count` dan baris `stream`/`policy` (cek load-unsafe-playlists)

## 2026-10-25 - Molina Shield C1 (proxy keamanan jaringan)
- [x] feat(security): ShieldProxy (proxy HTTP/CONNECT di 127.0.0.1, port acak + token), ShieldPolicy (cek IP hasil DNS, anti-rebinding, port sensitif), TrackerList, ShieldStats; mpv memakai http_proxy lewat stream-lavf-o/demuxer-lavf-o; whitelist protokol per entri + httpproxy - menunggu CI hijau
- [x] terverifikasi dengan ffprobe 6.1 + proxy rujukan: HTTP memakai GET URL absolut, HTTPS memakai CONNECT, token lewat tantangan 407, HLS (playlist+segmen) lewat proxy, redirect ke loopback diblokir di lompatan kedua
- [ ] belum terbukti: build FFmpeg n9.0.2 + mbedtls milik Molina (CONNECT + TLS verifikasi host), kompilasi Kotlin (belum dikompilasi di sandbox)
- [ ] uji perangkat: IPTV id.m3u (http polos), https biasa, HLS, TikTok, dan http://127.0.0.1 / redirect ke LAN (harus diblokir, cek log 'shield')
- [ ] tidak tercakup: rtmp/rtsp, yt-dlp di Debian. Berikutnya: C2 (pembersihan header, UI gembok) lalu D (perkecil build FFmpeg)

## 2026-10-25 - Molina Shield C2 (UI dan pengaturan)
- [x] C1 hijau dan lolos uji perangkat (IPTV, pemutaran normal)
- [x] feat(security): ShieldPrefs (sakelar Shield, blokir pelacak, hapus Referer) + bagian Molina Shield di Pengaturan > Keamanan jaringan (jumlah blokir + 8 kejadian terakhir: host dan alasan, tanpa URL) + lencana gembok/peringatan/perisai di bilah atas pemutar - menunggu CI hijau
- [ ] uji perangkat: lencana muncul (https=gembok, http=peringatan), ketuk lencana = info, sakelar Shield mati lalu Refresh Pemutar = pemutaran tetap jalan, blokir 127.0.0.1 tercatat di daftar
- [ ] berikutnya: daftar host LAN tepercaya per host (C3), lalu D (perkecil build FFmpeg + audit native)

## 2026-10-25 - Molina Shield D1 (perkecil permukaan serangan FFmpeg)
- [x] C2 hijau dan diuji di perangkat
- [x] feat(security): native/molina_ffmpeg_profile.sh = daftar putih FFmpeg (12 protokol, ~70 demuxer; default upstream 41 protokol dan 361 demuxer) + flag pengerasan (stack-protector-strong, FORTIFY, RELRO penuh, noexecstack); disisipkan ke ffmpeg.sh upstream oleh scripts/native_mpv.sh; scripts/ffmpeg_components_audit.sh memeriksa config_components.h hasil build (GAGAL bila ada ftp/concat/pipe/dst. atau jumlah melebihi batas); hasil audit komponen dan pengerasan ikut BUILD-INFO.txt
- [x] terverifikasi: configure FFmpeg SHA dipin dengan mbedtls+libdav1d+libxml2 lolos; hitungan 41->12 protokol, 361->70 demuxer; audit lulus pada profil dan GAGAL pada build default (uji negatif)
- [ ] BELUM terbukti: kompilasi penuh (sandbox 1 core), build Android arm64, dan pemutaran. Jalankan workflow Native mpv (manual), lalu isi NATIVE_TAG dan NATIVE_SHA256 baru di native/prebuilt.env, lalu CI APK
- [ ] uji perangkat setelah pin baru: mkv/mp4/mp3/ts, IPTV id.m3u, HLS, DASH, rtsp/rtmp bila punya, subtitle .srt/.ass, gambar. Format di luar daftar (mxf, wtv, swf, nut, dll.) memang tidak akan terbuka
- [ ] D2 (berikutnya): daftar putih decoder setelah D1 terbukti; native_audit dijadikan fatal bila build pertama bersih

## 2026-10-25 - Cinema E1 (pencarian judul di player, hasil sampai 50)
- [x] D1 di-push (commit 8f2cf73); pin native baru menyusul setelah workflow Native mpv dijalankan
- [x] feat(player): ikon cari di player menerima judul (bukan hanya URL): Play = daftar hasil lalu putar yang diketuk dan sisanya jadi antrean bergulir; Add to queue = tambahkan item yang diketuk (daftar tetap terbuka). Daftar menampilkan judul, kanal, durasi, sampai 50 hasil
- [x] fix(search): NewPipe hanya mengambil 1 halaman (~10-20 video); kini lanjut halaman berikutnya (maks. 6) sampai 50 video, hasil tanpa duplikat; Cinema Home memakai batas yang sama (sebelumnya 15)
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: ketik judul di dialog player, cek 50 hasil, ketuk = putar + antrean berlanjut, Add to queue, URL biasa tetap jalan, Open URL di layar utama/pemilih berkas tidak berubah
- [ ] batas: antrean bergulir hanya untuk URL tonton YouTube; pencarian lewat yt-dlp (ytsearchN) tidak diubah

## 2026-10-25 - Cinema UI menu atas + playlist TV
- [x] feat(player-ui): dialog_top_menu kini berikon (ikon di kiri teks, 2 kolom ringkas, ID tombol tetap); style Widget.Molina.MenuItem dan Widget.Molina.TvKey
- [x] feat(player-ui): Playlist ala daftar channel TV: nomor 001/002/..., baris terpilih kotak biru bergradasi, Shuffle dan Repeat pindah ke footer, action bar Shuffle/Repeat/Info/Menu/Exit, PICK FILE dan OPEN URL berikon tetap ada
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: tampil di API 26, Shuffle/Repeat berubah warna, Info (toast), Menu membuka menu atas, Exit menutup, klik item = putar, antrean pending tampil redup

## 2026-10-25 - Cinema UI v2 (menu ikon-saja + pencarian playlist)
- [x] feat(player-ui): menu atas hanya ikon (ImageButton, tooltip tekan-lama, ID tetap); Audio only aktif berwarna biru; chapter prev/next berikon
- [x] feat(player-ui): playlist: tombol Cari (saring nama; ketik angka = lompat ke nomor channel, Enter = putar), penghitung 003/012 dan jam HH:mm di header; nomor channel tetap saat disaring
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: keyboard + resize dialog, filter, lompat nomor, Enter memutar, ikon menu terbaca, audio-only toggle
- [ ] belum dikerjakan: nama grup (header Semua/kiri-kanan) karena entri playlist belum membawa group-title M3U

## 2026-10-25 - Cinema UI v3 (hasil pencarian Home ala playlist TV)
- [x] feat(home-ui): dialog Hasil pencarian (Compose) diganti SearchResultsDialog di CinemaResults.kt: baris bernomor 001.., baris terpilih kotak biru (tekan = sorot lalu putar), penghitung 003/050 + jam, Cari (teks = saring, angka = lompat, Enter = putar), Info, Tutup; logika putar tetap CinemaActions.playResults
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: keyboard saat Cari, jumlah baris 50, scroll, ketuk baris memutar + antrean berlanjut
- [ ] dialog hasil pencarian di dalam player (MPVActivity, ArrayAdapter 'Search results') belum diseragamkan

## 2026-10-25 - Cinema UI v4 (Prev/Next hasil pencarian + pencarian player disamakan)
- [x] feat(home-ui): SearchResultsDialog Home: tombol Prev/Next (geser sorotan satu halaman = 10 baris)
- [x] feat(player-ui): hasil pencarian di player (MPVActivity.showSearchResults) kini memakai SearchResultsDialog (View) bergaya sama dengan Home: nomor, kotak biru, Prev/Next, Cari/lompat nomor, Info, Exit, jam + penghitung; mode Play (putar + antrean bergulir) dan Add to queue (daftar tetap terbuka) tidak berubah; user mencari dan memutar dari player tanpa kembali ke Home
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat: Prev/Next di Home dan player, Add to queue (daftar tetap terbuka, sorotan pindah), keyboard + resize, lompat nomor
- [ ] dialog_search_item.xml (tata letak lama) kini tidak terpakai; hapus setelah CI hijau

## 2026-10-25 - Cinema UI v5 (dok menu setting + playlist)
- [x] feat(player-ui): menu setting jadi strip ikon vertikal (ScrollView) didok di pojok kanan-bawah (lebar 72dp); playlist didok ke tepi kanan (lebar maks. 380dp, tengah vertikal); helper AlertDialog.dockTo di MPVActivity
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat (landscape dan portrait): posisi tidak tertutup bar navigasi/cutout, strip tidak terpotong, scroll saat banyak item, menu Advanced masih di tengah (belum didok)

## 2026-10-25 - Cinema UI v6 (umur unggahan di hasil pencarian)
- [x] feat(search): StreamSearchItem + uploadedAtMs/uploadedText; NewPipe mengisi dari uploadDate (+ teks mentah), yt-dlp dari timestamp/upload_date; RelativeTime.label membuat teks Indonesia ('3 hari lalu', '2 tahun lalu'; teks 'N days ago' diterjemahkan); tampil di baris meta hasil pencarian Home dan player
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji: DateWrapper.offsetDateTime() pada NewPipeExtractor v0.26.5 (jika gagal kompilasi, ganti ke date()/teks mentah), YouTube hanya memberi umur perkiraan sehingga bulan/tahun kasar; siaran langsung tanpa label
- [ ] ide berikutnya: urutkan Terbaru (sort) pada dialog hasil, penanda 'BARU' untuk < 7 hari

## 2026-10-25 - Code Editor: Blueprint UI v1 [BLUEPRINT-CODE-EDITOR-V1]
- [x] Desain Code Editor (tema Molina Cinema) dicatat di `docs/blueprint-code-editor.md`
- [x] Keputusan: panel direktori gaya Windows 7; tombol Buka Folder/Buka Berkas diperkecil
- [ ] F1 Tokens dan tema -> F6 Uji CI (lihat checklist di blueprint)

## 2026-10-25 - Code Editor: Blueprint v2 (palet toolbar, menu, pencarian) [BLUEPRINT-CODE-EDITOR-V2]
- [x] Palet toolbar khas Molina Cinema (token v2, aturan ikon, opsi A/B)
- [x] Inventaris menu titik tiga + usulan pengelompokan
- [x] Spesifikasi find bar dan dialog Opsi pencarian
- [x] Koreksi: word wrap default aktif
- [ ] Menunggu keputusan toolbar A/B dan jawaban pertanyaan terbuka 7-11

## 2026-10-25 - Code Editor: Blueprint v3 (Simpan sebagai, UTF-8 lock, Tinjauan gelap) [BLUEPRINT-CODE-EDITOR-V3]
- [x] Dialog Simpan sebagai: Enkode terkunci UTF-8 + styling Molina
- [x] Pemilih berkas: ikon folder gold_soft, FAB kapsul cokelat
- [x] Layar Tinjauan: hitam pekat, CSS Markdown, cegah kilatan putih
- [x] Temuan: file tersimpan di root /storage/emulated/0/, usulan default ke folder kerja
- [ ] Menunggu jawaban pertanyaan terbuka 12-16

## 2026-10-25 - Sesi modul Code: kickoff [SESI-CODE-MODULE-KICKOFF-2026-10-25]
- [x] Clone dan baca blueprint-code-editor v1-v3, HANDOFF, BLUEPRINT Fase 6
- [x] Temuan: belum ada kode editor; tab Code masih placeholder; :app Compose, :feature:mpv View/XML
- [ ] Menunggu konfirmasi tahapan dari pemilik dan jawaban pertanyaan terbuka (stack token, Rosemoe, SAF, toolbar A/B)

## 2026-10-25 - Code Editor: keputusan pertanyaan terbuka [CODE-KEPUTUSAN-Q1-16]
- [x] Stack: :feature:code (Compose) + Rosemoe via AndroidView; token Compose Color; akses file path langsung
- [x] Toolbar Opsi A, 10 ikon 36dp (hitbox 48dp); Play menurut ekstensi (eksekusi menunggu CommandBus)
- [x] Hapus menu 'Lakukan' dan toggle terang Tinjauan; 'Kirim ke Library' disembunyikan; Blok = cari dalam seleksi
- [x] UTF-8 ketat (gagal = read-only + banner); simpan tanpa BOM, LF untuk file baru, line ending asli dipertahankan
- [x] Tinjauan: WebView JS mati + commonmark-java (dependensi baru, wajib lolos telemetry_gate)
- [x] Koreksi path: folder kerja/default simpan = /storage/emulated/0/molina-studio (bukan .../Molina)
- [ ] Saran: Code=emas #FFB300, Settings=champagne #CDBFA3 di nav bawah (F5)
- [ ] Berikutnya: F1 tokens + modul :feature:code (menyentuh settings.gradle.kts, app/build.gradle.kts, kemungkinan Manifest)

## 2026-10-25 - Code Editor F1: modul dan token [CODE-F1-TOKENS]
- [x] feat(code): modul :feature:code (Compose, minSdk 26) + CodeColors (token v2/v3) + CodeDimens; dipasang di settings.gradle.kts dan app/build.gradle.kts
- [x] Manifest TIDAK diubah (READ/WRITE_EXTERNAL_STORAGE sudah ada); JNI mpv tidak disentuh; Rosemoe belum ditambahkan (F2)
- [x] Kontras diukur (WCAG) di atas surface_2: champagne 9.19, gold 9.29, icon_dim 3.26, icon_disabled 2.06 (nonaktif), danger 4.26 (ikon, bukan teks)
- [x] telemetry_gate lulus; script patch idempotent (dijalankan 2x tanpa perubahan)
- [ ] BELUM dikompilasi (menunggu CI hijau)
- [ ] catatan merek: tiga emas berbeda (merek #E6B450, editor #FFB300, gold_soft #E9B949); putuskan penyatuan di F5

## 2026-10-25 - Code Editor F2: layout editor [CODE-F2-LAYOUT]
- [x] F1 hijau di CI (#103)
- [x] feat(code): CodeSession/EditorTab (editor per tab), EditorHost (Rosemoe via AndroidView), CodeScreen (header path, tab bar, dialog Buang/Batal untuk tab dirty); tab Code di MainActivity kini membuka layar ini
- [x] PIN SADAR: io.github.Rosemoe.sora-editor:editor:0.24.6 (Maven Central, Jun 2026); 0.24.7 dilewati karena baru terbit. Lisensi LGPL-2.1-or-later, dicatat di THIRD_PARTY_NOTICES.md. Belum ada dependency verification untuk Maven Central
- [x] Dampak: Gradle (feature/code: foundation + sora-editor), MainActivity.kt. Manifest dan JNI mpv tidak disentuh. telemetry_gate lulus
- [x] Tambahan di luar blueprint: tombol + tab baru; nilai HeaderHeight/TabBarHeight 40dp, TabMin/Max 96/180dp
- [ ] BELUM dikompilasi (menunggu CI). Risiko: nama API Rosemoe (release, setWordwrap, subscribeEvent, konstanta EditorColorScheme); AAR dibangun compileSdk 36/AGP 9.1 vs proyek 35/8.7.3; kotlin-stdlib 2.3.10 runtime vs compiler 2.0.21
- [ ] uji perangkat: tab Code terbuka; ketik = titik emas; + tab baru, pindah tab menjaga isi dan undo; X pada tab dirty = dialog; tutup tab terakhir = tab kosong baru; pindah ke tab mpv/Terminal lalu kembali = tab tetap; keyboard menyembunyikan nav bawah; word wrap aktif; gutter dan kursor emas
- [ ] batas diketahui: rotasi layar menghapus semua tab (Activity dibuat ulang, autosave draf menyusul di F5); titik tiga di header baru visual (F3b); tab bar belum auto-scroll ke tab aktif; dirty tetap true setelah undo ke isi awal; ripple masih bawaan (gold ripple di F3)
- [ ] berikutnya: F3 toolbar (Simpan/Tempel/Undo/Redo/Cari/kursor/Tab/Play/Tutup, rail emas, Opsi A)

## 2026-10-25 - Keputusan: warna nav bawah [CODE-NAV-WARNA-DITUNDA]
- [x] Pemilik menyetujui saran: tab Code emas #FFB300, Settings champagne #CDBFA3 di nav bawah; dikerjakan nanti (setelah F2 hijau, sebelum atau bersama F3)
- [ ] terkait: putuskan penyatuan tiga emas (merek #E6B450, editor #FFB300, gold_soft #E9B949) setelah melihat tampilannya di perangkat

## 2026-10-25 - Code Editor F2: fix koordinat Rosemoe [CODE-F2-FIX-KOORDINAT]
- [x] CI merah di :app:checkDebugAarMetadata: io.github.Rosemoe.sora-editor:editor:0.24.6 tidak ditemukan
- [x] Akar masalah: grup salah. Source resmi tag 0.24.6 memakai group io.github.rosemoe, artifact editor. Diperbaiki ke io.github.rosemoe:editor:0.24.6 (feature/code/build.gradle.kts, THIRD_PARTY_NOTICES.md)
- [x] Nama API F2 dicocokkan dengan source 0.24.6: release, setWordwrap, subscribeEvent, ContentChangeEvent, 9 konstanta EditorColorScheme semuanya ada
- [x] Dampak: hanya Gradle dependency dan notices. Manifest dan JNI mpv tidak disentuh
- [ ] menunggu CI. Risiko tersisa: AAR dibangun compileSdk 36/AGP 9.1 vs proyek 35/8.7.3; kotlin-stdlib 2.3.10 runtime vs compiler 2.0.21; keberadaan artefak di Maven Central belum terverifikasi langsung

## 2026-10-25 - Code Editor F2: fix compileSdk Rosemoe [CODE-F2-FIX-COMPILESDK]
- [x] Koordinat 0.24.6 sudah benar (io.github.rosemoe:editor), dependency ter-resolve; error baru checkDebugAarMetadata: mensyaratkan compileSdk 36, proyek 35 / AGP 8.7.3
- [x] Analisis: semua Rosemoe >= 0.23.7 dibangun compileSdk 36; 0.24.x juga Kotlin 2.2+ (proyek 2.0.21). Menaikkan AGP/Kotlin/compileSdk merembet ke semua modul dan Gradle wrapper
- [x] PIN SADAR (menggantikan 0.24.6): io.github.Rosemoe.sora-editor:editor:0.23.6 (grup lama, compileSdk 35, Kotlin 2.1.21 terbaca compiler 2.0.21). API F2 dicocokkan ke source 0.23.6: semua ada
- [x] Dampak: hanya feature/code/build.gradle.kts dan THIRD_PARTY_NOTICES.md. Manifest dan JNI mpv tidak disentuh
- [ ] menunggu CI. Risiko: AAR 0.23.6 dibangun AGP 8.8.0 vs proyek 8.7.3; ketersediaan 0.23.6 di Maven Central belum terverifikasi langsung
- [ ] jalur naik versi (AGP + Kotlin 2.2+ + compileSdk 36 untuk semua modul) ditunda sebagai tugas tersendiri
- [x] Pemilik menyetujui warna nav bawah (Code #FFB300, Settings #CDBFA3) dikerjakan bersamaan F3 setelah CI hijau

## 2026-10-25 - Code Editor F3: toolbar + warna nav [CODE-F3-TOOLBAR]
- [x] F2 hijau di CI (#106) setelah Rosemoe 0.23.6
- [x] feat(code): EditorToolbar (Opsi A: flat Surface2 + rail emas, 10 ikon Canvas rata lebar, pembatas grup, badge jumlah tab), ToolbarIcons.kt, token ToolbarIconSize; efek tekan gold 16% menggantikan ripple bawaan
- [x] Aksi hidup: Tempel (aktif bila clipboard ada), Undo/Redo (aktif sesuai stack), kursor kiri/kanan, Tab, Tutup (dirty = dialog Buang/Batal)
- [x] Warna nav bawah: Code emas #FFB300, Settings champagne #CDBFA3 (sesuai keputusan pemilik)
- [x] Dampak: Gradle tidak berubah; MainActivity.kt hanya dua nilai warna; Manifest dan JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (menunggu CI). Risiko: gambar ikon Canvas (when sebagai ekspresi terakhir lambda), moveSelection/pasteText Rosemoe (sudah dicocokkan ke source 0.23.6)
- [ ] keputusan sementara, perlu konfirmasi: tombol Tab = pindah ke tab berikutnya melingkar (blueprint belum mendefinisikan); Simpan/Cari/Play tampil tapi belum bereaksi (F3e/F3c/fase Play); Cari dan Play berwarna icon_disabled
- [ ] uji perangkat: toolbar di atas nav, rail emas tipis; Tempel nonaktif saat clipboard kosong; Undo/Redo menyala setelah mengetik; kursor kiri/kanan; badge angka berubah saat + tab; Tab berpindah melingkar; Tutup tab dirty memunculkan dialog; keyboard terbuka: nav tersembunyi, toolbar menempel di atas keyboard; nav Code emas dan Settings champagne
- [ ] terkait: putuskan penyatuan tiga emas (#E6B450, #FFB300, #E9B949) setelah melihat tampilan
- [ ] berikutnya: F3b menu titik tiga, F3c find bar, F3d kursor/seleksi

## 2026-10-25 - Nav bawah: palet diselaraskan [NAV-PALET-SELARAS]
- [x] F3 toolbar hijau di CI (#107)
- [x] style(nav): garis atas nav diganti rail emas 1dp (transparan, #FFB300 40%, transparan) selaras rail toolbar Code; tab mpv #B388FF menjadi #B9A7E8, Terminal #69F0AE menjadi #8FCBA6 (hue dipertahankan agar cocok dengan aksen Cinema/Terminal di dalam layar)
- [x] Dampak: hanya warna dan tinggi garis di MainActivity.kt. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (menunggu CI). Risiko rendah (nilai warna dan satu lambda listOf)
- [ ] temuan: latar nav sebenarnya #0A0A0F (MolinaTheme.Background), bukan #2A2218 seperti asumsi blueprint, sehingga toolbar cokelat hangat Code duduk di atas nav hitam dingin; menunggu keputusan pemilik
- [ ] terkait: putuskan penyatuan tiga emas (#E6B450, #FFB300, #E9B949) setelah melihat tampilan

## 2026-10-25 - Keputusan Opsi B: permukaan Code ikut nav hitam [CODE-OPSI-B]
- [x] Pemilik memilih Opsi B agar Terminal dan nav tidak berubah
- [x] style(code): 10 token CodeColors diganti ke keluarga hitam dingin (Surface1 #14161B = CardBg app, Hairline #2A2D35 = CardLine app, SurfaceNav #0A0A0F nilai aktual, dst); emas, champagne, danger, kapsul, baris aktif tetap
- [x] docs: blueprint-code-editor.md bagian 25 mencatat keputusan dan tabel nilai lama/baru
- [x] Dampak: hanya CodeColors.kt (nilai warna). Gradle, Manifest, JNI mpv, MainActivity, Terminal tidak disentuh
- [ ] BELUM dikompilasi (menunggu CI); risiko sangat rendah (nilai literal)
- [ ] uji perangkat: toolbar/tab bar/header menyatu dengan nav hitam; ikon champagne terbaca; ikon dim/disabled masih dapat dibedakan; garis hairline tidak terlalu terang
- [ ] terkait: penyatuan tiga emas (#E6B450, #FFB300, #E9B949) masih menunggu keputusan setelah melihat tampilan

## 2026-10-25 - Toolbar No. 8: pilih semua + salin [CODE-TOMBOL-SALIN-SEMUA]
- [x] Koreksi pemilik: tombol Tab di toolbar = pilih semua lalu salin otomatis (bukan manajemen tab)
- [x] feat(code): ikon SelectAll (kotak putus-putus + kotak penuh), aksi selectAll()+copyText()+Toast, nonaktif saat dokumen kosong; badge jumlah tab dan CodeSession.selectNext() dihapus
- [x] docs: blueprint-code-editor.md bagian 26 mencatat koreksi
- [x] Dampak: hanya :feature:code (ToolbarIcons, EditorToolbar, CodeScreen, CodeSession). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (menunggu CI). Risiko: PathEffect.dashPathEffect pada Stroke di dalam scale(); selectAll/copyText Rosemoe (sudah dicocokkan ke source 0.23.6)
- [ ] uji perangkat: tombol redup saat dokumen kosong; ketik lalu tekan = Toast + seluruh teks terseleksi; Tempel di tab lain menempel isi penuh; ikon putus-putus terlihat jelas pada 22dp

## 2026-10-25 - F3b menu titik tiga + judul tab + auto-scroll [CODE-F3B-MENU-TAB]
- [x] fix(code): judul tab "Tidak berjudul" jadi "Baru"; angka tidak lagi naik saat tab terakhir ditutup (nomor terkecil yang bebas, bukan penghitung kumulatif)
- [x] feat(code): tab bar auto-scroll ke tab aktif (ScrollState + posisi tab via onGloballyPositioned)
- [x] feat(code): OverflowMenu.kt, menu titik tiga 5 grup; aktif: Baru, Bagikan, Pembungkus kata, Hanya baca, Statistik; sisanya redup menunggu fase
- [x] docs: blueprint-code-editor.md bagian 27
- [x] Dampak: hanya :feature:code (CodeSession, EditorHost, CodeScreen, OverflowMenu baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (menunggu CI). Risiko: CodeEditor.setEditable(Boolean) dan Content.lineCount pada Rosemoe 0.23.6; import onSizeChanged/positionInParent; Popup di dalam Box anchor
- [ ] uji perangkat: tutup tab terakhir = kembali "Baru" (bukan Baru 2); buka 8+ tab lalu tab aktif tergulir ke layar; titik tiga membuka menu di bawah header; Pembungkus kata dan Hanya baca bekerja per tab; Bagikan membuka chooser; item redup tidak bisa diketuk

## 2026-10-25 - F3c find bar + Pergi ke baris [CODE-F3C-FIND]
- [x] feat(code): FindBar.kt (find bar, dialog Opsi pencarian, dialog Pergi ke baris) dan FindState.kt; pencarian memakai EditorSearcher Rosemoe 0.23.6 (API dicocokkan ke sumber)
- [x] feat(code): tombol Cari toolbar + item menu Cari/Pergi ke baris aktif; seleksi pendek jadi kata cari awal; sorotan hasil emas 35%
- [x] refactor(code): EditorTab.editor jadi state Compose; ikon titik tiga vektor (ToolbarIcon.More)
- [x] docs: blueprint-code-editor.md bagian 28
- [x] Dampak: hanya :feature:code (FindBar, FindState, CodeSession, EditorHost, EditorToolbar, OverflowMenu, ToolbarIcons, CodeScreen, CodeDimens). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (menunggu CI). Risiko: smart cast pada fungsi lokal di composable, KeyboardActions onSearch/onGo/onDone, delegate pada EditorTab.editor
- [ ] uji perangkat: ketik kata cari lalu hitung n/total muncul dan sorotan emas; Enter lompat ke hasil berikutnya; regex salah = merah 'Regex salah'; GANTI/SEMUA mengubah teks; Pergi ke baris 1 dan baris terakhir; find bar tidak tertutup keyboard; pindah tab menghentikan sorotan tab lama
- [ ] keputusan pemilik: arti 'Blok' (kata utuh atau dalam seleksi); sementara kata utuh
- [ ] berikutnya: F3e Simpan + autosave draf, lalu F4 panel direktori

## 2026-10-25 - Klarifikasi Pintasan skrip (dokumen saja) [DOCS-PINTASAN-SKRIP]
- [x] docs: blueprint-code-editor.md dikoreksi: "Kirim ke Library" -> "Script execution"; Library = Pintasan skrip di Settings (tahap setelah Code); pertanyaan terbuka 7 terjawab
- [x] docs: inventaris dialog "Ubah entri" dari screenshot desain lama dicatat di blueprint
- [x] Dampak: hanya dokumen. Kode, Gradle, Manifest, JNI mpv tidak disentuh; item menu tetap redup
- [ ] menunggu konfirmasi pemilik: P-SKRIP-1 sampai P-SKRIP-3 di blueprint (arti Script execution, alur Edit di Editor, beda dengan Play dan Lakukan)

## 2026-10-25 - F3e Simpan, Simpan sebagai, autosave draf [CODE-F3E-SIMPAN]
- [x] feat(code): FileStore.kt (baca UTF-8 ketat, tulis atomik, validasi nama, DraftStore) dan SaveAsDialog.kt (nama + ekstensi bersaran, Enkode terkunci UTF-8 berikon gembok, konfirmasi TIMPA)
- [x] feat(code): tombol Simpan toolbar aktif saat dirty; menu "Simpan sebagai"; dialog tutup tab dirty punya SIMPAN (simpan lalu tutup)
- [x] feat(code): autosave draf di filesDir/code-drafts (debounce 1,5 dtk + flush sinkron di onStop); tab dan isi dipulihkan saat proses dimatikan atau layar diputar
- [x] refactor(code): CodeSession(workDir, draftDir) memakai Handler + satu thread IO (tanpa dependency coroutines baru); EditorTab.file/loading/edits; setText tidak lagi menandai dirty
- [x] keputusan pemilik: akses file path langsung /storage/emulated/0 (bukan SAF)
- [x] docs: blueprint-code-editor.md bagian 30; pertanyaan terbuka 3 terjawab
- [x] Dampak: :feature:code (CodeSession, EditorHost, EditorToolbar, OverflowMenu, CodeScreen, ToolbarIcons, FileStore baru, SaveAsDialog baru) dan :app (MainActivity: ref sesi, folder draf, flush di onStop). Gradle, Manifest (WRITE_EXTERNAL_STORAGE sudah ada), JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (menunggu CI). Risiko: ContentChangeEvent.action/ACTION_SET_NEW_TEXT (sudah dicocokkan ke sumber 0.23.6), DialogProperties(usePlatformDefaultWidth), moveFocus Next, io.submit(Runnable)
- [ ] uji perangkat: ketik di "Baru" lalu Simpan -> dialog, simpan sebagai x.txt -> judul tab jadi x.txt dan titik hilang; edit lagi + Simpan menimpa tanpa dialog; nama sudah ada -> TIMPA; nama kosong/"a/b" ditolak; matikan paksa app lalu buka lagi -> tab dan isi dirty kembali; putar layar -> tab tetap; tutup tab dirty -> SIMPAN menyimpan lalu menutup; izin storage ditolak -> toast gagal menyimpan
- [ ] keputusan pemilik: BOM dan line ending (blueprint pertanyaan 13); sementara UTF-8 tanpa BOM, line ending apa adanya
- [ ] berikutnya: F4 panel direktori (Buka, Muat ulang, pemilih folder untuk Simpan sebagai)

## 2026-10-25 - F4 Panel Direktori [CODE-F4-DIREKTORI]
- [x] feat(code): DirectoryPanel/State/Dialogs/Icons baru; CodeScreen, FileStore, EditorHost, OverflowMenu, SaveAsDialog, CodeSession, CodeColors, CodeDimens diperbarui (commit dc7667d)
- [x] Patch awal terpotong saat transfer (SyntaxError baris 953); diperbaiki dengan molina_f4_apply.py (verifikasi sha256 per payload)
- [ ] CI dc7667d: ISI SETELAH CEK TAB ACTIONS (hijau/merah)
- [ ] uji perangkat: buka folder, muat ulang, tab duplikat, dirty baseline, BOM/line ending
- [x] Catatan: commit docs memakai [skip ci] dan build.yml punya paths-ignore **.md, jadi CI hanya jalan untuk commit kode

## Housekeeping
- [x] .gitignore: __pycache__/ dan molina_*.py
- [ ] Hapus file lama yang masih ter-track di root: molina_patch_*.sh, molina_terminal_v2.sh, molina_terminal_v3.sh, molina_ui_v5_dock.py

## 2026-10-25 - Temuan CI [CI-SKIP-PUSH]
- [x] F3e (c253d45) dan F4 (dc7667d) tidak memicu CI: dua commit (feat lalu docs [skip ci]) dikirim dalam satu push, sehingga seluruh push dilewati. Run terakhir tetap #112 (F3c).
- [x] Aturan: push commit kode terpisah dari commit docs [skip ci].
- [ ] jalankan manual workflow Build Molina Studio (workflow_dispatch) di main; catat hasil untuk F3e dan F4.

## 2026-10-25 - Indikator hijau/merah [CODE-F4C-INDIKATOR]
- [x] feat(code): titik tab dan status bar selalu tampil: hijau = sama dengan isi awal, merah = ada perubahan; undo ke isi awal kembali hijau.
- [x] perbaikan sebelumnya: changeRoot (6519b08) mengatasi bentrok JVM setRoot.
- [ ] CI commit indikator: ISI SETELAH CEK TAB ACTIONS
- [ ] uji perangkat: buka berkas (hijau), ubah 1 karakter (merah), undo (hijau), redo (merah)

## 2026-10-25 - Menu baris direktori jadi dropdown [CODE-MENU-DROPDOWN]
- [x] refactor(code): menu aksi berkas tidak lagi Dialog penuh; Popup menempel di titik tiga baris tree (TreeActionMenu) dengan posisi rata kanan, flip ke atas, dijepit layar
- [x] "Aksi file" dari header memakai dropdown kanan atas; baris terpilih disorot saat menu terbuka
- [x] feat(code): item "Salin nama"; Hapus dipisah hairline; baris 40dp
- [x] Dampak: hanya :feature:code (DirectoryDialogs, DirectoryPanel, DirectoryState). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: import Popup/PopupPositionProvider, ActionItem.separatorBefore
- [ ] uji perangkat: menu dekat ⋮ baris paling bawah (harus flip ke atas), baris panjang, Aksi file dari header, Back menutup menu

## 2026-10-25 - Nomor baris warna Claude, sorot baris, baris:kolom [CODE-LINENUM-CLAUDE]
- [x] feat(code): LineNumber terakota redup, LINE_NUMBER_CURRENT terang, panel nomor baris (LINE_NUMBER_PANEL) senada
- [x] feat(code): jumpToLineHighlighted(line, column) dan flashCurrentLine: nomor dan latar baris menyala 1,4 dtk (Pergi ke baris) atau 0,7 dtk (Cari next/prev)
- [x] feat(code): Pergi ke baris menerima baris:kolom (pemisah : , . spasi); kolom dijepit; hint menampilkan baris kini
- [x] Dampak: hanya :feature:code (CodeColors, EditorHost, FindBar). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko (belum dicocokkan ke source Rosemoe 0.23.6): LINE_NUMBER_CURRENT, LINE_NUMBER_PANEL, LINE_NUMBER_PANEL_TEXT, cursor.leftLine, text.getColumnCount, setSelection(line, column)
- [ ] uji perangkat: Pergi ke 1, tengah, terakhir; 57:12; 57:999 (dijepit); Cari next/prev berulang cepat; scroll cepat (panel nomor); ganti tab saat menyala

## 2026-10-25 - Cari: default blok+sensitif, blok warna Claude [CODE-FIND-CLAUDE]
- [x] feat(code): FindState.wholeWord default ON (matchCase sudah ON); label "Sensitif huruf besar/kecil"
- [x] feat(code): EditorSelection/SearchMatch terakota Claude (50%/28%)
- [x] feat(code): stopSearchKeepSelection: kata hasil terakhir tetap terblok saat find bar ditutup
- [x] Dampak: hanya :feature:code (FindState, FindBar, CodeColors). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko (belum dicocokkan ke source Rosemoe 0.23.6): Cursor.isSelected/leftLine/leftColumn/rightLine/rightColumn, CodeEditor.setSelectionRegion(4 Int)
- [ ] uji perangkat: teks "Kt.KT,kt": cari Kt/KT/kt masing-masing 1/1; Sensitif OFF cari kt = 3/3; tutup find bar, kata tetap terblok terakota

## 2026-10-25 - Highlight sintaks bawaan Obsidian Claude [CODE-SYNTAX-OBSIDIAN]
- [x] feat(code): SyntaxLexer.kt (lexer murni Kotlin: kode C-like/Python/Shell/Config, JSON, XML/HTML, Markdown) dan SyntaxHighlight.kt (ObsidianClaude + MolinaLanguage di atas SimpleAnalyzeManager)
- [x] EditorHost: ObsidianClaude.apply di applyMolinaColors; syncSyntax memasang bahasa dari nama berkas dan menyegarkan saat Simpan sebagai
- [x] Diuji di JVM (kotlinc 2.1.21): contoh per bahasa dan 2.700 teks acak (posisi naik ketat, jumlah baris benar); adapter dicek tipe terhadap source Rosemoe 0.23.6 tanpa error
- [x] Simbol Rosemoe yang sebelumnya belum dicocokkan kini terverifikasi di 0.23.6: LINE_NUMBER_CURRENT, LINE_NUMBER_PANEL(_TEXT), Cursor.leftLine/leftColumn/rightLine/rightColumn/isSelected, setSelectionRegion(4 Int), Content.getColumnCount
- [x] Dampak: hanya :feature:code (file baru SyntaxLexer, SyntaxHighlight; EditorHost). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko tersisa: sisi Compose/Android (belum bisa dicek di luar CI)
- [ ] uji perangkat: buka .kt, .py, .json, .xml, .md (warna sesuai tema); ketik cepat di berkas besar; Simpan sebagai x.kt dari tab Baru (warna muncul); komentar/string multi-baris; berkas > 400 ribu karakter tetap lancar
- [ ] Code→Action→Live Preview: desain PreviewProvider native (Markdown/JSON/output skrip), WebView opsional untuk HTML
- [2026-10-25] design(mpv): MolinaLinkSheet v2 - border hairline 0,75dp (sheet + field), BasicTextField kustom, tombol Tempel/Hapus, chip riwayat, label Putar/Cari - status: file dibuat, menunggu integrasi pemanggil

## 2026-10-25 - Pemilih file lokal ala Windows 7 [CINEMA-WIN7-OPEN]
- [x] feat(cinema): Win7Aero.kt (token Aero, ikon Canvas, tombol) dan Win7OpenDialog.kt (bilah alamat breadcrumb, panel Favorit/Komputer, kolom Nama/Diubah/Ukuran, Nama file + filter, izin READ_EXTERNAL_STORAGE)
- [x] CinemaHome: tombol File membuka Win7OpenDialog (menggantikan OpenDocument bawaan Android); CinemaActions.playPath memakai extra filepath
- [x] Dampak: hanya modul :app (Compose). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: API Compose/Material3 pada BOM 2024.12.01
- [ ] uji perangkat Android 8.0: izin storage, buka folder, pilih file lalu ketuk lagi/Buka, urut kolom, kartu SD, Back naik folder

## 2026-10-25 - Pemilih file lama mpv diseragamkan ke Windows 7 [MPV-WIN7-THEME]
- [x] style(mpv): primary ungu #722b72 menjadi biru Aero (#B9D1EA, aksen #3C7FB1); FilePickerTheme terang (Theme.AppCompat.Light); ikon folder kuning (win7_folder)
- [x] Dampak: hanya resource :feature:mpv (colors.xml, styles.xml, 2 layout nnf). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Uji: layar pemilih file lama (ActionBar biru muda, teks gelap, folder kuning)

## 2026-10-25 - Settings ala Panel Kontrol Windows 7 + submenu Sistem [SETTINGS-WIN7-SYSTEM]
- [x] feat(settings): beranda = ubin Sistem + ringkasan perangkat (versi, Android/API, perangkat, ABI, RAM); halaman Sistem memuat Terminal, Log, Mesin cadangan (yt-dlp), Keamanan dan privasi, About
- [x] feat(settings): jendela Win7 (tombol kembali bulat, breadcrumb, kotak Cari setelan dengan indeks 14 entri, tombol Segarkan Sistem di bilah perintah); halaman lama otomatis terang lewat Win7Scheme
- [x] Log dan Tampilan Molina tetap tema gelap (log dan pratinjau warna prompt terminal dirancang untuk latar gelap)
- [x] Dampak: hanya modul :app (settings). Gradle, Manifest, JNI mpv tidak disentuh. Bergantung pada Win7/Win7BackOrb (Win7Aero.kt)
- [ ] BELUM dikompilasi (CI). Risiko: parameter lightColorScheme surfaceContainer* pada Material3 BOM 2024.12.01
- [ ] uji perangkat: Home > Sistem > Terminal > Debugging, tombol kembali/Back, breadcrumb, cari 'log' dan 'font', dialog Segarkan Sistem (terang), Switch/Radio/Slider terbaca

## 2026-10-25 - Perbaikan tema putih -> hitam pekat mpv + Settings [FIX-DARK-THEME]
- [x] fix(mpv): FilePickerTheme & AppTheme dari AppCompat.Light ke AppCompat gelap; windowBackground #000000; primary/primary_dark #000000, accent #4DA3FF; dialog alert gelap
- [x] fix(settings): Win7Scheme jadi darkColorScheme; palet baru W7D (Aero Gelap) menggantikan Color.White dan token Win7 terang di Win7SettingsUi/SettingsScreen
- [x] Dampak: resource :feature:mpv (styles, colors) dan :app (settings). Gradle, Manifest, JNI mpv tidak disentuh. Win7OpenDialog (Cinema) tidak diubah
- [ ] BELUM dikompilasi (CI). Uji: Settings (beranda, Sistem, Log, dialog Segarkan), layar Open external subtitle, pemilih file lama, dialog Buat Folder
- [ ] Log menunjukkan playlist-count=0 untuk .../playlist.m3u (kemungkinan gagal dibuka): investigasi terpisah

## 2026-10-25 - Desain ulang layar Log profesional [LOG-PRO-V2]
- [x] feat(log): chip sumber (Semua/Cinema/Terminal/Sistem/Logcat/Crash) + jumlah per sumber; dropdown level minimum; menu aksi (Salin, Bagikan, Kirim pesan uji, Hapus dengan konfirmasi)
- [x] feat(log): baris berstruktur (jam, badge level, sumber/tag berwarna, pesan indentasi menggantung); log Cinema beruntun (>=4) dikelompokkan dan bisa dilipat dengan ringkasan galat/peringatan; tombol "Ke terbaru"; keadaan kosong + Atur ulang filter
- [x] Dampak: hanya app/.../log/LogScreen.kt (LogBus/LogIpc tidak diubah). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: DropdownMenuItem/AlertDialog Material3 BOM 2024.12.01, LevelFilter.entries (Kotlin 1.9+)
- [ ] uji perangkat: ganti chip, level Galat saja, lipat/buka grup Cinema, tombol Ke terbaru saat scroll naik, Hapus + Batal, Salin/Bagikan hanya yang terfilter

## 2026-10-25 - Cari di log, ekspor .txt, dialog Cinema hitam pekat [LOG-SEARCH-EXPORT]
- [x] feat(log): tombol Cari membuka kolom cari (pesan/tag/sumber, tanpa beda huruf besar-kecil) dengan sorotan emas; grup Cinema dimatikan saat mencari
- [x] feat(log): menu Simpan ke berkas (.txt) menulis <dataRoot>/export/molina-log-yyyyMMdd-HHmmss.txt (header: versi app, perangkat, filter, jumlah)
- [x] style(cinema): token Win7 (Win7Aero.kt) dan Win7OpenDialog.kt diseragamkan ke palet hitam pekat + aksen biru; tambah token Input/BarTop/BarBottom
- [x] Dampak: LogScreen.kt, Win7Aero.kt, Win7OpenDialog.kt. Gradle, Manifest, JNI mpv tidak disentuh. Berbagi berkas sebagai lampiran (FileProvider) belum dibuat karena perlu perubahan Manifest
- [ ] BELUM dikompilasi (CI). Risiko: BasicTextField.decorationBox, FocusRequester.requestFocus (dibungkus runCatching)
- [ ] uji perangkat: Cari "playlist" (sorotan emas), Cari + filter level, Simpan ke berkas lalu cek folder export, dialog Buka Cinema (latar, kolom, baris terpilih, daftar jenis berkas)

## 2026-10-25 - Investigasi artefak tampilan MainActivity [UI-GLITCH-INVESTIGASI]
- [x] Commit 902fcc7 (Win7 dialog + tema picker) dan bbcb03a (tema gelap) ditinjau: hanya resource tema dan Compose :app, tidak menyentuh MPVView/MPVActivity/JNI
- [ ] Penyebab teks bertumpuk belum terbukti; langkah reproduksi menunggu dari pengguna
- [ ] Uji bisect via CI dari 902fcc7^ dan/atau hardwareAccelerated=false pada MainActivity

## 2026-10-25 - Workaround glitch gambar FilePickerActivity [MPV-PICKER-SW]
- [x] fix(mpv): FilePickerActivity hardwareAccelerated=false (pola korupsi 128px + sisa gambar app lain di Android 8); kode layar = upstream, bukan dari tema
- [x] Dampak: AndroidManifest :feature:mpv saja. Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Uji: player > tombol subtitle > Open external subtitle, portrait dan landscape
- [ ] Fallback bila masih glitch: windowTranslucentNavigation di FilePickerThemeSpecial

## 2026-10-25 - Dialog Tautan atau judul berbingkai [CINEMA-LINK-FRAME]
- [x] style(cinema): AlertDialog bawaan diganti MolinaFrameDialog (margin tepi 40dp, lebar maks 380dp, sudut 20dp, bingkai tipis bergradasi emas, garis aksen judul, tombol sejajar kanan); kolom isian bergaya Molina
- [x] Dampak: hanya :app (CinemaHome.kt + MolinaFrameDialog.kt baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: OutlinedTextFieldDefaults.colors (Material3 BOM 2024.12.01), Modifier.border(Dp, Brush, Shape)
- [ ] uji perangkat: Cinema Home > Tautan, portrait dan landscape, keyboard muncul (dialog tidak tertutup), tombol Batal/Buka

## 2026-10-25 - Dialog Hasil pencarian berbingkai [CINEMA-RESULTS-FRAME]
- [x] style(cinema): SearchResultsDialog diseragamkan dengan MolinaFrameDialog (margin tepi 24dp, lebar maks 480dp, sudut 20dp, bingkai tipis bergradasi emas); kolom Cari bergaya Molina. Daftar channel dan palet biru tidak diubah
- [x] Dampak: hanya app/.../cinema/CinemaResults.kt. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: OutlinedTextFieldDefaults.colors (Material3 BOM 2024.12.01)
- [ ] uji perangkat: Cinema Home > cari judul > daftar hasil, portrait dan landscape, tombol Cari/Prev/Next

## 2026-10-25 - Code -> Action -> Live Preview native (tanpa WebView) [CODE-PLAY-PREVIEW]
- [x] feat(code): tombol Play aktif. Markdown/JSON: panel pratinjau langsung di bawah editor (debounce 300 ms, render native Compose: LazyColumn, tabel, tautan, kutipan, daftar tugas). JSON: cetak rapi berwarna + galat baris:kolom, ketuk untuk lompat ke posisi galat
- [x] feat(code): .sh/.py dijalankan dari isi editor (tanpa harus Simpan) di Debian PRoot lewat antarmuka ScriptExecutor; keluaran mengalir ke panel Keluaran (stderr merah, ringkasan exit/waktu, Berhenti/Bersihkan)
- [x] feat(app): ProotScriptExecutor (berkas sementara di rootfs /tmp/molina-run, bind storage sama dengan Terminal, timeout 60 dtk, batas 1 jt karakter, pohon proses dimatikan saat Berhenti/lewat waktu). TerminalSessions.WORKSPACE_DIR jadi internal
- [x] Modul murni Kotlin baru (com.molina.code.preview): PreviewKinds, RunPlans, GuestPaths, OutputBuffer, Markdown parser, JsonPreview
- [x] Diuji di JVM (kotlinc 2.1.21): 4.630 uji lulus (scripts/code-tests/run.sh). Gerbang telemetri lulus
- [x] Dampak: :feature:code (+kotlinx-coroutines-core 1.9.0), :app (ProotScriptExecutor, MainActivity, TerminalSessions). Manifest dan JNI mpv tidak disentuh
- [ ] CI commit Play/preview: ISI SETELAH CEK TAB ACTIONS. Risiko: ClickableText, LazyColumn items(count), IntrinsicSize, withFrameNanos
- [ ] uji perangkat: Play di .md/.json (valid dan rusak, lompat ke galat), .sh, .py (print + sleep, keluaran live), Berhenti, timeout, Debian belum terpasang, python3 belum ada, pindah tab saat berjalan
- [ ] lanjut: Refreshers id "code", pratinjau HTML native, sinkron scroll editor-pratinjau, item menu "Script execution"

## 2026-10-25 - Tinjauan halaman penuh [CODE-PREVIEW-FULLPAGE]
- [x] feat(code): Play pada .md/.json membuka halaman Tinjauan penuh (appbar hitam: kembali, judul "Tinjauan", subjudul path berkas; isi native; tanpa toggle terang). Back sistem dan tombol panah menutupnya; bottom nav disembunyikan saat terbuka
- [x] Panel bawah kini khusus Keluaran skrip (.sh/.py). Galat JSON: "Lompat ke baris" menutup Tinjauan dan menyorot posisi galat di editor
- [x] Dampak: :feature:code (+androidx.activity:activity-compose:1.9.3, versi sama dengan :app), :app (MainActivity: sembunyikan bottom nav). Manifest dan JNI mpv tidak disentuh
- [ ] CI commit Tinjauan penuh: ISI SETELAH CEK TAB ACTIONS. Risiko: BackHandler, clickable(indication = null)
- [ ] uji perangkat: Play di .md -> halaman penuh; Back; ubah isi lalu Play lagi; JSON rusak -> lompat ke galat; bottom nav kembali muncul

## 2026-10-25 - Tinjauan: scroll tersinkron, Bagikan, Penyegaran skrip [CODE-PREVIEW-SYNC]
- [x] feat(code): Tinjauan Markdown menggulir ke blok tempat kursor editor berada (sekali saat dibuka); MdDoc/parseWithLines memetakan blok ke baris sumber
- [x] feat(code): tombol BAGIKAN di appbar Tinjauan (maks 200 rb karakter)
- [x] feat(app): CodeRefreshable (id "code") mendaftar ke Penyegaran sistem; ProotScriptExecutor.cancelActive/isRunning
- [x] Diuji di JVM: 6.634 uji lulus. Gerbang telemetri lulus. Manifest dan JNI mpv tidak disentuh
- [ ] CI commit scroll/Bagikan/Penyegaran: ISI SETELAH CEK TAB ACTIONS
- [ ] uji perangkat: kursor di tengah dokumen .md lalu Play (terbuka di bagian itu); BAGIKAN; skrip `sleep 100` lalu Penyegaran sistem (berhenti, ringkasan "Dihentikan")

## 2026-10-26 - Settings: Terminal, Log, Keamanan, info perangkat dipindah ke submenu Sistem [SETTINGS-SYSTEM-SUBMENU]
- [x] refactor(settings): Home Settings hanya memuat ubin "Sistem" (links dikosongkan, W7SystemSummary dicabut dari Home)
- [x] Halaman Sistem memuat Terminal, Log, Keamanan dan privasi, Mesin cadangan (yt-dlp), About, lalu card informasi perangkat
- [x] Dampak: hanya app/.../settings/SettingsScreen.kt. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah
- [ ] uji perangkat: Home hanya ubin Sistem; klik Sistem tampil 4 elemen + card RAM; Back ke Home; pencarian "Log" dan "Keamanan" jalur "Settings > Sistem"; ikon refresh tetap jalan
- [ ] keputusan: yt-dlp dan About tetap di Sistem, atau dipindah?

## 2026-10-26 - Settings: keyboard tidak lagi terpicu otomatis [SETTINGS-NO-AUTOFOCUS]
- [x] fix(settings): kolom "Cari setelan" hanya bisa fokus setelah disentuh (focusProperties + pointerInput); fokus otomatis saat Settings dibuka dicegah
- [x] fix(settings): fokus dilepas setiap halaman Settings berganti (LaunchedEffect(page) clearFocus), keyboard menutup saat klik menu
- [x] Dampak: app/.../settings/Win7SettingsUi.kt dan SettingsScreen.kt. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah (focusProperties, onFocusChanged, pointerInput)
- [ ] uji perangkat: buka Settings tanpa keyboard; ketuk kolom cari lalu ketik; klik menu menutup keyboard; Back dan pindah tab

## 2026-10-26 - Settings: menu Script execution, Paket, dan pintasan Debian apt [SETTINGS-SCRIPTS-PAKET-APT]
- [x] feat(settings): Home Settings kini memuat ubin Sistem, Script execution, dan Paket
- [x] feat(settings): Script execution dan Paket berisi placeholder "Segera hadir"; yt-dlp dipindah dari Sistem ke Paket (jalur Settings > Paket > Mesin cadangan)
- [x] feat(settings): Sistem memuat pintasan Debian apt update dan apt upgrade -y (dialog konfirmasi + keluaran langsung lewat ProotScriptExecutor, timeout 10/40 menit)
- [x] Dampak: :app saja (SettingsScreen.kt, DebianAptUi.kt, PackageScriptUi.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: Canvas drawRoundRect, RunRequest named args, delegasi state di lambda
- [ ] uji perangkat: 3 ubin di Home; yt-dlp via Paket; apt update/upgrade (internet aktif, Debian terpasang/belum); Berhenti; Salin; pencarian apt/paket/script
- [ ] lanjut: isi Script execution (pintasan script), isi Paket (status, pasang/hapus), generalisasi RunDialog

## 2026-10-26 - Pratinjau HTML native + opsi WebView [CODE-HTML-PREVIEW]
- [x] feat(code): Play pada .html/.htm/.xhtml membuka Tinjauan penuh; HtmlPreview (pure Kotlin) mengubah HTML ke model blok yang sama dengan Markdown (judul, paragraf, daftar bertingkat, tabel, kutipan, pre, tautan, inline). Script/style/head/svg dilewati, toleran HTML rusak
- [x] feat(settings): halaman "Editor Code" (Sistem) dengan saklar "Pakai WebView untuk HTML" (CodePrefs, default mati). WebView: JS mati, jaringan diblokir, tanpa akses berkas/konten, navigasi dicegat
- [x] Diuji di JVM: 10.682 uji lulus. Gerbang telemetri lulus. Manifest dan JNI mpv tidak disentuh
- [ ] CI commit HTML: ISI SETELAH CEK TAB ACTIONS. Risiko: WebView/AndroidView(onRelease), pencarian Settings
- [ ] uji perangkat: Play di .html (tabel, daftar, tautan); saklar WebView lalu Play lagi; tautan di WebView terbuka di peramban; Back menutup Tinjauan
- [ ] lanjut: aktifkan isi halaman "Script execution" (Pintasan skrip)

## 2026-10-26 - Script Execution + keluaran skrip satu halaman [SCRIPT-EXECUTION]
- [x] feat(code): menu titik tiga "Kirim ke Script Execution" (ganti "Kirim ke Library"); sumber sama = entri diperbarui
- [x] feat(code): Play pada .sh/.py membuka halaman Keluaran penuh (status, BERHENTI/ULANGI/BAGIKAN); panel bawah lama dihapus
- [x] feat(settings): Script execution: accordion judul/catatan/isi (maks 3.000 baris), tambah/ubah/hapus/salin, ScriptStore JSON atomik (maks 50)
- [x] feat(settings): JALANKAN menulis skrip ke /root/.molina/scripts lalu mengetik chmod +x + Enter ke sesi Molina Terminal (aktif/baru), pindah ke tab Terminal
- [x] Diuji di JVM: 12.712 uji lulus (ScriptLaunch: hitung baris, CRLF, nama berkas, perintah). Manifest dan JNI mpv tidak disentuh
- [ ] CI commit Script Execution: ISI SETELAH CEK TAB ACTIONS. Risiko: BasicTextField 3.000 baris, key(), return@Column di accordion, ScriptNav di MainActivity
- [ ] uji perangkat: kirim dari editor lalu cek di Settings; kirim ulang (diperbarui); tambah/edit/hapus; >3.000 baris menolak simpan; JALANKAN sesi aktif dan baru; Debian belum terpasang; .py dan .sh membuka Keluaran penuh, Back menutup
- [ ] lanjut: sinkron dua arah (Settings ke editor) dan tombol "Buka di editor"

## 2026-10-26 - Script Execution: daftar ringkas dan edit di editor Code [SCRIPTS-COMPACT-EDIT-IN-CODE]
- [x] refactor(settings): daftar skrip hanya judul + ikon jalankan; ketuk judul menampilkan detail penuh (catatan, meta, pratinjau 30 baris)
- [x] feat(code): EDIT dan + TAMBAH di Script Execution membuka tab editor Code (CodeNav + CodeSession.openScript); Kirim ke Script Execution memperbarui entri yang sama dan memadamkan penanda belum-disimpan
- [x] Editor inline di Settings dihapus; CATATAN tetap diubah lewat dialog kecil
- [x] ScriptSink kini mengembalikan ScriptSendResult(ok, message)
- [x] Dampak: :feature:code dan :app (ScriptSink, CodeSession, ScriptStore, ScriptRunner, MainActivity, ScriptExecutionUi). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang-rendah
- [ ] uji perangkat: daftar ringkas; ketuk judul; ketuk segitiga; EDIT ke tab Code; kirim balik tidak menggandakan; + TAMBAH dengan template; hapus/catatan/salin
- [ ] catatan: tab skrip yang dipulihkan dari draf setelah proses mati kehilangan tautan (kirim ulang membuat entri baru)

## 2026-10-26 - Paket: alat khusus (pasang, versi, hapus) [PAKET-ALAT-PASANG-VERSI-HAPUS]
- [x] feat(settings): Paket berisi yt-dlp (halaman khusus), alat Debian bawaan (Python 3, Git, FFmpeg), dan alat buatan pengguna (ToolStore, SharedPreferences JSON, maks 30)
- [x] feat(settings): tiap alat punya skrip pasang, versi, hapus; dijalankan di Debian PRoot lewat ToolRunDialog (konfirmasi + pratinjau perintah, keluaran langsung, Berhenti/Salin); hasil VERSI tersimpan
- [x] feat(settings): yt-dlp mendapat Versi dan Hapus; DebianAptDialog kini memakai ToolRunDialog (kode dibagi)
- [x] Pembagian menu: Script execution = skrip umum; Paket = skrip khusus alat
- [x] Dampak: :app saja (packages/ToolStore.kt, settings/ToolRunUi.kt, ToolsUi.kt, DebianAptUi.kt, PackageScriptUi.kt, SettingsScreen.kt, SystemRefreshUi.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang-rendah
- [ ] uji perangkat: kartu alat; VERSI/PASANG/HAPUS; tambah-ubah-buang alat; yt-dlp Versi/Hapus; apt update/upgrade
- [ ] lanjut: ubah nama skrip dari Settings (Script execution); SVG branding untuk ikon ubin

## 2026-10-26 - Settings > Sistem > Debian: update + upgrade digabung [DEBIAN-APT-GABUNG]
- [x] refactor(settings): AptJob.Update dan AptJob.Upgrade digabung jadi AptJob.UpdateUpgrade (`apt update && apt upgrade -y`, upgrade hanya jalan bila update sukses; timeout 50 menit)
- [x] SettingsScreen: dua menu Debian jadi satu "Perbarui dan tingkatkan paket"; entri pencarian apt update/upgrade menunjuk ke aksi yang sama
- [x] feat(settings): ikon baru W7IconAptSync (dua panah melingkar) menggantikan ubin teks upd/upg
- [x] feat(settings): ToolRun mendapat keyPattern + summarize (opsional); dialog apt menampilkan ringkasan ("N paket ditingkatkan..." / "Sistem sudah terbaru.") dari baris kunci, tahan log terpotong
- [x] Dampak: :app saja (settings/DebianAptUi.kt, ToolRunUi.kt, PackageScriptUi.kt, SettingsScreen.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah-sedang (ikon Canvas + parameter baru ToolRun, semua default sehingga pemanggil lain tidak berubah)
- [ ] uji perangkat: ikon tampil rapi; konfirmasi lalu update->upgrade berurutan; ringkasan muncul di status; offline -> upgrade tidak jalan, tanpa ringkasan

## 2026-10-26 - Debian: daftar tool terpasang, hapus tuntas, riwayat apt [DEBIAN-TOOLS-PURGE]
- [x] feat(settings): menu Debian "Daftar tool terpasang" (InstalledToolsUi.kt): satu skrip sh mendeteksi Python 3, Git, FFmpeg, alat buatan pengguna, dan yt-dlp beserta versinya + info rilis Debian; hasil disimpan ke kartu Paket (ToolStore.setCheck)
- [x] feat(packages): ToolPurge.script = purge paket, autoremove --purge, apt clean, hapus konfigurasi/cache/sisa berkas, verifikasi (exit 3 bila ada sisa); dipakai HAPUS Python 3, Git, FFmpeg dan Hapus yt-dlp (semua salinan biner, cache, config)
- [x] feat(settings): menu apt menampilkan "Terakhir berhasil: <tanggal>" (AptHistory, SharedPreferences molina_apt); ringkasan apt dicatat ke Log; tombol Salin menyertakan status + ringkasan
- [x] Dampak: :app saja (packages/ToolStore.kt, settings/ToolsUi.kt, SystemRefreshUi.kt, ToolRunUi.kt, DebianAptUi.kt, PackageScriptUi.kt, SettingsScreen.kt, InstalledToolsUi.kt baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (string skrip Kotlin + parsing keluaran)
- [ ] uji perangkat: Daftar tool terpasang (rootfs ada/tidak ada); HAPUS Python 3/Git/FFmpeg -> baris Verifikasi "Tuntas"; Hapus yt-dlp -> status kembali NoYtdlp; pasang ulang berhasil; subtitle "Terakhir berhasil" muncul setelah apt sukses
- [ ] catatan: alat buatan pengguna tetap memakai skrip hapus buatan sendiri (tidak otomatis tuntas)

## 2026-10-26 - Debian: daftar berwarna, pratinjau hapus, cadangan konfigurasi [DEBIAN-UX-PREVIEW-BACKUP]
- [x] feat(settings): "Daftar tool terpasang" kini daftar Compose berwarna (hijau = terpasang, abu-abu = tidak ada, kuning = skrip versi kosong) + info rilis Debian; tidak lagi log mentah; tombol Salin menyalin daftar teks
- [x] feat(settings): HAPUS alat bawaan lewat PurgePreviewDialog: simulasi `apt purge --auto-remove -s` menampilkan paket yang akan dihapus (paket lain yang ikut terhapus ditandai kuning); lanjut hanya bila simulasi berhasil; konfirmasi kedua dilewati
- [x] feat(packages): opsi cadangan sebelum HAPUS Git (.gitconfig, .config/git) ke /sdcard/molina-studio/.molina-backup/<alat>-<waktu>; .git-credentials TIDAK dicadangkan (token teks biasa di storage bersama); folder otomatis dikecualikan lewat .git/info/exclude; gagal salin -> exit 4, hapus dibatalkan
- [x] refactor(settings): startShell, describe, copyOutput menjadi internal (dipakai dialog baru); ToolItem mendapat purgePkgs dan backupPaths (default kosong, alat buatan pengguna tidak berubah)
- [x] Dampak: :app saja (packages/ToolStore.kt, settings/ToolRunUi.kt, ToolsUi.kt, InstalledToolsUi.kt, PurgePreviewUi.kt baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (dua dialog Compose baru + parsing keluaran apt)
- [ ] uji perangkat: titik warna daftar tool; HAPUS Git -> pratinjau muncul, cek kotak cadangan, berkas muncul di /storage/emulated/0/molina-studio/.molina-backup; `git status` di repo tidak menampilkan folder cadangan; HAPUS Python 3 -> paket lain yang ikut terhapus berwarna kuning
- [ ] catatan: cadangan hanya bekerja bila folder kerja molina-studio ter-bind di PRoot (default); bila tidak, berkas tertulis di dalam rootfs

## 2026-10-26 - Settings: daftar tool berwarna, pratinjau hapus, cadangan Git
- [x] Daftar tool terpasang berwarna (titik status per tool)
- [x] Pratinjau paket yang akan terhapus sebelum HAPUS
- [x] Cadangan konfigurasi Git ke `.molina-backup` (opsional via centang)
- [x] Commit + push, build via CI, uji fisik di perangkat: lolos
- Dampak: hanya modul `:app`; Gradle, Manifest, JNI mpv tidak berubah
- Berikutnya: tombol "Pulihkan cadangan" di kartu Git; halaman "Inventaris Molina"

## 2026-10-26 - Penjelajah Molina di Settings [EXPLORER-SETTINGS]
- [x] feat(settings): Win7OpenDialog mendapat mode JELAJAH (browse=true: filter Semua file, judul Penjelajah Molina, tombol Salin path dan Ke Terminal); mode PILIH_MEDIA untuk Cinema tidak berubah (parameter baru semua default)
- [x] feat(settings): ExplorerBridge.kt (MolinaExplorer): path internal dipetakan ke /sdcard (di-bind PRoot), di-quote untuk shell, dikirim lewat emulator.paste tanpa Enter
- [x] feat(settings): menu Sistem > Berkas > Penjelajah Molina + entri pencarian
- [x] Dampak: :app saja (cinema/Win7OpenDialog.kt, settings/SettingsScreen.kt, settings/ExplorerBridge.kt baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah-sedang (parameter default Compose + sisipan layout)
- [ ] uji perangkat: Settings > Sistem > Penjelajah Molina; pilih file > Ke Terminal (path ter-quote muncul di prompt, tanpa Enter); Salin path; folder SD card -> peringatan tidak terlihat di Debian; tombol File di Cinema tetap mode media
- [ ] catatan: hanya penyimpanan internal yang di-bind ke Debian

## 2026-10-26 - Penjelajah Molina: aksi cd dan favorit [EXPLORER-CD-FAV]
- [x] feat(settings): tombol cd di Penjelajah Molina (aktif untuk folder): mengirim `cd '<path>'` + Enter ke sesi aktif lewat TerminalActions.send; ditolak bila di luar penyimpanan internal
- [x] feat(settings): favorit Molina Studio (/sdcard/molina-studio) di panel kiri, hanya pada mode JELAJAH; tata letak aksi: path di baris sendiri, tiga tombol berbagi lebar
- [x] Audit: FilePickerActivity dan paket filepicker/ (:feature:mpv) MASIH dipakai MPVActivity (tambah playlist, audio/subtitle eksternal), jadi TIDAK dihapus
- [x] Audit: cinema.MainActivity dan MainScreenFragment tampak yatim (tidak ada pemanggil); penghapusan butuh ubah Manifest mpv (MainActivity, parentActivityName PreferenceActivity), ditunda
- [x] Dampak: :app saja (Win7OpenDialog.kt, SettingsScreen.kt, ExplorerBridge.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah
- [ ] uji perangkat: pilih folder > cd (prompt pindah direktori); file terpilih > tombol cd nonaktif; favorit Molina Studio muncul hanya di Penjelajah (tidak di tombol File Cinema); folder SD card > cd ditolak dengan toast

## 2026-10-26 - Pembersihan UI lama mpv [MPV-CLEANUP-MAIN]
- [x] refactor(mpv): hapus MainActivity.kt, MainScreenFragment.kt, layout activity_main.xml dan fragment_main_screen.xml (UI mpv-android lama; tidak ada pemanggil, layar utama kini CinemaHome di :app)
- [x] refactor(mpv): Manifest mpv: hapus entri .MainActivity dan parentActivityName=.MainActivity pada PreferenceActivity (home sudah ditangani onBackPressedDispatcher)
- [x] Dipertahankan: FilePickerActivity dan paket filepicker/ (masih dipakai MPVActivity: tombol Audio/Subtitle eksternal di menu atas dan tombol Pilih file di playlist)
- [x] Dampak: :feature:mpv (kode, layout, AndroidManifest.xml). Gradle dan JNI mpv tidak disentuh. Aman bagi :app (MainActivity :app berbeda paket)
- [ ] BELUM dikompilasi (CI). Risiko: sedang (hapus kelas + Manifest; pengaman Python sudah memindai pemakai tersisa)
- [ ] uji perangkat: aplikasi tetap terbuka ke CinemaHome; putar video; menu Audio/Subtitle eksternal dan playlist Pilih file tetap jalan; Pengaturan pemutar terbuka dan tombol kembali berfungsi; About dari pengaturan terbuka

## 2026-10-26 - Hapus picker lama mpv [MPV-REMOVE-PICKER]
- [x] refactor(mpv): hapus FilePickerActivity, paket filepicker/ (AbstractFilePickerFragment, FilePickerFragment, DocumentPickerFragment, dst.), MPVFilePickerFragment, MPVDocumentPickerFragment, layout/menu picker; entri Manifest FilePickerActivity dibuang
- [x] refactor(mpv): MPVActivity: buang plumbing picker (openFilePickerFor, onActivityResult, RCODE_*, ActivityResultCallback) dan tombol Audio/Subtitle eksternal di menu atas; PlaylistDialog: buang tombol Pilih file (tombol URL tetap)
- [x] Keputusan pengguna (opsi C): fitur tambah audio/subtitle eksternal dan tambah file ke playlist dari dalam pemutar dihapus; memilih file dilakukan lewat Penjelajah/Win7OpenDialog di Cinema
- [x] Dampak: :feature:mpv (kode, layout, AndroidManifest.xml). Gradle dan JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang-tinggi (banyak berkas dihapus + penyuntingan MPVActivity); pengaman Python sudah memindai sisa referensi
- [ ] uji perangkat: putar video dari Cinema; menu atas pemutar (tanpa tombol Audio/Subtitle eksternal); playlist terbuka (tombol URL tetap ada); lanjut/putar ulang normal; pengaturan pemutar terbuka

## 2026-10-26 - Tombol File Cinema: pilih berkas atau folder [CINEMA-OPEN-FOLDER]
- [x] feat(cinema): tombol File di CinemaHome membuka dialog pilihan (CinemaOpenChooser): Buka berkas (Win7OpenDialog seperti sebelumnya) atau Buka folder
- [x] feat(cinema): Win7OpenDialog mode pickFolder (judul Buka folder, tombol Pilih folder, tanpa kolom nama file); folder dipilih = folder yang sedang ditampilkan
- [x] feat(mpv): MpvFolder.firstMediaIn (publik, tidak rekursif); CinemaActions.playFolder mengirim filepath + folder_playlist ke MPVActivity, playlist dibangun Utils.folderPlaylist (tanpa ubah MPVActivity)
- [x] Dampak: :app (CinemaHome, CinemaActions, Win7OpenDialog, CinemaOpenChooser baru) dan :feature:mpv (MpvFolder baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah-sedang (edit Compose di Win7OpenDialog)
- [ ] uji perangkat: File > dialog 2 pilihan; Buka berkas tetap normal; Buka folder > masuk folder berisi beberapa video > Pilih folder > Playlist di pemutar berisi semua video berurutan alami; folder tanpa media > pesan di Cinema; folder berisi 1 media > diputar tunggal

## 2026-10-26 - Perbaikan buka folder dan cd Penjelajah [FIX-FOLDER-CD]
- [x] fix(mpv): MPVActivity.onNewIntent: intent folder_playlist tidak lagi masuk jalur "tambah di akhir" saat pemutar berjalan di latar (hanya berkas tunggal yang ditambahkan); kini mengganti playlist lewat loadReplacing. Toast "Playlist folder: N berkas" sebagai umpan balik
- [x] fix(explorer): MolinaExplorer.visibleInDebian mengikuti bind PRoot: tanpa Akses penyimpanan penuh hanya /sdcard/molina-studio yang terlihat; cd ke folder lain ditolak dengan petunjuk (sebelumnya cd terkirim dan gagal: No such file or directory)
- [x] Catatan: folder /sdcard/molina_suite tidak ada di Debian karena bind default hanya molina-studio; solusi: aktifkan Akses penyimpanan penuh (sesi baru) atau pindahkan folder
- [x] Dampak: :app (ExplorerBridge.kt) dan :feature:mpv (MPVActivity.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah
- [ ] uji perangkat: putar video lalu keluar ke Cinema (pemutar di latar) > File > Buka folder > pemutar naik ke depan, toast Playlist folder N berkas, daftar playlist terisi; Penjelajah: cd ke luar molina-studio tanpa akses penuh > toast petunjuk; dengan akses penuh (sesi baru) > cd berhasil

## 2026-10-26 - Penanda Debian di Penjelajah [EXPLORER-DEBIAN-BADGE]
- [x] feat(explorer): panel path Penjelajah menampilkan penanda kecil "Terlihat di Debian" (hijau) atau "Di luar Debian" (kuning) untuk folder/berkas terpilih, mengikuti MolinaExplorer.visibleInDebian (bind PRoot + Akses penyimpanan penuh)
- [x] Dampak: :app (Win7OpenDialog.kt, SettingsScreen.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah
- [ ] uji perangkat: Penjelajah > molina-studio = Terlihat di Debian; folder lain = Di luar Debian; aktifkan Akses penyimpanan penuh (sesi baru) > semua internal Terlihat; kartu SD selalu Di luar

## 2026-10-26 - Playlist folder + subfolder dan ikon emas [CINEMA-FOLDER-DEEP]
- [x] feat(cinema): dialog File punya pilihan ketiga "Buka folder dan subfolder"; MpvFolder.mediaTree (rekursif, maks 1000 berkas, kedalaman 6, lewati folder tersembunyi dan putaran symlink) berjalan di thread IO, hasilnya dikirim sebagai playlist memory:// (CinemaActions.playTree) tanpa ubah MPVActivity
- [x] feat(cinema): ikon dialog diganti ikon vektor emas (film, folder, tumpukan folder) via Canvas; emoji dihapus dari CinemaOpenChooser
- [x] Subtitle otomatis: TIDAK perlu kode baru. mpv memakai sub-auto=exact secara default (tidak ada override di repo), jadi film.srt di samping film.mkv dimuat otomatis
- [x] Dampak: :app (CinemaHome, CinemaActions, CinemaOpenChooser) dan :feature:mpv (MpvFolder). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (kode Canvas Compose baru)
- [ ] uji perangkat: File > 3 pilihan dengan ikon emas; Buka folder dan subfolder pada folder berisi subfolder per judul > playlist berisi semua media berurutan; folder > 1000 berkas terpotong di 1000; film.srt di samping film.mkv tampil otomatis

## 2026-10-26 - Penjelajah ikuti sesi aktif dan kirim + Enter [EXPLORER-SESSION-ENTER]
- [x] fix(terminal): TerminalSessions mencatat lingkup storage per sesi saat dibuat (fullAccessOf); sesi lama tetap memakai bind lama walau Akses penyimpanan penuh diaktifkan kemudian
- [x] fix(explorer): visibleInDebian dan penanda Debian mengikuti sesi aktif; toast cd memberi tahu bila sesi dibuat sebelum akses penuh aktif (buka NEW SESSION)
- [x] feat(explorer): sakelar "Ke Terminal langsung ditekan Enter": path di-quote lalu dikirim dengan Enter (\r); untuk folder diubah menjadi cd. Default mati (perilaku lama: tempel tanpa Enter)
- [x] Temuan uji: setelah akses penuh diaktifkan, /sdcard/mykuniran tidak ditemukan karena sesi terminal lama tetap berjalan (sesi hidup di service); perlu sesi baru
- [x] Dampak: :app (TerminalSessions.kt, ExplorerBridge.kt, Win7OpenDialog.kt, SettingsScreen.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (menyentuh TerminalSessions.debianShell)
- [ ] uji perangkat: aktifkan akses penuh saat sesi lama hidup > penanda tetap Di luar Debian + cd ditolak dengan petunjuk NEW SESSION; buka NEW SESSION > ls /sdcard menampilkan seluruh storage, penanda hijau, cd berhasil; sakelar Enter aktif: file > dijalankan, folder > cd; mati: path hanya ditempel

## 2026-10-26 - Kontrak penyimpanan Molina: akar aplikasi terpisah dari project [STORAGE-CONTRACT-1]
- [x] Keputusan: /sdcard/molina-studio = folder project (pengembangan), BUKAN data aplikasi; data aplikasi pindah ke akar terpisah /sdcard/Molina
- [x] MolinaPaths: appRoot (/sdcard/Molina), devProject (/sdcard/molina-studio), privateRoot; workspace jadi alias devProject sementara; dataRoot/.molina-data dihapus
- [x] MolinaDir: Downloads, Video/Audio/Iptv (Media/...), Workspace, Scripts, Backup, Documents, Export (Documents/Export); Media lama dipecah
- [x] AndroidMolinaPaths.ensureDirs membuat appRoot + semua subfolder; AppPaths mencatat appRoot ke AppLog
- [x] Dampak: :core:common, :core:storage, :app (AppPaths.kt) + docs/HANDOFF.md. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah (tanpa pemakai MolinaDir.Media; hanya Export dipakai LogScreen)
- [ ] uji perangkat: buka aplikasi dengan izin storage > /sdcard/Molina berisi Downloads, Media/{Video,Audio,IPTV}, Workspace, Scripts, Backup, Documents/Export; ekspor log tersimpan di Documents/Export; log sistem memuat appRoot=/sdcard/Molina
- [ ] LANGKAH BERIKUT (belum dikerjakan): pindahkan pemakai satu per satu: backup ToolStore > Backup, simpan script Settings > Scripts, download mpv > Downloads/Media, HOME_DIR/bind terminal > Workspace (molina-studio jadi bind opsional); migrasi isi .molina-data lama bila ada

## 2026-10-26 - Skrip Molina DL ikut struktur Media [DL-SCRIPT-MEDIA-DIRS]
- [x] molina_dl.py: video > /sdcard/Molina/Media/Video, audio > /sdcard/Molina/Media/Audio (MOLINA_ROOT / MOLINA_DL_DIR sebagai override)
- [x] Akar tidak dibuat skrip; bila belum terlihat di Debian dipakai folder cadangan molina-studio/unduhan/{Video,Audio} dengan peringatan
- [x] Tambahan: tolak URL playlist, Ctrl+C di prompt kembali ke menu, daftar berkas hasil, retry, nama berkas aman, cek ruang kosong
- [x] Dampak: hanya skrip pengguna (di luar repo). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] uji perangkat dengan yt-dlp asli: video dan audio masuk ke folder yang benar setelah akses penuh aktif
- [ ] LANGKAH BERIKUT: bind appRoot di TerminalSessions dan ProotScriptExecutor supaya /sdcard/Molina terlihat tanpa akses penuh; opsional kirim MOLINA_ROOT dari aplikasi

## 2026-10-26 - Bind /sdcard/Molina ke Debian tanpa akses penuh [PROOT-BIND-APPROOT]
- [x] StorageBinds.app (baru, :app/terminal): bind appRoot ke /sdcard/Molina dan /storage/emulated/0/Molina; titik mount rootfs hanya dibuat bila bind dipasang (skrip di guest bisa mendeteksi bind lewat isdir)
- [x] TerminalSessions.debianShell dan ProotScriptExecutor.exec menambah bind appRoot di mode default (mode akses penuh tidak berubah)
- [x] GuestPaths.of: parameter extraRoots (default kosong, tes lama tetap); cwd skrip dari /sdcard/Molina kini terpetakan
- [x] ExplorerBridge.visibleInDebian + teks pengaturan: /sdcard/Molina ikut dianggap terlihat di Debian; 2 tes baru di PreviewTests
- [x] Dampak: :app, :feature:code, scripts/code-tests. Gradle, Manifest, JNI mpv tidak disentuh; bind molina-studio tidak diubah
- [ ] BELUM dikompilasi (CI). Risiko: sedang (menyentuh debianShell dan exec PRoot)
- [ ] uji perangkat: akses penuh MATI + sesi BARU > ls /sdcard/Molina menampilkan Media/Video dst; molina_dl.py memakai Media/Video dan Media/Audio tanpa peringatan; Penjelajah: cd ke /sdcard/Molina berhasil; sesi lama (sebelum update) tetap tanpa bind; akses penuh AKTIF tetap normal
- [ ] LANGKAH BERIKUT: pindahkan HOME_DIR terminal ke Workspace; bundle molina_dl.py sebagai asset

## 2026-10-26 - Folder Crash + tempat Molina di dialog buka berkas [STORAGE-CRASH-DIR]
- [x] MolinaDir.Crash (/sdcard/Molina/Crash) ditambahkan; ensureDirs otomatis membuatnya
- [x] CrashReporter menyalin laporan ke Crash/ (URL dan token disamarkan LogRedact); salinan privat files/crash tetap utama dan tetap dipakai daftar di Settings. Hapus di Settings tidak menghapus salinan di Crash/
- [x] Dialog buka berkas: tempat "Molina" (ikon folder emas Win7MolinaFolder) menuju /sdcard/Molina, tampil juga di mode pilih media; tempat "Molina Studio" diganti "Project" (hanya mode Penjelajah) karena menunjuk folder project molina-studio
- [x] Dampak: :core:common, :app. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah
- [ ] uji perangkat: buka aplikasi dengan izin storage > /sdcard/Molina/Crash ada; picu crash uji > crash-*.txt muncul di Crash/ dan di Settings > Terminal > Debugging; dialog buka berkas menampilkan "Molina" berikon emas di Favorit
- [ ] LANGKAH BERIKUT: bundle molina_dl.py sebagai asset (salin ke Scripts/ tanpa menimpa, kirim MOLINA_ROOT); terakhir pindahkan HOME_DIR terminal ke Workspace

## 2026-10-26 - Alat berkas di Penjelajah Molina [EXPLORER-FILEOPS]
- [x] feat(explorer): Settings > Sistem > Berkas > Penjelajah Molina mendapat menu titik tiga "⋮ Alat" (hanya mode JELAJAH; tombol File Cinema tidak berubah): Salin, Pindah, Tempel, Ubah nama, Kompres (ZIP), Pindah ke favorit / Hapus dari favorit, Properti
- [x] FileOps.kt (baru, murni JVM): salin/pindah rekursif (nama bentrok jadi "nama (1)"), tolak folder ke dalam dirinya sendiri, pindah beda volume = salin lalu hapus sumber, ubah nama tervalidasi, ZIP rekursif (tautan simbolik dilewati), properti (ukuran dan isi folder dihitung di IO)
- [x] ExplorerFavorites.kt (baru): favorit buatan pengguna disimpan di SharedPreferences molina_explorer, tampil di panel Favorit setelah favorit bawaan
- [x] Semua operasi berjalan di Dispatchers.IO, tombol Alat dikunci selama proses, pesan hasil hijau (sukses) atau merah (gagal); butuh izin WRITE_EXTERNAL_STORAGE (sudah diminta MainActivity)
- [x] Dampak: :app saja (cinema/Win7OpenDialog.kt, FileOps.kt, ExplorerFavorites.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (Popup + Dialog bertingkat di Compose, edit Win7OpenDialog)
- [ ] uji perangkat: pilih berkas > Salin > buka folder lain > Tempel (muncul); Pindah + Tempel (sumber hilang); Pindah ke folder yang sama ditolak; salin folder ke dalam dirinya ditolak; Ubah nama (ekstensi tidak ikut terpilih, nama bentrok ditolak); Kompres folder > .zip muncul dan bisa dibuka; Pindah ke favorit pada folder > muncul di panel kiri dan bertahan setelah aplikasi ditutup, lalu Hapus dari favorit; Properti folder besar tidak membekukan UI; tanpa pilihan, Properti menampilkan folder aktif
- [ ] LANGKAH BERIKUT: Hapus (dengan konfirmasi), pilih banyak berkas, Buat folder baru, Ekstrak ZIP, progres untuk salinan besar

## 2026-10-26 - Bundle molina_dl.py dan HOME_DIR terminal = Workspace [BUNDLE-DL-HOME-WORKSPACE]
- [x] feat(scripts): assets/molina/molina_dl.py (disalin dari perangkat; syntax dicek saat patch); BundledScripts.install menyalin ke /sdcard/Molina/Scripts tanpa menimpa berkas yang sudah ada, dipanggil AppPaths.ensureIfPermitted setelah ensureDirs sukses
- [x] feat(terminal): MolinaShell.APP_ROOT=/sdcard/Molina dan HOME_DIR=/sdcard/Molina/Workspace (sebelumnya /storage/emulated/0/molina-studio); MOLINA_HOME mengikuti; MOLINA_ROOT dikirim ke sesi hanya bila folder aplikasi terlihat di Debian (akses penuh atau bind appRoot)
- [x] fix(terminal): bashrc: prompt hijau di ~, Workspace, dan /sdcard/molina-studio; /storage/emulated/0/... disamakan dengan /sdcard/... (sebelumnya hanya cocok bila PWD persis HOME_DIR)
- [x] Tidak diubah: HOME=/root (dotfile tetap di rootfs), cwd awal /root, bind molina-studio, ProotScriptExecutor/ScriptCommand (skrip dari editor Code belum menerima MOLINA_ROOT; molina_dl.py memakai /sdcard/Molina sebagai default)
- [x] Dampak: :app (BundledScripts baru, AppPaths, MolinaShell, TerminalSessions, assets bashrc dan molina_dl.py). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (menyentuh debianShell dan assets bashrc)
- [ ] uji perangkat: buka aplikasi dengan izin storage > /sdcard/Molina/Scripts/molina_dl.py ada (dan tidak ditimpa bila diedit); sesi BARU > echo $MOLINA_ROOT = /sdcard/Molina; cd /sdcard/Molina/Workspace > prompt hijau; cd /storage/emulated/0/Molina/Workspace > hijau; folder lain merah; python3 /sdcard/Molina/Scripts/molina_dl.py berjalan tanpa peringatan folder cadangan

## 2026-10-26 - Molina DL v2: indikator progres gaya IDM [DL-V2-PROGRESS]
- [x] feat(scripts): molina_dl.py v2: bar langsung (persen, kecepatan, ETA, ukuran) dari --progress-template yt-dlp; satu baris per aliran (Video/Audio) + langkah Merger/ExtractAudio; antrian banyak URL dengan ringkasan; Ctrl+C membatalkan dan .part dilanjutkan; mode non-TTY cetak per 10%; MOLINA_DL_ASCII/NO_COLOR
- [x] Uji: yt-dlp 2026.10.21 + ffmpeg asli ke server lokal (PTY 44-50 kolom): antrian sukses+gagal, batal+lanjut, tanpa proses sisa; dua aliran + Merger memakai yt-dlp tiruan. Belum diuji: situs sungguhan dan layar Molina Terminal
- [x] Dampak: hanya skrip pengguna/asset molina_dl.py. Kotlin, Gradle, Manifest, JNI mpv tidak disentuh
- [ ] uji perangkat: bar tampil benar di Molina Terminal (font blok, warna emas); unduh YouTube video+audio; antrian 2-3 URL; Ctrl+C lalu lanjut
- [ ] LANGKAH BERIKUT: halaman native Unduhan (streaming per baris lewat jalur ProotScriptExecutor, parser event di :core:common, foreground service); mode CLI non-interaktif/--json; BundledScripts memperbarui salinan yang belum diedit pengguna (hash)

## 2026-10-26 - molina_dl.py belum ter-commit [DL-ASSET-GITIGNORE]
- [x] Temuan: .gitignore (molina_*.py) mengabaikan assets/molina/molina_dl.py sehingga aset bundel tidak ada di repo/CI
- [ ] Tambah pengecualian .gitignore, commit molina_dl.py ke assets
- [ ] LANGKAH BERIKUT: mode CLI --url --json; BundledScripts perbarui salinan via hash; halaman native Unduhan

## 2026-10-26 - molina_dl v3: mode --json, inti Unduhan, pembaruan skrip via hash [DL-V3-JSON-HASH]
- [x] fix(repo): .gitignore (molina_*.py) mengabaikan assets/molina/molina_dl.py sehingga tidak pernah ter-commit/CI; ditambah pengecualian dan skrip kini ada di repo
- [x] feat(scripts): molina_dl.py v3 (dibangun dari salinan v1 di perangkat; fitur v2 ditulis ulang): bar emas per aliran, antrian banyak URL, mode non-TTY per 10%, CLI non-interaktif (--json, --info, --mode/--height/--ext/--audio-format/--bitrate/--dir, --version), kode keluar 0/1/2/130, SIGTERM membatalkan dengan .part tetap
- [x] Uji: yt-dlp 2026.10.21 + ffmpeg ke server lokal: --json (antrian 404+sukses, ulang=existed, audio mp3, URL salah rc=2, --info, batal rc=130), bar PTY 44 kolom. Belum diuji: situs sungguhan, dua aliran video+audio dengan Merger, layar Molina Terminal
- [x] feat(core): DownloadEvent/DownloadEventParser, DownloadReducer (keadaan sesi), BundledScriptPolicy; tes JVM scripts/core-tests/run.sh (45 pemeriksaan lulus)
- [x] feat(terminal): BundledScripts memperbarui salinan yang belum diedit (SHA-256 tercatat di prefs molina_bundled_scripts); berkas diedit/asal tak diketahui tidak ditimpa, versi baru ditulis sebagai molina_dl.py.new
- [x] docs: docs/unduhan-native.md (rancangan halaman Unduhan)
- [x] Dampak: :core:common (berkas baru), :app (BundledScripts), assets, .gitignore. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI); Kotlin baru diuji dengan kotlinc 2.1.20 (JVM murni). Risiko: rendah-sedang (BundledScripts memakai API Android)
- [ ] uji perangkat: perangkat Anda sudah punya molina_dl.py (v2) tanpa hash tercatat > buka aplikasi > muncul Scripts/molina_dl.py.new, berkas lama tetap; python3 molina_dl.py.new --version = 3; hapus berkas lama lalu buka aplikasi > terpasang v3 dan tercatat
- [ ] LANGKAH BERIKUT: RunRequest.args + ProotScriptExecutor; DownloadRunner (state flow); foreground service; UnduhanScreen

## 2026-10-26 - Penjelajah: keyboard, pindah ke root, kelola berkas lanjutan [EXPLORER-FILEOPS-2]
- [x] fix(explorer): keyboard tidak lagi muncul sendiri saat Penjelajah dibuka (soft input mode HIDDEN pada jendela dialog, clearFocus, dan sembunyikan IME); mengetik di kolom Nama file tetap memunculkannya
- [x] refactor(settings): Penjelajah Molina pindah dari Sistem ke beranda Settings sebagai ubin "Berkas" sejajar Sistem, Script execution, dan Paket (Page.Files membuka dialog otomatis; tutup dialog kembali ke beranda); entri pencarian ikut diperbarui; Category "Berkas" di halaman Sistem dihapus
- [x] feat(explorer): mode Pilih (kotak centang, Semua, Selesai; Back keluar dari mode pilih dulu) sehingga Salin, Pindah, Tempel, Kompres, Hapus bekerja massal
- [x] feat(explorer): Hapus dengan dialog konfirmasi (akar penyimpanan dan /sdcard/Molina dilindungi), Folder baru, Ekstrak ZIP (zip-slip ditolak), Kompres banyak item jadi satu ZIP
- [x] feat(explorer): progres byte + tombol Hentikan untuk salin/pindah/ZIP/ekstrak/hapus; hasil parsial dibersihkan saat gagal atau dihentikan
- [x] feat(explorer): ikon garis bergaya Aero di menu Alat (Canvas, tanpa aset); Properti mendukung banyak item (total ukuran dan isi)
- [x] Dampak: :app saja (cinema/FileOps.kt, cinema/Win7OpenDialog.kt, settings/SettingsScreen.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: sedang (Popup/Dialog bertingkat, setSoftInputMode, edit Compose besar). FileOps sudah diuji JVM murni dengan kotlinc di luar CI
- [ ] uji perangkat: buka Settings > Berkas > keyboard TIDAK muncul; ubin Berkas ada di beranda dan tidak lagi di Sistem; Pilih > centang beberapa berkas > Salin > folder lain > Tempel; Pindah massal; Hapus 1 berkas dan 1 folder (konfirmasi muncul, akar penyimpanan ditolak); Folder baru (nama bentrok ditolak); Kompres banyak item; Ekstrak ZIP; salinan berkas besar menampilkan progres lalu Hentikan membatalkan dan tidak meninggalkan sisa; ikon menu tampil; Properti multi-item; Back saat mode Pilih hanya menutup mode Pilih
- [ ] LANGKAH BERIKUT: sortir/pencarian nama di Penjelajah, buka berkas dengan aplikasi lain, kirim ke Script execution dari Penjelajah, tampilan grid

## 2026-10-26 - Penjelajah: pencarian, sortir, buka dengan, kirim ke Script execution [EXPLORER-SEARCH-OPEN]
- [x] feat(explorer): kotak "Cari nama" di baris alamat (semua kata harus ada di nama, tanpa beda huruf besar/kecil); centang "Cari sampai subfolder" muncul saat mengetik (BFS maks 500 hasil, 20.000 entri, kedalaman 12, tautan tidak diikuti, dibatalkan otomatis saat kueri berubah); hasil subfolder menampilkan jalur relatif; mengetuk folder hasil membuka folder dan mengosongkan pencarian
- [x] feat(explorer): menu "Urut": Nama, Tanggal, Ukuran, Jenis, Menaik/Menurun, Folder di atas, Tampilkan berkas tersembunyi; semua diingat (SharedPreferences molina_explorer); bilah alat kini bisa digeser mendatar
- [x] feat(explorer): menu Alat > Putar di mpv (berkas, banyak berkas jadi playlist memory://, atau folder), Buka di Code (maks 10 berkas teks, tab Code aktif otomatis), Kirim ke Script execution (sumber = path berkas, sama dengan kiriman editor sehingga tidak menggandakan); ketuk ulang berkas terpilih = buka cerdas (media ke mpv, teks ke Code)
- [x] refactor(explorer): logika daftar/pencarian/sortir dipindah ke ExplorerList.kt (JVM murni, diuji kotlinc di luar CI); W7Sort diganti ExplorerList.Key
- [x] Dampak: :app saja (cinema/ExplorerList.kt, cinema/ExplorerPrefs.kt, cinema/Win7OpenDialog.kt, settings/ExplorerBridge.kt, settings/SettingsScreen.kt, code/ScriptRunner.kt CodeNav, MainActivity.kt). Gradle, Manifest, JNI mpv tidak disentuh; "Aplikasi lain" lewat chooser sistem BELUM ada (butuh FileProvider + Manifest)
- [ ] BELUM dikompilasi (CI). Risiko: sedang (suspend lambda sebagai parameter dialog, Popup, horizontalScroll)
- [ ] uji perangkat: ketik di kotak cari (hasil menyaring, "x" menghapus, pesan "Tidak ada hasil"); centang subfolder (hasil dengan jalur relatif, folder hasil bisa dibuka); Urut: tiap kunci + menurun + folder di atas + tersembunyi, tutup-buka Penjelajah (setelan bertahan); pilih video > Putar di mpv; pilih folder > Putar folder; pilih 3 video (mode Pilih) > Putar; pilih .md/.sh > Buka di Code (pindah ke tab Code, berkas terbuka); berkas biner ditolak; pilih .sh > Kirim ke Script execution lalu cek Settings > Script execution (kirim ulang = diperbarui, bukan dobel); ketuk ulang video/teks = buka cerdas
- [ ] LANGKAH BERIKUT: Aplikasi lain lewat chooser sistem (FileProvider), tampilan grid/thumbnail, Bagikan, riwayat folder (maju/mundur), buka ZIP tanpa ekstrak

## 2026-10-26 - Perbaikan build CI: showExplorer [FIX-SHOWEXPLORER]
- [x] fix(settings): `Unresolved reference 'showExplorer'` di SettingsScreen.kt:140; deklarasi `var showExplorer` dipindah ke sebelum `LaunchedEffect(page)` yang memakainya (urutan deklarasi variabel lokal Kotlin)
- [x] Dampak: :app saja (settings/SettingsScreen.kt, 1 baris dipindah). Gradle, Manifest, JNI mpv tidak disentuh. Perilaku tidak berubah
- [ ] BELUM dikompilasi (CI). Risiko: rendah. Bila CI memunculkan error lain di berkas Explorer, kirim lognya

## 2026-10-26 - Penjelajah: keyboard tidak muncul sendiri + ikon emas Molina [EXPLORER-KB-GOLD]
- [x] fix(explorer): keyboard masih muncul otomatis karena fokus awal dialog mendarat di kolom teks pertama setelah clearFocus; ditambah modifier tapToFocus() (focusProperties canFocus baru true setelah sentuhan pengguna, dikunci lagi saat fokus lepas) pada kotak Cari nama dan kolom Nama file
- [x] feat(ui): ikon Penjelajah beraksen emas MolinaTheme.Gold di latar gelap: Win7Folder (gradasi emas), Win7MolinaFolder (emas terang + bintang), Win7File (halaman gelap, tepi emas, lencana putar untuk video dan not untuk audio); konsisten dengan bar unduhan molina_dl
- [x] Dampak: :app saja (cinema/Win7OpenDialog.kt, cinema/Win7Aero.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah-sedang (Modifier.composed, awaitEachGesture, import baru)
- [ ] uji perangkat: Settings > Berkas > keyboard TIDAK muncul; ketuk Cari nama > keyboard muncul dan bisa mengetik; ketuk di luar/Back > keyboard tutup; ketuk lagi bisa fokus; Nama file (mode simpan) sama; ikon folder, folder Molina (bintang), video, dan audio tampil emas dan terbaca di baris 20dp
- [ ] LANGKAH BERIKUT: Aplikasi lain lewat chooser sistem (FileProvider), tampilan grid/thumbnail, Bagikan, riwayat folder

## 2026-10-26 - Penjelajah: Jalankan/Sisipkan, ZIP AES-256, SHA-256 [EXPLORER-RUN-ZIPAES]
- [x] refactor(explorer): tombol "Ke Terminal" + kotak centang Enter dihapus (duplikat cd); diganti "Jalankan" (berkas: interpreter dari shebang/ekstensi, subshell (cd folder && interpreter berkas), folder kerja sesi tidak berubah) dan "Sisipkan (N)" (folder/seleksi banyak: path ter-quote tanpa Enter)
- [x] feat(explorer): Kompres ZIP + kata sandi (AES-256, zip4j) dengan dialog dua kolom; Ekstrak ZIP mendeteksi enkripsi dan meminta sandi, sandi salah meminta ulang, hasil parsial dibersihkan; progres + Hentikan tetap bekerja
- [x] feat(explorer): Properti satu berkas: Hitung SHA-256 (bertahap, berhenti saat dialog ditutup) + Salin hash
- [x] Uji JVM murni (kotlinc 2.1.20 + zip4j 2.11.5): ZIP AES round-trip, NeedPassword/WrongPassword, tanpa sisa folder, folder kosong, symlink dilewati, ZIP biasa tidak regresi, banyak item, pembatalan, SHA-256 = sha256sum, runnerFor (shebang env/ekstensi/nama berbahaya ditolak). telemetry_gate lulus
- [x] Dampak: Gradle (+net.lingala.zip4j:zip4j:2.11.5), :app (cinema/FileOps.kt, cinema/Win7OpenDialog.kt, settings/ExplorerBridge.kt, settings/SettingsScreen.kt). Manifest dan JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (dependensi baru, Compose dialog baru, parameter onFail pada runOp). Parameter sendEnter/onSendEnterChange/onSendToTerminal kini tak terpakai (hanya peringatan)
- [ ] uji perangkat: pilih berkas .sh > Jalankan (hasil muncul di Terminal, cwd sesi tidak berubah); .py > Jalankan; .txt > pesan "belum dikenali"; pilih folder > Sisipkan; mode Pilih 3 berkas > Sisipkan (3); Alat > Kompres ZIP + kata sandi (kosong/beda sandi ditolak) > ekstrak tanpa sandi diminta sandi > salah minta ulang > benar berhasil; buka ZIP terenkripsi di aplikasi lain (7-Zip/ZArchiver) dengan sandi sama; Properti berkas besar > Hitung SHA-256 > Salin hash; tutup dialog saat menghitung tidak crash
- [ ] LANGKAH BERIKUT: ekstrak 7z/tar.gz (commons-compress sudah ada, 7z butuh org.tukaani:xz), jelajah isi ZIP tanpa ekstrak, Bagikan (FileProvider), riwayat folder

## 2026-10-27 - Penjelajah: Buka Terminal, Bagikan [EXPLORER-OPENTERM-SHARE]
- [x] refactor(explorer): tombol "Jalankan" dan "cd" diganti "Buka Terminal": membuat sesi Debian baru dengan cwd di folder tujuan (folder induk bila berkas) lalu langsung pindah ke tab Terminal. Batas 5 sesi penuh: fallback cd di sesi aktif. Pengecekan "terlihat di Debian" memakai setelan sesi BARU
- [x] feat(explorer): menu Alat > Bagikan (berkas, FileProvider, share sheet Android); folder dilewati
- [x] cleanup: hapus runFile, sendToTerminal, state/parameter sendEnter, onChangeDir, onRunInTerminal (FileOps.runnerFor dibiarkan, tidak dipakai)
- [x] Dampak: AndroidManifest (+FileProvider ${applicationId}.fileprovider), res/xml/molina_file_paths.xml, TerminalSessions (newSession/debianShell + cwd), ExplorerBridge, Win7OpenDialog, SettingsScreen. JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (Manifest provider baru, parameter composable berubah)
- [ ] uji perangkat: pilih folder > Buka Terminal (prompt di folder itu, tab pindah, sesi lama utuh); pilih berkas > Buka Terminal (folder induk); folder di luar Debian > toast penjelasan; 5 sesi penuh > cd di sesi aktif; Alat > Bagikan 1 berkas ke aplikasi lain; Bagikan 3 berkas; pilih folder > Bagikan nonaktif; berkas di kartu SD > toast "di luar penyimpanan internal"
- [ ] LANGKAH BERIKUT: ekstrak 7z/tar.gz, jelajah isi ZIP, riwayat folder

## 2026-10-27 - Penjelajah: aksi cepat, ikon >_, nama sesi [EXPLORER-QUICKACT-LABEL]
- [x] feat(explorer): tombol bawah kini Aksi cepat kontekstual | Sisipkan | >_ Terminal. Aksi cepat: media = Putar (mpv), berkas teks = Edit (Code), selain itu = Properti. "Salin path" pindah ke menu Alat
- [x] feat(ui): Win7Button punya parameter accent; tombol Terminal berikon >_ hijau (senada tab Terminal)
- [x] feat(term): sesi yang dibuka dari Penjelajah bernama folder tujuan di drawer sesi ("[3] molina-studio"); sesi biasa tetap memakai judul shell atau "Debian"
- [x] Dampak: Win7Aero.kt, Win7OpenDialog.kt, TerminalSessions.kt, SessionDrawer.kt, ExplorerBridge.kt. Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (parameter composable baru, referensi `sheet`/`scope` di baris tombol)
- [ ] uji perangkat: pilih video > tombol "Putar"; pilih .sh/.txt > "Edit" membuka Code; pilih folder atau berkas lain > "Properti"; Alat > Salin path; tombol ">_ Terminal" tidak terpotong; Terminal dari folder > drawer menampilkan nama folder; "Sisipkan (3)" pada seleksi banyak masih muat
- [ ] LANGKAH BERIKUT: riwayat folder (5 terakhir) di Favorit

## 2026-10-27 - Penjelajah: mode Pilih bisa membuka folder [EXPLORER-PICK-FOLDER]
- [x] fix(explorer): di mode Pilih, folder bisa dicentang (sudah) tetapi tidak bisa dibuka karena ketukan hanya mencentang. Kini baris folder punya tombol "›" di kanan untuk masuk folder; ketuk bagian lain tetap mencentang. Petunjuk singkat muncul saat mode Pilih diaktifkan
- [x] Dampak: hanya cinema/Win7OpenDialog.kt (parameter onEnter pada W7FileArea dan W7EntryRow). Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah
- [ ] uji perangkat: Pilih > centang folder + berkas > Salin/Pindah/Hapus/Kompres/Sisipkan; ketuk "›" di folder membuka folder (centang direset, Salin/Pindah yang sudah di clipboard tetap bisa Tempel di folder tujuan); Semua mencentang folder juga; ketuk ">" tidak ikut mencentang barisnya
- [ ] LANGKAH BERIKUT: riwayat folder (5 terakhir) di Favorit

## Backlog Enhancement (2026-10-27)
- [ ] Penjelajah: seleksi tetap, pratinjau cepat, tempat sampah 7 hari (riwayat folder dibatalkan)
- [ ] Penjelajah: jalan pintas folder Debian, Buka Terminal di sini, Putar dengan Molina Player
- [ ] Settings: Pulihkan ke bawaan, cadangan JSON (tanpa kredensial), profil Terminal, halaman Penyimpanan
- [ ] Cinema: lanjutkan menonton, memori subtitle/audio; Unduhan: antrean + retry
- [ ] About: versi + commit; ekspor log

## 2026-10-27 - Penjelajah: seleksi tetap lintas folder [EXPLORER-PICK-PERSIST]
- [x] feat(explorer): centang mode Pilih tidak direset saat pindah folder; "Semua" hanya untuk folder yang dibuka; tombol "Kosongkan"
- [x] fix(explorer): zipAll menolak nama tingkat atas kembar dengan pesan jelas
- [x] Keputusan: riwayat folder/folder terakhir DIBATALKAN
- [x] Dampak: :app saja (cinema/Win7OpenDialog.kt, cinema/FileOps.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah
- [ ] uji perangkat: centang 2 berkas di folder A > buka folder B (›) > centang 1 > "Selesai (3)" > Salin > Tempel di folder C; "Semua" di B tidak menghapus centang A; "Kosongkan"; Kompres 2 berkas bernama sama dari folder berbeda > pesan bentrok; Back saat Pilih menutup mode Pilih
- [ ] LANGKAH BERIKUT: halaman native Unduhan (RunRequest.args, DownloadRunner, foreground service, UnduhanScreen); lalu halaman Penyimpanan; ekstrak 7z/tar.gz

## 2026-10-27 - Unduhan native langkah 1: argumen skrip + env di jalur PRoot [DL-STEP1-ARGS]
- [x] feat(code): RunRequest punya `args: List<String>` dan `env: Map<String,String>` (bernilai bawaan kosong; RunController dan ToolRunUi tidak berubah). Argumen diteruskan tanpa shell sehingga harfiah; tidak pernah masuk log
- [x] refactor(code): ScriptCommand dipindah ke ScriptCommand.kt (murni) + `validate()`: tolak NUL, argumen >4096 karakter, >256 argumen, nama env tidak valid/PATH/HOME/LANG/TERM/IFS/SHELL/LD_*/PROOT*; ProotScriptExecutor memvalidasi di awal runSync (gagal = startError, skrip tidak jalan)
- [x] test: scripts/code-tests/run_script_command.sh (kotlinc 2.1.20): bentuk argv tanpa args/env identik dengan sebelumnya (golden), args harfiah berurutan, env sebelum interpreter, semua penolakan validasi
- [x] Dampak: :feature:code (run/ScriptExecutor.kt), :app (code/ScriptCommand.kt baru, code/ProotScriptExecutor.kt). Gradle, Manifest, JNI mpv tidak disentuh. Editor Code dan ToolRunUi memakai jalur yang sama: perilaku bawaan tidak berubah
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (jalur exec PRoot dipakai bersama editor Code)
- [ ] uji perangkat: Code > jalankan .sh dan .py seperti biasa (tanpa regresi); Settings > tool run (setup) masih jalan; Dibatalkan/Timeout tetap berfungsi
- [ ] LANGKAH BERIKUT: langkah 2 DownloadRunner (state flow dari --json, naikkan batas keluaran), lalu foreground service dan UnduhanScreen

## 2026-10-27 - Unduhan native langkah 2: DownloadRunner (state flow dari --json) [DL-STEP2-RUNNER]
- [x] feat(core): DownloadCommand (DownloadRequest -> argv `--json --mode ... -- URL...`, validasi: http/https saja, maks 50 URL, tanpa karakter kontrol, opsi dibatasi, folder harus absolut) dan DownloadFeed (potongan stdout/stderr -> DownloadRunState; buffer terpisah per aliran, baris >64K dibuang, stderr disamarkan LogRedact maks 20 baris, sesi yang berhenti tanpa summary dituntaskan: dibatalkan/mati/gagal mulai)
- [x] feat(code): RunRequest.maxOutputChars (bawaan 1.000.000 = perilaku lama); ProotScriptExecutor punya jalur (Lane) CODE dan DOWNLOAD: unduhan panjang tidak memblokir editor Code/Settings, dan Penyegaran sistem (isRunning/cancelActive tanpa argumen = CODE) tidak membatalkan unduhan
- [x] feat(download): DownloadRunner (:app) memakai skrip bawaan assets (bukan salinan Scripts/ yang bisa diedit), StateFlow<DownloadRunState>, start/cancel/clear; URL tidak pernah masuk log
- [x] test: scripts/core-tests/run_feed.sh (kotlinc 2.1.20): 36 pemeriksaan termasuk keluaran nyata molina_dl.py (dipecah 1 karakter, CRLF), alur lengkap, pembatalan, proses mati, penolakan input
- [x] Dampak: :core:common (DownloadCommand.kt, DownloadFeed.kt), :feature:code (run/ScriptExecutor.kt), :app (code/ProotScriptExecutor.kt, download/DownloadRunner.kt baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (ProotScriptExecutor memakai peta lane, jalur yang sama dipakai editor Code)
- [ ] uji perangkat: Code > jalankan .sh/.py seperti biasa; Penyegaran sistem tetap membatalkan skrip Code; (setelah UI ada) unduhan berjalan sambil menjalankan skrip Code; pembatalan menghentikan proses yt-dlp/ffmpeg
- [ ] LANGKAH BERIKUT: langkah 3 foreground service (dataSync, notifikasi overallPercent, Manifest berubah), lalu langkah 4 UnduhanScreen

## 2026-10-27 - Unduhan native langkah 3: DownloadService foreground (notifikasi persen) [DL-STEP3-SERVICE]
- [x] CI #167 (langkah 2: DownloadRunner/DownloadFeed/jalur DOWNLOAD) hijau
- [x] feat(download): DownloadService (foreground, dataSync): mengamati DownloadRunner.state, notifikasi persen gabungan + "n/total selesai" + aksi Batalkan, notifikasi ringkasan saat selesai (tanpa URL), wake lock berbatas waktu, berhenti sendiri (stopSelf(startId) aman terhadap unduhan beruntun)
- [x] DownloadRunner.start() menyalakan service setelah sesi berjalan; start dari latar (Android 12+) ditolak sistem dibungkus runCatching: unduhan tetap jalan tanpa notifikasi
- [x] Manifest: service .download.DownloadService (foregroundServiceType=dataSync) + izin FOREGROUND_SERVICE_DATA_SYNC (tidak wajib di targetSdk 28, disiapkan untuk kenaikan target)
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (Manifest berubah). JNI mpv tidak disentuh
- [ ] uji perangkat (Android 8): setelah UI ada, mulai unduhan lalu tekan Home: notifikasi muncul dan persen bergerak; "Batalkan" menghentikan unduhan; notifikasi ringkasan muncul saat selesai
- [ ] LANGKAH BERIKUT: langkah 4 UnduhanScreen (Compose, daftar item, bar emas, tombol mulai/batalkan, hasil)

## 2026-10-27 - Unduhan native langkah 4: tab Unduhan (UnduhanScreen) [DL-STEP4-SCREEN]
- [x] feat(download): UnduhanScreen (Compose) sebagai tab baru "Unduhan" (setelah mpv): kolom URL multi-baris, tombol Tempel dari clipboard, pilihan Video (Terbaik/1080p/720p/480p, MP4/MKV) atau Audio (MP3/M4A/FLAC, 128k/192k/320k), Mulai/Batalkan, Kosongkan hasil
- [x] Progres per item: bar emas (hijau saat selesai, merah saat gagal), kecepatan dan sisa waktu, tahap pemrosesan, pesan galat per item, peringatan dan galat global
- [x] Setelah selesai: tombol Putar per berkas (CinemaActions.playPath, path Debian /sdcard dipetakan ke Android) dan Buka folder (Penjelajah terbuka di folder hasil lewat parameter baru Win7OpenDialog.startDir)
- [x] Hasil sesi terakhir tetap tampil sampai Kosongkan (DownloadRunner.clear()); unduhan baru menggantikannya
- [x] Dampak: :app (download/UnduhanScreen.kt baru, cinema/Win7OpenDialog.kt +parameter startDir, MainActivity.kt +tab). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (nav bawah kini 5 tab; Win7OpenDialog dipakai Settings dan Cinema, parameter baru berbawaan null)
- [ ] uji perangkat (Android 8): tempel URL, mulai, tekan Home (notifikasi persen), batalkan, lanjutkan dengan URL sama; setelah selesai: Putar membuka Molina Player, Buka folder membuka Penjelajah di folder hasil; Settings > Penjelajah dan tab mpv tetap normal
- [ ] LANGKAH BERIKUT (opsional): tombol "Unduh" dari kartu hasil pencarian Cinema ke tab Unduhan, uji unit untuk pemetaan path dan format ukuran

## 2026-10-27 - Unduhan native langkah 5: integrasi pencarian Cinema -> Unduhan + UX + uji [DL-STEP5-INTEGRASI]
- [x] feat(cinema): kartu hasil pencarian punya tombol bulat emas "↓" (kirim satu URL ke tab Unduhan) dan tombol bar bawah "Unduh semua" (semua hasil yang tampil, termasuk hasil saringan; maks 50). Ketuk baris tetap memutar
- [x] feat(download): DownloadNav (jembatan URL ke tab Unduhan; MainActivity pindah tab, UnduhanScreen menggabungkan ke kolom, duplikat dilewati, batas 50 URL, Toast ringkasan). Tidak langsung mengunduh: mode/kualitas dipilih dulu
- [x] fix(ux): formulir Unduhan (URL, mode, kualitas) disimpan di DownloadForm, tidak hilang saat pindah tab (sebelumnya rememberSaveable ikut hilang karena tab hanya dikomposisi saat dipilih)
- [x] ux: tombol "Mulai unduh (n)" menampilkan jumlah URL dan nonaktif saat kosong
- [x] refactor(core): DownloadUi.kt (DownloadFormat.size/eta, DownloadPaths.toAndroid menolak path di luar /sdcard, segmen .. dan karakter kontrol, DownloadUrls.split/merge); UnduhanScreen memakainya
- [x] test: scripts/core-tests/run_ui.sh (kotlinc 2.1.20): 27 pemeriksaan LULUS (format ukuran/ETA termasuk locale id, pemetaan path, traversal, penggabungan URL dan batas 50)
- [x] Dampak: :core:common (DownloadUi.kt), :app (download/DownloadNav.kt baru, UnduhanScreen.kt, cinema/CinemaResults.kt, MainActivity.kt, res/drawable/ic_tv_download.xml). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (CinemaResults: tanda tangan ChannelRow berubah, hanya dipakai di berkas itu)
- [ ] uji perangkat (Android 8): cari judul di tab mpv > ketuk ↓ pada satu kartu: pindah ke tab Unduhan dengan URL terisi; ulangi dengan kartu lain (URL bertambah, tidak ganda); "Unduh semua" dengan saringan; pindah ke tab lain lalu kembali: isi formulir masih ada; ketuk baris (bukan ↓) tetap memutar di Player
- [ ] LANGKAH BERIKUT (opsional): ingat pilihan mode/kualitas terakhir di SharedPreferences, riwayat unduhan, ketuk notifikasi selesai langsung membuka tab Unduhan

## 2026-10-27 - Unduhan langkah 6: ingat pilihan, notifikasi buka tab Unduhan, lencana tab [DL-STEP6-PREFS-NOTIF]
- [x] feat(download): DownloadPrefs (SharedPreferences molina_download) mengingat mode/tinggi/format video/format audio/bitrate terakhir; disimpan saat unduhan dimulai, dipulihkan sekali per proses di MainActivity.onCreate. URL tidak disimpan
- [x] feat(core): DownloadChoices.sanitize (murni): nilai tidak valid/prefs rusak jatuh ke bawaan per bidang
- [x] feat(download): ketuk notifikasi Unduhan (berjalan maupun selesai) membuka tab Unduhan lewat extra EXTRA_OPEN_TAB; ditangani di onCreate (hanya saat savedInstanceState null) dan onNewIntent; extra dibuang setelah dipakai
- [x] feat(ui): lencana pada tab Unduhan: emas saat unduhan berjalan, hijau saat selesai dan belum dilihat (hilang saat tab dibuka)
- [x] test: scripts/core-tests/run_ui.sh ditambah 6 pemeriksaan DownloadChoices
- [x] Keputusan: launchMode Manifest TIDAK diubah (Intent notifikasi sudah FLAG_ACTIVITY_SINGLE_TOP, onNewIntent terpanggil)
- [x] Dampak: :core:common (DownloadUi.kt), :app (download/DownloadPrefs.kt baru, DownloadNav.kt, UnduhanScreen.kt, DownloadService.kt, MainActivity.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (MainActivity: onNewIntent + BadgedBox/Badge Material3; jika signature onNewIntent beda dengan versi androidx.activity, CI akan menolak)
- [ ] uji perangkat (Android 8): pilih Audio/FLAC/320k > Mulai > tutup aplikasi > buka lagi: pilihan tetap; saat berjalan, buka tab lain: lencana emas di tab Unduhan; Home > ketuk notifikasi (berjalan) membuka tab Unduhan; setelah selesai di tab lain: lencana hijau, ketuk notifikasi "Unduhan selesai" membuka tab Unduhan (aplikasi hidup dan aplikasi sudah dimatikan); lencana hilang saat tab dibuka; rotasi layar tidak memindahkan tab lagi
- [ ] LANGKAH BERIKUT (opsional): riwayat unduhan (daftar sesi lama), tombol "Ulangi" untuk item gagal

## 2026-10-27 - Unduhan langkah 7: riwayat sesi + tombol Ulangi [DL-STEP7-HISTORY-RETRY]
- [x] feat(core): DownloadHistory (murni): susun entri dari permintaan + keadaan akhir sesi (satu item per URL, termasuk yang tak sempat mulai), push maks 20 entri, serialisasi JSON versi 1 yang tahan data rusak/versi asing
- [x] feat(download): DownloadHistoryStore (files/download-history.json, atomik via berkas sementara, I/O satu thread latar, best-effort). Privasi: URL penuh HANYA untuk item belum berhasil (dipakai Ulangi); item berhasil hanya skema+host; pesan galat disamarkan LogRedact; path berkas tidak disimpan
- [x] feat(download): DownloadRunner mengingat permintaan terakhir (lastReq): retryUrls()/retry(only?) memulai sesi baru dengan mode/kualitas SAMA; "Kosongkan hasil" ikut mengosongkan lastReq (tidak ada Ulangi menyasar item yang sudah berhasil)
- [x] feat(ui): kartu item gagal punya "Ulangi"; tombol "Ulangi yang gagal (n)" untuk sesi yang tampil; bagian "Riwayat (n)" (tutup secara bawaan): kartu sesi (tanggal, mode, berhasil/gagal/dibatalkan), ketuk untuk rincian, Ulangi dengan mode/kualitas ASLI sesi itu, Hapus satu, Hapus semua (dengan konfirmasi). Sesi terbaru tidak digandakan saat hasilnya masih tampil. Ulangi menggulir ke atas dan Toast bila ditolak (mis. masih ada unduhan berjalan)
- [x] test: scripts/core-tests/run_history.sh (kotlinc 2.1.20): 24 pemeriksaan LULUS (build, penyamaran URL/galat, retry, label, push/batas, JSON bolak-balik dengan karakter khusus, data rusak)
- [x] Dampak: :core:common (DownloadHistory.kt baru), :app (download/DownloadHistoryStore.kt + DownloadHistoryUi.kt baru; DownloadRunner.kt, UnduhanScreen.kt, MainActivity.kt satu baris). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (UnduhanScreen: LazyColumn kini punya state, ItemCard tanda tangan bertambah parameter berbawaan; AlertDialog Material3; DownloadRunner: jalur onFinished kini mencatat riwayat)
- [ ] uji perangkat (Android 8): unduh 3 URL dengan 1 URL salah > item gagal punya "Ulangi" > ketuk: hanya item itu diulang dengan mode/kualitas sama dan layar menggulir ke atas; "Ulangi yang gagal (n)"; batalkan sesi di tengah > Ulangi mengunduh sisanya (item tak sempat mulai ikut); tutup paksa aplikasi > buka lagi > Riwayat tetap ada; buka Riwayat > ketuk kartu > rincian; Ulangi dari kartu lama memakai mode/kualitas kartu itu (bukan pilihan formulir saat ini); Ulangi saat unduhan berjalan: tombol nonaktif; Hapus satu dan Hapus semua (konfirmasi); "Kosongkan hasil" tidak menghapus riwayat dan tidak menampilkan Ulangi untuk sesi lama
- [ ] LANGKAH BERIKUT (opsional): tombol Putar pada item berhasil di riwayat (cek berkas masih ada), salin URL item gagal, pelacakan antrean multi-sesi

## 2026-10-27 - Fix CI: telemetry gate menolak FOREGROUND_SERVICE_DATA_SYNC [FIX-GATE-DATASYNC]
- [x] fix(ci): scripts/telemetry_gate.py: izin android.permission.FOREGROUND_SERVICE_DATA_SYNC masuk ALLOWED_PERMISSIONS (dipakai DownloadService, izin normal, bukan telemetri). Penyebab: commit 58c503a menambah izin di Manifest tanpa memperbarui allowlist
- [x] Verifikasi lokal: python3 scripts/telemetry_gate.py lulus (exit 0)
- [x] Dampak: hanya scripts/telemetry_gate.py. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] Menunggu GitHub Actions hijau

## 2026-10-27 - Unduhan pindah ke Settings, gaya Win7 hitam pekat [UI-DL-IN-SETTINGS]
- [x] refactor(ui): tab Unduhan dihapus dari bilah bawah; Settings beranda mendapat ubin "Unduhan" (Page.Downloads) sejajar Sistem/Script execution/Paket/Berkas, dan masuk indeks pencarian
- [x] feat(ui): UnduhanScreen + DownloadHistoryUi memakai palet W7D (hitam pekat, aksen biru Aero W7D.Accent); judul/garis emas dibuang (breadcrumb Settings sudah menjadi judul)
- [x] refactor(nav): DownloadNav.visible baru; kartu Cinema dan notifikasi Unduhan membuka Settings > Unduhan lewat SettingsNav.openDownloads; lencana pindah ke tab Settings
- [x] Dampak: :app (MainActivity, SettingsScreen, Win7SettingsUi, DownloadNav, UnduhanScreen, DownloadHistoryUi, DownloadTileIcon.kt baru). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (when(page) Settings kini punya Page.Downloads; LazyColumn Unduhan dipasang langsung tanpa verticalScroll induk)
- [ ] uji perangkat (Android 8): bilah bawah 4 tab; Settings > Unduhan terbuka dan tombol kembali ke beranda; ketuk ↓ di kartu Cinema membuka Settings > Unduhan dengan URL terisi; mulai unduhan lalu pindah tab: lencana emas di Settings, hijau saat selesai; ketuk notifikasi membuka Settings > Unduhan; cari "unduh" di kotak cari

## 2026-10-27 - Peta jalan tahap berikutnya (belum dikerjakan) [ROADMAP-NEXT]
- [ ] A: Recycle Bin (.molina-trash + manifest JSON, pulihkan/kosongkan/auto-purge 30 hari) + Urungkan + cari/urut/terbaru di Berkas
- [ ] B: Settings > Penyimpanan dan Folder Molina (lokasi default unduhan/konversi/trash)
- [ ] C: Konverter di bawah Unduhan (FFmpeg via Debian PRoot, preset, antrean, Unduh > Konversi > Putar)
- [ ] D: Cadangkan/Pulihkan setelan (ZIP AES), E: Kesehatan sistem
- [ ] Semua bergaya Win7 hitam pekat (W7D); tanpa izin/host baru

## 2026-10-27 - Tahap A: Recycle Bin + Urungkan di Berkas [RECYCLE-BIN-A]
- [x] feat(core): TrashBin.kt (:core:common, JVM murni): .molina-trash per volume (rename atomik, info.properties UTF-8, .nomedia), trash semua-atau-tidak, list/restore (nama bentrok " (1)", folder asal dibuat ulang)/purge/empty/autoPurge 30 hari/usage; tahan data rusak, nama "..", origin palsu, item di luar bin
- [x] test: scripts/core-tests/run_trash.sh (kotlinc 2.1.20): 28 pemeriksaan LULUS
- [x] feat(ui): Win7RecycleBin.kt (ikon + jendela Recycle Bin); Win7OpenDialog: Hapus -> Recycle Bin, "Hapus permanen...", tombol Urungkan, item "Sampah" di panel kiri, auto-purge saat Berkas dibuka, .molina-trash masuk guard; W7Panel jadi internal
- [x] Dampak: :core:common (TrashBin.kt baru), :app (cinema/Win7RecycleBin.kt baru, Win7OpenDialog.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (named arg + trailing lambda pada W7NavPane; runOp dengan onDone)
- [ ] uji perangkat (Android 8): hapus lalu Urungkan; folder bersarang; nama bentrok saat pulih; Hapus permanen; Kosongkan; kartu SD; folder Molina tetap tertolak
- [ ] LANGKAH BERIKUT: daftar "Terbaru" di Berkas, lalu tahap B (Penyimpanan + Folder Molina, termasuk pemakaian Recycle Bin)

## 2026-10-27 - Penjelajah: buka ZIP + diagnosis kegagalan [ZIP-VIEW]
- [x] fix(explorer): mengetuk berkas .zip (ketuk ulang atau tombol aksi cepat) dulu hanya menampilkan "Tidak ada aplikasi Molina untuk membuka jenis berkas ini" (smartOpen tidak mengenal ZIP) dan aksi cepat jatuh ke "Properti"; kini membuka dialog isi ZIP (daftar entri, jumlah, total ukuran, penanda AES) dengan tombol Ekstrak
- [x] feat(explorer): FileOps.listZip (zip4j, cadangan java.util.zip) membaca isi tanpa mengekstrak, termasuk ZIP berkata sandi; ZIP 0 byte/rusak diberi pesan jelas
- [x] fix(explorer): pesan error kini menyebut jenis exception dan ZIP rusak/tak didukung (dulu "Gagal mengekstrak"/"Operasi gagal." tanpa sebab); kegagalan operasi dicatat ke Settings > Log (sumber "penjelajah", tag FileOps) lengkap dengan 8 baris tumpukan
- [x] Dampak: :app saja (cinema/FileOps.kt, cinema/Win7OpenDialog.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah-sedang (cabang `when`/sheet baru, dialog Compose)
- [ ] uji perangkat: ketuk berkas .zip (pilih lalu ketuk lagi) > dialog isi ZIP muncul > Ekstrak > folder hasil muncul; ZIP AES > penanda AES > Ekstrak meminta kata sandi; ZIP salah/rusak > pesan jelas; bila ada kegagalan, salin baris dari Settings > Log (sumber penjelajah)
- [ ] CATATAN: ZIP berkata sandi memakai AES-256 (WinZip); banyak aplikasi bawaan Android/pengelola berkas TIDAK bisa membukanya (butuh 7-Zip, ZArchiver, WinZip). ZIP biasa (tanpa sandi) kompatibel di mana saja
- [ ] LANGKAH BERIKUT: ekstrak sebagian (pilih entri), ekstrak ke folder lain, uji perangkat ZIP besar (progres)

## 2026-10-27 - Penjelajah: riwayat folder dan beranda Debian [EXPLORER-RECENT-DEBIAN]
- [x] feat(explorer): Penjelajah dibuka di folder terakhir (startDir dari tab Unduhan tetap diutamakan); panel kiri punya bagian "Terakhir" (5 folder terakhir, tanpa duplikat favorit/akar)
- [x] feat(explorer): akar "Debian" (rootfs/root) di daftar Komputer: kelola berkas Debian dari Penjelajah; path dipetakan ke path Debian (/root/...), sehingga Salin path, Sisipkan, dan Buka Terminal memakai path yang benar. Akar ini tidak bisa dinaiki ke atas, terlindung dari hapus/ubah nama, dan punya .molina-trash sendiri (Recycle Bin per volume)
- [x] Dampak: cinema/ExplorerRecent.kt (baru), cinema/Win7OpenDialog.kt, settings/ExplorerBridge.kt, settings/SettingsScreen.kt. Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (parameter baru W7NavPane, import RootfsManager)
- [ ] uji perangkat: buka Berkas, masuk folder, tutup, buka lagi (kembali ke folder terakhir); bagian Terakhir muncul dan bekerja; tombol "Buka folder di Penjelajah" dari Unduhan tetap membuka folder unduhan; akar Debian terlihat, Naik tidak keluar dari /root; di Debian > Buka Terminal (prompt di /root/...) dan Salin path (mulai /root); hapus berkas di Debian > Recycle Bin > Urungkan

## 2026-10-27 - Settings: halaman Penyimpanan [SETTINGS-STORAGE]
- [x] feat(settings): Sistem > Penyimpanan: sisa ruang internal, ukuran rootfs Debian, cache apt, cache aplikasi, folder kerja molina-studio, Recycle Bin (internal, kartu SD, Debian); Hitung ulang
- [x] feat(settings): Bersihkan cache apt (hapus .deb unduhan dan partial, berkas lock dibiarkan), Bersihkan cache aplikasi (proot-tmp dilewati karena dipakai sesi Terminal), dan Kosongkan Recycle Bin dengan konfirmasi (TrashBin.usage/empty). Symlink tidak diikuti saat menghitung dan menghapus
- [x] Dampak: settings/StorageUi.kt (baru), settings/SettingsScreen.kt (enum Page, titleOf, indeks pencarian, tautan Sistem). Manifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (ArrayDeque.removeLast, sumOf pada Long, TrashBin dari :core:common)
- [ ] uji perangkat: Settings > Sistem > Penyimpanan: angka muncul tanpa macet UI; apt install paket lalu Bersihkan cache apt (angka turun, paket tetap jalan); Bersihkan cache aplikasi; hapus berkas di Berkas lalu Kosongkan Recycle Bin (angka Recycle Bin menjadi 0); sesi Terminal tetap normal; pencarian "penyimpanan" menemukan halaman
- [ ] CATATAN: tahap B peta jalan (Penyimpanan dan Folder Molina) tumpang tindih dengan halaman ini; sisa B = lokasi default unduhan/konversi/trash
- [ ] LANGKAH BERIKUT: cadangkan/pulihkan setelan, profil Terminal

## 2026-10-27 - Penjelajah: Buka dengan aplikasi lain, riwayat Mundur/Maju [EXPLORER-OPENWITH-HISTORY]
- [x] feat(explorer): berkas jenis tak dikenal (PDF, gambar, APK, dokumen) yang diketuk ulang kini dibuka lewat chooser sistem (ACTION_VIEW + FileProvider yang sudah ada), bukan lagi pesan "Tidak ada aplikasi Molina"; menu Alat > "Buka dengan..." untuk satu berkas apa pun. ZIP, media (mpv), dan teks (Code) tetap memakai jalur Molina
- [x] feat(explorer): FileProvider kini juga mencakup beranda Debian (files/rootfs/debian/root) sehingga Bagikan dan Buka dengan bekerja untuk berkas di akar "Debian"
- [x] feat(explorer): riwayat Mundur/Maju (maks. 50): tombol bulat kiri atas = Mundur bila ada riwayat, jika tidak Naik; perintah "Maju" di bilah alat; folder yang sudah hilang dilewati
- [x] Dampak: cinema/Win7OpenDialog.kt, settings/ExplorerBridge.kt, settings/SettingsScreen.kt, res/xml/molina_file_paths.xml. AndroidManifest, Gradle, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (when baru di smartOpen, fungsi lokal go/goBack/goForward memakai mutableStateListOf)
- [ ] uji perangkat: ketuk PDF/gambar dua kali > chooser muncul; APK > installer; berkas tanpa aplikasi > toast; Alat > Buka dengan... pada satu berkas; berkas di akar Debian > Buka dengan dan Bagikan; masuk folder A > B > C lewat ketukan, tombol bulat kembali ke B lalu A; Maju kembali ke B; lompat lewat Favorit/Terakhir lalu Mundur kembali ke folder sebelumnya; hapus folder lalu Mundur melewati folder yang hilang
- [ ] LANGKAH BERIKUT: daftar "Terbaru" (sudah di peta jalan sesi lain), lalu Folder Molina (lokasi default) di Penyimpanan

## 2026-10-27 - System monitor pindah ke Settings, gaya Win7 + frame RGB [SETTINGS-MONITOR]
- [x] refactor(term): bar status 4 chip (unduh/unggah/CPU/RAM) dihapus dari halaman Terminal (`StatusChips()` dilepas dari TerminalScreen.kt, StatusChips.kt dihapus); baris Extra Keys TIDAK diubah
- [x] feat(settings): Sistem > Monitor Sistem (`settings/SystemMonitorUi.kt`, Page.Monitor): jendela kaca Aero (tepi biru berkilap, bilah judul, area klien hitam), 4 meter berbilah progres glossy, dibalut frame RGB berputar (sweep gradient, 4 dtk/putaran); data tetap dari `SystemStats` (hanya aktif selama halaman terbuka)
- [x] Dampak: :app saja (settings/SystemMonitorUi.kt baru, settings/SettingsScreen.kt, terminal/TerminalScreen.kt, terminal/StatusChips.kt dihapus). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (Brush.sweepGradient + rotate di drawBehind, animateFloatAsState, import alias Level)
- [ ] uji perangkat (Android 8): halaman Terminal tanpa bar chip dan ruang terminal bertambah; Extra Keys utuh; Settings > Sistem > Monitor Sistem tampil, angka bergerak tiap detik; frame RGB berputar mulus; unduh file besar lalu bilah Unduh naik; keluar halaman lalu sampling berhenti; pencarian "cpu"/"ram" menemukan halaman
- [ ] LANGKAH BERIKUT: riwayat grafik garis CPU/RAM 60 detik, opsi matikan animasi RGB (hemat baterai)

## 2026-10-27 - Unduhan: desain Win7 glass + grafik Monitor + saklar RGB [UNDUHAN-WIN7]
- [x] refactor(ui): komponen kaca dipusatkan di `settings/Win7Glass.kt` (RgbFrame, Win7GlassWindow, GlossBar, win7Card, Sparkline, GlassPrefs); SystemMonitorUi.kt memakainya
- [x] feat(download): halaman Unduhan tidak lagi flat: formulir dalam jendela kaca Aero berbingkai RGB (berputar HANYA saat unduhan berjalan, statis emas-biru saat diam), tombol Win7Button (Tempel, Mulai unduh, Batalkan, Buka folder, Ulangi), kartu item/riwayat berbingkai warna status, bilah progres glossy (biru mengunduh, emas memproses, hijau selesai, indeterminate bergeser), ringkasan sesi berindikator warna
- [x] feat(settings): Monitor Sistem: grafik garis CPU dan RAM 60 detik + saklar "Animasi frame RGB" (SharedPreferences molina_ui/rgb_anim, berlaku juga di Unduhan)
- [x] Dampak: :app saja (settings/Win7Glass.kt baru, settings/SystemMonitorUi.kt, download/UnduhanScreen.kt, download/DownloadHistoryUi.kt). Logika DownloadRunner/DownloadRequest tidak diubah. Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (blok formulir dipindah ke ColumnScope Win7GlassWindow, Win7Button, Canvas return@Canvas, import baru di UnduhanScreen)
- [ ] uji perangkat (Android 8): formulir tampil dalam jendela kaca; Tempel/Hapus/chip mode berfungsi; Mulai unduh (frame RGB mulai berputar), Batalkan; bilah progres bergerak dan hijau saat selesai; Buka folder dan Ulangi; riwayat tampil dengan bingkai hijau/kuning; Monitor Sistem: grafik terisi tiap detik; saklar RGB mati -> bingkai statis di Monitor dan Unduhan

## 2026-10-27 - Terminal: header Win7 + utilitas, Unduhan: preset dan total sesi [TERMINAL-WIN7]
- [x] feat(term): header kaca Aero di atas viewport (`terminal/TerminalChrome.kt`): titik status, nomor sesi [n/total] + label folder, zoom font A-/A+ (TerminalPrefs 8..14sp), saklar Layar tetap menyala (disimpan di molina_ui/term_keep_on), menu: Hentikan proses (Ctrl+C), Cari riwayat (Ctrl+R), Simpan transkrip ke /sdcard/molina-studio/terminal-logs/, Pengaturan
- [x] feat(term): viewport Terminal dibingkai tipis (sudut 6dp) tanpa mengubah kolom: perhitungan Terminal Margin Adjustment memperhitungkan inset bingkai. Extra Keys (RGB ESC/CLEAR merah, HOME/panah biru, COPY/PASTE hijau, CTRL/ALT/TAB kuning, tombol drawer) TIDAK diubah
- [x] feat(download): preset Cinema (1080p MP4), Hemat (480p MP4), Musik (MP3 320k); ringkasan sesi menampilkan bilah progres keseluruhan, kecepatan total item aktif, dan sisa waktu terlama
- [x] Dampak: :app saja (terminal/TerminalChrome.kt baru, terminal/TerminalScreen.kt, download/UnduhanScreen.kt). Gradle, Manifest, JNI mpv tidak disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (modifier bingkai di AndroidView, perhitungan lebar margin, sumOf Long pada stream progress)
- [ ] uji perangkat (Android 8): header tampil, A-/A+ mengubah font dan jumlah kolom; saklar layar menyala menahan layar; Ctrl+C menghentikan `sleep 100`; Ctrl+R membuka pencarian riwayat bash; Simpan transkrip membuat berkas dan toast path; kolom tetap rata tengah (Margin Adjustment); Extra Keys utuh; Unduhan: chip preset mengubah chip Video/Audio, bilah total dan kecepatan muncul saat berjalan
- [ ] LANGKAH BERIKUT: tombol gulir-ke-bawah saat membaca scrollback, snippet perintah favorit, tab sesi dengan nama kustom

## 2026-10-27 - Home Cinema (mpv) dan Code Editor: desain Win7 Aero [WIN7-HOME-CODE]
- [x] feat(cinema): beranda `CinemaWin7Body` (cinema/CinemaWin7Home.kt): jendela kaca Aero berbingkai RGB (berputar HANYA saat live/mencari), kolom cari + Tempel/Putar (IME Search), 4 ubin aksi (Berkas, Tautan, Setelan, Segarkan), strip status dengan GlossBar saat mencari. Dialog Tautan dan Chooser lama tetap dipakai
- [x] feat(cinema): daftar "Terakhir diputar" (cinema/CinemaRecent.kt, maks. 8, SharedPreferences molina_ui/cinema_recent): dicatat dari play/playPath/playResults, ketuk untuk putar ulang, tombol Hapus
- [x] feat(code): header kaca Aero (sama dengan header Terminal), tab bar + tab aktif gradasi biru bergaris terang, toolbar gradasi Win7, bilah status bawah (Ln/Kol, jumlah baris, bahasa, UTF-8, Diubah/Tersimpan) di `ui/CodeWin7.kt`. Area editor tetap hitam pekat, syntax highlight dan logika tidak diubah
- [x] Dampak: :app (cinema/CinemaHome.kt, CinemaActions.kt, CinemaWin7Home.kt baru, CinemaRecent.kt baru) dan :feature:code (CodeScreen.kt, EditorToolbar.kt, CodeWin7.kt baru). Gradle, Manifest, JNI mpv, MPVActivity TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (CinemaWin7Body memakai RgbFrame/Win7GlassWindow/GlossBar internal dari package settings, ed.cursor.leftLine dan ed.text.lineCount milik sora-editor, patch teks pada CodeScreen)
- [ ] uji perangkat (Android 8): Home tampil jendela kaca; Putar dengan judul membuka hasil; Putar dengan URL memutar; Tempel mengisi kolom; ubin Berkas/Tautan/Setelan/Segarkan berfungsi; putar berkas lalu kembali > muncul di Terakhir diputar tanpa duplikat; ketuk entri memutar ulang; Hapus mengosongkan; bingkai RGB diam saat idle; Code: header biru, tab aktif terang, toolbar tidak bergeser, bilah status Ln/Kol ikut kursor, status berubah Diubah > Tersimpan setelah Simpan; geser tepi kiri tetap membuka panel direktori
- [ ] LANGKAH BERIKUT: lanjutkan posisi putar terakhir, A-B loop, snippet & tema editor, Win7 untuk dialog Code (Simpan sebagai, Tutup tab)

## 2026-10-27 - Player WMP12, A-B loop, dialog Code Win7, snippet Terminal, ikon bola Aero [WIN7-PLAYER-SNIPPETS]
- [x] feat(mpv): kontrol player bergaya Windows Media Player 12 (res/drawable bg_wmp_controls/btn/pill/play, wmp_seek_progress/thumb): panel kaca biru, tombol bulat glossy, tombol Play berupa bola 56dp, seekbar glossy berthumb bola. ID dan logika tombol TIDAK diubah
- [x] feat(mpv): tombol Loop A-B (`abLoopBtn`) memakai perintah mpv `ab-loop` (A, B, mati); label "A-" / "A-B" (emas saat aktif); ikut daftar audioButtons/videoButtons di updateAudioUI agar tidak disembunyikan
- [x] Lanjut dari posisi terakhir: SUDAH ada (watch-later dipanggil di savePosition, MPVActivity), tidak diubah
- [x] feat(code): dialog bergaya Win7 (CodeWin7Dialog.kt): bingkai kaca untuk 5 dialog (Simpan sebagai dst.), Konfirmasi tutup tab ditulis ulang dengan bilah judul Aero dan tombol Simpan/Buang/Batal
- [x] feat(term): menu titik tiga > Snippet perintah (terminal/TerminalSnippets.kt): Sisip (tanpa Enter) atau Jalan (dengan Enter), tambah/hapus, 5 snippet bawaan, tersimpan di molina_ui/term_snippets
- [x] feat(brand): ikon bola biru Aero + segitiga emas `drawable/ic_cinema_orb.xml`, dipakai di beranda Cinema. Ikon launcher aplikasi (terminal) TIDAK diganti
- [x] Dampak: :feature:mpv (player.xml, 7 drawable baru, MPVActivity.kt), :feature:code (CodeScreen.kt, 5 dialog, CodeWin7Dialog.kt), :app (TerminalChrome.kt, TerminalSnippets.kt, CinemaWin7Home.kt, ic_cinema_orb.xml). Gradle, Manifest, JNI mpv TIDAK disentuh. ViewBinding PlayerBinding akan memuat abLoopBtn otomatis
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (viewGroupReorder error bila id hilang: abLoopBtn sudah ditambahkan ke kedua array; lebar baris tombol video bisa sempit di layar 360dp; painterResource vector berbasis aapt gradient)
- [ ] uji perangkat (Android 8): player: panel kontrol biru kaca, seekbar glossy dan thumb bola, Play berbentuk bola; ketuk A-B tiga kali (A, B, mati) dan video berputar antara A dan B; label emas saat aktif; ganti video audio/video tombol A-B tetap tampil; baris tombol video tidak terpotong. Code: tutup tab belum tersimpan > dialog Win7; Simpan sebagai berbingkai kaca. Terminal: Snippet perintah > Sisip menaruh teks di prompt, Jalan mengeksekusi, Tambah dan hapus tersimpan setelah restart. Cinema Home: bola Aero tampil
- [ ] LANGKAH BERIKUT: activity-alias ikon launcher Cinema terpisah (perlu ubah Manifest), kirim pilihan teks dari Code ke Terminal, daftar bookmark posisi video

## 2026-10-27 - Ikon launcher Molina Cinema + kirim teks Code ke Terminal [CINEMA-LAUNCHER-BRIDGE]
- [x] feat(brand): ikon launcher kedua "Molina Cinema" lewat `<activity-alias android:name=".CinemaLauncher">` ke MainActivity (ikon adaptif bola Aero + segitiga emas, latar biru tua, versi monochrome). Mengetuknya membuka tab Cinema (CinemaNav.openTab, MainActivity.handleOpenTabIntent). Ikon Molina lama tidak diubah
- [x] refactor(manifest): MainActivity diberi `launchMode="singleTask"` agar peluncuran dari dua ikon tidak membuat dua instance; onNewIntent sudah ada
- [x] feat(code): menu titik tiga > "Sisipkan ke Terminal" (tanpa Enter) dan "Jalankan di Terminal" (dengan Enter) untuk seleksi atau baris kursor; lewat `CodeSession.terminalSink` dan `code/CodeTerminalBridge.kt` (emulator.paste = bracketed paste, batas 20.000 karakter, ditolak saat program layar penuh aktif), lalu pindah ke tab Terminal
- [x] Dampak: AndroidManifest.xml (alias + launchMode, tanpa izin baru), :app (MainActivity.kt, cinema/CinemaNav.kt, code/CodeTerminalBridge.kt, 5 resource ikon + 1 string), :feature:code (CodeSession.kt, OverflowMenu.kt). Gradle dan JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (singleTask mengubah perilaku peluncuran; ed.cursor.isSelected/left/right dan text.substring/getLineString milik sora-editor)
- [ ] uji perangkat (Android 8): muncul dua ikon (Molina dan Molina Cinema); ikon Cinema membuka tab Cinema, ikon Molina membuka tab terakhir; ketuk ikon saat app sudah jalan tidak membuat layar ganda; notifikasi Unduhan masih membuka tab Unduhan; video diputar lalu ketuk ikon Molina > kembali ke pemutar. Code: pilih teks > Sisipkan ke Terminal menaruh teks di prompt tanpa jalan; tanpa seleksi mengirim baris kursor; Jalankan di Terminal mengeksekusi; teks 3 baris tidak jalan sebelum Enter; menu redup bila tidak ada teks
- [ ] LANGKAH BERIKUT: tombol "Sisip dari Terminal ke Code" (salin keluaran), snippet dari seleksi Code (Simpan sebagai snippet), bookmark posisi video

## 2026-10-27 - Satu ikon launcher + bingkai Cinema penuh sampai navigasi bawah [SATU-IKON-BINGKAI]
- [x] fix(manifest): `<activity-alias .CinemaLauncher>` DIHAPUS (penyebab dua aplikasi "Molina Studio" dan "Molina Cinema"); `launchMode="singleTask"` dikembalikan seperti semula. Kini hanya SATU ikon launcher
- [x] feat(brand): Molina Cinema jadi jalan pintas statis (tekan lama ikon > Cinema): `res/xml/molina_shortcuts.xml` + meta-data `android.app.shortcuts` pada MainActivity; MainActivity membaca extra `molina_open_tab=cinema`
- [x] fix(cinema): bingkai RGB + jendela kaca beranda Cinema mengisi seluruh tinggi sampai tepat di atas navigasi bawah (Win7GlassWindow: parameter `modifier` dan `fillClient`; scroll dipindah ke area klien)
- [x] Dampak: AndroidManifest.xml (alias dihapus, meta-data baru), :app (MainActivity.kt, settings/Win7Glass.kt, cinema/CinemaWin7Home.kt, res/xml/molina_shortcuts.xml baru, res/values/molina_cinema_launcher.xml). Gradle dan JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (Modifier.weight di argumen Column, verticalScroll pada area klien berbobot)
- [ ] uji perangkat (Android 8): hanya SATU ikon; tekan lama ikon menampilkan "Molina Cinema" dan membuka tab mpv; bingkai Cinema berakhir tepat di atas rail emas navigasi; layar kecil: isi jendela bisa digulir; Terminal Snippet, Monitor Sistem, Unduh tetap normal

## 2026-10-27 - Jalan pintas Terminal/Code, StatusStrip dipaku, ikon utama bola Aero [PINTAS-STATUS-IKON]
- [x] feat(brand): shortcut statis (tekan lama ikon) kini 3: Cinema, Terminal (ikon lama), Code (ikon `< >` emas baru: mipmap-anydpi-v26/ic_shortcut_code + drawable/ic_shortcut_code_foreground). Pola extra `molina_open_tab` = cinema | terminal | code
- [x] refactor(nav): `ShortcutNav` (indeks tab) menggantikan `CinemaNav` (dihapus karena tak dipakai lagi); MainActivity memetakan extra ke Tab.Mpv/Terminal/Code
- [x] feat(brand): ikon utama aplikasi di Manifest = bola Aero + segitiga emas (`ic_cinema_launcher`); ikon Terminal lama dipindah ke shortcut Terminal
- [x] fix(cinema): StatusStrip dipaku di dasar jendela (dengan garis pemisah); isi di atasnya (Brand, cari, ubin, Terakhir diputar) bisa digulir. `Win7GlassWindow.fillClient` kini hanya memberi bobot tinggi, scroll diatur pemanggil
- [x] Dampak: AndroidManifest.xml (icon/roundIcon aplikasi), :app (MainActivity.kt, ShortcutNav.kt baru, CinemaNav.kt dihapus, settings/Win7Glass.kt, cinema/CinemaWin7Home.kt, res/xml/molina_shortcuts.xml, res/values/molina_cinema_launcher.xml, 2 resource ikon baru). Gradle dan JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (Modifier.weight pada Column bersarang dengan verticalScroll; `Tab` private dipakai di handleOpenTabIntent satu file)
- [ ] uji perangkat (Android 8): ikon launcher kini bola biru emas (instal ulang bila ikon lama tersangkut cache); tekan lama menampilkan Cinema, Terminal, Code dan masing-masing membuka tab yang benar; status di dasar jendela Cinema selalu menempel, tidak ikut tergulir; layar kecil: bagian atas bisa digulir; notifikasi Unduhan tetap membuka tab Unduhan
- [ ] LANGKAH BERIKUT: bookmark posisi video, tombol "Sisip dari Terminal ke Code", snippet dari seleksi Code

## 2026-10-27 - Penanda posisi video, Sisip Terminal ke Code, Snippet dari seleksi Code [BOOKMARK-SISIP-SNIPPET]
- [x] feat(mpv): Penanda posisi video. Menu setelan player (strip ikon) > ikon bookmark: ketuk penanda = lompat, tekan lama = hapus, tombol "Tandai mm:ss" menyimpan posisi sekarang, "Hapus semua". Disimpan di SharedPreferences molina_bookmarks (maks. 200 media, 50 penanda per media); kunci = berkas lokal atau tautan asal (streamSources), stream googlevideo tanpa tautan asal ditolak. Berkas baru: VideoBookmarks.kt, ic_menu_bookmarks.xml, molina_bookmarks.xml (string); dialog_top_menu.xml + MPVActivity (streamSources, bookmarkKey, openBookmarks, MenuItem bookmarkBtn)
- [x] feat(term): menu titik tiga Terminal > "Sisip keluaran ke Code": dialog 50 / 200 baris terakhir atau seluruh transkrip, disisipkan di posisi kursor tab Code aktif (mengganti seleksi), lalu pindah ke tab Code. `CodeInbox` (:app) + `CodeSession.insertAtCursor` (antre `EditorTab.queuedInsert` bila editor belum dibuat; dihitung sebagai edit sehingga dirty dan autosave draf berjalan). Terminal/TerminalToCode.kt baru, TerminalActions.outputText
- [x] feat(code): menu titik tiga > "Simpan sebagai snippet" (seleksi atau baris kursor, maks. 4000 karakter; nama = baris pertama tak kosong, maks. 40 karakter; duplikat isi ditolak) lewat `CodeSession.snippetSink` -> `TerminalSnippets.addFromCode`
- [x] fix(term): Snippet "Sisip"/"Jalan" untuk isi banyak baris kini memakai paste (`TerminalActions.insert`) agar baris tidak langsung dieksekusi saat "Sisip"
- [x] Dampak: :feature:mpv (layout dialog_top_menu, drawable, string baru ber-tools:ignore MissingTranslation, MPVActivity, VideoBookmarks), :feature:code (CodeSession, EditorHost, OverflowMenu), :app (MainActivity, ShortcutNav, CodeInbox, TerminalSnippets, TerminalActions, TerminalChrome, TerminalToCode). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (sora-editor 0.23.6 `CodeEditor.insertText(CharSequence, Int)`; `AlertDialog.listView` bisa null; Win7Button dengan parameter modifier posisional)
- [ ] uji perangkat (Android 8): putar video lokal > menu setelan > bookmark: Tandai, tutup, buka lagi, penanda muncul urut waktu, ketuk melompat, tekan lama menghapus; video YouTube via Cinema: penanda tersimpan dan muncul lagi pada putaran berikutnya dengan tautan yang sama; Terminal: jalankan perintah, menu > Sisip keluaran ke Code > 50 baris: teks masuk di kursor tab Code, tab berpindah, titik "belum disimpan" menyala; coba saat tab Code belum pernah dibuka; Code: pilih teks > menu > Simpan sebagai snippet > muncul di Terminal > Snippet perintah; snippet banyak baris: Sisip tidak menjalankan, Jalan menjalankan
- [ ] LANGKAH BERIKUT: daftar penanda bernama (ubah nama), penanda di bilah seek, tombol "Salin keluaran perintah terakhir" ke Code, impor/ekspor snippet

## 2026-10-27 - Penanda bernama + konfirmasi CI/uji fase sebelumnya [BOOKMARK-NAMA]
- [x] Konfirmasi pengguna: fase [BOOKMARK-SISIP-SNIPPET] (commit 16819e0) build CI SUKSES dan sudah diuji di perangkat
- [x] feat(mpv): penanda bernama. Tekan lama penanda > pilih "Ubah nama" (dialog input, maks. 40 karakter, kosong = hapus nama) atau "Hapus". Daftar menampilkan "mm:ss  .  nama". `Mark(sec, name)` menggantikan Double di `VideoBookmarks`; format simpan lama (angka) tetap terbaca, penanda tanpa nama tetap disimpan sebagai angka
- [x] Dampak: :feature:mpv (VideoBookmarks.kt, MPVActivity.openBookmarks, molina_bookmarks.xml). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah (`when` pada `arr.get(i)` JSONObject/Number; AlertDialog bersarang + restoreState)
- [ ] uji perangkat (Android 8): tandai posisi > tekan lama > Ubah nama > nama muncul di daftar; kosongkan nama > kembali hanya waktu; tekan lama > Hapus; penanda lama (sebelum update) tetap muncul; video terjeda saat dialog dan lanjut setelah ditutup
- [ ] LANGKAH BERIKUT: penanda di bilah seek, tombol "Salin keluaran perintah terakhir" ke Code, impor/ekspor snippet

## 2026-10-27 - Penanda di bilah seek, salin keluaran terakhir ke Code, impor/ekspor snippet [SEEK-SALIN-SNIPPETIO]
- [x] feat(mpv): titik emas penanda di bilah seek. `MarkedSeekBar` (turunan SeekBar, menggambar lingkaran emas #E6B450 berbingkai gelap di posisi penanda) menggantikan SeekBar di player.xml (id playbackSeekbar tetap). `refreshSeekMarks()` dipanggil saat durasi diketahui dan setiap dialog Penanda ditutup
- [x] feat(term): menu titik tiga Terminal > "Salin keluaran terakhir ke Code" tanpa dialog. `TerminalActions.lastCommandOutput()` mengenali baris prompt `user@host:path$`/`#` pada transkrip; bila prompt tak dikenali muncul toast
- [x] feat(term): Snippet perintah > tombol "Ekspor" / "Impor" ke /sdcard/Molina/molina-snippets.json (folder aplikasi, terlihat dari Debian lewat bind). Impor menggabungkan dan melewati perintah yang sudah ada (maks. 200 snippet)
- [x] Dampak: :feature:mpv (MarkedSeekBar.kt baru, player.xml, MPVActivity), :app (TerminalActions, TerminalChrome, TerminalSnippets). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (override `onDraw` SeekBar dengan @Synchronized; regex prompt pada string mentah; Win7Button tanpa modifier)
- [ ] uji perangkat (Android 8): beri 2-3 penanda pada video > titik emas muncul di seekbar pada posisi benar, titik hilang saat Hapus semua, video tanpa penanda tidak tergambar apa pun; Terminal: jalankan `ls` > menu > Salin keluaran terakhir ke Code > hanya keluaran `ls` yang masuk (tanpa prompt); jalankan perintah tanpa keluaran > toast "tidak ditemukan"; Snippet > Ekspor > cek /sdcard/Molina/molina-snippets.json; hapus satu snippet > Impor > kembali muncul tanpa duplikat
- [ ] LANGKAH BERIKUT: ketuk titik di bilah seek untuk lompat ke penanda, cadangan otomatis snippet saat berubah, tampilkan nama penanda di label posisi saat menyeret

## 2026-10-27 - Ketuk titik penanda, cadangan otomatis snippet, nama penanda di label seek [TAP-AUTOBACKUP-LABEL]
- [x] feat(mpv): ketukan singkat pada titik emas di bilah seek (radius 16dp, tanpa geser) lompat tepat ke penanda dan menampilkan labelnya 1,5 detik; geseran biasa tidak berubah. `MarkedSeekBar.onMarkTap`, `MPVActivity.jumpToMark`
- [x] feat(mpv): saat bilah seek diseret melewati penanda bernama (+-2 detik), label gestur menampilkan "mm:ss . nama"; label disembunyikan saat lepas/menjauh (`showMarkLabelWhileSeeking`, `hideMarkLabel`)
- [x] feat(term): cadangan otomatis snippet. Tiap daftar berubah (tambah/hapus/simpan dari Code) ditulis ke /sdcard/Molina/molina-snippets.json lewat executor satu thread. Saat prefs kosong (pasang baru / data aplikasi dihapus) daftar dipulihkan dari berkas itu, selain itu memakai bawaan. Ekspor/Impor manual tetap ada
- [x] Dampak: :feature:mpv (MarkedSeekBar.kt, MPVActivity), :app (TerminalSnippets). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah (override onTouchEvent SeekBar; lambda `Runnable` yang memakai `binding`; `return` di dalam `try` pada restoreOrDefaults)
- [ ] uji perangkat (Android 8): ketuk titik emas > lompat tepat ke posisi + label muncul; geser dari atas titik tetap bisa seek normal; beri nama penanda lalu seret melewatinya > label berisi nama; Snippet: tambah satu > cek berkas JSON ikut berubah; hapus data aplikasi (atau pasang ulang) > buka Snippet > daftar kembali dari cadangan
- [ ] LANGKAH BERIKUT: tanda emas penanda di label durasi/preview, urutan ulang snippet, cadangan penanda video (ekspor/impor) ke folder Molina

## 2026-10-27 - Cadangan penanda video, urut ulang snippet, penanda di label pratinjau seek [BACKUP-MARKS-REORDER-PREVIEW]
- [x] feat(mpv): cadangan penanda video. Setiap perubahan ditulis ke /sdcard/Molina/Backup/molina-bookmarks.json (executor satu thread); prefs kosong (pasang baru / data dihapus) dipulihkan dari berkas itu; data lama dicadangkan sekali saat dibaca. Dialog Penanda: tombol "Cadangan" (menggantikan "Batal"; tombol Kembali tetap menutup) > Ekspor / Impor (gabung, lewati posisi yang sudah ada)
- [x] refactor(term): cadangan snippet dipindah ke /sdcard/Molina/Backup/molina-snippets.json (folder `MolinaDir.Backup` yang sudah ada); berkas lama di /sdcard/Molina/ tetap terbaca bila berkas baru belum ada
- [x] feat(term): urut ulang snippet dengan tombol panah naik/turun di tiap baris (`TerminalSnippets.move`)
- [x] feat(mpv): "pratinjau seek": aplikasi belum punya pratinjau gambar, jadi label gestur saat bilah seek diseret kini menampilkan titik emas + waktu (+ nama) untuk SETIAP penanda dalam +-2 detik, bukan hanya yang bernama
- [x] Dampak: :feature:mpv (VideoBookmarks.kt ditulis ulang, MPVActivity, molina_bookmarks.xml; memakai :core:common yang sudah menjadi dependensi), :app (TerminalSnippets). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (`sumOf` pada Collection<MutableList>, `return` di dalam elvis pada readAll, `Modifier.clickable(enabled=)` dengan lambda di Column dalam Row)
- [ ] uji perangkat (Android 8): buat penanda > cek /sdcard/Molina/Backup/molina-bookmarks.json; hapus data aplikasi > putar video yang sama > penanda kembali; Cadangan > Ekspor/Impor memberi toast; seret seekbar melewati penanda tanpa nama > label "titik + waktu"; Snippet: panah naik/turun mengubah urutan dan urutan bertahan setelah aplikasi ditutup
- [ ] LANGKAH BERIKUT: pratinjau gambar (thumbnail) saat seek, ekspor ke satu berkas gabungan di Backup (penanda + snippet + setelan), tombol pulihkan dari Backup di Pengaturan

## 2026-10-27 - Thumbnail seek, cadangan gabungan, tombol Pulihkan dari Backup [THUMB-BACKUP-RESTORE]
- [x] feat(mpv): pratinjau saat bilah seek diseret. Kartu di atas bilah kontrol (`seekPreview`) mengikuti jempol seekbar: bingkai video 160x90dp + label waktu (+ titik emas/nama penanda terdekat). Bingkai diambil `SeekThumbnailer` (MediaMetadataRetriever, OPTION_CLOSEST_SYNC, thread latar tunggal, permintaan usang dilewati). HANYA berkas lokal berjalur "/..."; stream dan fd:// hanya menampilkan label tanpa gambar. Label tengah layar lama dipakai lagi hanya untuk ketukan titik penanda
- [x] feat(backup): cadangan gabungan /sdcard/Molina/Backup/molina-backup.json (`BackupManager`): penanda video, snippet, setelan (SharedPreferences: molina_ui[rgb_anim, term_keep_on], molina_code, molina_shield, molina_privacy, molina_stream, molina_download, molina_explorer tanpa recent_v1, prefs default mpv) + setelan Terminal. TIDAK ikut: riwayat/cache, data alat, setelan akses penyimpanan penuh. Penulisan lewat berkas .tmp lalu rename
- [x] feat(settings): Settings > Sistem > "Cadangan dan pemulihan" (dialog): "Cadangkan sekarang" dan "Pulihkan dari Backup" (dua langkah konfirmasi). Pulihkan menggabungkan penanda dan snippet, menimpa setelan; perlu mulai ulang aplikasi agar semua setelan berlaku. Ditambahkan ke indeks pencarian Settings
- [x] refactor: `VideoBookmarks.snapshot/mergeFrom` dan `TerminalSnippets.snapshot/mergeFrom` dipakai bersama oleh Impor tiap fitur dan cadangan gabungan
- [x] Dampak: :feature:mpv (SeekThumbnailer.kt, bg_seek_preview.xml, player.xml, MPVActivity, VideoBookmarks), :app (BackupManager.kt, BackupUi.kt, SettingsScreen, TerminalSnippets). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (id `seekPreview*` pada view binding player.xml; `binding.outside`; `for ((k, v) in prefs.all)` + `when` dengan `else -> continue`; `catch (ignored: Exception)` kosong; Compose AlertDialog dengan Column di confirmButton)
- [ ] uji perangkat (Android 8): putar video LOKAL > seret seekbar > kartu bingkai + waktu mengikuti jempol, hilang saat lepas; video YouTube/stream > kartu hanya teks; titik penanda dekat > label berawalan titik emas; Settings > Sistem > Cadangan dan pemulihan > Cadangkan > cek molina-backup.json; ubah beberapa setelan (font Terminal, Shield) > Pulihkan (konfirmasi dua langkah) > nilai kembali setelah mulai ulang; hapus data aplikasi > Pulihkan > penanda dan snippet kembali
- [ ] LANGKAH BERIKUT: bingkai pratinjau untuk stream (sprite/storyboard) bila tersedia, pilih bagian yang dipulihkan (centang), cadangan gabungan otomatis harian

## 2026-10-27 - Indikator progres dialog jalankan + bingkai Win7 ubin Settings [UI-PROGRES-FRAME]
- [x] feat(settings): `RunProgressTracker` dan `RunProgressPanel` (GlossBar, fase, persen) di `ToolRunDialog`. Dipakai Debian update/upgrade, pasang/hapus paket, yt-dlp. Log mentah disembunyikan di balik "Tampilkan rincian"
- [x] feat(settings): `Win7FrameBox`, bingkai biru tipis saat ditekan/terpilih. Dipakai `W7CategoryTile` (+ penanda kategori terakhir dibuka) dan `W7Link`
- [x] Dampak: :app saja (settings/RunProgressUi.kt, Win7FrameUi.kt baru; ToolRunUi, Win7SettingsUi, SettingsScreen). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (Triple destructuring pada `when`, `clickable(indication = null)`, `rememberSaveable` untuk `Page?`)
- [ ] uji perangkat (Android 8): Sistem > Perbarui dan tingkatkan paket > bilah biru bergerak, fase berganti, persen naik saat upgrade, berakhir hijau; matikan internet > merah; tekan Berhenti > kuning; ubin beranda: tekan > bingkai biru, kembali dari Sistem > ubin Sistem tetap berbingkai
- [ ] LANGKAH BERIKUT: tampilkan ukuran unduhan di fase "Mengunduh paket", ubin Settings dengan ikon status (mis. jumlah paket bisa diupgrade)

## 2026-10-27 - Periksa pembaruan paket, lencana ubin Sistem, bersihkan cache apt, pewaktu progres [APT-CHECK-BADGE-CLEAN]
- [x] feat(settings): `AptJob.CheckUpdates` (apt update + hitung `apt list --upgradable`, tanpa konfirmasi) dan `AptHistory.pending` (SharedPreferences molina_apt). Berhasil upgrade mengatur pending=0
- [x] feat(settings): `W7Badge` merah glossy di ubin Sistem beranda saat pending > 0
- [x] feat(settings): `AptJob.CleanUp` (apt-get autoremove -y + apt-get clean) di Settings > Sistem > Debian
- [x] feat(settings): pewaktu `m:ss` di `RunProgressPanel` (ticker 1 dtk di ToolRunDialog), ditampilkan bersama persen
- [x] Dampak: :app saja (DebianAptUi, ToolRunUi, RunProgressUi, Win7FrameUi, Win7SettingsUi, SettingsScreen). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (`return@ToolRunDialog` di lambda trailing, referensi fungsi `::countSummary` bertipe eksplisit, `Column(Modifier.weight(1f))` di ubin)
- [ ] uji perangkat (Android 8): Sistem > Periksa pembaruan paket > dialog langsung jalan, selesai menampilkan "N paket dapat ditingkatkan" > beranda: lencana N di ubin Sistem; Perbarui dan tingkatkan > lencana hilang; Bersihkan cache > selesai hijau; waktu berjalan di panel bertambah tiap detik
- [ ] LANGKAH BERIKUT: periksa otomatis maks. 1x/24 jam saat Wi-Fi, tampilkan ukuran unduhan di fase "Mengunduh paket", lencana serupa untuk pembaruan yt-dlp
- [x] Fix build: hapus import matchParentSize (member BoxScope) di Win7FrameUi.kt

## 2026-10-27 - Tukar Berkas dan Terminal: nav bawah dan ubin Settings [SWAP-FILES-TERMINAL]
- [x] refactor(nav): `Tab.Terminal` diganti `Tab.Files` ("Berkas") di posisi nav bawah yang sama (indeks 1), jadi `ShortcutNav.TAB_CODE=2` tetap benar. Urutan: mpv | Berkas | Code | Settings
- [x] feat(settings): ubin beranda Settings ke-4 kini "Terminal" (menggantikan Berkas), membuka `Page.TerminalApp` yang menampilkan `TerminalScreen` penuh. Tombol gear di Terminal membuka Settings > Sistem > Terminal (preferensi). `Page.Files` dihapus
- [x] feat(files): `FilesTabScreen` (settings/FilesTabScreen.kt) membuka Penjelajah Molina otomatis saat tab Berkas dipilih; bila ditutup tersedia tombol untuk membukanya lagi
- [x] refactor(nav): `SettingsNav.openTerminalApp`; `ScriptNav.openTerminal` (Script execution, Code, Penjelajah "Buka Terminal di sini") dan jalan pintas ikon "terminal" kini pindah ke Settings > Terminal. Tambah jalan pintas extra "files"
- [x] Dampak: :app saja (MainActivity, settings/SettingsScreen, settings/FilesTabScreen baru, ShortcutNav, pesan di CodeTerminalBridge/ProotScriptExecutor/ScriptRunner). Gradle, Manifest, JNI mpv TIDAK disentuh. Sesi Terminal tetap di `TerminalSessions` (singleton) sehingga tidak putus saat pindah halaman
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (`when` dengan blok pada `handleOpenTabIntent`, `Page.TerminalApp -> {}` pada `when` penuh, glyph U+25A4 di font Android 8)
- [ ] uji perangkat (Android 8): nav bawah menampilkan Berkas di posisi lama Terminal; Berkas > Penjelajah terbuka otomatis, tutup > tombol "Penjelajah Molina"; Settings beranda > ubin Terminal > sesi Debian jalan, keyboard muncul, gear > preferensi Terminal; Penjelajah > "Buka Terminal di sini" > pindah ke Settings > Terminal dengan cwd benar; Script execution > Jalankan > pindah ke Settings > Terminal; tekan lama ikon app > Terminal > Settings > Terminal
- [ ] LANGKAH BERIKUT: tombol Back di Settings > Terminal sekarang kembali ke beranda Settings (bukan keluar app); pertimbangkan sembunyikan bilah breadcrumb Settings saat halaman Terminal agar ruang terminal lebih luas

## 2026-10-27 - Header Settings disembunyikan di Terminal dan ikon tab Berkas [FILES-TAB-ICON-HIDE-HEADER]
- [x] feat(settings): `Win7SettingsWindow` punya parameter `showHeader` (default true). Halaman `Page.TerminalApp` menyembunyikan breadcrumb dan kotak cari agar ruang terminal lebih luas. Back sistem tetap kembali ke beranda Settings; gear Terminal tetap membuka preferensi
- [x] fix(nav): tab Berkas memakai `TabFolderIcon` (Canvas, mengikuti warna tab) menggantikan glyph U+25A4 yang berisiko tampil kotak kosong di font Android 8
- [x] Dampak: :app saja (settings/Win7SettingsUi, settings/SettingsScreen, settings/FilesTabScreen, MainActivity). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah (parameter default sebelum trailing lambda, import Canvas/geometry)
- [ ] uji perangkat (Android 8): Settings > Terminal tanpa bilah atas; Back kembali ke beranda Settings; ikon Berkas biru tampil jelas, berubah redup saat tidak dipilih
- [ ] LANGKAH BERIKUT: konfirmasi saat Back di Terminal bila ada proses berjalan; token warna tab di MolinaTheme

## 2026-10-27 - Tab Berkas langsung membuka Penjelajah, tanpa tombol X [FILES-DIRECT-NO-CLOSE]
- [x] refactor(files): `FilesTabScreen` tidak lagi menampilkan kartu perantara "Penjelajah Molina"; tab Berkas langsung membuka dialog explorer
- [x] feat(files): `Win7OpenDialog` punya parameter `showClose` (default true). Tab Berkas memakai `showClose = false`, sehingga ikon X pojok kanan atas hilang; dialog lain (mpv Buka, Unduhan) tidak berubah
- [x] feat(nav): Back di root explorer memanggil `onExit` dan kembali ke tab mpv (`MainActivity`)
- [x] Dampak: :app saja (cinema/Win7OpenDialog, settings/FilesTabScreen, MainActivity). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah (import Column tidak terpakai lagi diganti Box; import lain yang tak terpakai hanya warning)
- [ ] uji perangkat (Android 8): tap Berkas > explorer langsung tampil tanpa kartu; tidak ada X; Back naik folder lalu di root pindah ke tab mpv; "Buka di Code" dan "Buka Terminal di sini" tetap pindah halaman; dialog Buka di mpv/Unduhan masih punya X
- [ ] LANGKAH BERIKUT: ubah explorer dari Dialog menjadi konten tab biasa (nav bawah tetap terlihat) sehingga perpindahan tab tidak bergantung pada Back
- [x] refactor(files): tab Berkas langsung membuka Penjelajah (tanpa kartu & tombol X); Win7OpenDialog mendukung mode asScreen

## 2026-10-28 - Panel bawah Penjelajah dirancang ulang [FILES-DOCK]
- [x] feat(files): `W7BrowseDock` menggantikan baris path/status, tombol aksi, kolom Nama file, combo jenis, dan Buka/Batal untuk mode jelajah
- [x] Tinggi panel tetap; nama dipotong di tengah, path mempertahankan ujung kanan dan bisa diketuk untuk menyalin, status Debian berupa titik berwarna
- [x] Dampak: :app saja (cinema/Win7OpenDialog, cinema/W7BrowseDock). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah
- [ ] uji perangkat (Android 8): nama/path sangat panjang tidak mengubah tinggi panel; Putar/Edit/Buka ZIP/Properti sesuai seleksi; multi-pilih menampilkan jumlah item; Buka pada folder masuk ke folder
- [ ] LANGKAH BERIKUT: pencarian kilat di Penjelajah dan pratinjau thumbnail video di panel

## 2026-10-28 - Aksen merek, thumbnail video, chip riwayat putar [FILES-POLISH]
- [x] refactor(theme): 24 token Win7 dipindah ke `MolinaTheme.Explorer`; `Win7` kini hanya alias (call site tidak berubah). `DefaultLine` memakai `MolinaTheme.Gold`
- [x] feat(files): tombol Buka beraksen emas, Terminal dan titik Debian memakai `MolinaTheme.Good`/`Warn`
- [x] feat(files): `W7Thumb` thumbnail video di panel bawah; dekode di Dispatchers.IO, jeda 250 ms, LruCache 32 entri, kegagalan dicatat agar tak diulang
- [x] feat(files): chip "Terakhir diputar" (aksen Cinema) dan "Diputar" di daftar berkas, bersumber dari `CinemaRecent` (maks. 8)
- [x] Dampak: :app saja (ui/MolinaTheme, cinema/Win7Aero, Win7OpenDialog, W7BrowseDock, W7Thumb baru). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (produceState, MediaMetadataRetriever)
- [ ] uji perangkat (Android 8): pilih mp4 lalu thumbnail muncul; mkv bisa gagal dan jatuh ke ikon putar; chip muncul setelah memutar berkas dari Penjelajah; warna dialog lain tidak berubah kecuali garis tombol utama
- [ ] LANGKAH BERIKUT: thumbnail audio (cover art) lewat embeddedPicture, dan pencarian kilat di Penjelajah

## 2026-10-28 - Cover art audio di panel Penjelajah [FILES-COVER]
- [x] feat(files): `W7Thumb` mendukung audio via `embeddedPicture`; decode memakai inSampleSize, maks. 120x120, tetap di Dispatchers.IO + LruCache
- [x] Panel bawah: cover persegi 30 dp untuk audio, ikon nada bila tidak ada cover; video tidak berubah
- [x] Catatan: pencarian di Penjelajah SUDAH ada (kotak cari + cari dalam), jadi tidak dibuat ulang
- [x] Dampak: :app saja (cinema/W7Thumb, cinema/W7BrowseDock). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah
- [ ] uji perangkat (Android 8): pilih mp3 yang punya cover lalu cover tampil; mp3 tanpa cover menampilkan ikon nada
- [ ] LANGKAH BERIKUT: antrean "Putar semua di folder ini" dari panel bawah

## 2026-10-28 - Antrean putar, urutan terakhir diputar, detail media [FILES-QUEUE]
- [x] feat(files): menu Alat: Putar semua di daftar ini (N) dan Putar mulai dari item ini, mengikuti urutan tampil; playlist m3u/pls/cue dikecualikan
- [x] feat(files): Urutan terakhir diputar dulu (stabil, folder tetap di atas, sumber CinemaRecent)
- [x] feat(files): W7MediaInfo menambah Judul/Artis/Album/Durasi/Resolusi/Bitrate/Format di Properti (Dispatchers.IO)
- [x] Dampak: :app saja. Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] uji perangkat: antrean 5+ lagu; mulai dari item ke-3; Properti mp4/mp3 menampilkan durasi

## 2026-10-28 - Urutan terakhir diputar dulu menjadi persisten [FILES-PERSIST]
- [x] feat(files): ExplorerPrefs.Data.playedFirst (default false) disimpan di molina_explorer kunci played_first
- [x] Dampak: :app saja (ExplorerPrefs, Win7OpenDialog). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] uji perangkat: aktifkan urutan, tutup lalu buka Penjelajah, pilihan tetap aktif
- [ ] LANGKAH BERIKUT: cek hasil CI semua patch files-*, lalu uji perangkat gabungan

## 2026-10-28 - Ikon berkas per ekstensi [FILES-ICONS]
- [x] feat(files): `W7FileIcon` membedakan jenis berkas: gambar (bukit+matahari), video (putar), audio (not), dan label berwarna PY/XML/SVG/JSON/YML/CFG/KT/JV/GRD/C/C++/JS/SH/MD/TXT/LOG/CSV/ZIP/PDF/DOC/XLS/PPT/APK/SUB/LST
- [x] Ekstensi tak dikenal: aksen emas dengan 3 huruf pertama ekstensi; tanpa ekstensi: halaman polos
- [x] Dampak: :app saja (cinema/W7FileIcon baru, cinema/Win7OpenDialog 1 baris). Gradle, Manifest, JNI mpv TIDAK disentuh. Win7File lama tetap dipakai CinemaWin7Home
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah
- [ ] uji perangkat (Android 8): label tiga huruf terbaca di ikon 22 dp; font besar sistem tidak memotong label

## 2026-10-28 - Ikon jenis berkas di panel bawah [FILES-DOCKICON]
- [x] feat(files): panel bawah menampilkan W7FileIcon (28 dp) atau ikon folder (26 dp) di kiri nama; media bertumbnail tetap memakai thumbnail
- [x] Dampak: :app saja (cinema/W7BrowseDock). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] uji perangkat: pilih py/xml/zip lalu ikon sesuai; pilih folder lalu ikon folder; pilih mp4 lalu thumbnail

## 2026-10-28 - Buka berkas di dalam ZIP tanpa ekstrak penuh [ZIP-OPEN]
- [x] feat(zip): `FileOps.extractEntry` mengekstrak SATU entri ke cacheDir/zipview (bukan seluruh ZIP); zip-slip ditolak, hasil dipakai ulang, cache dipangkas 300 MB; ZIP AES lewat zip4j dengan kata sandi
- [x] feat(zip): dialog ZIP kini punya ikon per entri, pilih lalu Buka (atau ketuk dua kali); entri diserahkan ke smartOpen: media ke mpv, teks ke Code, ZIP bersarang ke dialog ZIP, lainnya Buka dengan
- [x] Catatan: berkas dibuka dari salinan sementara; perubahan di Code tidak ditulis balik ke ZIP
- [x] Dampak: :app saja (cinema/FileOps, cinema/Win7OpenDialog). Gradle, Manifest, JNI mpv TIDAK disentuh; tidak ada izin baru (cacheDir internal)
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (zip4j extractFile, panggilan W7PasswordDialog bertingkat)
- [ ] uji perangkat (Android 8): ZIP biasa buka .txt/.py (Code), .mp4 (mpv), .jpg; ZIP berkata sandi (kata sandi benar/salah); entri besar; buka dua kali memakai cache
- [ ] LANGKAH BERIKUT: tombol Simpan balik ke ZIP untuk berkas teks yang diedit, dan pratinjau gambar langsung di dialog

## 2026-10-28 - Simpan balik ke ZIP, pratinjau gambar, navigasi folder ZIP, Terminal [ZIP-SAVE]
- [x] feat(zip): dialog ZIP menjelajah per folder (path), dengan baris naik; batas daftar ZIP_LIST_MAX 300 menjadi 5000
- [x] feat(zip): tombol Simpan menulis kembali entri yang disunting ke ZIP (`FileOps.replaceEntry`, zip4j); cadangan otomatis di zipbak, pemulihan bila gagal; ZIP AES dienkripsi ulang AES-256
- [x] feat(zip): deteksi suntingan tiap detik (cap lastModified/length), penanda diubah di baris, peringatan saat Tutup dengan perubahan belum disimpan
- [x] feat(zip): pratinjau gambar langsung dari ZIP (`W7Thumb.zipImage`, maks. 12 MB, di Dispatchers.IO) tanpa ekstrak ke disk
- [x] feat(zip): tombol Terminal membuka folder salinan entri (bila terlihat dari Debian); salinan disimpan di getExternalFilesDir/zipview
- [x] Dampak: :app saja (cinema/FileOps, cinema/W7Thumb, cinema/Win7OpenDialog). Gradle, Manifest, JNI mpv TIDAK disentuh; tanpa izin baru
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (zip4j removeFile/addFile, produceState, dialog password bertingkat)
- [ ] uji perangkat (Android 8): masuk folder dalam ZIP; buka .txt di Code lalu edit lalu Simpan lalu buka ulang; ZIP berkata sandi; gambar pratinjau; Terminal pada salinan; Tutup dengan perubahan memunculkan peringatan
- [ ] LANGKAH BERIKUT: tambah berkas baru ke ZIP dari Penjelajah (seret atau Sisipkan) dan hapus entri dari ZIP

## 2026-10-28 - Tambah dan hapus entri di dalam ZIP [ZIP-EDIT]
- [x] feat(zip): `FileOps.addEntries` menambah berkas/folder ke folder ZIP yang sedang dibuka (nama sama diganti; ZIP AES dienkripsi ulang AES-256 dengan kata sandi yang sama; ZIP itu sendiri dikecualikan)
- [x] feat(zip): `FileOps.deleteEntries` menghapus berkas atau folder beserta isinya; konfirmasi dua ketukan di UI
- [x] feat(zip): tombol Tempel (sumber: klip Salin Penjelajah) dan Hapus di dialog ZIP; cadangan otomatis zipbak dan pemulihan bila gagal dipakai bersama lewat `zipGuarded`
- [x] Dampak: :app saja (cinema/FileOps, cinema/Win7OpenDialog). Gradle, Manifest, JNI mpv TIDAK disentuh; tanpa izin baru
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (zip4j removeFiles/addFolder + setRootFolderNameInZip, ExcludeFileFilter, dialog sandi tingkat 3)
- [ ] uji perangkat (Android 8): salin berkas di Penjelajah lalu buka ZIP lalu Tempel; Tempel folder; nama sama (diganti); ZIP berkata sandi benar/salah; Hapus berkas dan folder (dua ketukan); cek zipbak/<nama>.zip.bak

