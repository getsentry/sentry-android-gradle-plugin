package io.sentry.android.gradle.instrumentation.androidx.navigation3

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.navigation3.visitor.RememberDecoratedNavEntriesRedirectingVisitor
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.warn
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

/**
 * Instruments Navigation 3's back stack `NavDisplay` so it installs Sentry's navigation observer.
 *
 * The back stack overload of `NavDisplay` turns its back stack into entries with exactly one call
 * to `rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)`. This instrumentable
 * redirects that call to `SentryNavAutoInstrumentation.rememberSentryDecoratedNavEntries` in
 * sentry-android-navigation3, which has the same compiled signature: it observes the back stack and
 * then calls the original function. Only the call's owner and name change; its arguments, return
 * value, and Compose bookkeeping stay as they are.
 *
 * The call is an implementation detail of `NavDisplay`, unchanged from navigation3-ui 1.0.0 through
 * at least 1.3.0-alpha03. If a future release drops it, nothing is rewritten and a warning is
 * logged, so the build never produces broken bytecode.
 *
 * The `NavDisplay(entries = ...)` and `NavDisplay(sceneState = ...)` overloads never reach this
 * call and aren't instrumented.
 */
class Navigation3Display : ClassInstrumentable {

  override val fqName: String
    get() = NAV_DISPLAY_CLASSNAME

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor {
    var redirectedCalls = 0
    val commonVisitor =
      CommonClassVisitor(
        apiVersion,
        originalVisitor,
        NAV_DISPLAY_CLASSNAME,
        listOf(
          object : MethodInstrumentable {
            // The call lives in a NavDisplay method today, but scan the whole class so a move to a
            // helper method inside it doesn't silently disable the instrumentation.
            override fun isInstrumentable(data: MethodContext): Boolean = true

            override fun getVisitor(
              instrumentableContext: MethodContext,
              apiVersion: Int,
              originalVisitor: MethodVisitor,
              parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
            ): MethodVisitor =
              RememberDecoratedNavEntriesRedirectingVisitor(apiVersion, originalVisitor) {
                redirectedCalls++
              }
          }
        ),
        parameters,
      )

    return object : ClassVisitor(apiVersion, commonVisitor) {
      override fun visitEnd() {
        super.visitEnd()
        if (redirectedCalls == 0) {
          SentryPlugin.logger.warn {
            "Navigation 3 auto-instrumentation skipped: $NAV_DISPLAY_CLASSNAME has no " +
              "rememberDecoratedNavEntries call to instrument, likely because this " +
              "navigation3-ui version is not supported yet. Call SentryNavEffect manually instead."
          }
        } else {
          SentryPlugin.logger.info {
            "Instrumented $redirectedCalls rememberDecoratedNavEntries call(s) in NavDisplay."
          }
        }
      }
    }
  }

  override fun isInstrumentable(data: ClassContext): Boolean =
    data.currentClassData.className == NAV_DISPLAY_CLASSNAME

  companion object {
    /** The multi-file facade part that holds `NavDisplay`'s implementation. */
    internal const val NAV_DISPLAY_CLASSNAME = "androidx.navigation3.ui.NavDisplayKt__NavDisplayKt"
  }
}
