package io.sentry.android.gradle.util

import com.android.builder.model.Version
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.util.GradleVersion

internal object AgpVersions {
  val CURRENT: SemVer = SemVer.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION)
  val VERSION_7_4_0: SemVer = SemVer.parse("7.4.0-rc01")
  val isAGP74: Boolean
    get() = isAGP74(CURRENT)

  fun isAGP74(current: SemVer) = current >= VERSION_7_4_0
}

internal object GradleVersions {
  val CURRENT: SemVer = SemVer.parse(GradleVersion.current().version)
  val VERSION_7_4: SemVer = SemVer.parse("7.4")
  val VERSION_7_5: SemVer = SemVer.parse("7.5")
  val VERSION_8_0: SemVer = SemVer.parse("8.0")
}

internal object SentryVersions {
  internal val VERSION_DEFAULT = SemVer()
  internal val VERSION_PERFORMANCE = SemVer(4, 0, 0)
  internal val VERSION_ANDROID_OKHTTP = SemVer(5, 0, 0)
  internal val VERSION_FILE_IO = SemVer(5, 5, 0)
  internal val VERSION_COMPOSE = SemVer(6, 7, 0)
  internal val VERSION_LOGCAT = SemVer(6, 17, 0)
  internal val VERSION_APP_START = SemVer(7, 1, 0)
  internal val VERSION_SQLITE = SemVer(6, 21, 0)
  internal val VERSION_ANDROID_OKHTTP_LISTENER = SemVer(6, 20, 0)
  internal val VERSION_OKHTTP = SemVer(7, 0, 0)
}

internal object SentryModules {
  internal val SENTRY = DefaultModuleIdentifier.newId("io.sentry", "sentry")
  internal val SENTRY_ANDROID = DefaultModuleIdentifier.newId("io.sentry", "sentry-android")
  internal val SENTRY_ANDROID_CORE =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-android-core")
  internal val SENTRY_ANDROID_NDK = DefaultModuleIdentifier.newId("io.sentry", "sentry-android-ndk")
  internal val SENTRY_ANDROID_SQLITE =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-android-sqlite")
  internal val SENTRY_ANDROID_OKHTTP =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-android-okhttp")
  internal val SENTRY_ANDROID_COMPOSE =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-compose-android")
  internal val SENTRY_ANDROID_FRAGMENT =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-android-fragment")
  internal val SENTRY_ANDROID_NAVIGATION =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-android-navigation")
  internal val SENTRY_ANDROID_TIMBER =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-android-timber")
  internal val SENTRY_OKHTTP = DefaultModuleIdentifier.newId("io.sentry", "sentry-okhttp")
  internal val SENTRY_KOTLIN_EXTENSIONS =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-kotlin-extensions")
  internal val SENTRY_JDBC = DefaultModuleIdentifier.newId("io.sentry", "sentry-jdbc")
  internal val SENTRY_GRAPHQL = DefaultModuleIdentifier.newId("io.sentry", "sentry-graphql")
  internal val SENTRY_LOG4J2 = DefaultModuleIdentifier.newId("io.sentry", "sentry-log4j2")
  internal val SENTRY_LOGBACK = DefaultModuleIdentifier.newId("io.sentry", "sentry-logback")
  internal val SENTRY_QUARTZ = DefaultModuleIdentifier.newId("io.sentry", "sentry-quartz")
  internal val SENTRY_SPRING5 = DefaultModuleIdentifier.newId("io.sentry", "sentry-spring")
  internal val SENTRY_SPRING6 = DefaultModuleIdentifier.newId("io.sentry", "sentry-spring-jakarta")
  internal val SENTRY_SPRING_BOOT2 =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-spring-boot")
  internal val SENTRY_SPRING_BOOT3 =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-spring-boot-jakarta")
  internal val SENTRY_BOM = DefaultModuleIdentifier.newId("io.sentry", "sentry-bom")
  internal val SENTRY_OPENTELEMETRY_AGENTLESS =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-opentelemetry-agentless")
  internal val SENTRY_OPENTELEMETRY_AGENTLESS_SPRING =
    DefaultModuleIdentifier.newId("io.sentry", "sentry-opentelemetry-agentless-spring")
}
