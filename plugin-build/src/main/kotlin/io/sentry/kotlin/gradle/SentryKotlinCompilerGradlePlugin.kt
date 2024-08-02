package io.sentry.kotlin.gradle

import io.sentry.BuildConfig
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class SentryKotlinCompilerGradlePlugin : KotlinCompilerPluginSupportPlugin {

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "io.sentry.kotlin.compiler"

  override fun getPluginArtifact(): SubpluginArtifact {
    // needs to match sentry-kotlin-compiler-plugin/build.gradle.kts
    return SubpluginArtifact(
      groupId = "io.sentry",
      artifactId = "sentry-kotlin-compiler-plugin",
      version = BuildConfig.Version,
    )
  }

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    return project.provider { emptyList() }
  }
}
