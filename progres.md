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

## 2026-10-28 - Ikon kecil di menu titik tiga dan Aksi file modul Code [CODE-MENUICON]
- [x] feat(code): `MenuIcons.kt` baru: 28 ikon garis kisi 24x24 (Save/Find/Paste/Play/Lock/Redo memakai ulang ToolbarIcons), tanpa dependency ikon
- [x] feat(code): `MenuEntry` dan `ActionItem` punya parameter `icon`; baris tanpa ikon tetap rata teksnya (ruang ikon dicadangkan)
- [x] feat(code): menu titik tiga (OverflowMenu) dan menu Aksi file/tree (CompactActionMenu) menampilkan ikon 18 dp warna champagne; Hapus tetap merah
- [x] Dampak: :feature:code saja. Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (drawArc/Size di MenuIcons.kt)
- [ ] uji perangkat (Android 8): ikon terbaca di 18 dp, baris redup (disabled) ikut redup, teks sejajar, ikon Hapus merah, menu tree dan menu Aksi file sama

## 2026-10-28 - Palet Perintah, tautan error di Keluaran, menu dirapikan [CODE-PALETTE]
- [x] feat(code): Palet Perintah (menu titik tiga > Palet perintah): satu kotak cari untuk perintah aktif, tab terbuka, dan berkas folder utama (fuzzy, awal kata/camelCase diutamakan; maks. 4000 berkas, kedalaman 8, tanpa symlink/biner/.git/build; dipindai di Dispatchers.IO). Enter menjalankan hasil teratas
- [x] refactor(code): daftar menu titik tiga diekstrak ke `menuGroups()` sehingga menu dan palet memakai satu sumber perintah
- [x] feat(code): `ErrorLinks` mengenali File "x", line N (Python), x: line N: (bash), dan x.ext:baris[:kolom] (gcc/kotlinc/dll.); tautan di panel Keluaran bergaris bawah emas, ketuk membuka berkas dan melompat ke baris (fallback: tab skrip yang sedang dijalankan)
- [x] chore(code): placeholder yang selalu redup (Ekspor, Konversi berkas, Syntax, Gaya Visual, Lakukan) dihapus dari menu titik tiga
- [x] Dampak: :feature:code saja (file baru CommandPalette, OutputLinks, preview/ErrorLinks). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (ClickableText di Compose BOM 2024.12.01, Dialog usePlatformDefaultWidth, refactor menuGroups)
- [ ] uji perangkat (Android 8): buka palet lalu ketik "cari", nama tab, nama berkas; Enter; jalankan .py yang error lalu ketuk tautan File "x", line N; jalankan .sh dengan error baris N; folder besar tidak membuat palet lag
- [ ] LANGKAH BERIKUT: Find in Files (grep di PRoot), Outline per berkas, penanda perubahan di gutter, scratch file/live template, tombol Play di gutter

## 2026-10-28 - Find in Files dan Struktur berkas (Outline) [CODE-SEARCH]
- [x] feat(code): "Cari di berkas" (menu titik tiga dan Palet Perintah): cari isi berkas teks di folder utama, opsi Aa (huruf besar/kecil) dan .* (regex), hasil dikelompokkan per berkas dengan sorotan emas; ketuk baris membuka berkas dan melompat ke baris/kolom lewat `openLocation`
- [x] Mesin murni Kotlin (tanpa PRoot) di Dispatchers.IO: berkas dari `PaletteScan` (maks. 4000), lewati biner (NUL di 4 KB pertama) dan berkas > 1 MB, satu hasil per baris, maks. 50 per berkas dan 500 total, debounce 300 ms, dibatalkan saat query berubah
- [x] feat(code): "Struktur berkas": daftar kelas/fungsi/judul Markdown tab aktif (kt, kts, java, py, js/ts, sh, md) berbasis regex, indent jadi kedalaman, ketuk untuk melompat
- [x] Dampak: :feature:code saja (FindInFiles.kt, Outline.kt baru). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (isActive di dalam withContext, LazyColumn items, regex Kotlin raw string dengan interpolasi MODS_KT)
- [ ] uji perangkat (Android 8): cari kata umum di folder besar (tidak lag, bisa diubah saat jalan); regex tidak valid memberi pesan; ketuk hasil membuka berkas dan melompat; Outline pada .kt, .py, .sh, .md
- [ ] CATATAN: regex pengguna yang berat (backtracking) hanya bisa dibatalkan antar berkas; baris dipotong 2000 karakter untuk meredam
- [ ] LANGKAH BERIKUT: penanda perubahan di gutter, scratch file dan Live Template, tombol Play di gutter

- [x] Push ke origin/main berhasil setelah jaringan pulih (kegagalan awal: Could not resolve host github.com). Menunggu hasil CI GitHub Actions

## 2026-10-28 - Verifikasi CI [CI-214]
- [x] Build #214 (Find in Files + Outline) sukses di GitHub Actions; ikon menu, Palet Perintah, tautan error, Cari di berkas, dan Struktur berkas lolos kompilasi
- [ ] Uji perangkat Android 8 untuk semua fitur di atas masih tertunda

## 2026-10-28 - Rapikan menu dan bilah status [CODE-STATUS]
- [x] refactor(code): jumlah karakter ditambahkan ke bilah status ("N baris, M karakter"); grup menu "Statistik" (Toast) dihapus
- [x] refactor(code): baris "Enkode: UTF-8 (terkunci)" dihapus dari menu (mati permanen; UTF-8 sudah tampil di bilah status)
- [x] Placeholder lama (Ekspor, Konversi berkas..., Syntax, Gaya Visual, Lakukan) sudah tidak ada di kode sejak commit sebelumnya
- [x] Dampak: :feature:code saja (OverflowMenu.kt, CodeWin7.kt). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko rendah. Import Toast tetap dipakai oleh "Bagikan"
- [ ] LANGKAH BERIKUT: penanda perubahan di gutter (perlu verifikasi API sora-editor 0.23.6), scratch file dan Live Template, tombol Play di gutter

## 2026-10-28 - Penanda perubahan di gutter [CODE-GUTTER]
- [x] feat(code): garis tipis di tepi kiri gutter: hijau = baris baru, biru = baris diubah, tanda merah = ada baris dihapus, dibandingkan dengan isi tersimpan terakhir
- [x] Diff berbasis baris (buang awalan/akhiran sama, LCS pada sisa, batas 1 juta sel), debounce 400 ms, dihitung di Dispatchers.Default; berkas > 400 KB tidak diberi penanda
- [x] Memakai hook resmi sora-editor 0.23.6: `CodeEditor.onCreateRenderer()` + `EditorRenderer.drawLineNumber` (diverifikasi dari source tag 0.23.6)
- [x] Teks tersimpan disimpan di EditorTab.baseText (applyDiskText/markSaved); setelah pemulihan draf dibaca ulang dari disk dan dipakai hanya bila sidik jari cocok
- [x] Dampak: :feature:code saja (ChangeGutter.kt baru, CodeSession.kt, EditorHost.kt). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (override Java protected `drawLineNumber`/`onCreateRenderer` dari Kotlin)
- [ ] uji perangkat: edit, tambah, hapus baris lalu cek warna; Simpan menghapus semua penanda; undo kembali ke isi awal menghapus penanda; mode word wrap (penanda hanya di baris visual pertama)
- [ ] LANGKAH BERIKUT: scratch file dan Live Template, tombol Play di gutter

## 2026-10-28 - Scratch baru dan Live Template [CODE-TEMPLATE]
- [x] feat(code): menu "Scratch baru": pilih jenis (kt, py, sh, js, java, md, json, html, txt) lalu tab Scratch.<ext> (Scratch 2.<ext>, dst.); highlight, Struktur berkas, dan Play (.sh/.py/.md/.json/.html) langsung jalan karena judul tab dipakai sebagai ekstensi (previewKind)
- [x] feat(code): menu "Live Template": 16 template bawaan per jenis berkas (main, logd, shebang, for, if, log, code, table, task, todo, date, time, now); variabel $CURSOR$ $SELECTION$ $DATE$ $TIME$ $DATETIME$ $FILE$; seleksi diganti/dibungkus, indent mengikuti baris kursor, kursor mendarat di $CURSOR$
- [x] "Simpan seleksi..." membuat template sendiri (singkatan huruf/angka/_/-, maks. 4000 karakter) disimpan di live_templates.json folder draf privat; bisa dihapus dari daftar. Muncul juga di Palet Perintah karena memakai menuGroups
- [x] Dampak: :feature:code saja (LiveTemplates.kt baru; CodeSession.kt, OverflowMenu.kt, CodeScreen.kt, PreviewState.kt). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (Compose dialog baru, ed.text.replace + indexer.getCharPosition diverifikasi dari source sora-editor 0.23.6)
- [ ] uji perangkat: Scratch.py lalu Play; Live Template main di .kt dan .py; seleksi + template code di .md; template buatan sendiri bertahan setelah force-stop; indent di dalam blok
- [ ] KEPUTUSAN: menutup scratch berisi tetap memunculkan konfirmasi Simpan/Buang/Batal (aman dari kehilangan data). Opsi lanjutan: scratch tanpa konfirmasi
- [ ] LANGKAH BERIKUT: tombol Play di gutter (baris shebang/main), Ganti di semua berkas

## 2026-10-28 - Tombol Play di gutter [CODE-PLAYGUTTER]
- [x] feat(code): untuk tab skrip (.sh/.py atau shebang) nomor baris 1 diganti segitiga Play hijau; saat skrip berjalan jadi kotak Stop merah. Ketuk gutter baris 1 = tombol Play toolbar (`session.playActive()`)
- [x] Sentuhan di area gutter baris 1 dikonsumsi `MolinaCodeEditor.onTouchEvent` (toleransi touch slop), sentuhan lain diteruskan ke editor seperti biasa; batas gutter dari `measureTextRegionOffset()` (diverifikasi dari source sora-editor 0.23.6)
- [x] Dampak: :feature:code saja (ChangeGutter.kt, EditorHost.kt). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (override onTouchEvent, import PreviewKind di EditorHost)
- [ ] uji perangkat: Play muncul di .sh/.py dan tidak di .kt/.md; ketuk menjalankan skrip, ikon jadi Stop; ketuk lagi menghentikan; scroll/ketuk di baris lain tetap normal
- [ ] KETERBATASAN: hanya baris 1 (belum baris `main`/`if __name__`)
- [ ] LANGKAH BERIKUT: Ganti di semua berkas (Replace in Files)

## 2026-10-28 - Ganti di semua berkas (Replace in Files) [CODE-REPLACE]
- [x] feat(code): dialog "Cari di berkas" punya tombol "Ganti": kolom pengganti, lalu "Ganti semua..." membuka pratinjau per berkas (jumlah kemunculan, baris contoh - sebelum / + sesudah) sebelum "Terapkan"
- [x] Aturan aman: penggantian per baris (EOL tidak disentuh); mode regex mendukung $0..$9, kecocokan kosong tidak diganti; berkas bukan UTF-8 murni, biner, > 1 MB, dan berkas dengan tab belum disimpan DILEWATI dan dilaporkan; maks. 200 berkas per operasi
- [x] Terapkan menulis atomik (FileStore.writeUtf8Atomic) setelah memastikan isi disk sama dengan saat pratinjau; tab bersih yang terbuka dimuat ulang (doReload), panel direktori disegarkan
- [x] Urungkan (menu "Urungkan ganti di berkas" dan tombol di dialog): memulihkan isi lama di memori (maks. 10 juta karakter), berkas yang berubah lagi setelah ganti dilewati
- [x] Dampak: :feature:code saja (ReplaceInFiles.kt baru; FindInFiles.kt, CodeSession.kt, OverflowMenu.kt). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sedang (Compose dialog bersarang, Regex.replace dengan lambda)
- [ ] uji perangkat: ganti kata di 2-3 berkas uji lalu cek git diff; regex dengan $1; tab dirty dilewati; Urungkan memulihkan; tab terbuka ikut ter-refresh
- [ ] KETERBATASAN: Urungkan hilang saat aplikasi dimatikan (andalkan git diff / git checkout); pencarian hanya baris tunggal
- [ ] LANGKAH BERIKUT: tombol Play di baris main/if __name__, Command Palette registry aksi tunggal


## 2026-10-28 - Redesain UI Player mpv: gaya TV digital + tema hitam [PLAYER-TV-BLACK]
- [x] style(mpv): panel kontrol hitam pekat dengan garis aksen amber, tombol kotak-bulat ala remote, seekbar tipis + thumb batang, jam monospace
- [x] Tema biru diganti hitam: accent/playlist aktif/fokus D-pad jadi amber #E6B450; bg_channel_selected hitam-amber
- [x] Dampak: HANYA res (drawable, colors.xml, player.xml). ID view, Kotlin, Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah (resource XML saja)
- [ ] uji perangkat: kontras kontrol di atas video terang, fokus D-pad, tombol disabled prev/next, PiP, mode kunci

## 2026-10-28 - Player mpv: signage Molina Cinema + jam digital + judul condensed [PLAYER-TV-BANNER]
- [x] style(mpv): baris status baru di panel kontrol (tvStatusRow): label "MOLINA CINEMA" amber + TextClock HH:mm (update otomatis, tanpa Kotlin)
- [x] Judul (video & audio) memakai font condensed tebal ala banner channel
- [x] Dampak: HANYA layout/player.xml. Baris status ditaruh di luar controls_title_group (viewGroupReorder menyembunyikan anak tak terdaftar). Gradle, Manifest, JNI TIDAK disentuh
- [ ] BELUM dikompilasi (CI). Risiko: rendah
- [ ] uji perangkat: baris status ikut fade bersama panel, jam akurat, mode audio, layar kecil/portrait
- [ ] LANGKAH BERIKUT: palet Explorer Cinema (MolinaTheme.Explorer) dan CinemaResults.kt masih biru Win7; migrasi ke hitam-emas bila diinginkan

## 2026-10-28 - Molina Cinema: tile Tautan diganti Saluran (playlist M3U/IPTV) [CINEMA-SALURAN]
- [x] feat(cinema): tile "Tautan" (duplikat kolom cari) diganti "Saluran": dialog bergaya daftar channel TV (nomor 001.., jam, Cari teks/grup/nomor, Prev/Next, Muat, Hapus), baris hitam + aksen emas
- [x] M3uParser (core:common, murni Kotlin): EXTINF/group-title/tvg-name/EXTGRP, koma dalam kutip, BOM, dedupe, maks 5000, nama dibersihkan (kontrol + bidi), URL wajib stream jarak jauh + lolos UrlPolicy, playlist HLS tunggal dikenali
- [x] IptvFetch (core:common): hanya https (app tanpa cleartext), pengalihan diikuti manual dan DIPERIKSA ULANG tiap lompatan, batas 5 MB, timeout 15/20 dtk
- [x] IptvPlayback: saluran terpilih jadi item pertama playlist memory:// (maks 300 entri / 200 ribu karakter demi batas Binder); prev/next di pemutar = ganti saluran. Memakai CinemaActions.playTree, validasi MPVActivity/MpvLoad tidak diubah
- [x] Sumber playlist (maks 10) di SharedPreferences molina_ui/cinema_iptv; cache hasil baca selama proses hidup; label = host (token di URL tidak ditampilkan)
- [x] Diuji di JVM (kotlinc + server HTTP lokal): 22 pengecekan lolos untuk parser, fetch, pengalihan, batas ukuran, kebijakan URL, rotasi playlist
- [x] Dampak: :core:common (+2 file), :app cinema (+2 file; CinemaActions, CinemaWin7Home, CinemaHome diubah). Gradle, Manifest, JNI mpv, MPVActivity TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: sedang (Compose dialog baru)
- [ ] uji perangkat: tambah URL https .m3u, muat, filter/nomor, ketuk saluran lalu prev/next di pemutar, http ditolak dengan pesan, hapus sumber, putar ulang layar
- [ ] KETERBATASAN: .m3u http tidak bisa dimuat (cleartext diblokir; tempel di kolom cari lalu Putar); #EXTVLCOPT (referer/user-agent) diabaikan; daftar tidak di-cache ke disk; prev dari saluran pertama tidak berputar
- [ ] LANGKAH BERIKUT: cache disk + offline, logo tvg-logo, favorit saluran, palet Explorer/CinemaResults (masih biru) ke hitam-emas

## 2026-10-28 - Otomasi: periksa pembaruan 24 jam/Wi-Fi, lencana yt-dlp, cadangan harian [AUTO-UPDATE-BACKUP]
- [x] fix(settings): hapus import matchParentSize di Win7FrameUi.kt (member BoxScope)
- [x] feat(automation): `AutoPrefs`, `AutoWork`, `UpdateCheckWorker` (WorkManager periodik 24 jam, hanya Wi-Fi/UNMETERED, baterai tidak lemah, tunda awal 15 menit). Tanpa notifikasi: hasil hanya mengubah lencana
- [x] feat(automation): `UpdateChecker` (apt: perintah CheckUpdates di jalur Lane.AUTO; yt-dlp: --version terpasang vs tag rilis GitHub), `ProotScriptExecutor.Lane.AUTO` agar tidak memblokir editor/unduhan
- [x] feat(settings): lencana biru Aero (`W7Badge(aero)`) di ubin Paket dan baris yt-dlp bila ada rilis lebih baru; hilang setelah `YtdlpInstaller` sukses (`AutoPrefs.setYtdlpInstalled`) atau yt-dlp dihapus
- [x] feat(settings): halaman Settings > Sistem > Otomasi dan pembaruan (`AutomationPage`): sakelar pembaruan otomatis, Periksa sekarang, status apt/yt-dlp, sakelar cadangan harian
- [x] feat(backup): `AutoBackupWorker` (harian, simpan molina-backup.prev.json, pulihkan berkas lama bila hasil baru < 50% ukuran). Sakelar otomasi ikut `BackupManager` (molina_auto)
- [x] Dampak: Gradle (androidx.work:work-runtime-ktx:2.9.1), AndroidManifest hasil merge menambah RECEIVE_BOOT_COMPLETED dari WorkManager. JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (`BadgeOverlay` memakai `Modifier.align` di dalam Box, `suspendCancellableCoroutine` + RunListener, WorkManager 2.9.1 vs compileSdk 35)
- [ ] uji perangkat (Android 8): Sistem > Otomasi dan pembaruan > Periksa sekarang > status apt/yt-dlp terisi, lencana merah di Sistem bila ada paket, lencana biru di Paket bila yt-dlp usang; Perbarui yt-dlp > lencana hilang; matikan sakelar > pekerjaan dibatalkan; nyalakan cadangan harian > cek molina-backup.json dan .prev.json
- [ ] LANGKAH BERIKUT: pilih bagian yang dipulihkan, ukuran unduhan di fase Mengunduh paket, ubin Settings dengan ikon status

## 2026-10-28 - Lanjutkan terakhir + unduhan masuk Cinema [RESUME-POSTDL]
- [x] feat(cinema): kartu "Lanjutkan" di beranda Molina Cinema (item pertama Terakhir diputar, emas), satu ketukan memutar lagi
- [x] feat(settings): Settings > Sistem > Otomasi > Produktivitas: sakelar "Lanjutkan dari posisi terakhir" memakai kunci save_position yang sama dengan Setelan pemutar (`ResumeBridge`), MpvPrefs mendengarkan perubahan sehingga langsung berlaku
- [x] feat(download): `PostDownload.onFinished` dipanggil `DownloadService.finishWith`: video/audio yang berhasil diunduh masuk Terakhir diputar Cinema bila sakelar "Unduhan masuk ke Cinema" menyala (default mati, ikut Cadangan gabungan)
- [x] Dampak: :app saja (cinema/ResumeBridge, automation/PostDownload baru; AutoPrefs, AutomationUi, CinemaWin7Home, DownloadService, BackupManager diubah). Gradle, Manifest, JNI mpv TIDAK disentuh. WAJIB setelah patch_molina_auto.sh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah-sedang (`background(Color, RoundedCornerShape)`, `var ... by remember` di AutomationPage)
- [ ] uji perangkat (Android 8): putar video, keluar di tengah, nyalakan Lanjutkan, buka Cinema, ketuk kartu > lanjut dari posisi terakhir; nyalakan Unduhan masuk ke Cinema, unduh video, cek Terakhir diputar
- [ ] KETERBATASAN: posisi hanya diingat bila save_position menyala; berkas di dalam rootfs Debian (bukan /sdcard) tidak ditambahkan

## 2026-10-28 - Selaraskan status warna direktori [DIR-STATUS-COLOR]
- [x] fix(term): prompt hijau hanya di home internal /sdcard/Molina (+ subfolder); semua lokasi lain merah, termasuk project /sdcard/molina-studio dan ~
- [x] Alias /storage/emulated/0, /storage/self/primary, /mnt/sdcard dinormalkan ke /sdcard sebelum dicocokkan
- [x] Dampak: assets/molina/bashrc saja. Gradle, Manifest, JNI mpv TIDAK disentuh. Berlaku di sesi terminal baru
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah
- [ ] uji perangkat: cd /sdcard/Molina hijau; cd /storage/emulated/0/Molina hijau; cd /sdcard/molina-studio merah; cd ~ merah

