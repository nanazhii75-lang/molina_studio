package com.molina.studio.packages

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Satu alat di Settings > Paket: tiga skrip (pasang, versi, hapus) yang dijalankan di Debian. */
data class ToolItem(
    val id: String,
    val name: String,
    val note: String,
    val install: String,
    val version: String,
    val remove: String,
    val builtin: Boolean = false
)

/** Hasil pemeriksaan versi terakhir; [text] kosong = belum diperiksa. */
class ToolCheck(val text: String, val at: Long)

class ToolResult(val ok: Boolean, val message: String, val id: String? = null)

private const val APT = "-o APT::Sandbox::User=root"

/** Alat bawaan (tidak bisa diubah atau dibuang). yt-dlp punya halaman sendiri karena memverifikasi SHA-256. */
object BuiltinTools {
    val all: List<ToolItem> = listOf(
        ToolItem(
            "b-python3", "Python 3", "Interpreter Python di Debian. yt-dlp membutuhkannya.",
            "apt update $APT\napt install -y $APT python3", "python3 --version", "apt remove -y $APT python3", true
        ),
        ToolItem(
            "b-git", "Git", "Sistem kontrol versi.",
            "apt update $APT\napt install -y $APT git", "git --version", "apt remove -y $APT git", true
        ),
        ToolItem(
            "b-ffmpeg", "FFmpeg", "Konversi dan pemrosesan audio/video.",
            "apt update $APT\napt install -y $APT ffmpeg",
            "command -v ffmpeg >/dev/null && ffmpeg -version | head -n 1", "apt remove -y $APT ffmpeg", true
        )
    )
}

/**
 * Alat buatan pengguna (SharedPreferences, JSON) dan hasil cek versi terakhir tiap alat. Skrip alat
 * khusus untuk memasang/memeriksa/menghapus satu alat; skrip umum ada di Script Execution.
 */
object ToolStore {
    const val MAX_TOOLS = 30
    const val MAX_NAME = 40
    const val MAX_NOTE = 200
    const val MAX_SCRIPT = 20_000
    private const val PREFS = "molina_tools"

    private var prefs: SharedPreferences? = null
    private val _custom = MutableStateFlow<List<ToolItem>>(emptyList())
    private val _checks = MutableStateFlow<Map<String, ToolCheck>>(emptyMap())
    val custom: StateFlow<List<ToolItem>> = _custom.asStateFlow()
    val checks: StateFlow<Map<String, ToolCheck>> = _checks.asStateFlow()

    @Synchronized
    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _custom.value = decodeTools(p.getString("custom", null))
        _checks.value = decodeChecks(p.getString("checks", null))
    }

    @Synchronized
    fun save(id: String?, name: String, note: String, install: String, version: String, remove: String): ToolResult {
        val n = name.trim()
        if (n.isEmpty()) return ToolResult(false, "Nama alat wajib diisi")
        if (n.length > MAX_NAME) return ToolResult(false, "Nama maksimal $MAX_NAME karakter")
        val i = norm(install)
        val v = norm(version)
        val r = norm(remove)
        if (i.isEmpty() && v.isEmpty() && r.isEmpty()) return ToolResult(false, "Isi minimal satu skrip (pasang, versi, atau hapus)")
        if (listOf(i, v, r).any { it.length > MAX_SCRIPT }) return ToolResult(false, "Skrip terlalu panjang (maks ${MAX_SCRIPT / 1000} rb karakter)")
        val nt = note.trim().take(MAX_NOTE)
        val list = _custom.value
        if (id == null) {
            if (list.size >= MAX_TOOLS) return ToolResult(false, "Daftar alat penuh ($MAX_TOOLS)")
            val item = ToolItem(newId(list), n, nt, i, v, r)
            persist(list + item)
            return ToolResult(true, "\"$n\" ditambahkan", item.id)
        }
        if (list.none { it.id == id }) return ToolResult(false, "Alat tidak ditemukan (mungkin sudah dibuang)")
        persist(list.map { if (it.id == id) it.copy(name = n, note = nt, install = i, version = v, remove = r) else it })
        return ToolResult(true, "\"$n\" disimpan", id)
    }

    @Synchronized
    fun delete(id: String) {
        persist(_custom.value.filterNot { it.id == id })
        setCheck(id, "")
    }

    /** Simpan hasil cek versi; [text] kosong menghapus hasil lama. */
    @Synchronized
    fun setCheck(id: String, text: String) {
        val m = _checks.value.toMutableMap()
        if (text.isBlank()) m.remove(id) else m[id] = ToolCheck(text.trim().take(120), System.currentTimeMillis())
        _checks.value = m
        val a = JSONObject()
        for ((k, c) in m) a.put(k, JSONObject().put("t", c.text).put("at", c.at))
        prefs?.edit()?.putString("checks", a.toString())?.apply()
    }

    private fun norm(s: String): String = s.replace("\r\n", "\n").replace('\r', '\n').trim()

    private fun newId(existing: List<ToolItem>): String {
        while (true) {
            val id = "c-" + UUID.randomUUID().toString().replace("-", "").take(8)
            if (existing.none { it.id == id }) return id
        }
    }

    private fun persist(list: List<ToolItem>) {
        _custom.value = list
        val arr = JSONArray()
        for (t in list) {
            arr.put(
                JSONObject().put("id", t.id).put("name", t.name).put("note", t.note)
                    .put("install", t.install).put("version", t.version).put("remove", t.remove)
            )
        }
        prefs?.edit()?.putString("custom", arr.toString())?.apply()
    }

    private fun decodeTools(raw: String?): List<ToolItem> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    if (id.isEmpty() || any { it.id == id }) continue
                    add(
                        ToolItem(
                            id, o.optString("name", "Alat"), o.optString("note"),
                            o.optString("install"), o.optString("version"), o.optString("remove")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun decodeChecks(raw: String?): Map<String, ToolCheck> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return try {
            val o = JSONObject(raw)
            buildMap { for (k in o.keys()) put(k, ToolCheck(o.getJSONObject(k).optString("t"), o.getJSONObject(k).optLong("at"))) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
