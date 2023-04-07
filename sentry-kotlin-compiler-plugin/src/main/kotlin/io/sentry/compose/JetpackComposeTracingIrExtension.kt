package io.sentry.compose

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
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
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
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

class JetpackComposeTracingIrExtension(
    private val messageCollector: MessageCollector,
    private val enabled: Boolean
) : IrGenerationExtension {

    companion object {
        private const val SENTRY_BASE_MODIFIER = "sentryBaseModifier"
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val composableAnnotation = FqName("androidx.compose.runtime.Composable")

        val modifierClassFqName = FqName("androidx.compose.ui.Modifier")
        val modifierClassId = FqName("androidx.compose.ui").classId("Modifier")
        val modifierType = pluginContext.referenceClass(modifierClassId)!!.owner.defaultType
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

        val sentryModifierClassCompanion = FqName("io.sentry.compose.Modifier")
            .classId("Companion")

        val sentryModifierFunction = FqName("io.sentry.compose.Modifier")
            .classId("Companion")
            .callableId("sentryModifier")

        val sentryModifierCompanionRef = pluginContext.referenceClass(sentryModifierClassCompanion)
        val sentryModifierFunctionRefs = pluginContext.referenceFunctions(sentryModifierFunction)

        if (sentryModifierFunctionRefs.isEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "io.sentry.compose.Modifier.sentryModifier() not found, " +
                    "Sentry Kotlin Compiler plugin won't run. " +
                    "Please ensure you're using the " +
                    "'io.sentry:sentry-compose-android' is defined as a dependency."
            )
            return
        } else if (sentryModifierFunctionRefs.size != 1) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Multiple definitions io.sentry.compose.Modifier.sentryModifier() found, " +
                    "Sentry Kotlin Compiler plugin won't run. " +
                    "Please ensure your versions of 'io.sentry:sentry-compose-android' " +
                    "and the sentry Android Gradle plugin match."
            )
            return
        }

        val sentryModifierFunctionRef = sentryModifierFunctionRefs.single()

        // keeps track of sentryBaseModifier per function
        val getModifierMap = mutableMapOf<ScopeWithIr, IrGetValue>()

        val transformer = object : IrElementTransformerVoidWithContext() {

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                val isComposable = declaration.symbol.owner.hasAnnotation(composableAnnotation)

                val isAndroidXPackage = declaration.getPackageFragment().fqName.asString()
                    .startsWith("androidx")

                val isSentryPackage = declaration.getPackageFragment().fqName.asString()
                    .startsWith("io.sentry.compose")

                if (isComposable && !isAndroidXPackage && !isSentryPackage) {
                    val body = declaration.body
                    if (body != null) {
                        declaration.body = addSentryModifierToMethodBody(declaration, body)
                    }
                }
                return super.visitFunctionNew(declaration)
            }

            private fun addSentryModifierToMethodBody(function: IrFunction, body: IrBody): IrBody {
                return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
                    val sentryModifier = irTemporary(
                        irCall(sentryModifierFunctionRef, modifierType).also { call ->
                            call.dispatchReceiver = irGetObject(sentryModifierCompanionRef!!)
                            call.putValueArgument(0, irString(function.name.toString()))
                        },
                        nameHint = SENTRY_BASE_MODIFIER
                    )
                    // TODO use scopes instead
                    // or a simple queue https://github.com/JakeWharton/cite/blob/dc310bb6115df7be290ed6cd68ff7c182bcccbf2/cite-kotlin-plugin/src/main/kotlin/com/jakewharton/cite/plugin/kotlin/CiteElementTransformer.kt#L53
                    getModifierMap[currentFunction!!] = irGet(sentryModifier)

                    for (statement in body.statements) {
                        +statement
                    }
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression) as IrCall

                // Case A: modifier is not supplied
                // -> simply set our modifier as param
                // e.g. BasicText(text = "abc")
                // into BasicText(text = "abc", modifier = sentryBaseModifier)
                val modifierArgumentIndex =
                    call.symbol.owner.valueParameters.indexOfFirst {
                        modifierClassFqName == it.type.classFqName
                    }
                if (modifierArgumentIndex != -1) {
                    val modifierArgument = call.getValueArgument(modifierArgumentIndex)
                    if (modifierArgument == null) {
                        getModifierMap[currentFunction]?.let {
                            call.putValueArgument(modifierArgumentIndex, it)
                        }
                    }
                }
                return call
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                // Case B: modifier is already supplied
                // -> chain the modifiers
                // e.g. BasicText(text = "abc", modifier = Modifier.fillMaxSize())
                // into BasicText(text = "abc", modifier = sentryBaseModifier.then(Modifier.fillMaxSize())
                if (declaration.type.classFqName == modifierClassFqName &&
                    !declaration.name.toString().contains(SENTRY_BASE_MODIFIER)
                ) {
                    // TODO ensure this also works for field references, global modifiers, etc.
                    if (declaration.initializer is IrCall) {
                        val sentryBaseModifierVal = getModifierMap[currentFunction]
                        sentryBaseModifierVal?.let { sentryBaseModifier ->
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
                            wrappedCall.dispatchReceiver = sentryBaseModifier
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
