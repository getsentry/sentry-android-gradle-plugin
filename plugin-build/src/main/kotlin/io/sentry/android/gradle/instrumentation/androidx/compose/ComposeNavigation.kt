package io.sentry.android.gradle.instrumentation.androidx.compose

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.compose.visitor.RememberNavControllerMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

open class ComposeNavigation : ClassInstrumentable {

  companion object {
    private const val NAV_HOST_CONTROLLER_CLASSNAME =
      "androidx.navigation.compose.NavHostControllerKt"
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
      NAV_HOST_CONTROLLER_CLASSNAME,
      listOf(
        object : MethodInstrumentable {

          override val fqName: String
            get() = "rememberNavController"

          override fun getVisitor(
            instrumentableContext: MethodContext,
            apiVersion: Int,
            originalVisitor: MethodVisitor,
            parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
          ): MethodVisitor {
            return RememberNavControllerMethodVisitor(
              apiVersion,
              originalVisitor,
              instrumentableContext,
            )
          }
        }
      ),
      parameters,
    )
  }

  override fun isInstrumentable(data: ClassContext): Boolean {
    return data.currentClassData.className == NAV_HOST_CONTROLLER_CLASSNAME
  }
}
