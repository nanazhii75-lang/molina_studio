# BLUEPRINT ARSITEKTUR FINAL & SPESIFIKASI TEKNIS: MOLINA STUDIO

**Target System:** Android 8.0 (API Level 26) | **Target ABI:** `arm64-v8a` Only

**Official Repository:** `[https://github.com/nanazhii75-lang/molina_studio.git](https://github.com/nanazhii75-lang/molina_studio.git)`

**Execution Environment:** Linux Debian via PRoot | **Working Path:** `/storage/emulated/0/molina-studio` (Debian: `/sdcard/molina-studio`)

---

## 1. DOKUMENTASI LENGKAP & ALUR SISTEM METODE TERINTEGRASI

MOLINA STUDIO diarsitekturkan sebagai **Modular Single-Activity System** dengan **Unidirectional Data Flow (UDF)** memanfaatkan Jetpack Compose / View Binding, StateFlow, Coroutines, dan Native Inter-Process Communication (IPC).

```
 +---------------------------------------------------------------------------------------------------+
 |                                   MOLINA STUDIO UI CORE SHELL                                      |
 |                           (Jetpack Navigation Multi-Host / BottomNav)                             |
 +------------------+--------------------+--------------------+--------------------+-----------------+
                    |                    |                    |                    |
                    v                    v                    v                    v
          +------------------+ +------------------+ +------------------+ +------------------+
          |  1. MPV PLAYER   | |   2. TERMINAL    | |  3. CODE EDITOR  | |   4. SETTINGS    |
          |  (Molina Cinema) | | (PRoot Subsystem)| | (Rosemoe Native) | |    (State/IPC)   |
          +--------+---------+ +--------+---------+ +--------+---------+ +--------+---------+
                   |                    |                    |                    |
                   | (NewPipe Extractor)| (PTY Native FD)    | (File Stream)      | (DataStore)
                   v                    v                    v                    v
 +---------------------------------------------------------------------------------------------------+
 |                                     SHARED CORE SUBSYSTEM                                         |
 |  - Local Unix Domain Socket (/tmp/molina.sock)                                                    |
 |  - File System Abstraction Layer (/storage/emulated/0/molina-studio)                               |
 |  - ActivityLog & LogGrouping.kt Engine                                                            |
 +---------------------------------------------------------------------------------------------------+

```

---

## 2. RINCAN DETAIL 4 NAVIGASI & TEKNOLOGI ACUAN RESMI

### Navigasi 1: MPV Player (Molina Cinema) + NewPipe Extractor

* **Core Engine:** `libmpv` NDK JNI. Surface di-render via OpenGL ES 3.0 Context (`mpv_render_context`).
* **NewPipe Extractor Service:** Modul Java/Kotlin yang mengurai URL (YouTube, SoundCloud, dll) secara langsung di perangkat tanpa YouTube Data API v3. Output berupa direct Stream URL (HLS/DASH/MP4) dan meta-data audio/video.
* **Logcat & Activity Logging (`LogGrouping.kt`):** Mengirim event pemutaran (file name, duration, current seek, buffer rate) ke bus socket internal `/tmp/molina.sock`.
* **Referensi Resmi:**
* *mpv-android* Engine JNI Integration (`[github.com/mpv-android/mpv-android](https://github.com/mpv-android/mpv-android)`)
* *TeamNewPipe/NewPipeExtractor* Standalone Parser (`[github.com/TeamNewPipe/NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)`)



### Navigasi 2: Terminal (PRoot Debian Subsystem)

* **Core Engine:** Engine Terminal berbasis `libvterm` & `termux-app` PTY Driver.
* **Environment:** PRoot Debian yang terisolasi di `/sdcard/molina-studio`. Bekerja menggunakan emulasi `ptrace` tanpa memerlukan akses root.
* **Git Native Integration:** Git berjalan di dalam Debian PRoot. Proses kompilasi kode dilarang secara lokal dan wajib dieksekusi di **GitHub Actions CI**. Commit non-kode otomatis menyertakan tag `[skip ci]`.
* **Referensi Resmi:**
* *Termux App Architecture* PTY Execution (`[github.com/termux/termux-app](https://github.com/termux/termux-app)`)
* *PRoot Documentation* for Linux User-space Emulation (`proot-termux`)



