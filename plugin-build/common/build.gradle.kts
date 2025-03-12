plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.spotless)
}

dependencies {
    compileOnly(libs.gradleApi)
}

spotless {
    kotlin {
        ktfmt(libs.versions.ktfmt.get()).googleStyle()
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
