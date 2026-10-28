# JWT 게시판 — 우리의 기록

웹서비스설계 JWT 인증 게시판 개인 과제입니다. 누구나 글을 볼 수 있고, 로그인한 사용자만 글을 쓸 수 있으며, 본인 글만 수정·삭제할 수 있습니다.

## 작성자와 서비스 소개

- 학번 / 이름: 202412863 / 김원준
- 서비스 목적과 대상 사용자: 수업 과제용 간단한 게시판입니다. 방문자는 로그인 없이 글 목록과 상세를 볼 수 있고, 로그인한 사용자는 글을 작성하고 자신의 글을 수정·삭제할 수 있습니다.
- 선택한 기술: Vue 3 (Vite) + Spring Boot 3.5.16 (JJWT 0.13.0)
- 주요 기능: 공개 목록·상세, 로그인과 JWT 발급, JWT 검증과 내 정보 조회, 글 작성·수정·삭제, 작성자 권한 검사, 토큰 만료·로그아웃 처리

## 보고서와 테스트 증거

- [구현 보고서 PDF](docs/report.pdf)
- [테스트 증거](docs/evidence/)

## 프로젝트 구조

```text
.
├── README.md
├── .gitignore
├── frontend/          # Vue 소스 (src/api.js, src/App.vue)
├── backend/           # Spring Boot 소스 (AuthService.java 등)
├── tools/             # 제공된 자동 검사 도구 (check_api.py)
└── docs/
    ├── report.pdf
    └── evidence/
```

## 실행 환경

| 항목 | 사용 버전 |
|---|---|
| 운영체제 | Windows 11 |
| 프론트 런타임 Node.js | v24.21.0 |
| 백엔드 런타임 | JDK 25 (pom.xml의 java.version은 17) |
| 프레임워크 | Vue 3 + Vite / Spring Boot 3.5.16 |
| 데이터 저장 | SQLite (data/board.sqlite) |

## 설치와 실행

### 1 저장소 복제

```sh
git clone https://github.com/kwj24/jwt-board.git
cd jwt-board
```

### 2 백엔드 준비와 실행 (터미널 A)

Spring Boot는 `.env`를 읽지 않으므로, 서버를 실행할 터미널에서 환경변수를 먼저 설정합니다. 실제 키는 저장소에 올리지 않습니다.

1. JWT_SECRET 새로 만들기 (64자리 16진수, PowerShell):

```powershell
$b = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$key = ($b | ForEach-Object { $_.ToString("x2") }) -join ""
```

2. 환경변수 설정과 실행 (`backend` 폴더에서):

```powershell
cd backend
$env:JWT_SECRET = $key
$env:TOKEN_TTL = "90"
.\mvnw.cmd package
java -jar target/jwt-board-1.0.0.jar
```

- 환경설정 예시 파일: `backend/env.example.txt`
- TOKEN_TTL: 기본 90초. 만료 테스트에서는 `3`처럼 줄여서 서버를 다시 시작합니다.
- 백엔드 주소: http://127.0.0.1:8000
- 정상 실행 확인: http://127.0.0.1:8000/api/health 가 `status: ok`
- JWT_SECRET이 비어 있거나 64자리 hex가 아니거나 예시 키이면 서버가 시작되지 않습니다.
- 반드시 `backend` 폴더에서 실행합니다. DB 경로(`data/board.sqlite`)가 실행 위치 기준이기 때문입니다.

### 3 프론트 빌드와 로컬 실행 (터미널 B)

백엔드를 켜 두고 새 터미널에서 실행합니다.

```sh
cd frontend
npm ci
npm run build
npm run preview
```

- 최종 확인 주소: http://127.0.0.1:4173
- 개발 중 실행: `npm run dev`, http://127.0.0.1:5173
- API 연결: 프론트의 `/api` 요청이 Vite 프록시를 통해 백엔드 8000번으로 전달됩니다.
- 종료: 각 터미널에서 Ctrl+C

### 4 테스트 계정과 DB

| 사용자 | 이메일 | 실습용 비밀번호 |
|---|---|---|
| Alice | alice@class.test | Classroom!2026 |
| Bob | bob@class.test | Classroom!2026 |

- SQLite 파일: `backend/data/board.sqlite` (서버 실행 시 자동 생성, 저장소에는 올리지 않음)
- DB 초기화: 서버를 종료하고 `backend/data/board.sqlite`를 삭제
- 서버 재시작 후 데이터 유지 확인 결과: check_api.py --mode final 40개 PASS

