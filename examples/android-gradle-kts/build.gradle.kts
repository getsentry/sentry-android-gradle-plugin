import io.sentry.android.gradle.sourcecontext.CodeSourceExploderTask

plugins {
    id("com.android.application")
    id("io.sentry.android.gradle")
}

android {
    compileSdk = LibsVersion.SDK_VERSION
    defaultConfig {
        minSdk = LibsVersion.MIN_SDK_VERSION
        targetSdk = LibsVersion.SDK_VERSION
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    namespace = "com.example.sampleapp"
}

sentry {
    autoUploadProguardMapping.set(CI.canAutoUpload())

    tracingInstrumentation {
        enabled.set(false)
    }
}

//tasks.register<CodeSourceExploderTask>("sentrySourceBundle") {
//    javaSourceFiles.setFrom(project.sourceSets.allJava)
//    groovySourceFiles.setFrom(dependencyAnalyzer.groovySourceFiles)
//    dependencyAnalyzer.javaSourceFiles?.let { javaSourceFiles.setFrom(it) }
//    kotlinSourceFiles.setFrom(dependencyAnalyzer.kotlinSourceFiles)
//    scalaSourceFiles.setFrom(dependencyAnalyzer.scalaSourceFiles)
//    output.set(outputPaths.explodedSourcePath)
//}
