package io.sentry.android.gradle.internal

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.io.File

object BootstrapAndroidSdk {
    fun locateAndroidSdk(project: Project, extra: ExtraPropertiesExtension) {
        val sdkPath: String? = when {
            !System.getenv("ANDROID_SDK_ROOT").isNullOrBlank() -> System.getenv("ANDROID_SDK_ROOT")
            !System.getenv("ANDROID_HOME").isNullOrBlank() -> System.getenv("ANDROID_HOME")
            else -> {
                val localProperties = File("${project.rootDir}/..", "local.properties")
                if (localProperties.exists()) {
                    val properties = java.util.Properties()
                    localProperties.inputStream().use { properties.load(it) }
                    properties["sdk.dir"] as String
                } else {
                    null
                }
            }
        }

        if (sdkPath != null) {
            val platforms = File(sdkPath, "platforms")
            val latest = platforms.listFiles()
                ?.filter { it.isDirectory }
                ?.maxOf { sdkNameToVersionNumber(it.name.substringAfter("-")) }
            if (latest != null) {
                extra["androidSdkPath"] = "$sdkPath/platforms/android-$latest/android.jar"
            } else {
                project.logger.warn("No available android sdks. The tests in plugin-build might not work")
            }
        } else {
            project.logger.warn("Unable to detect the android sdk path. The tests in plugin-build might not work")
        }
    }

    private fun sdkNameToVersionNumber(sdkName: String): Int {
        return when {
            sdkName.toIntOrNull() != null -> sdkName.toInt()
            sdkName.equals("UpsideDownCake", true) -> 34
            sdkName.equals("Tiramisu", true) -> 33
            sdkName.equals("TiramisuPrivacySandbox", true) -> 33
            else -> 0
        }
    }
}
