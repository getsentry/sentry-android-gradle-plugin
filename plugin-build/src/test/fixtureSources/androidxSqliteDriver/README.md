# androidxSqliteDriver bytecode fixtures

Java sources for the precompiled `.class` files under
`src/test/resources/testFixtures/instrumentation/androidxSqliteDriver/`. Every committed `.class`
has a matching source here so the fixtures stay regenerable. All fixtures are stored flat in the
resources directory (no package subdirectories) regardless of their declared package.

Each fixture exercises one shape of the `RoomDatabase.Builder.setDriver(SQLiteDriver)` call site
that `SQLiteDriverCallSiteVisitor` must (or must not) wrap:

| Fixture | Lang | Scenario |
| --- | --- | --- |
| `InlineConstruction` | Java | driver constructed inline as the argument |
| `LocalTypedAsImpl` | Java | argument is a local typed as the concrete `BundledSQLiteDriver` |
| `LocalTypedAsBridge` | Java | argument is a `SQLiteDriver`-typed local holding a `SupportSQLiteDriver` |
| `InlineBridge` | Java | `SupportSQLiteDriver` bridge constructed inline |
| `FieldLoad` | Java | argument loaded from an instance field |
| `FactoryReturn` | Java | argument returned from a factory method |
| `InvokeInterface` | Java | `setDriver` invoked via an interface receiver (`INVOKEINTERFACE`) |
| `TwoSetDriver` | Java | two `setDriver` call sites in one method |
| `ManualWrap` | Java | already wrapped with `SentrySQLiteDriver.create()` (double-wrap case) |
| `SentrySetDriver` | Java | Sentry-owned class (`io.sentry.sqlite`) that must not be instrumented |
| `NoSetDriver` | Java | negative case: no `setDriver` call site |
| `InferredLocal` | Java | a `checkcast` precedes `setDriver` (the shape kotlinc emits for an inferred-type local) |

`InferredLocal.kt` is a Kotlin reference showing the idiomatic equivalent; the compiled `.class`
used by tests comes from the Java source.

## Regenerate

Recompile after editing any source. The fixtures depend on the test stubs (`androidx.*`,
`io.sentry.sqlite.*`) compiled into `build/classes/kotlin/test`.

```bash
cd plugin-build
./gradlew compileTestKotlin

OUT=/tmp/androidxSqliteDriver-fixtures
FIXTURES=src/test/resources/testFixtures/instrumentation/androidxSqliteDriver

javac -d "$OUT" -cp build/classes/kotlin/test \
  src/test/fixtureSources/androidxSqliteDriver/*.java

# Copy compiled classes back, flattened (sources declare com.example or io.sentry.sqlite, but
# fixtures live flat).
cp "$OUT"/com/example/*.class "$FIXTURES"/
cp "$OUT"/io/sentry/sqlite/SentrySetDriver.class "$FIXTURES"/
```
