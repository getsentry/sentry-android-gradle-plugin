package io.sentry.android.gradle.instrumentation.androidx.navigation3.visitor

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Redirects calls to `androidx.navigation3.runtime.rememberDecoratedNavEntries(backStack,
 * entryDecorators, entryProvider)` to Sentry's wrapper with the same compiled signature.
 *
 * Calls match on owner, name, and descriptor exactly. The other `rememberDecoratedNavEntries`
 * overload, which re-decorates existing entries, has a different descriptor and is left alone.
 *
 * @param onRedirect invoked once for every redirected call.
 */
class RememberDecoratedNavEntriesRedirectingVisitor(
  apiVersion: Int,
  originalVisitor: MethodVisitor,
  private val onRedirect: () -> Unit,
) : MethodVisitor(apiVersion, originalVisitor) {

  override fun visitMethodInsn(
    opcode: Int,
    owner: String,
    name: String,
    descriptor: String,
    isInterface: Boolean,
  ) {
    if (
      opcode == Opcodes.INVOKESTATIC &&
        owner == ORIGINAL_OWNER &&
        name == ORIGINAL_NAME &&
        descriptor == DESCRIPTOR
    ) {
      onRedirect()
      super.visitMethodInsn(opcode, REPLACEMENT_OWNER, REPLACEMENT_NAME, descriptor, false)
    } else {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
  }

  companion object {
    internal const val ORIGINAL_OWNER = "androidx/navigation3/runtime/DecoratedNavEntriesKt"
    internal const val ORIGINAL_NAME = "rememberDecoratedNavEntries"

    /** `(backStack, entryDecorators, entryProvider, $composer, $changed, $default) -> entries` */
    internal const val DESCRIPTOR =
      "(Ljava/util/List;Ljava/util/List;Lkotlin/jvm/functions/Function1;" +
        "Landroidx/compose/runtime/Composer;II)Ljava/util/List;"

    internal const val REPLACEMENT_OWNER =
      "io/sentry/compose/navigation3/SentryNavAutoInstrumentation"
    internal const val REPLACEMENT_NAME = "rememberSentryDecoratedNavEntries"
  }
}
