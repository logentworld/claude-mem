#!/usr/bin/env bash
# 태그와 VERSION이 같은 산출물만 GitHub 릴리스에 올려요.
set -euo pipefail
cd "$(dirname "$0")/.."
built=false
if [[ "${1:-}" == --built ]]; then built=true; shift; fi
version="$(tr -d '[:space:]' < VERSION)"
tag="v$version"
[[ "$(git rev-parse "$tag^{commit}")" == "$(git rev-parse HEAD)" ]] || { echo "현재 커밋과 VERSION 태그가 달라요." >&2; exit 1; }
if [[ "$built" == false ]]; then
    : "${LOGENT_CONTENT_SIGNING_KEY_FILE:?설치용 콘텐츠 서명 키 파일을 지정해 주세요.}"
    ./logent/test.sh
    ./logent/build.sh
fi
name="logent-claude-mem"
dist="$PWD/build/dist"
assets=("$dist/$name-v$version.aar" "$dist/$name-v$version-assets-source.zip" "$dist/$name-v$version.lpg" "$dist/SHA256SUMS")
for asset in "${assets[@]}"; do [[ -s "$asset" ]] || { echo "릴리스 산출물이 없어요: $asset" >&2; exit 1; }; done
repo="$(git remote get-url origin)"
if gh release view "$tag" --repo "$repo" >/dev/null 2>&1; then
    gh release upload "$tag" "${assets[@]}" --repo "$repo" --clobber
else
    gh release create "$tag" "${assets[@]}" --repo "$repo" --verify-tag --title "$name $tag" --notes "${1:-스킬 AAR·자산 원본·서명된 .lpg 패키지를 배포해요.}"
fi