## 구현 완료 범위

| 기능 | 완료 여부 | 주요 파일 또는 함수 |
|---|---|---|
| 공개 목록과 상세 | 완료 | `BoardController`, `frontend/src/App.vue` |
| 로그인과 JWT 발급 | 완료 | `AuthService.issueToken`, `api.js login` |
| JWT 검증과 내 정보 조회 | 완료 | `AuthService.currentUserId`, `GET /api/me` |
| 글 작성과 작성자 저장 | 완료 | `BoardController` (작성자는 검증된 사용자 ID로 서버가 결정) |
| 본인 글 수정·삭제 | 완료 | `AuthService.assertOwner`, `api.js canEdit` |
| 토큰 만료와 로그아웃 | 완료 | `AuthService.currentUserId`, `api.js request` (401 시 상태 해제) |
| 입력 오류와 권한 오류 처리 | 완료 | `Errors.java`(제공), `assertOwner` |

알려진 문제와 미완성 기능: T11 미실행, T10은 /me만 확인

## 테스트 방법과 결과 요약

- 실행한 검사: 저장소 루트에서 `python tools/check_api.py --mode final` → check_api_final.txt
- 만료 모드 검사(`--expiry`, TOKEN_TTL=3): 미실행
- Postman으로 직접 확인: 토큰 없음 401 TOKEN_REQUIRED, 형식 오류 401 TOKEN_REQUIRED, 변조 401 INVALID_TOKEN, 만료 401 TOKEN_EXPIRED, Bob이 Alice 글 수정 403 FORBIDDEN
- 브라우저 확인: 로그인 → 글 등록 → 본인 글 수정·삭제 버튼 표시, 타인 글 버튼 숨김, 새로고침 시 로그아웃
- 전체 T01~T17 결과와 증거: = jwt-board/docs/evidence/

직접 수행한 결과만 기록했습니다.

## 핵심 구현 설명

1. 로그인: `POST /api/auth/login`에서 계정을 확인하면 `AuthService.issueToken`이 HS256 JWT(sub, iss, aud, iat, exp)를 서버 비밀 키로 서명해 발급합니다.
2. 토큰 보관·전송: 프론트(`frontend/src/api.js`)는 토큰을 메모리에만 저장하고, 요청마다 `Authorization: Bearer <JWT>`로 보냅니다. 401이면 저장된 토큰과 사용자를 지웁니다.
3. 서버 검증: `AuthService.currentUserId`가 헤더 형식, 서명, 만료, iss, aud, sub 형식, 실제 계정 존재를 확인하고 검증된 사용자 ID만 반환합니다.
4. 작성자 권한: `AuthService.assertOwner`가 현재 사용자 ID와 저장된 글의 작성자 ID를 비교하며, 다르면 403 FORBIDDEN입니다. 요청 본문의 작성자 값은 사용하지 않습니다.

상세 설명은 보고서 3절에 있습니다.

## AI 활용 요약과 참고 자료

- AI 사용 여부: 사용
- 도구와 사용 목적: Claude (Opus 5.5, Sonnet 5.5) — 개념 학습, 설계, 코드 생성, 디버깅, 문장 정리
- 반영 위치: `AuthService.java`(생성자, issueToken, currentUserId, assertOwner), `frontend/src/api.js`, `frontend/src/App.vue`의 일부. AI가 제안한 코드를 거의 그대로 반영했고, 환경변수 설정·실행·테스트·캡처는 직접 수행했습니다.
- 상세 활용 기록: 보고서 5절
- 참고한 자료: 수업 제공 템플릿 `JWT-Assignment` (공지, API 명세, 구현 체크리스트, 테스트 시나리오)

## 제출 확인

- [ ] README의 안내 문구를 실제 내용으로 교체했다.
- [ ] 새 폴더에 clone하여 README만으로 설치·환경설정·빌드·로컬 실행했다.
- [ ] 4173번 빌드 화면에서 API 연결과 전체 필수 기능을 확인했다.
- [ ] GitHub에서 보고서와 증거 링크가 열린다.
- [ ] 교수자가 저장소를 열람·복제할 수 있다.
- [ ] 비밀 키·실제 토큰·DB·node_modules·.venv·target을 커밋하지 않았다.
- [ ] 최종 push 후 저장소 URL과 제출 기준 커밋 SHA를 LMS에 제출했다.