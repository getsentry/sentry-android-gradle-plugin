plugins {
  `kotlin-dsl`
  id("java-gradle-plugin")
}

repositories { mavenCentral() }

sourceSets { main { java.srcDir("../../buildSrc/src/main/java") } }

dependencies { compileOnly(libs.gradleApi) }

gradlePlugin {
  plugins {
    register("aar2jarPlugin") {
      id = "io.sentry.android.gradle.aar2jar"
      implementationClass = "io.sentry.android.gradle.internal.Aar2JarPlugin"
    }
  }
}