## 2026-10-28 - Saluran Cinema: muat playlist dari berkas .m3u di folder IPTV Molina [IPTV-LOCAL-M3U]
- [x] feat(core): `IptvLocal` (:core:common, JDK murni): `list(dir)` daftar .m3u/.m3u8 (tanpa subfolder, maks 100, urut nama) dan `load(dir, file)` membaca berkas (maks 5 MB, UTF-8, BOM aman) lewat `M3uParser` + `IptvFetch.interpret`, jadi kebijakan URL saluran sama dengan playlist dari internet
- [x] Keamanan jalur: hanya ANAK LANGSUNG folder IPTV setelah canonicalFile (".." , subfolder, dan symlink keluar folder ditolak); jalur yang tersimpan di prefs tidak dipercaya
- [x] feat(cinema): dialog Saluran punya chip "Berkas": panel daftar berkas di /sdcard/Molina/Media/IPTV (nama + ukuran), ketuk = simpan sebagai sumber + muat. Sumber berkas disimpan sebagai jalur absolut di sumber playlist yang sama (maks 10), chip berlabel nama berkas, Muat ulang membaca ulang berkas, Hapus hanya menghapus sumber (berkas tidak dihapus)
- [x] Uji JVM: scripts/core-tests/run_iptv_local.sh (20 pengecekan: filter ekstensi, subfolder, BOM, batas 5 MB/100 berkas, traversal, symlink, HLS, kosong)
- [x] Dampak: :core:common (+IptvLocal.kt), :app cinema (CinemaChannels, IptvSources). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah-sedang (`when` tanpa subjek berisi composable, smart cast `val list`, import verticalScroll/AppPaths)
- [ ] uji perangkat (Android 8): salin .m3u ke /sdcard/Molina/Media/IPTV, buka Saluran > Berkas > ketuk berkas > saluran tampil; tutup lalu buka lagi > sumber berkas termuat otomatis; ubah isi berkas lalu Muat > terbaca ulang; folder kosong/izin ditolak > pesan; Hapus > chip hilang, berkas tetap ada
- [ ] KETERBATASAN: hanya entri stream jarak jauh (http/https/rtsp/rtmp) yang jadi saluran; entri jalur lokal (mis. /sdcard/video.mp4) dilewati kebijakan URL. Subfolder tidak dipindai. Encoding selain UTF-8 bisa menampilkan nama rusak
- [ ] LANGKAH BERIKUT: pemindai subfolder opsional, tombol "Buka folder IPTV" di Penjelajah, cache disk + offline, favorit saluran

## 2026-10-28 - Code: ikon X tab ke tepi kanan [CODE-TABCLOSE-EDGE]
- [x] style(code): `TabItem` di CodeScreen.kt, baris tab `fillMaxWidth()` + judul `weight(1f)`; ikon X kini di tepi kanan tab, bukan menempel di judul. Lebar tab kini seragam (maks 180dp)
- [x] Dampak: CodeScreen.kt dua baris saja. Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI). Risiko: sangat rendah
- [ ] uji perangkat (Android 8): X di tepi kanan tab aktif dan tidak aktif; judul panjang terpotong "…" tanpa menimpa X; buka banyak tab, bilah tab tetap bisa digeser; ketuk X menutup tab

## 2026-10-28 - Saluran Cinema: tombol "Buka folder IPTV" (Penjelajah langsung di folder IPTV) [IPTV-OPEN-FOLDER]
- [x] feat(cinema): panel Berkas di dialog Saluran punya tombol "Buka folder IPTV": membuka Penjelajah Molina (`Win7OpenDialog(browse = true, startDir = /sdcard/Molina/Media/IPTV)`) di atas dialog Saluran, pola sama dengan UnduhanScreen. Folder dibuat bila belum ada
- [x] Saat Penjelajah ditutup, daftar berkas di panel disegarkan otomatis (`refreshTick`), jadi .m3u yang baru disalin/dipindah langsung muncul
- [x] Dampak: :app cinema (CinemaChannels, CinemaHome). Memakai MolinaExplorer yang sudah ada. Gradle, Manifest, JNI mpv, :core:common TIDAK disentuh. Wajib setelah patch_iptv_local.py
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah (parameter baru berdefault, `iptvTick++` pada state delegasi)
- [ ] uji perangkat (Android 8): Saluran > Berkas > Buka folder IPTV > Penjelajah terbuka di folder IPTV; salin/tempel .m3u ke sana, tutup > daftar berkas ikut terbarui tanpa Segarkan; ketuk berkas > saluran termuat

## 2026-10-28 - Saluran Cinema: poles tombol Folder IPTV, petunjuk folder kosong, Buka di Code [IPTV-POLISH]
- [x] style(cinema): tombol "Buka folder IPTV" jadi "Folder IPTV" dengan ikon folder kuning (`TabFolderIcon` + `MolinaTheme.Explorer.Folder`), sejajar bahasa visual Penjelajah
- [x] Petunjuk folder kosong kini mengarahkan ke tombol Folder IPTV (salin playlist, lalu tutup Penjelajah; daftar tersegarkan otomatis)
- [x] feat(cinema): tiap berkas di panel Berkas punya tombol "Code" yang menutup dialog Saluran lalu membuka berkas di tab Code (`MolinaExplorer.openInCode` -> `CodeNav.pendingFiles`). Penjelajah IPTV juga meneruskan `onOpenInCode`, jadi menu "Buka di Code" dan ketuk ulang berkas teks bekerja
- [x] Dampak: :app cinema (CinemaChannels, CinemaHome). Gradle, Manifest, JNI mpv, :core:common TIDAK disentuh. Wajib setelah patch_open.py
- [ ] BELUM dikompilasi Gradle (CI). Risiko: rendah (parameter baru berdefault, `onCode?.let { fn -> { fn(e.file) } }`, `Box(Modifier.size(20.dp)) { TabFolderIcon }`)
- [ ] uji perangkat (Android 8): Saluran > Berkas > ikon folder kuning + "Folder IPTV" terbuka di folder IPTV; folder kosong tampil petunjuk baru; ketuk "Code" pada sebuah .m3u > pindah ke tab Code dengan berkas terbuka; di Penjelajah IPTV ketuk ulang .m3u > terbuka di Code
- [ ] KETERBATASAN: setelah pindah ke Code, kembali ke Cinema dialog Saluran sudah tertutup; ketuk berkas lain di baris (bukan "Code") tetap memuat saluran

## 2026-10-28 - Hasil pencarian Cinema: urut waktu, saring judul+kanal, kanal terkait [SEARCH-SORT-RELATED]
- [x] feat(core): `SearchOrder` (:core:common, JDK murni) + `SearchSort` {Relevansi, Terbaru, Terlama}; `RelativeTime.estimateAtMs` memperkirakan waktu unggah dari uploadedAtMs atau teks "3 days ago" (bulan 30 hari, tahun 365 hari). Pengurutan stabil, hasil tanpa tanggal selalu di akhir
- [x] feat(cinema): dialog Hasil pencarian punya chip Relevansi/Terbaru/Terlama (bawaan Terbaru), nomor 001.. mengikuti urutan tampil, catatan "N hasil tanpa tanggal ditaruh di akhir"
- [x] feat(cinema): kolom Cari kini menyaring judul DAN nama kanal; baris chip "Terkait" menampilkan kanal yang muncul >= 2 kali, ketuk = saring daftar dengan kanal itu, ketuk lagi = lepas
- [x] Playlist pemutar mpv kini mengikuti urutan yang tampil di dialog (`onPick(list, item)` -> `CinemaActions.playResults(ctx, list, r)`), jadi dialog playlist di pemutar ikut terurut
- [x] Uji JVM: scripts/core-tests/run_search_order.sh (21 pengecekan: estimasi waktu, urut baru/lama, stabil, tanpa tanggal di akhir, saring, kanal terkait)
- [x] Dampak: :core:common (+SearchOrder.kt, RelativeTime.kt), :app cinema (CinemaResults, CinemaHome). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah-sedang (`related.forEach { (name, n) -> }`, `SearchSort.values()`, `horizontalScroll(rememberScrollState())` di dalam `if`)
- [ ] uji perangkat (Android 8): cari kata di beranda > chip Terbaru aktif, video terbaru di nomor 001; ketuk Terlama > urutan terbalik; ketuk chip kanal > daftar tersaring; putar item > dialog playlist di pemutar berurutan sama
- [ ] KETERBATASAN: sumber yt-dlp mode flat sering tanpa tanggal, hasilnya masuk "tanpa tanggal" di akhir; tanggal teks YouTube ("3 weeks ago") kasar sehingga hasil sewaktu mengikuti urutan relevansi. Pencarian di dialog playlist PEMUTAR (layout XML) belum diberi chip urutan/kanal

## 2026-10-28 - Hasil pencarian Cinema: saran "Cari juga" + kanal terkait maks 3 [SEARCH-RELATED-QUERIES]
- [x] feat(core): `SearchOrder.relatedQueries(items, query)` menurunkan saran pencarian dari judul hasil (kata/dua kata berurutan yang ada di >= 2 judul, tanpa kata kueri dan kata umum; hasil berupa kueri utuh "kueri + frasa"). Tanpa panggilan jaringan baru
- [x] style(core): `SearchOrder.related` (kanal terkait) batas bawaan 4 -> 3
- [x] feat(cinema): baris chip "Cari juga" di dialog Hasil pencarian; ketuk = isi kolom pencarian beranda dan cari ulang (`onSearch` -> `search(s)`); Home menyimpan `lastQuery`
- [x] Uji JVM: scripts/core-tests/run_search_order.sh bertambah 6 pengecekan (frasa/kata, batas, kueri kosong, tanpa pengulangan, daftar kosong, kanal maks 3)
- [x] Dampak: :core:common (SearchOrder.kt), :app cinema (CinemaResults, CinemaHome). Gradle, Manifest, JNI mpv TIDAK disentuh. Wajib setelah patch_search_sort.py
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah (parameter baru berdefault, smart cast `onSearch` di lambda)
- [ ] uji perangkat (Android 8): cari "lagu" > chip "Cari juga" muncul (mis. "lagu terbaru"); ketuk > hasil dimuat ulang dan kolom beranda terisi; kanal terkait tampil maks 3
- [ ] KETERBATASAN: saran dari judul hasil saja (bukan sugesti mesin pencari), jadi kualitasnya mengikuti judul; kata umum yang belum ada di daftar kata henti bisa muncul

### Rencana Modul Settings (Molina Hub) - revisi
- [x] Koreksi: repo satu app (com.molina.studio); modul :molina-core dihapus (tidak dipakai, duplikat prefs)
- [ ] Kartu status Molina Hub di Home Settings (apt, yt-dlp, Debian, backup)
- [ ] About lengkap: SHA build, ABI, API, versi libmpv/yt-dlp, lisensi, salin info debug
- [ ] Tile Pemutar/Cinema (resume, IPTV, decoder)
- [ ] Ekspor laporan bug gabungan (crash + log + info sistem)
- [ ] Shortcut dinamis: Putar terakhir, Unduhan, Penyimpanan

## 2026-10-28 - Settings: kartu Status Molina + About lengkap [HUB-STATUS]
- [x] feat(settings): kartu "Status Molina" di beranda Settings (Debian, paket apt, yt-dlp, cadangan, pembaruan otomatis); ketuk baris membuka halaman terkait. Memakai data yang sudah ada (RootfsManager, AptHistory, AutoPrefs), tanpa pemeriksaan/jaringan baru
- [x] feat(settings): About lengkap (versi + kode, waktu pembaruan, Android/API, perangkat, ABI, status ringkas), tombol "Salin info debug", dan atribusi lisensi (Termux, PRoot, Debian, mpv-android, sora-editor)
- [x] BackupManager.lastBackupMs(ctx): waktu ubah berkas cadangan (cadangan manual ikut terbaca, bukan hanya otomatis)
- [x] Dampak: :app settings (HubStatusUi.kt, AboutUi.kt baru; SettingsScreen, BackupManager). Gradle, Manifest, JNI mpv TIDAK disentuh. Tanpa URL baru (aman untuk telemetry_gate)
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah (`when` atas RootfsState sealed, `remember { a; b; c }`)
- [ ] uji perangkat (Android 8): beranda Settings > kartu Status Molina tampil di atas ubin; ketuk baris Debian/yt-dlp/Cadangan > pindah halaman; About > ketuk "Salin info debug" > tempel di catatan
- [ ] KETERBATASAN: versi libmpv dan SHA commit build belum ditampilkan (SHA butuh buildConfig di Gradle; versi mpv hanya tersedia dari log mpv)

## 2026-10-28 - Settings: ubin Pemutar + laporan bug gabungan dan per kelompok [HUB-PLAYER-REPORT]
- [x] feat(settings): ubin "Pemutar" di beranda Settings > halaman Pemutar: sakelar Ingat posisi putar (ResumeBridge), sakelar Dekoder perangkat keras (HwdecBridge, kunci hardware_decoding), daftar sumber IPTV (ketuk = hapus, URL tidak ditampilkan), jalur Folder IPTV, pintasan ke Setelan pemutar lengkap dan tab Cinema
- [x] feat(log): BugReport.export: satu berkas molina-laporan-bug-<stempel>.txt di Documents/Export berisi (1) ringkasan, (2) PER KELOMPOK (Cinema, Terminal, Sistem, Logcat, Crash; tag terbanyak per kelompok, maks 400 baris terakhir), (3) GABUNGAN kronologis semua sumber, (4) 3 laporan crash terbaru; semua disamarkan LogRedact (URL dan token)
- [x] Tautan: menu titik tiga di halaman Log dan "Laporan bug" di halaman Sistem. Ekspor log lama (Salin, Bagikan, Simpan .txt terfilter) tidak diubah
- [x] Dampak: :app settings (PlayerSettingsUi.kt baru, SettingsScreen), :app cinema (HwdecBridge.kt baru), :app log (BugReport.kt baru, LogScreen). Gradle, Manifest, JNI mpv TIDAK disentuh. Wajib setelah patch_hub_status.py
- [ ] BELUM dikompilasi Gradle (CI): sisi Compose hanya dicek sintaks. Risiko: rendah-sedang (`when` penuh atas enum Page, destrukturisasi Pair di forEach, Path di Canvas)
- [ ] uji perangkat (Android 8): Settings > ubin Pemutar > ubah sakelar, buka video: dekoder mengikuti; ketuk sumber IPTV > hapus; Log > titik tiga > Laporan bug > buka berkas di Documents/Export: bagian 1-4 ada, URL tersamarkan
- [ ] KETERBATASAN: sakelar dekoder berlaku pada video berikutnya (bukan yang sedang diputar); log hanya sejak proses hidup (buffer 1000 baris); folder Export terbaca aplikasi lain, jadi tinjau berkas sebelum dibagikan

## 2026-10-28 - Shortcut dinamis ikon: Putar terakhir, Unduhan, Penyimpanan [SHORTCUT-DYNAMIC]
- [x] feat(shortcut): `MolinaShortcuts` (ShortcutManager.setDynamicShortcuts, thread latar) menerbitkan "Putar terakhir" (hanya bila riwayat Cinema tidak kosong), "Unduhan", dan "Penyimpanan"; dipanggil saat MainActivity dibuat dan setiap riwayat Cinema berubah (simpan/hapus)
- [x] Privasi: intent hanya memuat nama tujuan (`molina_open_tab` = play_last | downloads | storage); URL dan judul riwayat TIDAK masuk shortcut (bisa dibaca launcher). Tujuan "play_last" diselesaikan saat diketuk lewat `CinemaActions.playLast`
- [x] MainActivity.handleOpenTabIntent: tujuan baru downloads (DownloadNav.openTab), storage (SettingsNav.openStorage > halaman Penyimpanan), play_last (putar item teratas riwayat, pindah ke tab Cinema)
- [x] Ikon baru (adaptive, pola ic_shortcut_code): ic_shortcut_last (lavender), ic_shortcut_downloads (biru), ic_shortcut_storage (hijau); string di values/molina_shortcuts_dynamic.xml
- [x] Dampak: :app (MolinaShortcuts.kt, res baru, MainActivity, CinemaRecent, CinemaActions, SettingsScreen). molina_shortcuts.xml, AndroidManifest.xml, Gradle, JNI mpv TIDAK disentuh; 3 shortcut statis (Cinema, Terminal, Code) tetap
- [ ] BELUM dikompilasi Gradle (CI): risiko rendah-sedang (`Icon.createWithResource` dengan adaptive-icon mipmap, `setDynamicShortcuts`, `when` mengembalikan Int di handleOpenTabIntent)
- [ ] uji perangkat (Android 8): putar video > tekan lama ikon > "Putar terakhir" muncul dan memutar ulang; Settings > Pemutar/Cinema hapus riwayat > shortcut itu hilang; ketuk "Unduhan" dan "Penyimpanan" membuka halaman masing-masing (saat app mati maupun hidup)
- [ ] KETERBATASAN: launcher umumnya hanya menampilkan 4 shortcut dan menaruh yang statis lebih dulu; dengan 3 statis, hanya 1 dinamis (Putar terakhir) yang pasti terlihat. Kuota total dibatasi `maxShortcutCountPerActivity` dikurangi shortcut statis

## 2026-10-28
- [ANALISIS] Files Manager: dialog ZIP, tombol "Tutup" terpotong ("Tut") dan baris tombol tidak sejajar.
- Rencana: header dengan ikon ⓘ/✕, teks bantuan dipindah ke bottom sheet, aksi sekunder ke menu ⋮, tombol berbobot seragam.
- Status: menunggu source dialog ZIP untuk patch.

## 2026-10-28 - Files Manager: perbaiki dialog ZIP, tombol "Tutup" terpotong [ZIP-LAYOUT]
- [x] fix(files): penyebab tombol terpotong = Win7Button berlebar min 76dp + padding 14dp tanpa weight, 4 tombol di baris kedua melebihi lebar dialog
- [x] fix(files): Win7Button dapat parameter `compact` (1 baris, padding 4dp, tinggi 32dp); dua baris tombol dialog ZIP (3 + 4) memakai Modifier.weight(1f)
- [x] refactor(files): teks bantuan panjang dipindah ke panel "Cara memakai ZIP" (tautan Bantuan); ringkasan jadi 1 baris; "Yakin hapus?" -> "Yakin?"
- [x] Dampak: :app cinema (Win7Aero.kt, Win7OpenDialog.kt). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko rendah
- [ ] uji perangkat (Android 8): Berkas > buka ZIP > semua label tombol utuh (termasuk "Tutup"); ketuk Bantuan membuka panel dan Tutup menutupnya; Hapus dua kali tetap meminta konfirmasi

## 2026-10-28 - Brand: ikon aplikasi kembali ke logo terminal [BRAND-TERMINAL-ICON]
- [x] fix(brand): AndroidManifest android:icon/roundIcon dari ic_cinema_launcher (bola Aero + play, commit ec6e2bc) ke ic_launcher / ic_launcher_round (jendela terminal + prompt emas)
- [x] feat(brand): ic_launcher_foreground: sisakan 1 titik bar judul (sebelumnya 3) dan tambah lencana play kecil di kanan bar judul sebagai penanda Cinema; ic_launcher_monochrome mendapat lencana yang sama
- [x] Shortcut tidak diubah: Cinema tetap ic_cinema_launcher, Terminal tetap ic_launcher; aset ic_cinema_launcher_* dipertahankan
- [x] Dampak: AndroidManifest.xml, 2 drawable vektor. Gradle dan JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko sangat rendah
- [ ] uji perangkat (Android 8): pasang ulang APK, ikon launcher berupa terminal dengan segitiga kecil di bar judul; tekan lama ikon: Cinema tetap bola Aero
- [ ] KETERBATASAN: launcher dapat menyimpan ikon lama, perlu pasang ulang atau bersihkan cache launcher

## 2026-10-28 - Settings > Monitor Sistem: koneksi tidak realtime [MONITOR-REALTIME]
- [x] fix(monitor): SystemStats.flow ditulis ulang sebagai channelFlow; pemindaian PID (3 dtk) dan PSS (5 dtk) pindah ke coroutine terpisah sehingga emisi tiap detik tidak tertunda
- [x] fix(monitor): counter jaringan dibaca tepat saat dt diambil; jika getUidRxBytes tidak didukung, jatuh ke total perangkat dan StatsSnapshot.netPerUid=false memunculkan peringatan di UI
- [x] feat(monitor): grafik riwayat 60 detik untuk unduh dan unggah (skala logaritmik) di SystemMonitorUi
- [x] Dampak: :app terminal (SystemStats.kt), :app settings (SystemMonitorUi.kt). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko rendah-sedang (channelFlow + launch + Atomic*)
- [ ] uji perangkat (Android 8): Settings > Monitor Sistem saat unduh/streaming: meter dan grafik bergerak tiap detik; amati apakah peringatan kuning muncul
- [ ] KETERBATASAN: counter per-UID Android 8 bisa diperbarui kasar oleh sistem; opsi lanjutan = cuplikan 500 ms + rata-rata 2 sampel

## 2026-10-28 - Script Execution: analisis peningkatan produktivitas [SCRIPT-ENHANCE]
- [ANALISIS] Skrip belum ikut cadangan (BackupManager tanpa ScriptStore, allowBackup=false); hanya bisa dijalankan dari Settings, selalu dengan konfirmasi; tanpa favorit/cari/urutan dan tanpa argumen
- Rencana paket A: cadangan skrip, pintasan layar utama (requestPinShortcut, intent hanya id), pemilih Skrip di Terminal
- Rencana paket B: favorit/cari/urutan, argumen + saklar percaya, status terakhir (exit code)
- Status: menunggu persetujuan paket

## 2026-10-28 - Code: Jalankan di Terminal untuk .py memakai dialog [CODE-PYRUN]
- [x] feat(code): menu "Jalankan di Terminal" pada tab .py membuka dialog: (1) heredoc `python3 << 'PYEOF'` ... `PYEOF` (pembatas otomatis PYEOF_1 dst. bila isi bentrok), (2) `python3 nama.py` (pengguna cukup isi nama; .py dideteksi/ditambah otomatis)
- [x] feat(code): sumber heredoc = seleksi bila ada, selain itu seluruh isi tab (sebelumnya hanya seleksi atau baris kursor); tab non-.py tetap memakai alur lama
- [x] feat(code): mode berkas menulis isi tab ke berkas lalu menjalankan `(cd dir && python3 nama.py)` lewat jalur guest; isi lama dicadangkan, menu baru "Urungkan jalankan Terminal" memulihkan (dilewati bila berkas sudah berubah; berkas baru dihapus)
- [x] test: scripts/code-tests/run_python_run.sh (logika murni PythonTerminalRun)
- [x] Dampak: :feature:code (PythonTerminalRun, TerminalRunUndo, PythonRunDialog, CodeSession, OverflowMenu, CodeScreen), :app (MainActivity, CodeTerminalBridge). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko sedang (Compose dialog baru + 2 anchor di :app)
- [ ] uji perangkat (Android 8): tab .py > titik tiga > Jalankan di Terminal > pilih opsi, cek pratinjau; mode berkas dengan nama yang sudah ada menampilkan peringatan timpa; Urungkan memulihkan isi lama
- [ ] KETERBATASAN: Urungkan hanya di memori (hilang bila proses mati), tidak membatalkan perintah yang sudah dijalankan di Terminal; heredoc memakai stdin sehingga input() tidak berfungsi (pakai mode berkas)

