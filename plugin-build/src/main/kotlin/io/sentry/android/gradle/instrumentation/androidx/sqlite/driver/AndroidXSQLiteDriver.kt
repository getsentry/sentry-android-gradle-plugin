package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor.SQLiteDriverCallSiteVisitor
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class AndroidXSQLiteDriver : ClassInstrumentable {

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor {
    val currentClassName = instrumentableContext.currentClassData.className
    return CommonClassVisitor(
      apiVersion,
      originalVisitor,
      currentClassName.substringAfterLast('.'),
      METHOD_INSTRUMENTABLES,
      parameters,
    )
  }

  // Broad activation: the setDriver call site can appear in any class. The MethodInstrumentable
  // and MethodVisitor filter at the call-site level. Per-method overhead is one MethodVisitor
  // construction; per-invoke overhead is one visitMethodInsn predicate evaluation.
  override fun isInstrumentable(data: ClassContext): Boolean = !data.isSentryClass()

  companion object {
    // SetDriverCallSiteInstrumentable is stateless, and AndroidXSQLiteDriver runs against every
    // class AGP visits (isInstrumentable returns true). Sharing one immutable list across all
    // class visits avoids two allocations (the instrumentable + the singleton list) per class.
    private val METHOD_INSTRUMENTABLES: List<MethodInstrumentable> =
      listOf(SetDriverCallSiteInstrumentable())
  }
}

class SetDriverCallSiteInstrumentable : MethodInstrumentable {

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor = SQLiteDriverCallSiteVisitor(apiVersion, originalVisitor)

  override fun isInstrumentable(data: MethodContext): Boolean = true
}
