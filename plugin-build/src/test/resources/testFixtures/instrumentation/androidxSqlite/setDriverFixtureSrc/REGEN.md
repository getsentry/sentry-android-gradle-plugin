# Regenerating the `setDriver` call-site fixtures

These `.class` fixtures exercise `SQLiteDriverMethodVisitor` / `AndroidXSQLiteDriver` in
`io.sentry.android.gradle.instrumentation.VisitorTest`. Each caller fixture is a tiny class with a
`RoomDatabase.Builder.setDriver(SQLiteDriver)` call site whose argument has a different static type,
covering the WRAP/SKIP decision boundary:

| Fixture                   | setDriver arg static type                         | Expected injected `create` |
|---------------------------|---------------------------------------------------|----------------------------|
| `SetDriverConcrete`       | `androidx/sqlite/driver/bundled/BundledSQLiteDriver` | 1 (WRAP)                |
| `SetDriverConcreteLocal`  | `androidx/sqlite/driver/AndroidSQLiteDriver` (local) | 1 (WRAP)                |
| `SetDriverBridge`         | `androidx/sqlite/driver/SupportSQLiteDriver`         | 0 (SKIP — no double-wrap) |
| `SetDriverSentryTyped`    | `io/sentry/sqlite/SentrySQLiteDriver` (concrete)     | 0 (SKIP — already wrapped, typed) |
| `SetDriverAlreadySentry`  | `androidx/sqlite/SQLiteDriver` (create() return)     | 0 (SKIP — already wrapped, erased) |
| `SetDriverBareInterface`  | `androidx/sqlite/SQLiteDriver` (method return)       | 0 (SKIP — erased)        |

The `stubs/` tree contains the minimal androidx/room/sentry types needed only to COMPILE the
callers. They are NOT needed at test time: `VisitorTest` resolves missing types via
`GeneratingMissingClassesClassLoader`, so the real SDK artifact is never on the classpath.

## Recipe

Run from this directory (`.../testFixtures/instrumentation/androidxSqlite/setDriverFixtureSrc`):

```sh
rm -rf out && mkdir out
find . -name '*.java' > sources.txt
javac -source 8 -target 8 -d out @sources.txt
# copy the compiled caller classes (default package) up one level, next to the other fixtures:
cp out/SetDriver*.class ..
rm -rf out sources.txt
```

Only the default-package `SetDriver*.class` files are committed as fixtures; the compiled stub
classes under `out/androidx`, `out/io` are throwaway.
