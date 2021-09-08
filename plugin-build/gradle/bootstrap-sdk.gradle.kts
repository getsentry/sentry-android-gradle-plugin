// bootstrap the local android-sdk path, so we can use it as a dependency to run bytecode instrumentation tests
fun locateAndroidSdk() {
    val sdkPath: String? = when {
        !System.getenv("ANDROID_SDK_ROOT").isNullOrBlank() -> System.getenv("ANDROID_SDK_ROOT")
        !System.getenv("ANDROID_HOME").isNullOrBlank() -> System.getenv("ANDROID_HOME")
        else -> {
            val localProperties = File("$rootDir/..", "local.properties")
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
            ?.maxOf { it.name.substringAfter("-").toInt() }
        if (latest != null) {
            extra["androidSdkPath"] = "$sdkPath/platforms/android-${latest}/android.jar"
        } else {
            logger.warn("No available android sdks. The tests in plugin-build might not work")
        }
    } else {
        logger.warn("Unable to detect the android sdk path. The tests in plugin-build might not work")
    }
}
locateAndroidSdk()