## 2026-10-28 - Code: Jalankan di Terminal untuk .py/.sh, ingat pilihan, langsung [CODE-PYRUN2]
- [x] feat(code): dialog Jalankan di Terminal kini juga untuk .sh (`bash << 'SHEOF'` atau `bash nama.sh`); pembatas bentrok otomatis SHEOF_1 dst.
- [x] feat(code): pilihan terakhir (heredoc/berkas) diingat; kotak "Ingat pilihan ini, langsung jalankan lain kali" melewati dialog; menu baru "Dialog jalankan: tanya dulu/langsung" membalikkannya
- [x] aturan langsung: mode heredoc selalu langsung; mode berkas hanya untuk tab yang sudah punya berkas (tanpa menimpa berkas lain), tab baru tetap membuka dialog untuk memberi nama
- [x] test: scripts/code-tests/run_python_run.sh diperluas (langOf, .sh, pembatas SHEOF)
- [x] Dampak: :feature:code (PythonTerminalRun + enum TerminalRunLang, RunStore baru, PythonRunDialog, CodeSession, OverflowMenu), :app (MainActivity: SharedPreferences molina_code_run). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko sedang (dialog Compose, objek RunStore anonim di MainActivity)
- [ ] uji perangkat (Android 8): .sh dan .py membuka dialog; centang "Ingat pilihan" lalu Jalankan lagi tanpa dialog; menu "Dialog jalankan" mengembalikan dialog; pilihan bertahan setelah aplikasi dibuka ulang

## 2026-10-28 - Pratinjau perintah/skrip seragam di aksi Terminal [CODE-CMDBLOCK]
- [x] refactor(ui): spesifikasi tunggal CommandBlockSpec (monospace 11,5sp, tinggi baris 16sp, sudut 5dp, tepi 1dp, padding 10dp, aturan potong pratinjau) di :feature:code, murni tanpa Compose sehingga bisa diuji JVM
- [x] refactor(ui): komponen CommandBlock di :app (palet W7D) menggantikan blok inline Script Execution (tampilan tetap sama persis) dan teks polos di konfirmasi ToolRunDialog (kini berlatar, bertepi, geser horizontal)
- [x] feat(ui): konfirmasi "Jalankan di Molina Terminal?" (Script Execution) kini menampilkan awal skrip + "(+N baris lagi)"; sebelumnya skrip dijalankan tanpa pratinjau isi
- [x] refactor(code): pratinjau di dialog Jalankan di Terminal memakai spesifikasi yang sama (palet CodeColors)
- [x] test: run_python_run.sh diperluas (clip: pendek, baris panjang, sisa baris, CRLF)
- [x] Dampak: :feature:code (CommandBlockSpec, PythonTerminalRun, PythonRunDialog), :app (CommandBlock baru, ScriptExecutionUi, ToolRunUi). Gradle, Manifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko sedang (Column baru di dialog Script Execution; impor lama jadi tak terpakai hanya peringatan)
- [ ] uji perangkat (Android 8): Settings > Script Execution: kartu skrip tampil sama seperti sebelumnya; ketuk jalankan: dialog menampilkan awal skrip; Paket/alat dengan konfirmasi: blok perintah berlatar gelap
- [ ] KETERBATASAN: Snippet Terminal (daftar baris) dan keluaran log tidak diubah karena berupa daftar/aliran, bukan blok perintah

## 2026-10-28 - Script Execution: analisis peningkatan produktivitas [SCRIPT-ENHANCE]
- [ANALISIS] Skrip belum ikut cadangan (BackupManager tanpa ScriptStore, allowBackup=false); hanya bisa dijalankan dari Settings, selalu dengan konfirmasi; tanpa favorit/cari/urutan dan tanpa argumen
- Rencana paket A: cadangan skrip, pintasan layar utama (requestPinShortcut, intent hanya id), pemilih Skrip di Terminal
- Rencana paket B: favorit/cari/urutan, argumen + saklar percaya, status terakhir (exit code)
- Status: menunggu persetujuan paket

## 2026-10-28 - Script Execution: analisis peningkatan produktivitas [SCRIPT-ENHANCE]
- [ANALISIS] Skrip belum ikut cadangan (BackupManager tanpa ScriptStore, allowBackup=false); hanya bisa dijalankan dari Settings, selalu dengan konfirmasi; tanpa favorit/cari/urutan dan tanpa argumen
- Rencana paket A: cadangan skrip, pintasan layar utama (requestPinShortcut, intent hanya id), pemilih Skrip di Terminal
- Rencana paket B: favorit/cari/urutan, argumen + saklar percaya, status terakhir (exit code)
- Status: menunggu persetujuan paket

## 2026-10-28 - About: versi libmpv/FFmpeg + Code jadi shortcut dinamis [ABOUT-MPV-CODE]
- [x] feat(about): baris "libmpv" dan "FFmpeg" di Versi dan perangkat (ikut Salin info debug). Sumber: MpvVersionNote (:feature:mpv) mencatat properti mpv-version dan ffmpeg-version dari mpv yang berjalan, sekali setelah player.initialize; About membaca lewat MpvVersionBridge (SharedPreferences molina_ui). Sebelum pernah memutar video tampil "belum tercatat (putar video sekali)"
- [x] refactor(shortcut): shortcut Code dipindah dari molina_shortcuts.xml (statis) ke MolinaShortcuts (dinamis, id molina_code_dyn, rank 3). Statis tersisa: Cinema, Terminal. Urutan dinamis: Putar terakhir, Unduhan, Penyimpanan, Code
- [x] Dampak: :feature:mpv (MpvVersionNote.kt baru, satu baris di MPVActivity setelah player.initialize; hanya memanggil MPVLib.getPropertyString yang sudah ada, TANPA mengubah binding JNI/C++), :app (MpvVersionBridge, AboutUi, MolinaShortcuts, molina_shortcuts.xml). Manifest dan Gradle TIDAK disentuh
- [ ] BELUM dikompilasi Gradle (CI): risiko rendah
- [ ] uji perangkat (Android 8): putar video sekali > Settings > About: libmpv dan FFmpeg terisi; tekan lama ikon: Cinema, Terminal, lalu dinamis; ketuk Code membuka tab Code
- [ ] KETERBATASAN: launcher menampilkan sekitar 4 shortcut (statis dulu), jadi Penyimpanan dan Code di bawah lipatan pada launcher 4 slot; shortcut Code lama yang sudah disematkan di layar utama (id molina_code) menjadi nonaktif dan perlu disematkan ulang; SHA commit build belum ditampilkan (butuh buildConfig di Gradle)

## 2026-10-28 - Keyboard tidak lagi muncul sendiri saat app dibuka [KEYBOARD-HIDDEN]
- [x] fix(ui): MainActivity windowSoftInputMode `adjustResize` -> `stateHidden|adjustResize` (AndroidManifest.xml). Penyebab: kolom "Cari di YouTube atau tempel tautan" (OutlinedTextField di CinemaWin7Home.SearchBlock) adalah elemen fokus pertama di tab Cinema; Compose memberi fokus awal ke elemen itu saat jendela mendapat fokus, dan `adjustResize` tanpa state membuat sistem menampilkan keyboard otomatis
- [x] Efek: keyboard tidak otomatis tampil saat app dibuka, dari shortcut, maupun saat kembali dari pemutar; keyboard tetap muncul saat kolom diketuk (pemanggilan eksplisit tidak terpengaruh)
- [x] Dampak: HANYA AndroidManifest.xml (satu atribut). Kode, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi/diuji: perlu build CI lalu uji perangkat (Android 8): buka app dari nol dan dari shortcut > keyboard tidak muncul; ketuk kolom > keyboard muncul; Terminal, Code, dan pencarian di Log tetap bisa mengetik
- [ ] KETERBATASAN: kolom Cinema mungkin masih tampak berfokus (tepi aktif) tanpa keyboard sampai diketuk; diagnosis dari kode, belum direproduksi di perangkat

## 2026-10-28 - Tema grafit netral: frame Win7 biru jadi hitam/abu, aksen biru tetap [THEME-GRAPHITE]
- [x] refactor(ui): tint biru diganti abu netral di MolinaTheme.Explorer (frame, garis, tombol, input, bar), W7D + Win7Scheme (Settings), kaca Win7Glass, header Aero Terminal (TerminalChrome), header/tab/toolbar/dialog Code (CodeWin7, CodeWin7Dialog), border Terminal, baris hasil Cinema, dan latar ikon
- [x] Peta utama: FrameTop 1A2230>1B1B1B, FrameBottom 0A0F16>0A0A0A, FrameLine 2E4A68>3A3A3A, Hairline 26303B>262626, Line 5A6675>5C5C5C, BtnTop/Bottom 1E252E/10151B>202020/111111, Input 0F1318>101010, Bar 14181E/05070A>151515/060606, Ink EAEFF5>EDEDED, TextDim 9AA5B1>A3A3A3; header Aero 3B6EA5/244A75/14263F>4A4A4A/262626/111111; kaca 4A7DB5/244A75/0F1F34>5A5A5A/2A2A2A/101010; tepi kaca 99BFE0FF>99D0D0D0
- [x] TIDAK diubah (aksen A): Accent 6CB4FF, NavHeader 8FC4EE, SelFill 12263A, SelLine 3C7FB1, tombol default 4DA3FF, rail/garis tab Code 8FC4EE, warna status, ikon jenis berkas, sapuan RGB, ANSI Terminal, MolinaTheme.Background/CardBg/CardLine/Muted, themes.xml, ikon peluncur, CodeColors
- [x] Dampak: hanya literal warna di berkas UI Kotlin (:app dan :feature:code). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi/dilihat di perangkat: periksa kontras teks di header Terminal/Code dan tepi kaca; kembalikan dengan git revert commit ini bila tidak cocok
- [ ] LANJUTAN (opsional): opsi B (aksen emas) dan penyatuan W7D ke MolinaTheme.Explorer (semantik nama berbeda: W7D.Line = Explorer.Field)

## 2026-10-28 - Script Execution paket A: cadangan, pintasan layar utama, pemilih di Terminal [SCRIPT-PACK-A]
- [x] feat(backup): skrip Script Execution ikut molina-backup.json (kunci "scripts"). ScriptStore.snapshot() dan mergeFrom(): gabung tanpa menimpa (judul+isi sama dilewati), id tidak aman/bentrok diganti id baru (id dipakai di nama berkas), batas 50 skrip dihormati. Teks BackupUi diperbarui
- [x] feat(shortcut): tombol PINTASAN per skrip memasang pinned shortcut (ScriptShortcuts.pin, requestPinShortcut). Intent hanya membawa id (molina_run_script); MainActivity menampilkan dialog konfirmasi lewat ScriptNav.confirmRunId + ScriptRunHost. Hapus skrip menonaktifkan pintasannya
- [x] feat(term): menu titik tiga Terminal > "Pilih skrip": daftar urut terakhir dijalankan, kolom cari bila lebih dari 6 skrip, ketuk baris membuka konfirmasi lalu jalan
- [x] refactor: dialog konfirmasi diekstrak jadi ScriptRunDialog (dipakai Settings, pemilih, pintasan) sehingga tidak ada jalur tanpa konfirmasi
- [x] branding: segitiga jalankan di Script Execution kini emas (MolinaTheme.Gold)
- [x] Dampak: hanya :app (ScriptStore, ScriptRunner/ScriptNav, BackupManager, BackupUi, ScriptExecutionUi, MainActivity, TerminalChrome, file baru ScriptPickerUi.kt dan ScriptShortcuts.kt). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji (Android 8): Backup lalu hapus skrip lalu Pulihkan; PINTASAN lalu sematkan lalu ketuk dari layar utama: dialog muncul; Terminal > titik tiga > Pilih skrip
- [ ] KETERBATASAN: Activity bisa menerima extra molina_run_script dari app lain, tetapi hanya memunculkan konfirmasi (tidak jalan diam-diam). Judul pintasan tidak ikut berubah bila skrip di-rename; pintasan lama dari skrip yang dihapus hanya dinonaktifkan. Paket B (favorit/cari/urutan, argumen + saklar percaya, status exit) belum dikerjakan

## 2026-10-28 - Fitur baru: Statistik tontonan (lokal) [WATCH-STATS]
- [x] feat(cinema): WatchStats (:feature:mpv) menghitung waktu putar AKTIF (dijeda tidak dihitung) per hari dan per judul; disimpan di filesDir/watch-stats.json (atomik, thread latar, maks 120 hari dan 200 judul). Judul URL disamarkan LogRedact. Tanpa jaringan, tanpa telemetri
- [x] Hook di MPVActivity: attach (onCreate), pause (eventProperty Boolean), START_FILE, FILE_LOADED, END_FILE, flush (onDestroy). Hanya Kotlin, JNI/C++ mpv TIDAK disentuh
- [x] feat(settings): ubin dan halaman "Statistik tontonan" (kartu Hari ini/7 hari/Total, grafik batang 7 hari emas, 5 judul teratas, saklar catat, hapus dengan konfirmasi). Masuk indeks pencarian Settings
- [x] Dampak: :feature:mpv (WatchStats.kt baru, 6 baris di MPVActivity), :app (WatchStatsUi.kt baru, SettingsScreen). Manifest dan Gradle TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: putar video 1 menit, jeda 30 dtk, lanjut; Settings > Statistik tontonan: Hari ini sekitar 1 mnt; matikan saklar lalu putar lagi: angka tidak naik; Hapus lalu nol
- [ ] KETERBATASAN: jam dinding (buffering ikut terhitung, kecepatan putar tidak memengaruhi); segmen belum ditutup hilang bila proses dibunuh paksa; statistik belum ikut cadangan gabungan (usulan berikutnya)

## 2026-10-28 - Script Execution paket B: favorit, cari, argumen, percaya [SCRIPT-PACK-B]
- [x] feat(script): ScriptItem punya favorite, args, trusted (default aman, JSON lama tetap terbaca; ikut cadangan lewat snapshot/mergeFrom). Bintang emas di Settings; favorit di urutan atas Settings dan Pemilih skrip; kolom cari muncul bila lebih dari 6 skrip
- [x] feat(script): tombol OPSI: argumen (satu baris gaya prompt shell, karakter kontrol dibuang, maks 300) ditambahkan ke perintah lewat ScriptLaunch.terminalCommand(args); dialog konfirmasi menampilkan argumen
- [x] feat(script): saklar "Percaya skrip ini" melewati konfirmasi HANYA untuk ketukan dalam aplikasi (segitiga Settings, Pemilih skrip). Pintasan layar utama SENGAJA tetap selalu konfirmasi karena Activity bisa menerima extra dari aplikasi lain
- [x] test: dua kasus baru di scripts/code-tests/PreviewTests.kt (argumen dan tanpa baris baru); belum dijalankan lokal
- [x] Dampak: :app (ScriptStore, ScriptRunner, ScriptPickerUi, ScriptExecutionUi), :feature:code (ScriptLaunch, parameter default sehingga pemanggil lama aman). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: bintang lalu urutan naik; OPSI argumen "a b" lalu jalankan: terlihat di dialog dan terminal; aktifkan Percaya lalu ketuk segitiga: langsung jalan; pintasan tetap konfirmasi
- [ ] BELUM: status exit code terakhir (butuh membaca keluaran terminal; dipisah ke paket tersendiri)
- 2026-10-28: Riset saran fitur utilitas Settings (profil setelan, backup berversi, pusat pembersihan, pustaka skrip, aturan otomasi, diagnostik). Menunggu keputusan prioritas.
- 2026-10-28: feat(settings): Profil setelan (ProfileManager + ProfileUi, satu JSON per profil di Backup/profiles, tanpa mengubah format molina-backup.json).
- 2026-10-28: feat(settings): Diagnostik sekali klik (DiagnosticsManager + DiagnosticsUi): ZIP ringkasan, laporan bug, crash ke Documents/Export, URL disamarkan, simpan 3 terbaru.
- 2026-10-28: feat(settings): Diagnostik: SHA-256 tiap berkas di ringkasan.txt, SHA-256 ZIP di berkas .sha256 (format sha256sum) dan ikut teks berbagi.
- 2026-10-28: feat(settings): Pusat Pembersihan Cerdas (CleanupManager + CleanupUi) di Penyimpanan: 7 kategori dengan estimasi byte dan konfirmasi; laporan Export > 7 hari, crash lama (3 terbaru dijaga), sisa unduhan Debian; fix: cache aplikasi tidak lagi menghapus debian-rootfs.tar.gz(.part) saat unduhan berjalan.

## 2026-10-28 - Warna menu titik tiga tab Code [MENU-WHITE]
- [x] ui(code): teks dan ikon menu titik tiga jadi putih polos (CodeColors.MenuText); "Jalankan di Terminal" merah (CodeColors.MenuDanger). IconDefault champagne TIDAK diubah (dipakai di 55 tempat lain)
- [x] Dampak: hanya :feature:code (CodeColors.kt, OverflowMenu.kt). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: buka menu titik tiga, semua teks putih, "Jalankan di Terminal" merah, "Muat ulang" dan "Aksi file" tetap redup

## 2026-10-28 - Warna menu titik tiga dan info line editor [MENU-WHITE] [STATUS-BLUE]
- [x] ui(code): teks dan ikon menu titik tiga putih polos (CodeColors.MenuText); "Jalankan di Terminal" merah (CodeColors.MenuDanger). IconDefault champagne tidak diubah (dipakai di 55 tempat lain)
- [x] ui(code): info line bilah status (Ln/Kol dan "N baris, N karakter") biru muda CodeColors.InfoBlue #4FC3F7; bahasa, UTF-8, status simpan tetap
- [x] Dampak: hanya :feature:code (CodeColors.kt, OverflowMenu.kt, CodeWin7.kt). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: menu titik tiga putih, Jalankan di Terminal merah; info line biru

## 2026-10-28 - Halaman Sistem > Info [INFO-PAGE]
- [x] refactor(settings): kartu Status Molina dipindah dari beranda Settings ke halaman baru Sistem > Info (Page.Info). Kartu informasi dasar perangkat (W7SystemSummary) dipindah dari bawah halaman Sistem ke Info, jadi tidak ada duplikat
- [x] Item "Info" di urutan teratas halaman Sistem; indeks pencarian: Info, Status Molina, Informasi perangkat
- [x] Dampak: hanya :app (SettingsScreen.kt, HubStatusUi.kt). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: beranda tanpa kartu status; Sistem > Info menampilkan status dan info perangkat; ketuk baris yt-dlp membuka Paket; pencarian "status" membuka Info
- [ ] Usulan: halaman About masih punya kartu Status Molina sendiri (duplikat); tunggu keputusan

## 2026-10-28 - Info jadi rumah diagnostik, About digabung [INFO-PAGE]
- [x] refactor(settings): halaman About dihapus (Page.About) dan digabung ke Sistem > Info (HubInfoPage di AboutUi.kt). Kartu Status Molina kini hanya di Info
- [x] feat(settings): Info memuat baris Molina Terminal (versi + emulator Termux v0.118.0) dan Molina Cinema (versi + libmpv), FFmpeg, RAM; kartu info perangkat lama dilebur ke sini
- [x] refactor(settings): Diagnostik sekali klik, Laporan bug, Log, Monitor Sistem dipindah dari Sistem ke Info; tombol back Log/Monitor kembali ke Info; indeks pencarian diperbarui
- [x] Dampak: hanya :app (SettingsScreen.kt, AboutUi.kt, HubStatusUi.kt). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: Sistem > Info urut Status, Versi dan perangkat, Diagnostik, Lisensi; "Molina Terminal" tidak terpotong; libmpv terisi setelah memutar video; back dari Log kembali ke Info
- [ ] Catatan: "v0.118.0" di baris Molina Terminal ditulis manual; W7SystemSummary kini tidak dipakai (boleh dibersihkan)

## 2026-10-28 - Panel kiri Penjelajah jadi drawer geser di potret [NAV-SWIPE]
- [x] ui(explorer): Favorit/Terakhir/Komputer (W7NavPane) tersembunyi di potret; buka lewat swipe tepi kiri atau tombol ☰, tutup lewat swipe kiri, ketuk scrim, Back, atau pilih folder. Landscape tetap dock 92dp
- [x] Dampak: hanya :app (Win7OpenDialog.kt). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat

## 2026-10-28 - Bilah alat Penjelajah dirampingkan [TOOLBAR-SLIM]
- [x] ui(explorer): tombol teks Naik dan Maju dihapus dari bilah alat; Mundur/Naik tetap lewat tombol bulat kiri atas. Sisa: Segarkan, Urut, Alat
- [x] Dampak: hanya :app (Win7OpenDialog.kt). goForward() kini tak dipanggil (peringatan unused). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat

## 2026-10-28 - Perbaikan build: W7HelpPanel hilang [HELP-PANEL]
- [x] fix(explorer): W7HelpPanel dipanggil di Mode Pilih (baris 802) dan dialog ZIP (baris 1853) tetapi definisinya tidak ikut ter-commit di 3eddb0b; kini ditambahkan di Win7OpenDialog.kt (judul, ringkasan, daftar istilah-penjelasan yang bisa digulir, tombol Tutup)
- [x] Dampak: hanya :app (Win7OpenDialog.kt). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI); cek CI hijau, lalu uji ikon "?" di Mode Pilih dan bantuan di dialog ZIP

## 2026-10-28 - Baris Dekoder aktif di Sistem > Info [INFO-DECODER]
- [x] feat(cinema): MPVActivity mencatat hwdec-current via MpvVersionNote.recordDecoder (kunci hwdec_active di prefs molina_ui), juga saat pemutar di latar
- [x] feat(settings): kartu Versi dan perangkat di Info memuat "Dekoder aktif": SW / HW+ (mediacodec) / HW (nilai); ikut tersalin di Salin info debug
- [x] Dampak: Kotlin saja (:feature:mpv MPVActivity.kt, MpvVersionNote.kt; :app MpvVersionBridge.kt, AboutUi.kt). Manifest, Gradle, JNI C/C++ TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: sebelum putar tampil "belum tercatat"; setelah putar video tampil HW+/HW; ganti ke SW lewat tombol dekoder lalu cek Info
- [ ] Catatan: file audio saja akan tercatat "SW" (mpv melaporkan no); batasi ke trek video bila membingungkan

