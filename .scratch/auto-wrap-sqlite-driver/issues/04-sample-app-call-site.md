# Add a SQLiteDriver call site to the instrumentation sample app

Status: ready-for-agent
Type: AFK
Blocked by: 03

## Description

Low-cost, non-blocking: give the feature a real, compiled `setDriver(SQLiteDriver)` call site in the
sample app so it can be smoke-tested manually and so functional tooling has a real instrumentation
target. See `.scratch/auto-wrap-sqlite-driver/PRD.md` (Implementation Decision 7, Out of Scope).

- In `examples/android-instrumentation-sample` (which already uses Room — see `data/TracksDatabase.kt`),
  add a `RoomDatabase.Builder.setDriver(...)` call site using a concrete driver
  (`BundledSQLiteDriver()` from `androidx.sqlite:sqlite-bundled`, or `AndroidSQLiteDriver()` from
  `androidx.sqlite:sqlite-framework`), adding the dependency to that module's build file if needed.
- Keep it minimal and ensure the sample still compiles and builds. This is primarily a manual
  smoke-test affordance.

Caveats (be conservative):
- The plugin only injects `SentrySQLiteDriver.create(...)` when `isSQLiteDriverInstrEnabled()` is true,
  which requires a sentry-android-sqlite version that ships `SentrySQLiteDriver` (not yet published).
  Until then this call site simply will not be wrapped — that is fine; it must still compile/build.
- Do NOT bump the sample's sentry-android-sqlite to an unpublished version. Use whatever the sample
  already resolves. If adding the driver call site cannot be done without an unpublished/unavailable
  dependency, keep the change minimal (or note the blocker in Comments) rather than breaking the build.

## Acceptance criteria

- [ ] The sample app has a concrete `setDriver(...)` call site (or a clearly documented blocker note if a required dependency is unavailable).
- [ ] The sample app still compiles/builds.
- [ ] `./gradlew spotlessApply` passes for any changed files; the relevant sample build task succeeds.

## Comments
