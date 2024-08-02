plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
}

dependencies {
    compileOnly(Libs.GRADLE_API)
}

spotless {
    kotlin {
        ktfmt(BuildPluginsVersion.KTFMT).googleStyle()
        targetExclude("**/generated/**")
    }
    kotlinGradle {
        ktfmt(BuildPluginsVersion.KTFMT).googleStyle()
        targetExclude("**/generated/**")
    }
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xjvm-default=enable")
        languageVersion = "1.4"
        apiVersion = "1.4"
    }
}
