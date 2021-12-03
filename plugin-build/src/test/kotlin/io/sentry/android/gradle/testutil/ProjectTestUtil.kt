package io.sentry.android.gradle.testutil

import com.android.build.gradle.AppExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import proguard.gradle.plugin.android.dsl.ProGuardAndroidExtension

fun createTestAndroidProject(
    parent: Project? = null,
    block: AppExtension.() -> Unit = {}
): Pair<Project, AppExtension> {
    val project = ProjectBuilder
        .builder()
        .apply { parent?.let { withParent(parent) } }
        .build()
    project.plugins.apply("com.android.application")
    val appExtension = project.extensions.getByType(AppExtension::class.java).apply {
        compileSdkVersion(30)
        this.block()
    }
    // This will force the project to be evaluated
    project.getTasksByName("assembleDebug", false)
    return project to appExtension
}

fun createTestProguardProject(
    parent: Project? = null,
    block: AppExtension.() -> Unit = {}
): Pair<Project, AppExtension> {
    val project = ProjectBuilder
        .builder()
        .apply { parent?.let { withParent(parent) } }
        .build()
    project.plugins.apply("com.android.application")
    val appExtension = project.extensions.getByType(AppExtension::class.java).apply {
        compileSdkVersion(30)
        this.block()
    }
    project.plugins.apply("com.guardsquare.proguard")
    project.extensions.getByType(ProGuardAndroidExtension::class.java).apply {
        configurations.create("release") {
            it.defaultConfiguration("proguard-android-optimize.txt")
        }
    }
    // This will force the project to be evaluated
    project.getTasksByName("assembleDebug", false)
    return project to appExtension
}
