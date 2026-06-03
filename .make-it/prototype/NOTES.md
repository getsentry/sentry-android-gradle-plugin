# Prototype NOTES — SQLiteDriver call-site wrapping (LOGIC)

Run with `./run.sh` (needs JDK 17 + ASM 9.8 in the Gradle cache; both present). Throwaway.

## What it validates

The core, riskiest piece of the feature: an ASM `MethodVisitor` that intercepts
`RoomDatabase.Builder.setDriver(SQLiteDriver)` call sites and injects
`io.sentry.sqlite.SentrySQLiteDriver.create(...)` around the **argument** (not the driver
construction), using `AnalyzerAdapter` to read the argument's static type and skip the
`SupportSQLiteDriver` bridge / already-wrapped / erased-interface cases.

It compiles real stub classes (`androidx.sqlite.SQLiteDriver`, `AndroidSQLiteDriver`,
`SupportSQLiteDriver`, `androidx.room.RoomDatabase$Builder`, `io.sentry.sqlite.SentrySQLiteDriver`)
+ a `Caller`, rewrites `Caller`'s bytecode, then **runs the transformed `Caller` on a stock JVM
with no ASM on the classpath** — so a clean run is a JVM-verifier pass plus observable behavior.

## Result (the answer the prototype gives)

The approach works. Observed decisions + runtime behavior:

| Scenario | setDriver arg static type | Decision | Runtime type passed to setDriver |
|----------|---------------------------|----------|----------------------------------|
| A inline `new AndroidSQLiteDriver()` | `androidx/sqlite/driver/AndroidSQLiteDriver` | **WRAP** | `SentrySQLiteDriver` ✅ |
| B inline `new SupportSQLiteDriver(h)` | `androidx/sqlite/driver/SupportSQLiteDriver` | **SKIP** | `SupportSQLiteDriver` ✅ (no double-wrap) |
| C `AndroidSQLiteDriver d=...; setDriver(d)` | `androidx/sqlite/driver/AndroidSQLiteDriver` | **WRAP** | `SentrySQLiteDriver` ✅ |
| D `SQLiteDriver d=new SupportSQLiteDriver(); setDriver(d)` | `androidx/sqlite/driver/SupportSQLiteDriver` | **SKIP** | `SupportSQLiteDriver` ✅ |
| E `setDriver(SentrySQLiteDriver.create(...))` | `androidx/sqlite/SQLiteDriver` | **SKIP** | `SentrySQLiteDriver` ✅ (single wrap) |
| F `setDriver(provideDriver())` | `androidx/sqlite/SQLiteDriver` | **SKIP** | `AndroidSQLiteDriver` (false-negative, by design) |

`DONE (transformed Caller loaded & ran => JVM verifier accepted it)` printed every time.

## Key findings carried into the PRD

1. **Wrap the argument, not the constructor.** This is type-safe in every idiom. The existing
   File-I/O `wrap` machinery wraps at the constructor and only verifies because
   `SentryFileInputStream extends FileInputStream`. `SentrySQLiteDriver` is `final` and only
   `implements SQLiteDriver`, so wrapping `new AndroidSQLiteDriver()` and storing it into a
   concrete `AndroidSQLiteDriver` local (scenario C) would be a verifier error. Wrapping the
   `setDriver` argument (interface-typed slot) sidesteps this entirely — scenario C runs clean.

2. **`AnalyzerAdapter` gives the arg's static type for free**, with no class loading. It tracks the
   value's type from its producing instruction:
   - `NEW X` / concrete-typed local → reports the concrete type (A, C wrap; B, D skip the bridge).
   - method return / `create()` result → reports the declared return type
     (`SQLiteDriver` interface) → we SKIP (E avoids double-wrap; F is an accepted false-negative).
   - Note D: even an interface-typed local holding a freshly-constructed bridge is reported as
     `SupportSQLiteDriver`, so the bridge is skipped there too — better than worst-case.

3. **No-double-wrap is structural**: the bridge class is simply never wrapped (B, D). SDK
   idempotency additionally backstops re-wrapping a `SentrySQLiteDriver` (E).

4. **Frames stay valid with `COMPUTE_MAXS` only** (no `COMPUTE_FRAMES`, no class loading by the
   writer): injecting `create()` is net-zero stack effect (pop 1 `SQLiteDriver`, push 1), so
   existing stack-map frames remain correct. Confirmed by the clean verifier run.

5. **Owner-agnostic match** (`name == "setDriver"` && desc starts with `(Landroidx/sqlite/SQLiteDriver;)`)
   covers both `androidx/room/RoomDatabase$Builder` (Room 2.7) and `androidx/room3/...` (Room 3.0)
   without enumerating Builder owners. The prototype used the Room 2.7 owner and matched fine.

## Caveats / not covered by the prototype
- The real plugin must gate on sentry-android-sqlite version + `InstrumentationFeature.DATABASE`,
  and register the instrumentable broadly (`!isSentryClass()`) like `WrappingInstrumentable`.
- Real `AnalyzerAdapter` integration in the plugin should reuse the two-pass `AnalyzingVisitor`
  pattern; the prototype uses `AnalyzerAdapter` directly (sufficient to prove the logic).
- The prototype models the bridge constructor as taking `Object`; the real bridge takes a
  `SupportSQLiteOpenHelper`. Irrelevant to the call-site decision (which keys on the arg type).