### Navigasi 3: Code Editor (Local Native Android)

* **Core Engine:** **Rosemoe CodeEditor** (100% Native Canvas Drawing & Custom Android View).
* **Text Parser:** Engine sintaksis menggunakan *Tree-sitter C++ JNI Bindings* / *TextMate Grammar*.
* **Akses Berkas Direct:** Membaca dan menulis secara langsung ke direktori kerja `/storage/emulated/0/molina-studio`.
* **Referensi Resmi:**
* *Rosemoe/CodeEditor* Native View Architecture (`[github.com/Rosemoe/CodeEditor](https://github.com/Rosemoe/CodeEditor)`)
* *Tree-sitter* Parsing System (`tree-sitter.github.io/tree-sitter`)



### Navigasi 4: Setting & Integration Hub

* **Core Engine:** Android Jetpack DataStore (Preferences & Proto DataStore).
* **Inter-Module Routing & Config:**
* Mengatur skema sinkronisasi log dari MPV ke Terminal (`LogGrouping.kt`).
* Mengelola *hot-reload* file konfigurasi `mpv.conf` dan `input.conf` dari Code Editor langsung ke MPV Service.
* Opsi *Pembersihan URL Legacy* (donasi, tracker, telemetry).


* **Referensi Resmi:**
* *Android Developers:* DataStore Architecture Guide (`[developer.android.com/topic/libraries/architecture/datastore](https://developer.android.com/topic/libraries/architecture/datastore)`)



---

## 3. SPESIFIKASI ALUR INTEGRASI IPC (INTER-PROCESS COMMUNICATION)

```
[NewPipe Extractor] ---> (Stream URL) ---> [MPV Player Engine]
                                                  |
                                                  v (Send Event Logs)
[Code Editor] <--- (Edit mpv.conf) ---> [/tmp/molina.sock]
     |                                            |
     v (Save File)                                v (Receive Log Stream)
[/storage/emulated/0/molina-studio] <--- [PRoot Terminal Subsystem]

```

1. **Streaming Flow:** User memasukkan link video $\rightarrow$ NewPipe Extractor menguraikan stream $\rightarrow$ Stream URI diserahkan ke JNI `libmpv` $\rightarrow$ Render di SurfaceView.
2. **Log Logging Flow:** Modul MPV memancar event data $\rightarrow$ Ditangkap oleh `LogGrouping.kt` $\rightarrow$ Disalurkan melalui Unix Socket `/tmp/molina.sock` $\rightarrow$ Ditampilkan pada tab Log Terminal.
3. **Configuration & Live Edit Flow:** User mengubah `mpv.conf` di Code Editor Native $\rightarrow$ Berkas diperbarui di `/storage/emulated/0/molina-studio/mpv.conf` $\rightarrow$ Settings Hub mengirimkan signal `loadfile/reload` via JNI mpv tanpa restart aplikasi.

---

## 4. AUTOMATION PYTHON SCRIPT (IDEMPOTENT PATCHING)

Berikut adalah skrip Python *here-document* idempotent yang dijalankan dari `/storage/emulated/0/molina` untuk memperbarui berkas `progres.md` dan membersihkan telemetry/URL eksternal lama:

