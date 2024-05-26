import java.io.File
import java.nio.file.Files

class XmpFileWrapper(
  private val file: File
) {

  private val originalLines = ArrayList<String>()

  private val lines = ArrayList<String>()

  fun read() {
    lines.addAll(file.readLines())
    originalLines.addAll(lines)
  }

  fun iso(): Int? {
    var startIdx = lines.indexOfFirst { it.contains("<exif:ISOSpeedRatings>") }
    if (startIdx < 0) return null
    while (startIdx < lines.size) {
      val line = lines[startIdx]
      if (line.contains("<rdf:li>")) {
        return line.replace("<rdf:li>", "").replace("</rdf:li>", "").trim().toInt()
      }
      startIdx += 1
    }
    return null
  }

  fun lens(): String? {
    val line = lines.firstOrNull { it.contains("aux:Lens=") } ?: return null
    return line.trim().replace("aux:Lens=\"", "").replace("\"", "")
  }

  fun applySigmaLensProfile() {
    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      if (line.contains("crs:LensProfileEnable=\"0\"")) {
        lines.removeAt(i)
        lines.addAll(i, listOf(
            "crs:LensProfileEnable=\"1\"",
            "crs:LensProfileSetup=\"Custom\"",
            "crs:LensProfileName=\"Adobe (SIGMA 24-70mm F2.8 DG OS HSM A017, NIKON CORPORATION)\"",
            "crs:LensProfileFilename=\"NIKON CORPORATION (SIGMA 24-70mm F2.8 DG OS HSM A017) - RAW.lcp\"",
            "crs:LensProfileDigest=\"CACEB3E75CB0355F1471E572CA24EE90\"",
            "crs:LensProfileIsEmbedded=\"False\"",
            "crs:LensProfileDistortionScale=\"100\"",
            "crs:LensProfileVignettingScale=\"100\""
        ))
        break
      }
      i += 1
    }
  }

  fun applySharpness(value: Int) {
    applyParam("Sharpness", value.toString())
  }

  fun applyNoiseReduction(value: Int) {
    applyParam("LuminanceSmoothing", value.toString())
  }

  fun applyColorNoiseReduction(value: Int) {
    applyParam("ColorNoiseReduction", value.toString())
  }

  fun applyParam(paramName: String, value: String) {
    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      if (line.contains("crs:$paramName=")) {
        lines.removeAt(i)
        lines.add(i, "crs:$paramName=\"$value\"" )
        break
      }
      i++
    }
  }

  fun write() {
    Files.write(file.toPath(), lines)
    Files.write(File(file.absolutePath + ".bk").toPath(), originalLines)
  }
}