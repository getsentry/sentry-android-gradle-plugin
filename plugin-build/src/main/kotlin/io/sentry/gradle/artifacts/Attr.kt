/**
 * Adapted from https://github.com/autonomousapps/dependency-analysis-gradle-plugin/blob/1033bad8a8fe15c53c9095293eb97e79890ea63f/src/main/kotlin/com/autonomousapps/internal/artifacts/Attr.kt
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

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

/** Teach Gradle how custom configurations relate to each other, and the artifacts they provide and consume. */
internal class Attr<T : Named>(
  val attribute: Attribute<T>,
  val attributeName: String,
)