## 2026-10-29 - Perbaikan thumb slider ukuran font [SLIDER-THUMB]
- [x] fix(settings): Tampilan Molina, thumb slider ukuran font (garis kuning vertikal) terlalu tinggi karena bawaan M3 44dp; dipendekkan jadi 4dp x 24dp via SliderDefaults.Thumb(thumbSize = DpSize(4.dp, 24.dp))
- [x] Dampak: hanya :app (SettingsScreen.kt, fungsi TampilanPage). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: geser slider 8-14sp, garis kuning tidak lagi menjulang, ukuran font prompt tetap berubah per 1sp
- [ ] Catatan: kalau yang dimaksud bagian kuning lintasan, bukan thumb, ubah SliderDefaults.colors (activeTrackColor)

## 2026-10-29 - Layar Informasi berkas di menu titik tiga [CODE-INFO]
- [x] feat(code): item "Informasi berkas" di menu titik tiga (grup Cari, setelah Pergi ke baris; juga muncul di Palet Perintah) membuka layar penuh FileInfoScreen
- [x] Isi: statistik teks (baris, kata, karakter, tanpa spasi, ukuran UTF-8, posisi kursor), jenis berkas (nama, ekstensi, jenis, lokasi, ukuran disk, diubah terakhir, status), editor (encoding, akhir baris, pembungkus kata, hanya baca). Hitung di Dispatchers.Default; batas 3 juta karakter
- [x] Dampak: hanya :feature:code (FileInfoScreen.kt baru; CodeSession, CodeScreen, OverflowMenu, CodeWin7 languageOf jadi internal). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: menu titik tiga > Informasi berkas; angka berubah setelah mengetik lalu buka ulang; berkas .kt tampil ".kt" dan Kotlin; tombol back menutup layar
- [ ] Usulan: hapus "N baris, N karakter" dan label bahasa dari bilah status bawah (menunggu keputusan)

## 2026-10-29 - Ukuran font editor di menu titik tiga, waktu baca, seleksi [CODE-FONT] [INFO-READ]
- [x] feat(code): item "Ukuran font: Nsp" di menu titik tiga (grup terakhir, di atas Akhir baris) membuka FontSizeDialog: tombol - dan +, pilihan cepat, pratinjau, Setel ulang; rentang 8-24sp, bawaan 13sp, berlaku untuk semua tab
- [x] Ukuran diingat lewat RunStore yang sudah dipasang di MainActivity (kunci editor_font_sp); EditorHost.syncFontSize memasang ke editor aktif dan tab lain menyusul saat aktif. Tanpa perubahan di :app
- [x] feat(code): Informasi berkas menambah "Waktu baca" (200 kata per menit) dan "Seleksi" (karakter dan kata teks terpilih, hanya muncul bila ada seleksi)
- [x] Dampak: hanya :feature:code (FontSizeDialog.kt baru; RunStore, CodeSession, EditorHost, CodeScreen, MenuIcons, OverflowMenu, FileInfoScreen). Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: ganti ukuran lalu cek tab lain; tutup dan buka aplikasi, ukuran tetap; seleksi teks lalu buka Informasi berkas
- [ ] Catatan: angka Informasi berkas tidak berubah saat mengetik karena layar penuh menutupi editor; dihitung ulang tiap dibuka

## 2026-10-29 - Pilihan font, ligature, dan sticky scroll editor (paket cepat Cosmic) [CODE-STYLE] [CODE-STICKY]
- [x] feat(code): dialog Font editor (menu titik tiga) kini memuat pilihan JetBrains Mono (bawaan), Fira Code, atau Monospace sistem; saklar Ligature; saklar Sticky scroll. Semua diingat lewat RunStore (editor_font_family, editor_ligature, editor_sticky), berlaku untuk semua tab
- [x] feat(code): sticky scroll memakai blok berbasis indentasi (IndentBlocks.kt) yang dibangun di MolinaAnalyzeManager, satu lintasan, dilewati untuk berkas di atas SyntaxLexer.MAX_CHARS; maksimal 3 baris menempel
- [x] Aset: JetBrainsMono-Regular.ttf dan FiraCode-Regular.ttf (SIL OFL 1.1) di feature/code/src/main/assets/fonts (+- 560 KB); atribusi di THIRD_PARTY_NOTICES.md
- [x] Dampak: hanya :feature:code (EditorStyle.kt dan IndentBlocks.kt baru; CodeSession, EditorHost, FontSizeDialog, OverflowMenu, RunStore, SyntaxHighlight). Manifest, Gradle, JNI mpv TIDAK disentuh. API Sora (props.stickyScroll, setLigatureEnabled, Styles.addCodeBlock) diverifikasi dari sumber tag 0.23.6
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: ganti font lalu cek semua tab; Ligature mati/aktif pada "=> != ->"; buka berkas Kotlin panjang dan gulir, tajuk fungsi/kelas menempel di atas; berkas Python/YAML (indentasi); tutup-buka aplikasi, pilihan tetap
- [ ] Catatan: blok indentasi kurang tepat untuk kode tanpa indentasi rapi; bila perlu ganti ke blok berbasis token setelah TextMate (langkah 2). Berikutnya: highlight TextMate dan pilihan tema

