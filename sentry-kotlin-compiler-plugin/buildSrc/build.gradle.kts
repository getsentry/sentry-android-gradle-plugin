plugins {
    kotlin("jvm") version "1.8.20"
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir("../../buildSrc/src/main/java")
    }
}
