#!/usr/bin/env bash
# 같은 자산으로 JVM 단위 테스트·Android lint·Node 엔진 회귀 테스트를 실행해요.
set -euo pipefail
cd "$(dirname "$0")/.."
export LANG="${LANG:-C.UTF-8}"
export LC_ALL="${LC_ALL:-C.UTF-8}"
export PYTHONDONTWRITEBYTECODE=1
./logent/gradle.sh :logent-plugin:testDebugUnitTest :logent-plugin:lintRelease "$@"
node --test logent/tests/*.test.mjs
