rootProject.name = ("sentry-android-gradle-plugin")

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    mavenLocal()
  }

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
}

include(":common")
