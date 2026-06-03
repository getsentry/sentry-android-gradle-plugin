# PRD: Auto-wrap SQLiteDriver with SentrySQLiteDriver

Status: ready-for-agent

## Problem Statement

Room 2.7+ lets developers supply a `androidx.sqlite.SQLiteDriver` directly via
`RoomDatabase.Builder.setDriver(SQLiteDriver)`, and Room 3.0 makes the driver API mandatory. That
path bypasses `SupportSQLiteOpenHelper`, which is the only SQLite surface the Sentry Android Gradle
Plugin currently auto-instruments (via `AndroidXSQLiteOpenHelper`). Apps adopting the driver API
therefore silently lose automatic SQL performance spans. sentry-java PR #5466 introduces
`io.sentry.sqlite.SentrySQLiteDriver` (a `SQLiteDriver` wrapper that emits a span per statement),
but it is not auto-applied. We want the plugin to auto-wrap `SQLiteDriver` at the byte-code level,
the same zero-config experience users already get for the open helper.

The hard constraint: **never double-wrap**. AndroidX ships `androidx.sqlite.driver.SupportSQLiteDriver`,
a bridge that adapts an existing (possibly already Sentry-wrapped) `SupportSQLiteOpenHelper` into a
`SQLiteDriver`. Wrapping both the helper and the bridged driver produces duplicate spans for every
SQL statement. The SDK's `SentrySQLiteDriver.create()` is idempotent only for an already-
`SentrySQLiteDriver` delegate; it does **not** detect the bridge case, so avoiding the bridge is the
plugin's responsibility.

## Solution

Add a new ASM `ClassInstrumentable` (`AndroidXSQLiteDriver`) that, across all non-Sentry app classes,
finds `RoomDatabase.Builder.setDriver(SQLiteDriver)` call sites and injects
`io.sentry.sqlite.SentrySQLiteDriver.create(...)` around the **driver argument** (not the driver
construction). It uses ASM `AnalyzerAdapter` to read the argument's static type and:

- **WRAP** when the type is a concrete class assignable to `SQLiteDriver` and not the bridge
  (e.g. `AndroidSQLiteDriver`, `BundledSQLiteDriver`, custom drivers).
- **SKIP** when the type is `androidx/sqlite/driver/SupportSQLiteDriver` (the bridge — the
  no-double-wrap guarantee), `io/sentry/sqlite/SentrySQLiteDriver` (already wrapped), or the bare
  `androidx/sqlite/SQLiteDriver` interface (erased — bias toward a false-negative over a
  double-wrap; SDK idempotency / runtime guards own this case).

Wrapping the argument (interface-typed slot) rather than the constructor is required for type
safety: `SentrySQLiteDriver` is `final` and only implements `SQLiteDriver`, so it is not a subtype
of the final concrete drivers — replacing a driver value held in a concrete-typed local would fail
JVM verification. This was validated by a runnable prototype (`.make-it/prototype/`): the transformed
bytecode loads and runs on a stock JVM across 6 idioms, including the concrete-typed-local case.

The instrumentation is gated by a new `SentryModulesService.isSQLiteDriverInstrEnabled()` — the
`io.sentry:sentry-android-sqlite` artifact at the version that ships `SentrySQLiteDriver`, AND the
existing `InstrumentationFeature.DATABASE` toggle. No new public feature flag.

## User Stories

1. As an Android developer on Room 2.7+ using `setDriver(BundledSQLiteDriver())` with a qualifying
   sentry-android-sqlite version and `InstrumentationFeature.DATABASE` enabled, I get automatic SQL
   spans with zero code changes, because the plugin wraps my driver with `SentrySQLiteDriver`.
2. As a developer using the `SupportSQLiteDriver` bridge over a (Sentry-wrapped) open helper, I get
   exactly one span per SQL statement — the plugin does not wrap the bridge, so no duplicates.
3. As a developer who already calls `SentrySQLiteDriver.create(...)` manually, the plugin does not
   wrap again — no duplicate spans.
4. As a developer on Room < 2.7, an old AGP, or a sentry-android-sqlite version that predates
   `SentrySQLiteDriver`, my build is unchanged — the instrumentation is a no-op and produces
   byte-identical output where it does not apply.

## Implementation Decisions

