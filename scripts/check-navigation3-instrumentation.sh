#!/usr/bin/env bash
#
# Checks that the newest Navigation 3 release still has the shape the Sentry Android Gradle
# Plugin's NAVIGATION3 instrumentation relies on (see Navigation3Display):
#
#   1. navigation3-ui's NavDisplayKt__NavDisplayKt calls rememberDecoratedNavEntries(backStack,
#      entryDecorators, entryProvider) exactly once.
#   2. navigation3-runtime still declares that rememberDecoratedNavEntries overload with the same
#      compiled signature, which Sentry's wrapper mirrors.
#
# If either check fails, the instrumentation silently stops applying for that release, so this
# runs on a schedule (.github/workflows/check-navigation3-instrumentation.yml).
#
# Usage: scripts/check-navigation3-instrumentation.sh [navigation3-version]
# Defaults to the newest published version, pre-releases included. Requires curl, unzip, javap.

set -euo pipefail

MAVEN="https://dl.google.com/android/maven2/androidx/navigation3"
DESCRIPTOR="(Ljava/util/List;Ljava/util/List;Lkotlin/jvm/functions/Function1;Landroidx/compose/runtime/Composer;II)Ljava/util/List;"
CALL="Method androidx/navigation3/runtime/DecoratedNavEntriesKt.rememberDecoratedNavEntries:${DESCRIPTOR}"

latest_version() {
  curl -sSf "$MAVEN/$1/maven-metadata.xml" |
    sed -n 's:.*<version>\(.*\)</version>.*:\1:p' |
    tail -n 1
}

# Downloads an artifact's AAR and extracts one class from its classes.jar into $work.
extract_class() {
  local artifact=$1 version=$2 class=$3
  local dir="$work/$artifact"
  mkdir -p "$dir"
  curl -sSf -o "$dir/$artifact.aar" "$MAVEN/$artifact/$version/$artifact-$version.aar"
  unzip -q -o "$dir/$artifact.aar" classes.jar -d "$dir"
  unzip -q -o "$dir/classes.jar" "$class" -d "$dir"
  echo "$dir/$class"
}

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

version=${1:-$(latest_version navigation3-ui-android)}
echo "Checking Navigation 3 $version"
failed=0

nav_display=$(extract_class navigation3-ui-android "$version" \
  androidx/navigation3/ui/NavDisplayKt__NavDisplayKt.class)
calls=$(javap -c -p "$nav_display" | grep -cF "$CALL" || true)
if [[ "$calls" == "1" ]]; then
  echo "OK: NavDisplayKt__NavDisplayKt calls rememberDecoratedNavEntries once."
else
  echo "FAIL: NavDisplayKt__NavDisplayKt calls rememberDecoratedNavEntries $calls time(s), expected 1."
  failed=1
fi

decorated_entries=$(extract_class navigation3-runtime-android "$version" \
  androidx/navigation3/runtime/DecoratedNavEntriesKt.class)
# javap -s prints each method's descriptor on the line after its declaration.
if javap -p -s "$decorated_entries" |
  grep -B1 -xF "    descriptor: $DESCRIPTOR" |
  grep -q " rememberDecoratedNavEntries("; then
  echo "OK: DecoratedNavEntriesKt declares rememberDecoratedNavEntries$DESCRIPTOR."
else
  echo "FAIL: DecoratedNavEntriesKt no longer declares rememberDecoratedNavEntries$DESCRIPTOR."
  failed=1
fi

if [[ "$failed" != "0" ]]; then
  echo "Navigation 3 $version breaks the NAVIGATION3 instrumentation; see Navigation3Display."
fi
exit "$failed"
