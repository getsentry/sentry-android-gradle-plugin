@file:Suppress("UnstableApiUsage") // Shared build services are incubating but available from 6.1

package io.sentry.android.gradle.services

import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.getBuildServiceName
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class SentryModulesService : BuildService<BuildServiceParameters.None> {

    @get:Synchronized
    @set:Synchronized
    var modules: Map<String, SemVer> = emptyMap()

    companion object {
        fun register(project: Project): Provider<SentryModulesService> {
            return project.gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(SentryModulesService::class.java),
                SentryModulesService::class.java
            ) {}
        }
    }
}
