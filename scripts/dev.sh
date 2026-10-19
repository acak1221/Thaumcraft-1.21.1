#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
case "${1:-validate}" in
 build) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" :fabric:build ;;
 test) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" :common:test ;;
 compile|compileJava) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" :common:compileJava :fabric:compileJava ;;
 validate) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" :common:test :fabric:clientTestClasses :fabric:build :fabric:runGameTest "${@:2}" ;;
 gametest) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" :fabric:runGameTest ;;
 client-smoke) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" :fabric:runClientSmoke ;;
 server) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" :fabric:runServer --args=--nogui ;;
 *) exec ./gradlew --no-daemon --console=plain --gradle-user-home "$ROOT/.gradle21" "$@" ;;
esac
