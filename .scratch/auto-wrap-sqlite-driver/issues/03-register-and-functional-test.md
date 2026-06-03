# Register SQLiteDriver instrumentable in the chain + functional test + CHANGELOG

Status: ready-for-agent
Type: AFK
Blocked by: 01, 02

## Description

Wire the new instrumentable (slice 02) into the instrumentation chain behind the gate (slice 01),
add an end-to-end functional test, and record the change in the CHANGELOG. See
`.scratch/auto-wrap-sqlite-driver/PRD.md` (Implementation Decisions 5–6, Testing Decision 3).

1. `plugin-build/src/main/kotlin/io/sentry/android/gradle/instrumentation/SpanAddingClassVisitorFactory.kt`:
   In the `listOfNotNull(...)` that builds the `ChainedInstrumentable` (the memoized `instrumentable`
   property), add:
   ```kotlin
   AndroidXSQLiteDriver().takeIf { sentryModulesService.isSQLiteDriverInstrEnabled() },
   ```
   Place it alongside the other database instrumentables. No other wiring needed (the property is
   memoized).

2. Functional test — add ONE Gradle TestKit functional test in the `integration` test package,
   mirroring `SentryPluginIntegrationTest`. Build a real sample project (Room 2.7 + a qualifying
   `sentry-android-sqlite`) with a CONCRETE driver call site (e.g. `setDriver(BundledSQLiteDriver())`),
   then assert the injected `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create` bytecode in the
   built output (primary) OR the instrumentation log line (fallback — see the `printLogs`/in-memory
   log plumbing used by `VisitorTest`). This proves the instrumentable is actually added to the chain
   and the gating fires through real AGP.
   - If the sentry-android-sqlite version that ships `SentrySQLiteDriver` is not yet published at
     implementation time (it currently is not — PR #5466 is open), annotate the test `@Ignore` with a
     TODO referencing `VERSION_SENTRY_SQLITE_DRIVER`. NEVER hard-fail CI on an unpublished dependency.
     The unit-level coverage in slices 01–02 is the real safety net.

3. `CHANGELOG.md` — add a `### Features` entry under the Unreleased/next-version heading describing
   the auto-wrapping of `SQLiteDriver` with `SentrySQLiteDriver`, with the PR link placeholder
   (follow the existing `(#NNNN)` convention).

## Acceptance criteria

- [ ] `AndroidXSQLiteDriver` registered in `SpanAddingClassVisitorFactory` behind `isSQLiteDriverInstrEnabled()`.
- [ ] One functional/integration test added asserting the injected `create` bytecode (or log line); `@Ignore`d with a TODO only if the qualifying SDK version is unpublished.
- [ ] CHANGELOG `### Features` entry added.
- [ ] `./gradlew spotlessApply apiDump check` passes.

## Comments
