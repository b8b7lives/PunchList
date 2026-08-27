#!/usr/bin/env bash
# Production jar + real modlist, booted to the title screen; the in-mod probe
# force-loads every mixin-target class. Grep-driven pass/fail.
# Stack source: populate ../minecraft/.fullstack/mods/ with the jars of a real modded client instance.
set -uo pipefail
cd "$(dirname "$0")/.."

STACK=${STACK:-../minecraft/.fullstack}
LOG=${LOG:-harness/artifacts/loadcheck.log}
WINDOW=${WINDOW:-900}
mkdir -p harness/artifacts harness/run .gradle-home

count=$(ls "$STACK"/mods/*.jar 2>/dev/null | wc -l)
[ "$count" -gt 0 ] || { echo "FAIL: no mods in $STACK/mods - see header"; exit 1; }
echo "modlist: $count third-party jars"

docker build -q -f Dockerfile.build -t punchlist-build:local . >/dev/null

NAME=punchlist-loadcheck-$$
timeout -k 15 "$WINDOW" docker run --rm --name "$NAME" \
  -v "$PWD/.gradle-home":/gradle -v "$PWD":/work -v "$(cd "$STACK" && pwd)":/fullstack -w /work \
  -e GRADLE_USER_HOME=/gradle -e CI=true -u "$(id -u):$(id -g)" punchlist-build:local \
  ./gradlew runLoadCheck -PfullstackDir=/fullstack --no-daemon --console=plain > "$LOG" 2>&1
docker kill "$NAME" >/dev/null 2>&1 || true

fail=0
check() { # name, pattern, want(1=present,0=absent)
  if [ "$3" = 1 ]; then grep -qE "$2" "$LOG" && ok=1 || ok=0; else grep -qE "$2" "$LOG" && ok=0 || ok=1; fi
  if [ "$ok" = 1 ]; then echo "PASS: $1"; else echo "FAIL: $1"; fail=1; fi
}

echo "log: $LOG"
check "no mixin apply failure"  'Mixin apply failed|MixinApplyError|Critical injection failure' 0
check "no injection errors"     'InjectionError'                                                0
check "loadcheck pass marker"   'punchlist loadcheck PASS'                                      1
check "no loadcheck failure"    'punchlist loadcheck FAIL|loadcheck: .* FAILED'                 0
check "no client crash"         'Minecraft has crashed|Failed to start|A mod crashed'           0

grep -E 'Mixin apply failed|InjectionError|was not found|Cannot resolve' "$LOG" | head -5
exit "$fail"
