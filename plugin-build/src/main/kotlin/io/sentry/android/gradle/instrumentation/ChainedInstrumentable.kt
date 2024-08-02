@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.ClassContext
import java.util.LinkedList
import org.objectweb.asm.ClassVisitor

class ChainedInstrumentable(private val instrumentables: List<ClassInstrumentable> = emptyList()) :
  ClassInstrumentable {

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor {
    // build a chain of visitors in order they are provided
    val queue = LinkedList(instrumentables)
    var prevVisitor = originalVisitor
    var visitor: ClassVisitor? = null
    while (queue.isNotEmpty()) {
      val instrumentable = queue.poll()

      visitor =
        if (instrumentable.isInstrumentable(instrumentableContext)) {
          instrumentable.getVisitor(instrumentableContext, apiVersion, prevVisitor, parameters)
        } else {
          prevVisitor
        }
      prevVisitor = visitor
    }
    return visitor ?: originalVisitor
  }

  override fun isInstrumentable(data: ClassContext): Boolean =
    instrumentables.any { it.isInstrumentable(data) }

  override fun toString(): String {
    return "ChainedInstrumentable(instrumentables=" +
      "${instrumentables.joinToString(", ") { it.javaClass.simpleName }})"
  }
}
