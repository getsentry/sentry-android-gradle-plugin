plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir("../../buildSrc/src/main/java")
    }
}


// we cannot use the version from Dependencies.kt because it's not available at this moment, the
// proper way to share the dependency notation is to use libs.versions.toml, but that's for later
dependencies {
    compileOnly("dev.gradleplugins:gradle-api:7.6")
}

gradlePlugin {
    plugins {
        register("aar2jarPlugin") {
            id = "io.sentry.android.gradle.aar2jar"
            implementationClass = "io.sentry.android.gradle.internal.Aar2JarPlugin"
        }
    }
}
