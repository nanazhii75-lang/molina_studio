# Kunci tanda tangan tetap (Molina Studio)

Tanpa kunci tetap, tiap build CI memakai keystore debug acak sehingga APK baru
ditolak Android sebagai update (harus uninstall; rootfs Debian ikut hilang).

## Buat kunci (sekali, di terminal Molina)
    apt install -y openssl
    mkdir -p /sdcard/molina-keys && cd /sdcard/molina-keys
    PASS=$(openssl rand -base64 18 | tr -d '/+=')
    echo "PASSWORD: $PASS"
    openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 10000 -nodes -subj "/CN=Molina Studio/O=molina-studio"
    openssl pkcs12 -export -inkey key.pem -in cert.pem -out molina.p12 -name molina -passout "pass:$PASS"
    base64 -w0 molina.p12 > molina.p12.b64
    rm key.pem cert.pem

## GitHub Secrets (Settings > Secrets and variables > Actions)
- MOLINA_KEYSTORE_B64      = isi file molina.p12.b64
- MOLINA_KEYSTORE_PASSWORD = nilai PASSWORD di atas

## Wajib
- Cadangkan molina.p12 + password di luar repo dan luar ponsel. Hilang = uninstall lagi.
- Jangan commit atau kirim molina.p12, .b64, atau password ke mana pun.
- APK pertama dengan kunci tetap = uninstall sekali terakhir; setelahnya update langsung.
- Log CI "Cetak sidik jari tanda tangan APK": SHA-256 harus sama di setiap build.
