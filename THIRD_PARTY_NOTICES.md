# Third-party notices

- `terminal-emulator/` dan `terminal-view/`: disalin dari https://github.com/termux/termux-app
  tag v0.118.0 (commit 6e2689f55295fa444be8ac8592c527c2c5ef3253). Berasal dari
  Android Terminal Emulator (Apache-2.0) dengan modifikasi Termux. Cek lisensi
  upstream sebelum rilis publik.
  Paket Java diganti ke `com.molina.terminal` / `com.molina.terminal.view` dan library native ke `libmolina_pty` (rebranding); atribusi, lisensi, dan kode asal dipertahankan.

- `app/src/main/jniLibs/arm64-v8a/libproot*.so`: PRoot prebuilt dari https://github.com/green-green-avk/build-proot-android (commit 01f83b88, paket proot-android-aarch64.tar.gz). PRoot berlisensi GPL-2.0-or-later.
- Rootfs Debian trixie-slim arm64 diunduh saat runtime dari https://github.com/debuerreotype/docker-debian-artifacts (commit 9c4818e).

- `feature/code` memakai sora-editor (Rosemoe) `io.github.Rosemoe.sora-editor:editor:0.23.6` dari Maven Central, tanpa modifikasi. Lisensi LGPL-2.1-or-later (kompatibel dengan GPL-3.0-or-later proyek ini). Sumber: https://github.com/Rosemoe/sora-editor. Versi dipilih sadar: 0.23.6 adalah rilis terbaru yang dibangun dengan compileSdk 35; 0.23.7+ mensyaratkan compileSdk 36 dan 0.24.x juga mensyaratkan Kotlin 2.2+.

- `feature/code/src/main/assets/fonts/`: JetBrains Mono (JetBrainsMono-Regular.ttf, https://github.com/JetBrains/JetBrainsMono, SIL OFL 1.1, salinan lisensi OFL-JetBrainsMono.txt) dan Fira Code 6.2 (FiraCode-Regular.ttf, https://github.com/tonsky/FiraCode, SIL OFL 1.1). Dipakai sebagai pilihan font editor, tanpa modifikasi. OFL mengizinkan penyertaan dan distribusi bersama aplikasi.

- `feature/code/src/main/assets/textmate/`: grammar TextMate java, kotlin, python, javascript, xml, html, lua, markdown disalin dari aplikasi contoh sora-editor 0.23.6 (https://github.com/Rosemoe/sora-editor); grammar json, shell, css dari https://github.com/microsoft/vscode/tree/main/extensions (lisensi MIT, salinan LICENSE-vscode-MIT.txt). Tema abyss dan solarized_dark berasal dari contoh yang sama (tema VS Code, MIT). `obsidian_claude.json` dibuat oleh Molina. Tema darcula dari contoh sora TIDAK dipakai (asal Eclipse tm4e, EPL, tidak kompatibel dengan GPL). Pustaka `language-textmate:0.23.6` (LGPL-2.1-or-later) menarik gson, joni, jcodings, snakeyaml-engine (MIT/Apache-2.0).
