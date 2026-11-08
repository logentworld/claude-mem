# Logent Claude Mem

[Claude-Mem](https://github.com/thedotmack/claude-mem)의 원본 SQLite 메모리 엔진을 Logent에 연결하는 `.lpg` 플러그인이에요. 원본 소스와 이력은 이 포크에 보존하고, Logent 어댑터만 `logent/`·`logent-plugin/`에 추가해요.

## 설치

1. [릴리스](https://github.com/logentworld/claude-mem/releases)에서 운영 서명된 `logent-claude-mem-*.lpg`를 받아요. `-ci.lpg`는 테스트 서명이라 Logent에 설치할 수 없어요.
2. Logent의 **설정 → 플러그인과 스킬 → 플러그인 관리**에서 설치해요.
3. Termux에 Node.js 22.13 이상, unzip, sha256sum을 준비해요. 플러그인이 패키지를 자동 설치하지 않아요.
4. 플러그인 상세에서 자동 기록과 문맥 주입을 명시적으로 켜요. 기본값은 둘 다 꺼져 있어요.

`claude_mem`은 검색·조회·타임라인을, `claude_mem_write`는 기억 저장·수정·삭제를 맡아요. `/claude-mem`에서 상태와 활성 설정을 다뤄요. 원본 `~/.claude-mem` 데이터베이스는 자동으로 읽거나 변환하지 않아요.

## 빌드

```bash
./logent/test.sh -PlogentPluginApiMavenLocal=true
LOGENT_CONTENT_SIGNING_KEY_FILE="$HOME/.local/share/logent/content-signing.pem" \
  ./logent/build.sh -PlogentPluginApiMavenLocal=true
```

`plugin-api.lock`의 API artifact가 필요해요. 개발용 로컬 Maven을 쓰지 않으면 GitHub Packages 인증을 환경에서 받아요. `main`·`logent`의 각 커밋은 테스트·lint·LPG 빌드를 거쳐 `edge`에 게시하고, `v*` 태그는 해당 릴리스에 게시해요.

## 문서와 출처

- [Logent 실행·보안·데이터 안내](logent/README.md)
- [원본 README](README.upstream.md) · [라이선스](LICENSE) · [변경 고지](NOTICE)
- 원본 버전은 `package.json`, 고정 소스 커밋은 `UPSTREAM_COMMIT`, Logent 패키지 버전은 `VERSION`에 있어요.

원본 설치기·Claude 전용 플러그인·유료 observer·클라우드 동기화·사용 분석을 자동 실행하지 않아요. 이 어댑터의 동작 범위는 Logent 메모리 도구와 훅이에요.
