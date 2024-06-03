package io.sentry.compose

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames

// required only for Kotlin 2.0.0
// @UnsafeDuringIrConstructionAPI
class JetpackComposeTracingIrExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val composableAnnotation = FqName("androidx.compose.runtime.Composable")
        val kotlinNothing = FqName("kotlin.Nothing")

        val modifierClassFqName = FqName("androidx.compose.ui.Modifier")

        val modifierClassId = FqName("androidx.compose.ui").classId("Modifier")
        val modifierType = pluginContext.referenceClass(modifierClassId)!!.owner.defaultType

        val modifierCompanionClass =
            pluginContext.referenceClass(modifierClassId)?.owner?.companionObject()
        val modifierCompanionClassRef = modifierCompanionClass?.symbol

        if (modifierCompanionClass == null || modifierCompanionClassRef == null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "No definition of androidx.compose.ui.Modifier found, " +
                    "Sentry Kotlin Compiler plugin won't run. " +
                    "Please ensure you're applying to plugin to a compose-enabled project."
            )
            return
        }

        val modifierThenRefs = pluginContext.referenceFunctions(modifierClassId.callableId("then"))
        if (modifierThenRefs.isEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "No definition of androidx.compose.ui.Modifier.then() found, " +
                    "Sentry Kotlin Compiler plugin won't run. " +
                    "Please ensure you're applying to plugin to a compose-enabled project."
            )
            return
        } else if (modifierThenRefs.size != 1) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Multiple definitions androidx.compose.ui.Modifier.then() found, " +
                    "which is not supported by Sentry Kotlin Compiler plugin won't run. " +
                    "Please file an issue under " +
                    "https://github.com/getsentry/sentry-android-gradle-plugin"
            )
            return
        }
        val modifierThen = modifierThenRefs.single()

        val sentryModifierTagFunction = FqName("io.sentry.compose")
            .classId("SentryModifier")
            .callableId("sentryTag")

        val sentryModifierTagFunctionRefs = pluginContext
            .referenceFunctions(sentryModifierTagFunction)

        if (sentryModifierTagFunctionRefs.isEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "io.sentry.compose.Modifier.sentryTag() not found, " +
                    "Sentry Kotlin Compiler plugin won't run. " +
                    "Please ensure you're using " +
                    "'io.sentry:sentry-compose-android' as a dependency."
            )
            return
        } else if (sentryModifierTagFunctionRefs.size != 1) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Multiple definitions io.sentry.compose.Modifier.sentryTag() found, " +
                    "Sentry Kotlin Compiler plugin won't run. " +
                    "Please ensure your versions of 'io.sentry:sentry-compose-android' " +
                    "and the sentry Android Gradle plugin match."
            )
            return
        }
        val sentryModifierTagFunctionRef = sentryModifierTagFunctionRefs.single()

        val transformer = object : IrElementTransformerVoidWithContext() {

            // a stack of the function names
            private var visitingFunctionNames = ArrayDeque<String?>()

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                val anonymous = declaration.name == SpecialNames.ANONYMOUS

                // in case of an anonymous, let's try to fallback to it's enclosing function name
                val name = if (!anonymous) declaration.name.toString() else {
                    visitingFunctionNames.lastOrNull() ?: declaration.name.toString()
                }

                val isComposable = declaration.symbol.owner.hasAnnotation(composableAnnotation)

                val isAndroidXPackage = declaration.getPackageFragment().packageFqName.asString()
                    .startsWith("androidx")

                val isSentryPackage = declaration.getPackageFragment().packageFqName.asString()
                    .startsWith("io.sentry.compose")

                if (isComposable && !isAndroidXPackage && !isSentryPackage) {
                    visitingFunctionNames.add(name)
                } else {
                    visitingFunctionNames.add(null)
                }
                val irStatement = super.visitFunctionNew(declaration)

                visitingFunctionNames.removeLast()
                return irStatement
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val composableName = visitingFunctionNames.lastOrNull() ?: return super.visitCall(expression)

                // avoid infinite recursion by instrumenting ourselves
                val dispatchReceiver = expression.dispatchReceiver
                if (dispatchReceiver is IrCall &&
                    dispatchReceiver.symbol == sentryModifierTagFunctionRef
                ) {
                    return super.visitCall(expression)
                }

                for (idx in 0 until expression.symbol.owner.valueParameters.size) {
                    val valueParameter = expression.symbol.owner.valueParameters[idx]
                    if (valueParameter.type.classFqName == modifierClassFqName) {
                        val argument = expression.getValueArgument(idx)
                        expression.putValueArgument(idx, wrapExpression(argument, composableName))
                    }
                }
                return super.visitCall(expression)
            }

            private fun wrapExpression(expression: IrExpression?, composableName: String): IrExpression {

                // Case A: modifier is not supplied
                // -> simply set our modifier as param
                // e.g. BasicText(text = "abc")
                // into BasicText(text = "abc", modifier = Modifier.sentryTag("<composable>")

                // we can safely set the sentryModifier if there's no value parameter provided
                // but in case the Jetpack Compose Compiler plugin runs before us,
                // it will inject all default value parameters as actual parameters using IrComposite
                // hence we need to cover this case and overwrite the composite default/null value with sentryModifier
                // see https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposerParamTransformer.kt;l=287-298;drc=f0b820e062ac34044b43144a87617e90d74657f3

                val overwriteModifier = expression == null ||
                    (expression is IrComposite &&
                        expression.origin == IrStatementOrigin.DEFAULT_VALUE &&
                        expression.type.classFqName == kotlinNothing)


                if (overwriteModifier) {
                    val sentryTagCall = IrCallImpl(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        modifierType,
                        sentryModifierTagFunctionRef,
                        0,
                        1,
                        null,
                        null
                    ).also {
                        it.extensionReceiver = IrGetObjectValueImpl(
                            SYNTHETIC_OFFSET,
                            SYNTHETIC_OFFSET,
                            IrSimpleTypeImpl(
                                modifierCompanionClassRef,
                                false,
                                emptyList(),
                                emptyList()
                            ),
                            modifierCompanionClassRef
                        )
                        it.putValueArgument(
                            0,
                            IrConstImpl.string(
                                SYNTHETIC_OFFSET,
                                SYNTHETIC_OFFSET,
                                pluginContext.irBuiltIns.stringType,
                                composableName
                            )
                        )
                    }
                    return sentryTagCall
                } else {
                    // Case B: modifier is already supplied
                    // -> chain the modifiers
                    // e.g. BasicText(text = "abc", modifier = Modifier.fillMaxSize())
                    // into BasicText(text = "abc", modifier = Modifier.sentryTag("<>").then(Modifier.fillMaxSize())

                    // wrap the call with the sentryTag modifier

                    val sentryTagCall = IrCallImpl(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        modifierType,
                        sentryModifierTagFunctionRef,
                        0,
                        1,
                        null,
                        null
                    ).also {
                        it.extensionReceiver = IrGetObjectValueImpl(
                            SYNTHETIC_OFFSET,
                            SYNTHETIC_OFFSET,
                            IrSimpleTypeImpl(
                                modifierCompanionClassRef,
                                false,
                                emptyList(),
                                emptyList()
                            ),
                            modifierCompanionClassRef
                        )
                        it.putValueArgument(
                            0,
                            IrConstImpl.string(
                                SYNTHETIC_OFFSET,
                                SYNTHETIC_OFFSET,
                                pluginContext.irBuiltIns.stringType,
                                composableName
                            )
                        )
                    }

                    val wrappedCall = IrCallImpl(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        modifierType,
                        modifierThen,
                        0,
                        1,
                        null,
                        null
                    )
                    wrappedCall.putValueArgument(0, expression)
                    wrappedCall.dispatchReceiver = sentryTagCall

                    return wrappedCall
                }
            }
        }

        moduleFragment.transform(transformer, null)
    }
}

fun FqName.classId(name: String): ClassId {
    return ClassId(this, org.jetbrains.kotlin.name.Name.identifier(name))
}

fun ClassId.callableId(name: String): CallableId {
    return CallableId(this, org.jetbrains.kotlin.name.Name.identifier(name))
}
