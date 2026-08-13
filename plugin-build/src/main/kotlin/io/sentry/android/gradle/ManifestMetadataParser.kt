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
        val application =
          document.getElementsByTagName(TAG_APPLICATION).item(0) ?: return emptyMap()
        val metadata = linkedMapOf<String, Any>()

        for (index in 0 until application.childNodes.length) {
          val element = application.childNodes.item(index) as? Element ?: continue
          if (element.tagName != TAG_META_DATA) continue

          val name = element.getAttribute(ATTR_NAME)
          if (!name.startsWith(SENTRY_PREFIX)) continue
          val value = element.getAttribute(ATTR_VALUE)
          if (
            element.hasAttribute(ATTR_RESOURCE) ||
              !element.hasAttribute(ATTR_VALUE) ||
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
      else -> parseInteger(value) ?: value.toFloatOrNull() ?: value
    }

  private fun parseInteger(value: String): Int? =
    if (value.matches(DECIMAL_INTEGER)) {
      value.toIntOrNull()
    } else if (value.matches(HEX_INTEGER)) {
      value.removePrefix("+").let { Integer.decode(it) }
    } else {
      null
    }

  private const val TAG_APPLICATION = "application"
  private const val TAG_META_DATA = "meta-data"
  private const val ATTR_NAME = "android:name"
  private const val ATTR_VALUE = "android:value"
  private const val ATTR_RESOURCE = "android:resource"
  private const val SENTRY_PREFIX = "io.sentry."
  private val DECIMAL_INTEGER = Regex("[+-]?\\d+")
  private val HEX_INTEGER = Regex("[+-]?0[xX][0-9a-fA-F]+")
}
