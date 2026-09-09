#!/usr/bin/env bash
set -euo pipefail

# Regenerate the plugin-build dependency lockfile and verification metadata.
# Run after bumping a plugin-build dependency, otherwise STRICT-mode locking rejects
# the new version. See CONTRIBUTING.md.
#
# No --export-keys: CI runs this unattended and the keyring is a trust store. Regeneration
# records a trusted-key entry for a new signer but never adds the key itself, so the build
# fails until someone adds it by hand. To tidy the keyring afterwards, run
#   ./gradlew -p plugin-build --export-keys
cd "$(dirname "$0")/.."

./gradlew -p plugin-build resolveAndLockAll spotlessCheck \
  --write-locks --write-verification-metadata pgp,sha256
