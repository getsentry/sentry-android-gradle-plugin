package io.sentry.android.gradle.instrumentation.binder

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import org.objectweb.asm.ClassVisitor

class Binder : ClassInstrumentable {

  companion object {
    private const val CLASSNAME = "Binder"
  }

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor {
    return CommonClassVisitor(
      apiVersion,
      originalVisitor,
      CLASSNAME,
      listOf(BinderMethodInstrumentable()),
      parameters,
    )
  }

  override fun isInstrumentable(data: ClassContext) = true // !data.isSentryClass()
}
