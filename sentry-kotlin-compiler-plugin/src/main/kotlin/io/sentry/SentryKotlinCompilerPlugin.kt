package io.sentry

import com.google.auto.service.AutoService
import io.sentry.compose.JetpackComposeTracingIrExtension19
import io.sentry.compose.JetpackComposeTracingIrExtension21
import io.sentry.compose.JetpackComposeTracingIrExtension22
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class SentryKotlinCompilerPlugin : CompilerPluginRegistrar() {

  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val messageCollector =
      configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

    val versionString = KotlinCompilerVersion.getVersion()
    val version =
      if (versionString != null) {
        SimpleSemanticVersion.from(versionString)
      } else {
        SimpleSemanticVersion(2, 1, 20)
      }

    val extension: IrGenerationExtension =
      if (version >= SimpleSemanticVersion(2, 2, 0)) {
        JetpackComposeTracingIrExtension22(messageCollector)
      } else if (version >= SimpleSemanticVersion(2, 1, 20)) {
        // 2.1.20 removed some optional parameters, causing API incompatibility
        // e.g. java.lang.NoSuchMethodError
        // see https://github.com/JetBrains/kotlin/commit/dd508452c414a0ee8082aa6f76d664271cb38f2f
        JetpackComposeTracingIrExtension21(messageCollector)
      } else {
        JetpackComposeTracingIrExtension19(messageCollector)
      }

    IrGenerationExtension.registerExtension(extension)
  }

  data class SimpleSemanticVersion(val major: Int, val minor: Int, val patch: Int) :
    Comparable<SimpleSemanticVersion> {

    companion object {
      fun from(version: String): SimpleSemanticVersion {
        val parts = version.trim().split(".")
        require(parts.size == 3) { "Invalid semantic version: $version" }

        val (major, minor, patch) =
          parts.map { it.toIntOrNull() ?: error("Invalid number in version: $version") }
        return SimpleSemanticVersion(major, minor, patch)
      }
    }

    override fun compareTo(other: SimpleSemanticVersion): Int {
      return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }
  }
}
