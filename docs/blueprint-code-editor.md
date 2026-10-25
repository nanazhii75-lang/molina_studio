# Blueprint: Molina Code Editor (Tema "Molina Cinema")

- Status: DRAFT v3 (belum ada implementasi kode; lihat Addendum v2 bagian 12-18 dan Addendum v3 bagian 19-24)
- Tanggal: 2026-10-25
- Lingkup: tab **Code** (editor, panel direktori, toolbar). Tab mpv, Terminal, Settings tidak diubah.
- Acuan: 3 screenshot desain lama (panel terisi, panel kosong, editor kosong).

## 1. Prinsip Desain
1. Gelap dan hangat: hitam pekat + aksen emas/cokelat, kesan home theater modern.
2. Aksen emas hemat: hanya tab aktif, kapsul nav aktif, baris aktif, seleksi/fokus.
3. Kompak: panel direktori dan tombol lebih kecil dari desain lama.
4. Satu tangan: toolbar dan navigasi di bawah, panel dibuka dengan geser dari kiri.

## 2. Design Tokens
Nilai diambil dari screenshot lama. Verifikasi saat membuat colors.xml, jangan hardcode di layout.

| Token | Hex | Pemakaian |
|---|---|---|
| molina_editor_bg | #000000 | Latar area editor (alt #121212 untuk gutter) |
| molina_surface_header | #323336 | Header path, tab bar, toolbar |
| molina_surface_drawer | #2B2B2B | Panel direktori |
| molina_surface_nav | #2A2218 | Bottom navigation |
| molina_gold | #FFB300 | Aksen aktif (garis tab, label Code) |
| molina_capsule | #4E3B14 | Kapsul menu aktif |
| molina_active_line | #38280F | Highlight baris aktif |
| molina_line_number | #6E6E6E | Nomor baris (redup) |
| molina_text_primary | #E6E6E6 | Teks utama |
| molina_text_secondary | #9AA0A6 | Path, teks sekunder |
| molina_button_brown | #8F5A2C | Tombol aksi (diperkecil) |
| molina_danger | #E5484D | Tutup/close |
| molina_folder_yellow | #E9B949 | Ikon folder gaya Win7 |
| molina_select_fill | #FFB300 alpha 18% | Isi seleksi baris tree |
| molina_select_border | #FFB300 alpha 45% | Border seleksi baris tree |

## 3. Anatomi Layout (atas ke bawah)
+--------------------------------------+
| /storage/emulated/0/Molina         : |  Header path + menu
+--------------------------------------+
| ===== (garis emas di ATAS tab aktif) |
| Tidak berjudul  X |                  |  Tab bar
+--------------------------------------+
|  1 |                                 |  Gutter + area editor
|    |                                 |
+--------------------------------------+
| Save Paste Undo Redo Find < > Tabs Run X |  Toolbar
+--------------------------------------+
| mpv   Terminal   [Code]   Settings   |  Bottom nav
+--------------------------------------+
- Header: abu gelap, path folder kerja (default /storage/emulated/0/Molina), menu titik tiga di kanan.
- Tab aktif: garis aksen emas di bagian ATAS tab, latar lebih gelap, tombol X jelas. Tab tidak aktif tanpa garis.
- Gutter: nomor baris abu redup, lebar menyesuaikan jumlah digit.
- Area editor: hitam pekat, font monospace 13sp, baris aktif memakai molina_active_line.
- Toolbar: tepat di atas bottom nav.

## 4. Editor Toolbar (urutan kiri ke kanan)

| No | Aksi | Ikon | Warna ikon (acuan lama) |
|---|---|---|---|
| 1 | Simpan | Disket | Abu (aktif saat ada perubahan) |
| 2 | Clipboard / Tempel | Clipboard | Cyan #26C6DA |
| 3 | Undo | Panah melingkar kiri | Oranye #FFA726 |
| 4 | Redo | Panah melingkar kanan | Salmon #FF8A65 |
| 5 | Cari | Kaca pembesar | Abu |
| 6 | Kursor kiri | Panah kiri | Abu |
| 7 | Kursor kanan | Panah kanan | Abu |
| 8 | Manajemen tab | Tumpukan jendela | Abu |
| 9 | Eksekusi cepat | Segitiga play | Abu (nonaktif jika file tak bisa dijalankan) |
| 10 | Tutup | Silang | Merah molina_danger |

