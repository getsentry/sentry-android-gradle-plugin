package io.sentry

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class SentryKotlinCompilerPluginExtension(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
}
