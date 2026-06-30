# Contributing to sentry-android-gradle-plugin

We love pull requests from everyone. 
We suggest opening an issue to discuss bigger changes before investing on a big PR.

# Requirements

The project currently requires you run JDK version `17` and the Android SDK.

# Updating dependencies in `plugin-build`

The published plugin build (`plugin-build`) pins its full transitive
dependency graph for supply-chain hardening: resolved versions are recorded
in `plugin-build/gradle.lockfile` and a SHA-256 checksum for every artifact
in `plugin-build/gradle/verification-metadata.xml`. Locking runs in STRICT
mode, so any drift fails the build.

Whenever you add, remove, or bump a dependency in
`plugin-build/build.gradle.kts`, regenerate both files and commit them:

```bash
./gradlew -p plugin-build resolveAndLockAll --write-locks --write-verification-metadata sha256
```

Review the diff before committing — new transitive artifacts should look like
they belong. The compatibility test matrix overrides AGP/Kotlin/Gradle
versions via env vars and deliberately skips locking, so you only need to
regenerate against the canonical build.

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


# AI Use

You are welcome to use whatever tools you prefer for making a contribution. However, any changes you propose have to be reviewed and tested by you, a human, first, before you submit a pull request with them for the Sentry team to review. If we feel like that did not happen, we will close the PR outright. For example, we will not review visibly AI-generated PRs from an agent instructed to look for and "fix" open issues in the repo. This aligns with our SDK principle: [every line has an owner](https://develop.sentry.dev/sdk/getting-started/principles/#every-line-has-an-owner).