```bash
python3 << 'PYEOF'
import os
import re

WORKING_DIR = "/storage/emulated/0/molina-studio"
PROGRESS_FILE = os.path.join(WORKING_DIR, "progres.md")

# Ensure base directory exists
os.makedirs(WORKING_DIR, exist_ok=True)

progress_template = """# MOLINA STUDIO - FINAL ARCHITECTURE PROGRESS

## Project Metadata
- **Application studio:** MOLINA STUDIO (Molina Terminal & Molina Cinema/Player)
- **Repository Address:** https://github.com/nanazhii75-lang/molina_studio.git
- **Target Platform:** Android 8.0 (API Level 26) - arm64-v8a Only
- **Execution Working Dir:** /storage/emulated/0/molina-studio (Debian: /sdcard/molina-studio)

---

## Comprehensive Implementation Status

### [Phase 1: Architecture & System Blueprint] - COMPLETED
- [x] Perancangan Blueprint Final 4 Navigasi (MPV, Terminal, Code Editor, Settings).
- [x] Integrasi NewPipeExtractor untuk streaming tanpa API eksternal.
- [x] Pemilihan Rosemoe CodeEditor sebagai engine native editor berbasis Canvas & Tree-Sitter.
- [x] Spesifikasi IPC Unix Socket (`/tmp/molina.sock`) untuk integrasi `LogGrouping.kt`.
- [x] Konfigurasi build pipeline: GitHub Actions CI (Local build disabled, [skip ci] for docs).

### [Phase 2: Rebranding & Code Cleanup] - COMPLETED (diverifikasi 2026-10-23)
- [x] Refactoring Package Name menyeluruh menjadi `com.molina.studio`.
- [x] Pembersihan URL eksternal, tracker, donasi, dan server update lama.
- [x] Penyusun UI Navigation Shell 4 Tab dengan Jetpack Compose / Material 3.
- Catatan: paket library vendored kini `com.molina.terminal` (emulator) dan `com.molina.terminal.view` (view).

### [Phase 3: Terminal (PRoot Debian)] - COMPLETED (uji perangkat dilaporkan lulus pengguna, 2026-10-23)
- [x] Emulator Termux v0.118.0 (vendored), Debian trixie arm64 via PRoot, TerminalService, AppExit.
- [x] Multi-session maks 5, drawer sesi, extra keys, tema prompt, TerminalPrefs (DataStore + StateFlow).
- [x] Tanda tangan APK tetap via GitHub Secrets (docs/SIGNING.md).
- [x] Uji perangkat penuh (docs/UJI_PERANGKAT.md), dilaporkan lulus pengguna 2026-10-23.

### [Phase 4: Log & IPC] - COMPLETED (uji perangkat dilaporkan lulus pengguna, 2026-10-23)
- [x] LogGrouping + IPC LocalSocket abstract `@molina.log`, tampilan di Settings.

### [Phase 5: MPV Player / Molina Cinema] - NEXT (diprioritaskan sebelum Phase 6)
- [x] Prasyarat inti lunas: `MolinaPaths` + `AppPaths`, `MolinaApp` (`AppLog.install`), `ExitHooks`, `AppVisibility`; izin storage diminta sekali di `MainActivity`.
- [x] 5a libmpv arm64 di CI (artefak dipin, hash terverifikasi).
- [ ] 5b putar lokal + hook ExitHooks + MpvPrefs dan 5c event ke AppLog: KODE selesai, CI hijau; menunggu uji perangkat seksi G (`docs/UJI_PERANGKAT.md`) sebelum dicentang.
- [ ] 5d NewPipe (`:engine:newpipe`): kontrak `StreamResolver` + header per entri selesai (CI hijau, belum diuji di perangkat); spike HIJAU (hash di `scripts/newpipe/pins.sha256`); 5d-1: modul `:engine:newpipe` dibuat (NewPipeStreamResolver, MolinaDownloader HTTPS-only, StreamPicker, tugas verifyNewPipePins); CI hijau (run #67). 5d-2a: resolver terpasang di player (URL awal/Cinema Home, dialog URL, antrean, intent baru) lewat StreamResolvers + StreamLoader, judul lewat force-media-title; menunggu CI + uji perangkat. 5d-2b (pencarian judul) belum.

### [Phase 6: Code Editor (Rosemoe)] - PLANNED (dikerjakan setelah Phase 5 mpv)
- [x] Prasyarat modul: `:core:common` + `:core:storage`; `ExitHooks`; `LogLevel`/`LogEvent`/`LogSink`/`AppLog`; `AppVisibility`.
- [x] Prasyarat `MolinaPaths` + folder dan kelas Application (`AppLog.install`) lunas.
- [ ] `:feature:code` (Rosemoe) + hook `ExitHooks.register("editor")` + perintah `molina open`.

### [Phase 7: Settings Hub] - PLANNED
- [ ] Tombol Keluar (AppExit), hot-reload mpv.conf.
""" 
```

