package io.sentry

import com.google.auto.service.AutoService
import io.sentry.compose.JetpackComposeTracingIrExtension19
import io.sentry.compose.JetpackComposeTracingIrExtension21
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class SentryKotlinCompilerPlugin : CompilerPluginRegistrar() {

  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val messageCollector =
      configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

    val version = configuration.languageVersionSettings.languageVersion
    val extension: IrGenerationExtension =
      if (version.major > 2 || (version.major == 2 && version.minor >= 1)) {
        // 2.1.20 removed some optional parameters, causing API incompatibility
        // e.g. java.lang.NoSuchMethodError
        // see https://github.com/JetBrains/kotlin/commit/dd508452c414a0ee8082aa6f76d664271cb38f2f
        JetpackComposeTracingIrExtension21(messageCollector)
      } else {
        JetpackComposeTracingIrExtension19(messageCollector)
      }

    IrGenerationExtension.registerExtension(extension)
  }
}
