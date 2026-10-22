# Third-party notices

- `terminal-emulator/` dan `terminal-view/`: disalin dari https://github.com/termux/termux-app
  tag v0.118.0 (commit 6e2689f55295fa444be8ac8592c527c2c5ef3253). Berasal dari
  Android Terminal Emulator (Apache-2.0) dengan modifikasi Termux. Cek lisensi
  upstream sebelum rilis publik.

- `app/src/main/jniLibs/arm64-v8a/libproot*.so`: PRoot prebuilt dari https://github.com/green-green-avk/build-proot-android (commit 01f83b88, paket proot-android-aarch64.tar.gz). PRoot berlisensi GPL-2.0-or-later.
- Rootfs Debian trixie-slim arm64 diunduh saat runtime dari https://github.com/debuerreotype/docker-debian-artifacts (commit 9c4818e).
