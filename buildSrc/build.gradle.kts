import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        languageVersion = BuildPluginsVersion.KOTLIN_LANG_VERSION
    }
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
