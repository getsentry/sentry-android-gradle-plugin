import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(Samples.SpringBoot.springBoot) version
        BuildPluginsVersion.SPRING_BOOT
    id(Samples.SpringBoot.springDependencyManagement) version
        BuildPluginsVersion.SPRING_DEP_MANAGEMENT
    kotlin("jvm")
    kotlin("plugin.spring") version BuildPluginsVersion.KOTLIN
    id("io.sentry.android.gradle")
}

group = "io.sentry.samples.spring-boot"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    implementation(Samples.SpringBoot.springBootStarterSecurity)
    implementation(Samples.SpringBoot.springBootStarterWeb)
    implementation(Samples.SpringBoot.springBootStarterWebflux)
    implementation(Samples.SpringBoot.springBootStarterAop)
    implementation(Samples.SpringBoot.aspectj)
    implementation(Samples.SpringBoot.springBootStarter)
    implementation(Samples.SpringBoot.kotlinReflect)
    implementation(Samples.SpringBoot.springBootStarterJdbc)
    implementation(kotlin(Samples.SpringBoot.kotlinStdLib, KotlinCompilerVersion.VERSION))
    implementation("io.sentry:sentry-spring-boot-starter:6.5.0")
    implementation("io.sentry:sentry-logback:6.5.0")

    // database query tracing
    implementation("io.sentry:sentry-jdbc:6.5.0")
    runtimeOnly(Samples.SpringBoot.hsqldb)
    testImplementation(Samples.SpringBoot.springBootStarterTest) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}
