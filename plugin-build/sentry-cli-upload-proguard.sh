#!/usr/bin/env bash

export SENTRY_URL=http://127.0.0.1:8000
export SENTRY_AUTH_TOKEN=dummy-auth-token
export SENTRY_ORG=sentry-sdks
export SENTRY_PROJECT=sentry-fastlane-plugin
export SENTRY_LOG_LEVEL=info

sentry-cli upload-proguard --android-manifest plugin-build/src/test/resources/testFixtures/integration/AndroidManifest.xml plugin-build/src/test/resources/testFixtures/integration/AndroidExample.mapping.txt