# Upstream: mpv-android (basis Molina Cinema)

- Sumber: https://github.com/mpv-android/mpv-android
- Commit diimpor: `62013d44a6d30c26649a07c703c7626e9dd2ea99` (2026-10-20)
- Lisensi: MIT (Copyright (c) 2016 Ilya Zhuravlev, sfan5). Salinan: `feature/mpv/LICENSE-mpv-android`.
  Berkas `filepicker` berlisensi MPL-2.0 (header jangan dihapus).

## Diimpor (ke `feature/mpv`)
`app/src/main/{java,res,assets,AndroidManifest.xml}` dan `app/proguard-rules.pro`.

## Tidak diimpor
- `app/src/main/jni`: pustaka native dibangun oleh `scripts/native_mpv.sh` (workflow `native-mpv.yml`)
  dari commit yang sama; nama kelas JNI diubah ke `com.molina.cinema.MPVLib` saat build.
- `src/debug`, `fastlane/`, `docs/`, `buildscripts/`, gradle wrapper, README, flavor `allstorage`, ABI split.

## Memperbarui
Bandingkan commit baru dengan commit di atas (`git diff 62013d4..<baru> -- app/src/main`),
terapkan manual, lalu perbarui `native/pins.env` dan berkas ini.
