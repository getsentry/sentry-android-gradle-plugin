plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN
    id("org.jlleitschuh.gradle.ktlint") version BuildPluginsVersion.KTLINT
    id("com.vanniktech.maven.publish") version BuildPluginsVersion.MAVEN_PUBLISH apply false
}

dependencies {
    compileOnly(Libs.GRADLE_API)
}

ktlint {
    debug.set(false)
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
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
//val sep = File.separator
//
//distributions {
//    main {
//        contents {
//            from("build${sep}libs")
//            from("build${sep}publications${sep}maven")
//        }
//    }
//}
//
//apply {
//    plugin("com.vanniktech.maven.publish")
//}
//
//val publish = extensions.getByType(MavenPublishPluginExtension::class.java)
//// signing is done when uploading files to MC
//// via gpg:sign-and-deploy-file (release.kts)
//publish.releaseSigningEnabled = false
//
//tasks.named("distZip") {
//    dependsOn("publishToMavenLocal")
//    onlyIf {
//        inputs.sourceFiles.isEmpty.not().also {
//            require(it) { "No distribution to zip." }
//        }
//    }
//}
