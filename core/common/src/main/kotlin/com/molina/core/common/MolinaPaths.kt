package com.molina.core.common

import java.io.File

/** Subfolder data bersama di bawah [MolinaPaths.dataRoot]. */
enum class MolinaDir(val folderName: String) {
    Downloads("downloads"),
    Export("export"),
    Media("media"),
    Backup("backup"),
}

/**
 * Kontrak lokasi file. Kotlin murni (tanpa Context); implementasi Android ada di :core:storage.
 * Data bersama TIDAK ditaruh di [workspace] agar `git status` tetap bersih.
 */
interface MolinaPaths {
    /** Root working tree git (/sdcard/molina-studio). Bukan tempat data aplikasi. */
    val workspace: File

    /** Folder data tersembunyi di luar working tree (/sdcard/.molina-data). Tidak privat. */
    val dataRoot: File

    /** Penyimpanan privat aplikasi (filesDir). Satu-satunya tempat untuk rahasia. */
    val privateRoot: File

    fun dir(kind: MolinaDir): File = File(dataRoot, kind.folderName)

    companion object {
        const val DATA_FOLDER = ".molina-data"
    }
}
