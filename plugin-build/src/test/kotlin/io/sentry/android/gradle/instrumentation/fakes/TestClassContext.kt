@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.fakes

import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData

data class TestClassData(
    override val className: String,
    override val classAnnotations: List<String> = emptyList(),
    override val interfaces: List<String> = emptyList(),
    override val superClasses: List<String> = emptyList()
) : ClassData

data class TestClassContext(
    override val currentClassData: ClassData,
    private val classLoader: (String) -> ClassData? = { null }
) : ClassContext {

    constructor(className: String) : this(TestClassData(className))

    constructor(className: String, classLoader: (String) -> ClassData?) :
        this(TestClassData(className), classLoader)

    override fun loadClassData(className: String): ClassData? = classLoader(className)
}
