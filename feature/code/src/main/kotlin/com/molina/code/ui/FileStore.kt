package com.molina.code.ui

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * I/O berkas editor (F3e). Semua fungsi memblokir: panggil dari thread IO, bukan thread utama.
 * Enkode terkunci UTF-8 tanpa BOM (blueprint bagian 20.2).
 */
internal object FileStore {
    private const val MAX_NAME_BYTES = 240

    /** Baca UTF-8 ketat. Null bila berkas tidak terbaca atau bukan UTF-8 yang valid. */
    fun readUtf8OrNull(file: File): String? = try {
        val bytes = file.readBytes()
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
            .removePrefix("\uFEFF")
    } catch (e: Exception) {
        null
    }

    /**
     * Tulis atomik: ke berkas sementara di folder yang sama lalu rename, sehingga berkas lama
     * tidak rusak bila proses mati di tengah penulisan. Bila rename gagal, tulis langsung.
     */
    @Throws(IOException::class)
    fun writeUtf8Atomic(target: File, text: String) {
        val dir = target.absoluteFile.parentFile ?: throw IOException("Folder tidak valid")
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
            throw IOException("Tidak bisa membuat folder ${dir.path}")
        }
        val bytes = text.toByteArray(Charsets.UTF_8)
        val tmp = File(dir, ".${target.name}.molina-tmp")
        var done = false
        try {
            FileOutputStream(tmp).use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            done = tmp.renameTo(target)
        } catch (e: IOException) {
            done = false
        }
        if (!done) {
            tmp.delete()
            target.writeBytes(bytes)
        }
    }

    fun fullName(name: String, ext: String): String {
        val n = name.trim()
        val e = ext.trim().trimStart('.')
        return if (e.isEmpty()) n else "$n.$e"
    }

    /** Pesan galat untuk nama berkas yang tidak boleh dipakai, atau null bila valid. */
    fun validate(name: String, ext: String): String? {
        val n = name.trim()
        val e = ext.trim().trimStart('.')
        return when {
            n.isEmpty() -> "Nama berkas kosong"
            n == "." || n == ".." -> "Nama berkas tidak valid"
            '/' in n || '/' in e || '\u0000' in n || '\u0000' in e -> "Nama tidak boleh mengandung /"
            fullName(name, ext).toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES -> "Nama terlalu panjang"
            else -> null
        }
    }
}

/** Satu tab dalam snapshot draf. [text] null = jangan sentuh berkas draf yang sudah ada. */
internal class DraftEntry(
    val id: Int,
    val title: String,
    val path: String?,
    val dirty: Boolean,
    val wrap: Boolean,
    val readOnly: Boolean,
    val text: String?
)

internal class DraftSnapshot(val active: Int, val next: Int, val tabs: List<DraftEntry>)

/**
 * Penyimpanan draf di folder privat aplikasi (tanpa izin storage): session.json berisi daftar
 * tab, dan d<id>.txt berisi isi tab yang belum disimpan. Best-effort: galat I/O diabaikan.
 */
internal class DraftStore(private val dir: File) {
    private val index: File get() = File(dir, "session.json")

    private fun textFile(id: Int) = File(dir, "d$id.txt")

    fun load(): DraftSnapshot? = try {
        if (!index.isFile) {
            null
        } else {
            val root = JSONObject(index.readText(Charsets.UTF_8))
            val arr = root.getJSONArray("tabs")
            val tabs = (0 until arr.length()).map { i ->
                val t = arr.getJSONObject(i)
                DraftEntry(
                    id = t.getInt("id"),
                    title = t.getString("title"),
                    path = if (t.isNull("path")) null else t.getString("path"),
                    dirty = t.optBoolean("dirty", false),
                    wrap = t.optBoolean("wrap", true),
                    readOnly = t.optBoolean("ro", false),
                    text = null
                )
            }
            DraftSnapshot(root.optInt("active", -1), root.optInt("next", 1), tabs)
        }
    } catch (e: Exception) {
        null
    }

    fun readDraft(id: Int): String? = try {
        val f = textFile(id)
        if (f.isFile) f.readText(Charsets.UTF_8) else null
    } catch (e: Exception) {
        null
    }

    fun write(s: DraftSnapshot) {
        try {
            if (!dir.isDirectory) dir.mkdirs()
            val arr = JSONArray()
            for (t in s.tabs) {
                val body = t.text
                if (t.dirty && body != null) FileStore.writeUtf8Atomic(textFile(t.id), body)
                val o = JSONObject()
                o.put("id", t.id)
                o.put("title", t.title)
                o.put("path", t.path ?: JSONObject.NULL)
                o.put("dirty", t.dirty)
                o.put("wrap", t.wrap)
                o.put("ro", t.readOnly)
                arr.put(o)
            }
            val root = JSONObject()
            root.put("v", 1)
            root.put("active", s.active)
            root.put("next", s.next)
            root.put("tabs", arr)
            FileStore.writeUtf8Atomic(index, root.toString())
            // Hapus draf milik tab yang sudah bersih atau ditutup.
            val keep = s.tabs.filter { it.dirty }.map { "d${it.id}.txt" }.toSet()
            dir.listFiles()?.forEach { f ->
                if (f.name.startsWith("d") && f.name.endsWith(".txt") && f.name !in keep) f.delete()
            }
        } catch (e: Exception) {
            // best-effort
        }
    }
}