## 2026-10-29 - Highlight TextMate dan pilihan tema kode (langkah 2 Cosmic) [CODE-TM]
- [x] feat(code): dependensi `language-textmate:0.23.6`; MolinaTextMate.kt menyiapkan tema + grammar dari assets/textmate (java, kotlin, python, javascript, xml/svg, html, lua, markdown, json, shell, css). Bahasa lain (go, rust, c/c++, ts, yaml, toml, dll.) tetap memakai lexer Molina
- [x] feat(code): menu titik tiga > "Tema kode" (ThemeDialog.kt): Obsidian Claude (bawaan, tema buatan Molina), Abyss, Solarized Dark, plus saklar Highlight TextMate. Disimpan lewat RunStore (editor_tm_theme, editor_tm_on). Hanya warna token yang berubah; latar, gutter, seleksi tetap warna Molina (MolinaTmColorScheme)
- [x] Lexer Molina dipasang seketika, lalu diganti bahasa TextMate setelah dimuat di thread latar; grammar dimuat di thread latar saat editor pertama dibuat. Berkas di atas 400 ribu karakter tetap memakai lexer Molina (TM_MAX_CHARS)
- [x] Sticky scroll memakai blok lipat TextMate untuk berkas TextMate dan IndentBlocks untuk sisanya
- [x] Dampak: Gradle `:feature:code` (dependensi baru: language-textmate dan turunannya gson, joni, jcodings, snakeyaml-engine) + ~1,1 MB aset. Manifest dan JNI mpv TIDAK disentuh. Perilaku baru: bahasa TextMate membawa auto-indent dan pasangan kurung otomatis dari language-configuration
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: buka .kt/.py/.json/.sh/.css lalu ganti tema (semua tab ikut berubah); matikan saklar TextMate lalu warna kembali ke lexer lama; waktu buka berkas pertama di Android 8; mengetik { atau ( dan Enter (auto-indent); berkas besar
- [ ] Catatan: tema darcula tidak disertakan karena lisensi (EPL). yaml tidak disertakan: grammar VS Code-nya hanya kerangka yang memerlukan berkas turunan

## 2026-10-29 - Format kode via Debian + pemformat JSON/XML bawaan (langkah 3 Cosmic) [CODE-FORMAT]
- [x] feat(code): menu titik tiga > "Format kode" (grup Live Template, juga muncul di Palet Perintah) memformat seluruh isi tab aktif. Hasil dipasang sebagai satu langkah undo; kursor dijaga di baris yang sama; bila teks diedit saat formatter jalan, hasil dibuang (tidak menimpa ketikan)
- [x] Pemformat bawaan (tanpa Debian, murni Kotlin): JSON (JsonFormatter: angka, escape, dan urutan kunci disalin apa adanya; komentar/koma penutup ditolak dengan baris:kolom) dan XML/SVG/plist/dll. (XmlFormatter: hanya spasi pemisah; konten campuran, CDATA, xml:space=preserve utuh; tag multibaris diluruskan gaya Android Studio, atribut +4 spasi)
- [x] Pemformat Debian lewat stdin -> stdout (berkas di disk tidak disentuh): Python = `black -q -`; shell = `shfmt -i 2 -ln bash|posix` (shebang menang atas ekstensi); js/jsx/ts/tsx/css/scss/less/html/md/yaml/vue = `prettier --stdin-filepath=<nama>`. cwd = folder berkas (bila terlihat di guest) sehingga pyproject.toml dan .prettierrc terbaca
- [x] Pengaman: keluaran kosong untuk masukan non-kosong dianggap gagal; exit 127 atau perintah tak ada = pesan cara pasang; batas 500 ribu karakter; waktu habis 45 dtk; satu format pada satu waktu; jalur eksekusi sendiri (Lane.FORMAT) sehingga tidak bentrok dengan tombol Play atau Unduhan
- [x] Infrastruktur: RunRequest + guestArgv/stdinText/missingHint (mode argv tanpa berkas sementara); ScriptCommand.prootArgv (prootArgs kini memanggilnya, keluaran identik); ProotScriptExecutor menulis stdin di thread terpisah dan memeriksa perintah di rootfs (symlink dihitung ada)
- [x] Uji JVM murni lulus: scripts/code-tests/run_format.sh (JSON 22 kasus, XML 15, pemilih 10) dan run_script_command.sh (+3 kasus prootArgv)
- [x] Dampak: :feature:code (paket format baru, FormatController, FormatActions, CodeSession, OverflowMenu, MenuIcons) dan :app (ProotScriptExecutor, ScriptCommand, MainActivity). Gradle, AndroidManifest, JNI mpv TIDAK disentuh; tanpa dependensi atau aset baru
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Hanya logika murni yang sudah diuji; lapisan Android (FormatController, FormatActions, ProotScriptExecutor) baru ditinjau manual
- [ ] Uji di perangkat: (1) .json tidak rapi lalu Format kode, Urungkan mengembalikan; (2) .xml/layout Android; (3) .py setelah `apt install -y black` (tanpa black harus muncul pesan pasang); (4) .sh setelah `apt install -y shfmt`; (5) .css/.ts setelah prettier terpasang, catat lama pemanggilan pertama di Android 8; (6) JSON salah (koma penutup) memunculkan baris:kolom tanpa mengubah teks; (7) ketik saat prettier jalan, hasil dibatalkan
- [ ] Catatan: nama paket Debian (black, shfmt) dan cara pasang prettier (nodejs + npm) perlu diverifikasi di rootfs Molina; ubah pesan di Formatters.kt bila berbeda. Kotlin/Java belum didukung (ktfmt dan google-java-format butuh JVM, sengaja dilewati)
- [ ] Berikutnya: langkah 4 Pergi ke simbol. Opsi lanjutan format: "Format saat simpan" (saklar di RunStore) dan format hanya seleksi

## 2026-10-29 - Format seleksi dan Format saat simpan (lanjutan langkah 3) [CODE-FORMAT2]
- [x] feat(code): menu titik tiga > "Format seleksi" (aktif bila ada seleksi) memformat baris-baris yang tersentuh seleksi (diperluas ke baris utuh; seleksi yang berakhir di awal baris tidak menyeret baris itu). Jorokan bersama dilepas sebelum dikirim ke formatter lalu dipasang lagi (FragmentIndent), sehingga isi kelas/blok menjorok bisa diformat oleh black/shfmt/prettier. Setelah selesai, seleksi disetel ke rentang hasil
- [x] feat(code): saklar "Format saat simpan" (kotak centang di menu titik tiga, bawaan MATI, kunci RunStore editor_format_save). Berlaku untuk tab yang sudah punya berkas dan tidak hanya-baca; tab baru yang membuka Simpan sebagai tidak diformat
- [x] Aturan simpan: format TIDAK PERNAH menahan simpan. Tak ada pemformat untuk jenisnya = langsung simpan tanpa pesan; formatter gagal/waktu habis/teks berubah = tetap simpan dan toast "Tersimpan: nama · tanpa format: <alasan>". Batas waktu formatter Debian saat simpan 15 dtk (manual tetap 45 dtk)
- [x] Infrastruktur: FormatController.start + timeoutMs; FormatActions dirombak jadi runFormat (manual/seleksi/simpan satu jalur); CodeSession.save memanggil formatForSave
- [x] Uji JVM murni lulus: run_format.sh (+8 kasus FragmentIndent, termasuk potongan JSON menjorok bolak-balik)
- [x] Dampak: hanya :feature:code (FragmentIndent.kt baru; FormatActions, FormatController, CodeSession, RunStore, OverflowMenu). :app, Manifest, Gradle, JNI mpv TIDAK disentuh. Patch ini bertumpu pada molina_format_v1 (CODE-FORMAT)
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: (1) pilih satu metode di dalam kelas Python, Format seleksi: hanya baris itu berubah, jorokan tetap; (2) pilih potongan JSON menjorok; (3) seleksi tidak valid memunculkan galat dan teks utuh; (4) aktifkan Format saat simpan, simpan .json berantakan lalu cek isi di disk; (5) simpan .json salah (koma penutup): tetap tersimpan dengan catatan "tanpa format"; (6) tutup-buka aplikasi, saklar tetap; (7) simpan .py tanpa black terpasang: tersimpan + catatan, tidak macet
- [ ] Catatan: Format saat simpan memakai teks setelah format untuk dasar penanda dirty, jadi undo sesudah simpan menandai tab dirty kembali (perilaku wajar). Berikutnya: langkah 4 Pergi ke simbol setelah CI hijau

## 2026-10-29 - Pergi ke simbol lintas berkas (langkah 4 Cosmic) [CODE-SYMBOL]
- [x] feat(code): menu titik tiga > "Pergi ke simbol" (grup Cari, di atas Pergi ke baris; otomatis muncul di Palet Perintah) membuka SymbolDialog: kotak cari fuzzy atas kelas/fungsi seluruh folder utama, saklar "Berkas ini", baris hasil = jenis, nama, "di <induk>", path:baris. Ketuk atau Enter membuka berkas dan melompat ke barisnya (CodeSession.openLocation)
- [x] Peringkat: nama persis > awalan > fuzzy; tipe (class/interface/object/enum/record) sedikit di atas fungsi; simbol bersarang sedikit turun; bonus untuk berkas aktif. Query kosong menampilkan simbol berkas aktif
- [x] Indeks (SymbolIndex): memakai pengurai regex Outline (kt, kts, java, py, js/jsx/ts/tsx/mjs, sh/bash); cache per berkas berdasarkan (lastModified, length) selama sesi sehingga pembukaan kedua hampir instan; teks tab terbuka (termasuk yang belum disimpan) menggantikan isi disk; berkas hilang dipangkas; batas 1 MB per berkas dan 20 ribu simbol. Dibangun di Dispatchers.IO, dapat dibatalkan
- [x] Refactor: OutlineItem, Outline, dan fuzzyScore dipindah apa adanya ke SymbolCore.kt (Kotlin murni, agar bisa diuji di JVM). Tambahan kecil: Outline Kotlin kini mengenali modifier `inner` dan `value` (sebelumnya `inner class` tidak muncul di Struktur berkas). FifToggle jadi internal (dipakai bersama)
- [x] Uji JVM murni lulus: scripts/code-tests/run_symbols.sh (26 kasus: indeks, container, cache, override tab, pangkas, batal, peringkat)
- [x] Dampak: hanya :feature:code (SymbolCore.kt dan SymbolDialog.kt baru; Outline, CommandPalette, FindInFiles, CodeSession, CodeScreen, OverflowMenu, MenuIcons). :app, AndroidManifest, Gradle, JNI mpv TIDAK disentuh; tanpa dependensi atau aset baru
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Hanya logika murni yang sudah diuji; SymbolDialog dan pemasangan menu baru ditinjau manual
- [ ] Uji di perangkat: (1) menu titik tiga > Pergi ke simbol, ketik sebagian nama fungsi di proyek Kotlin/Python, ketuk hasil: berkas terbuka dan kursor di barisnya; (2) buka kedua kali, indeks terasa instan; (3) tambah fungsi di tab tanpa menyimpan lalu buka dialog: fungsi baru muncul; (4) "Berkas ini" membatasi ke tab aktif; (5) folder besar di Android 8: catat lama "Mengindeks"; (6) `inner class` kini tampil di Struktur berkas
- [ ] Catatan: simbol hasil regex, bukan parser penuh (properti/val tidak diindeks; Kotlin/Java di bawah indentasi tidak rapi bisa meleset). Ide lanjutan: awalan "@" di Palet Perintah untuk langsung mencari simbol; tipe lain (go, rust, c/c++) bila diminta. Berikutnya: langkah 5 Riwayat versi lokal setelah CI hijau

## 2026-10-29 - Gerbang telemetri: izinkan dev.w3.org [GATE-W3]
- [x] fix(ci): CI gagal di telemetry_gate.py karena css.tmLanguage.json baris 1573 memuat http://dev.w3.org/csswg/mediaqueries/ di dalam komentar regex (grammar VS Code, langkah 2 TextMate). Itu tautan spesifikasi, bukan koneksi; dev.w3.org ditambahkan ke ALLOWED_HOSTS (sejajar www.w3.org)
- [x] Dampak: hanya scripts/telemetry_gate.py. Manifest, Gradle, JNI mpv, dan source aplikasi TIDAK disentuh
- [ ] Menunggu CI hijau. Bila gerbang menemukan host lain dari aset textmate, tambahkan hanya kalau juga berupa komentar/tautan spesifikasi

## 2026-10-29 - Perbaikan build: OptIn Slider thumb [SLIDER-OPTIN]
- [x] fix(settings): CI gagal di :app:compileDebugKotlin (SettingsScreen.kt:556, "This material API is experimental") karena parameter `thumb` pada Slider (perbaikan SLIDER-THUMB) masih ExperimentalMaterial3Api. TampilanPage diberi @OptIn(ExperimentalMaterial3Api::class)
- [x] Dampak: hanya :app (SettingsScreen.kt). :feature:code sudah lolos kompilasi di run yang sama. Manifest, Gradle, JNI mpv TIDAK disentuh
- [ ] Menunggu CI hijau. Uji tetap sama dengan SLIDER-THUMB: geser slider 8-14sp, garis kuning tidak menjulang

## 2026-10-29 - Riwayat versi lokal per berkas (langkah 5 Cosmic) [CODE-HISTORY]
- [x] feat(code): menu titik tiga > "Riwayat versi" (grup berkas, sebelum Aksi file; juga di Palet Perintah; aktif untuk tab yang sudah punya berkas) membuka HistoryDialog. Daftar versi (waktu, jumlah baris, ukuran, jenis "Disimpan"/"Sebelum simpan"); ketuk versi = diff terhadap isi editor sekarang (hanya baris berubah + 2 baris konteks, merah = ada di versi itu, hijau = ada di isi sekarang); tombol Kembalikan memasang versi ke editor sebagai SATU langkah undo, TIDAK menulis ke disk (tab jadi "diubah", Urungkan membatalkan). Hapus riwayat berkas (dua ketukan)
- [x] Perekaman: setiap simpan (CodeSession.writeTab, di thread IO tunggal sesi) mencatat (1) isi disk sebelum ditimpa bila beda dari versi terbaru, sehingga perubahan dari luar (Termux/git) dan keadaan awal sebelum simpan pertama ikut tersimpan, lalu (2) teks yang disimpan. Isi yang sama dengan versi terbaru tidak dicatat dua kali. Format-saat-simpan ikut tercatat karena yang disimpan adalah teks hasil format. Galat riwayat tidak pernah menggagalkan simpan
- [x] Penyimpanan: folder privat aplikasi (draftDir/history/h<hash path kanonik>/), satu .gz per versi bernama `<ms>_<jenis>_<sidikjari>_<karakter>_<baris>.gz` (tanpa berkas indeks yang bisa rusak). Batas: 60 versi dan 8 MB per berkas, 100 MB total (dipangkas sekali per sesi, tertua dulu), berkas di atas 2 juta karakter dilewati. Teks LF, UTF-8
- [x] Diff: LineDiff murni Kotlin (awalan/akhiran dipangkas, LCS untuk bagian tengah; di atas 4 juta sel jadi diff ringkas seluruh-ganti)
- [x] Uji JVM murni lulus: scripts/code-tests/run_history.sh (36 kasus: rekam/daftar/baca, dedupe, jam mundur, batas versi, snapshot rusak, pangkas global, diff, konteks, format waktu/ukuran); run_symbols.sh tetap lulus
- [x] Dampak: hanya :feature:code (paket history baru: HistoryStore.kt, LineDiff.kt; ui/HistoryDialog.kt baru; CodeSession, CodeScreen, OverflowMenu, MenuIcons). :app, AndroidManifest, Gradle, JNI mpv TIDAK disentuh; tanpa dependensi atau aset baru. Perilaku baru: tiap simpan membaca berkas disk sekali (maks 5 MB) di thread IO dan menulis satu .gz
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Hanya logika murni yang sudah diuji; HistoryDialog, pemasangan menu, dan restoreHistoryText ditinjau manual
- [ ] Uji di perangkat: (1) buka berkas, ubah, simpan dua kali dengan isi berbeda: Riwayat versi menampilkan "Sebelum simpan" (isi awal) lalu dua "Disimpan"; (2) ubah berkas dari Termux lalu simpan dari editor: versi "Sebelum simpan" berisi perubahan Termux; (3) ketuk versi: diff benar, Kembalikan memasang isinya, Urungkan membatalkan, tab berstatus diubah dan disk belum berubah; (4) simpan tanpa perubahan tidak menambah versi; (5) Hapus riwayat (dua ketukan) mengosongkan; (6) tab baru/belum disimpan: menu abu-abu; (7) berkas besar (~1 MB) di Android 8: catat jeda saat simpan dan saat membuka diff
- [ ] Catatan: riwayat tidak mencakup perubahan lewat Ganti di berkas (ReplaceInFiles memakai undo sendiri) dan tidak ikut saat berkas dipindah/di-rename (kunci = path kanonik). Ide lanjutan: "Bandingkan dua versi", beri label/pin versi, ekspor versi ke berkas. Berikutnya: langkah 6 language server di Debian (paling akhir, berat untuk RAM Android 8)

## 2026-10-29 - Riwayat versi: Bandingkan dua versi, sematan versi, diff kontras tinggi [CODE-HISTORY2]
- [x] feat(code): Riwayat versi > kotak "Bandingkan dua versi" (hanya muncul bila ada >= 2 versi). Tandai dua versi (tanda centang, penghitung n/2), otomatis membuka perbedaan LAMA -> BARU (merah = hanya di lama, hijau = hanya di baru). Tombol "Pasang lama"/"Pasang baru" memasang salah satu ke editor sebagai SATU langkah undo (tidak menulis ke disk); tombol disembunyikan bila isinya sama dengan isi editor atau tab hanya-baca
- [x] feat(code): sematan versi. Bintang di tiap baris (atau tombol "Sematkan"/"Lepas sematan" di layar diff) menandai versi penting, mis. sebelum refactor besar. Versi bersemat TIDAK ikut dipangkas otomatis dan tidak dihitung ke batas 60 versi; maksimal 10 per berkas (MAX_PINS). Penanda disimpan di pins.txt per folder berkas (tulis atomik; pins.txt rusak diabaikan, daftar tetap jalan). Pangkas global (100 MB) membuang versi tak bersemat lebih dulu, bersemat paling akhir. Hapus riwayat ikut menghapus sematan
- [x] feat(code): Tema kode > "Diff kontras tinggi" (bawaan MATI, kunci RunStore editor_diff_contrast). Tetap memakai token Clean (hijau) dan Danger (merah) yang sudah ada: latar lebih pekat (alpha 0,42/0,46 dibanding 0,16/0,18), tanda +/- berwarna dan tebal, teks baris berubah putih. Berlaku untuk layar diff vs isi sekarang maupun bandingkan dua versi. Refactor: isi diff dipakai bersama lewat DiffBody
- [x] Uji JVM murni lulus: scripts/code-tests/run_history.sh (+14 kasus sematan: selamat dari pemangkasan, di luar batas versi, MAX_PINS, lepas, snapshot hilang, trimGlobal, clear, pins.txt rusak). Gerbang telemetri lulus
- [x] Dampak: hanya :feature:code (HistoryStore, HistoryDialog, ThemeDialog, CodeSession, RunStore). :app, AndroidManifest, Gradle, JNI mpv TIDAK disentuh; tanpa dependensi, aset, atau izin baru. Format snapshot lama tetap terbaca (pins.txt hanya tambahan)
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Lapisan Compose (HistoryDialog, ThemeDialog) hanya diperiksa sintaksnya, belum dikompilasi
- [ ] Uji di perangkat: (1) simpan berkas 3 kali dengan isi berbeda, buka Riwayat versi, ketuk bintang: berubah emas, tutup-buka dialog tetap emas; (2) sematkan 10 versi lalu ke-11: toast gagal; (3) simpan >60 kali: versi bersemat tidak hilang; (4) "Bandingkan dua versi": tandai dua, diff lama -> baru benar, Pasang lama/baru lalu Urungkan; (5) Tema kode > Diff kontras tinggi, buka diff: warna lebih pekat, tutup-buka aplikasi tetap aktif; (6) Hapus riwayat menghapus sematan; (7) bandingkan di layar 360dp: tombol tidak terpotong
- [ ] Catatan: pasangan versi yang sedang dibandingkan tidak menyegarkan status bintang bila ditoggle sesudahnya (hanya di daftar). Ide lanjutan: label teks pada versi bersemat, ekspor versi ke berkas. Berikutnya: langkah 6 (language server di Debian), diawali audit isi rootfs

## 2026-10-29 - Periksa kode: diagnostik sekali jalan (langkah 6a, tanpa language server) [CODE-CHECK]
- [x] feat(code): menu titik tiga > "Periksa kode" (juga di Palet Perintah) memeriksa isi tab aktif SEKALI JALAN lalu proses selesai: tidak ada server yang menetap di RAM. Hasil = garis bawah di editor (Sora DiagnosticsContainer: merah galat, kuning peringatan, info; ketuk garis bawah untuk pesan) + dialog "Daftar masalah" (ketuk baris = lompat ke posisinya; "Hapus tanda"). Teks tidak pernah diubah; bila teks berubah saat memeriksa, hasil dibuang
- [x] Pemeriksa: Python = `pyflakes3` (stdin, galat sintaks + undefined name = galat, sisanya peringatan); shell = `shellcheck -f json -s bash|sh -` (shebang menang atas ekstensi; level error/warning/info/style, kode SCnnnn); JSON dan XML/SVG/plist/dll. = pemeriksa sintaks bawaan (memakai JsonFormatter/XmlFormatter, tanpa Debian, tanpa perubahan di keduanya). Exit code temuan (1) bukan galat; exit lain atau keluaran tak terbaca = pesan galat; exit 127 = pesan cara pasang
- [x] Infrastruktur: paket check (Checkers, Parsers, MiniJson, CheckRange: Kotlin murni); CheckController (satu pemeriksaan sekali waktu, batas 30 dtk, keluaran maks 400 ribu karakter, hasil basi dibuang) memakai RunRequest mode argv + stdin seperti Format kode; Lane baru CHECK di ProotScriptExecutor agar tidak bentrok dengan Play/Format/Unduhan; MainActivity memasang checkExecutor. Batas 300 ribu karakter per periksa
- [x] Uji JVM murni lulus: scripts/code-tests/run_check.sh (41 kasus: pemilih, parser pyflakes/shellcheck termasuk json1 dan kedalaman berlebih, pemeriksa JSON/XML bawaan, pemetaan baris/kolom ke indeks). run_history/format/symbols tetap lulus; gerbang telemetri lulus
- [x] Dampak: :feature:code (paket check baru; CheckController, CheckActions, ProblemsDialog baru; CodeSession, CodeScreen, OverflowMenu, MenuIcons) dan :app (ProotScriptExecutor.Lane, MainActivity). Gradle, AndroidManifest, JNI mpv TIDAK disentuh; tanpa dependensi, aset, atau izin baru. API Sora (DiagnosticsContainer/DiagnosticRegion/DiagnosticDetail, CodeEditor.setDiagnostics) sudah dicocokkan dengan sumber 0.23.6
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Lapisan Android (CheckController, CheckActions, ProblemsDialog, pemasangan menu) hanya diperiksa sintaksnya
- [ ] Uji di perangkat: (1) .sh dengan `echo $x` tanpa kutip, Periksa kode setelah `apt install -y shellcheck`: garis bawah kuning + daftar; tanpa shellcheck harus muncul pesan pasang; (2) .py dengan import tak terpakai dan nama tak terdefinisi setelah `apt install -y pyflakes3`; (3) .json dengan koma penutup: galat di baris benar tanpa Debian; (4) ketuk garis bawah menampilkan pesan; (5) ketuk baris di daftar melompat ke posisi; (6) edit teks saat memeriksa: hasil dibuang; (7) Hapus tanda; (8) catat lama panggilan pertama shellcheck/pyflakes di Android 8; (9) Periksa kode dua kali cepat: toast "Masih memeriksa"
- [ ] Catatan: nama paket Debian (pyflakes3, shellcheck) perlu diverifikasi di rootfs trixie Molina; ubah installHint di Checkers.kt bila berbeda. Daftar masalah adalah potret (tidak ikut bergeser saat teks diedit; garis bawah di editor bergeser otomatis). Ide lanjutan 6b: "Periksa saat simpan" (saklar), lalu quick fix sederhana (mis. kutip variabel shellcheck dari fix JSON). Language server penuh (6c) tetap paling akhir, setelah audit rootfs dan uji RAM

## 2026-10-29 - Periksa saat simpan + quick fix shellcheck (langkah 6b) [CODE-CHECK2]
- [x] feat(code): menu titik tiga > kotak centang "Periksa saat simpan" (di bawah "Format saat simpan", bawaan MATI, kunci RunStore editor_check_save). Setelah berkas BERHASIL tersimpan, Periksa kode dijalankan senyap: tanpa toast "Memeriksa", tanpa dialog yang menutupi editor; hanya garis bawah di editor dan satu toast ringkas ("n masalah (...): x galat, y peringatan, z bisa diperbaiki"). Tidak pernah menahan atau mengubah simpan (periksa jalan SESUDAH tulis, jadi isi di disk = isi yang diformat). Dilewati bila tab hanya-baca, simpan sekaligus menutup tab, atau pemeriksa masih sibuk
- [x] Kegagalan pemeriksa saat simpan (mis. shellcheck/pyflakes belum terpasang, waktu habis) hanya dimunculkan SEKALI per sesi, supaya simpan berulang tidak dihujani toast. Hasil periksa yang basi (teks berubah selagi memeriksa) dibuang tanpa pesan
- [x] feat(code): quick fix dari saran perbaikan shellcheck. Parser kini membaca `fix.replacements` (maks 16; bentuk janggal = tanpa perbaikan). Daftar masalah menampilkan "Perbaikan: <label>" dan tombol "Perbaiki" di baris yang punya fix (SC2086/SC2248 "Kutip variabel", SC2046, SC2006, SC2162; lainnya "Terapkan perbaikan shellcheck"). Perbaikan dipasang ke editor sebagai SATU langkah undo (selisih minimal, satu replace), TIDAK menulis ke disk (tab jadi "diubah"; Urungkan membatalkan). Sesudahnya garis bawah lama dibuang, periksa diulang senyap, dan Daftar masalah dibuka lagi bila masih ada temuan
- [x] Pengaman: perbaikan ditolak bila (1) teks editor sudah berbeda dari teks yang diperiksa, (2) posisi di luar teks atau penggantian saling tumpang tindih, (3) baris yang disentuh memuat tab atau karakter surrogate (shellcheck menghitung kolom tab per 8 spasi, posisi bisa meleset), (4) hasilnya sama dengan teks asal. Pesan alasan tampil di toast; teks tidak berubah
- [x] Uji JVM murni lulus: scripts/code-tests/run_check.sh (66 kasus; +25 kasus quick fix: parser fix, kutip variabel, urutan penggantian, lintas baris, tab/surrogate, di luar rentang, tumpang tindih, tanpa perubahan, selisih minimal, kompatibilitas Diagnostic lama). run_format/run_history/run_symbols tetap lulus; gerbang telemetri lulus
- [x] Dampak: hanya :feature:code (Checkers.kt: FixEdit, Diagnostic.fix, CheckFix; CheckActions.kt dirombak: checkTab/checkAfterSave/applyQuickFix; ProblemsDialog, CodeSession, OverflowMenu, RunStore). :app, AndroidManifest, Gradle, JNI mpv TIDAK disentuh; tanpa dependensi, aset, atau izin baru. `Diagnostic` mendapat parameter terakhir berdefault sehingga pemanggil lama tetap kompatibel
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Lapisan Android/Compose (CheckActions, ProblemsDialog, CodeSession, OverflowMenu) hanya ditinjau manual
- [ ] Uji di perangkat: (1) .sh berisi `echo $foo` dan `cp $a $b`, setelah `apt install -y shellcheck`: Periksa kode, daftar menampilkan "Perbaikan: Kutip variabel" + tombol Perbaiki; ketuk: teks jadi `echo "$foo"`, tab "diubah", disk belum berubah, Urungkan mengembalikan; (2) setelah Perbaiki satu temuan, daftar muncul lagi dengan sisa temuan dan posisi benar; (3) edit teks lalu ketuk Perbaiki dari daftar lama: toast "Teks berubah sejak diperiksa"; (4) aktifkan Periksa saat simpan, simpan .sh bermasalah: toast Tersimpan lalu ringkasan masalah, garis bawah muncul, daftar TIDAK terbuka; (5) simpan .sh/.py tanpa pemeriksa terpasang: pesan pasang muncul sekali, simpan kedua tidak mengulang; (6) simpan .json salah: galat bawaan tanpa Debian; (7) kombinasi Format saat simpan + Periksa saat simpan: urutan format, tulis, periksa; (8) baris berjorok tab: tombol Perbaiki memberi "tidak bisa diterapkan dengan aman"; (9) tutup-buka aplikasi, saklar tetap; (10) catat jeda simpan pada .sh ~300 baris di Android 8
- [ ] Catatan: nama paket Debian (shellcheck, pyflakes3) masih perlu diverifikasi di rootfs trixie Molina lewat skrip audit rootfs. Quick fix baru untuk shellcheck; pyflakes tidak punya saran fix. Ide lanjutan: "Perbaiki semua yang aman" (terapkan semua fix non-tumpang-tindih sekaligus), quick fix SC2154/hapus import tak terpakai untuk Python lewat aturan sendiri. Berikutnya: 6c language server penuh, paling akhir, setelah audit rootfs dan uji RAM Android 8

## 2026-10-29 - Audit rootfs dan uji RAM sebelum 6c [CODE-LSP-AUDIT]
- [x] docs: scripts/rootfs_audit.sh (hanya-baca) + docs/rootfs_audit.txt + docs/lsp_probe.txt. Debian 13 trixie aarch64, RAM total 3,6 GB, tersedia 1,3 GB, swap 512 MB, disk bebas 41 GB
- [x] Paket apt terverifikasi: shellcheck 0.10.0-1, pyflakes3 3.2.0-3, python3-pylsp 1.12.0-3 (installHint di Checkers.kt benar). bash-language-server tidak ada di apt (butuh Node), clangd tidak relevan
- [x] Ukur: pyflakes3 300 baris 1,7 dtk/16 MB. shellcheck bersih 1000 baris 3,0 dtk/39 MB (sampel padat 300 baris dengan 600 temuan: 5,8 dtk/273 MB). pylsp idle 28 MB, initialize 3,7 dtk, diagnostik 300 baris 0,57 dtk/29 MB
- [x] Keputusan: 6c layak untuk Python (pylsp menetap, mati otomatis saat idle). Shell tetap sekali jalan. Tidak perlu batas jumlah baris untuk Periksa saat simpan
- [ ] Rencana 6c: (1) klien JSON-RPC murni Kotlin + uji JVM, (2) mode stream dua arah di ProotScriptExecutor + Lane baru (menyentuh :app), (3) saklar "Diagnostik langsung (Python)", debounce, idle shutdown, penjaga RAM. Ukur ulang RAM bila completion/definition (Jedi) ditambah

## 2026-10-29 - Klien LSP murni, tahap 1 dari 6c [CODE-LSP1]
- [x] feat(code): paket lsp baru di :feature:code, Kotlin murni tanpa Android dan tanpa proses. LspProtocol.kt (LspJson, LspFraming, LspDecoder, LspUri) dan LspClient.kt (LspClient, LspListener, LspDiagnostics). Belum dipakai UI/transport
- [x] Protokol: Content-Length dalam byte UTF-8; decoder bertahap (tahan potongan 1 byte, banyak pesan per potongan, header lain diabaikan); batas badan 8 MB dan header 4 KB; header/JSON rusak = LspException dan klien ditutup
- [x] Klien: dokumen yang dibuka sebelum siap ditahan dan dikirim setelah initialize; didChange mengirim seluruh teks (tanpa range) dan dilewati bila teks sama; permintaan server (workspace/configuration, registerCapability, workDoneProgress/create) selalu dijawab, metode asing dijawab -32601; shutdown rapi (shutdown lalu exit); onClosed tepat sekali
- [x] Diagnostik: severity 1 galat, 2/tanpa nilai peringatan, selain itu info; posisi LSP (0-based, UTF-16) jadi basis 1 dengan kolom akhir eksklusif; maks 2000 item, pesan maks 1000 karakter
- [x] Uji JVM murni lulus: scripts/code-tests/run_lsp.sh (81 kasus); run_check.sh dan gerbang telemetri tetap lulus
- [x] Dampak: hanya :feature:code (file baru). :app, AndroidManifest, Gradle, JNI mpv TIDAK disentuh; tanpa dependensi, aset, atau izin baru
- [ ] BELUM dikompilasi (CI) dan belum dipakai di perangkat. Berikutnya: tahap 2, transport proses menetap di Debian (mode stream dua arah di ProotScriptExecutor + Lane baru, menyentuh :app), lalu tahap 3 UI (saklar, debounce, idle shutdown, penjaga RAM)

## 2026-10-29 - Transport LSP dua arah, tahap 2 dari 6c [CODE-LSP2]
- [x] feat(code): :feature:code paket lsp (Kotlin murni). LspTransport.kt: kontrak StreamExecutor/StreamRequest/StreamListener/StreamHandle dan LeadingPidFilter (membuang baris PID pembungkus sh dari aliran byte, tahan potongan 1 byte dan baris pertama bukan angka). LspPathMap.kt: penerjemah path Android <-> Debian (/sdcard dan /storage/emulated/0), aturan terlihat sama dengan GuestPaths/bind PRoot, path di luar lingkup atau berisi `..` = null. LspSession.kt: merekatkan LspClient ke StreamExecutor; UI hanya memakai path Android, server hanya melihat URI guest; rootUri, didOpen/didChange/didClose, dan URI diagnostik diterjemahkan di batas ini (diagnostik berkas di luar lingkup diabaikan); semua akses klien diserialkan satu kunci; kegagalan start sinkron (Debian belum terpasang) tetap lewat onClosed; proses selalu dimatikan begitu klien tertutup
- [x] feat(app): ProotScriptExecutor kini juga StreamExecutor (startStream). Lane baru LSP (satu proses per jalur). Pembuatan proses proot dirapikan ke satu helper newProotProcess() yang dipakai exec() lama dan runStream() baru (perilaku exec() tidak berubah). Stream: stdout byte mentah, stderr terbatas 64 ribu karakter, tulis stdin lewat antrean + thread penulis (write() tidak memblokir; batas 16 MB bila proses tidak membaca), tanpa batas waktu, cancel() mematikan seluruh pohon lewat reapTree. cancelActive(Lane) juga membatalkan stream. Log hanya nama, kode keluar, lama; isi dokumen dan stderr tidak pernah dicatat
- [x] feat(code): CodeSession.lspExecutor dan MainActivity memasangnya (Lane.LSP). Belum dipakai UI
- [x] Uji JVM murni lulus: scripts/code-tests/run_lsp_session.sh (path map 19 kasus, filter PID 7 kasus, sesi 40+ kasus termasuk konkurensi stdout vs panggilan UI). run_lsp/check/format/history/symbols tetap lulus; gerbang telemetri lulus
- [x] Dampak: :feature:code (file lsp baru, CodeSession) dan :app (ProotScriptExecutor, MainActivity). Gradle, AndroidManifest, JNI mpv TIDAK disentuh; tanpa dependensi, aset, atau izin baru
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Lapisan Android (startStream/runStream/newProotProcess, wiring) hanya diperiksa sintaksnya; refactor exec() menyentuh jalur Play/Format/Periksa/Unduhan, jadi uji regresinya
- [ ] Uji di perangkat: (1) Play skrip, Format kode, Periksa kode, dan Unduhan tetap jalan seperti sebelumnya (regresi refactor newProotProcess); (2) setelah tahap 3 menyala: pylsp hidup, initialize sukses, diagnostik tiba di path Android yang benar untuk berkas di /storage/emulated/0/molina-studio; (3) berkas di luar folder kerja (akses penuh MATI): openDocument menolak, tidak ada proses yatim; (4) matikan sesi: `ps` di Debian tidak menyisakan pylsp; (5) kill proses dari luar: onClosed berisi exit code
- [ ] Catatan: pylsp belum dijalankan oleh siapa pun (UI tahap 3). Penutupan rapi: onClosed memanggil cancel() sehingga `exit` yang masih terantre bisa tak terbaca server; aman karena proses memang dimatikan. Berikutnya tahap 3: saklar "Diagnostik langsung (Python)", debounce didChange, idle shutdown (timer), penjaga RAM, rootUri dari folder proyek tab aktif

## 2026-10-29 - Diagnostik langsung Python lewat pylsp, tahap 3 dari 6c [CODE-LSP3]
- [x] feat(code): menu titik tiga > kotak centang "Diagnostik langsung (Python)" (bawaan MATI, kunci RunStore editor_lsp_live). Menyala: berkas .py (atau shebang python) yang sudah punya path di disk dikirim ke pylsp di Debian; hasilnya jadi garis bawah di editor + Daftar masalah (label pylsp), tanpa toast dan tanpa dialog. Sumber pycodestyle disembunyikan (terlalu berisik); pyflakes dkk tampil
- [x] Debounce 700 ms setelah edit terakhir; teks dibaca saat debounce selesai (tidak ada salinan menggantung). Diagnostik hanya dipasang bila isi editor masih persis yang dikirim (TextStamp), jadi garis bawah tidak pernah menandai tempat yang salah
- [x] Idle shutdown: 5 menit tanpa edit, sesi dimatikan rapi (shutdown+exit), dipaksa kill bila server diam 3 dtk; edit berikutnya membangunkan sesi baru. Penjaga RAM (/proc/meminfo MemAvailable): tidak mulai bila < 600 MB (tunda 60 dtk, satu toast), sesi dihentikan bila < 250 MB (cek tiap 20 dtk). Gagal beruntun: cooldown 10 dtk, menyerah setelah 3 kali sampai saklar dinyalakan ulang. Tab ditutup / Simpan sebagai: dokumen dilepas atau dipindah di server
- [x] Lencana status di bilah bawah (hanya bila saklar aktif dan berkas didukung): LSP siap (hijau), memuat dan RAM (emas), siaga dan tidur (abu), galat (merah). Menggantikan teks "UTF-8" agar muat di layar sempit
- [x] Kode: paket lsp LspLive.kt (LspLive, LspServers, MemInfo, TextStamp; Kotlin murni, penjadwal/sensor disuntik), ui/LiveAndroid.kt (AndroidLiveEnv, LiveBadge), CodeSession (saklar, liveSync, applyLiveDiagnostics), CheckActions (diagnosticsContainer dipakai bersama), CodeScreen, CodeWin7, OverflowMenu, RunStore, MainActivity (lspPathMap)
- [x] Uji JVM murni lulus: scripts/code-tests/run_lsp_live.sh (60+ kasus: debounce, idle, kill, RAM saat mulai/berjalan, gagal beruntun, menyerah, pylsp belum terpasang, Simpan sebagai, berkas tak terlihat, teks besar). run_lsp_session/lsp/check/format/history/symbols tetap lulus; gerbang telemetri lulus
- [x] Dampak: :feature:code dan :app (hanya MainActivity: lspPathMap). Gradle, AndroidManifest, JNI mpv TIDAK disentuh; tanpa dependensi, aset, atau izin baru. Refactor kecil: pembuat garis bawah Sora diekstrak ke diagnosticsContainer() (perilaku Periksa kode sama)
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Lapisan Android/Compose hanya diperiksa sintaksnya
- [ ] Uji di perangkat: (1) `apt install -y python3-pylsp`, nyalakan saklar, buka .py di folder kerja: lencana memuat lalu siap, import tak terpakai bergaris bawah; (2) tanpa pylsp: satu toast pesan pasang, lencana galat, tidak ada loop restart; (3) ketik cepat: tidak ada garis bawah meleset; (4) diam 5 menit: lencana tidur, `ps` di Debian tanpa pylsp; edit lagi: siap kembali; (5) tutup tab/Simpan sebagai: tidak ada diagnostik yatim; (6) .py di luar folder kerja (akses penuh mati): tanpa garis bawah, tanpa proses; (7) matikan saklar: proses mati; (8) layar 360dp: bilah status tidak terpotong; (9) catat RAM dan baterai di Android 8 selama 30 menit mengedit; (10) regresi Periksa kode/Format/Play
- [ ] Catatan: ambang RAM (600/250 MB) dan idle 5 menit adalah tebakan awal dari audit (pylsp ~30-100 MB); ubah di LspLiveConfig setelah uji perangkat. Diagnostik versi lama yang tiba terlambat bisa sesaat menimpa (diperbaiki otomatis oleh hasil berikutnya). Berikutnya (opsional): completion/definition via Jedi, atau server shell bila ada paket apt

## 2026-10-29 - Popup completion Python via pylsp, tahap 2 [CODE-LSP5]
- [x] feat(code): LspLive.completeBlocking (jembatan thread latar, batas waktu, null bila sesi tidak siap/teks terlalu besar); LspCompletionUi (prefixLength, kindName, shouldTrigger; Kotlin murni); ui/LspCompletion.kt (LspCompletionLanguage membungkus bahasa dasar, debounce 150 ms, timeout 1500 ms, jatuh ke completion bawaan bila gagal)
- [x] Sambungan: syncSyntax membungkus lexer Molina dan TextMate; CodeSession.completionBackendFor; label menu jadi "Python langsung (diagnostik + saran kode)"; warna jendela completion mengikuti chrome Molina
- [x] Uji JVM lulus: run_lsp_live (+9 kasus completeBlocking), run_lsp_features (+14 kasus aturan popup); run_lsp, run_lsp_session, run_check, run_format tetap lulus
- [x] Dampak: hanya :feature:code. Gradle, AndroidManifest, JNI mpv, :app TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat; lapisan Android hanya diperiksa sintaksnya. Periksa di CI: Language by base (metode default Java), SimpleCompletionItem.kind
- [ ] Uji di perangkat: (1) saklar nyala, buka .py, ketik "os.pa": popup muncul dengan ikon; pilih item: awalan terganti benar; (2) ketik "os." langsung: popup muncul; (3) pylsp belum siap atau saklar mati: completion bawaan tetap jalan, tidak ada galat; (4) ketik cepat: tidak ada popup basi atau lag; (5) sesi tidur: ketikan pertama membangunkan, saran muncul di ketikan berikutnya; (6) .py di luar folder kerja: tanpa popup LSP; (7) indentasi otomatis dan quick quote di .py tetap normal (regresi delegasi bahasa); (8) catat RAM dan jeda popup di Android 8 selama 30 menit mengedit
- [ ] Berikutnya: tahap 3 go to definition (aksi menu/tekan lama, buka tab dan lompat; target di luar lingkup tampil toast)


## 2026-10-29 - Pergi ke definisi (Python, pylsp/Jedi), fase 3 dari completion/definition [CODE-LSP6]
- [x] feat(code): menu titik tiga > "Pergi ke definisi" (di bawah "Pergi ke simbol"; redup bila "Python langsung" mati, tab tanpa berkas, tab memuat, atau permintaan sedang jalan). Nama di kursor (atau tepat sebelum kursor) dikirim sebagai textDocument/definition; hasil: 1 tujuan = langsung lompat (buka berkas bila perlu), >1 = dialog daftar "berkas:baris", 0 = toast "Definisi tidak ditemukan"
- [x] Logika murni baru lsp/LspDefinitionUi.kt (LspDefinitionLogic: wordAt, classify, label, message; WordSpan, DefinitionChoice, DefinitionOutcome, LspDefinitionReply). Kolom permintaan selalu di DALAM kata; angka bukan pengenal; target di posisi kursor sendiri dibuang (hanya itu = "Kursor sudah di definisi"); urutan berkas aktif dulu lalu path/baris/kolom; maks 30 pilihan; path yang tidak ada di disk atau hanya di Debian (pustaka sistem/typeshed) = pesan "Definisi ada di Debian (path), tidak terbuka dari Android"
- [x] Jembatan LspLive.requestDefinition (main thread, tidak memblokir, cb tepat sekali): kirim teks terbaru lebih dulu, timeout 5 dtk (DEFINITION_TIMEOUT_MS di CodeSession), balasan terlambat atau sesi mati saat menunggu = satu galat saja. Sesi SLEEPING/IDLE dibangunkan oleh permintaan lalu pengguna diminta mengulang ("Server Python sedang dimulai")
- [x] CodeSession.goToDefinition/onDefinitionReply/openDefinitionChoice + DefinitionDialog.kt + pemasangan di CodeScreen dan OverflowMenu (otomatis ikut Palet Perintah). Hasil dibuang bila pengguna pindah tab; bila teks berubah selama menunggu: toast "Teks berubah saat mencari, ketuk lagi" (stempel TextStamp, tidak pernah melompat ke baris yang sudah bergeser)
- [x] Uji JVM murni lulus: scripts/code-tests/run_lsp_definition.sh (wordAt, classify, label, pesan; 41 kasus) dan 18 kasus baru di run_lsp_live.sh (permintaan, terjemah path Android/Debian, null, galat server, timeout, balasan terlambat, proses mati, teks terlalu besar, berkas di luar jangkauan, bangun dari tidur). run_lsp/session/features/check/format/history/symbols tetap lulus; gerbang telemetri lulus
- [x] Dampak: HANYA :feature:code (file baru LspDefinitionUi.kt dan DefinitionDialog.kt; LspLive, CodeSession, CodeScreen, OverflowMenu diubah). :app, Gradle, AndroidManifest, JNI mpv TIDAK disentuh; tanpa dependensi, aset, atau izin baru
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Lapisan Compose/Android (CodeSession, DefinitionDialog, menu) hanya diperiksa sintaksnya (0 galat sintaks); jalur logika dan jembatan sudah teruji di JVM
- [ ] Uji di perangkat: (1) `apt install -y python3-pylsp`, nyalakan Python langsung, buka .py: kursor di pemanggilan fungsi lokal > Pergi ke definisi: lompat ke `def` dan baris tersorot; (2) `import modul_lokal` lalu nama dari modul itu: berkas lain terbuka di baris yang benar; (3) nama dengan dua definisi (mis. if/else): dialog daftar muncul, ketuk membuka; (4) `os.path.join`/builtin: toast "Definisi ada di Debian (...)" tanpa crash; (5) kursor di spasi/angka/komentar: toast, tanpa permintaan hang; (6) sesi tidur (diam 5 menit): menu pertama membangunkan, toast minta ulang, ketuk kedua berhasil; (7) saklar mati: menu redup; (8) ketuk menu lalu ganti tab sebelum jawaban: tidak melompat; (9) ketik saat menunggu: toast "Teks berubah"; (10) ukur RAM pylsp saat completion/definition aktif di Android 8 (Jedi lebih berat dari diagnostik): bila sisa < 600 MB, sesuaikan COMPLETION_TIMEOUT_MS (CodeSession/LspCompletion) dan startMinKb/stopBelowKb di LspLiveConfig
- [ ] Catatan: progres LSP4 (klien definition/completion, parser Jedi) dan LSP5 (popup completion, commit fc80334) belum punya bagian sendiri di berkas ini; isinya ada di kode (LspFeatures.kt, LspCompletion.kt) dan uji run_lsp_features.sh. Ide lanjutan: tombol "Kembali" setelah lompat (tumpukan posisi), buka berkas pustaka Debian read-only lewat path rootfs, aksi di menu long-press Sora


## 2026-10-29 - Navigasi editor: Kembali, pustaka Debian hanya-baca, aksi tekan-lama, lencana LSP [CODE-NAV1]
- [x] feat(code): tombol "Kembali" setelah lompat. Tumpukan posisi kecil (maks 50, hanya di memori) dicatat di openLocation, jadi mencakup Pergi ke definisi, Pergi ke simbol, Cari di berkas, dan tautan keluaran. Lompatan di baris yang sama tidak dicatat; tab yang sudah ditutup dilewati. Jalur kembali: pil "Kembali ke nama:baris" (8 dtk setelah lompat), menu titik tiga/Palet perintah "Kembali ke posisi sebelumnya", dan tombol di panel tekan-lama
- [x] feat(code): Pergi ke definisi yang berujung di pustaka Debian kini membuka berkasnya lewat path rootfs (resolusi gaya chroot: symlink absolut dibaca relatif ke rootfs, ".." tidak keluar dari rootfs). Tab dikunci hanya-baca: saklar Hanya baca dan Aksi file redup, Simpan ditolak (Simpan sebagai ke folder kerja tetap boleh), tidak dikirim ke pylsp, tanpa completion. Status bar: "Debian - hanya baca". Beberapa tujuan = dialog daftar berlabel "[Debian] path:baris"
- [x] feat(code): panel aksi tekan-lama/sentuh-kursor Sora diganti subkelas MolinaActionWindow dengan tombol tambahan: Pergi ke definisi, Pergi ke simbol (disembunyikan saat ada seleksi), Kembali (hanya bila ada riwayat). Lebar panel dilonggarkan dari 230dp bawaan agar tombol tidak tersembunyi
- [x] style(code): lencana LSP: siap = emas hangat (CodeColors.Gold, sama dengan penanda aktif Molina Cinema); memuat/siaga/tidur = abu; RAM rendah = terakota; galat = merah
- [x] Uji JVM murni lulus: scripts/code-tests/run_nav.sh (JumpStack, RootfsPaths termasuk symlink jahat/berputar, classifyDebian); run_lsp_definition, run_lsp_live, run_lsp_session tetap lulus
- [x] Dampak: :feature:code (file baru nav/JumpStack.kt, nav/RootfsPaths.kt, ui/BackPill.kt, ui/ActionPanel.kt, 3 ikon vektor di res/drawable; CodeSession, OverflowMenu, MenuIcons, CodeScreen, CodeWin7, EditorHost, LiveAndroid, LspDefinitionUi diubah) dan :app (MainActivity: satu baris debianRoot). Gradle, AndroidManifest, JNI mpv TIDAK disentuh; tanpa dependensi atau izin baru; satu aset baru (res/drawable di :feature:code)
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Periksa di CI: override applyColorScheme/displayWindow pada EditorTextActionWindow (protected/public), com.molina.code.R.drawable, ImageButton.tooltipText (API 26), smart cast di OverflowMenu (tab.file)
- [ ] Uji di perangkat: (1) definisi lokal lalu pil Kembali muncul, ketuk: kursor kembali ke baris asal; (2) definisi ke berkas lain, tutup tab tujuan, Kembali: kembali ke asal; (3) lompat 3 kali lalu Kembali 3 kali: urutan terbalik; (4) `import os` lalu Pergi ke definisi pada `os.path`: berkas /usr/lib/python3*/ terbuka hanya-baca, status bar "Debian - hanya baca", ketik tidak bisa, Simpan menolak, Hanya baca dan Aksi file redup; (5) tujuan ganda: dialog berlabel [Debian]; (6) tekan-lama di kata: panel menampilkan ikon definisi dan simbol, tidak terpotong di layar 360dp; ketuk definisi = sama dengan menu; (7) pilih teks: ikon simbol hilang; (8) lencana: siap berwarna emas, memuat abu; (9) putar layar/ganti tab: tumpukan tetap; (10) regresi: salin/tempel/pilih semua di panel bawaan tetap jalan; Play/Format/Periksa kode tidak berubah
- [ ] Catatan: Pergi ke definisi DARI dalam berkas pustaka Debian belum didukung (pylsp hanya diberi path storage); toast menjelaskannya. Bila hasil campuran (sebagian di storage, sebagian di Debian), hanya yang di storage dipakai. Ide lanjutan: pemetaan rootfs di LspPathMap agar rantai definisi bisa berlanjut di dalam pustaka

## 2026-10-29 - Mesin arsip Penjelajah: 7z, tar, gz/bz2/xz + dialog ekstrak [ARCHIVE1]
- [x] feat(explorer): ArchiveEngine (list + ekstrak) untuk 7z (termasuk berkata sandi AES), tar, tar.gz/tgz, tar.bz2, tar.xz, dan gz/bz2/xz tunggal lewat commons-compress + org.tukaani:xz. ZIP tetap lewat zip4j/FileOps (jalur ekstrak penuh ke folder bernama tidak berubah)
- [x] Keamanan: nama entri tidak dipercaya (".." dan NUL ditolak, jalur kanonik dicek, tautan/perangkat tar dilewati), berkas yang sudah ada tidak ditimpa (diberi nama "nama (1).ext"), cek ruang kosong, gagal/batal = hasil dibatalkan (folder bernama dihapus; mode "di sini" hanya menghapus berkas yang dibuat operasi ini)
- [x] feat(explorer): ArchiveExtractDialog: tujuan (ke folder bernama arsip / di sini) dan ekstrak sebagian (centang berkas/folder, telusur folder). Menu Alat: "Ekstrak arsip" (cepat) dan "Ekstrak pilihan..."; ketuk arsip non-ZIP dan tombol dock "Buka arsip" membuka dialog; sandi 7z lewat dialog sandi yang sama
- [x] style(explorer): label emas 7Z/TAR/GZ/BZ2/XZ/RAR/ISO di W7FileIcon (ZIP tetap amber)
- [x] Refactor kecil: FileOps.Ctl.cancelled(), FileOps.pump/mapZip jadi internal, pesan NeedPassword jadi "Arsip ini dilindungi kata sandi". Logika murni dipisah di ArchiveNames.kt (diuji JVM tanpa pustaka)
- [x] Uji JVM lulus: scripts/app-tests/run_archive_names.sh (52 kasus) dan run_archive_engine.sh (149 kasus dengan arsip nyata dari Python tarfile/py7zr: semua format, sandi benar/salah/tanpa, sebagian, bentrok nama, ".." dan symlink jahat, batal, rusak, progres)
- [x] Dampak: HANYA :app. Gradle: +1 dependensi (org.tukaani:xz:1.10). AndroidManifest, JNI mpv, izin TIDAK disentuh. THIRD_PARTY_NOTICES.md diperbarui
- [ ] BELUM dikompilasi lewat CI dan belum diuji di perangkat. Lapisan Compose (ArchiveExtractDialog, patch Win7OpenDialog/W7FileIcon) hanya diperiksa sintaksnya; mesin diuji JVM terhadap pustaka yang dikompilasi dari sumber (versi sama)
- [ ] Uji di perangkat (Android 8): (1) ketuk .7z/.tar.gz/.tar.xz: dialog terbuka, isi terbaca; (2) Ekstrak ke folder bernama dan Di sini; ekstrak dua kali: tidak menimpa; (3) Ekstrak sebagian: centang folder dan berkas, hasil sesuai; (4) 7z berkata sandi: dialog sandi, salah/benar; (5) tar.xz besar: progres jalan, tombol Hentikan membersihkan hasil; (6) Menu Alat untuk .zip: "Ekstrak arsip" tetap seperti dulu, "Ekstrak pilihan..." membuka dialog baru termasuk ZIP AES; (7) ikon label 7Z/TAR/GZ di daftar; (8) layar 360dp: dialog tidak terpotong; (9) catat RAM saat 7z/LZMA2 besar (kamus besar bisa menekan RAM Android 8)
- [ ] Belum ada: membuat 7z/tar (hanya baca/ekstrak), arsip multi-bagian, edit isi arsip non-ZIP, rar/iso/cab lewat 7z Debian, intent VIEW/SEND dari luar (menyentuh AndroidManifest), dialog Kompres dengan pilihan format/level, pilih folder tujuan bebas

## 2026-10-29 - Verifikasi ARCHIVE1 [ARCHIVE1-VERIFIED]
- [x] CI #279 hijau (feat(explorer): mesin arsip 7z/tar/gz/bz2/xz + dialog ekstrak)
- [x] Sudah di-debug di perangkat oleh pengguna

### Analisis gap Molina Terminal (jantung Molina)
- Status: analisis selesai, belum ada perubahan kode
- Keputusan arah: Terminal sebagai inti, Player menjadi klien Terminal
- Fondasi: foreground service + siklus hidup PTY (batas background Android 8), emulasi terminal, konfigurasi PRoot per-sesi, cek lisensi saat rebranding
- Fitur pembeda: Molina CLI bridge (molina play/status/log), profil utilitas satu ketuk, manajer rootfs, Molina Git aman (Keystore)
- UX: extra keys, tab/split, tema "Molina Amber", tag logcat Molina/Term dan Molina/Mpv
- Ditunda: audit modul mpv dan skema Room untuk Player

## 2026-10-29 - Jembatan CLI `molina` (Debian -> aplikasi) [CLI1]
- [x] feat(cmd): perintah `molina` di Debian (ping, version, status, play, stop, log) lewat abstract Unix socket @molina.cmd; server cmd/CommandServer.kt (hanya UID sama), protokol murni cmd/CommandProtocol.kt, CLI dipasang ke rootfs oleh MolinaShell.installCli
- [x] CI #281 hijau. Catatan .gitignore: pola `molina_*.py` membuang aset .py baru; aset bernama `molina-cli.py`
- [ ] Belum diuji di perangkat. Risiko utama: abstract socket dari DALAM PRoot belum terbukti di Android 8; cadangan: TCP loopback + token

## 2026-10-29 - molina open (Debian -> editor Code) [CLI2]
- [x] feat(cmd): `molina open <berkas>...` (maks 10) membuka berkas di tab Code lewat CodeNav.pendingFiles (jalur yang sama dengan Penjelajah); penulisan state lewat Handler main thread; pemetaan path dan penolakan ".." sama dengan `play`
- [x] Uji: test_molina_cli.py 15 kasus, run_command_protocol.sh 57 kasus, gerbang telemetri lulus, patch idempotent diuji di clone segar
- [x] Dampak: HANYA :app (cmd/CommandProtocol.kt, cmd/CommandServer.kt, assets/molina/molina-cli.py). Gradle, AndroidManifest, JNI mpv, izin, dependensi TIDAK disentuh
- [ ] BELUM dikompilasi lewat CI dan belum diuji di perangkat
- [ ] Uji di perangkat: (1) molina ping = pong; (2) molina open progres.md membuka tab Code; (3) dua berkas, nama berspasi, path relatif; (4) berkas tak ada dan folder: pesan galat tanpa crash; (5) 11 berkas ditolak; (6) regresi play dan log
- [ ] Berikutnya: hook pasca-unduh (automation/PostDownload.kt) memutar hasil unduhan, `molina notify`

## 2026-10-29 - Siklus hidup PTY, langkah 1 [TERM-PTY1]
- [x] Audit: TerminalService sudah sesuai batas Android 8 (channel di onCreate, startForeground pertama di onStartCommand); waiter waitpid dan sapuan AppExit sudah ada
- [x] fix(term/jni): ReleaseStringUTFChars untuk cwd memakai objek yang benar; fd ptm ditutup di jalur galat grantpt/unlockpt dan fork
- [x] fix(term): TerminalSession.finishIfRunning membunuh grup proses (-pid) sebelum PID shell
- [x] feat(term): closeSession memakai SIGHUP ke grup, lalu SIGKILL setelah 400 ms bila masih hidup (TerminalSessions.terminate)
- [x] Dampak: :terminal-emulator (molina_pty.c = kode native PTY, bukan binding mpv; TerminalSession.java) dan :app (TerminalSessions.kt). Gradle, AndroidManifest, JNI mpv TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat
- [ ] Uji di perangkat: (1) buka 2 sesi, jalankan `sleep 999 &` di sesi 2, tutup dengan ✕: `ps` di sesi 1 tidak menyisakan sleep; (2) ketik beberapa perintah lalu ✕: riwayat bash tersimpan di ~/.bash_history; (3) tutup 5 sesi bergantian: tidak ada fd bocor (`ls /proc/$(pidof com.molina.studio)/fd | wc -l` stabil); (4) `exit` di sesi terakhir: aplikasi tertutup seperti sebelumnya; (5) regresi `molina status/play/stop`
- [ ] Berikutnya: langkah 2, konfigurasi PRoot per-sesi dan manajer rootfs

## 2026-10-29 - Konfigurasi PRoot per-sesi, langkah 2a [TERM-PROOT1]
- [x] refactor(term): ProotConfig (pembangun argumen proot murni Kotlin) menggantikan array hardcoded di TerminalSessions.debianShell; urutan argumen identik (diuji golden)
- [x] feat(term): ProotCaps memindai libproot.so; --sysvipc dipasang hanya bila didukung. Binary bawaan (green-green-avk commit 01f83b88) TIDAK mengenal sysvipc, jadi perilaku saat ini tidak berubah
- [x] feat(term): FakeProc: /proc/{loadavg,uptime,stat,version,vmstat} palsu, di-bind hanya bila berkas asli tak bisa dibuka (Android 10+)
- [x] Uji JVM lulus: scripts/app-tests/run_proot_config.sh (25 kasus); gerbang telemetri lulus
- [x] Dampak: HANYA :app (TerminalSessions.kt, ProotConfig.kt baru). Gradle, AndroidManifest, JNI TIDAK disentuh; tanpa dependensi/izin baru
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat
- [ ] Uji di perangkat: (1) terminal terbuka normal, prompt user@localhost; (2) `ls /proc/stat && cat /proc/loadavg`; (3) apt update dan git tetap jalan; (4) bind /sdcard/molina-studio dan `molina status` tetap jalan; (5) 5 sesi bergantian
- [ ] Catatan: argumen proot masih dibangun di 2 tempat lain (code/ScriptCommand.kt, core/common/YtdlpCommand.kt); penyatuan jadi item terpisah. Bila perlu --sysvipc (postgres dkk.), ganti libproot dengan build yang mendukungnya
- [ ] Berikutnya: 2b manajer rootfs (backup, restore, ekspor tarball, hapus/reset, cek ruang kosong)

## 2026-10-29 - Manajer rootfs, langkah 2b: engine backup/restore + hapus aman [TERM-ROOTFS1]
- [x] fix(term): SafeTree.delete menggantikan File.deleteRecursively di Rootfs.kt (3 tempat). deleteRecursively mengikuti symlink: symlink guest ke storage nyata (mis. ln -s /sdcard/molina-studio ~/ws) bisa membuat data di luar rootfs ikut terhapus saat install ulang. Terbukti di JVM
- [x] feat(term): RootfsBackup (export tar.gz via .part, inspect, restore atomik dengan rollback), AndroidRootfsPosix (Os.lstat/chmod/symlink). Murni JVM kecuali lapisan Android; tanpa dependensi baru (commons-compress sudah ada)
- [x] Uji JVM lulus: scripts/app-tests/run_rootfs_backup.sh (42 kasus; butuh ARCHIVE_DEPS). Mutation check: SafeTree, cek parent-di-dalam-rootfs, dan normalisasi '../' terbukti menangkap kerusakan
- [x] Dampak: HANYA :app (terminal/). Gradle, AndroidManifest, JNI, izin TIDAK disentuh. Engine belum dipanggil dari UI (tahap 2c)
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Periksa di CI: ekstensi properti TarArchiveEntry (lastModifiedTime, setUserId), visibilitas internal AndroidRootfsPosix
- [ ] Uji di perangkat (setelah 2c): backup rootfs bersimbol ~/ws -> /sdcard/...; restore; install ulang; cek data di /sdcard utuh
- [ ] Catatan: run_proot_config.sh tercatat mode 644 di git (storage Android tidak menyimpan bit eksekusi); jalankan dengan `bash scripts/app-tests/...`
- [ ] Berikutnya: 2c pembungkus RootfsManager (backup/restore/reset dengan StateFlow, hentikan sesi dulu) + kartu UI di Setelan

## 2026-10-29 - Manajer rootfs, langkah 2c: RootfsAdmin + kartu UI Setelan [TERM-ROOTFS2]
- [x] feat(term): RootfsAdmin (StateFlow Idle/Running/Finished) membungkus RootfsBackup: backup ke /sdcard/Molina/Backup/rootfs/molina-rootfs-<stempel>.tar.gz, restore dari daftar cadangan di folder itu, reset lewat SafeTree (tidak mengikuti symlink)
- [x] feat(term): RootfsState.Maintenance (Rootfs.kt): refresh() dan install() menolak saat Maintenance; SetupPanel dan HubStatusUi menampilkan status "Sedang dikelola"; Terminal terkunci selama operasi
- [x] Urutan aman: tolak bila ada pekerjaan proot (lane CODE/DOWNLOAD/AUTO/FORMAT/CHECK; LSP dibatalkan otomatis) -> Maintenance -> TerminalSessions.killAll() di main thread + TerminalService.requestStop -> operasi -> keluar Maintenance. Tidak memakai closeSession karena EXIT_APP_ON_SESSION_END=true bisa menutup aplikasi
- [x] feat(settings): dialog "Kelola rootfs Debian" (Setelan > Sistem > Debian) dengan progres, tombol Batalkan, konfirmasi restore/reset, layar tetap menyala selama operasi; ditambah entri pencarian
- [x] Dampak: HANYA :app (terminal/RootfsAdmin.kt baru, Rootfs.kt, SetupPanel.kt; settings/RootfsAdminUi.kt baru, SettingsScreen.kt, HubStatusUi.kt). Gradle, AndroidManifest, JNI mpv, izin, dependensi TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Periksa di CI: `when` eksaustif RootfsState setelah state baru, visibilitas ProotScriptExecutor (internal) dari RootfsAdmin
- [ ] Uji di perangkat: (1) Cadangkan sekarang: berkas muncul di Backup/rootfs, progres jalan, sesi Terminal tertutup; (2) Batalkan di tengah: tidak ada .part tersisa, Terminal normal kembali; (3) Pulihkan dari cadangan: isi rootfs sesuai, prompt dan `molina status` jalan; (4) Reset: rootfs hilang, /sdcard utuh, SetupPanel muncul dan install ulang jalan; (5) symlink ~/ws -> /sdcard/molina-studio: backup, reset, restore tidak menghapus data di /sdcard; (6) mulai skrip Code/unduhan lalu coba Cadangkan: ditolak dengan pesan; (7) buka tab Terminal saat operasi: panel "Sedang dikelola", tanpa crash; (8) rootfs Missing: Pulihkan tetap bisa
- [ ] Batas yang diketahui: restore hanya dari berkas di folder Backup/rootfs (belum ada pemilih berkas SAF); sesi dihentikan dengan SIGKILL sehingga riwayat bash sesi aktif tidak tersimpan; cek ruang kosong backup hanya ambang tetap 128 MB; belum ada wake lock/foreground service khusus bila aplikasi dibackground saat proses panjang
- [ ] Berikutnya: 3 profil utilitas satu ketuk, atau `molina notify` / hook pasca-unduh (CLI)

## 2026-10-29 - Rootfs 2d: nama cadangan berversi + ikon Molina [TERM-ROOTFS3]
- [x] feat(term): nama cadangan jadi molina-rootfs-debian<versi>-molina<versi>-<stempel>.tar.gz (versi dari /etc/debian_version dan versionName; dibersihkan ke [A-Za-z0-9.-], maks 16 karakter, cadangan "x"). Daftar restore tetap mengenali semua *.tar.gz lama
- [x] feat(settings): ikon monogram "M" emas (MolinaTheme.Gold #E6B450) pada tautan dan judul dialog Kelola rootfs. Emas merek TIDAK diubah ke #E8B04A: nilai resmi ada di MolinaTheme dan harus sama dengan SGR ANSI
- [x] Dampak: HANYA :app (RootfsAdmin.kt, RootfsAdminUi.kt, SettingsScreen.kt). Gradle, AndroidManifest, JNI, izin TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Uji: cadangkan lalu cek nama berkas; ikon M tampil di Sistem > Debian dan di judul dialog
- [ ] Berikutnya (setelah CI hijau dan 2c lolos uji perangkat): profil utilitas satu ketuk, atau molina notify / hook pasca-unduh

## 2026-10-29 - Profil utilitas satu ketuk: paket Starter [TERM-STARTER1]
- [x] feat(term): packages/StarterProfile.kt (git, python3, ffmpeg, openssh-client, vim; apt-get --no-install-recommends, repositori resmi Debian). Cek ruang kosong 1 GB (exit 4; df gagal = lanjut), update lalu install berantai (&&), idempoten
- [x] feat(settings): AptJob.InstallStarter memakai ToolRunDialog yang ada (konfirmasi dengan pratinjau perintah, progres, timeout 40 menit, Batalkan); ringkasan khusus "N paket baru dipasang" atau "Semua paket Starter sudah terpasang"; tautan Setelan > Sistem > Debian > "Pasang paket Starter" dan entri pencarian
- [x] Uji: scripts/app-tests/run_starter_profile.sh (13 kasus, apt-get dan df tiruan; jalankan dengan `bash`): sintaks, daftar paket sama dengan skrip, ruang cukup/kurang/df gagal/tepat 1 GB, update gagal tidak memanggil install
- [x] Dampak: HANYA :app (packages/StarterProfile.kt baru, settings/DebianAptUi.kt, settings/SettingsScreen.kt). Gradle, AndroidManifest, JNI, izin, dependensi TIDAK disentuh. yt-dlp sengaja di luar profil (punya halaman sendiri dengan verifikasi SHA-256)
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Periksa di CI: `when (job)` eksaustif untuk AptJob baru, string raw dengan $ di StarterProfile.kt
- [ ] Uji di perangkat: (1) ketuk Pasang paket Starter: dialog konfirmasi menampilkan skrip; (2) jalankan di rootfs baru: semua paket terpasang, git/python3/ffmpeg/ssh/vim jalan; (3) jalankan lagi: ringkasan "sudah terpasang"; (4) Batalkan di tengah unduhan lalu ulangi: lanjut tanpa rusak; (5) tanpa internet: gagal rapi dengan kode keluar tidak nol; (6) `df -Pk /root` di PRoot Android 8 memberi angka masuk akal (bila tidak, cek dilewati); (7) cek `Daftar tool terpasang` mengenali alat baru
- [ ] Belum ada: pilihan paket kustom (ceklis), profil lain (Dev, Media), pemasangan otomatis saat rootfs pertama siap, deteksi mana yang sudah terpasang sebelum konfirmasi
- [ ] Berikutnya: `molina notify` + opsi putar-otomatis pasca-unduh (PostDownload sudah menambah ke Terakhir diputar Cinema)

## 2026-10-29 - Sinkronisasi konteks dari ringkasan Claude lain [SYNC-20261029]
- [x] Clone ulang repo resmi dan bandingkan ringkasan dengan kode: langkah 1-4 (PTY, PRoot per-sesi/rootfs, CLI bridge, paket Starter) sudah ada di kode, ringkasan tertinggal
- [x] Dicatat: emas merek tetap #E6B450 (MolinaTheme), bukan #E8B04A
- [ ] Status CI commit terbaru belum terverifikasi dari sesi ini; cek tab Actions
- [ ] Menunggu keputusan pemilik untuk langkah berikutnya

## 2026-10-29 - Audit UX terminal dan Bridge API [AUDIT-UX1]
- [x] Ada: extra keys, pinch zoom, laci sesi, snippet. Belum ada: pencarian scrollback, deteksi URL, command palette terminal, verb CLI clip/notify/share, tag log Molina/Term dan Molina/Mpv
- [ ] Menunggu keputusan pemilik; usulan urutan: molina clip/notify/share, pencarian scrollback + URL, palette, tag log

## 2026-10-29 - Bridge API Android: molina clip/notify/share, protokol v2 [CLI3-BRIDGE]
- [x] feat(cmd): verb clip (get/set, base64 UTF-8, maks 64 KB), notify (judul opsional, jeda 300 ms, 20 slot), share (berkas maks 10 lewat MolinaExplorer.share, atau teks); CommandProtocol.VERSION 2, MAX_LINE 131072, MAX_ARG 4096; handler Android di cmd/BridgeCommands.kt
- [x] feat(settings): sakelar "Akses clipboard dari CLI" (default mati; tidak ikut BackupManager). Isi clipboard/notifikasi/share tidak dicatat ke log
- [x] Uji: run_command_protocol.sh 95 kasus (+38), test_molina_cli.py 30 tes (+15), gerbang telemetri lulus, patch idempotent, 3 mutasi tertangkap
- [x] Dampak: HANYA :app (cmd/, terminal/TerminalPrefs.kt, settings/SettingsScreen.kt, assets/molina/molina-cli.py). Gradle, AndroidManifest, JNI, izin, dependensi TIDAK disentuh
- [ ] BELUM dikompilasi (CI) dan belum diuji di perangkat. Periksa di CI: return@runCatching, visibilitas MolinaExplorer, when eksaustif ClipParse
- [ ] Batas: clip get kosong di Android 10+ saat latar belakang; share sheet dari latar belakang dibatasi Android 10+; hasil share tidak diketahui CLI
- [ ] Berikutnya: pencarian scrollback + deteksi URL, command palette terminal, tag log Molina/Term dan Molina/Mpv, hook pasca-unduh memakai molina notify

## 2026-10-29 - Dialog Kompres Penjelajah: 7z, tar.gz/bz2/xz, tar, level, nama, sandi ZIP [ARCHIVE2]
- [x] feat(explorer): ArchiveEngine.compress: 7z (LZMA2, COPY di level 0), tar, tar.gz, tar.bz2, tar.xz lewat commons-compress; ZIP tetap lewat FileOps.zipAll (kini menerima nama dasar dan level 0..9, termasuk ZIP AES-256)
- [x] feat(explorer): ArchiveCompressDialog (format, level, nama arsip, kata sandi khusus ZIP); menu Alat: "Kompres... (7z, tar.gz, ...)"
- [x] Aman: tautan simbolik dilewati, arsip tidak memasukkan dirinya sendiri, tidak menimpa (nama diberi (1)), gagal/batal = keluaran dihapus. Level xz/7z maks 6 demi RAM
- [x] Uji JVM lulus: run_archive_compress.sh (240 kasus) + verifikasi silang 48 arsip (zipfile, tarfile, py7zr, pyzipper, 7-Zip asli); regresi 52 dan 149 tetap lulus
- [x] Dampak: HANYA :app. Gradle, AndroidManifest, JNI mpv, izin, dependensi TIDAK disentuh. W7SecretField private -> internal
- [ ] BELUM dikompilasi lewat CI dan belum diuji di perangkat (Compose hanya diperiksa sintaksnya)
- [ ] Uji di perangkat: semua format dan level, ZIP + sandi, nama kustom, kompres dua kali (nama (1)), Hentikan membersihkan hasil, RAM tar.xz/7z level 6 di Android 8, layar 360dp

## 2026-10-29 - Seragamkan tag logcat [LOG-TAGS1]
- [x] Audit: LogcatReporter memfilter dengan --pid (bukan tag); LogScreen/BugReport/filter Cinema berbasis source dan label source/tag, jadi tag in-app TIDAK diubah
- [x] refactor(log): MolinaLog (TerminalPrefs.kt) mengirim ke logcat dengan tag Molina/Term; MpvLog.emit dengan tag Molina/Mpv. Tag asal jadi awalan pesan ([TerminalEmulator], [playback]). AppLog tetap memakai tag asal
- [x] Dampak: HANYA :app (terminal/TerminalPrefs.kt) dan :feature:mpv (MpvLog.kt). Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] BELUM dikompilasi (CI). Periksa di CI: const val dalam internal object MpvLog, fungsi private tagged() di dua object terpisah
- [ ] Uji di perangkat: `logcat -s Molina/Term Molina/Mpv` (Log Level terminal Debug/Verbose); putar video lalu cek baris Molina/Mpv dengan awalan [playback]; halaman Log in-app: label tetap `mpv`, bukan `mpv/Molina/Mpv`
- [ ] Belum diseragamkan: ytdlp, auto, CommandBridge, CommandServer, rootfs-admin, shield (lewat AppLog)
- [ ] Berikutnya: pencarian scrollback + deteksi URL (terminal-view), command palette

## 2026-10-30 - Deteksi URL di Terminal [TERM-LINKS1]
- [x] feat(term): TerminalLinkScan (JVM murni) memindai transkrip: skema http/https/rtsp/rtmp/rtmps, buang tanda baca penutup dan kurung tak berpasangan, tolak host kosong dan URL > 2048 karakter, unik, terbaru di atas, maks 30
- [x] feat(term): menu header "Tautan di layar" membuka LinksDialog: Putar (CinemaActions.play), Buka (peramban, http/https saja), Salin, Sisip (ke prompt tanpa Enter). Pemindaian di Dispatchers.Default
- [x] Uji: scripts/app-tests/run_terminal_links.sh, 29 pemeriksaan lulus (kotlinc 2.1.20)
- [x] Dampak: HANYA :app (terminal/). terminal-view, terminal-emulator, Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh. Read-only atas transcriptText
- [ ] BELUM dikompilasi (CI). Periksa: LinksDialog/LinkRow (Compose), impor CinemaActions.Input.Direct
- [ ] Uji di perangkat: jalankan `echo https://example.com/a; yt-dlp --version`, buka menu ⋮ > Tautan di layar; uji URL panjang yang membungkus baris; uji Putar ke Cinema
- [ ] Berikutnya: pencarian scrollback (Cari + sorot), command palette

## 2026-10-30 - Lompat dan sorot hasil pencarian Terminal [TERM-JUMP1]
- [x] Koreksi: pencarian langkah 1 memakai baris logis (transcriptText menggabungkan baris terbungkus), tidak cocok dengan indeks topRow. Diganti pencarian per baris layar (TerminalSearch.findRows, termasuk kolom dengan karakter lebar)
- [x] feat(term): tombol Lompat menggulir TerminalView (showSearchHit) dan menyorot kolom hasil (kotak #E6B450, alpha 40%)
- [x] feat(view): pin tampilan (mSearchPinned): pembaruan output tidak mengembalikan ke bawah; riwayat yang bergeser ikut digeser; pin dilepas saat ACTION_DOWN, onKeyDown, atau commitText (IME)
- [x] Layar penuh (vim/htop, buffer alternatif): pencarian dinonaktifkan dengan pesan
- [x] Uji JVM: scripts/app-tests/run_terminal_search.sh, 32 pemeriksaan lulus (kotlinc 2.1.20)
- [ ] BELUM dikompilasi (CI). Periksa: terminal-view TerminalView.java (Paint, onScreenUpdated, onDraw), TerminalSearchDialog.kt (akses screen, produceState)
- [ ] Uji di perangkat: cetak >200 baris; cari kata lama, Lompat, pastikan sorotan di baris yang benar; lalu jalankan `yes | head -500` dan pastikan tampilan tetap di hasil; ketik sesuatu dan pastikan pin lepas
- [ ] Uji regresi: seleksi teks dengan tahan-tekan, scroll manual, vim, tmux
- [ ] Berikutnya: command palette

## 2026-10-30 - Audit sisa Penjelajah vs ZArchiver [AUDIT-ARCHIVE1]
- [x] Audit repo (HEAD 826846c): ArchiveEngine/Extract/Compress dialog sudah ada
- [x] Keputusan pemilik: poin 7 dan 8 di-skip
- [ ] Belum: multi-bagian, edit arsip non-ZIP, rar/iso/cab via 7z Debian, pecah ukuran, folder tujuan bebas, komentar ZIP
- [ ] Verifikasi: CI ARCHIVE2; uji perangkat ARCHIVE1 dan ARCHIVE2
- [ ] Rapikan checklist yt-dlp (baris 143-153)
- [ ] Berikutnya: jalur 7z Debian (cek lisensi p7zip-full)

## 2026-10-30 - Patch molina_archive_v2 redundan [PATCHV2-REDUNDANT]
- [x] git apply --reverse --check: seluruh file kode patch sudah ada di HEAD (hanya progres.md berbeda)
- [x] Dampak: tidak ada perubahan kode; Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] Status CI commit a58bed8 (memuat ArchiveCompressDialog.kt) belum terverifikasi
- [ ] Uji perangkat ARCHIVE1 dan ARCHIVE2 BELUM

## 2026-10-30 - Verifikasi CI ARCHIVE2 [ARCHIVE2-CI-OK]
- [x] CI hijau untuk commit kode yang memuat ARCHIVE2 (a58bed8 dan sesudahnya), dikonfirmasi pemilik
- [x] Dampak: tidak ada perubahan kode; Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] Uji perangkat ARCHIVE1 dan ARCHIVE2: hasil per item belum dicatat
- [ ] RAM tar.xz/7z level 6 di Android 8: belum terukur

## 2026-10-30 - SevenZipCommand (argv 7z di PRoot, parse, galat) [ARCHIVE3-CMD]
- [x] feat(core): SevenZipCommand: -spd, --, -o<dir>, sandi via stdin, bind /sdcard, parseList, classify, parseProgress (commit b834bb8)
- [x] CI hijau (dikonfirmasi pemilik)
- [x] Uji JVM lulus di sandbox: run_7zip_command.sh (81 kasus, termasuk integrasi 7-Zip 23.01 asli); mutation check -spd dan urutan classify menangkap kerusakan
- [x] Dampak: HANYA core/common dan scripts. Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] Belum diuji pada 7z 25.01 di perangkat
- [ ] Klasifikasi RAR (Unsupported Method) belum diuji dengan berkas RAR nyata
- [ ] Berikutnya: DebianArchive.kt (runner proses, staging, validasi jalur) di :app
- [ ] Penjelajah: rencana DebianArchive.kt (runner 7z via PRoot, staging + validasi, deteksi 7z)

