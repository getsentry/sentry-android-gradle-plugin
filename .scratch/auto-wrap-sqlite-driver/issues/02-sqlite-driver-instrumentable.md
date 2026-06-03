# Implement SQLiteDriver call-site wrapping instrumentable + visitor

Status: done
Type: AFK

## Description

Implement the core ASM transform: across all non-Sentry app classes, find
`RoomDatabase.Builder.setDriver(SQLiteDriver)` call sites and wrap the driver **argument** with
`io.sentry.sqlite.SentrySQLiteDriver.create(...)`, skipping the `SupportSQLiteDriver` bridge (the
no-double-wrap guarantee), already-`SentrySQLiteDriver` args, and bare-interface (erased) args. This
slice is independently unit-testable via `VisitorTest` and does NOT require gating (slice 01) or
chain registration (slice 03). See `.scratch/auto-wrap-sqlite-driver/PRD.md` (Implementation
Decisions 3–4, Testing Decision 1).

A runnable, validated reference prototype lives at `.make-it/prototype/` — read
`.make-it/prototype/NOTES.md` and `.make-it/prototype/transformer/proto/DriverWrapTransformer.java`.
The visitor logic should match that prototype's decision rule. Also read `.make-it/RESEARCH.md` for
confirmed signatures.

Files (under `plugin-build/src/main/kotlin/io/sentry/android/gradle/`):

1. `instrumentation/androidx/sqlite/AndroidXSQLiteDriver.kt`:
   - `class AndroidXSQLiteDriver : ClassInstrumentable` with
     `isInstrumentable(ClassContext) = !data.isSentryClass()` (broad targeting — a `setDriver` call
     can appear in any user class; `ClassData` has no method bodies). Mirror `wrap/WrappingInstrumentable`.
   - `getVisitor(...)` returns a `CommonClassVisitor` whose `MethodInstrumentable` applies the driver
     visitor to method bodies (the per-instruction filtering happens in the visitor; the method
     instrumentable can match broadly). Mirror the structure of `AndroidXSQLiteOpenHelper.kt`.

2. `instrumentation/androidx/sqlite/visitor/SQLiteDriverMethodVisitor.kt`:
   - Purpose-built, modeled on `visitor/SQLiteOpenHelperMethodVisitor.kt` but extending ASM
     `org.objectweb.asm.commons.AnalyzerAdapter` (a new ASM pattern in this codebase — note it in the
     PR description; lean on the verifier test). DO NOT reuse `wrap/visitor/WrappingVisitor` (it wraps
     constructor/return sites, not call arguments, and is shared by 5 other features).
   - Override `visitMethodInsn`. Match owner-agnostically: `name == "setDriver"` and
     `descriptor.startsWith("(Landroidx/sqlite/SQLiteDriver;)")` (covers Room 2.7
     `androidx/room/RoomDatabase$Builder` and Room 3.0 `androidx/room3/RoomDatabase$Builder`).
   - Before delegating the matched call, read the top-of-stack type from `AnalyzerAdapter.stack`:
     - SKIP (emit nothing extra) if the type is `androidx/sqlite/driver/SupportSQLiteDriver` (bridge),
       `io/sentry/sqlite/SentrySQLiteDriver` (already wrapped), or `androidx/sqlite/SQLiteDriver`
       (bare interface / erased).
     - Otherwise WRAP: emit
       `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create (Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;`
       before the original `setDriver` instruction.
   - Net stack effect of the injection is zero (pop one `SQLiteDriver`, push one), so existing stack
     frames remain valid — `CheckClassAdapter` must pass.

3. Tests — `VisitorTest` fixtures + assertion:
   - Add the mock classes needed to compile the fixtures alongside the existing mocks
     (`plugin-build/src/test/kotlin/androidx/sqlite/db/SupportSQLiteOpenHelper.kt` neighborhood):
     `androidx/sqlite/SQLiteDriver` (interface), `androidx/sqlite/driver/AndroidSQLiteDriver` and/or
     `androidx/sqlite/driver/bundled/BundledSQLiteDriver`, `androidx/sqlite/driver/SupportSQLiteDriver`,
     `androidx/room/RoomDatabase` (with nested `Builder.setDriver(SQLiteDriver): Builder`), and
     `io/sentry/sqlite/SentrySQLiteDriver` (with static `create(SQLiteDriver): SQLiteDriver`).
   - Author caller fixtures, compile them, and commit the resulting `.class` files under
     `plugin-build/src/test/resources/testFixtures/instrumentation/androidxSqlite/`. Document the
     regen recipe in a comment in the fixture source. Follow exactly how the existing
     `androidxSqlite` fixtures are wired into `instrumentation/VisitorTest.kt` (`parameters()` list).
   - The existing `VisitorTest` only asserts the verifier passes. ADD an assertion (a small
     `ClassReader` + counting `MethodVisitor` helper, or a new test method) that the number of
     injected `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create` calls equals the expected
     0 or 1 per fixture. Verification uses the existing `GeneratingMissingClassesClassLoader`, so the
     real SDK artifact is NOT needed on the classpath.
   - Must-have fixture cases: WRAP inline concrete driver (1 injected); SKIP inline `SupportSQLiteDriver`
     bridge (0); SKIP already-`SentrySQLiteDriver` (0); SKIP bare-`SQLiteDriver`-interface arg (0).
   - Nice-to-have: concrete-typed local (`SQLiteDriver d = AndroidSQLiteDriver(); setDriver(d)`) → 1
     injected (exercises the `AnalyzerAdapter` concrete-type read).

## Acceptance criteria

- [ ] `AndroidXSQLiteDriver` (ClassInstrumentable) + `SQLiteDriverMethodVisitor` (AnalyzerAdapter-based) implemented per the rule above.
- [ ] Mock androidx/room/sentry classes + committed `.class` fixtures added under the existing `androidxSqlite` test paths.
- [ ] `VisitorTest` parameterized over the new fixtures AND asserts injected-`create` count (0/1) per fixture, including the must-have WRAP + 3 SKIP cases. Bridge-skip case present (the hard no-double-wrap guarantee).
- [ ] All new/modified fixtures pass `CheckClassAdapter` verification.
- [ ] `./gradlew spotlessApply apiDump check` passes.

## Comments
