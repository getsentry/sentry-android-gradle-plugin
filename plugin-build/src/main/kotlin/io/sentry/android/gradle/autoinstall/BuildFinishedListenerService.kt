@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.util.getBuildServiceName
import org.gradle.api.invocation.Gradle
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class BuildFinishedListenerService : BuildService<BuildServiceParameters.None>,
    AutoCloseable {
    private val actionsOnClose = mutableListOf<() -> Unit>()

    fun onClose(action: () -> Unit) {
        actionsOnClose.add(action)
    }

    override fun close() {
        for (action in actionsOnClose) {
            action()
        }
        actionsOnClose.clear()
    }

    companion object {
        fun getInstance(gradle: Gradle): BuildFinishedListenerService {
            return gradle.sharedServices
                .registerIfAbsent(
                    getBuildServiceName(BuildFinishedListenerService::class.java),
                    BuildFinishedListenerService::class.java
                ) {}.get()
        }
    }
}
