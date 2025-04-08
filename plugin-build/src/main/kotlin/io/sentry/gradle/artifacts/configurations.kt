/**
 * Adapted from https://github.com/autonomousapps/dependency-analysis-gradle-plugin/blob/1033bad8a8fe15c53c9095293eb97e79890ea63f/src/main/kotlin/com/autonomousapps/internal/artifacts/configurations.kt
 *
 * Copyright 2024 Anthony Robalik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sentry.gradle.artifacts

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Creates a "dependency scope"-type configuration, which we can think of as a _bucket_ for declaring dependencies. See
 * also [resolvableConfiguration] and [consumableConfiguration].
 */
internal fun Project.dependencyScopeConfiguration(configurationName: String): NamedDomainObjectProvider<out Configuration> {
    return configurations.register(configurationName) { config ->
      config.isCanBeResolved = false
      config.isCanBeConsumed = true
      config.isVisible = false
  }
}

/**
 * Creates a "resolvable"-type configuration, which can be thought of as the method by which projects "resolve" the
 * dependencies that they declare on the [dependencyScopeConfiguration] configurations.
 */
internal fun Project.resolvableConfiguration(
  configurationName: String,
  dependencyScopeConfiguration: Configuration,
  configureAction: Action<in Configuration>,
): NamedDomainObjectProvider<out Configuration> {
  return configurations.register(configurationName) { config ->
    config.isCanBeResolved = true
    config.isCanBeConsumed = false
    config.isVisible = false

    config.extendsFrom(dependencyScopeConfiguration)

    configureAction.execute(config)
  }
}

/**
 * Creates a "consumable"-type configuration, which can be thought of as the method by which projects export artifacts
 * to consumer projects, which have declared a dependency on _this_ project using the [dependencyScopeConfiguration]
 * configuration (which may be `null` for this project).
 */
internal fun Project.consumableConfiguration(
  configurationName: String,
  dependencyScopeConfiguration: Configuration? = null,
  configureAction: Action<in Configuration>,
): NamedDomainObjectProvider<out Configuration> {
 return configurations.register(configurationName) { config ->
   config.isCanBeConsumed = true
   config.isCanBeResolved = false
   config.isVisible = false

    dependencyScopeConfiguration?.let { config.extendsFrom(it) }

    configureAction.execute(config)
 }
}
