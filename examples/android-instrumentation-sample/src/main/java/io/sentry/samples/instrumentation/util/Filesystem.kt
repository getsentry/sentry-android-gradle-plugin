package io.sentry.samples.instrumentation.util

import android.content.Context as AndroidContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.Serializable

sealed class Filesystem : Serializable {

  abstract fun read(context: AndroidContext, filename: String): String

  abstract fun write(context: AndroidContext, filename: String, text: String)

  companion object {
    fun from(which: Int) =
      when (which) {
        0 -> IOStream
        1 -> ReaderWriter
        2 -> Context
        else -> error("Unknown File API")
      }
  }

  object ReaderWriter : Filesystem() {

    override fun read(context: AndroidContext, filename: String): String {
      val file = File(context.filesDir, filename)
      return if (file.exists()) FileReader(file).use { it.readText() } else ""
    }

    override fun write(context: AndroidContext, filename: String, text: String) {
      val file = File(context.filesDir, filename)
      if (!file.exists()) {
        file.createNewFile()
      }
      FileWriter(file).use { it.write(text) }
    }
  }

  object IOStream : Filesystem() {
    override fun read(context: AndroidContext, filename: String): String {
      val file = File(context.filesDir, filename)
      return if (file.exists()) file.readText() else ""
    }

    override fun write(context: AndroidContext, filename: String, text: String) {
      val file = File(context.filesDir, filename)
      if (!file.exists()) {
        file.createNewFile()
      }
      file.writeText(text)
    }
  }

  object Context : Filesystem() {
    override fun read(context: AndroidContext, filename: String): String {
      val fis = context.openFileInput(filename)
      return fis.bufferedReader().readText()
    }

    override fun write(context: AndroidContext, filename: String, text: String) {
      val fos = context.openFileOutput(filename, AndroidContext.MODE_PRIVATE)
      fos.bufferedWriter().write(text)
    }
  }
}