- State nonaktif: alpha 38%.
- Tutup pada file yang belum disimpan: tampilkan konfirmasi.
- Catatan: 10 ikon pada layar 360dp = 36dp per ikon (di bawah 48dp). Lihat Pertanyaan Terbuka no. 4.

## 5. Panel Direktori (gaya Windows 7 Explorer, versi gelap)

### 5.1 Perilaku
- Dibuka dengan geser dari tepi kiri area editor. Lebar +/-72% layar (maks 320dp), ada scrim.
- Ditutup dengan geser ke kiri, tap scrim, atau tombol X.

### 5.2 Header panel
- Isi: tombol naik satu level, nama folder + path ringkas, tombol File baru, Folder baru, Tutup.
- Tinggi 48dp (desain lama +/-64dp), ikon 20dp.

### 5.3 Tree (menggantikan gaya Android Studio lama)
- Tinggi baris 32dp (lama +/-54dp), font 13-14sp, indent 14dp per level.
- Expander: segitiga kecil gaya Win7 (outline abu saat tertutup, terisi saat terbuka), 10dp. Bukan chevron.
- Ikon folder kuning khas Win7 (16-18dp), versi terbuka untuk folder yang di-expand. File memakai ikon per ekstensi (.kt, .py, .md, .json, .xml, .sh, default).
- Seleksi: kotak sudut 3dp, isi molina_select_fill + border 1dp molina_select_border (padanan gelap dari highlight biru Win7). State tekan: alpha isi 10%.
- Urutan: folder dulu, lalu file, A-Z tanpa peka huruf besar/kecil. Berkas tersembunyi (.git, .github) tampil redup 60%.
- Tekan lama: menu konteks (Buka, Ganti nama, Hapus, Salin path, File baru, Folder baru).
- Usulan fase lanjut: breadcrumb ala address bar Explorer di bawah header.

### 5.4 Empty state ("Belum ada folder")
- Teks penjelasan 13sp, rata tengah.
- Tombol BUKA FOLDER dan BUKA BERKAS: tinggi 36dp (lama +/-54dp), lebar wrap_content (min 128dp), teks 12sp, sudut 4dp, warna molina_button_brown, jarak antartombol 8dp, ikon kecil di kiri teks.
- Tata letak: berdampingan jika muat, jika tidak ditumpuk.

## 6. Bottom Navigation
- 4 menu: mpv, Terminal, Code, Settings.
- Code aktif: kapsul molina_capsule, ikon dan label molina_gold.
- Menu lain mempertahankan warna ikon lama (mpv merah muda, Terminal teal, Settings emas).

## 7. Perilaku Editor
- Berkas baru bernama "Tidak berjudul". Tab bar bisa di-scroll horizontal.
- Penanda belum disimpan (dirty) pada judul tab.
- Undo/redo per tab. Word wrap default mati.

## 8. Rencana Teknis (usulan, verifikasi dengan codebase)
- Struktur: ui/code/ (CodeScreen, EditorView, EditorTabBar, EditorToolbar, FileExplorerDrawer, FileTreeAdapter), data/code/ (FileRepository, EditorSession), tema Molina Cinema di resource theme.
- Warna dan ukuran lewat token (colors.xml, dimens.xml). Tidak ada hardcode di layout.
- I/O file di Dispatchers.IO. Tree lazy-load per folder. List memakai DiffUtil/key stabil.
- Akses file: SAF (ACTION_OPEN_DOCUMENT_TREE) atau path langsung /storage/emulated/0. Lihat Pertanyaan Terbuka no. 3.

## 9. Pemeriksaan Dampak
- AndroidManifest.xml: permission storage jika memakai path langsung (API 26 = runtime permission).
- Gradle: hindari dependency baru. Jika memakai library editor teks, cek kompatibilitas minSdk 26 dan arm64.
- Resource: colors, dimens, drawable vector baru (ikon Win7, ikon ekstensi).
- JNI/mpv: tidak disentuh. Pastikan pindah tab tidak melepas surface mpv.
- Proguard/R8: cek aturan jika ada refleksi pada library baru.

## 10. Checklist Fase
- [x] F0 Blueprint (dokumen ini)
- [ ] F1 Tokens dan tema (colors.xml, dimens.xml, style)
- [ ] F2 Layout editor (header, tab bar, gutter, area)
- [ ] F3 Toolbar editor
- [ ] F4 Panel direktori Win7 + empty state
- [ ] F5 Integrasi bottom nav dan state
- [ ] F6 Uji di perangkat API 26 arm64 via GitHub Actions CI

