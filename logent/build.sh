#!/usr/bin/env bash
# AAR·자산 원본 ZIP을 만들고, 키를 명시하면 설치용 .lpg 패키지도 서명해요.
set -euo pipefail
cd "$(dirname "$0")/.."
./logent/gradle.sh :logent-plugin:pluginArtifacts "$@"
version="$(tr -d '[:space:]' < VERSION)"
name="logent-claude-mem"
dist="$PWD/build/dist"
mkdir -p "$dist"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
suffix=""
key="${LOGENT_CONTENT_SIGNING_KEY_FILE:-}"
if [[ -z "$key" ]]; then
    suffix="-ci"
    key="$work/ci-signing.pem"
    umask 077
    openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -pkeyopt ec_param_enc:named_curve -out "$key"
    public="$work/ci-public.der"
    openssl ec -in "$key" -pubout -outform DER -conv_form uncompressed -param_enc named_curve -out "$public" 2>/dev/null
else
    if [[ "${LOGENT_OPERATIONAL_SIGNING:-true}" == "false" ]]; then
        public="${LOGENT_PLUGIN_PUBLIC_KEY_FILE:?CI 임시 공개키 경로가 없어요.}"
    else
        # 앱이 고정한 운영 공개키와 다른 키로 서명한 패키지는 게시하지 않아요.
        public="$work/content-public.der"
        base64 --decode content-public-key.der.b64 > "$public"
    fi
fi
artifacts=("$dist/$name-v$version.aar" "$dist/$name-v$version-assets-source.zip")
python3 logent/plugin-pack.py build \
    --definition "$PWD/logent-plugin.json" --version "$version" \
    --aar "$dist/$name-v$version.aar" \
    --key "$key" --public-key "$public" \
    --output "$dist/$name-v$version$suffix.lpg"
artifacts+=("$dist/$name-v$version$suffix.lpg")
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
