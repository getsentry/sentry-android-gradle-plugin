@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.ClassData
import com.android.build.gradle.internal.instrumentation.ClassContextImpl
import com.android.build.gradle.internal.instrumentation.ClassesDataCache
import com.android.build.gradle.internal.instrumentation.ClassesHierarchyResolver

data class MethodContext(
    val access: Int,
    val name: String?,
    val descriptor: String?,
    val signature: String?,
    val exceptions: List<String>?
)

fun ClassData.toClassContext() =
    ClassContextImpl(this, ClassesHierarchyResolver.Builder(ClassesDataCache()).build())
