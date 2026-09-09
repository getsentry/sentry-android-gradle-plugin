package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import io.sentry.android.gradle.instrumentation.MethodContext
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.objectweb.asm.Opcodes

class SetDriverMethodInstrumentableTest {

  private val instrumentable = SetDriverMethodInstrumentable()

  @Test
  fun `isInstrumentable returns true for setDriver with SQLiteDriver parameter`() {
    assertTrue(
      instrumentable.isInstrumentable(
        methodContext(
          SetDriverMethodInstrumentable.SET_DRIVER,
          "${SetDriverMethodInstrumentable.SET_DRIVER_DESCRIPTOR_PREFIX}Landroidx/room/RoomDatabase\$Builder;",
        )
      )
    )
  }

  @Test
  fun `isInstrumentable returns false for unrelated method names`() {
    assertFalse(instrumentable.isInstrumentable(methodContext("build", "()V")))
  }

  @Test
  fun `isInstrumentable returns false for setDriver with non-SQLiteDriver descriptor`() {
    assertFalse(
      instrumentable.isInstrumentable(
        methodContext(SetDriverMethodInstrumentable.SET_DRIVER, "(Ljava/lang/Object;)V")
      )
    )
  }

  @Test
  fun `isInstrumentable returns false when descriptor is null`() {
    assertFalse(
      instrumentable.isInstrumentable(methodContext(SetDriverMethodInstrumentable.SET_DRIVER, null))
    )
  }

  private fun methodContext(name: String, descriptor: String?) =
    MethodContext(Opcodes.ACC_PUBLIC, name, descriptor, null, null)
}
