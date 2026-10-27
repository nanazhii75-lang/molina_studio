package com.molina.studio.download

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Jembatan ke tab Unduhan dari layar lain (mis. kartu hasil pencarian Cinema). [request] menaruh URL di
 * [pendingUrls]; MainActivity pindah ke tab Unduhan dan UnduhanScreen memindahkannya ke kolom URL lalu
 * mengosongkannya. Tidak memulai unduhan: pengguna masih memilih mode/kualitas lalu menekan Mulai.
 */
internal object DownloadNav {
    var pendingUrls by mutableStateOf<List<String>>(emptyList())

    fun request(urls: List<String>) {
        pendingUrls = (pendingUrls + urls).distinct()
    }
}

/**
 * Isi formulir Unduhan. Disimpan di sini (bukan rememberSaveable) karena tab hanya dikomposisi saat dipilih:
 * pindah ke tab lain tidak boleh menghapus URL dan pilihan yang sudah diisi.
 */
internal object DownloadForm {
    val urls = mutableStateOf("")
    val video = mutableStateOf(true)
    val height = mutableStateOf(0)
    val videoExt = mutableStateOf("mp4")
    val audioFmt = mutableStateOf("mp3")
    val bitrate = mutableStateOf("192k")
}
