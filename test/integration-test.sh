#! /bin/bash

start_server() {
    python3 test/integration-test-server.py
}

stop_server() {
    curl http://127.0.0.1:8000/STOP
}

start_server &

if ! (plugin-build/sentry-cli-upload-proguard.sh) ; then
    stop_server
    exit 1
fi

stop_server
exit 0