All paths under `plugin-build/src/main/kotlin/io/sentry/android/gradle/`.

1. **Version constant** — `util/Versions.kt`, in `internal object SentryVersions`: add
   `internal val VERSION_SENTRY_SQLITE_DRIVER = SemVer(8, 43, 0)`. **TODO: pin to the actual
   sentry-java release that ships PR #5466** (PR is still OPEN; latest released is 8.42.0, so the
   real value is a later release — 8.43.0 is a placeholder). Reuse the existing
   `SentryModules.SENTRY_ANDROID_SQLITE` module id.

2. **Gating** — `services/SentryModulesService.kt`: add, mirroring `isNewDatabaseInstrEnabled()`:
   ```kotlin
   fun isSQLiteDriverInstrEnabled(): Boolean =
     sentryModules.isAtLeast(SENTRY_ANDROID_SQLITE, VERSION_SENTRY_SQLITE_DRIVER) &&
       parameters.features.get().contains(InstrumentationFeature.DATABASE)
   ```
   and extend the `InstrumentationFeature.DATABASE` branch of `isInstrumentationEnabled(...)` to OR
   in `isSQLiteDriverInstrEnabled()` so the integration is reported as enabled when only the driver
   path qualifies.

3. **New instrumentable** — `instrumentation/androidx/sqlite/AndroidXSQLiteDriver.kt`:
   - `class AndroidXSQLiteDriver : ClassInstrumentable` with
     `isInstrumentable(ClassContext) = !data.isSentryClass()` (broad targeting, mirroring
     `wrap/WrappingInstrumentable`, since a call site can appear in any user class and `ClassData`
     has no method bodies). `getVisitor(...)` returns a `CommonClassVisitor` whose method
     instrumentable applies the driver visitor to every method.
   - A `MethodInstrumentable` that matches all methods (the per-instruction filtering happens in the
     visitor) — model on how `wrap/Wrap` / `logcat` apply to method bodies broadly.

4. **Method visitor** — `instrumentation/androidx/sqlite/visitor/SQLiteDriverMethodVisitor.kt`:
   purpose-built, **mirroring the structure of `SQLiteOpenHelperMethodVisitor`** (the existing
   open-helper wrapper) but extending ASM `org.objectweb.asm.commons.AnalyzerAdapter` to read the
   operand-stack type of the `setDriver` argument. Do **not** reuse `wrap/visitor/WrappingVisitor`
   (it wraps constructor/return sites, not call arguments, and is depended on by 5 other features).
   Logic (validated by the prototype):
   - Override `visitMethodInsn`. Match owner-agnostically: `name == "setDriver"` and
     `descriptor.startsWith("(Landroidx/sqlite/SQLiteDriver;)")` — covers both
     `androidx/room/RoomDatabase$Builder` (Room 2.7) and `androidx/room3/RoomDatabase$Builder`
     (Room 3.0).
   - Before delegating, read the top-of-stack type from `AnalyzerAdapter.stack`. Apply the WRAP/SKIP
     rule above. On WRAP, emit
     `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create (Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;`
     before the original `setDriver` instruction. Net-zero stack effect → existing frames stay valid.
   - Note `AnalyzerAdapter` is a new ASM pattern in this codebase (no existing usages) — call it out
     in the PR description and rely on the `CheckClassAdapter` verifier test.

5. **Register in the chain** — `instrumentation/SpanAddingClassVisitorFactory.kt`: add
   `AndroidXSQLiteDriver().takeIf { sentryModulesService.isSQLiteDriverInstrEnabled() }` to the
   `listOfNotNull(...)` that builds the `ChainedInstrumentable` (the `instrumentable` is memoized; no
   other wiring needed).

6. **CHANGELOG** — add a `### Features` entry under the Unreleased/next-version heading with the PR
   link (enforced convention).

7. **Sample app (recommended, low cost)** — in `examples/android-instrumentation-sample`
   (`data/TracksDatabase.kt` already uses Room), add a `setDriver(BundledSQLiteDriver())` call site to
   provide a real compiled, instrumented call site and a manual smoke test. Non-blocking.

## Testing Decisions

