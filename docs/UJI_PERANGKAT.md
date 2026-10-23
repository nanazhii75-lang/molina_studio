# Checklist Uji Perangkat (Android 8.0, arm64)
Centang `[x]` bila lulus. Catat build (nomor Run CI) di atas hasil uji.

## A. Instalasi & kunci tetap
- [ ] Log CI: "Kunci tetap dipakai" + SHA-256 sama di 2 build berturut-turut
- [ ] Uninstall build lama (terakhir), pasang APK baru, rootfs Debian terunduh & terverifikasi
- [ ] Build berikutnya terpasang sebagai UPDATE (tanpa uninstall); pengaturan tetap

## B. Terminal dasar
- [ ] Banner emas "Welcome Molina Terminal"; prompt hijau di home, merah di luar (`cd /tmp`)
- [ ] `apt update`, `git --version`, `python3 --version` jalan
- [ ] `/sdcard/molina-studio` bisa dibaca & ditulis dari Debian
- [ ] Extra keys: ESC, CTRL, ALT (tap & tahan = kunci), panah, HOME, CLEAR, COPY, PASTE, tab
- [ ] Pinch-zoom font 8-14sp, tersimpan setelah force-stop
- [ ] Keyboard aktif: nav bawah tersembunyi, extra key tetap di atas keyboard

## C. Multi-session & drawer
- [ ] NEW SESSION sampai 5 (tombol nonaktif di 5); ✕ minta konfirmasi lalu menutup tab
- [ ] Sesi terakhir keluar = aplikasi keluar
- [ ] Swipe kiri menutup drawer; tap/swipe ≡ membuka; teks & lebar tombol pas
- [ ] KEYBOARD berfungsi; gear membuka Settings > Terminal

## D. Service & Keluar
- [ ] Notifikasi persisten (Buka/Keluar); sesi tetap hidup 5+ menit di background
- [ ] Keluar: semua proses mati (`ps`), hilang dari recents

## E. Pengaturan (DataStore)
- [ ] Ubah semua toggle Settings > Terminal, force-stop, buka: nilai tersimpan
- [ ] Tema prompt default Classic; ganti Cinema Gold: prompt berikutnya berubah; kembali Classic

## F. Log, status bar, stabilitas
- [ ] Settings > Log: event muncul, filter & hapus jalan, baris identik digabung
- [ ] Bar status: net/CPU/RAM bergerak; berhenti saat app di background
- [ ] Rotasi layar & 10 menit pemakaian: tanpa crash/ANR (cek juga saat startup)
