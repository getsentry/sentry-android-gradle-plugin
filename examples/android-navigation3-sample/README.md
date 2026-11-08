# android-navigation3-sample

A Navigation 3 app that doesn't call `SentryNavEffect`. It enables the plugin's Navigation 3 auto-instrumentation
(`tracingInstrumentation { navigation3 { enabled = true } }`), which records navigation transactions, breadcrumbs, and screen names for
every `NavDisplay(backStack = ...)`.

The screens cover:

- pushes and pops between `@Serializable` keys with and without `@SerialName` (`/Home`, `/Profile`, and the fully qualified
  `/io.sentry.samples.navigation3.Settings`);
- replacing the back stack (Settings > "Replace with Home");
- a nested `NavDisplay` (Tabs), whose navigation isn't recorded because only the outer `NavDisplay` records;
- a key that isn't `@Serializable` (Legacy), recorded as `/unknown`.

Auto-instrumentation only applies with a `sentry-android-navigation3` version that contains its entry point
(`SentryVersions.VERSION_NAVIGATION3`). Until that version is the plugin's bundled SDK version, the sample builds without it. To try it with
an unreleased SDK, publish sentry-java to Maven Local and point the sample at it:

```bash
# in sentry-java
./gradlew publishToMavenLocal -PversionName=8.64.0

# in this repository
./gradlew :examples:android-navigation3-sample:installRelease -PuseLocalSentrySdk -PsentryNavigation3Version=8.64.0
```

With `--info`, the build logs `Instrumented 1 rememberDecoratedNavEntries call(s) in NavDisplay.` The app logs Sentry's debug output to
logcat.

The sample needs AGP 8.9.1+ and Kotlin 2, so `settings.gradle.kts` skips it when the compatibility matrix runs older versions.
