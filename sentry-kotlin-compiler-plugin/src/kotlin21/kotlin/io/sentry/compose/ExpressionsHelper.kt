package io.sentry.compose

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

object Kotlin21 {
  fun createIrCall(
    builder: IrBuilderWithScope,
    callee: IrSimpleFunctionSymbol,
    type: IrType,
  ): IrCall = builder.irCall(callee, type = type)
}