## 2026-10-30 - Audit aktual Penjelajah vs ZArchiver (koreksi) [AUDIT-ARCHIVE2]
- [x] Clone HEAD e4383cd dibaca: mesin arsip, kompres, ekstrak sebagian, edit ZIP, SevenZipCommand sudah ada
- [x] Koreksi: klaim "tanpa INTERNET" tidak berlaku (manifest memuat INTERNET untuk yt-dlp/streaming)
- [ ] Gap nyata: RAR/ISO/CAB, multi-bagian, edit 7z/tar, 7z bersandi + enkripsi nama (-mhe=on), kompres multi-thread, uji integritas
- [ ] Berikutnya: DebianArchive.kt (jalur 7z di PRoot) lalu uji arsip (7z t) dan edit non-ZIP
- [ ] Cek lisensi RAR di rootfs (apt-cache policy p7zip-rar / 7zip-rar) dan perbarui THIRD_PARTY_NOTICES.md
- [ ] Uji perangkat ARCHIVE1 dan ARCHIVE2 serta pengukuran RAM tar.xz/7z level 6 di Android 8 masih tertunda

## 2026-10-30 - molina_dl kompatibel WhatsApp
- [x] Diagnosis: selector `ext=mp4` memilih AV1/VP9+Opus (tidak diputar di WhatsApp/HP lain).
- [x] `video_format` memprioritaskan avc1+mp4a; fallback ke codec lain.
- [x] `+faststart` saat merge MP4.
- [x] `ensure_compat` (ffprobe) -> konversi H.264/AAC/yuv420p hanya bila perlu; event JSON baru `stage=recode`.
- [ ] Verifikasi: kirim hasil unduhan via WhatsApp ke device lain.
- [ ] Pastikan parser Kotlin halaman Unduhan menoleransi `stage` baru.
- [x] Patch awal: langkah pemanggilan `ensure_compat` terlewat (bug idempotency check); diperbaiki dengan patch lanjutan.
- [ ] `molina_dl.py` ter-ignore .gitignore: tentukan salinan resmi (assets/) lalu commit.

