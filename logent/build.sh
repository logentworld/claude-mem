#!/usr/bin/env bash
# AAR을 D8로 변환해 .lpg만 만들어요.
set -euo pipefail
cd "$(dirname "$0")/.."
version="$(tr -d '[:space:]' < VERSION)"
name="logent-claude-mem"
dist="$PWD/build/dist"
./logent/gradle.sh :logent-plugin:pluginArtifacts "$@"
lpg="$dist/$name-v$version.lpg"
[[ -s "$lpg" ]] || { echo ".lpg가 없어요: $lpg" >&2; exit 1; }
printf '산출물을 만들었어요: %s\n' "$lpg"
