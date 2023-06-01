package io.sentry.android.gradle.instrumentation.androidx.sqlite.visitor

import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

class SQLiteOpenHelperMethodVisitor(
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    instrumentableContext: MethodContext
) : AdviceAdapter(
    apiVersion,
    originalVisitor,
    instrumentableContext.access,
    instrumentableContext.name,
    instrumentableContext.descriptor
) {

    private val replacement = Replacement(
        "Lio/sentry/android/sqlite/SentrySupportSQLiteOpenHelper;",
        "create",
        "(Landroidx/sqlite/db/SupportSQLiteOpenHelper;)Landroidx/sqlite/db/SupportSQLiteOpenHelper;"
    )

    var alreadyInstrumented = false

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if(
            name == "create" &&
                owner == "androidx/sqlite/db/SupportSQLiteOpenHelper\$Factory" &&
                descriptor == "(Landroidx/sqlite/db/SupportSQLiteOpenHelper\$Configuration;)Landroidx/sqlite/db/SupportSQLiteOpenHelper;"
        ) {
            // We can skip any method which contains a call to
            // SupportSQLiteOpenHelper$Factory.create (SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper
            // This covers the use case of wrappers around an OpenHelper (e.g. SQLiteCopyOpenHelperFactory from Room).
            // It works because we would wrap any delegated open helper anyway.
            alreadyInstrumented = true
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun onMethodExit(opcode: Int) {
        // SupportSQLiteOpenHelper is the return value, thus it's already on top of stack

        if (!alreadyInstrumented) {
            invokeStatic(
                Type.getType(replacement.owner),
                Method(replacement.name, replacement.descriptor)
            )
        }
        super.onMethodExit(opcode)
    }
}
