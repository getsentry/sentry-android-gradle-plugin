rootProject.name = ("sentry-snapshots-runtime")

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
  versionCatalogs.create("libs") { from(files("../gradle/libs.versions.toml")) }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}
