package io.sentry.android.gradle.instrumentation.classloader

import io.sentry.android.gradle.instrumentation.classloader.mapping.deletionDaoMissingClasses
import io.sentry.android.gradle.instrumentation.classloader.mapping.insertionDaoMissingClasses
import io.sentry.android.gradle.instrumentation.classloader.mapping.okHttpMissingClasses
import io.sentry.android.gradle.instrumentation.classloader.mapping.selectDaoMissingClasses
import io.sentry.android.gradle.instrumentation.classloader.mapping.sqliteCopyOpenHelperMissingClasses
import io.sentry.android.gradle.instrumentation.classloader.mapping.updateDaoMissingClasses

class GeneratingMissingClassesClassLoader : ClassLoader(getSystemClassLoader()) {

    companion object {
        private val missingClasses = mapOf(
            *deletionDaoMissingClasses,
            *insertionDaoMissingClasses,
            *updateDaoMissingClasses,
            *selectDaoMissingClasses,
            *sqliteCopyOpenHelperMissingClasses,
            *okHttpMissingClasses
        )
    }

    override fun findClass(name: String): Class<*> {
        if (name in missingClasses) {
            return generateClass(name, missingClasses[name]!!.invoke(name))
        }

        return try {
            super.findClass(name)
        } catch (e: ClassNotFoundException) {
            generateClass(name)
        }
    }

    private fun generateClass(
        name: String,
        source: String = standardClassSource(name)
    ): Class<*> {
        val fqName = name.replace('.', '/')
        val bytes = compileClass(fqName, source)
        return defineClass(name, bytes.toByteArray(), 0, bytes.size())
    }
}
