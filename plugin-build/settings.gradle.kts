rootProject.name = ("sentry-android-gradle-plugin")

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    mavenLocal()
  }
  versionCatalogs.create("libs") { from(files("../gradle/libs.versions.toml")) }

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
}