## 11. Pertanyaan Terbuka
1. UI saat ini berbasis View/XML atau Jetpack Compose?
2. Komponen editor teks: custom EditText atau library (mis. sora-editor)?
3. Akses file: SAF atau path langsung?
4. Toolbar 10 ikon: muat sama rata (36dp) atau scroll horizontal?
5. Tombol Play menjalankan apa (kirim ke Terminal Molina, atau berdasarkan ekstensi .py/.sh)?
6. Konfirmasi nilai hex pada bagian 2.


---

## 12. Addendum v2 [BLUEPRINT-CODE-EDITOR-V2]

Tanggal: 2026-10-25. Sumber: 3 screenshot tambahan desain lama (menu titik tiga, lanjutan menu, dialog Opsi pencarian).

Penggantian terhadap bagian sebelumnya:
- Kolom "Warna ikon" di bagian 4 digantikan oleh bagian 13.3.
- Token permukaan di bagian 2 (header, toolbar, drawer) digantikan oleh bagian 13.2.
- Bagian 7: word wrap default AKTIF (menu lama menampilkan "Pembungkus kata" tercentang). Butir "word wrap default mati" dibatalkan.

## 13. Toolbar: Palet Khas Molina Cinema

### 13.1 Temuan desain lama
- Ikon toolbar berwarna-warni (abu, cyan, oranye, salmon, merah, biru saat dirty). Tidak ada identitas, kesannya acak.
- Header/toolbar abu netral (#323336) sedangkan bottom nav cokelat hangat (#2A2218). Dua keluarga warna bentrok.
- Warna di luar palet Molina: kursor biru-ungu, checkbox dialog lavender, ikon Simpan biru saat dirty, titik dirty oranye.

### 13.2 Token v2 (usulan; keluarga "espresso hangat")

| Token | Hex | Pemakaian |
|---|---|---|
| molina_surface_1 | #1C1814 | Header path, tab bar |
| molina_surface_2 | #231D17 | Toolbar, find bar |
| molina_surface_drawer | #1F1B17 | Panel direktori |
| molina_surface_menu | #1A1511 | Popup menu titik tiga, dialog (#1F1A15) |
| molina_surface_nav | #2A2218 | Bottom nav (tetap) |
| molina_hairline | #3A2F22 | Pembatas 1dp, border popup/dialog |
| molina_icon_default | #CDBFA3 | Ikon toolbar normal (champagne) |
| molina_icon_dim | #7A6C55 | Ikon "bersih/tidak ada aksi" (mis. Simpan saat tidak ada perubahan) |
| molina_icon_disabled | #5A4E3C | Ikon nonaktif |
| molina_gold | #FFB300 | Aksen kuat: Play, Simpan saat dirty, state aktif |
| molina_gold_soft | #E9B949 | Aksen lembut (ikon folder, badge) |
| molina_ripple | #FFB300 alpha 16% | Efek tekan |
| molina_danger | #E5484D | Tutup |

Perkiraan kontras di atas molina_surface_2: champagne sekitar 9:1, gold sekitar 9:1, icon_dim sekitar 3:1. Verifikasi di perangkat.

### 13.3 Aturan warna ikon toolbar

| No | Aksi | Normal | State khusus |
|---|---|---|---|
| 1 | Simpan | icon_dim | Dirty: gold + titik kecil gold di pojok kanan atas ikon |
| 2 | Clipboard / Tempel | icon_default | Nonaktif jika clipboard kosong |
| 3 | Undo | icon_default | icon_disabled jika stack kosong |
| 4 | Redo | icon_default | icon_disabled jika stack kosong |
| 5 | Cari | icon_default | Find bar terbuka: gold + kapsul #4E3B14 (sama dengan kapsul bottom nav) |
| 6 | Kursor kiri | icon_default | - |
| 7 | Kursor kanan | icon_default | - |
| 8 | Manajemen tab | icon_default | Badge jumlah tab (gold_soft, 10sp) |
| 9 | Eksekusi cepat | gold | icon_disabled jika file tidak bisa dijalankan |
| 10 | Tutup | danger | Konfirmasi jika dirty |

Prinsip: maksimal dua warna aksen (gold dan danger) di toolbar. Sisanya champagne tunggal.

### 13.4 Elemen khas "Cinema"
1. Rail emas: garis 1dp di tepi atas toolbar, gradient transparan -> gold alpha 40% -> transparan (seperti rel lampu sorot/tirai).
2. Kapsul aktif konsisten: warna kapsul yang sama dengan bottom nav dipakai untuk state aktif di toolbar.
3. Badge jumlah tab pada ikon manajemen tab.
4. Grup ikon dengan pembatas hairline 1dp setinggi 16dp: [Simpan] | [Tempel Undo Redo] | [Cari Kiri Kanan] | [Tab Play] | [Tutup].
5. Ripple emas 16% menggantikan ripple abu bawaan.

Opsi tata letak toolbar:
- Opsi A (rekomendasi, dikerjakan di F3): toolbar flat penuh lebar di molina_surface_2 + rail emas.
- Opsi B (dievaluasi setelah F5): "dock" melayang, margin 8dp, sudut 16dp, border molina_hairline. Kesan kontrol home theater, tetapi memakan tinggi layar.

## 14. Menu Titik Tiga (Overflow)

### 14.1 Inventaris desain lama (urutan asli)

| No | Item | Catatan di screenshot |
|---|---|---|
| 1 | Baru | aktif |
| 2 | Buka | aktif |
| 3 | Muat ulang | nonaktif (belum ada file) |
| 4 | Aksi file | nonaktif |
| 5 | Cari | aktif |
| 6 | Bagikan | aktif |
| 7 | Kirim ke Library | nonaktif |
| 8 | Ekspor | nonaktif |
| 9 | Konversi berkas... | aktif |
| 10 | Pergi ke baris | aktif |
| 11 | Syntax | aktif |
| 12 | Enkode | aktif |
| 13 | Gaya Visual | aktif |
| 14 | Statistik | aktif |
| 15 | Lakukan | aktif |
| 16 | Pembungkus kata | checkbox, ON (centang emas) |
| 17 | Hanya baca | checkbox, OFF |

### 14.2 Masalah desain lama
- Baris sekitar 52dp dan 17 item, sehingga harus scroll panjang.
- Item nonaktif nyaris tak terbaca (abu gelap di latar gelap).
- Tidak ada pengelompokan.

### 14.3 Usulan
- Popup: molina_surface_menu, border molina_hairline, sudut 12dp, elevasi 8dp. Lebar 208-248dp, tinggi maks 72% layar, di-anchor kanan atas.
- Baris 44dp, teks 15sp champagne. Nonaktif memakai alpha 38%.
- Checkbox: terisi gold dengan centang hitam. Tidak tercentang: border champagne alpha 50%. (Desain lama sudah emas, pertahankan.)
- Pengelompokan dengan hairline antar grup:

| Grup | Isi |
|---|---|
| File | Baru, Buka, Muat ulang, Aksi file |
| Cari dan Navigasi | Cari, Pergi ke baris |
| Bagikan dan Ekspor | Bagikan, Kirim ke Library, Ekspor, Konversi berkas... |
| Tampilan dan Teks | Syntax, Enkode, Gaya Visual, Pembungkus kata, Hanya baca |
| Info dan Aksi | Statistik, Lakukan |

- Opsional: ikon 18dp di kiri tiap item (champagne), diputuskan saat F3b.

## 15. Pencarian (Find Bar dan Dialog Opsi)

### 15.1 Alur (dari screenshot)
Cari (toolbar atau menu) -> find bar muncul tepat di atas toolbar (field "Cari" + tombol X tutup) -> dialog "Opsi pencarian".

### 15.2 Opsi dan default desain lama

| Opsi | Default |
|---|---|
| Huruf besar/kecil | ON |
| Blok | OFF |
| Persamaan reguler | OFF |
| Membungkus di sekitar | ON |
| Ganti teks | OFF (jika ON, tampilkan field pengganti) |

Tombol dialog: BATAL dan TERAPKAN.

### 15.3 Usulan styling
- Dialog: latar #1F1A15, sudut 12dp, border molina_hairline, judul 18sp champagne.
- Checkbox: gold (menggantikan lavender bawaan Material).
- BATAL: champagne alpha 70%. TERAPKAN: gold.
- Find bar: latar molina_surface_2, garis bawah gold saat fokus.
- Hasil cocok: highlight gold alpha 35%. Cocok aktif: border gold 1.5dp.
- Penghitung hasil (mis. 3/12) di kanan field. (Usulan baru, belum ada di desain lama.)

## 16. Detail Editor Lain
- Kursor (caret): gold, lebar 2dp (lama biru-ungu).
- Highlight seleksi: gold alpha 30%. Handle seleksi: gold.
- Penanda belum disimpan pada tab: titik gold (lama oranye).
- Saat keyboard terbuka: bottom nav disembunyikan, find bar dan toolbar menempel di atas keyboard (perilaku desain lama, dipertahankan).

## 17. Checklist Tambahan
- [ ] F1b Token v2 (bagian 13.2) masuk colors.xml
- [ ] F3 Toolbar: palet, rail emas, grup, badge tab (Opsi A)
- [ ] F3b Menu titik tiga: grup, ukuran, warna
- [ ] F3c Find bar dan dialog Opsi pencarian
- [ ] F3d Kursor, seleksi, titik dirty berwarna gold

## 18. Pertanyaan Terbuka Tambahan
7. "Library" pada "Kirim ke Library" itu apa (library media mpv atau library snippet)?
8. "Lakukan" melakukan apa, dan apa bedanya dengan tombol Play di toolbar?
9. "Blok" pada opsi pencarian: kata utuh, atau pencarian di dalam blok seleksi?
10. Toolbar: Opsi A (flat + rail) atau Opsi B (dock melayang)? Default sementara: A.
11. Isi submenu "Aksi file" (rename, hapus, salin path, lainnya)?


---

## 19. Addendum v3 [BLUEPRINT-CODE-EDITOR-V3]

Tanggal: 2026-10-25. Sumber: 2 screenshot desain lama (dialog "Simpan sebagai" + pemilih berkas, layar "Tinjauan").

Keputusan:
- Enkode dikunci UTF-8.
- Layar Tinjauan (preview) mengikuti tema Molina hitam pekat. Desain lama masih putih.

## 20. Dialog "Simpan sebagai" dan Enkode Terkunci UTF-8

### 20.1 Inventaris desain lama
- Pemilih berkas (layar di belakang dialog): appbar hitam dengan tombol kembali, judul "Simpan sebagai", ikon urut, bintang (favorit), filter. Daftar folder berikon biru-abu dengan tanggal. FAB "+" cokelat-oranye di kanan bawah.
- Dialog: latar abu #424242, judul oranye, label "Nama Berkas:", field nama + titik + field ekstensi dengan saran otomatis (contoh: mm, mma, mml), label "Enkode:" dengan dropdown berisi UTF-8, tombol aksi di bagian bawah (label tertutup keyboard di screenshot).
- Keyboard menampilkan tombol aksi "Selesai".

### 20.2 Keputusan: Enkode terkunci UTF-8
- Field Enkode menjadi teks read-only "UTF-8" dengan ikon gembok kecil (16dp, molina_icon_dim). Tidak ada dropdown dan tidak ada panah.
- Ketuk field: tampilkan snackbar/tooltip singkat "Molina selalu menyimpan dalam UTF-8".
- Penyimpanan selalu UTF-8 (BOM dan line ending: lihat pertanyaan terbuka 13).
- Menu titik tiga (bagian 14): item "Enkode" diganti item read-only "Enkode: UTF-8 (terkunci)" di grup Tampilan dan Teks.
- Perilaku saat membuka file non-UTF-8: menunggu konfirmasi (pertanyaan terbuka 12). Usulan sementara: decode UTF-8 ketat, jika gagal buka sebagai read-only dengan banner peringatan.

### 20.3 Styling dialog (selaras bagian 15.3)

| Elemen | Lama | Molina |
|---|---|---|
| Latar dialog | #424242 | #1F1A15, sudut 12dp, border molina_hairline |
| Judul | Oranye | molina_gold |
| Label (Nama Berkas, Enkode) | Putih | champagne alpha 70% |
| Field | Garis bawah putih, fokus oranye | Garis bawah molina_hairline, fokus molina_gold, kursor gold |
| Dropdown saran ekstensi | Abu #424242 | molina_surface_menu, item 40dp, teks champagne, bagian cocok gold |
| Tombol | Teks oranye | BATAL champagne alpha 70%, SIMPAN gold (label usulan) |

### 20.4 Pemilih berkas
- Latar daftar dan appbar: #000000. Ikon appbar (urut, favorit, filter): champagne.
- Ikon folder: kuning molina_gold_soft gaya Win7 (selaras bagian 5.3), menggantikan biru-abu.
- Tanggal dan teks sekunder: champagne alpha 60%.
- FAB "+": latar molina_capsule (#4E3B14), ikon plus gold, border gold alpha 45%.
- Saran ekstensi: maks 5 item, urut berdasarkan kecocokan awalan. Usulan: ekstensi default mengikuti Syntax aktif.
- Validasi: nama tidak boleh kosong atau mengandung "/"; jika file sudah ada, tampilkan konfirmasi timpa.

### 20.5 Folder simpan default
Screenshot Tinjauan menunjukkan file tersimpan di /storage/emulated/0/ (root), padahal path kerja editor adalah /storage/emulated/0/Molina. Usulan: folder awal dialog Simpan sebagai = folder kerja yang sedang terbuka, fallback /storage/emulated/0/Molina.

## 21. Layar Tinjauan (Preview): Molina Hitam Pekat

### 21.1 Inventaris desain lama
- Appbar hitam: tombol kembali, judul "Tinjauan", subjudul path file, ikon toggle mode gelap/terang di kanan.
- Body putih dengan teks hitam (hasil render Markdown).

### 21.2 Spesifikasi baru
- Default: GELAP. Body #000000, teks #E6E6E6 (bukan putih murni, mengurangi silau di OLED).
- Appbar: #000000, hairline 1dp molina_hairline di bawahnya. Judul champagne 18sp. Subjudul path champagne alpha 60%, 12sp, ellipsize di tengah. Ikon kembali dan toggle: champagne.
- Status bar dan navigation bar hitam, ikon status terang.
- Cegah kilatan putih saat membuka: windowBackground tema layar ini hitam, root layout hitam, dan jika memakai WebView panggil setBackgroundColor(Color.BLACK) sebelum loadData.
- Toggle mode: dipertahankan. Mode terang memakai varian krem hangat (#F5EFE3 dengan teks #1C1814), bukan putih murni. Pilihan terakhir diingat di preferensi (lihat pertanyaan terbuka 14).

### 21.3 CSS Markdown (acuan jika render lewat HTML/WebView)

```css
:root { color-scheme: dark; }
body { background:#000000; color:#E6E6E6; font:16px/1.6 sans-serif; padding:16px; margin:0; }
h1,h2 { color:#E9B949; border-bottom:1px solid #3A2F22; padding-bottom:4px; }
a { color:#FFB300; }
code { background:#1C1814; color:#E9B949; padding:1px 4px; border-radius:4px; }
pre { background:#121212; border:1px solid #3A2F22; border-radius:8px; padding:12px; overflow-x:auto; }
pre code { background:transparent; color:#E6E6E6; padding:0; }
blockquote { border-left:3px solid rgba(255,179,0,0.6); background:#0D0B09; margin:0; padding:8px 12px; color:#CDBFA3; }
table { border-collapse:collapse; }
th,td { border:1px solid #3A2F22; padding:6px 10px; }
th { background:#1C1814; }
hr { border:0; border-top:1px solid #3A2F22; }
::selection { background:rgba(255,179,0,0.3); }
```

## 22. Token Tambahan v3

| Token | Hex | Pemakaian |
|---|---|---|
| molina_preview_bg | #000000 | Latar Tinjauan |
| molina_preview_text | #E6E6E6 | Teks Tinjauan |
| molina_preview_link | #FFB300 | Tautan |
| molina_preview_code_bg | #121212 | Blok kode |
| molina_preview_quote_bg | #0D0B09 | Kutipan |
| molina_preview_light_bg | #F5EFE3 | Mode terang (opsional) |
| molina_preview_light_text | #1C1814 | Teks mode terang (opsional) |

## 23. Checklist Tambahan
- [ ] F3e Dialog Simpan sebagai: styling, Enkode terkunci UTF-8, saran ekstensi
- [ ] F3f Pemilih berkas: appbar, ikon folder gold_soft, FAB
- [ ] F3g Layar Tinjauan: tema hitam pekat, CSS Markdown, cegah kilatan putih, toggle
- [ ] F3h Menu: item Enkode menjadi indikator read-only UTF-8
- [ ] F3i Folder simpan default mengikuti folder kerja

## 24. Pertanyaan Terbuka Tambahan
12. Cakupan kunci UTF-8: hanya saat menyimpan, atau juga saat membuka? Untuk file non-UTF-8: tolak, buka read-only dengan peringatan, atau decode fallback?
13. Simpan UTF-8 tanpa BOM dengan line ending LF: setuju?
14. Toggle terang di Tinjauan: dipertahankan (varian krem hangat) atau dihapus (hitam saja)?
15. Tinjauan memakai WebView atau komponen render Markdown native?
16. Folder simpan default: ikut folder kerja terbuka, atau tetap root /storage/emulated/0/?
