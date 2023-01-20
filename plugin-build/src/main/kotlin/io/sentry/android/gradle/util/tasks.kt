package io.sentry.android.gradle.util

import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.SentryTasksProvider.getTransformerTask
import io.sentry.android.gradle.util.SentryPluginUtils.withLogging
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

fun TaskProvider<out Task>.hookWithMinifyTasks(
    project: Project,
    variantName: String,
    experimentalGuardsquareSupport: Boolean
) {
    // we need to wait for project evaluation to have all tasks available, otherwise the new
    // AndroidComponentsExtension is configured too early to look up for the tasks
    project.afterEvaluate {
        val transformerTaskProvider = withLogging(project.logger, "transformerTask") {
            getTransformerTask(
                project,
                variantName,
                experimentalGuardsquareSupport
            )
        }

        if (experimentalGuardsquareSupport &&
            GroovyCompat.isDexguardEnabledForVariant(project, variantName)
        ) {
            project.tasks.named(
                "dexguardApk${variantName.capitalized}"
            ).configure { it.finalizedBy(this) }
            project.tasks.named(
                "dexguardAab${variantName.capitalized}"
            ).configure { it.finalizedBy(this) }
        } else {
            // we just hack ourselves into the Proguard/R8 task's doLast.
            transformerTaskProvider?.configure {
                it.finalizedBy(this)
            }
        }
    }
}
