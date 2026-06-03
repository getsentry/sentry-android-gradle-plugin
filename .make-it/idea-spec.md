# Idea Spec

## Problem
Room 2.7+ allows supplying a `SQLiteDriver` directly via `RoomDatabase.Builder.setDriver(SQLiteDriver)`, bypassing the `SupportSQLiteOpenHelper` path that the Sentry Android Gradle Plugin currently auto-instruments. Apps that adopt the new driver-based API therefore lose automatic SQL span/performance tracing, and there is no zero-config way to wrap a `SQLiteDriver` with `SentrySQLiteDriver` (introduced in sentry-java PR #5466). The plugin must close this gap via bytecode transformation while never producing duplicate spans for a single database — the central hazard being `androidx.sqlite.driver.SupportSQLiteDriver`, the AndroidX adapter that bridges an existing (possibly already Sentry-wrapped) `SupportSQLiteOpenHelper` into a `SQLiteDriver`.

## Target Users
Android app developers using the Sentry Android Gradle Plugin who:
- Have migrated (or are migrating) to Room 2.7+ and supply a `SQLiteDriver` via `setDriver(...)`.
- Want automatic, zero-config SQL performance instrumentation with no manual SDK wiring.
- May still be using the legacy `SupportSQLiteOpenHelper` path, or a hybrid (bridge) setup, and must not be double-instrumented.

## Solution Overview
Add a new `ClassInstrumentable` (proposed `AndroidXSQLiteDriver`) to the existing chain-of-responsibility in `SpanAddingClassVisitorFactory`. It detects bytecode call sites that pass a `SQLiteDriver` into `RoomDatabase.Builder.setDriver(...)` and injects a static call wrapping the argument with `io.sentry.android.sqlite.SentrySQLiteDriver.create(SQLiteDriver)` (exact factory name/signature pending research from PR #5466) on the operand stack before the `setDriver` invocation.

Injection point: a `MethodVisitor` (AdviceAdapter-style) that intercepts the `INVOKE...` to `androidx/room/RoomDatabase$Builder.setDriver(Landroidx/sqlite/SQLiteDriver;)...` and rewrites the driver argument in place, mirroring the existing `SQLiteOpenHelperMethodVisitor` injection pattern but at the call site rather than the factory return.

Double-wrap prevention (the hard constraint) uses a layered strategy:
1. **Runtime/SDK-level idempotency (primary):** `SentrySQLiteDriver.create(...)` is expected to be idempotent — if the supplied driver is already a `SentrySQLiteDriver`, OR is an `androidx.sqlite.driver.SupportSQLiteDriver` bridging an already-`SentrySupportSQLiteOpenHelper`-wrapped helper, it returns it unwrapped / detects the existing Sentry wrapping and does not add a second span layer. This is the authoritative guard because the bridge case is only fully knowable at runtime. (Confirmation of this behavior is a research dependency on PR #5466.)
2. **Compile-time guard at the call site:** the method visitor sets an `alreadyInstrumented`-style flag if the same method already constructs the driver via the `SupportSQLiteDriver` bridge from a helper, mirroring the existing `alreadyInstrumented` guard in `SQLiteOpenHelperMethodVisitor`, to avoid wrapping a driver that the plugin can statically see is bridge-derived.
3. The open-helper path and the driver path may both be enabled simultaneously under the single `InstrumentationFeature.DATABASE` flag; they instrument disjoint call sites (`SupportSQLiteOpenHelper$Factory.create` return vs `RoomDatabase.Builder.setDriver` argument). The only overlap is the bridge, handled by guards (1) and (2).

Gating: a new `isSQLiteDriverInstrEnabled()` in `SentryModulesService`, requiring `io.sentry:sentry-android-sqlite >= <VERSION_SENTRY_SQLITE_DRIVER>` (exact version TBD from PR #5466 release) AND `InstrumentationFeature.DATABASE`. This reuses the existing DATABASE feature toggle rather than adding a new feature enum value. The new driver instrumentable is added to the `ChainedInstrumentable` list `takeIf { isSQLiteDriverInstrEnabled() }`, alongside the existing open-helper instrumentable.

## Key Constraints
- **HARD: never double-wrap.** No single database may be wrapped by both the open-helper path and the driver path. The `SupportSQLiteDriver` bridge is the canonical double-wrap risk and must be defended at both compile-time (call-site guard) and runtime (SDK idempotency).
- Reuse the existing instrumentation architecture: `ClassInstrumentable`/`MethodInstrumentable` + `ChainedInstrumentable`, ASM `MethodVisitor`/`AdviceAdapter`, mirroring `AndroidXSQLiteOpenHelper`.
- Reuse `InstrumentationFeature.DATABASE` (no new public feature flag).
- Version-gate on the sentry-android-sqlite version that actually ships `SentrySQLiteDriver`; if the SDK on the classpath is too old, do not inject (false-negative is acceptable; a broken build is not).
- Backward compatibility: must be a no-op for projects on Room < 2.7 (no `setDriver`), older AGP, and older sentry SDKs. Must not break the existing open-helper instrumentation for legacy users.
- Target the `setDriver(SQLiteDriver)` call site generically (any `SQLiteDriver` impl: `BundledSQLiteDriver`, `AndroidSQLiteDriver`, `NativeSQLiteDriver`, `SupportSQLiteDriver`), not specific driver subclasses — wrapping by interface type is robust to new impls.
- Test parity: add precompiled `.class` fixtures under `src/test/resources/testFixtures/instrumentation/androidxSqlite/` plus parameterized `VisitorTest` cases validated with `CheckClassAdapter`, and at least one functional test covering the bridge/no-double-wrap case.

## Out of Scope
- Modifying sentry-java / authoring `SentrySQLiteDriver` itself (delivered by PR #5466).
- Instrumenting `SQLiteDriver` usage outside of Room's `setDriver` (e.g., raw KMP `SQLiteDriver` usage with no Room builder) — initial scope is the Room builder call site only.
- Kotlin Multiplatform / non-Android targets.
- Removing or redesigning the legacy `AndroidXSQLiteDatabase`/`AndroidXSQLiteStatement` old-path instrumentation.
- A user-facing config flag to choose driver-vs-helper wrapping (driven entirely by classpath + DATABASE feature).
- Runtime detection of multiple `RoomDatabase` instances sharing one driver (assumed out of scope; one wrap per `setDriver` call site).

## Open Questions
- Exact `SentrySQLiteDriver` factory API from PR #5466: is it `SentrySQLiteDriver.create(SQLiteDriver): SQLiteDriver`, a constructor, or other? Exact descriptor needed for the injected `invokeStatic`.
- The precise `sentry-android-sqlite` version that first ships `SentrySQLiteDriver` (new `SentryVersions.VERSION_SENTRY_SQLITE_DRIVER`).
- Does `SentrySQLiteDriver.create(...)` perform runtime idempotency / bridge-aware detection (unwrapping an already-Sentry-wrapped helper bridged via `SupportSQLiteDriver`)? If not, the plugin must shoulder more of the double-wrap defense at compile time.
- Exact fully-qualified name and descriptor of `RoomDatabase.Builder.setDriver` in Room 2.7 (`androidx/room/RoomDatabase$Builder` vs a Kotlin builder DSL surface) and whether the Kotlin builder DSL emits a different call shape.
- Whether `androidx.sqlite.SQLiteDriver` is the correct interface FQN/descriptor in the shipped Room 2.7 / sqlite-framework artifact.

## Success Criteria
- A project using Room 2.7+ with `setDriver(BundledSQLiteDriver())` and a qualifying sentry-android-sqlite version automatically emits SQL spans, with the driver wrapped by `SentrySQLiteDriver` in the transformed bytecode.
- A project using the `SupportSQLiteDriver` bridge over an (already Sentry-wrapped) `SupportSQLiteOpenHelper` emits exactly one span per SQL statement — no duplicates.
- Projects on Room < 2.7 / old AGP / old sentry SDK build unchanged (instrumentation is a no-op; transformed bytecode is byte-identical where the feature does not apply).
- Existing open-helper instrumentation and tests continue to pass unchanged.
- New `VisitorTest` fixtures pass `CheckClassAdapter` verification; functional test confirms single-span behavior in the bridge case.
