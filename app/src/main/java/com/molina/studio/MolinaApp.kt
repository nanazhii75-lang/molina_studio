package com.molina.studio

import android.app.Application
import com.molina.cinema.MpvLifecycle
import com.molina.core.common.AppLog
import com.molina.core.common.CompositeStreamResolver
import com.molina.core.common.StreamResolvers
import com.molina.engine.newpipe.NewPipeStreamResolver
import com.molina.studio.log.LogBus
import com.molina.studio.stream.StreamEnginePrefs
import com.molina.studio.stream.YtdlpStreamResolver
import com.molina.studio.system.MpvControl
import com.molina.studio.system.SystemRefresh

/** Titik masuk proses: pasang sink log sedini mungkin agar log sebelum Activity tidak hilang. */
class MolinaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.install(LogBus)
        MpvLifecycle.installExitHook()
        StreamEnginePrefs.init(this)
        MpvControl.install(this)
        SystemRefresh.install(this)
        // NewPipe lebih dulu; yt-dlp (default mati, lihat Pengaturan) hanya untuk URL yang tak dikenalinya.
        StreamResolvers.install {
            CompositeStreamResolver(
                primary = NewPipeStreamResolver(),
                fallback = YtdlpStreamResolver(this, StreamEnginePrefs::isYtdlpEnabled),
            )
        }
    }
}
