# Add a SQLiteDriver call site to the instrumentation sample app

Status: ready-for-human
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

**Deferred during Phase 6 (make-it auto), 2026-06.** Implementing this now would knowingly produce a
broken sample:

- The sample (`examples/android-instrumentation-sample`) resolves the latest published
  `sentry-android` (8.43.0), which is `>=` the placeholder `VERSION_SENTRY_SQLITE_DRIVER` (8.43.0).
  With the gate satisfied, the plugin would inject `io.sentry.sqlite.SentrySQLiteDriver.create(...)`
  at a `setDriver(...)` call site — but `SentrySQLiteDriver` does NOT exist in 8.43.0 (sentry-java
  PR #5466 is still open/unmerged). The sample would build but crash at launch (the DB is created in
  `SampleApp`).
- The sample's Room may also predate 2.7 (no `setDriver`), which would be a compile error.

**Unblock condition:** once sentry-java PR #5466 ships and `VERSION_SENTRY_SQLITE_DRIVER` is pinned to
that real release, bump the sample to that SDK version and add a concrete-driver
`Room.databaseBuilder(...).setDriver(BundledSQLiteDriver())` call site (adding
`androidx.sqlite:sqlite-bundled`). Then the injection resolves and the sample becomes a live smoke
test. Until then this is intentionally not implemented. This slice was always marked non-blocking in
the PRD.
