package com.molina.studio

import android.app.Application
import com.molina.core.common.AppLog
import com.molina.studio.log.LogBus

/** Titik masuk proses: pasang sink log sedini mungkin agar log sebelum Activity tidak hilang. */
class MolinaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.install(LogBus)
    }
}
