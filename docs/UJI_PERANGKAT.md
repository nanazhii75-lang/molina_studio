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
- [ ] Notifikasi persisten mpv (prioritas rendah, tanpa bunyi) muncul saat video main di latar; Play/Pause (dan Prev/Next bila playlist) berfungsi dari notifikasi; badge LIVE ungu tampil di Cinema Home selama service aktif (catatan: `POST_NOTIFICATIONS` tidak berlaku di perangkat uji API 26; relevan hanya bila kelak diuji di Android 13+)
- [ ] Keluar aplikasi (AppExit) saat BackgroundPlaybackService aktif: proses mpv berhenti, notifikasi & badge LIVE hilang, tidak ada service nyangkut (`dumpsys activity services` bersih dari BackgroundPlaybackService)

## E. Pengaturan (DataStore)
- [ ] Ubah semua toggle Settings > Terminal, force-stop, buka: nilai tersimpan
- [ ] Tema prompt default Classic; ganti Cinema Gold: prompt berikutnya berubah; kembali Classic

## F. Log, status bar, stabilitas
- [ ] Settings > Log: event muncul, filter & hapus jalan, baris identik digabung
- [ ] Bar status: net/CPU/RAM bergerak; berhenti saat app di background
- [ ] Rotasi layar & 10 menit pemakaian: tanpa crash/ANR (cek juga saat startup)
- [ ] Konsistensi warna: badge LIVE di Cinema Home dan ikon sumber mpv di tab Log memakai ungu yang sama (MolinaTheme.Cinema)
## G. Cinema / mpv (Fase 5)
- [ ] Cinema Home: putar file lokal via File (SAF) dan via Tautan (http/https/rtsp/rtmp); URL YouTube ditolak (menunggu 5d)
- [ ] Gestur: geser horizontal = maju/mundur, geser vertikal kiri = kecerahan, kanan = volume, tap kiri/tengah/kanan sesuai setelan
- [ ] Setelan mpv (Settings > mpv): ubah toggle (gpu_next, hardware_decoding, background_play, dst.), force-stop, buka ulang Cinema: nilai tersimpan
- [ ] Reset (Cinema Home): mengembalikan setelan mpv ke bawaan
- [ ] Keluar dari pemutar (back/tombol): tanpa crash, tidak ada proses mpv nyangkut (`ps` tidak ada sisa)
- [ ] Keluar aplikasi (AppExit) saat video sedang main: hook mpv menutup dalam ~1,5 dtk, tidak ANR
- [ ] Tab Log, filter Cinema: baris sumber mpv tampil dengan ikon putar ungu; Error merah, Warn kuning
- [ ] Log Permintaan/Mulai/Dimuat/Selesai (PlaybackLog) muncul saat membuka & menutup video
- [ ] Putar file rusak / URL salah: tercatat sebagai Error di Log dengan penyebab (galat terakhir mpv)
- [ ] Pemutaran latar: kirim app ke background saat main; notifikasi persisten muncul; log mulai/berhenti di tab Log; mpv berhenti sendiri saat video selesai
- [ ] Audio focus: terima panggilan telepon / putar audio app lain saat video main; log GAIN/LOSS muncul; uji toggle "Ignore Audio Focus" aktif vs nonaktif
- [ ] Simpan posisi (bila diaktifkan): tutup & buka ulang video yang sama, posisi lanjut
- [ ] Ingat kecerahan (bila diaktifkan): kecerahan tersimpan antar sesi pemutaran
- [ ] Peringatan keluar daftar putar aktif: coba keluar dari playlist, muncul dialog konfirmasi
- [ ] Header stream (Fase 5c-hdr): putar URL dari server uji yang memantulkan header permintaan dengan intent ACTION_VIEW + extra `headers` (String[] [nama, nilai, ...]); header terkirim ke server, log hanya menampilkan jumlah 'dipakai/ditolak' (bukan isinya); header tak valid (spasi/CRLF/Host) ditolak; tanpa extra, perilaku sama seperti sebelumnya. Syarat: mpv mendukung `loadfile <url> <flag> -1 <opsi>` (mpv >= 0.38)
- [ ] [G-ui-pill] Kontrol player: grup ikon Lock/PiP/cari/Settings berlatar pill gelap tepat di atas teks durasi akhir; tombol prev/next tidak menempel tepi layar; show/hide/fade ikut kontrol; ikon PiP tampak jelas (tint belum diverifikasi)
- [ ] [G-ui-dialog] Dialog Settings dan Playlist bertema gelap (#0F0F0F, teks putih, sudut 28dp); item yang sedang diputar berwarna biru dan tebal; shuffle/repeat aktif biru; dialog track/speed/subtitle MASIH terang (utang, bukan bug baru)
- [ ] [G-audio-only] Audio only (menu Settings): video dimatikan dan UI audio tampil; mati lagi = video kembali; tombol tersembunyi bila media tak punya video+audio; berdampingan dengan 'Play in background' tanpa dialog jadi scroll
- [ ] [G-search-url] Ikon cari/URL di player: dialog gelap; Play mengganti dan lanjut main (walau sebelumnya jeda), Add to queue menambah ke playlist; URL tak valid ditolak; teks judul biasa belum didukung (menunggu 5d)
- [ ] [G-folder] Folder jadi playlist: buka satu berkas lokal, seluruh media satu folder masuk playlist urut alami mulai dari berkas terpilih; berkas tersembunyi dan cue/m3u tidak ikut; berlaku untuk pemilih berkas lama dan File (SAF) di Cinema Home; Settings > UI > 'Play entire folder' dimatikan = putar tunggal; SAF content:// tanpa path nyata tercatat di Log 'Playlist folder tidak dipakai'
- [ ] [G-notif-nav] Notifikasi latar dan PiP: tombol Prev/Next muncul hanya bila playlist > 1 dan ikut diperbarui saat jumlah playlist berubah
- [ ] [G-memory] Multi-pilih berkas (pemilih lama) lalu putar: playlist memory:// terbuka tanpa 'No file given' (perbaikan 862e434); intent dari aplikasi lain dengan filepath memory:// berisi skema/path terlarang atau lebih dari 1000 entri ditolak
