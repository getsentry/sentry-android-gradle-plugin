// ktlint-disable max-line-length
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinSpring)
    id("io.sentry.jvm.gradle")
}

group = "io.sentry.samples.spring-boot"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.sample.springBoot.starterSecurity)
    implementation(libs.sample.springBoot.starterWeb)
    implementation(libs.sample.springBoot.starterWebflux)
    implementation(libs.sample.springBoot.starterAop)
    implementation(libs.sample.springBoot.aspectj)
    implementation(libs.sample.springBoot.starter)
    implementation(libs.sample.springBoot.kotlinReflect)
    implementation(libs.sample.springBoot.starterJdbc)
    implementation(kotlin(Samples.SpringBoot.kotlinStdLib, KotlinCompilerVersion.VERSION))

    runtimeOnly(libs.sample.springBoot.hsqldb)
    testImplementation(libs.sample.springBoot.starterTest) {
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

sentry {
    debug.set(true)
    includeSourceContext.set(true)
    additionalSourceDirsForSourceContext.set(setOf("testsrc"))
}
