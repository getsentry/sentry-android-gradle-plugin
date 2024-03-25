package io.sentry.android.gradle.util

import io.sentry.android.gradle.SentryTasksProvider.getAssembleTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getInstallTaskProvider
import io.sentry.android.gradle.SentryTasksProvider.getMinifyTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageBundleTask
import io.sentry.android.gradle.SentryTasksProvider.getPackageProvider
import io.sentry.android.gradle.SentryTasksProvider.getPreBundleTask
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
        val minifyTask = getMinifyTask(
            project,
            variantName,
            dexguardEnabled
        )

        // we just hack ourselves into the Proguard/R8/DexGuard task's doLast.
        minifyTask?.configure {
            it.finalizedBy(this)
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
        getAssembleTaskProvider(project, variant)?.configure {
            it.finalizedBy(this)
        }
        getInstallTaskProvider(project, variant)?.configure {
            it.finalizedBy(this)
        }
        // if its a bundle aab, assemble might not be executed, so we hook into bundle task
        bundleTask?.configure { it.finalizedBy(this) }
    }
}
