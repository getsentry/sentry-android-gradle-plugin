# Changelog

## Unreleased

- Migrate Dependencies to Gradle version catalog ([#712](https://github.com/getsentry/sentry-android-gradle-plugin/pull/712))

## 4.6.0

### Fixes

- Do not include `BuildConfig` into source bundles ([#705](https://github.com/getsentry/sentry-android-gradle-plugin/pull/705))
- Fix misleading auth-token error message in case "sentry-cli info" fails ([#708](https://github.com/getsentry/sentry-android-gradle-plugin/pull/708))

### Dependencies

- Bump CLI from v2.31.1 to v2.31.2 ([#702](https://github.com/getsentry/sentry-android-gradle-plugin/pull/702))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2312)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.31.1...2.31.2)
- Bump Android SDK from v7.8.0 to v7.9.0 ([#706](https://github.com/getsentry/sentry-android-gradle-plugin/pull/706))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#790)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.8.0...7.9.0)

## 4.5.1

### Fixes

- Fix auth-token error for ProGuard mapping upload, even when mapping upload is disabled (fixed with sentry-cli 2.31.1) 

### Dependencies

- Bump CLI from v2.31.0 to v2.31.1 ([#700](https://github.com/getsentry/sentry-android-gradle-plugin/pull/700))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2311)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.31.0...2.31.1)

## 4.5.0

### Fixes

- Remove excessive info logging ([#696](https://github.com/getsentry/sentry-android-gradle-plugin/pull/696))

### Dependencies

- Bump Android SDK from v7.6.0 to v7.8.0 ([#690](https://github.com/getsentry/sentry-android-gradle-plugin/pull/690))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#780)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.6.0...7.8.0)
- Bump CLI from v2.28.6 to v2.31.0 ([#684](https://github.com/getsentry/sentry-android-gradle-plugin/pull/684))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2310)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.28.6...2.31.0)

## 4.4.1

### Fixes

- Fix circular depencency issue between mergeAssets and minify tasks ([#682](https://github.com/getsentry/sentry-android-gradle-plugin/pull/682))
- Fix sentry-cli not being extracted prior to task execution ([#691](https://github.com/getsentry/sentry-android-gradle-plugin/pull/691))

## 4.4.0

### Fixes
- Do not pollute build classpath with groovy dependencies ([#677](https://github.com/getsentry/sentry-android-gradle-plugin/pull/677))
- Ensure sentry-cli works well with configuration cache ([#675](https://github.com/getsentry/sentry-android-gradle-plugin/pull/675))
- Fix circular task dependency in combination with DexGuard plugin ([#678](https://github.com/getsentry/sentry-android-gradle-plugin/pull/678))

### Dependencies

- Bump Android SDK from v7.5.0 to v7.6.0 ([#671](https://github.com/getsentry/sentry-android-gradle-plugin/pull/671))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#760)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.5.0...7.6.0)

## 4.3.1

### Fixes

- Do not pollute build classpath with groovy dependencies ([#660](https://github.com/getsentry/sentry-android-gradle-plugin/pull/660))
- Do not execute `minifyWithR8` task when running tests with `isIncludeAndroidResources` enabled ([#662](https://github.com/getsentry/sentry-android-gradle-plugin/pull/662))
- Make tasks with reproducible inputs Cacheable ([#664](https://github.com/getsentry/sentry-android-gradle-plugin/pull/664))
  - `SentryGenerateIntegrationListTask`
  - `SentryGenerateDebugMetaPropertiesTask`
  - `GenerateBundleIdTask`
  - `SentryGenerateProguardUuidTask`

### Dependencies

- Bump CLI from v2.28.0 to v2.28.6 ([#655](https://github.com/getsentry/sentry-android-gradle-plugin/pull/655), [#657](https://github.com/getsentry/sentry-android-gradle-plugin/pull/657))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2286)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.28.0...2.28.6)
- Bump Android SDK from v7.3.0 to v7.4.0 ([#659](https://github.com/getsentry/sentry-android-gradle-plugin/pull/659))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#740)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.3.0...7.4.0)

## 4.3.0

### Fixes

- Bundle ASM version within the plugin and bump it to `9.4` ([#637](https://github.com/getsentry/sentry-android-gradle-plugin/pull/637))
  - This should fix the `java.lang.AssertionError: Class with incorrect id found` exception when using `kotlinx.serialization`
- Fall back to `findTask` if `assembleProvider` of AndroidVariant is null when hooking source bundle and native symbols upload tasks ([#639](https://github.com/getsentry/sentry-android-gradle-plugin/pull/639))
- Hook source context tasks to also run after `install{Variant}` tasks ([#643](https://github.com/getsentry/sentry-android-gradle-plugin/pull/643))
- Do not run sentry-cli commands if telemetry is disabled ([#648](https://github.com/getsentry/sentry-android-gradle-plugin/pull/648))
- Proguard and source context tasks don't run on every build ([#634](https://github.com/getsentry/sentry-android-gradle-plugin/pull/634))
  - Proguard UUID task now depends on the proguard mapping file. I.e. it will only run if the mapping file has changed
  - Source context tasks now depend on source file changes, if there are no source changes, the tasks won't run

### Dependencies

- Bump CLI from v2.25.0 to v2.28.0 ([#638](https://github.com/getsentry/sentry-android-gradle-plugin/pull/638), [#640](https://github.com/getsentry/sentry-android-gradle-plugin/pull/640), [#642](https://github.com/getsentry/sentry-android-gradle-plugin/pull/642), [#647](https://github.com/getsentry/sentry-android-gradle-plugin/pull/647), [#652](https://github.com/getsentry/sentry-android-gradle-plugin/pull/652))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2280)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.25.0...2.28.0)
  Bump Android SDK from v7.2.0 to v7.3.0 ([#646](https://github.com/getsentry/sentry-android-gradle-plugin/pull/646))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#730)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.2.0...7.3.0)

## 4.2.0

### Features

- Consider `sentry-bom` version when auto-installing integrations and the SDK ([#625](https://github.com/getsentry/sentry-android-gradle-plugin/pull/625)) 

### Fixes

- Support Room kotlin codegen ([#630](https://github.com/getsentry/sentry-android-gradle-plugin/pull/630))
- Make sentry-cli path calculation configuration-cache compatible ([#631](https://github.com/getsentry/sentry-android-gradle-plugin/pull/631))
  - This will prevent build from failing when e.g. switching branches with stale configuration cache
- Fix `FacebookInitProvider` instrumentation ([#633](https://github.com/getsentry/sentry-android-gradle-plugin/pull/633))

### Dependencies

- Bump CLI from v2.23.1 to v2.25.0 ([#622](https://github.com/getsentry/sentry-android-gradle-plugin/pull/622), [#624](https://github.com/getsentry/sentry-android-gradle-plugin/pull/624), [#629](https://github.com/getsentry/sentry-android-gradle-plugin/pull/629))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2250)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.23.1...2.25.0)
- Bump Android SDK from v7.1.0 to v7.2.0 ([#632](https://github.com/getsentry/sentry-android-gradle-plugin/pull/632))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#720)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.1.0...7.2.0)

## 4.1.1

### Fixes

- Fix VerifyError when optimized code is instrumented ([#619](https://github.com/getsentry/sentry-android-gradle-plugin/pull/619))

### Dependencies

- Bump CLI from v2.23.0 to v2.23.1 ([#615](https://github.com/getsentry/sentry-android-gradle-plugin/pull/615))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2231)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.23.0...2.23.1)

## 4.1.0

### Features

- Do not consider user-defined sentry versions when auto-installing integrations. This is necessary because we want to align integrations versions to the same one as one of `sentry-android-core`/`sentry`/`sentry-android`/`sentry-spring-boot` to prevent runtime crashes due to binary incompatibility. ([#602](https://github.com/getsentry/sentry-android-gradle-plugin/pull/602))
    - If you have directly defined one of the core versions, we will use that to install integrations, otherwise `autoInstallation.sentryVersion` or the default bundled SDK version is used.

This means if you have defined something like that:
```kotlin
// direct deps
dependencies {
  implementation("io.sentry:sentry-android-core:7.0.0")
  implementation("io.sentry:sentry-android-okhttp:6.34.0")
}

// or with the gradle plugin
sentry {
  autoInstallation.sentryVersion = '7.0.0' // or the latest version bundled within the plugin
}

dependencies {
  implementation("io.sentry:sentry-android-okhttp:6.34.0")
}
```

Then in both cases it will use `7.0.0` when installing the `sentry-android-okhttp` integration and print a warning that we have overridden the version.

- Add aarch64 sentry-cli ([#611](https://github.com/getsentry/sentry-android-gradle-plugin/pull/611))
    - This is used when the build is executed inside a docker container on an Apple silicon chip (e.g. M1)

- Instrument ContentProvider/Application onCreate calls to measure app-start performance ([#565](https://github.com/getsentry/sentry-android-gradle-plugin/pull/565))
    - This feature requires the `sentry-java` SDK version `7.1.0` and is enabled by default
    - To disable the feature, set `sentry.tracingInstrumentation.appStart.enabled` to `false`
```kotlin
sentry {
  tracingInstrumentation {
    appStart {
      enabled.set(false)
    }
  }
}
```

### Fixes

- Fix sentry-cli url parameter position ([#610](https://github.com/getsentry/sentry-android-gradle-plugin/pull/610))

### Dependencies

- Bump CLI from v2.22.3 to v2.23.0 ([#607](https://github.com/getsentry/sentry-android-gradle-plugin/pull/607))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2230)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.22.3...2.23.0)
- Bump Android SDK from v7.0.0 to v7.1.0 ([#612](https://github.com/getsentry/sentry-android-gradle-plugin/pull/612))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#710)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.0.0...7.1.0)

## 4.0.0

Version 4 of the Sentry Android Gradle plugin brings a variety of features and fixes. The most notable changes are:
- Bump Sentry Android SDK to `7.0.0`. Please, refer to the [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#700) of the SDK for more details
- Rename `experimentalGuardsquareSupport` flag to `dexguardEnabled`
- Add new `excludes` option to allow excluding certain classes from instrumentation. It's available under the `sentry.tracingInstrumentation` extension

## Sentry Android SDK Compatibility

Make sure to use Sentry Gradle plugin 4.+ together with the Sentry Android SDK 7.+, otherwise it might crash at runtime due to binary incompatibility. (E.g. if you're using `-timber`, `-okhttp` or other packages)

If you can't do that for some reason, you can specify sentry version via the plugin config block:

```kotlin
sentry {
  autoInstallation {
    sentryVersion.set("7.0.0")
  }
}
```

Similarly, if you have a Sentry SDK (e.g. `sentry-android-core`) dependency on one of your Gradle modules and you're updating it to 7.+, make sure the Gradle plugin is at 4.+ or specify the SDK version as shown in the snippet above.

## Breaking Changes

- Rename `experimentalGuardsquareSupport` flag to `dexguardEnabled` ([#589](https://github.com/getsentry/sentry-android-gradle-plugin/pull/589))
- Bump Sentry Android SDK to `7.0.0`

## Other Changes

### Features

- Print a warning if the Sentry plugin is not applied on the app module ([#586](https://github.com/getsentry/sentry-android-gradle-plugin/pull/586))
- Add new `excludes` option to exclude classes from instrumentation ([#590](https://github.com/getsentry/sentry-android-gradle-plugin/pull/590))
- Send telemetry data for plugin usage ([#582](https://github.com/getsentry/sentry-android-gradle-plugin/pull/582))
  - This will collect errors and timings of the plugin and its tasks (anonymized, except the sentry org id), so we can better understand how the plugin is performing. If you wish to opt-out of this behavior, set `telemetry = false` in the `sentry` plugin configuration block.

### Chores

- Change cli command from `upload-dif` to `debug-files upload` for native symbols ([#587](https://github.com/getsentry/sentry-android-gradle-plugin/pull/587))
- Use new AGP api for native symbols upload ([#592](https://github.com/getsentry/sentry-android-gradle-plugin/pull/592))

### Dependencies

- Bump Android SDK from v6.32.0 to v7.0.0 ([#588](https://github.com/getsentry/sentry-android-gradle-plugin/pull/588), [#593](https://github.com/getsentry/sentry-android-gradle-plugin/pull/593), [#597](https://github.com/getsentry/sentry-android-gradle-plugin/pull/597))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#700)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.32.0...7.0.0)
- Bump CLI from v2.21.2 to v2.22.3 ([#598](https://github.com/getsentry/sentry-android-gradle-plugin/pull/598), [#600](https://github.com/getsentry/sentry-android-gradle-plugin/pull/600))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2223)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.21.2...2.22.3)

## 3.14.0

### Features

- Add `url` option which is passed through to `sentry-cli` ([#572](https://github.com/getsentry/sentry-android-gradle-plugin/pull/572))
  - In case you are self hosting Sentry, you can set `url` to your self hosted instance if your org auth token does not contain a URL
- Provide detailed message for failed sentry-cli API requests ([#576](https://github.com/getsentry/sentry-android-gradle-plugin/pull/576)) 

### Fixes

- Use `spring-boot` instead of `spring-boot-starter` for auto install detection ([#543](https://github.com/getsentry/sentry-android-gradle-plugin/pull/543))
- Fix tracing instrumentation not working when configuration-cache is enabled on Gradle 8+ ([#568](https://github.com/getsentry/sentry-android-gradle-plugin/pull/568))
- Fix source context not working with configuration cache enabled on Gradle 8+ ([#570](https://github.com/getsentry/sentry-android-gradle-plugin/pull/570))
- Make proguard release association backward-compatible ([#576](https://github.com/getsentry/sentry-android-gradle-plugin/pull/576))

### Dependencies

- Bump CLI from v2.21.1 to v2.21.2 ([#569](https://github.com/getsentry/sentry-android-gradle-plugin/pull/569))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2212)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.21.1...2.21.2)
- Bump Android SDK from v6.30.0 to v6.32.0 ([#573](https://github.com/getsentry/sentry-android-gradle-plugin/pull/573), [#581](https://github.com/getsentry/sentry-android-gradle-plugin/pull/581))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6320)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.30.0...6.32.0)

## 3.13.0

### Features

- Auto install `sentry-quartz` if `quartz` is installed ([#562](https://github.com/getsentry/sentry-android-gradle-plugin/pull/562))

### Internal

- Change room span op name to `db.sql.room` ([#557](https://github.com/getsentry/sentry-android-gradle-plugin/pull/557))

### Fixes

- Reduce log level from warn to info for some auto install messages ([#553](https://github.com/getsentry/sentry-android-gradle-plugin/pull/553))
  - There was some confusion especially around our Spring and Spring Boot integrations where we offer a different set of dependencies for Spring 5 (`sentry-spring`), Spring 6 (`sentry-spring-jakarta`), Spring Boot 2 (`sentry-spring-boot`) and Spring Boot 3 (`sentry-spring-boot-jakarta`) where there's always going to be one that's installed and one that's not installed.

### Dependencies

- Bump CLI from v2.20.3 to v2.21.1 ([#540](https://github.com/getsentry/sentry-android-gradle-plugin/pull/540), [#545](https://github.com/getsentry/sentry-android-gradle-plugin/pull/545), [#550](https://github.com/getsentry/sentry-android-gradle-plugin/pull/550), [#556](https://github.com/getsentry/sentry-android-gradle-plugin/pull/556), [#561](https://github.com/getsentry/sentry-android-gradle-plugin/pull/561))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2211)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.20.3...2.21.1)
- Bump Android SDK from v6.28.0 to v6.30.0 ([#555](https://github.com/getsentry/sentry-android-gradle-plugin/pull/555), [#563](https://github.com/getsentry/sentry-android-gradle-plugin/pull/563))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6300)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.28.0...6.30.0)

## 3.12.0

### Features

- Add release information args to proguard mapping upload task ([#476](https://github.com/getsentry/sentry-android-gradle-plugin/pull/476))
  - Requires Sentry version `>=23.7.2` if you're using self-hosted
- Auto install Sentry integrations for Java Backend, Desktop, etc. ([#521](https://github.com/getsentry/sentry-android-gradle-plugin/pull/521))
- Use Spring Boot autoconfigure modules (`sentry-spring-boot` and `sentry-spring-boot-jakarta`) for auto install ([#542](https://github.com/getsentry/sentry-android-gradle-plugin/pull/542))

### Fixes

- Disable source context tasks if not enabled ([#536](https://github.com/getsentry/sentry-android-gradle-plugin/pull/536))

### Dependencies

- Bump CLI from v2.19.1 to v2.20.3 ([#520](https://github.com/getsentry/sentry-android-gradle-plugin/pull/520), [#531](https://github.com/getsentry/sentry-android-gradle-plugin/pull/531), [#537](https://github.com/getsentry/sentry-android-gradle-plugin/pull/537))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2203)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.19.1...2.20.3)
- Bump Android SDK from v6.22.0 to v6.28.0 ([#527](https://github.com/getsentry/sentry-android-gradle-plugin/pull/527), [#528](https://github.com/getsentry/sentry-android-gradle-plugin/pull/528), [#532](https://github.com/getsentry/sentry-android-gradle-plugin/pull/532), [#541](https://github.com/getsentry/sentry-android-gradle-plugin/pull/541))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6280)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.22.0...6.28.0)

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
    - Requires Sentry version `>=23.5.0` if you're using self-hosted
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

### Features

- Bump AGP to 7.2.1 and Gradle to 7.5.0 ([#363](https://github.com/getsentry/sentry-android-gradle-plugin/pull/363))
- Bump Android SDK to v6.3.1 ([#365](https://github.com/getsentry/sentry-android-gradle-plugin/pull/365))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#631)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.3.0...6.3.1)
- Bump CLI to v2.5.0 ([#358](https://github.com/getsentry/sentry-android-gradle-plugin/pull/358))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#250)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.4.1...2.5.0)

### Fixes

- Detect minified classes and skip instrumentation to avoid build problems ([#362](https://github.com/getsentry/sentry-android-gradle-plugin/pull/362))

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