---

## 5. ARSITEKTUR MODUL & PENYIMPANAN BERSAMA (rencana, mulai Fase 6)

Tujuan: kontrak antar modul eksplisit (tanpa dependensi melingkar) dan satu pintu penyimpanan bersama.

### 5.1 Modul & aturan dependensi
```
:app (perakit) --> :feature:{terminal, mpv, code, settings}
       |       --> :engine:newpipe
       v
:feature:* ---> :core:storage ---> :core:common <--- :engine:*
:feature:terminal ---> :terminal-view (vendored, tidak diubah)
```
- `feature:*` tidak saling bergantung; hanya ke `core:common`, `core:storage` (terminal juga ke `:terminal-view`).
- `engine:*` = Kotlin/JVM, mengimplementasi kontrak dari `core:common` (mis. `StreamResolver`); tidak melihat `feature:*`.
- `feature:settings` tidak mengimpor fitur lain: tiap fitur mengekspos `SettingsSection`; `:app` mengumpulkan dan memberikannya.
- `core:common` = Kotlin murni (tanpa Context/Uri/Compose); `core:storage` mengimplementasi `MolinaPaths`.
- Hanya `:app` yang merakit implementasi (service locator manual) dan boleh melihat semua modul.
- Urutan migrasi: mpv + engine:newpipe (baru, Fase 5) > code (`:feature:code`, Fase 6) > settings > terminal paling akhir.
- Terminal paling akhir karena berisiko: TerminalService di manifest, assets `molina/bashrc`, jniLibs + `useLegacyPackaging` tetap di `:app`.

### 5.2 Isi `core:common`
- `ExitHook` + registri `ExitHooks` (pengganti pemanggilan langsung `AppExit`).
- `LogLevel`, `LogEvent`, `LogSink` + pintu `AppLog` (pengganti ketergantungan langsung ke `LogBus`).
- `MolinaCommand` (`OpenFile(path)`, `Play(uri)`) + `CommandBus` untuk jembatan CLI `molina`.
- `MolinaPaths` (antarmuka lokasi private/shared/subfolder) dan `ModuleId`.
- `SettingsSection` (halaman pengaturan per fitur) dan `StreamResolver` (kontrak resolusi URL stream; implementasi di `:engine:newpipe`).
- `AppVisibility` (status foreground: `StateFlow` read-only; `:app` menulis lewat `setForeground`).
- Sudah dipindah: `ExitHooks` (dari `AppExit`), `LogLevel`/`LogEvent`/`LogSink`/`AppLog` (`LogBus` tetap di `:app` sebagai implementasi `LogSink`).
- Sudah dipindah juga: `AppVisibility` (`StateFlow` read-only, ditulis hanya oleh `:app`). Tidak ada kandidat core lagi. `MolinaLog` (terminal) bukan kandidat core: logger `android.util.Log` terikat `TerminalPrefs`, ikut `:feature:terminal`.

---

## 6. PEMBARUAN FILE PROGRES (`progres.md`)

