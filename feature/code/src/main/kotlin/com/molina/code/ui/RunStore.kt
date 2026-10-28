package com.molina.code.ui

/** [CODE-PYRUN2] Penyimpanan kecil pilihan dialog Jalankan di Terminal. Dipasang :app (SharedPreferences). */
interface RunStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

internal object RunPrefs {
    /** "heredoc" atau "file": mode terakhir yang dipakai. */
    const val KEY_MODE = "run_mode"

    /** "0" = jangan tanya lagi (langsung pakai mode terakhir), selain itu dialog tampil. */
    const val KEY_ASK = "run_ask"
}
