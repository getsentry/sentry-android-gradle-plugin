package io.sentry.android.gradle.util

import io.sentry.android.gradle.SentryTasksProvider.capitalized
import io.sentry.android.gradle.SentryTasksProvider.getAssembleTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageProvider
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getTransformerTask
import io.sentry.android.gradle.util.SentryPluginUtils.withLogging
import io.sentry.gradle.common.SentryVariant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

fun TaskProvider<out Task>.hookWithMinifyTasks(
    project: Project,
    variantName: String,
    dexguardEnabled: Boolean
) {
    // we need to wait for project evaluation to have all tasks available, otherwise the new
    // AndroidComponentsExtension is configured too early to look up for the tasks
    project.afterEvaluate {
        val transformerTaskProvider = withLogging(project.logger, "transformerTask") {
            getTransformerTask(
                project,
                variantName,
                dexguardEnabled
            )
        }

        if (dexguardEnabled &&
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

fun TaskProvider<out Task>.hookWithPackageTasks(
    project: Project,
    variant: SentryVariant
) {
    val variantName = variant.name
    val preBundleTaskProvider = withLogging(project.logger, "preBundleTask") {
        getPreBundleTask(project, variantName)
    }
    val packageBundleTaskProvider = withLogging(project.logger, "packageBundleTask") {
        getPackageBundleTask(project, variantName)
    }

    // To include proguard uuid file into aab, run before bundle task.
    preBundleTaskProvider?.configure { task ->
        task.dependsOn(this)
    }
    // The package task will only be executed if the generateUuidTask has already been executed.
    getPackageProvider(variant)?.configure { task ->
        task.dependsOn(this)
    }

    // App bundle has different package task
    packageBundleTaskProvider?.configure { task ->
        task.dependsOn(this)
    }
}

fun TaskProvider<out Task>.hookWithAssembleTasks(
    project: Project,
    variant: SentryVariant
) {
    // we need to wait for project evaluation to have all tasks available, otherwise the new
    // AndroidComponentsExtension is configured too early to look up for the tasks
    project.afterEvaluate {
        val bundleTask = withLogging(project.logger, "bundleTask") {
            getBundleTask(project, variant.name)
        }
        getAssembleTaskProvider(variant)?.configure {
            it.finalizedBy(this)
        }
        // if its a bundle aab, assemble might not be executed, so we hook into bundle task
        bundleTask?.configure { it.finalizedBy(this) }
    }
}
