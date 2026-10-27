package com.molina.cinema

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/**
 * Penanda posisi video (detik) per media, disimpan sebagai satu objek JSON di SharedPreferences
 * "molina_bookmarks". Kunci media = berkas lokal atau tautan asal (bukan URL stream hasil resolve).
 * Batas: 200 media (yang paling lama tak disentuh dibuang) dan 50 penanda per media.
 */
object VideoBookmarks {
    private const val PREFS = "molina_bookmarks"
    private const val KEY = "all"
    private const val MAX_VIDEOS = 200
    private const val MAX_PER_VIDEO = 50
    private const val MIN_GAP_SEC = 1.0

    private fun readAll(ctx: Context): LinkedHashMap<String, MutableList<Double>> {
        val out = LinkedHashMap<String, MutableList<Double>>()
        val raw = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return out
        try {
            val o = JSONObject(raw)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = o.getJSONArray(k)
                out[k] = MutableList(arr.length()) { i -> arr.getDouble(i) }
            }
        } catch (e: Exception) {
            out.clear()
        }
        return out
    }

    private fun writeAll(ctx: Context, all: LinkedHashMap<String, MutableList<Double>>) {
        while (all.size > MAX_VIDEOS) all.remove(all.keys.first())
        val o = JSONObject()
        for ((k, v) in all) if (v.isNotEmpty()) o.put(k, JSONArray(v))
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, o.toString()).apply()
    }

    fun list(ctx: Context, key: String): List<Double> = readAll(ctx)[key]?.sorted() ?: emptyList()

    /** True bila ditambahkan; false bila sudah ada penanda dalam 1 detik dari posisi ini. */
    fun add(ctx: Context, key: String, sec: Double): Boolean {
        val all = readAll(ctx)
        val marks = all.remove(key) ?: mutableListOf()
        val s = Math.round(sec * 10.0) / 10.0
        if (marks.any { abs(it - s) < MIN_GAP_SEC }) {
            all[key] = marks
            writeAll(ctx, all)
            return false
        }
        marks.add(s)
        marks.sort()
        while (marks.size > MAX_PER_VIDEO) marks.removeAt(0)
        all[key] = marks
        writeAll(ctx, all)
        return true
    }

    fun removeAt(ctx: Context, key: String, index: Int) {
        val all = readAll(ctx)
        val marks = all[key]?.sorted()?.toMutableList() ?: return
        if (index in marks.indices) {
            marks.removeAt(index)
            all[key] = marks
            writeAll(ctx, all)
        }
    }

    fun clear(ctx: Context, key: String) {
        val all = readAll(ctx)
        if (all.remove(key) != null) writeAll(ctx, all)
    }
}
