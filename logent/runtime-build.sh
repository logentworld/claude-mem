#!/usr/bin/env bash
# 개발·CI에서만 의존성을 설치하고 원본 엔진을 묶어요. 휴대폰에서는 실행하지 않아요.
set -euo pipefail
cd "$(dirname "$0")/.."
command -v node >/dev/null && node -e 'const [a,b]=process.versions.node.split(".").map(Number);process.exit(a>22||(a===22&&b>=13)?0:1)' || { echo 'Node.js 22.13.0 이상을 설치한 뒤 다시 빌드해 주세요.' >&2; exit 1; }
command -v npm >/dev/null || { echo 'npm을 설치한 뒤 다시 빌드해 주세요.' >&2; exit 1; }
npm ci --prefix logent/runtime --ignore-scripts --no-audit --no-fund
npm run build --prefix logent/runtime
