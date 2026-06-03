#!/usr/bin/env bash
# One-command throwaway prototype for the SQLiteDriver call-site wrapping transform.
# Compiles stub androidx/room/sentry classes + a Caller, rewrites Caller's bytecode with the
# prototype transformer, then RUNS the transformed Caller on a stock JVM (= JVM verifier pass)
# and prints wrap/skip decisions and runtime types.
set -euo pipefail
cd "$(dirname "$0")"

# Resolve a consistent ASM 9.8 classpath from the Gradle cache.
CACHE="$HOME/.gradle/caches/modules-2/files-2.1/org.ow2.asm"
asm() { find "$CACHE/$1/9.8" -name "$1-9.8.jar" 2>/dev/null | head -1; }
ASM_CP="$(asm asm):$(asm asm-tree):$(asm asm-analysis):$(asm asm-commons):$(asm asm-util)"
echo "ASM classpath: $ASM_CP"

rm -rf build && mkdir -p build/classes build/tools

echo "== 1. compile stubs + Caller + Main =="
find stubs -name '*.java' > build/stub_sources.txt
javac -d build/classes @build/stub_sources.txt

echo "== 2. compile transformer (needs ASM) =="
javac -cp "$ASM_CP" -d build/tools $(find transformer -name '*.java')

echo "== 3. disassemble Caller BEFORE transform (grep for SentrySQLiteDriver.create) =="
java -cp "build/tools:$ASM_CP" proto.Disassemble build/classes/demo/Caller.class \
  | grep -nE "setDriver|SentrySQLiteDriver" || echo "  (no SentrySQLiteDriver calls before transform - expected)"

echo "== 4. transform Caller.class in place =="
java -cp "build/tools:$ASM_CP" proto.DriverWrapTransformer build/classes/demo/Caller.class

echo "== 5. disassemble Caller AFTER transform (grep for SentrySQLiteDriver.create) =="
java -cp "build/tools:$ASM_CP" proto.Disassemble build/classes/demo/Caller.class \
  | grep -nE "setDriver|SentrySQLiteDriver.create"

echo "== 6. RUN transformed Caller on stock JVM (no ASM on cp => proves self-contained + verified) =="
java -cp build/classes demo.Main
