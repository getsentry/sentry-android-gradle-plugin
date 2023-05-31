/*
 * Adapted from https://github.com/JetBrains/kotlin/blob/bc602b3827bea5e3b30fbc871aec4be7309ddaee/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/BuildFinishedListenerService.kt
 *
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.util.getBuildServiceName
import org.gradle.api.invocation.Gradle
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class BuildFinishedListenerService :
    BuildService<BuildServiceParameters.None>,
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
            return gradle.sharedServices.registerIfAbsent(
                getBuildServiceName(BuildFinishedListenerService::class.java),
                BuildFinishedListenerService::class.java
            ) {}.get()
        }
    }
}
