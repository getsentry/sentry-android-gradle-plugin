package io.sentry.android.gradle.util

import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Attribute

/**
 * Adapted from https://gist.github.com/autonomousapps/f0133e58a612b6837f3f4f6554337035
 */

private val attributeKey: Attribute<String> = Attribute.of("artifactType", String::class.java)

fun Configuration.artifactsFor(
    attrValue: String
) = externalArtifactViewOf(attrValue)
    .artifacts

fun Configuration.externalArtifactViewOf(
    attrValue: String
): ArtifactView = incoming.artifactView { view ->
    view.attributes.attribute(attributeKey, attrValue)
    // If some dependency doesn't have the expected attribute, don't fail. Continue!
    view.lenient(true)
    // Only resolve external dependencies! Without this, all project dependencies will get _compiled_.
    view.componentFilter { id -> id is ModuleComponentIdentifier }
}
