package io.sentry.android.gradle.util

import com.android.build.gradle.api.ApplicationVariant
import io.sentry.android.gradle.util.GroovyCompat.isDexguardEnabledForVariant
import java.io.File
import java.util.Locale
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import proguard.gradle.plugin.android.dsl.ProGuardAndroidExtension

internal object SentryPluginUtils {

    fun withLogging(
        logger: Logger,
        varName: String,
        initializer: () -> TaskProvider<Task>?
    ) = initializer().also {
        logger.info { "$varName is ${it?.name}" }
    }

    fun String.capitalizeUS() = if (isEmpty()) {
        ""
    } else {
        substring(0, 1).toUpperCase(Locale.US) + substring(1)
    }

    fun isMinificationEnabled(
        project: Project,
        variant: ApplicationVariant,
        experimentalGuardsquareSupport: Boolean = false
    ): Boolean {
        if (experimentalGuardsquareSupport) {
            var isConfiguredWithGuardsquareProguard = false
            project.plugins.withId("com.guardsquare.proguard") {
                val proguardExtension = project.extensions.getByType(
                    ProGuardAndroidExtension::class.java
                )
                val variantConfiguration = proguardExtension.configurations.findByName(variant.name)
                isConfiguredWithGuardsquareProguard = variantConfiguration != null
            }
            val isConfiguredWithGuardsquareDexguard = isDexguardEnabledForVariant(
                project,
                variant.name
            )
            if (isConfiguredWithGuardsquareProguard || isConfiguredWithGuardsquareDexguard) {
                return true
            }
        }
        return variant.buildType.isMinifyEnabled
    }

    fun getAndDelete(property: Provider<RegularFile>): File {
        val file = property.get().asFile
        file.delete()
        return file
    }
}
