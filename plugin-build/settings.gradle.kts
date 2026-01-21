rootProject.name = ("sentry-android-gradle-plugin")

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    mavenLocal()
    exclusiveContent {
      forRepository {
        maven(url = "https://repo.gradle.org/gradle/libs-releases")
      }
      filter {
        includeGroup("org.gradle.experimental")
      }
    }
  }
  versionCatalogs.create("libs") { from(files("../gradle/libs.versions.toml")) }

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
}
