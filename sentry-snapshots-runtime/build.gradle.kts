plugins {
  alias(libs.plugins.kotlin)
  id("distribution")
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
}

spotless {
  kotlin {
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
    target("**/*.kt")
  }
  kotlinGradle {
    target("**/*.kts")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
  }
}

val sep = File.separator

distributions {
  main {
    contents {
      from("build${sep}libs")
      from("build${sep}publications${sep}maven")
    }
  }
}

listOf("distZip", "distTar").forEach { taskName ->
  tasks.named(taskName) {
    dependsOn("publishToMavenLocal")
    onlyIf { inputs.sourceFiles.isEmpty.not().also { require(it) { "No distribution to zip." } } }
  }
}

dependencies { compileOnly(libs.androidxAnnotation) }

plugins.withId("com.vanniktech.maven.publish.base") {
  configure<PublishingExtension> {
    repositories {
      maven {
        name = "mavenTestRepo"
        url = file("${rootProject.projectDir}/../build/mavenTestRepo").toURI()
      }
      maven {
        name = "mavenCentralSnapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        credentials {
          username = findProperty("mavenCentralUsername")?.toString()
          password = findProperty("mavenCentralPassword")?.toString()
        }
      }
    }
  }
}

kotlin { jvmToolchain(11) }
