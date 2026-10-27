# Halaman Unduhan native (rancangan)

Status: **inti logika selesai dan teruji** (`:core:common`); UI Compose dan service belum disambung.

## Kontrak
`molina_dl.py --json URL...` mencetak satu objek JSON per baris stdout (protokol v1, lihat docstring skrip).
`DownloadEventParser.parse(baris)` -> `DownloadEvent`; `DownloadReducer.reduce(state, event)` -> `DownloadSessionState`.
`--info --json URL` hanya mengirim event `info` (judul, durasi, daftar resolusi) untuk dialog "Tambah unduhan".
Kode keluar: 0 sukses, 1 ada gagal, 2 galat penggunaan/lingkungan, 130 dibatalkan (SIGTERM/SIGINT).

## Yang masih perlu disambung (urut)
1. [SELESAI 2026-10-27: args/env di RunRequest, ScriptCommand.kt, validasi, uji JVM] `RunRequest`/`ScriptCommand.prootArgs` belum menerima argumen skrip: tambah `args: List<String>` (di :feature:code dan
   `ProotScriptExecutor`), lalu jalankan `/sdcard/Molina/Scripts/molina_dl.py --json ...` dengan `MOLINA_ROOT` bila folder terlihat.
   Risiko sedang: menyentuh jalur exec PRoot yang juga dipakai editor Code.
2. [SELESAI 2026-10-27: DownloadRunner + DownloadFeed + jalur DOWNLOAD + batas keluaran per permintaan] `DownloadRunner` (di :app): `RunListener.onOutput` per baris -> `DownloadEventParser` -> `MutableStateFlow<DownloadSessionState>`.
   Batas keluaran executor saat ini harus dinaikkan atau dikecualikan (progres menghasilkan banyak baris).
3. Foreground service (tipe dataSync) agar unduhan lanjut saat layar mati; notifikasi memakai `overallPercent`.
   Manifest berubah: izin FOREGROUND_SERVICE + FOREGROUND_SERVICE_DATA_SYNC (API 34+) dan deklarasi service.
4. Layar Compose `UnduhanScreen` yang hanya mengamati state flow.

## Rancangan tampilan (Molina Cinema)
- Latar gelap, aksen emas Molina (`#D7AF00`, setara warna 178 di terminal) untuk bar dan persen; trek abu `#585858`.
- Satu kartu per URL: gambar mini, judul, bar gabungan (`overallPercent`), kecepatan dan ETA dari aliran aktif,
  chip tahap ("Menggabungkan", "Mengonversi audio"), ikon status (selesai hijau, gagal merah dengan pesan galat).
- Aksi: Jeda = batalkan (SIGTERM, `.part` tetap), Lanjutkan = jalankan ulang URL yang sama, Buka = putar di Molina Player.
- Dialog "Tambah": tempel banyak URL, pilih Video/Audio, resolusi dari event `info`, format dan bitrate.
- URL pada log selalu lewat `LogRedact.target` (tanpa token/query).