(Per the testing consultation. The prototype already proved the wrap/skip logic and verifier-safety;
tests are a CI regression fence for the two failure modes: wrap→skip = missed span, skip→wrap =
double span. No span-count assertions at plugin-test level — that's sentry-java runtime, owned by
PR #5466. No new dependency on an unpublished sentry-android-sqlite artifact.)

1. **`VisitorTest` fixtures (highest value)** — add `.class` fixtures under
   `src/test/resources/testFixtures/instrumentation/androidxSqlite/` and params to
   `instrumentation/VisitorTest.kt`. Author the caller fixtures' source (compile + commit the
   `.class`, documenting the regen recipe in a comment) alongside the existing mock-androidx classes
   (`src/test/kotlin/androidx/sqlite/db/SupportSQLiteOpenHelper.kt` neighborhood). Add mock
   `androidx/sqlite/SQLiteDriver`, `androidx/sqlite/driver/SupportSQLiteDriver`,
   `androidx/room/RoomDatabase$Builder` (with `setDriver`), and `io/sentry/sqlite/SentrySQLiteDriver`
   (with `create`) as needed to compile the fixtures. The existing `VisitorTest` only asserts the
   verifier passes — **add an assertion** (a small `ClassReader` + counting `MethodVisitor` helper)
   that the number of injected `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create` calls is
   exactly 0 or 1 per fixture. Verification uses the existing `GeneratingMissingClassesClassLoader`,
   so the real SDK is not required on the classpath.
   - Must-have fixtures (the decision boundary):
     - **WRAP** — inline concrete real driver (`setDriver(new BundledSQLiteDriver())`) → exactly 1
       injected `create`.
     - **SKIP** — inline `SupportSQLiteDriver` bridge → 0 injected (the HARD no-double-wrap test; the
       single most important case).
     - **SKIP** — already-`SentrySQLiteDriver` argument → 0 injected.
     - **SKIP** — bare `SQLiteDriver`-interface (erased) argument → 0 injected.
   - Nice-to-have: concrete-typed local (`SQLiteDriver d = AndroidSQLiteDriver(); setDriver(d)`) →
     exercises the `AnalyzerAdapter` concrete-type read; 1 injected.

2. **`SentryModulesServiceTest` (new file)** — gating truth table for `isSQLiteDriverInstrEnabled()`,
   in the mock-modules-map style of `autoinstall/okhttp/OkHttpInstallStrategyTest`:
   - sqlite >= threshold AND DATABASE on → true
   - sqlite < threshold → false
   - sqlite absent → false
   - DATABASE off (qualifying SDK) → false
   - exactly at threshold version → true (locks `>=` vs `>`)

3. **One Gradle TestKit functional test** — in the `integration` package, mirroring
   `SentryPluginIntegrationTest`. Build a real sample (Room 2.7 + a qualifying sentry-android-sqlite)
   with a **concrete** driver call site; assert the injected `create` bytecode in the built output
   (primary) or the instrumentation log line (fallback). It proves end-to-end wiring/gating through
   real AGP. **`@Ignore` with a TODO** if the `SentrySQLiteDriver`-shipping SDK version is not yet
   published at merge time — never hard-fail CI on an unpublished dependency.

## Out of Scope

- Authoring/modifying `SentrySQLiteDriver` in sentry-java (delivered by PR #5466).
- Wrapping `SQLiteDriver` usage outside Room's `setDriver` (raw KMP/SQLDelight driver usage).
- Kotlin Multiplatform / non-Android targets; `NativeSQLiteDriver` (no Android bytecode).
- Changing or removing the legacy open-helper / old-database instrumentation.
- A new public `InstrumentationFeature` flag.
- Runtime single-span E2E verification (belongs in a sentry-java integration app; file as a separate
  cross-repo QA item).
- User-facing docs (live in `getsentry/sentry-docs`; follow-up PR there).

## Further Notes

- No `CONTEXT.md` / `docs/adr/` exist in this repo — no domain-glossary or ADR constraints apply.
- The chosen design pivots from the idea-spec's original "compile-time guard at call site" framing to
  "wrap the argument, skip the bridge by static type," justified by research (SDK `create()` is only
  driver-level idempotent, the bridge is a distinct class we can simply never target) and confirmed by
  the prototype.
- Version-pinning `VERSION_SENTRY_SQLITE_DRIVER` is the one external dependency that must be
  reconciled at merge time against the actual sentry-java release.

## Comments
