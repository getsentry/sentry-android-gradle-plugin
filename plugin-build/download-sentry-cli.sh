#!/bin/bash
cd $(dirname "$0")
REPO=getsentry/sentry-cli
VERSION=1.65.0
PLATFORMS="Darwin-x86_64 Linux-i686 Linux-x86_64 Windows-i686"

rm -f src/main/resources/bin/sentry-cli-*
for plat in $PLATFORMS; do
  suffix=''
  if [[ $plat == *"Windows"* ]]; then
    suffix='.exe'
  fi
  echo "${plat}"
  download_url=https://github.com/$REPO/releases/download/$VERSION/sentry-cli-${plat}${suffix}
  fn="src/main/resources/bin/sentry-cli-${plat}${suffix}"
  curl -SL --progress-bar "$download_url" -o "$fn"
  chmod +x "$fn"
  sha1sum src/main/resources/bin/sentry-cli-* > src/main/resources/bin/checksums.sha || shasum src/main/resources/bin/sentry-cli-* > src/main/resources/bin/checksums.sha
done
