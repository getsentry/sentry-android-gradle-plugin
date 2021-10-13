package io.sentry.android.gradle.instrumentation.classloader

import io.sentry.android.gradle.instrumentation.classloader.mapping.deletionDaoMissingClasses
import io.sentry.android.gradle.instrumentation.classloader.mapping.insertionDaoMissingClasses

class GeneratingMissingClassesClassLoader : ClassLoader(getSystemClassLoader()) {

    companion object {
        private val missingClasses = mapOf(
            *deletionDaoMissingClasses,
            *insertionDaoMissingClasses
        )
    }

    override fun findClass(name: String): Class<*> {
        if (name in missingClasses) {
            val fqName = name.replace('.', '/')
            val source = missingClasses[name]!!.invoke(name)
            val bytes = compileClass(fqName, source)
            return defineClass(name, bytes.toByteArray(), 0, bytes.size())
        }
        return super.findClass(name)
    }
}
