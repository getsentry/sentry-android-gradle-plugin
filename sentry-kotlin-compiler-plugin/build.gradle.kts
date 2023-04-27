plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN
    kotlin("kapt")
    // can't use BuildPluginsVersion.MAVEN_PUBLISH here, as coordinates is not available in this version
    id("com.vanniktech.maven.publish") version BuildPluginsVersion.MAVEN_PUBLISH
}

val publish = extensions.getByType(
    com.vanniktech.maven.publish.MavenPublishPluginExtension::class.java
)
// signing is done when uploading files to MC
// via gpg:sign-and-deploy-file (release.kts)
publish.releaseSigningEnabled = false

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    compileOnly(Libs.KOTLIN_COMPILE_EMBEDDABLE)

    kapt(Libs.AUTO_SERVICE)
    compileOnly(Libs.AUTO_SERVICE_ANNOTATIONS)

    testImplementation(kotlin("test-junit"))
    testImplementation(Libs.KOTLIN_COMPILE_EMBEDDABLE)
    testImplementation(Libs.KOTLIN_COMPILE_TESTING)
    testImplementation(Libs.COMPOSE_DESKTOP_RUNTIME)
}

plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "mavenTestRepo"
                url = file("${rootProject.buildDir}/mavenTestRepo").toURI()
            }
        }
    }
}
