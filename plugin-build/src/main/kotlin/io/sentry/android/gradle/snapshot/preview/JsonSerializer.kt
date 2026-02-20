package io.sentry.android.gradle.snapshot.preview

/** Hand-rolled JSON serialization for [PreviewSnapshotConfig]. */
internal object JsonSerializer {

  fun serialize(configs: List<PreviewSnapshotConfig>): String {
    val sb = StringBuilder()
    sb.append("[\n")
    configs.forEachIndexed { index, config ->
      sb.append("  {")
      sb.append("\"displayName\":\"").append(escapeJson(config.displayName)).append("\",")
      sb.append("\"className\":\"").append(escapeJson(config.className)).append("\",")
      sb.append("\"methodName\":\"").append(escapeJson(config.methodName)).append("\"")
      sb.append("}")
      if (index < configs.size - 1) sb.append(",")
      sb.append("\n")
    }
    sb.append("]")
    return sb.toString()
  }

  private fun escapeJson(s: String): String =
    s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\t", "\\t")
      .replace("\r", "\\r")
}
