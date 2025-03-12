package io.sentry.android.gradle.instrumentation.util

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import org.objectweb.asm.util.Textifier

class FileLogTextifier(apiVersion: Int, log: File, methodName: String?, methodDescriptor: String?) :
  Textifier(apiVersion), ExceptionHandler {

  private var hasThrown = false

  private val fileOutputStream =
    FileOutputStream(log, true).apply {
      write("function $methodName $methodDescriptor".toByteArray())
      write("\n".toByteArray())
    }

  override fun visitMethodEnd() {
    if (!hasThrown) {
      flushPrinter()
    }
  }

  override fun handle(exception: Throwable) {
    hasThrown = true
    flushPrinter()
  }

  private fun flushPrinter() {
    val printWriter = PrintWriter(fileOutputStream)
    print(printWriter)
    printWriter.flush()
    // ASM textifier uses plain "\n" chars, so do we. As it's only for debug and dev purpose
    // it doesn't matter to the end user
    fileOutputStream.write("\n".toByteArray())
    fileOutputStream.close()
  }
}
