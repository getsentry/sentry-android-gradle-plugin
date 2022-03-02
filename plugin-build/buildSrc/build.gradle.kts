plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir("../../buildSrc/src/main/java")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {

    kotlinOptions {
        languageVersion = "1.5"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