```markdown
# MOLINA STUDIO - FINAL ARCHITECTURE PROGRESS

## Project Metadata
- **Application studio:** MOLINA STUDIO (Molina Terminal & Molina Cinema/Player)
- **Repository Address:** https://github.com/nanazhii75-lang/molina_studio.git
- **Target Platform:** Android 8.0 (API Level 26) - arm64-v8a Only
- **Execution Working Dir:** /storage/emulated/0/molina-studio (Debian: /sdcard/molina-studio)

---

## Comprehensive Implementation Status

### [Phase 1: Architecture & System Blueprint] - COMPLETED
- [x] Perancangan Blueprint Final 4 Navigasi (MPV, Terminal, Code Editor, Settings).
- [x] Integrasi NewPipeExtractor untuk streaming tanpa API eksternal.
- [x] Pemilihan Rosemoe CodeEditor sebagai engine native editor berbasis Canvas & Tree-Sitter.
- [x] Spesifikasi IPC Unix Socket (`/tmp/molina.sock`) untuk integrasi `LogGrouping.kt`.
- [x] Konfigurasi build pipeline: GitHub Actions CI (Local build disabled, [skip ci] for docs).

### [Phase 2: Rebranding & Code Cleanup] - COMPLETED (diverifikasi 2026-10-23)
- [x] Refactoring Package Name menyeluruh menjadi `com.molina.studio`.
- [x] Pembersihan URL eksternal, tracker, donasi, dan server update lama.
- [x] Penyusun UI Navigation Shell 4 Tab dengan Jetpack Compose / Material 3.
- Catatan: paket library vendored kini `com.molina.terminal` (emulator) dan `com.molina.terminal.view` (view).

### [Phase 3: Terminal (PRoot Debian)] - COMPLETED (uji perangkat dilaporkan lulus pengguna, 2026-10-23)
- [x] Emulator Termux v0.118.0 (vendored), Debian trixie arm64 via PRoot, TerminalService, AppExit.
- [x] Multi-session maks 5, drawer sesi, extra keys, tema prompt, TerminalPrefs (DataStore + StateFlow).
- [x] Tanda tangan APK tetap via GitHub Secrets (docs/SIGNING.md).
- [x] Uji perangkat penuh (docs/UJI_PERANGKAT.md), dilaporkan lulus pengguna 2026-10-23.

### [Phase 4: Log & IPC] - COMPLETED (uji perangkat dilaporkan lulus pengguna, 2026-10-23)
- [x] LogGrouping + IPC LocalSocket abstract `@molina.log`, tampilan di Settings.

### [Phase 5: MPV Player / Molina Cinema] - NEXT (diprioritaskan sebelum Phase 6)
- [x] Prasyarat inti lunas: `MolinaPaths` + `AppPaths`, `MolinaApp` (`AppLog.install`), `ExitHooks`, `AppVisibility`; izin storage diminta sekali di `MainActivity`.
- [x] 5a libmpv arm64 di CI (artefak dipin, hash terverifikasi).
- [ ] 5b putar lokal + hook ExitHooks + MpvPrefs dan 5c event ke AppLog: KODE selesai, CI hijau; menunggu uji perangkat seksi G (`docs/UJI_PERANGKAT.md`) sebelum dicentang.
- [ ] 5d NewPipe (`:engine:newpipe`): kontrak `StreamResolver` + header per entri selesai (CI hijau, belum diuji di perangkat); spike HIJAU (hash di `scripts/newpipe/pins.sha256`); modul belum dibuat.

### [Phase 6: Code Editor (Rosemoe)] - PLANNED (dikerjakan setelah Phase 5 mpv)
- [x] Prasyarat modul: `:core:common` + `:core:storage`; `ExitHooks`; `LogLevel`/`LogEvent`/`LogSink`/`AppLog`; `AppVisibility`.
- [x] Prasyarat `MolinaPaths` + folder dan kelas Application (`AppLog.install`) lunas.
- [ ] `:feature:code` (Rosemoe) + hook `ExitHooks.register("editor")` + perintah `molina open`.

### [Phase 7: Settings Hub] - PLANNED
- [ ] Tombol Keluar (AppExit), hot-reload mpv.conf.

```