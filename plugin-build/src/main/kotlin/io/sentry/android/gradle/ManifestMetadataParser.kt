package io.sentry.android.gradle

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

internal object ManifestMetadataParser {
  fun parse(manifest: File): Map<String, Any>? =
    runCatching {
        val document =
          manifest.inputStream().buffered().use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
          }
        val application = document.getElementsByTagName("application").item(0) ?: return emptyMap()
        val metadata = linkedMapOf<String, Any>()

        for (index in 0 until application.childNodes.length) {
          val element = application.childNodes.item(index) as? Element ?: continue
          if (element.tagName != "meta-data") continue

          val name = element.getAttribute("android:name")
          if (!name.startsWith("io.sentry.")) continue
          val value = element.getAttribute("android:value")
          if (
            element.hasAttribute("android:resource") ||
              !element.hasAttribute("android:value") ||
              value.startsWith("@") ||
              value.contains("${'$'}{")
          ) {
            SentryPlugin.logger.info(
              "Sentry manifest metadata was not optimized because $name could not be resolved at build time."
            )
            return null
          }
          metadata[name] = inferType(value)
        }
        metadata
      }
      .onFailure {
        SentryPlugin.logger.info(
          "Sentry manifest metadata could not be parsed for optimization.",
          it,
        )
      }
      .getOrNull()

  internal fun inferType(value: String): Any =
    when (value) {
      "true" -> true
      "false" -> false
      else -> parseInteger(value) ?: value.toFloatOrNull()?.takeIf { it.isFinite() } ?: value
    }

  private fun parseInteger(value: String): Int? =
    if (value.matches(DECIMAL_INTEGER)) {
      value.toIntOrNull()
    } else if (value.matches(HEX_INTEGER)) {
      value.removePrefix("+").let { Integer.decode(it) }
    } else {
      null
    }

  private val DECIMAL_INTEGER = Regex("[+-]?\\d+")
  private val HEX_INTEGER = Regex("[+-]?0[xX][0-9a-fA-F]+")
}
