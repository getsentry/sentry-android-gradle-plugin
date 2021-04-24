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
