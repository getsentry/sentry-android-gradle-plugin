# AGENTS.md

Guidance for AI agents and new contributors working in this repo. For human-facing
setup and debugging instructions, see [CONTRIBUTING.md](CONTRIBUTING.md).

## What this is

The Sentry Android Gradle Plugin (SAGP). It uploads ProGuard/R8 mappings, debug files,
and source context, and performs bytecode instrumentation (e.g. Room, OkHttp, file I/O).

## Repository layout

This is a **Gradle composite build**, not a single project. The root
`settings.gradle.kts` orchestrates the sample apps under `examples/` and pulls in three
separate builds via `includeBuild`:

- `plugin-build/` — the actual plugin source lives here, under `plugin-build/src`. It has
  its own `settings.gradle.kts` and `gradle.properties`. Look here first for plugin code.
- `sentry-kotlin-compiler-plugin/` — Kotlin compiler plugin which performs Jetpack Compose instrumentation, substituted into samples
  without publishing.
- `sentry-snapshots-runtime/` — snapshots runtime, also substituted into samples.

`buildSrc/` holds version and dependency definitions (`Dependencies.kt`, plugin versions).

## Requirements

- JDK **17** and the Android SDK.

## Common commands

Run from the repo root (the root build delegates into the included builds):

- `make format` → `./gradlew spotlessApply` — apply formatting (ktfmt, Google style).
  Run this before committing; `spotlessCheck` runs in CI.
- `make preMerge` → `./gradlew preMerge --continue` — the full local verification that
  mirrors CI: `check` on the root, the compiler plugin, the snapshots runtime, and each
  sample app.
- `./gradlew integrationTest` — runs the plugin's integration tests (publishes the
  compiler plugin to a local test repo first).
- Plugin unit tests live in `plugin-build`: `./gradlew :plugin-build:test` (or via the
  `plugin-build` included build).
- `plugin-build` pins its full dependency graph (lockfile + SHA-256 verification metadata)
  in STRICT mode. Adding, removing, or bumping a dependency there fails the build until you
  regenerate both:
  `./gradlew -p plugin-build resolveAndLockAll --write-locks --write-verification-metadata sha256`.
  See [CONTRIBUTING.md](CONTRIBUTING.md) for details.
- `sentry-kotlin-compiler-plugin` verifies its dependencies with PGP signatures
  (`sentry-kotlin-compiler-plugin/gradle/verification-metadata.xml` plus an armored keyring).
  Regenerate with
  `./gradlew -p sentry-kotlin-compiler-plugin resolveAll spotlessCheck --write-verification-metadata pgp,sha256 --export-keys`,
  which is idempotent and preserves three deliberately narrowed trust scopes — don't widen those
  back. Verification only applies when that build runs standalone
  (`./gradlew -p sentry-kotlin-compiler-plugin ...`), not via the root composite, which is why
  CI has a dedicated `verify-compiler-plugin-dependencies` job. See
  [CONTRIBUTING.md](CONTRIBUTING.md).

Some tests upload mappings/source context and fail without an auth token:

```bash
export SENTRY_AUTH_TOKEN=<your_token>
```

## Testing conventions

- Prefer [Google Truth](https://truth.dev/) for assertions in new unit tests
  (`import com.google.common.truth.Truth.assertThat`). Much of the existing suite still
  uses `kotlin.test`/JUnit assertions; don't rewrite those wholesale, but reach for Truth
  when adding new tests or touching assertions in a test you're already editing.

```kotlin
import com.google.common.truth.Truth.assertThat

assertThat(actual).isEqualTo(expected)
assertThat(list).containsExactly("a", "b").inOrder()
```

## Local sentry-cli

To test against a local `sentry-cli`, set `cli.executable` in the target project's
`sentry.properties`:

```properties
cli.executable=/path/to/your/local/sentry-cli
```

## Debugging the plugin

Run a sample build with the debug agent attached, then connect a Remote JVM debug
configuration from the IDE:

```bash
./gradlew :examples:android-instrumentation-sample:assembleDebug -Dorg.gradle.debug=true --no-daemon
```

If breakpoints in task actions don't hit, the tasks may be up-to-date or served from the
build cache — `./gradlew clean` and add `--no-build-cache`.

## Pull request conventions

- **Don't edit [CHANGELOG.md](CHANGELOG.md) by hand.** Craft generates each release
  section from the PR titles merged since the last release, using the categories in
  [.github/release.yml](.github/release.yml). A bot comments a preview of the entry on
  every PR.
- Because the PR title *is* the changelog entry, write it for users, in
  conventional-commit style with a scope: `fix(snapshots): ...`, `build(deps): ...`.
  The type and scope are stripped from the rendered entry.
- To write an entry that differs from the title, add a `### Changelog Entry` section to
  the PR description; its contents are used verbatim. Nested bullets are preserved.
- To leave a PR out of the changelog, add `#skip-changelog` to its description or apply
  the `skip-changelog` label.
- `feat:` PRs should update [sentry-docs](https://github.com/getsentry/sentry-docs).
