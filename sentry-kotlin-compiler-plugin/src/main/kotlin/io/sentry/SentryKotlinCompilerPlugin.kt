package io.sentry

import com.google.auto.service.AutoService
import io.sentry.compose.registerComposeTracing19
import io.sentry.compose.registerComposeTracing21
import io.sentry.compose.registerComposeTracing22
import io.sentry.compose.registerComposeTracing24
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class SentryKotlinCompilerPlugin : CompilerPluginRegistrar() {

  override val supportsK2: Boolean
    get() = true

  override val pluginId: String = PLUGIN_ID

  // The extension registration API is binary-incompatible across compiler versions
  // (e.g. 2.4 replaced ProjectExtensionDescriptor with ExtensionPointDescriptor), so the
  // registration itself is delegated to a per-version helper compiled against that version's
  // compiler. This class only dispatches and must reference no version-specific compiler API.
  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val versionString = KotlinCompilerVersion.getVersion()
    val version =
      if (versionString != null) {
        SimpleSemVer.parse(versionString)
      } else {
        SimpleSemVer(2, 1, 20)
      }

    when {
      version >= SimpleSemVer(2, 4, 0) -> registerComposeTracing24(configuration)
      version >= SimpleSemVer(2, 2, 0) -> registerComposeTracing22(configuration)
      version >= SimpleSemVer(2, 1, 20) -> registerComposeTracing21(configuration)
      else -> registerComposeTracing19(configuration)
    }
  }
}
