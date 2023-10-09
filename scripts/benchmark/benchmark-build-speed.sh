#!/bin/bash
set -eux

if ! command -v gradle-profiler &> /dev/null
then
    echo "Make sure to install gradle-profiler - brew install gradle-profiler"
    exit
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

if [ ! -d Android ]; then
    git clone git@github.com:duckduckgo/Android.git
    cd Android
    git checkout 1301996268b2c6c02ea0ae88934eca9da74bf806
    git submodule update --init --recursive
    cd ..
fi

gradle-profiler --benchmark \
 --project-dir Android \
 --scenario-file duckduckgo/duckduckgo.scenarios \
 --output-dir results/pre-sentry/

cd Android
git apply ../duckduckgo/add-sentry-to-duckduckgo.patch
./gradlew tasks --continue
cd ..

gradle-profiler --benchmark \
 --project-dir Android \
 --scenario-file duckduckgo/duckduckgo.scenarios \
 --output-dir results/post-sentry/

rm -rf Android
rm -rf gradle-user-home
