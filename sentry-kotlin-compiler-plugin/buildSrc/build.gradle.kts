plugins {
    kotlin("jvm") version "1.6.10"
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
