package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import io.sentry.android.gradle.instrumentation.androidx.room.RoomMethodType
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

class InstrumentableMethodsCollectingVisitorTest {

    class Fixture {
        val logger = CapturingTestLogger()
        var nextVisitorInitializer: NextVisitorInitializer = { ClassWriter(0) }

        val sut
            get() = InstrumentableMethodsCollectingVisitor(
                Opcodes.ASM9,
                nextVisitorInitializer,
                logger
            )
    }

    private val fixture: Fixture = Fixture()

    @Test
    fun `visitEnd triggers nextVisitor`() {
        val nextVisitor = NextVisitor()
        fixture.nextVisitorInitializer = { nextVisitor }
        fixture.sut.visitEnd()

        assertTrue { nextVisitor.visited }
    }

    @Test
    fun `if method is not in the lookup - does not add to methodsToInstrument`() {
        lateinit var methodsToInstrument: List<Pair<MethodNode, RoomMethodType>>
        fixture.nextVisitorInitializer = { methodsToInstrument = it; NextVisitor() }

        val sut = fixture.sut
        val mv = sut.visitMethod(0, "test", null, null, null)
        mv.visitMethodInsn(0, "androidx/room/RoomDatabase", "endTransaction", null, false)
        sut.visitEnd()

        assertTrue { methodsToInstrument.isEmpty() }
    }

    @Test
    fun `if method is a transaction - adds to methodsToInstrument with a proper type`() {
        lateinit var methodsToInstrument: List<Pair<MethodNode, RoomMethodType>>
        fixture.nextVisitorInitializer = { methodsToInstrument = it; NextVisitor() }

        val sut = fixture.sut
        val mv = sut.visitMethod(0, "test", null, null, null)
        mv.visitMethodInsn(0, "androidx/room/RoomDatabase", "beginTransaction", null, false)
        sut.visitEnd()

        assertTrue {
            val (node, type) = methodsToInstrument.first()
            node.name == "test" && type == RoomMethodType.TRANSACTION
        }
    }

    @Test
    fun `if method is a query - adds to methodsToInstrument with a proper type`() {
        lateinit var methodsToInstrument: List<Pair<MethodNode, RoomMethodType>>
        fixture.nextVisitorInitializer = { methodsToInstrument = it; NextVisitor() }

        val sut = fixture.sut
        val mv = sut.visitMethod(0, "test", null, null, null)
        mv.visitMethodInsn(0, "androidx/room/util/DBUtil", "query", null, false)
        sut.visitEnd()

        assertTrue {
            val (node, type) = methodsToInstrument.first()
            node.name == "test" && type == RoomMethodType.QUERY
        }
    }

    @Test
    fun `if method is a query with transaction - adds to methodsToInstrument with a proper type`() {
        lateinit var methodsToInstrument: List<Pair<MethodNode, RoomMethodType>>
        fixture.nextVisitorInitializer = { methodsToInstrument = it; NextVisitor() }

        val sut = fixture.sut
        val mv = sut.visitMethod(0, "test", null, null, null)
        mv.visitMethodInsn(0, "androidx/room/util/DBUtil", "query", null, false)
        mv.visitMethodInsn(0, "androidx/room/RoomDatabase", "beginTransaction", null, false)
        sut.visitEnd()

        assertTrue {
            val (node, type) = methodsToInstrument.first()
            node.name == "test" && type == RoomMethodType.QUERY_WITH_TRANSACTION
        }
    }

    @Test
    fun `if method type is unknown - logs message and removes from methodsToInstrument`() {
        lateinit var methodsToInstrument: List<Pair<MethodNode, RoomMethodType>>
        fixture.nextVisitorInitializer = { methodsToInstrument = it; NextVisitor() }

        val sut = fixture.sut
        val mv = sut.visitMethod(0, "unknownFunctionType", null, null, null)
        mv.visitMethodInsn(0, "androidx/room/util/DBUtil", "query", null, false)
        mv.visitMethodInsn(0, "androidx/room/util/DBUtil", "query", null, false)
        sut.visitEnd()

        assertTrue { methodsToInstrument.isEmpty() }
        assertEquals(
            fixture.logger.capturedMessage,
            "[sentry] Unable to identify RoomMethodType, " +
                "skipping unknownFunctionType from instrumentation"
        )
    }
}

class NextVisitor : ClassVisitor(Opcodes.ASM9) {

    var visited: Boolean = false

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        visited = true
        super.visit(version, access, name, signature, superName, interfaces)
    }
}
