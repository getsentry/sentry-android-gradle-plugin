# Changelog

## Unreleased

### Features

- Bump Android SDK from v6.4.0 to v6.4.1 ([#372](https://github.com/getsentry/sentry-android-gradle-plugin/pull/372))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#641)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.4.0...6.4.1)

## 3.1.5

### Features

- Bump CLI from v2.5.0 to v2.5.2 ([#368](https://github.com/getsentry/sentry-android-gradle-plugin/pull/368))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#252)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.5.0...2.5.2)
- Bump Android SDK from v6.3.1 to v6.4.0 ([#369](https://github.com/getsentry/sentry-android-gradle-plugin/pull/369))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#640)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.3.1...6.4.0)

## 3.1.4

### Fixes

- Detect minified classes and skip instrumentation to avoid build problems ([#362](https://github.com/getsentry/sentry-android-gradle-plugin/pull/362))

### Features

- Bump AGP to 7.2.1 and Gradle to 7.5.0 ([#363](https://github.com/getsentry/sentry-android-gradle-plugin/pull/363))
- Bump Android SDK to v6.3.1 ([#365](https://github.com/getsentry/sentry-android-gradle-plugin/pull/365))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#631)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.3.0...6.3.1)
- Bump CLI to v2.5.0 ([#358](https://github.com/getsentry/sentry-android-gradle-plugin/pull/358))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#250)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.4.1...2.5.0)

## 3.1.3

### Features

- Bump CLI to v2.4.1 ([#338](https://github.com/getsentry/sentry-android-gradle-plugin/pull/338), [#341](https://github.com/getsentry/sentry-android-gradle-plugin/pull/341), [#343](https://github.com/getsentry/sentry-android-gradle-plugin/pull/343), [#350](https://github.com/getsentry/sentry-android-gradle-plugin/pull/350))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#241)
  - [diff](https://github.com/getsentry/sentry-cli/compare/1.72.0...2.4.1)
- Bump Android SDK to v6.3.0 ([#357](https://github.com/getsentry/sentry-android-gradle-plugin/pull/357))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#630)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.1.3...6.3.0)

### Fixes

- Skip jar processing on AGP < 7.1.2 for signed multi release jars ([#334](https://github.com/getsentry/sentry-android-gradle-plugin/pull/334))
- Fix `OkHttp` version `3.x` was not instrumented ([#351](https://github.com/getsentry/sentry-android-gradle-plugin/pull/351))

## 3.1.2

### Features

- Bump sentry runtime SDK version to `6.1.3` ([#333](https://github.com/getsentry/sentry-android-gradle-plugin/pull/333))

## 3.1.1

### Features

- Bump sentry runtime SDK version to `6.1.0` ([#321](https://github.com/getsentry/sentry-android-gradle-plugin/pull/321))

### Fixes

- Fix `OkHttp` auto-instrumentation crash, when `sentry-android-okhttp` is not present on classpath ([#327](https://github.com/getsentry/sentry-android-gradle-plugin/pull/327))

## 3.1.0

### Features
* Auto-install sentry-android SDK and integration dependencies (fragment, timber, okhttp) (#282)
* `OkHttp` auto-instrumentation (#288)

### Fixes

* Ignore R8 minified libs from instrumentation (#316)
* obfuscated libs instrumentation (adcolony) (#307)

## 3.1.0-beta.2

### Fixes

* Ignore R8 minified libs from instrumentation (#316)

## 3.1.0-beta.1

* Fix: obfuscated libs instrumentation (adcolony) (#307)

## 3.0.1

* Fix: obfuscated libs instrumentation (gms) (#303)

## 3.1.0-alpha.1

* Feature: Auto-install sentry-android SDK and integration dependencies (fragment, timber, okhttp) (#282)
* Feature: `OkHttp` auto-instrumentation (#288)

## 3.0.0

* Bump: AGP to 7.1.2 (#287)
* Feature: Add support for GuardSquare's Proguard (#263) by @cortinico
* Feature: Add support for GuardSquare's Dexguard (#267) by @cortinico
* Bump: sentry-cli 1.72.0 which prevent daemonize mode from crashing upload process (#262) by @cortinico
* Introduce the `includeProguardMapping` option to exclude the proguard logic, and deprecate `autoUpload` in favor of `autoUploadProguardMapping` (#240) by @cortinico
* Feature: New File I/O auto-instrumentation (#249)
* Feature: Add compile-time check for sentry-android SDK presence (#243)
* Fix: Correctly add the proguard UUID output directory to the source set (#226)
* Feature: Make the ignoreXXX properties in SentryPluginExtension sets (#225)
* Add support for dry-run on upload native symbols (#209)
* Feature: Add support for M1 Macs (#204)
* Feature: Auto-instrumentation for `androidx.sqlite` and `androidx.room` (#180)

**Breaking changes**

* The min AGP version required is `7.0.0`

See the migration guide on our [documentation](https://docs.sentry.io/platforms/android/migration/#migrating-from-iosentrysentry-android-gradle-plugin-2x-to-iosentrysentry-android-gradle-plugin-300).

## 3.0.0-rc.3

* Fix MetaInfStripTransform breaking Kotlin Gradle and IDE plugin (#291)

## 3.0.0-rc.2

* Bump: AGP to 7.1.2 (#287)

## 3.0.0-rc.1

* Feature: Add support for GuardSquare's Proguard (#263)
* Feature: Add support for GuardSquare's Dexguard (#267)
* Fix: Do not resolve dependencies at configuration time (#278)

## 3.0.0-beta.4

* Fix: Strip out unsupported java classes from META-INF/ (so AGP does not fail before our code is reached) (#264)
* Bump sentry-cli 1.72.0 which prevent daemonize mode from crashing upload process (#262)
* Fix: Incompatibilities with other Gradle plugins using the same API from AGP for bytecode instrumentation (#270)

## 3.0.0-beta.3

* Introduce the `includeProguardMapping` option to exclude the proguard logic, and deprecate `autoUpload` in favor of `autoUploadProguardMapping` (#240)
* Feature: New File I/O auto-instrumentation (#249)
* Feature: Add compile-time check for sentry-android SDK presence (#243)
* Feature: New configuration option `tracingInstrumentation.features` to allow enabling/disabling certain features for auto-instrumentation (#245)

## 3.0.0-beta.2

* Fix: Correctly add the proguard UUID output directory to the source set (#226)
* Feature: Make the ignoreXXX properties in SentryPluginExtension sets (#225)
* Expose SentryPluginExtension.tracingInstrumentation (#229)
* Ref: Change Room queries description to Dao class name (#232)
* Fix: Log broken bytecode when build fails (#233)
* Ref: Change db operation names (#237)

## 3.0.0-beta.1

* Fix: Associate spans and events when it throws (#219)

## 3.0.0-alpha.2

* Fix: Do not throw exceptions in case something goes wrong with instrumentation (#217)
* Add support for dry-run on upload native symbols (#209)

## 3.0.0-alpha.1

* Feat: Add support for M1 Macs (#204)
* Feat: Auto-instrumentation for `androidx.sqlite` and `androidx.room` (#180)

**Breaking changes**

* The min AGP version required is `7.0.0`
* The min Sentry's Android SDK is `4.0.0`

See the migration guide on our [documentation](https://github.com/getsentry/sentry-docs/pull/4281).

## 2.1.5

* Bump: AGP to 7.0.2 (#193)
* Bump sentry-cli 1.69.1 which includes a fix for Dart debug symbols (#191)

## 2.1.4

* Fix: Pass buildDir as task input (#166)

## 2.1.3

* Fix: Use task logger instead of project logger (#165)

## 2.1.2

* Bump: AGP to 4.2.2 (#106)
* Fix: missing sentry-cli on embedded resources (#162)

## 2.1.1

* Enhancement: Avoid Eager Task Configuration (#156)
* Fix: Do not hardcode the build/ folder (#158)

## 2.1.1-beta.2

- No documented changes.

## 2.1.1-beta.1

- No documented changes.

## 2.1.0

* Feature: Add support for variant filtering. (#140)

## 2.0.1

* Fix: Only upload debug symbols for non debuggable App. (#139)

## 2.0.0

This release comes with a full rewrite of the Sentry Gradle Plugin.

Here is the [Migration Guide](https://docs.sentry.io/platforms/android/migration/#migrating-from-iosentrysentry-android-gradle-plugin-1x-to-iosentrysentry-android-gradle-plugin-200).

Improvements:

* Using lazily Providers
* Support for Configuration Avoidance
* Only try to upload mapping file if `minifyEnabled` is enabled
* Plugin Marker is published, so you may use the `plugins` block
* Rewritten in Kotlin
* Unit and Integration tests
* CI Matrix that runs against different AGP/Gradle/Java and OS versions

Breaking changes:

* Sentry Android Gradle Plugin requires Android Gradle Plugin >= 4.0.0
* The `autoProguardConfig` flag has been removed

Changes:

* Bump: sentry-cli to 1.65.0 (#133)
* Bump: Gradle 7.0.2 (#135)

Thank you:

* @cortinico for coding most of it.
* @ansman for driving the first PoC of the full rewrite.
* @cerisier for EA and small fixes.

## 2.0.0-beta.3

* Enhancement: Clean up deprecated/removed Dex and Transform tasks (#130)

## 2.0.0-beta.2

* Enhancement: Use pluginManager instead of project.afterEvaluate (#119)
* Enhancement: Use assembleTaskProvider lazily (#121)
* Enhancement: Use packageProvider lazily (#125)
* Enhancement: Use mappingFileProvider lazily (#128)

## 2.0.0-beta.1

* Feat: Support Configuration Avoidance (#112)
* Fix: Silence the warning for missing mapping file on variants that don't enable minification (#111)
* Bump: sentry-cli to 1.64.1

## 2.0.0-alpha.3

* Fix: Only wire upload mapping task if minifyEnabled (#86) @cerisier

## 2.0.0-alpha.2

* Fix: Publish Plugin Marker on maven central @marandaneto

## 2.0.0-alpha.1

* Feat: Gradle plugin v2 (#50) @cortinico
* Enhancement: Allow module level sentry properties file (#33) @MatthewTPage

## 1.x

* See GH releases https://github.com/getsentry/sentry-android-gradle-plugin/releases
