package io.sentry

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class SentryKotlinCompilerGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = getPluginArtifact().let {
        "${it.groupId}.${it.artifactId}"
    }

    override fun getPluginArtifact(): SubpluginArtifact {
        // needs to match sentry-kotlin-compiler-plugin/build.gradle.kts
        return SubpluginArtifact(
            groupId = "io.sentry",
            artifactId = "sentry-kotlin-compiler-plugin",
            version = "1.0.0-SNAPSHOT"
        )
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider {
            emptyList()
        }
    }
}
