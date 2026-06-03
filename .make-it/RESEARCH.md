> Valid for this sprint only. Delete when this feature ships.

# Research: Auto-wrapping SQLiteDriver with SentrySQLiteDriver

All signatures below are confirmed from primary sources (sentry-java PR #5466 head ref `feat/support-sqlite-driver`; AndroidX `androidx/androidx` API txt files) unless marked TBD.

## 1. SentrySQLiteDriver API (sentry-java PR #5466)

Source: `sentry-android-sqlite/src/main/java/io/sentry/sqlite/SentrySQLiteDriver.kt` + `api/sentry-android-sqlite.api`.

- **Package: `io.sentry.sqlite`** — NOT `io.sentry.android.sqlite` (where `SentrySupportSQLiteOpenHelper` lives). Deliberate: packaged so it can be lifted into a KMP module later.
- Factory (the call to inject):
  ```
  owner:      io/sentry/sqlite/SentrySQLiteDriver
  name:       create
  descriptor: (Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;
  static:     yes (@JvmStatic on companion)
  ```
- Constructor is `private`; `create(...)` is the only entry point.
- **Idempotency (verbatim):** `public fun create(delegate: SQLiteDriver): SQLiteDriver = delegate as? SentrySQLiteDriver ?: SentrySQLiteDriver(delegate)`. So wrapping an already-`SentrySQLiteDriver` is a no-op. **It does NOT inspect/unwrap the `SupportSQLiteDriver` bridge** — bridge-based double-wrap is unprotected at the SDK level (see §3).
- Same artifact as the open helper: `io.sentry:sentry-android-sqlite` (the `.api` file is `sentry-android-sqlite.api`). So version-gating reuses the existing `SentryModules.SENTRY_ANDROID_SQLITE` module id, with a NEW minimum version.

## 2. AndroidX / Room signatures (androidx/androidx)

- `androidx.sqlite.SQLiteDriver` — interface (in `androidx.sqlite:sqlite`). Internal name `androidx/sqlite/SQLiteDriver`. (Confirmed via sentry `.api`: `SentrySQLiteDriver : androidx/sqlite/SQLiteDriver`.)
- Concrete driver impls (all `implements androidx.sqlite.SQLiteDriver`, all in `androidx.sqlite.driver.*`):
  - `androidx/sqlite/driver/AndroidSQLiteDriver` — `final class`, `androidx.sqlite:sqlite-framework`, Android. **Primary target.**
  - `androidx/sqlite/driver/bundled/BundledSQLiteDriver` — `final class`, `androidx.sqlite:sqlite-bundled`. Common (KMP/consistency). **Secondary target.**
  - `androidx/sqlite/driver/NativeSQLiteDriver` — KMP native only; no Android/JVM bytecode → irrelevant to this plugin.
- **`androidx/sqlite/driver/SupportSQLiteDriver`** — the bridge, `final class implements androidx.sqlite.SQLiteDriver`, in `androidx.sqlite:sqlite-framework` (`src/androidMain/.../driver/SupportSQLiteDriver.android.kt`). Constructed from a `SupportSQLiteOpenHelper`. **This is the one class we must NEVER wrap** (the double-wrap hazard).
- `RoomDatabase.Builder.setDriver`: `RoomDatabase.Builder<T> setDriver(androidx.sqlite.SQLiteDriver driver)`. Bytecode:
  ```
  name:       setDriver
  descriptor: (Landroidx/sqlite/SQLiteDriver;)Landroidx/room/RoomDatabase$Builder;     (Room 2.7, package androidx.room)
              (Landroidx/sqlite/SQLiteDriver;)Landroidx/room3/RoomDatabase$Builder;    (Room 3.0, package androidx.room3 on trunk)
  ```
  Note: AndroidX trunk has renamed the Room 3.0 module to `room3` / package `androidx.room3`. Room 2.7 ships as `androidx.room`. A call-site matcher should accept both Builder owners (or match by method name + `(Landroidx/sqlite/SQLiteDriver;)` arg, ignoring the Builder owner — see WrappingVisitor's owner-agnostic fallback, §4).

## 3. The double-wrap reality (THE hard constraint)

PR #5466 callout [4] + the `create()` source together mean:
- The SDK guards against re-wrapping a `SentrySQLiteDriver` (driver-level idempotency) — so plugin-wrap + manual-wrap of the *same driver* is safe.
- The SDK does **NOT** guard the bridge case: `SupportSQLiteDriver(sentryWrappedHelper)` is a plain `SupportSQLiteDriver`, so `SentrySQLiteDriver.create(...)` will happily wrap it, producing duplicate spans (helper layer + driver layer). The SDK author mitigates this with docs ("do not wrap both") and considered logging an error.
- Therefore the plugin's job re: no-double-wrap reduces to **never auto-wrapping the `SupportSQLiteDriver` bridge.** Real drivers (`AndroidSQLiteDriver`, `BundledSQLiteDriver`, custom) have no open-helper layer, so wrapping them is always single-instrumentation.
- The existing open-helper instrumentation wraps `SupportSQLiteOpenHelper$Factory.create` returns (incl. `FrameworkSQLiteOpenHelperFactory`), so in the bridge idiom the helper IS already plugin-wrapped — confirming we must not also wrap the bridge driver.

## 4. Existing instrumentation architecture — the right template

`isInstrumentable(ClassData)` only sees class name / interfaces / superclasses / annotations — **not method bodies**. So an instrumentation that targets a *call site* (a call that can appear in any user class) must return `isInstrumentable = true` broadly and then bail cheaply per-method. Precedent already exists:
- `wrap/WrappingInstrumentable` (the **File I/O** instrumentation): `isInstrumentable(ClassContext) = !data.isSentryClass()` → visits ALL non-Sentry classes; uses a two-pass `AnalyzingVisitor` (first pass collects `MethodNode`s) then `CommonClassVisitor` + `wrap/visitor/WrappingVisitor` to rewrite specific calls.
- `logcat/Logcat`, `remap/RemappingInstrumentable` — also `!isSentryClass()`.

`WrappingVisitor` rewrites a target call into `original-call` + `INVOKESTATIC <wrapper>.create(originalReturnType, ...originalArgs)`. It maps `Replacement(owner,name,desc) -> Replacement(...)` (with an owner-agnostic fallback when `owner == ""`), and already handles the gnarly NEW/DUP/ASTORE local-slot bookkeeping.

### Critical type-safety finding (drives the design)
The File I/O wrap works at the **constructor site** (`new FileInputStream(...)` → `SentryFileInputStream`) ONLY because `SentryFileInputStream extends FileInputStream` — the wrapper is a subtype, so storing it back into a `FileInputStream`-typed local verifies.

`SentrySQLiteDriver` is `final`; it `implements SQLiteDriver` but is **not** a subtype of `AndroidSQLiteDriver`/`BundledSQLiteDriver` (themselves `final`). So **constructor-site wrapping is unsafe**: `val d = AndroidSQLiteDriver(); builder.setDriver(d)` compiles `d` as a local typed `AndroidSQLiteDriver`; storing a `SentrySQLiteDriver` there is a verifier error. Inline `setDriver(AndroidSQLiteDriver())` would be fine, but the local-variable idiom would break.

➡️ **Wrap the `setDriver(SQLiteDriver)` ARGUMENT instead.** The argument slot is the interface type `SQLiteDriver`; `SentrySQLiteDriver` IS-A `SQLiteDriver`, so the wrapped value always verifies regardless of how the driver was constructed/stored. This is type-safe in every idiom.

## 5. Recommended design (carried into the PRD)

New `ClassInstrumentable` (e.g. `AndroidXSQLiteDriver` under `instrumentation/androidx/sqlite/`) that:
1. `isInstrumentable(ClassContext) = !data.isSentryClass()` (broad, mirrors WrappingInstrumentable). Visits app classes; bails cheaply where no `setDriver` call exists.
2. In a `MethodVisitor`, intercept `visitMethodInsn` for `setDriver` whose descriptor is `(Landroidx/sqlite/SQLiteDriver;)...` on a `RoomDatabase$Builder` owner (accept `androidx/room/RoomDatabase$Builder` and `androidx/room3/RoomDatabase$Builder`; an owner-agnostic name+arg match is acceptable and more robust).
3. Before delegating the `setDriver` call, the driver arg is on stack top → inject `INVOKESTATIC io/sentry/sqlite/SentrySQLiteDriver.create (Landroidx/sqlite/SQLiteDriver;)Landroidx/sqlite/SQLiteDriver;`. Stack `[builder, driver]` → `[builder, wrappedDriver]` → original `setDriver`. No DUP/SWAP needed.
4. **Bridge exclusion (no double-wrap):** skip injection when the argument is the `SupportSQLiteDriver` bridge. Determine the arg's static type via an `AnalyzerAdapter`-style pass (the plugin already two-passes via `AnalyzingVisitor`); if the top-of-stack type is `androidx/sqlite/driver/SupportSQLiteDriver`, do NOT wrap. Bias: when the arg type is only the bare `androidx/sqlite/SQLiteDriver` interface (can't prove it isn't a bridge), prefer to SKIP (false-negative) over risking a double-wrap (false-positive) — matches the agreed bias. The common inline/concrete-typed idioms are positively identifiable and DO get wrapped.
   - SDK idempotency additionally backstops the `SentrySQLiteDriver`-already-wrapped case for free.
5. Gating: new `SentryModulesService.isSQLiteDriverInstrEnabled()` = `sentry-android-sqlite >= VERSION_SENTRY_SQLITE_DRIVER` AND `InstrumentationFeature.DATABASE`. Add to the `ChainedInstrumentable` list `takeIf { isSQLiteDriverInstrEnabled() }`. Reuse `InstrumentationFeature.DATABASE` (no new public flag).

### Version gate value (TBD)
PR #5466 is still OPEN; latest released sentry-java is **8.42.0** (2026-05-20), so `SentrySQLiteDriver` ships in a later release. Use a placeholder `SentryVersions.VERSION_SENTRY_SQLITE_DRIVER` (proposed `SemVer(8, 43, 0)`) with a clear TODO to pin to the actual release that contains the merged PR. Existing constants for reference: `VERSION_SQLITE = 6.21.0`, `VERSION_PERFORMANCE = 4.0.0`.

## 6. Open items for the prototype / implementation
- Confirm the cleanest way to read the `setDriver` arg's static type for bridge exclusion: reuse `instrumentation/util/AnalyzingVisitor` + ASM `AnalyzerAdapter`, or a peephole over the first-pass `MethodNode` (detect the value's producing instruction). Prototype both; pick the one that verifies cleanly with `CheckClassAdapter`.
- Decide whether to also positively allowlist concrete driver types vs. wrap any concrete (non-bridge) `SQLiteDriver` arg. Leaning: wrap any arg whose static type is a concrete class assignable to `SQLiteDriver` and != `SupportSQLiteDriver`; skip bare-interface args.
- Confirm Kotlin Room builder DSL (`Room.databaseBuilder{ }.setDriver(...)`) emits a plain INVOKEVIRTUAL to `RoomDatabase$Builder.setDriver` (expected yes).
- Test fixtures: add `.class` fixtures + `VisitorTest` params for (a) `setDriver(AndroidSQLiteDriver())` → asserts injected `SentrySQLiteDriver.create`; (b) bridge `setDriver(SupportSQLiteDriver(helper))` → asserts NO wrap. Validate with `CheckClassAdapter`. Functional test for single-span behavior in the bridge case.

## Sources
- sentry-java PR #5466 (`getsentry/sentry-java`), files `SentrySQLiteDriver.kt`, `api/sentry-android-sqlite.api` @ ref `feat/support-sqlite-driver`.
- `androidx/androidx`: `sqlite/sqlite-framework/api/current.txt`, `sqlite/sqlite-bundled/api/current.txt`, `room3/room3-runtime/api/current.txt`, `sqlite/sqlite-framework/src/androidMain/kotlin/androidx/sqlite/driver/SupportSQLiteDriver.android.kt`.
- This repo: `instrumentation/wrap/{WrappingInstrumentable,Replacements}.kt`, `instrumentation/wrap/visitor/WrappingVisitor.kt`, `instrumentation/androidx/sqlite/AndroidXSQLiteOpenHelper.kt`, `services/SentryModulesService.kt`, `util/Versions.kt`.
- Maven: latest `io.sentry:sentry-android-sqlite` = 8.42.0 (2026-05-20).
