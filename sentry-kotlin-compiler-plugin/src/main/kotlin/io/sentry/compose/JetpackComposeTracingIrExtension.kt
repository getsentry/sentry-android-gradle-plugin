package io.sentry.compose

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
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
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class JetpackComposeTracingIrExtension(
    private val messageCollector: MessageCollector,
    private val enabled: Boolean
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val composableAnnotation = FqName("androidx.compose.runtime.Composable")

        val modifierFqName = FqName("androidx.compose.ui.Modifier")
        val modifierClass = FqName("androidx.compose.ui")
            .classId("Modifier")

        val sentryModifierCompanion = FqName("io.sentry.compose.Modifier")
            .classId("Companion")

        val sentryModifierFunction = FqName("io.sentry.compose.Modifier")
            .classId("Companion")
            .callableId("sentryModifier")

        val modifierAsType = pluginContext.referenceClass(modifierClass)!!.owner.defaultType

        val sentryModifierCompanionRef = pluginContext.referenceClass(sentryModifierCompanion)
        val sentryModifierFunctionRef =
            pluginContext.referenceFunctions(sentryModifierFunction).single()

        val getModifierMap = mutableMapOf<ScopeWithIr, IrGetValue>()

        val transformer = object : IrElementTransformerVoidWithContext() {

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                val isComposable = declaration.symbol.owner.hasAnnotation(composableAnnotation)
                val isAndroidXPackage = currentClass?.let {
                    it.javaClass.packageName.startsWith("androidx")
                } ?: false

                if (isComposable && !isAndroidXPackage) {
                    val body = declaration.body
                    if (body != null) {
                        declaration.body = addSentryModifierToMethodBody(declaration, body)
                    }
                }
                return super.visitFunctionNew(declaration)
            }

            private fun addSentryModifierToMethodBody(function: IrFunction, body: IrBody): IrBody {
                return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
                    val sentryTag =
                        irTemporary(irString(function.name.toString()), nameHint = "sentryTag")


                    val sentryModifier = irTemporary(
                        irCall(sentryModifierFunctionRef, modifierAsType).also { call ->
                            call.dispatchReceiver = irGetObject(sentryModifierCompanionRef!!)
                            call.putValueArgument(0, irGet(sentryTag))
                        },
                        nameHint = "sentryModifier"
                    )
                    // TODO use scopes instead
                    getModifierMap[currentFunction!!] = irGet(sentryModifier)

                    for (statement in body.statements) {
                        +statement
                    }
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression) as IrCall
                val modifierArgumentIndex =
                    call.symbol.owner.valueParameters.indexOfFirst {
                        modifierFqName == it.type.classFqName
                    }
                if (modifierArgumentIndex != -1) {
                    val modifierArgument = call.getValueArgument(modifierArgumentIndex)
                    if (modifierArgument == null) {
                        // modifier is not supplied, simply get our previously defined one
                        getModifierMap[currentFunction]?.let {
                            call.putValueArgument(modifierArgumentIndex, it)
                        }
                    } else {
                        // it's already there, add modifier to expression
                        // TODO implement
                    }
                }
                return call
            }
        }
        moduleFragment.transform(transformer, null)
    }
}


fun FqName.classId(name: String): ClassId {
    return ClassId(this, org.jetbrains.kotlin.name.Name.identifier(name))
}

fun FqName.callableId(name: String): CallableId {
    return CallableId(this, org.jetbrains.kotlin.name.Name.identifier(name))
}

fun ClassId.callableId(name: String): CallableId {
    return CallableId(this, org.jetbrains.kotlin.name.Name.identifier(name))
}
