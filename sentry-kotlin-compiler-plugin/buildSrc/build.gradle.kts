plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir("../../buildSrc/src/main/java")
    }
}
