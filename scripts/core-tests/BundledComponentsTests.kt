import com.molina.core.common.Assurance
import com.molina.core.common.BundledComponents
import com.molina.core.common.ComponentRole

private var fails = 0
private fun check(name: String, ok: Boolean) {
    if (ok) println("ok   $name") else { fails++; println("GAGAL $name") }
}

fun main() {
    val all = BundledComponents.all("v0.26.5")
    check("id unik", all.map { it.id }.toSet().size == all.size)
    check("versi NewPipe disuntikkan", all.first { it.id == "newpipe" }.version == "v0.26.5")
    check("Sha256 wajib punya pinRef", all.filter { it.assurance == Assurance.Sha256 }.all { !it.pinRef.isNullOrBlank() })
    check("tanpa Sha256 tidak boleh punya pinRef", all.filter { it.assurance != Assurance.Sha256 }.all { it.pinRef == null })
    check("sumber tanpa skema URL", all.none { it.source.contains("://") || it.note.contains("://") })
    check("ada komponen Wajib", all.any { it.role == ComponentRole.Wajib })
    check("yt-dlp opsional dan runtime", all.first { it.id == "ytdlp" }.role == ComponentRole.Opsional)
    check("hitung peran", BundledComponents.roleCounts(all).values.sum() == all.size)
    println(if (fails == 0) "SEMUA LULUS" else "$fails GAGAL")
    if (fails != 0) kotlin.system.exitProcess(1)
}
