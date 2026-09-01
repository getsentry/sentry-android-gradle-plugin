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

# Dependency verification in `sentry-kotlin-compiler-plugin`

The compiler plugin build verifies its dependencies with PGP signatures
rather than checksums. Trust lives in
`sentry-kotlin-compiler-plugin/gradle/verification-metadata.xml`, and the
public keys in `verification-keyring.keys` next to it, kept in armored
(text) form so the keyring diffs readably.

Trusting a publisher's key instead of an artifact's checksum means a version
bump usually changes nothing here. That is the point: a diff to these files
should be rare enough that it gets read. Checksums remain as the fallback for
the handful of artifacts nobody signs — mostly the Gradle Plugin Portal
marker POMs — and those do change on every bump of the module in question.

Key servers are disabled. They are slow, they rate-limit, and a key Gradle
fetches silently at build time is a trust decision no human made. Keys are
added deliberately instead.

## Regenerating

```bash
./gradlew -p sentry-kotlin-compiler-plugin resolveAll spotlessCheck \
  --write-verification-metadata pgp,sha256 --export-keys
```

This is idempotent, and preserves both the explanatory header comment and any
entry already in the file. That last part matters: two trust scopes are
deliberately narrower than the ones Gradle's bootstrap infers, and
regenerating keeps them. Don't widen them back.

| Key | What the bootstrap infers | Why it's narrowed |
| --- | --- | --- |
| JetBrains Compose | all of `org.jetbrains` | would let it vouch for Kotlin itself |
| Error Prone | all of `com.google` | would cover Guava, Gson and AutoService |

`spotlessCheck` is in the command because Spotless resolves ktfmt through a
detached configuration only when the task actually runs; `resolveAll` alone
never sees it and silently leaves those artifacts out of the metadata.

Review the diff before committing. New keys should belong to someone you can
identify — the keyring's `pub`/`uid` headers name most of them, and
`keyserver.ubuntu.com` covers the rest:

```bash
curl -s "https://keyserver.ubuntu.com/pks/lookup?op=index&options=mr&search=0x<FINGERPRINT>"
```

For a new signed dependency, add its key to the keyring first. With key
servers off the bootstrap cannot fetch it, and will quietly fall back to a
checksum instead:

```bash
curl -s "https://keyserver.ubuntu.com/pks/lookup?op=get&options=mr&search=0x<FINGERPRINT>" \
  >> sentry-kotlin-compiler-plugin/gradle/verification-keyring.keys
```

## Where it applies

Dependency verification is scoped to the root of a build tree, so an included
build's own configuration is ignored. Running `preMerge` from the repo root
therefore does *not* verify this build — it only takes effect when the
compiler plugin is built on its own:

```bash
./gradlew -p sentry-kotlin-compiler-plugin resolveAll spotlessCheck
```

No PR job runs that today, so the first thing to actually verify these
dependencies is the snapshot publish, which builds the compiler plugin from
its own directory. Run the command above before pushing a dependency change.

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
