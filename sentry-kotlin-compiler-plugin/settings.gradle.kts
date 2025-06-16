rootProject.name = ("sentry-kotlin-compiler-plugin")

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
  versionCatalogs.create("libs") { from(files("../gradle/libs.versions.toml")) }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}