## 2026-10-30 - molina_dl kompatibel WhatsApp [DL-COMPAT]
- [x] molina_dl.py v4: selector avc1+mp4a, +faststart, ensure_compat (ffprobe), flag --no-compat, event stage=recode
- [x] UnduhanScreen: label merge/extract/recode dalam bahasa Indonesia
- [x] Dampak: Gradle, AndroidManifest, JNI mpv TIDAK disentuh
- [ ] CI dan uji perangkat (kirim via WhatsApp) belum
- [ ] Toggle UI "Kompatibel (WhatsApp)" vs "Kualitas maksimum" belum

## 2026-10-30 - DebianArchive: runner 7z, staging, audit (belum terhubung ke UI) [ARCHIVE4-RUNNER]
- [x] feat(archive): DebianArchive.kt (probe, list, extract via 7z di PRoot; proses bersih, stdin sandi, batal mematikan pohon proses)
- [x] feat(archive): DebianArchiveStaging.kt (DebianFormats + volume .001/.partN.rar; staging .molina-stage-* di folder tujuan, audit tautan, commit tanpa menimpa, rollback, sapu sisa usang)
- [x] feat(core): SevenZipCommand: prootArgsScoped (hanya folder arsip dan staging yang di-bind), extractArgs sebagian, bindHostOk, parseListStream
- [x] Temuan 7z: nama yang diminta tidak ada tetap rc 0 dengan nol berkas, maka nol berkas dianggap galat di Kotlin; wildcard di nama tidak diperluas (-spd)
- [x] Uji JVM sandbox: run_7zip_command.sh 103 kasus + run_debian_archive_staging.sh 30 kasus; uji ujung-ke-ujung dengan proot palsu (list, ekstrak penuh/sebagian, sandi, batal, progres, multi-bagian)
- [x] Dampak: :app (2 berkas baru) + core/common; Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] CI belum dijalankan untuk commit ini; DebianArchive.kt hanya diverifikasi kompilasi dengan stub Android
- [ ] Belum dihubungkan ke ArchiveExtractDialog / daftar format Penjelajah (ArchiveFormat tidak diubah)
- [ ] RAR nyata belum diuji; lisensi RAR rootfs belum dicek
- [ ] Uji perangkat (proot asli, Android 8, bind /mnt/molina-*) belum; refactor pembangun proses bersama ditunda
- [ ] Berikutnya: hubungkan ke dialog, lalu uji arsip (7z t) dan edit non-ZIP

