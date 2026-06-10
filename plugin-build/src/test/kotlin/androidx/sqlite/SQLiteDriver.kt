package androidx.sqlite

/**
 * Minimal stub of `androidx.sqlite.SQLiteDriver` so ASM can resolve the type referenced by the
 * instrumented bytecode.
 *
 * Must not coexist with androidx.sqlite >= 2.5 on the test classpath: that version introduces the
 * real `SQLiteDriver`, which would collide with this stub. Safe today because `libs.sqlite` is
 * pinned below 2.5 and `testImplementationAar` does not pull transitive dependencies (Room 2.7+
 * depends on androidx.sqlite 2.5+, but `Aar2JarPlugin` sets `isTransitive = false`).
 */
interface SQLiteDriver
