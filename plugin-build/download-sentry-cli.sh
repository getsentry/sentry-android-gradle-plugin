#!/bin/bash
cd $(dirname "$0")

props_file="sentry-cli.properties"

function prop {
  grep "$1" $props_file | cut -d'=' -f2 | xargs
}

base_url="$(prop 'repo')/releases/download/$(prop 'version')"
target_dir="src/main/resources/bin/"
PLATFORMS="Darwin-universal Linux-i686 Linux-x86_64 Linux-aarch64 Windows-i686"

rm -f $target_dir/sentry-cli-*
for plat in $PLATFORMS; do
  suffix=''
  if [[ $plat == *"Windows"* ]]; then
    suffix='.exe'
  fi
  echo "${plat}"
  download_url=$base_url//sentry-cli-${plat}${suffix}
  fn="$target_dir/sentry-cli-${plat}${suffix}"
  curl -SL --progress-bar "$download_url" -o "$fn"
  chmod +x "$fn"
  cp $props_file $target_dir/$props_file
done
