#!/usr/bin/env bash
# AAR·자산 원본 ZIP을 만들고, 키를 명시하면 설치용 .lpg 패키지도 서명해요.
set -euo pipefail
cd "$(dirname "$0")/.."
./logent/gradle.sh :logent-plugin:pluginArtifacts "$@"
version="$(tr -d '[:space:]' < VERSION)"
name="logent-claude-mem"
dist="$PWD/build/dist"
artifacts=("$dist/$name-v$version.aar" "$dist/$name-v$version-assets-source.zip")
if [[ -n "${LOGENT_CONTENT_SIGNING_KEY_FILE:-}" ]]; then
    mkdir -p build/keys
    if [[ -n "${LOGENT_PLUGIN_PUBLIC_KEY_FILE:-}" ]]; then
        public="$LOGENT_PLUGIN_PUBLIC_KEY_FILE"
    else
        base64 --decode content-public-key.der.b64 > build/keys/content-public.der
        public="$PWD/build/keys/content-public.der"
    fi
    python3 logent/plugin-pack.py build \
        --definition "$PWD/logent-plugin.json" --version "$version" \
        --aar "$dist/$name-v$version.aar" \
        --key "$LOGENT_CONTENT_SIGNING_KEY_FILE" --public-key "$public" \
        --output "$dist/$name-v$version.lpg"
    artifacts+=("$dist/$name-v$version.lpg")
fi
python3 - "$dist" "${artifacts[@]}" <<'PY'
import hashlib
import pathlib
import sys
root = pathlib.Path(sys.argv[1])
with (root / 'SHA256SUMS').open('w', encoding='ascii') as output:
    for name in sys.argv[2:]:
        path = pathlib.Path(name)
        with path.open('rb') as asset:
            digest = hashlib.file_digest(asset, 'sha256').hexdigest()
        output.write(f'{digest}  {path.name}\n')
PY
printf '산출물을 만들었어요: %s\n' "$dist"
