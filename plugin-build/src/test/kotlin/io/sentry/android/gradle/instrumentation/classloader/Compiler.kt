package io.sentry.android.gradle.instrumentation.classloader

import java.io.ByteArrayOutputStream
import java.net.URI
import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.ToolProvider

fun compileClass(fqName: String, source: String): ByteArrayOutputStream {
    val baos = ByteArrayOutputStream()
    val simpleJavaFileObject =
        object : SimpleJavaFileObject(
            URI.create("$fqName.java"),
            JavaFileObject.Kind.SOURCE
        ) {
            override fun getCharContent(ignoreEncodingErrors: Boolean) = source

            override fun openOutputStream() = baos
        }

    val javaFileManager = object : ForwardingJavaFileManager<StandardJavaFileManager>(
        ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)
    ) {
        override fun getJavaFileForOutput(
            location: JavaFileManager.Location?,
            className: String?,
            kind: JavaFileObject.Kind?,
            sibling: FileObject?
        ) = simpleJavaFileObject
    }

    ToolProvider.getSystemJavaCompiler()
        .getTask(null, javaFileManager, null, null, null, listOf(simpleJavaFileObject))
        .call()
    return baos
}
