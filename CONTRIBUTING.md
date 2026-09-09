# Contributing to sentry-android-gradle-plugin

We love pull requests from everyone. 
We suggest opening an issue to discuss bigger changes before investing on a big PR.

# Requirements

The project currently requires you run JDK version `17` and the Android SDK.

# Dependency verification

Both published builds, `plugin-build` and `sentry-kotlin-compiler-plugin`, verify every
artifact they resolve. They follow the same principles:

- **Trust PGP keys, not checksums.** Trust lives in each build's
  `gradle/verification-metadata.xml`, with the public keys in
  `verification-keyring.keys` beside it. Trusting a publisher's key instead of an
  artifact's checksum means a version bump usually changes nothing in these files. That is
  the point: a diff here should be rare enough that it gets read. Checksums remain as the
  fallback for the handful of artifacts nobody signs, mostly Gradle Plugin Portal marker
  POMs, and those do change on every bump of the module in question.
- **Keyrings are armored.** The text format diffs readably; the binary `.gpg` form does
  not, and a keyring nobody can review is not doing much.
- **Keys are added by hand.** A key Gradle fetches on its own is a trust decision no human
  made, so verification runs with key servers off. Regeneration records a `trusted-key` entry
  for a new signer but never adds the key itself, so the build fails until someone adds it.
  That failure is the review gate.
- **Trust is scoped to what a key actually signs.** A key that belongs to a publisher may
  vouch for that publisher's own group tree; an individual maintainer's key gets only the
  groups it signs. Gradle's bootstrap generalizes more freely than that, so each file
  carries a few deliberately narrowed scopes, listed in its header comment. Regeneration
  preserves them. Don't widen them back.
- **Resolution has to be reproducible.** Verification metadata is generated from a
  resolved graph, so if the graph can move on its own, the metadata records something that
  may not exist tomorrow. `plugin-build` pins its graph with a lockfile;
  `sentry-kotlin-compiler-plugin` uses `failOnNonReproducibleResolution()`, which rejects
  dynamic versions, changing versions and ranges anywhere in the graph, transitives
  included. Gradle refuses to enable both at once.

## Regenerating

`plugin-build` additionally records its resolved versions in `plugin-build/gradle.lockfile`
and runs locking in STRICT mode, so any drift fails the build. Whenever you add, remove or
bump one of its dependencies, regenerate the lockfile and the metadata together and commit
both:

```bash
scripts/relock-plugin-build.sh
```

For the compiler plugin:

```bash
./gradlew -p sentry-kotlin-compiler-plugin resolveAll spotlessCheck \
  --write-verification-metadata pgp,sha256 --export-keys
```

`--export-keys` tidies a hand-appended key into Gradle's own layout. The relock script omits it
because CI runs that one unattended; run `./gradlew -p plugin-build --export-keys` yourself when
plugin-build's keyring needs it.

Both commands are idempotent, and preserve the explanatory header comment along with every
entry already in the file, including the narrowed trust scopes:

| Build | Key | What the bootstrap infers | Why it's narrowed |
| --- | --- | --- | --- |
| both | Ktfmt Team | all of `com.facebook` | would cover every artifact Facebook publishes |
| plugin-build | Guava release | all of `com.google` | would cover Tink, Dagger, protobuf, Gson |
| plugin-build | Chris Povirk | all of `com.google` | signs Guava, J2ObjC and Truth only |
| plugin-build | Éamonn McManus | all of `com.google` | signs Gson and the Auto\* projects only |
| plugin-build | JAXB release | all of `com.sun` | would cover everything Oracle publishes there |
| compiler plugin | JetBrains Compose | all of `org.jetbrains` | would let it vouch for Kotlin itself |
| compiler plugin | Error Prone | all of `com.google` | would cover Guava, Gson and AutoService |

`spotlessCheck` is in both commands because Spotless resolves ktfmt through a detached
configuration only when the task actually runs; resolving the declared configurations alone
never sees it and silently leaves those artifacts out of the metadata.

Keep any commentary in that header. Gradle rewrites the document body on every regeneration
and drops comments placed between entries, so a note next to a `trusted-key` disappears the
next time anyone relocks.

Review the diff before committing. New transitive artifacts should look like they belong,
and a new key should belong to someone you can identify — the keyring's `pub`/`uid` headers
name most of them, and `keyserver.ubuntu.com` covers the rest:

```bash
curl -s "https://keyserver.ubuntu.com/pks/lookup?op=index&options=mr&search=0x<FINGERPRINT>"
```

For a new signed dependency, add its key to the keyring yourself — regeneration records the
`trusted-key` entry but not the key, and the build fails until you do:

```bash
curl -s "https://keyserver.ubuntu.com/pks/lookup?op=get&options=mr&search=0x<FINGERPRINT>" \
  >> plugin-build/gradle/verification-keyring.keys
```

Some keys are only on `keys.openpgp.org`; fetch those with
`https://keys.openpgp.org/vks/v1/by-keyid/<LONG_KEY_ID>`. Then rerun the regeneration command
and check that the artifact verifies against a `trusted-key` entry rather than a checksum.

The compatibility test matrix overrides AGP/Kotlin/Gradle versions via env vars and
deliberately skips locking, so you only need to regenerate against the canonical build.

## Where it applies

Dependency verification is scoped to the root of a build tree, so an included build's own
configuration is ignored. Running `preMerge` from the repo root therefore verifies neither
build — it only takes effect when each is built on its own:

```bash
./gradlew -p plugin-build resolveAll spotlessCheck
./gradlew -p sentry-kotlin-compiler-plugin resolveAll spotlessCheck
```

The `verify-plugin-build-dependencies` and `verify-compiler-plugin-dependencies` jobs in
[pre-merge.yaml](.github/workflows/pre-merge.yaml) run exactly that on every PR, so a stale
entry fails there rather than in the snapshot publish. Run the commands above locally before
pushing a dependency change.

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
