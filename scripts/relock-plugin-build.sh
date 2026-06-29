#!/usr/bin/env bash
set -euo pipefail

# Regenerate the plugin-build dependency lockfile and SHA-256 verification metadata.
# Run after bumping a plugin-build dependency, otherwise STRICT-mode locking rejects
# the new version. See CONTRIBUTING.md.
cd "$(dirname "$0")/.."

./gradlew -p plugin-build resolveAndLockAll --write-locks --write-verification-metadata sha256
