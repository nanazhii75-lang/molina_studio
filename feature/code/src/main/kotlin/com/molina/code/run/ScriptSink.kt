package com.molina.code.run

/**
 * Isi editor yang dikirim ke Script Execution. [sourceKey] mengenali sumber yang sama (path berkas,
 * atau judul tab untuk tab tanpa berkas) sehingga mengirim ulang memperbarui entri lama, bukan menggandakan.
 */
class ScriptShare(val title: String, val sourceKey: String, val text: String)

/** Dipasang :app (penyimpanan skrip di Settings). Mengembalikan pesan siap tampil untuk Toast. */
fun interface ScriptSink {
    fun send(share: ScriptShare): String
}
