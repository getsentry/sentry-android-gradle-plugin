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

/**
 * Looks at the constant pool entries and searches for R8 markers
 */
internal fun ClassReader.isMinifiedClass(): Boolean {
    val charBuffer = CharArray(maxStringLength)
    // R8 marker is usually in the first 3-5 entries, so we limit it at 10 to speed it up
    // (constant pool size can be huge otherwise)
    val poolSize = minOf(10, itemCount)
    for (i in 1 until poolSize) {
        try {
            val constantPoolEntry = readConst(i, charBuffer)
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
