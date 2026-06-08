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

Some tests upload mappings/source context and fail without an auth token:

```bash
export SENTRY_AUTH_TOKEN=<your_token>
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

- **Changelog is enforced** by [dangerfile.js](dangerfile.js): every PR must add an entry
  to the "Unreleased" section of [CHANGELOG.md](CHANGELOG.md) that references the PR
  number (e.g. `- Fix X ([#1234](...))`). To opt out, add `#skip-changelog` to the PR
  description. The entry needs the PR number, which you can get with
  `gh pr view --json number -q '.number'` (or `gh pr view <branch> ...`).
- `feat:` PRs get a reminder to update [sentry-docs](https://github.com/getsentry/sentry-docs).
- Commit subjects use conventional-commit style with a scope, e.g. `fix(snapshots): ...`,
  `build(deps): ...`.
