package com.molina.core.storage

import android.content.Context
import android.os.Environment
import com.molina.core.common.MolinaDir
import com.molina.core.common.MolinaPaths
import java.io.File

/** Implementasi Android untuk [MolinaPaths]. Butuh izin storage agar [ensureDirs] berhasil. */
class AndroidMolinaPaths(context: Context) : MolinaPaths {
    private val external: File = Environment.getExternalStorageDirectory()

    override val workspace: File = File(external, "molina-studio")
    override val dataRoot: File = File(external, MolinaPaths.DATA_FOLDER)
    override val privateRoot: File = context.applicationContext.filesDir

    /** Membuat dataRoot + semua subfolder. Operasi disk: panggil di luar main thread. */
    fun ensureDirs(): Boolean {
        var ok = true
        for (kind in MolinaDir.entries) {
            val d = dir(kind)
            ok = (d.isDirectory || d.mkdirs()) && ok
        }
        return ok
    }
}
