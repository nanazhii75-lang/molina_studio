<!-- handoff-v2 -->
# HANDOFF - Molina Studio

Disusun ulang 2026-10-24. Versi lama rusak (101 byte NUL dan teks acak sejak commit pertama `af13289`, tidak bisa dipulihkan dari git).
Untuk Claude lain yang melanjutkan. Baca seluruhnya sebelum menyentuh kode.

## 0. Cara memakai
1. `git pull`, lalu baca `progres.md` (seluruh berkas) dan `docs/BLUEPRINT.md`.
2. Dokumen bisa tertinggal dari kode. Contoh: BLUEPRINT Fase 5 belum mencentang 5a-5c padahal kodenya ada. Verifikasi di kode sebelum menyatakan sesuatu selesai atau belum.
3. Bila dokumen ini bertentangan dengan keputusan terbaru pemilik, ikuti pemilik lalu perbarui berkas ini.
4. CI hijau bukan uji perangkat. JNI, UI, dan perilaku pemutaran tidak terbukti oleh CI. Jangan menulis "lulus" tanpa bukti.
5. Saat menerima konteks dari sesi lain, rangkum pemahaman dan minta konfirmasi pemilik sebelum mengerjakan.

## 1. Aturan kerja (wajib)
- Bahasa Indonesia profesional, teknis, langsung ke solusi. Beri saran proaktif soal branding dan fungsi produk.
- Build hanya lewat GitHub Actions. Tidak ada build lokal.
- Otomatisasi: Python 3 here-document `python3 << 'PYEOF'`, siap-copy, tanpa placeholder, WAJIB idempotent (periksa kondisi sebelum menulis).
- Setiap kemajuan dicatat di `progres.md` (sertakan snippet pembaruannya di akhir tugas).
- Commit memakai Conventional Commits. Commit non-kode memakai `[skip ci]`.
- ATURAN PUSH: push commit KODE lebih dulu (CI jalan), commit docs `[skip ci]` di push TERPISAH. Bila digabung, GitHub melewati CI untuk seluruh push (terjadi 2026-10-24, 3 push terdampak).
- Beri peringatan dampak bila perubahan menyentuh Gradle, `AndroidManifest.xml`, atau JNI mpv.
- Mulai pekerjaan dari clone/pull repo resmi: https://github.com/nanazhii75-lang/molina_studio.git

## 2. Lingkungan
- Perangkat uji: Android 8.0 (API 26), arm64-v8a saja.
- Folder kerja: `/storage/emulated/0/molina-studio` (di Debian: `/sdcard/molina-studio`).
- Git di PRoot pada storage bersama: `git config core.createObject rename` (hardlink tidak andal).
- Lisensi kode Molina: GPL-3.0-or-later (FFmpeg GPL, NewPipeExtractor GPLv3). Atribusi upstream: `THIRD_PARTY_NOTICES.md`, `docs/UPSTREAM.md`.
- Penandatanganan APK: `docs/SIGNING.md` (secrets `MOLINA_KEYSTORE_B64`, `MOLINA_KEYSTORE_PASSWORD`).

## 3. Arsitektur singkat
- Modul: `:app` (perakit) > `:feature:mpv` (sudah modul), `:core:common` (Kotlin murni), `:core:storage`. Terminal, emulator, dan view masih di `:app` atau vendored (`terminal-emulator`, `terminal-view`, dari Termux v0.118.0). Rencana lengkap: BLUEPRINT bagian 5.
- Kontrak di `core:common` yang SUDAH ada: `ExitHooks`, `LogLevel/LogEvent/LogSink/AppLog`, `AppVisibility`, `MolinaPaths` + `MolinaDir`, `LogRedact`.
- Kontrak yang BELUM ada di kode: `StreamResolver`, `SettingsSection`, `MolinaCommand/CommandBus`.
- Penyimpanan: workspace `/sdcard/molina-studio`, data bersama `/sdcard/.molina-data`, rahasia di `filesDir`/Keystore.
- Native mpv: dibangun `native-mpv.yml` dari mpv-android `62013d4`; versi dipin di `native/pins.env`, hash unduhan di `native/pins.sha256`, rilis yang dipakai APK di `native/prebuilt.env`. Mengubah pin harus SADAR dan dicatat di `progres.md`.
- Gerbang telemetri `scripts/telemetry_gate.py` memeriksa host, izin, dan dependensi. Mengubah allowlist harus SADAR dan dicatat. Folder `scripts/` dan baris komentar tidak dipindai.
- Log: semua log mpv lewat `MpvLog` dan `PlaybackLog` yang disamarkan `LogRedact` (URL hanya skema+host, token, path lokal). Jangan memakai `android.util.Log` langsung di `feature/mpv`.
- Keamanan intent: `MPVActivity` exported. Extra `filepath` divalidasi `isAllowedCustomPath` (allowlist skema, tolak path data privat, playlist `memory://` hanya bila tiap entri lolos, maks 1000). Jangan melonggarkan.
- Gestur geser horizontal wajib lewat `Modifier.onHorizontalSwipe` (`com.molina.studio.ui`).
- Pola setelan: `MpvPrefs` membungkus SharedPreferences bawaan (layar setelan XML menulis ke berkas yang sama), bukan DataStore. Ekstrak base class generik hanya bila ada 3 store atau lebih.

