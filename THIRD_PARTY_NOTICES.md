# Third-party notices

- `terminal-emulator/` dan `terminal-view/`: disalin dari https://github.com/termux/termux-app
  tag v0.118.0 (commit 6e2689f55295fa444be8ac8592c527c2c5ef3253). Berasal dari
  Android Terminal Emulator (Apache-2.0) dengan modifikasi Termux. Cek lisensi
  upstream sebelum rilis publik.
  Paket Java diganti ke `com.molina.terminal` / `com.molina.terminal.view` dan library native ke `libmolina_pty` (rebranding); atribusi, lisensi, dan kode asal dipertahankan.

- `app/src/main/jniLibs/arm64-v8a/libproot*.so`: PRoot prebuilt dari https://github.com/green-green-avk/build-proot-android (commit 01f83b88, paket proot-android-aarch64.tar.gz). PRoot berlisensi GPL-2.0-or-later.
- Rootfs Debian trixie-slim arm64 diunduh saat runtime dari https://github.com/debuerreotype/docker-debian-artifacts (commit 9c4818e).

- `feature/code` memakai sora-editor (Rosemoe) `io.github.Rosemoe.sora-editor:editor:0.24.6` dari Maven Central, tanpa modifikasi. Lisensi LGPL-2.1-or-later (kompatibel dengan GPL-3.0-or-later proyek ini). Sumber: https://github.com/Rosemoe/sora-editor. Versi dipilih sadar: rilis Jun 2026 (lebih dari 2 pekan), versi 0.24.7 sengaja dilewati karena baru terbit.
