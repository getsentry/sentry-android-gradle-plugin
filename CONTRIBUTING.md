# Contributing to sentry-android-gradle-plugin

We love pull requests from everyone. 
We suggest opening an issue to discuss bigger changes before investing on a big PR.

# Requirements

The project currently requires you run JDK version `17` and the Android SDK.

# Overriding `sentry-cli` for local development

If you want to use a local version of the sentry-cli for testing integration with the plugin, you can do so by setting the `cli.executable` property in the `sentry.properties` file of the target project.

Example `sentry.properties` file:

```properties
cli.executable=/path/to/your/local/sentry-cli
```


# Tests

When running tests locally, some tests might fail due to failed upload of proguard mappings/source
contexts. This is because of the missing auth token, make sure to export a new env variable containing
your token:

```bash
export SENTRY_AUTH_TOKEN=<your_token>
```

# CI

Build and tests are automatically run against branches and pull requests
via GH Actions.

# Debugging the plugin

Set breakpoints in the plugin code like you normally would. Then run a build from the command line
(we are using `android-instrumentation-sample` as an example, as it's the most complete sample):

```bash
$ ./gradlew :examples:android-instrumentation-sample:assembleDebug -Dorg.gradle.debug=true --no-daemon
```

It will probably look like it's hanging. You now have to create a new run configuration in IDEA. 
Click the *Edit configurations* button, and then the *+* button to add a configuration, and then choose the *Remote* template. 
Name this configuration something like "GradleDebug" and click OK. Now, click the debug button and IDEA will connect to the gradle build you started from the command line. 
You should see it hitting your breakpoints.

> if it seems like your breakpoints aren't being hit, and these are in task actions, it might be that the tasks are up to date or coming from the build cache (if build cache is enabled). In this case, run a `clean` and then use `--no-build-cache` when you run your debug build:

```bash
$ ./gradlew clean && ./gradlew :examples:android-instrumentation-sample:assembleDebug --no-build-cache -Dorg.gradle.debug=true --no-daemon
```

Another possibility is that gradle is just broken somehow, in which case

```bash
$ ./gradlew --stop
```

and try again.
