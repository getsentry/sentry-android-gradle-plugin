/*
 * Adapted from https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/blob/10c4fc09519beedd5583db866e4ffa28a57998f1/src/main/kotlin/com/autonomousapps/internal/artifactViews.kt
 *
 * Copyright 2019 Anthony Robalik
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

package io.sentry.android.gradle.util

import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Attribute

private val attributeKey: Attribute<String> = Attribute.of("artifactType", String::class.java)

fun Configuration.artifactsFor(attrValue: String) = externalArtifactViewOf(attrValue).artifacts

fun Configuration.externalArtifactViewOf(attrValue: String): ArtifactView =
  incoming.artifactView { view ->
    view.attributes.attribute(attributeKey, attrValue)
    // If some dependency doesn't have the expected attribute, don't fail. Continue!
    view.lenient(true)
    // Only resolve external dependencies! Without this, all project dependencies will get
    // _compiled_.
    view.componentFilter { id -> id is ModuleComponentIdentifier }
  }
