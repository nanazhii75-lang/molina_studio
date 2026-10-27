package com.molina.cinema

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/** Satu penanda: posisi (detik) dan nama opsional (kosong = hanya waktu). */
data class Mark(val sec: Double, val name: String = "")

/**
 * Penanda posisi video per media, disimpan sebagai satu objek JSON di SharedPreferences
 * "molina_bookmarks". Kunci media = berkas lokal atau tautan asal (bukan URL stream hasil resolve).
 * Format elemen: angka (tanpa nama, format lama) atau {"t":detik,"n":"nama"}; format lama tetap terbaca.
 * Batas: 200 media (yang paling lama tak disentuh dibuang), 50 penanda per media, nama maks. 40 karakter.
 */
object VideoBookmarks {
    private const val PREFS = "molina_bookmarks"
    private const val KEY = "all"
    private const val MAX_VIDEOS = 200
    private const val MAX_PER_VIDEO = 50
    private const val MAX_NAME = 40
    private const val MIN_GAP_SEC = 1.0

    private fun readAll(ctx: Context): LinkedHashMap<String, MutableList<Mark>> {
        val out = LinkedHashMap<String, MutableList<Mark>>()
        val raw = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return out
        try {
            val o = JSONObject(raw)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = o.getJSONArray(k)
                val list = mutableListOf<Mark>()
                for (i in 0 until arr.length()) {
                    when (val e = arr.get(i)) {
                        is JSONObject -> list.add(Mark(e.getDouble("t"), e.optString("n", "")))
                        is Number -> list.add(Mark(e.toDouble()))
                    }
                }
                out[k] = list
            }
        } catch (e: Exception) {
            out.clear()
        }
        return out
    }

    private fun writeAll(ctx: Context, all: LinkedHashMap<String, MutableList<Mark>>) {
        while (all.size > MAX_VIDEOS) all.remove(all.keys.first())
        val o = JSONObject()
        for ((k, v) in all) {
            if (v.isEmpty()) continue
            val arr = JSONArray()
            for (m in v) {
                if (m.name.isBlank()) arr.put(m.sec)
                else arr.put(JSONObject().put("t", m.sec).put("n", m.name))
            }
            o.put(k, arr)
        }
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, o.toString()).apply()
    }

    fun list(ctx: Context, key: String): List<Mark> = readAll(ctx)[key]?.sortedBy { it.sec } ?: emptyList()

    /** True bila ditambahkan; false bila sudah ada penanda dalam 1 detik dari posisi ini. */
    fun add(ctx: Context, key: String, sec: Double): Boolean {
        val all = readAll(ctx)
        val marks = all.remove(key) ?: mutableListOf()
        val s = Math.round(sec * 10.0) / 10.0
        if (marks.any { abs(it.sec - s) < MIN_GAP_SEC }) {
            all[key] = marks
            writeAll(ctx, all)
            return false
        }
        marks.add(Mark(s))
        marks.sortBy { it.sec }
        while (marks.size > MAX_PER_VIDEO) marks.removeAt(0)
        all[key] = marks
        writeAll(ctx, all)
        return true
    }

    /** Ubah nama penanda ke-[index] (urutan waktu). Nama kosong = hapus nama. */
    fun rename(ctx: Context, key: String, index: Int, name: String) {
        val all = readAll(ctx)
        val marks = all[key]?.sortedBy { it.sec }?.toMutableList() ?: return
        if (index in marks.indices) {
            marks[index] = marks[index].copy(name = name.trim().take(MAX_NAME))
            all[key] = marks
            writeAll(ctx, all)
        }
    }

    fun removeAt(ctx: Context, key: String, index: Int) {
        val all = readAll(ctx)
        val marks = all[key]?.sortedBy { it.sec }?.toMutableList() ?: return
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
