package io.sentry.compose

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames

class JetpackComposeTracingIrExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {

    companion object {
        private const val SENTRY_BASE_MODIFIER = "sentryBaseModifier"
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val composableAnnotation = FqName("androidx.compose.runtime.Composable")
        val kotlinNothing = FqName("kotlin.Nothing")

        val modifierClassFqName = FqName("androidx.compose.ui.Modifier")
        val modifierCompanionClassFqName = FqName("androidx.compose.ui.Modifier.Companion")
        val modifierCompanionClass = FqName("androidx.compose.ui.Modifier")
            .classId("Companion")

        val modifierClassId = FqName("androidx.compose.ui").classId("Modifier")
        val modifierType = pluginContext.referenceClass(modifierClassId)!!.owner.defaultType
        val modifierCompanionClassRef = pluginContext.referenceClass(modifierCompanionClass)

        if (modifierCompanionClassRef == null) {
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
            private var visitingFunctionNames = ArrayDeque<String>()

            // a stack of the sentryModifiers val getters
            private var visitingFunctionSentryModifier = ArrayDeque<IrGetValue?>()

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

                var modifierAdded = false
                if (isComposable && !isAndroidXPackage && !isSentryPackage) {
                    val body = declaration.body
                    if (body != null) {
                        declaration.body =
                            DeclarationIrBuilder(pluginContext, declaration.symbol)
                                .irBlockBody {
                                    val sentryModifier = irTemporary(
                                        irCall(
                                            sentryModifierTagFunctionRef,
                                            modifierType
                                        ).also { call ->
                                            call.extensionReceiver =
                                                irGetObject(modifierCompanionClassRef)
                                            call.putValueArgument(0, irString(name))
                                        },
                                        nameHint = SENTRY_BASE_MODIFIER
                                    )

                                    visitingFunctionSentryModifier.add(irGet(sentryModifier))
                                    visitingFunctionNames.add(name)
                                    modifierAdded = true

                                    for (statement in body.statements) {
                                        +statement
                                    }
                                }
                    }
                }
                // in case we didn't add a modifier, add an empty modifier to keep the stack in sync
                if (!modifierAdded) {
                    visitingFunctionSentryModifier.add(null)
                    visitingFunctionNames.add(name)
                }
                val irStatement = super.visitFunctionNew(declaration)

                visitingFunctionNames.removeLast()
                visitingFunctionSentryModifier.removeLast()

                return irStatement
            }

            override fun visitCall(expression: IrCall): IrExpression {
                // Case A: modifier is not supplied
                // -> simply set our modifier as param
                // e.g. BasicText(text = "abc")
                // into BasicText(text = "abc", modifier = sentryBaseModifier)
                val modifierArgumentIndex = expression.symbol.owner.valueParameters.indexOfFirst {
                    it.type.classFqName == modifierClassFqName
                }

                if (modifierArgumentIndex != -1) {
                    val modifierArgument: IrExpression? =
                        expression.getValueArgument(modifierArgumentIndex)

                    // we can safely set the sentryModifier if there's no value parameter provided
                    // but in case the Jetpack Compose Compiler plugin runs before us,
                    // it will inject all default value parameters as actual parameters using IrComposite
                    // hence we need to cover this case and overwrite the composite default/null value with sentryModifier
                    // see https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposerParamTransformer.kt;l=287-298;drc=f0b820e062ac34044b43144a87617e90d74657f3
                    val setModifier = (
                        modifierArgument == null ||
                            (
                                modifierArgument is IrComposite &&
                                    modifierArgument.origin == IrStatementOrigin.DEFAULT_VALUE &&
                                    modifierArgument.type.classFqName == kotlinNothing
                                )
                        )

                    if (setModifier) {
                        visitingFunctionSentryModifier.lastOrNull()?.let { sentryModifier ->
                            expression.putValueArgument(modifierArgumentIndex, sentryModifier)
                        }
                    }
                }
                return super.visitCall(expression)
            }

            override fun visitSetValue(expression: IrSetValue): IrExpression {
                if (expression.value is IrGetObjectValue &&
                    expression.value.type.classFqName == modifierCompanionClassFqName
                ) {
                    visitingFunctionSentryModifier.lastOrNull()?.let { sentryModifier ->
                        expression.value = sentryModifier
                    }
                }
                return super.visitSetValue(expression)
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                // Case B: modifier is already supplied
                // -> chain the modifiers
                // e.g. BasicText(text = "abc", modifier = Modifier.fillMaxSize())
                // into BasicText(text = "abc", modifier = sentryBaseModifier.then(Modifier.fillMaxSize())

                val isModifierVariable = declaration.type.classFqName == modifierClassFqName &&
                    !declaration.name.toString().contains(SENTRY_BASE_MODIFIER)

                if (isModifierVariable) {
                    // TODO ensure this also works for field references, global modifiers, etc.
                    if (declaration.initializer is IrCall) {
                        visitingFunctionSentryModifier.lastOrNull()?.let { sentryModifier ->

                            val call = declaration.initializer as IrCall
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
                            wrappedCall.putValueArgument(0, call)
                            wrappedCall.dispatchReceiver = sentryModifier
                            declaration.initializer = wrappedCall
                        }
                    }
                }
                return super.visitVariable(declaration)
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
