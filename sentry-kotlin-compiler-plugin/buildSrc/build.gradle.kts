plugins {
    kotlin("jvm") version "1.8.20"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

sourceSets {
    main {
        java.srcDir("../../buildSrc/src/main/java")
    }
}