## 2026-10-30 - Dialog ekstrak terhubung ke DebianArchive [ARCHIVE5-DIALOG]
- [x] feat(archive): ArchiveRoute (murni: supports/viaDebian/label/folderName/canPassword; mesin bawaan selalu didahulukan) dan ArchiveOps (penghubung list/extract)
- [x] feat(ui): ArchiveExtractDialog + Win7OpenDialog memakai ArchiveRoute/ArchiveOps; keterangan "Dibuka lewat 7z di Debian" untuk format Debian
- [x] fix(archive): DebianArchive.list menolak bagian non-pertama dengan petunjuk
- [x] Uji JVM sandbox: run_debian_archive_staging.sh 37 kasus (termasuk tidak ada tumpang tindih rute), run_archive_names.sh 52, run_7zip_command.sh, e2e proot palsu lulus
- [x] Dampak: :app saja (2 berkas baru + 3 diubah); Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh; ArchiveFormat/ArchiveEngine TIDAK diubah
- [ ] Compose (ArchiveExtractDialog, Win7OpenDialog) belum dikompilasi di sandbox: tunggu CI
- [ ] Uji perangkat: ekstrak .iso/.cab/.rar nyata via proot asli di Android 8; RAR nyata belum pernah diuji; lisensi RAR rootfs belum dicek
- [ ] Pemeriksaan ruang kosong di awal untuk jalur Debian (teruskan totalSize dari daftar) belum
- [ ] Berikutnya: uji arsip (7z t) dan edit/buat 7z bersandi + enkripsi nama via Debian

## 2026-10-30 - molina_dl v5: doctor, update yt-dlp, subtitle, sampul audio, limit-rate [DL-PKG1]
- [x] feat(dl): --doctor (event `doctor`: versi yt-dlp/ffmpeg, ffprobe, mutagen, folder, ruang kosong, kode masalah) dan --update-ytdlp (event `update`: start/ok/latest/failed; pipx upgrade atau pip install -U; coba ulang tanpa --break-system-packages untuk pip lama; mendeteksi PATH yang masih memakai yt-dlp lama)
- [x] feat(dl): --subs id,en [--subs-mode embed|file]; embed = mov_text (MP4) / bawaan (MKV) tanpa sisa berkas .srt (--compat-options no-keep-subs); file = .srt di samping video dan ikut dilaporkan di event `done`; peringatan `no_subs` bila bahasa tidak tersedia
- [x] fix(dl): ensure_compat (recode H.264/AAC) tidak lagi membuang aliran subtitle (-map 0:s? + -c:s mov_text)
- [x] feat(dl): audio otomatis menyematkan sampul (jpg) + tag judul/artis untuk MP3; M4A/FLAC butuh mutagen (atau AtomicParsley untuk M4A), bila tidak ada: peringatan `no_cover` dan audio tetap diunduh; --no-cover untuk menonaktifkan; stage baru `subs` dan `cover`
- [x] feat(dl): --limit-rate (500K, 2M); menu interaktif: Periksa lingkungan, Perbarui yt-dlp, pilihan subtitle
- [x] feat(core): DownloadEvent.Doctor/Update + parser, DownloadReducer (state.doctor/state.update), DownloadRequest(subs, subsMode, cover, limitRate) + validasi ketat, DownloadCommand.doctorArgs/updateArgs; label stage subs/cover di UnduhanScreen
- [x] Uji sandbox: run.sh 51+, run_ui.sh, run_feed.sh, run_history.sh lulus; test_molina_dl.py 26 uji lulus (unit + integrasi nyata yt-dlp/ffmpeg vs server lokal: subtitle embed/file/recode, sampul MP3, --no-cover, limit-rate, doctor, update dengan pip palsu)
- [x] Dampak: skrip aset + core/common + 1 string di UnduhanScreen; Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh; protokol tetap v1 (event baru diabaikan parser lama)
- [ ] CI belum dijalankan; Compose (UnduhanScreen) belum dikompilasi di sandbox
- [ ] UI belum: tombol Doctor/Perbarui yt-dlp (butuh mode perawatan di DownloadRunner), pilihan subtitle/sampul/batas kecepatan di form Unduhan
- [ ] DownloadChoices/Ulangi belum membawa subs/cover/limitRate (Ulangi memakai bawaan)
- [ ] Uji perangkat Android 8 arm64 (proot asli, yt-dlp apt Debian, YouTube nyata) belum; sampul tidak dipotong persegi (yt-dlp melewati konversi bila thumbnail sudah jpg), sebaiknya ContentScale.Crop di Molina Player
- [ ] Berikutnya: playlist terkontrol, preset profil, --download-archive, --section, event pemindaian media (MediaScannerConnection), sidecar poster/info.json untuk Molina Cinema

## 2026-10-30 - Bersihkan root repo dari patch/backup/skrip sekali pakai [REPO-CLEAN]
- [x] chore(repo): hapus 20 berkas sekali pakai dari root (7 .patch/.bak, 10 skrip molina_*.sh, patch_cli_open.py, molina_ui_v5_dock.py, explorer.py); riwayat git tetap menyimpannya; salinan lokal di ../molina-arsip
- [x] chore(repo): .gitignore menolak /*.patch, /*.bak*, /molina_*.sh, /patch_*.py (hanya di root)
- [x] Dampak: tidak ada kode, Gradle, AndroidManifest, JNI mpv; CI/workflow tidak merujuk berkas-berkas ini; commit memakai [skip ci]
- [ ] Berikutnya: pecah progres.md (sangat besar) menjadi docs/progres-arsip.md + ringkasan aktif; simpan skrip patch baru di luar repo

## 2026-10-30 - Tombol Periksa lingkungan / Perbarui yt-dlp di halaman Unduhan [DL-PKG2-MAINT]
- [x] feat(core): DownloadMaintenance (murni JVM): MaintenanceKind, argumen perawatan, tabel hasil Doctor, petunjuk perbaikan per kode isu (apt install ...), teks/nada hasil Update, ringkasan sesi; hasil update gagal dipangkas dan disamarkan (LogRedact)
- [x] feat(dl): DownloadRunner.startMaintenance(kind) + maintenanceKind(): jalur DOWNLOAD, skrip bawaan, timeout 10 menit, tidak masuk riwayat, tidak bisa diulang; state/clear() mereset jenis perawatan
- [x] feat(ui): dua tombol di form Unduhan, kartu ringkasan + kartu Doctor/Update berwarna; riwayat tidak lagi kehilangan entri terbaru setelah sesi perawatan
- [x] test: scripts/core-tests/run_maint.sh (DownloadMaintTests.kt, termasuk alur JSON -> parser -> feed -> formatter); run.sh, run_ui.sh, run_feed.sh, run_history.sh tetap lulus
- [x] Dampak: :core:common (1 berkas baru) + :app (DownloadRunner, UnduhanScreen); Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh; protokol tetap v1
- [ ] Compose (UnduhanScreen) dan DownloadRunner belum dikompilasi di sandbox: tunggu CI
- [ ] Uji perangkat Android 8 arm64: Doctor/Update nyata via proot (pip/pipx, jaringan) belum
- [ ] Form subtitle/sampul/batas kecepatan dan DownloadChoices/Ulangi yang membawa opsi baru belum
- [ ] Berikutnya: preset profil unduhan, sidecar poster.jpg/.info.json untuk Molina Cinema, playlist terkontrol

## 2026-10-30 - Form Unduhan: subtitle, sampul audio, batas kecepatan [DL-PKG3-FORM]
- [x] feat(core): DownloadChoices + subs/subsMode/cover/limitRate (bawaan sama dengan perilaku lama), SUB_PRESETS (Tanpa/ID/EN/ID+EN), LIMIT_RATES (Bebas/500K/2M/5M), sanitize tahan nilai rusak; pemanggilan lama tetap kompatibel
- [x] feat(ui): chip Subtitle + Sematkan/File .srt (video), Sampul + tag (audio), Batas kecepatan (keduanya); DownloadForm/DownloadPrefs mengingat pilihan antar sesi
- [x] feat(history): riwayat menyimpan dan Ulangi membawa opsi baru (kunci JSON sb/sm/cv/lr, versi tetap 1, berkas lama terbaca dengan bawaan); modeLabel menampilkan sub/batas hanya bila aktif
- [x] test: scripts/core-tests/run_choices.sh (DownloadChoicesTests.kt: sanitize, preset lolos validasi, roundtrip riwayat, kompatibilitas berkas lama/rusak, argumen Ulangi)
- [x] Dampak: :core:common (DownloadUi, DownloadHistory) + :app (DownloadNav, DownloadPrefs, UnduhanScreen); Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh; protokol tetap v1
- [ ] Compose (UnduhanScreen) belum dikompilasi di sandbox: tunggu CI; lebar baris chip di layar sempit perlu dicek di perangkat
- [ ] Uji perangkat Android 8 arm64: subtitle/sampul/batas kecepatan nyata via proot belum
- [ ] Preset cepat (Cinema/Hemat/Musik) belum mengatur subtitle/sampul/batas; belum ada input bahasa subtitle bebas
- [ ] Berikutnya: sidecar poster.jpg/.info.json untuk Molina Cinema, playlist terkontrol

## 2026-10-30 - Preset cepat mengatur subtitle dan sampul [DL-PKG4-PRESET]
- [x] feat(core): DownloadPreset (CINEMA/HEMAT/MUSIK) murni JVM dengan apply/matches/active; Cinema = 1080p MP4 + subtitle ID+EN embed, Hemat = 480p MP4 tanpa subtitle, Musik = MP3 320k + sampul; batas kecepatan dan format yang bukan milik preset tidak disentuh
- [x] feat(ui): chip preset di form Unduhan dibangun dari DownloadPreset (status terpilih ikut subtitle/sampul); petunjuk di bawahnya diperbarui
- [x] test: scripts/core-tests/run_presets.sh (apply/matches konsisten, lolos validasi perintah, idempoten, hanya satu preset aktif, kolom pengguna tidak tersentuh)
- [x] Dampak: :core:common (1 berkas baru) + :app (UnduhanScreen saja); Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] Compose belum dikompilasi di sandbox: tunggu CI; preset belum bisa dikustomisasi/disimpan pengguna
- [ ] Berikutnya: sidecar poster.jpg/.info.json untuk Molina Cinema, playlist terkontrol

## 2026-10-30 - Sidecar poster + metadata ringkas untuk Molina Cinema: sisi unduhan + core [DL-PKG5-SIDECAR]
- [x] feat(dl): molina_dl v6 `--sidecar` (video saja, OPT-IN): --write-thumbnail --write-info-json (+ --convert-thumbnails jpg bila ada ffmpeg); nama sidecar = nama video (`Film [id].jpg`, `Film [id].info.json`); mode audio mengabaikan dengan peringatan `sidecar_ignored`; peringatan `no_poster` bila situs tidak punya poster
- [x] feat(dl): `.info.json` diringkas (slim_info_json: ~20 kunci, deskripsi maks 2000, tag maks 20, atomik, idempoten) karena aslinya bisa beberapa MB (daftar format); event `done` membawa `sidecars` hanya bila ada (protokol tetap v1, parser lama mengabaikannya); cadangan selisih-folder tidak lagi memasukkan sidecar ke `files`
- [x] feat(core): MediaSidecar.find (poster jpg|jpeg|png|webp + .info.json bernama sama dengan video) dan MediaSidecar.readInfo (judul, kanal, durasi, tahun, deskripsi pendek, URL http/https; tahan berkas hilang/rusak/besar); DownloadRequest.sidecar (bawaan false) + argumen `--sidecar` hanya untuk video; DownloadEvent.Done.sidecars
- [x] keputusan: sidecar bawaan MATI (opt-in). Alasan: kontrak lama "folder unduhan tanpa sisa berkas" (test_embed_subs_mp4) dan poster/JSON akan muncul di galeri Android bila selalu dinyalakan
- [x] test: scripts/core-tests/run_sidecar.sh (MediaSidecarTests.kt) + test_molina_dl.py (SidecarUnitTests dan 4 uji integrasi nyata yt-dlp/ffmpeg); 36 uji Python (naik dari 26) dan 33 pemeriksaan Kotlin baru lulus; run.sh (51), run_ui, run_feed, run_history, run_choices, run_maint, run_presets tetap lulus
- [x] Dampak: skrip aset molina_dl.py + :core:common (1 berkas baru, DownloadCommand, DownloadEvent); Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh; hash skrip bawaan berubah (BundledScriptPolicy menyalin/menyiapkan .new seperti pkg1)
- [ ] Belum ada UI: tombol/chip "Poster + info" di form Unduhan, DownloadChoices/riwayat/Ulangi, dan preset Cinema belum menyalakan sidecar; DownloadSession belum menyimpan sidecars per item
- [ ] Rak film Molina Cinema (SIDE-3) belum ada: W7Thumb belum memprioritaskan poster sidecar; Compose belum dikompilasi di sandbox, tunggu CI
- [ ] Uji perangkat Android 8 arm64 (yt-dlp apt Debian via proot, situs nyata) belum

## 2026-10-30 - Chip "Poster + info" di form Unduhan + preset Cinema menyalakannya [DL-PKG6-SIDECAR-UI]
- [x] feat(core): DownloadChoices.sidecar (bawaan false; sanitize tahan nilai rusak); riwayat mencatat kunci `sc` (versi tetap 1, berkas lama terbaca dengan sidecar mati); Ulangi membawa sidecar ke `--sidecar`; modeLabel menampilkan "poster" hanya untuk video dan hanya bila aktif (urutan: sub, poster, batas)
- [x] feat(core): DownloadPreset: Cinema menyalakan poster + info, Hemat mematikannya, Musik tidak menyentuh (audio); chip Cinema/Hemat tidak lagi tampil terpilih bila poster diubah pengguna; HINT menyebut poster
- [x] feat(ui): chip "Poster + info" di bagian video UnduhanScreen (+ keterangan bahwa poster bisa tampil di Galeri); DownloadForm/DownloadPrefs mengingat pilihan antar sesi; DownloadRequest.sidecar hanya untuk video
- [x] test: run_choices.sh, run_presets.sh diperluas (sanitize, riwayat encode/decode, kompatibilitas berkas lama/rusak, Ulangi, modeLabel, preset apply/matches/argumen); semua tes lama tetap lulus
- [x] Dampak: :core:common (DownloadUi, DownloadHistory, DownloadPreset) + :app (DownloadNav, DownloadPrefs, UnduhanScreen); skrip molina_dl.py, Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] Compose (UnduhanScreen) belum dikompilasi di sandbox: tunggu CI; tata letak keterangan poster di layar sempit perlu dicek di perangkat
- [ ] Pengguna Cinema lama: chip Cinema tidak tampil terpilih sampai diketuk ulang (sidecar tersimpan mati)
- [ ] Berikutnya: SIDE-3 (W7Thumb memprioritaskan poster sidecar + rak film Molina Cinema); DownloadSession belum menyimpan sidecars per item

## 2026-10-30 - W7Thumb memprioritaskan poster sidecar di panel Penjelajah [SIDE3-THUMB-POSTER]
- [x] feat(cinema): W7Thumb.load: video yang punya poster sidecar (Film.jpg/.jpeg/.png/.webp di samping video, dari molina_dl --sidecar) memakai poster itu; poster rusak atau > 8 MB -> kembali ke frame video; audio tetap memakai cover art tertanam (jpg senama diabaikan)
- [x] perf(cinema): seluruh kerja berkas (stat poster, cache, dekode) kini di Dispatchers.IO (sebelumnya stat lastModified di thread pemanggil); poster didekode hemat memori (inSampleSize lewat ThumbMath.sampleSize); kunci cache memuat jalur + waktu ubah poster sehingga poster yang baru muncul menggantikan thumbnail frame lama
- [x] feat(core): ThumbMath.sampleSize (pangkat dua terbesar yang hasilnya masih >= target, dibatasi 4096, tahan masukan tak sah)
- [x] test: run_sidecar.sh +8 pemeriksaan ThumbMath; scripts/app-tests/run_w7thumb.sh (W7ThumbTests.kt, 12 pemeriksaan) memakai stub Android ringan (Bitmap/BitmapFactory berbasis ImageIO, MediaMetadataRetriever terkendali, LruCache, withContext inline); tes lama tetap lulus
- [x] Dampak: :app (W7Thumb.kt saja) + :core:common (1 berkas baru); UI Compose (W7BrowseDock) TIDAK diubah; Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] Tes W7Thumb memakai stub: perilaku API Android asli (BitmapFactory.decodeFile, orientasi EXIF, WebP) belum diuji di CI/perangkat; Compose belum dikompilasi di sandbox, tunggu CI
- [ ] Efek samping yang disengaja: jpg/png/webp senama video buatan pengguna sendiri juga dipakai sebagai thumbnail
- [ ] Berikutnya: rak film Molina Cinema (kartu poster dari MediaSidecar + readInfo), info.json di dialog Properti (W7MediaInfo)

## 2026-10-30 - Kompres 7z bersandi via Debian [ARCHIVE6-7Z-BERSANDI]
- [x] feat(archive): 7z bersandi lewat `7z a` di Debian; dua opsi enkripsi: isi saja, atau isi + nama berkas (-mhe=on); di-push ed60c7b
- [x] feat(archive): SevenZipCommand.createArgs/classifyCreate, DebianArchive.create, DebianArchiveStaging.commitFile/symlinkExcludes, ArchiveRoute.compressViaDebian, ArchiveOps.compress, dialog Kompres (dua pilihan), Win7OpenDialog memanggil ArchiveOps.compress
- [x] fix(archive): ArchiveEngine menolak 7z+sandi (fail-closed; sebelumnya akan menulis 7z TANPA sandi)
- [x] Temuan 7z 23.01: tautan folder diikuti (isi /etc ikut), tautan absolut membuat ekstrak gagal -> semua tautan dilewati via -x!; sumber bernama * atau ? membuat sumber hilang lolos diam-diam -> ditolak; exit 1 tetap menulis arsip -> dianggap gagal
- [x] Uji sandbox: SevenZipCommand (integrasi 7z asli), staging + e2e 7z asli, archive_names lulus; DebianArchive.create lulus dengan proot palsu + 7z asli; sandi non-ASCII dibuka py7zr
- [x] Dampak: :app dan core/common saja; Gradle, AndroidManifest, JNI mpv, izin TIDAK disentuh
- [ ] Compose (ArchiveCompressDialog, Win7OpenDialog) belum dikompilasi: tunggu CI
- [ ] Berkas uji ARCHIVE6 (SevenZipCommandTests, DebianArchiveStagingTests, ArchiveCompressTests, run_debian_archive_staging.sh) belum masuk repo
- [ ] Uji perangkat: buat 7z bersandi (kedua opsi) via proot asli di Android 8, buka di aplikasi lain
- [ ] Berikutnya: uji arsip (7z t), pemeriksaan ruang kosong di awal, tombol pasang 7zip sekali ketuk

## [ARCHIVE6-TESTS-RUN] Eksekusi tes manual
- CI tidak terpicu (sengaja, [skip ci]); tes dijalankan manual di Debian PRoot.
- p7zip-full di trixie = paket transisi; gunakan paket 7zip (7zz).
- Terkendala: kotlinc/java belum terpasang; langkah pasang JDK + kotlinc 2.1.21 dicatat.
- Status: menunggu hasil run_debian_archive_staging.sh.

## [SIDE3-SHELF-PREP] Persiapan rak film Molina Cinema
- [x] Clone ulang repo, HEAD c8add17 sinkron dengan origin/main
- [x] Patch poster paralel dibatalkan (duplikat DL-PKG6-SIDECAR-UI)
- [ ] Berikutnya: model kartu poster (MediaSidecar.readInfo + CinemaRecent) lalu rak di CinemaWin7Home
