# Logent 메모리 연결

## 구조와 원본

- `src/storage/sqlite/`와 `src/core/schemas/`가 저장·검색·FTS·프로젝트·세션의 원본이에요. Logent용 스키마 복사본을 두지 않아요.
- `logent/runtime/`은 원본 저장소를 가져오는 CLI와 `bun:sqlite` → Node.js `node:sqlite` 경계 어댑터예요.
- `logent-plugin/`은 공개 API 4를 구현하는 Kotlin 도구·명령·설정·훅이에요.
- 빌드된 CLI는 서명 `.lpg` 자산에서 Termux로 전개해요. 실행할 때 npm·원격 코드를 다운로드하지 않아요.

## 실행 환경과 데이터

Node.js 22.13 이상, unzip, sha256sum을 Termux에 준비해요. 내장 SQLite와 FTS5를 사용하므로 Bun·네이티브 npm SQLite 모듈·별도 HTTP 워커가 필요하지 않아요. 설정하지 않은 원본 `~/.claude-mem` 폴더나 다른 에이전트 기록을 자동으로 읽지 않아요.

기록은 `~/.logent/plugins/claude-mem/data/claude-mem.db`에 보관해요. 작업 디렉터리의 실제 경로로 프로젝트를 구분하고, 세션 ID를 함께 남겨요. 존재하지 않는 작업 디렉터리는 오류로 처리해요. 조회·변경·삭제도 현재 프로젝트 범위를 지켜요.

## 사용과 개인정보

`claude_mem`으로 상태·검색·선택한 기억 조회·주변 타임라인·문맥·내보내기를 요청해요. `claude_mem_write`는 명시적 기억 저장·수정·삭제·가져오기·프로젝트 전체 삭제를 처리해요. 삭제나 가져오기는 원본 파일을 자동 재실행하는 동작이 아니에요.

자동 기록과 문맥 주입은 기본으로 꺼져 있어요. 플러그인 상세나 `/claude-mem`에서 명시적으로 켜고 꺼요. 자동 관찰은 사용자 메시지와 허용된 도구 결과만 대상으로 하며, 도구 인자·기억 검색 결과를 다시 기록하지 않아요. `<private>` 구간과 흔한 비밀값 패턴은 제거하지만 임의 인코딩·분할된 비밀까지 완벽히 탐지하지는 못해요. 민감 작업에서는 자동 기록을 꺼 주세요.

조회한 과거 기록은 명령이 아니라 데이터 경계 안에서 모델에 전달해요. 원본 로그가 현재 지시·권한·시스템 메시지로 승격되지 않게 태그를 무력화해요. 훅 오류는 본 작업을 막지 않지만, 사용자가 직접 실행한 메모리 도구의 실패는 오류로 반환해요. 실패한 쓰기를 자동으로 다시 실행하지 않아요.

선택한 외부 요약기를 사용할 때만 별도로 선언한 SECRET 키를 `PluginHost.http`로 보내요. 키는 Termux 셸·명령 인자·로그·문맥으로 보내지 않아요. 외부 요약기를 설정하지 않은 로컬 기록을 AI 요약이라고 표시하지 않아요. 원본의 유료 observer·계정 로그인·Claude SDK·클라우드 동기화·사용 분석·다른 하네스 설치기는 실행하지 않아요.

## 빌드와 업데이트

```bash
./logent/test.sh -PlogentPluginApiMavenLocal=true
LOGENT_CONTENT_SIGNING_KEY_FILE="$HOME/.local/share/logent/content-signing.pem" \
  ./logent/build.sh -PlogentPluginApiMavenLocal=true
```

고정된 `plugin-api.lock`의 계약을 사용해요. 운영 서명 비밀 `LOGENT_CONTENT_SIGNING_KEY`가 설정된 CI는 설치 가능한 `.lpg`를 만들어요. 키가 없으면 별도 테스트 키로 서명한 `-ci.lpg`와 설치 불가 안내를 게시해요. 개인키는 저장소나 배포 파일에 넣지 않아요.

`main`·`logent` 커밋마다 테스트·lint·빌드·`edge` 프리릴리스를 실행하고, `v*` 태그는 태그 릴리스로 올려요. 원본 수정 갱신은 `git fetch upstream` → 변경 검토·merge → 회귀 검증 → `UPSTREAM_COMMIT` 갱신 순서로 해요.

패키지 제거 후 Termux에 남은 데이터베이스까지 자동으로 지우지 않아요. 필요하면 먼저 내보내기를 한 뒤 명시적인 프로젝트 전체 삭제를 요청하거나 Termux의 해당 데이터 폴더를 직접 관리해요.
