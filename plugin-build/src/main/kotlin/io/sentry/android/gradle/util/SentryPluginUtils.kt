package io.sentry.android.gradle.util

import com.android.build.gradle.api.ApplicationVariant
import io.sentry.android.gradle.util.GroovyCompat.*
import java.util.Locale
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
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

    fun isMinificationEnabled(project: Project, variant: ApplicationVariant): Boolean {
        var isConfiguredWithGuardsquareProguard = false
        project.plugins.withId("com.guardsquare.proguard") {
            val proguardExtension = project.extensions.getByType(
                ProGuardAndroidExtension::class.java
            )
            val variantConfiguration = proguardExtension.configurations.findByName(variant.name)
            isConfiguredWithGuardsquareProguard = variantConfiguration != null
        }
        val isConfiguredWithGuardsquareDexguard = isDexguardEnabledForVariant(project, variant.name)

        return isConfiguredWithGuardsquareProguard ||
            isConfiguredWithGuardsquareDexguard ||
            variant.buildType.isMinifyEnabled
    }
}
