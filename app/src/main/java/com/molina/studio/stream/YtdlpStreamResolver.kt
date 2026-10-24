package com.molina.studio.stream

import android.content.Context
import com.molina.core.common.AppLog
import com.molina.core.common.LogLevel
import com.molina.core.common.LogRedact
import com.molina.core.common.ResolvedStream
import com.molina.core.common.StreamError
import com.molina.core.common.StreamOutcome
import com.molina.core.common.StreamResolver
import com.molina.core.common.StreamSearchItem
import com.molina.core.common.YtdlUrls
import com.molina.core.common.YtdlpCommand
import com.molina.core.common.YtdlpParse
import com.molina.core.common.YtdlpSearch

/**
 * Resolver cadangan: yt-dlp di Debian PRoot untuk situs yang tidak dikenali NewPipe (TikTok,
 * Facebook, dll.). [enabled] dibaca tiap kali sehingga sakelar di Pengaturan berlaku langsung
 * tanpa memasang ulang resolver. [canResolve] cepat dan tanpa jaringan/proses.
 */
internal class YtdlpStreamResolver(
    private val ctx: Context,
    private val enabled: () -> Boolean,
) : StreamResolver {

    override val id: String = "ytdlp"

    override fun canResolve(url: String): Boolean = enabled() && YtdlUrls.needsYtdl(url)

    override suspend fun resolve(pageUrl: String): StreamOutcome<ResolvedStream> {
        val bin = when (val st = YtdlpLocator.status(ctx)) {
            is YtdlpStatus.Ready -> st.guestBinary
            else -> return unsupported(st.hint())
        }
        val where = LogRedact.target(pageUrl)
        AppLog.log("stream", LogLevel.Info, TAG, "resolve $where")
        val o = YtdlpRunner.run(ctx, bin, YtdlpCommand.resolveArgs(pageUrl), RESOLVE_TIMEOUT_MS)
        if (o.timedOut) return fail(StreamError.NETWORK, "lewat waktu ${RESOLVE_TIMEOUT_MS / 1000}s ($where)")
        if (o.exit != 0) return failure(o.exit, o.stderr, where)
        val parsed = YtdlpParse.single(o.stdout)
            ?: return fail(StreamError.PARSE, "keluaran tidak terbaca ($where)")
        return StreamOutcome.Success(parsed)
    }

    override suspend fun search(query: String, limit: Int): StreamOutcome<List<StreamSearchItem>> {
        if (!enabled()) return StreamOutcome.Failure(StreamError.UNSUPPORTED, "yt-dlp dimatikan")
        val bin = when (val st = YtdlpLocator.status(ctx)) {
            is YtdlpStatus.Ready -> st.guestBinary
            else -> return unsupported(st.hint())
        }
        val o = YtdlpRunner.run(ctx, bin, YtdlpCommand.searchArgs(query, limit), SEARCH_TIMEOUT_MS)
        if (o.timedOut) return fail(StreamError.NETWORK, "pencarian lewat waktu")
        if (o.exit != 0) return failure(o.exit, o.stderr, "pencarian")
        return StreamOutcome.Success(YtdlpSearch.parse(o.stdout.lines(), limit))
    }

    private fun unsupported(hint: String?): StreamOutcome.Failure {
        AppLog.log("stream", LogLevel.Warn, TAG, hint ?: "yt-dlp tidak siap")
        return StreamOutcome.Failure(StreamError.UNSUPPORTED, hint)
    }

    private fun failure(exit: Int, stderr: String, where: String): StreamOutcome.Failure {
        // 126/127: biner/python tidak bisa dijalankan di guest (bukan galat situs).
        if (exit == 126 || exit == 127) {
            return unsupported("yt-dlp tidak bisa dijalankan (exit $exit). ${YtdlpCommand.HINT_INSTALL}")
        }
        val err = YtdlpCommand.classify(stderr)
        return fail(err, "exit=$exit ${YtdlpCommand.briefError(stderr)} ($where)")
    }

    private fun fail(error: StreamError, detail: String): StreamOutcome.Failure {
        AppLog.log("stream", LogLevel.Warn, TAG, "$error: $detail")
        return StreamOutcome.Failure(error, detail)
    }

    private companion object {
        const val TAG = "ytdlp"
        const val RESOLVE_TIMEOUT_MS = 75_000L
        const val SEARCH_TIMEOUT_MS = 60_000L
    }
}
