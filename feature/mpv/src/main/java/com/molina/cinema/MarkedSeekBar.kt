package com.molina.cinema

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.SeekBar

/** SeekBar dengan titik emas penanda posisi (lihat VideoBookmarks). Posisi dihitung dari paddingLeft/Right. */
class MarkedSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.seekBarStyle
) : SeekBar(context, attrs, defStyleAttr) {
    private var marks: List<Double> = emptyList()
    private var unitsPerSec = 1
    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6B450.toInt()
        style = Paint.Style.FILL
    }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }

    /** [secs] = posisi penanda (detik); [unitsPerSec] = skala progress SeekBar per detik. */
    fun setMarks(secs: List<Double>, unitsPerSec: Int) {
        marks = secs
        this.unitsPerSec = if (unitsPerSec > 0) unitsPerSec else 1
        invalidate()
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = max
        if (marks.isEmpty() || total <= 0) return
        val left = paddingLeft.toFloat()
        val span = (width - paddingLeft - paddingRight).toFloat()
        if (span <= 0f) return
        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val r = 3.5f * density
        for (s in marks) {
            val frac = (s * unitsPerSec / total).toFloat()
            if (frac < 0f || frac > 1f) continue
            val cx = left + span * frac
            canvas.drawCircle(cx, cy, r, fill)
            canvas.drawCircle(cx, cy, r, edge)
        }
    }
}
