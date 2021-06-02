# Unreleased

# 2.0.0

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

# 2.0.0-beta.3

* Enhancement: Clean up deprecated/removed Dex and Transform tasks (#130)

# 2.0.0-beta.2

* Enhancement: Use pluginManager instead of project.afterEvaluate (#119)
* Enhancement: Use assembleTaskProvider lazily (#121)
* Enhancement: Use packageProvider lazily (#125)
* Enhancement: Use mappingFileProvider lazily (#128)

# 2.0.0-beta.1

* Feat: Support Configuration Avoidance (#112)
* Fix: Silence the warning for missing mapping file on variants that don't enable minification (#111)
* Bump: sentry-cli to 1.64.1

# 2.0.0-alpha.3

* Fix: Only wire upload mapping task if minifyEnabled (#86) @cerisier

# 2.0.0-alpha.2

* Fix: Publish Plugin Marker on maven central @marandaneto

# 2.0.0-alpha.1

* Feat: Gradle plugin v2 (#50) @cortinico
* Enhancement: Allow module level sentry properties file (#33) @MatthewTPage

# 1.x

* See GH releases https://github.com/getsentry/sentry-android-gradle-plugin/releases
