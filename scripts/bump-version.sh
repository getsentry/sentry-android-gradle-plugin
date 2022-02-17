#!/bin/bash
set -eux

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR/..

OLD_VERSION="${1}"
NEW_VERSION="${2}"

GRADLE_FILEPATH="plugin-build/gradle.properties"

# Replace `version` with the given version
VERSION_NAME_PATTERN="version"
perl -pi -e "s/$VERSION_NAME_PATTERN = .*$/$VERSION_NAME_PATTERN = $NEW_VERSION/g" $GRADLE_FILEPATH
