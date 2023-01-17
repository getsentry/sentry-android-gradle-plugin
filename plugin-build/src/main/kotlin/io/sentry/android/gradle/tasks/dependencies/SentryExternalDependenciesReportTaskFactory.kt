package io.sentry.android.gradle.tasks.dependencies

import io.sentry.android.gradle.util.GradleVersions
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

object SentryExternalDependenciesReportTaskFactory {

    fun register(
        project: Project,
        configurationName: String,
        attributeValueJar: String,
        output: Provider<RegularFile>,
        includeReport: Provider<Boolean>,
        taskSuffix: String = ""
    ): TaskProvider<out Task> {
        // gradle 7.5 supports passing configuration resolution as task input and respects config
        // cache, so we have a different implementation from that version onwards
        return if (GradleVersions.CURRENT >= GradleVersions.VERSION_7_5) {
            SentryExternalDependenciesReportTaskV2.register(
                project,
                configurationName,
                attributeValueJar,
                includeReport,
                taskSuffix
            )
        } else {
            SentryExternalDependenciesReportTask.register(
                project,
                configurationName,
                attributeValueJar,
                output,
                includeReport,
                taskSuffix
            )
        }
    }
}
