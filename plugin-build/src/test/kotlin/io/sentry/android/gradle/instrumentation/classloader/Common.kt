@file:Suppress("TestFunctionName")

package io.sentry.android.gradle.instrumentation.classloader

fun EntityDeletionOrUpdateAdapter(ofType: String) =
    "androidx.room.EntityDeletionOrUpdateAdapter<$ofType>"

fun SharedSQLiteStatement() = "androidx.room.SharedSQLiteStatement"

fun Callable() = "java.util.concurrent.Callable"

fun standardClassSource(
    name: String,
    superclass: String = "",
    interfaces: Array<String> = emptyArray()
): String {
    val className = name.substringAfterLast('.')
    val path = name.substringBeforeLast('.')

    fun superclassString() = if (superclass.isNotEmpty()) "extends $superclass" else ""
    fun interfacesString() =
        if (interfaces.isNotEmpty()) "implements ${interfaces.joinToString()}" else ""

    // here it's just enough to have an abstract class, as the Java verifier just checks
    // if superclasses/interfaces can be resolved, but omits the implementation details
    return """
        package $path;
        public abstract class $className ${superclassString()} ${interfacesString()} { }
    """.trimIndent()
}
