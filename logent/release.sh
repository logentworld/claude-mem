#!/usr/bin/env bash
# 검증·버전 갱신·커밋·태그·푸시·GitHub 릴리스를 순서대로 수행해요.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -lt 2 ]]; then
    echo "사용법: ./logent/release.sh 'English summary' '한국어 해요체 본문' [Gradle 인자...]" >&2
    exit 1
fi
summary="$1"; body="$2"; shift 2
next="$(python3 - <<'PY'
import pathlib, re
version = pathlib.Path('VERSION').read_text().strip()
if not re.fullmatch(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)', version):
    raise SystemExit('VERSION은 x.y.z 형식으로 써 주세요.')
major, minor, patch = map(int, version.split('.'))
print(f'{major}.{minor}.{patch + 1}')
PY
)"
[[ "$summary" != *$'\n'* ]] && LC_ALL=C grep -q '^[ -~]*[A-Za-z][ -~]*$' <<< "$summary" || { echo "제목은 한 줄 영어로 써 주세요." >&2; exit 1; }
[[ "$body" =~ 요([.!?]|…)*[[:space:]]*$ ]] || { echo "본문은 한국어 해요체로 써 주세요." >&2; exit 1; }
: "${LOGENT_CONTENT_SIGNING_KEY_FILE:?설치용 콘텐츠를 서명할 Git 밖 개인 키 파일을 지정해 주세요.}"
for tool in git gh python3 openssl; do command -v "$tool" >/dev/null || { echo "필수 도구가 없어요: $tool" >&2; exit 1; }; done
gh auth status >/dev/null 2>&1 || { echo "먼저 gh auth login으로 인증해 주세요." >&2; exit 1; }
branch="$(git symbolic-ref --quiet --short HEAD)"
tag="v$next"
if git show-ref --verify --quiet "refs/tags/$tag"; then echo "이미 있는 태그예요: $tag" >&2; exit 1; fi
remote_branch="$(git ls-remote --heads origin "refs/heads/$branch")"
if [[ -n "$remote_branch" ]]; then
    git fetch origin "$branch" --quiet
    [[ "$(git rev-parse HEAD)" == "$(git rev-parse FETCH_HEAD)" ]] || { echo "로컬 브랜치와 원격 브랜치를 먼저 동기화해 주세요." >&2; exit 1; }
fi
[[ -z "$(git ls-remote origin "refs/tags/$tag")" ]] || { echo "원격에 이미 있는 태그예요: $tag" >&2; exit 1; }
previous="$(cat VERSION)"
start="$(git rev-parse HEAD)"
restore_version() {
    result=$?
    if [[ "$(git rev-parse HEAD)" == "$start" ]]; then printf '%s\n' "$previous" > VERSION; fi
    exit "$result"
}
trap restore_version EXIT
printf '%s\n' "$next" > VERSION
./logent/test.sh "$@"
./logent/build.sh "$@"
name="logent-claude-mem"
title="🚀 Release $name $tag: $summary"
git add -A
git commit -m "$title" -m "$body"
git tag -a "$tag" -m "$title"
trap - EXIT
git push --atomic origin "HEAD:refs/heads/$branch" "refs/tags/$tag"
./logent/publish-release.sh --built "$body"
