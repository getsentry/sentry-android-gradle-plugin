# Add gating for SQLiteDriver instrumentation

Status: done
Type: AFK

## Description

Add the version constant and the `SentryModulesService` gating that decides whether the new
`SQLiteDriver` auto-wrapping instrumentation should run. No instrumentation behavior is added in this
slice — it is the foundation that slice 03 wires up. See `.scratch/auto-wrap-sqlite-driver/PRD.md`
(Implementation Decisions 1–2, Testing Decision 2).

Changes (all under `plugin-build/src/main/kotlin/io/sentry/android/gradle/`):

1. `util/Versions.kt` — in `internal object SentryVersions`, add:
   ```kotlin
   internal val VERSION_SENTRY_SQLITE_DRIVER = SemVer(8, 43, 0)
   ```
   Add a `// TODO` comment: pin to the actual sentry-java release that ships `SentrySQLiteDriver`
   (PR getsentry/sentry-java#5466 — still open; latest released is 8.42.0, so 8.43.0 is a
   placeholder). Reuse the existing `SentryModules.SENTRY_ANDROID_SQLITE` module id (already defined).

2. `services/SentryModulesService.kt` — add, mirroring `isNewDatabaseInstrEnabled()`:
   ```kotlin
   fun isSQLiteDriverInstrEnabled(): Boolean =
     sentryModules.isAtLeast(SentryModules.SENTRY_ANDROID_SQLITE, SentryVersions.VERSION_SENTRY_SQLITE_DRIVER) &&
       parameters.features.get().contains(InstrumentationFeature.DATABASE)
   ```
   Then extend the `InstrumentationFeature.DATABASE` branch of `isInstrumentationEnabled(...)` to OR
   in the driver path, so the integration is reported enabled when only the driver path qualifies:
   ```kotlin
   InstrumentationFeature.DATABASE ->
     isOldDatabaseInstrEnabled() || isNewDatabaseInstrEnabled() || isSQLiteDriverInstrEnabled()
   ```

3. Tests — add a new `plugin-build/src/test/kotlin/io/sentry/android/gradle/services/SentryModulesServiceTest.kt`
   (no such file exists yet). Write a truth-table unit test for `isSQLiteDriverInstrEnabled()` in the
   mock-modules-map style used by `autoinstall/okhttp/OkHttpInstallStrategyTest` (register/obtain a
   `SentryModulesService`, set `sentryModules`, and the `features` parameter). Cover:
   - sentry-android-sqlite version >= `VERSION_SENTRY_SQLITE_DRIVER` AND `DATABASE` feature → `true`
   - version < threshold → `false`
   - module absent → `false`
   - `DATABASE` feature off (with a qualifying version) → `false`
   - version exactly equal to threshold → `true` (locks the `>=` semantics)

Look at how existing tests construct/register `SentryModulesService` and set `parameters.features`
(grep the test sources) and follow that idiom exactly so the test runs under the project harness.

## Acceptance criteria

- [ ] `SentryVersions.VERSION_SENTRY_SQLITE_DRIVER` exists with the TODO comment.
- [ ] `SentryModulesService.isSQLiteDriverInstrEnabled()` implemented and `isInstrumentationEnabled(DATABASE)` ORs it in.
- [ ] New `SentryModulesServiceTest` covers the 5 truth-table rows above and passes.
- [ ] `./gradlew spotlessApply apiDump check` passes (apiDump updated if the new public method changes the API file).

## Comments
