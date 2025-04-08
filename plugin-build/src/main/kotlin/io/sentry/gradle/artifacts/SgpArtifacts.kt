/**
 * Adapted from https://github.com/autonomousapps/dependency-analysis-gradle-plugin/blob/1033bad8a8fe15c53c9095293eb97e79890ea63f/src/main/kotlin/com/autonomousapps/internal/artifacts/DagpArtifacts.kt
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
import org.gradle.api.attributes.Category
import org.gradle.api.model.ObjectFactory

internal interface SgpArtifacts : Named {
  companion object {
    @JvmField val SGP_ARTIFACTS_ATTRIBUTE: Attribute<SgpArtifacts> = Attribute.of(
      "sgp.internal.artifacts", SgpArtifacts::class.java
    )

    @JvmField val CATEGORY_ATTRIBUTE: Attribute<Category> = Category.CATEGORY_ATTRIBUTE

    fun category(objects: ObjectFactory): Category {
      return objects.named(Category::class.java, "sentry")
    }
  }

  enum class Kind(
    val declarableName: String,
    val artifactName: String,
  ) {
    SOURCE_ROOTS("sourceRoots", "source-roots"),
    BUNDLE_ID("bundleId", "bundle-id"),
  }
}