## 4. Status fase (2026-10-24)
- Fase 1-4 (fondasi/CI, rebranding, Terminal PRoot Debian, Log+IPC): selesai, uji perangkat dilaporkan lulus oleh pemilik. Checklist `docs/UJI_PERANGKAT.md` belum dicentang per item.
- Fase 5 (Molina Cinema/mpv):
  - 5a native libmpv arm64 di CI: selesai (pin terisi sadar).
  - 5b putar lokal, Cinema Home, `MpvPrefs`, hook keluar mpv: kode selesai.
  - 5c event ke AppLog (PlaybackLog, AudioLog, service latar): kode selesai.
  - Tambahan UI: pill kontrol, dialog Settings dan playlist gelap, item aktif biru, Audio only, ikon cari/URL di player, folder jadi playlist (sakelar `play_folder`), prev/next notifikasi, badge LIVE.
  - Perbaikan `memory://` (commit `862e434`).
  - 5d NewPipe (`:engine:newpipe`): BELUM. Spike `spike-newpipe.yml` belum dijalankan.
  - BLUEPRINT 5a/5b/5c belum dicentang menunggu uji perangkat seksi G.
- Fase 6 (Code Editor Rosemoe) dan Fase 7 (Settings Hub): rencana. Urutan keputusan pemilik: mpv sebelum Code Editor.
- Status CI commit terbaru TIDAK terverifikasi dari sesi penyusunan ini (batas API GitHub). Cek tab Actions.

## 5. Utang sebelum 5d
Selesai: `memory://` ditolak (diperbaiki `862e434`), `HANDOFF.md` ditulis ulang.
Masih terbuka:
1. SELESAI (kode, menunggu CI + uji perangkat): kontrak `StreamResolver` + `ResolvedStream`/`StreamSearchItem`/`StreamOutcome`/`StreamError` dan penyaring `StreamHeaders` di `core:common`. Implementasi nanti di `:engine:newpipe`.
2. SELESAI (kode, menunggu CI + uji perangkat): header stream ke mpv lewat opsi `loadfile` per-entri (`MpvLoad`), jadi tiap entri playlist membawa headernya sendiri; extra intent `headers` didukung. Keputusan: `tls_verify` dari intent TIDAK didukung. Catatan asli: `MPVActivity.kt` punya `TODO: headers` di parsing extra intent. Opsi mpv `http-header-fields`. Tautan YouTube sering butuh User-Agent/Referer; `MPVView` memasang user-agent `MolinaCinema/<versi>` (risiko HTTP 403, hapus bila ada server menolak). Catatan desain: M3U `memory://` tidak bisa membawa header per entri; gunakan `loadfile` per-entri. URL stream bisa kedaluwarsa: untuk antrean panjang simpan `pageUrl` dan panggil `resolve` sesaat sebelum diputar (lazy), bukan me-resolve semua di depan (bagian 5d).
3. Jalankan spike NewPipe manual dari Actions (`spike-newpipe.yml`); koordinat JitPack: `com.github.TeamNewPipe:NewPipeExtractor:<versi>`. Minimal SDK 26 sudah sama dengan syarat, tanpa desugaring. Host YouTube perlu masuk gerbang telemetri/allowlist secara sadar.
4. Perbarui seksi G `docs/UJI_PERANGKAT.md` (tambahkan: playlist gelap/biru, folder playlist + sakelar, prev/next notifikasi, Audio only, ikon cari/URL, pill kontrol, multi-pilih `memory://`), jalankan uji, lalu centang BLUEPRINT 5a-5c.
5. Verifikasi CI hijau dan uji APK terbaru di perangkat.

Utang kecil: dialog track/speed/subtitle masih terang; teks sakelar baru hanya bahasa Inggris (13 locale); `onNewIntent` belum memuat folder; tint ikon PiP belum diverifikasi; FIXME upstream (`RESULT_OK` vs `CANCELED`, kemungkinan race di `BaseMPVView`, hack di `BackgroundPlaybackService`); alasan end-file tidak ada di JNI sehingga `PlaybackLog` tidak mencatatnya.
Bukan utang fase mpv: optimasi RAM, snapshot `MolinaDir.Backup`, Fase 6.

## 6. Jebakan yang pernah terjadi
- Push gabungan kode + docs `[skip ci]` melewati CI (lihat aturan push).
- Dependensi `feature/mpv` sengaja diturunkan agar cocok AGP 8.7.3; naikkan bersama AGP (lihat `docs/UPSTREAM.md`).
- Arsip unduhan native bisa berubah di sisi server: bila 'SHA-256 TIDAK COCOK', tinjau dulu sebelum memperbarui pin.
- JNI native dan UI Compose tidak terbukti oleh CI; wajib uji perangkat.
- `/sdcard/Molina` di handoff lama = salah tulis; yang benar `/sdcard/molina-studio`.

## 7. Langkah berikutnya yang disarankan
(a) kontrak `StreamResolver` + dukungan header di mpv, (b) perbarui seksi G dan uji perangkat, (c) jalankan spike NewPipe, (d) modul `:engine:newpipe` + pencarian judul di ikon cari player, (e) yt-dlp (Debian PRoot) sebagai cadangan opt-in setelah NewPipe.
