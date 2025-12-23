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
import org.jetbrains.kotlin.config.messageCollector

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class SentryKotlinCompilerPlugin : CompilerPluginRegistrar() {

  override val supportsK2: Boolean
    get() = true

  val pluginId: String = PLUGIN_ID

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val versionString = KotlinCompilerVersion.getVersion()
    val version =
      if (versionString != null) {
        SimpleSemVer.parse(versionString)
      } else {
        SimpleSemVer(2, 1, 20)
      }

    val extension: IrGenerationExtension =
      if (version >= SimpleSemVer(2, 2, 0)) {
        val messageCollector = configuration.messageCollector
        JetpackComposeTracingIrExtension22(messageCollector)
      } else if (version >= SimpleSemVer(2, 1, 20)) {
        val messageCollector = configuration.messageCollector
        // 2.1.20 removed some optional parameters, causing API incompatibility
        // e.g. java.lang.NoSuchMethodError
        // see https://github.com/JetBrains/kotlin/commit/dd508452c414a0ee8082aa6f76d664271cb38f2f
        JetpackComposeTracingIrExtension21(messageCollector)
      } else {
        val messageCollector =
          configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        JetpackComposeTracingIrExtension19(messageCollector)
      }

    IrGenerationExtension.registerExtension(extension)
  }
}
