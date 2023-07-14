# Changelog

## Unreleased

### Dependencies

- Bump CLI from v2.19.1 to v2.19.4 ([#520](https://github.com/getsentry/sentry-android-gradle-plugin/pull/520))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2194)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.19.1...2.19.4)
- Bump Android SDK from v6.22.0 to v6.25.2 ([#527](https://github.com/getsentry/sentry-android-gradle-plugin/pull/527))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6252)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.22.0...6.25.2)

## 3.11.1

### Fixes

- Fixed OkHttpEventListener crash at runtime with OkHttp4 ([#514](https://github.com/getsentry/sentry-android-gradle-plugin/pull/514))

### Dependencies

- Bump CLI from v2.18.1 to v2.19.1 ([#512](https://github.com/getsentry/sentry-android-gradle-plugin/pull/512))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2191)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.18.1...2.19.1)

## 3.11.0

### Features

- Add OkHttpEventListener automatically ([#504](https://github.com/getsentry/sentry-android-gradle-plugin/pull/504))
- New Sqlite instrumentation ([#502](https://github.com/getsentry/sentry-android-gradle-plugin/pull/502))
    - This integration replaces the old database instrumentation with the `sentry-android-sqlite` integration
    - Any implementation of SupportSQLiteOpenHelper.Factory is now supported

## 3.10.0

### Dependencies

- Bump Android SDK from v6.21.0 to v6.22.0 ([#506](https://github.com/getsentry/sentry-android-gradle-plugin/pull/506))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6220)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.21.0...6.22.0)

## 3.9.0

### Fixes

- Rename `project` to `projectName` in SentryPluginExtension ([#505](https://github.com/getsentry/sentry-android-gradle-plugin/pull/505))
- No longer ignore `org` and `projectName` in `sentry` config block ([#501](https://github.com/getsentry/sentry-android-gradle-plugin/pull/501))

## 3.8.0

### Features

- Create a new Sentry Gradle plugin with ID `io.sentry.jvm.gradle` for Java Backend, Desktop etc. ([#495](https://github.com/getsentry/sentry-android-gradle-plugin/pull/495)) 
- Source Context for Java ([#495](https://github.com/getsentry/sentry-android-gradle-plugin/pull/495))
  - To enable it apply the `io.sentry.jvm.gradle` plugin and set `includeSourceContext` to `true`
  - For more information on how to enable source context, please refer to [#633](https://github.com/getsentry/sentry-java/issues/633#issuecomment-1465599120)
- Allow setting sentry properties via the `sentry` plugin extension ([#500](https://github.com/getsentry/sentry-android-gradle-plugin/pull/500))

### Dependencies

- Bump CLI from v2.17.5 to v2.18.1 ([#493](https://github.com/getsentry/sentry-android-gradle-plugin/pull/493))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2181)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.17.5...2.18.1)
- Bump Android SDK from v6.19.0 to v6.21.0 ([#499](https://github.com/getsentry/sentry-android-gradle-plugin/pull/499))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6210)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.19.0...6.21.0)

## 3.7.0

### Features

- Bundle Java Sources and upload to Sentry ([#472](https://github.com/getsentry/sentry-android-gradle-plugin/pull/472))
    - For more information on how to enable source context, please refer to [#633](https://github.com/getsentry/sentry-java/issues/633#issuecomment-1465599120)
- New `debug` option to enable debug logging for sentry-cli ([#472](https://github.com/getsentry/sentry-android-gradle-plugin/pull/472))

### Fixes

- Add missing Kotlin Compiler Plugin Marker config ([#488](https://github.com/getsentry/sentry-android-gradle-plugin/pull/488))

### Dependencies

- Bump Android SDK from v6.18.1 to v6.19.0 ([#490](https://github.com/getsentry/sentry-android-gradle-plugin/pull/490))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6190)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.18.1...6.19.0)

## 3.6.0

### Features

- Add Kotlin Compiler plugin to enrich Composable functions ([#452](https://github.com/getsentry/sentry-android-gradle-plugin/pull/452))

### Fixes

- Do not transform the sdk name to Int in BootstrapAndroidSdk ([#478](https://github.com/getsentry/sentry-android-gradle-plugin/pull/478))

### Dependencies

- Bump CLI from v2.16.1 to v2.17.5 ([#469](https://github.com/getsentry/sentry-android-gradle-plugin/pull/469), [#475](https://github.com/getsentry/sentry-android-gradle-plugin/pull/475), [#480](https://github.com/getsentry/sentry-android-gradle-plugin/pull/480))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2175)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.16.1...2.17.5)
- Bump Android SDK from v6.17.0 to v6.18.1 ([#481](https://github.com/getsentry/sentry-android-gradle-plugin/pull/481))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6181)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.17.0...6.18.1)

## 3.5.0

### Features

- Add support for Logcat ([#455](https://github.com/getsentry/sentry-android-gradle-plugin/pull/455))
- Write enabled instrumentations to manifest ([#441](https://github.com/getsentry/sentry-android-gradle-plugin/pull/441))

### Dependencies

- Bump CLI from v2.15.2 to v2.16.1 ([#458](https://github.com/getsentry/sentry-android-gradle-plugin/pull/458))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2161)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.15.2...2.16.1)
- Bump Android SDK from v6.16.0 to v6.17.0 ([#462](https://github.com/getsentry/sentry-android-gradle-plugin/pull/462))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6170)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.16.0...6.17.0)

## 3.4.3

### Fixes

- Make plugin Gradle 8 compatible ([#428](https://github.com/getsentry/sentry-android-gradle-plugin/pull/428))
- Fix `uploadSentryNativeSymbols` task for Gradle 8 ([#447](https://github.com/getsentry/sentry-android-gradle-plugin/pull/447))

### Dependencies

- Bump Android SDK from v6.13.0 to v6.16.0 ([#442](https://github.com/getsentry/sentry-android-gradle-plugin/pull/442), [#449](https://github.com/getsentry/sentry-android-gradle-plugin/pull/449))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6160)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.13.0...6.16.0)
- Bump CLI from v2.12.0 to v2.15.2 ([#439](https://github.com/getsentry/sentry-android-gradle-plugin/pull/439), [#443](https://github.com/getsentry/sentry-android-gradle-plugin/pull/443), [#446](https://github.com/getsentry/sentry-android-gradle-plugin/pull/446), [#450](https://github.com/getsentry/sentry-android-gradle-plugin/pull/450))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2152)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.12.0...2.15.2)

## 3.4.2

### Fixes

- Fix failing room 2.5.0 instrumentation ([#435](https://github.com/getsentry/sentry-android-gradle-plugin/pull/435))

### Dependencies

- Bump CLI from v2.11.0 to v2.12.0 ([#433](https://github.com/getsentry/sentry-android-gradle-plugin/pull/433))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2120)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.11.0...2.12.0)

## 3.4.1

### Fixes

- Fix AGP 7.4.0 incompatibility when merging assets ([#431](https://github.com/getsentry/sentry-android-gradle-plugin/pull/431))

### Dependencies

- Bump Android SDK from v6.11.0 to v6.13.0 ([#427](https://github.com/getsentry/sentry-android-gradle-plugin/pull/427), [#432](https://github.com/getsentry/sentry-android-gradle-plugin/pull/432))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6130)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.11.0...6.13.0)

## 3.4.0

### Features

- Support configuration cache in dependencies report task from Gradle `7.5` onwards ([#423](https://github.com/getsentry/sentry-android-gradle-plugin/pull/423))

### Fixes

- Do not register dependencies report task if it's disabled ([#422](https://github.com/getsentry/sentry-android-gradle-plugin/pull/422))
- Ensure clean state before generating a new uuid by deleting the old `sentry-debug-meta.properties` file ([#420](https://github.com/getsentry/sentry-android-gradle-plugin/pull/420))

### Dependencies

- Bump Android SDK from v6.7.0 to v6.11.0 ([#406](https://github.com/getsentry/sentry-android-gradle-plugin/pull/406), [#408](https://github.com/getsentry/sentry-android-gradle-plugin/pull/408), [#411](https://github.com/getsentry/sentry-android-gradle-plugin/pull/411), [#414](https://github.com/getsentry/sentry-android-gradle-plugin/pull/414), [#424](https://github.com/getsentry/sentry-android-gradle-plugin/pull/424))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6110)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.7.0...6.11.0)
- Bump CLI from v2.8.1 to v2.11.0 ([#405](https://github.com/getsentry/sentry-android-gradle-plugin/pull/405), [#413](https://github.com/getsentry/sentry-android-gradle-plugin/pull/413), [#418](https://github.com/getsentry/sentry-android-gradle-plugin/pull/418))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2110)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.8.1...2.11.0)

## 3.3.0

### Features

- Populate events with dependencies metadata ([#396](https://github.com/getsentry/sentry-android-gradle-plugin/pull/396))
- Add auto-instrumentation for compose navigation ([#392](https://github.com/getsentry/sentry-android-gradle-plugin/pull/392))

### Dependencies

- Bump Android SDK from v6.6.0 to v6.7.0 ([#402](https://github.com/getsentry/sentry-android-gradle-plugin/pull/402))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#670)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.6.0...6.7.0)

## 3.2.1

### Fixes

- Ignore minified classes from any instrumentation ([#389](https://github.com/getsentry/sentry-android-gradle-plugin/pull/389))
- Fix short class names should not be flagged as minified ([#398](https://github.com/getsentry/sentry-android-gradle-plugin/pull/398))

### Dependencies

- Bump CLI from v2.7.0 to v2.8.1 ([#394](https://github.com/getsentry/sentry-android-gradle-plugin/pull/394), [#397](https://github.com/getsentry/sentry-android-gradle-plugin/pull/397))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#281)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.7.0...2.8.1)
- Bump Android SDK from v6.5.0 to v6.6.0 ([#393](https://github.com/getsentry/sentry-android-gradle-plugin/pull/393))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#660)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.5.0...6.6.0)

## 3.2.0

### Dependencies

- Bump CLI from v2.6.0 to v2.7.0 ([#383](https://github.com/getsentry/sentry-android-gradle-plugin/pull/383))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#270)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.6.0...2.7.0)
- Bump Android SDK from v6.4.3 to v6.5.0 ([#385](https://github.com/getsentry/sentry-android-gradle-plugin/pull/385))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#650)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.4.3...6.5.0)

## 3.1.7

### Features

- Bump AGP to v7.3.0 ([#378](https://github.com/getsentry/sentry-android-gradle-plugin/pull/378))
- Bump CLI from v2.5.2 to v2.6.0 ([#379](https://github.com/getsentry/sentry-android-gradle-plugin/pull/379))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#260)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.5.2...2.6.0)
- Bump Android SDK from v6.4.2 to v6.4.3 ([#384](https://github.com/getsentry/sentry-android-gradle-plugin/pull/384))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#643)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.4.2...6.4.3)

## 3.1.6

### Features

- Bump Android SDK from v6.4.0 to v6.4.2 ([#372](https://github.com/getsentry/sentry-android-gradle-plugin/pull/372), [#377](https://github.com/getsentry/sentry-android-gradle-plugin/pull/377))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#642)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.4.0...6.4.2)

### Fixes

- Update DirectoryProperty to use @InputDirectory ([#374](https://github.com/getsentry/sentry-android-gradle-plugin/pull/374))

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
