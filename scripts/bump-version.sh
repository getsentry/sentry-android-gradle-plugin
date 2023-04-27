#!/bin/bash
set -eux

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR/..

OLD_VERSION="${1}"
NEW_VERSION="${2}"

# composite builds do not share properties, thus we need to update all relevant property files
# Replace `version` with the given version

GRADLE_FILEPATH="plugin-build/gradle.properties"
VERSION_NAME_PATTERN="version"
perl -pi -e "s/^$VERSION_NAME_PATTERN = .*$/$VERSION_NAME_PATTERN = $NEW_VERSION/g" $GRADLE_FILEPATH

GRADLE_FILEPATH="sentry-kotlin-compiler-plugin/gradle.properties"
VERSION_NAME_PATTERN="VERSION_NAME"
perl -pi -e "s/^$VERSION_NAME_PATTERN = .*$/$VERSION_NAME_PATTERN = $NEW_VERSION/g" $GRADLE_FILEPATH
