package io.sentry.android.gradle.instrumentation.util

import java.lang.reflect.Field
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

/**
 * Looks up for the original [ClassWriter] up the visitor chain by looking at the private `cv` field
 * of the [ClassVisitor].
 */
internal fun ClassVisitor.findClassWriter(): ClassWriter? {
    var classWriter: ClassVisitor = this
    while (!ClassWriter::class.java.isAssignableFrom(classWriter::class.java)) {
        val cvField: Field = try {
            classWriter::class.java.allFields.find { it.name == "cv" } ?: return null
        } catch (e: Throwable) {
            return null
        }
        cvField.isAccessible = true
        classWriter = (cvField.get(classWriter) as? ClassVisitor) ?: return null
    }
    return classWriter as ClassWriter
}

/**
 * Looks up for [ClassReader] of the [ClassWriter] through intermediate SymbolTable field.
 */
internal fun ClassWriter.findClassReader(): ClassReader? {
    val clazz: Class<out ClassWriter> = this::class.java
    val symbolTableField: Field = try {
        clazz.allFields.find { it.name == "symbolTable" } ?: return null
    } catch (e: Throwable) {
        return null
    }
    symbolTableField.isAccessible = true
    val symbolTable = symbolTableField.get(this)
    val classReaderField: Field = try {
        symbolTable::class.java.getDeclaredField("sourceClassReader")
    } catch (e: Throwable) {
        return null
    }
    classReaderField.isAccessible = true
    return (classReaderField.get(symbolTable) as? ClassReader)
}

internal fun ClassReader.getSimpleClassName(): String {
    return className.substringAfterLast("/")
}

/**
 * Looks at the constant pool entries and searches for R8 markers
 */
internal fun ClassReader.isMinifiedClass(): Boolean {
    return isR8Minified(this) || classNameLooksMinified(this.getSimpleClassName(), this.className)
}

private fun isR8Minified(classReader: ClassReader): Boolean {
    val charBuffer = CharArray(classReader.maxStringLength)
    // R8 marker is usually in the first 3-5 entries, so we limit it at 10 to speed it up
    // (constant pool size can be huge otherwise)
    val poolSize = minOf(10, classReader.itemCount)
    for (i in 1 until poolSize) {
        try {
            val constantPoolEntry = classReader.readConst(i, charBuffer)
            if (constantPoolEntry is String && "~~R8" in constantPoolEntry) {
                // ~~R8 is a marker in the class' constant pool, which r8 itself is looking at when
                // parsing a .class file. See here -> https://r8.googlesource.com/r8/+/refs/heads/main/src/main/java/com/android/tools/r8/dex/Marker.java#53
                return true
            }
        } catch (e: Throwable) {
            // we ignore exceptions here, because some constant pool entries are nulls and the
            // readConst method throws IllegalArgumentException when trying to read those
        }
    }
    return false
}

/**
 * See https://github.com/getsentry/sentry-android-gradle-plugin/issues/360
 * and https://github.com/getsentry/sentry-android-gradle-plugin/issues/359#issuecomment-1193782500
 */
/* ktlint-disable max-line-length */
private val MINIFIED_CLASSNAME_REGEX = """^((([a-zA-z])\3{1,}([0-9]{1,})?(([a-zA-Z])\6{1,})?)|([a-zA-Z]([0-9])?))(${'\\'}${'$'}(((\w)\12{1,}([0-9]{1,})?(([a-zA-Z])\15{1,})?)|(\w([0-9])?)))*${'$'}""".toRegex()

/**
 * See https://github.com/getsentry/sentry/blob/c943de2afc785083554e7fdfb10c67d0c0de0f98/static/app/components/events/eventEntries.tsx#L57-L58
 */
private val MINIFIED_CLASSNAME_SENTRY_REGEX =
    """^(([\w\${'$'}]\/[\w\${'$'}]{1,2})|([\w\${'$'}]{2}\/[\w\${'$'}]\/[\w\${'$'}]))(\/|${'$'})""".toRegex()
/* ktlint-enable max-line-length */

fun classNameLooksMinified(simpleClassName: String, fullClassName: String): Boolean {
    return MINIFIED_CLASSNAME_REGEX.matches(simpleClassName) ||
        MINIFIED_CLASSNAME_SENTRY_REGEX.matches(fullClassName)
}

/**
 * Gets all fields of the given class and its parents (if any).
 *
 * Adapted from https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/reflect/FieldUtils.java
 */
private val Class<*>.allFields: List<Field>
    get() {
        val allFields = mutableListOf<Field>()
        var currentClass: Class<*>? = this
        while (currentClass != null) {
            val declaredFields = currentClass.declaredFields
            allFields += declaredFields
            currentClass = currentClass.superclass
        }
        return allFields
    }
