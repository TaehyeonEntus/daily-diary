# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

일기 작성 웹 애플리케이션 ("일기를 써봅시다").

- **Backend**: Spring Boot 3 REST API (`/backend`)
- **Frontend**: Next.js 16 + TypeScript (`/frontend`)
- **DB**: MySQL 8.0 (운영), H2 인메모리 (테스트)
- **배포**: Docker Compose (`backend/docker-compose.yml`)

## 명령어

### Backend (`/backend`)

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "com.daily_diary.backend.auth.service.AuthServiceTest.login_정상"

# API 문서 생성 (테스트 → Asciidoctor 순서)
./gradlew asciidoctor
# 생성 위치: build/docs/asciidoc/index.html

# 로컬 DB 실행 (MySQL)
docker compose up db -d
```

### Frontend (`/frontend`)

```bash
npm run dev       # 개발 서버 (포트 3001)
npm run build
npm run start     # 프로덕션 (포트 3001)
npm run lint
```

## 아키텍처

### Backend 패키지 구조

도메인 우선(Domain-First) 구조. 자세한 내용은 `backend/.claude/rules/architecture.md` 참고.

```
com.daily_diary.backend
├── auth/     # JWT 인증 (로그인·회원가입·토큰 갱신)
├── user/     # 사용자 프로필
├── post/     # 일기 게시글 (QueryDSL 검색/정렬)
├── comment/  # 댓글
└── global/
    ├── security/   # JwtFilter, JwtProvider, SecurityConfig
    └── exception/  # BusinessException, GlobalExceptionHandler
```

각 도메인의 레이어: `entity/` → `infra/` (Repository) → `service/` → `web/` (Controller + DTO) → `exception/`

**주요 설계 결정:**
- Refresh Token은 DB가 아닌 **Caffeine 인메모리 캐시**에 저장 (서버 재시작 시 전체 무효화)
- QueryDSL로 게시글 검색(SearchType: DEFAULT·NICKNAME·TITLE·CONTENT) 및 정렬(OrderType: DATE·VIEW·LIKE) 구현
- DB 마이그레이션: Flyway (`src/main/resources/db/`)
- 모니터링: Spring Actuator + Prometheus (`/actuator/prometheus`)

### Frontend 구조

```
frontend/
├── app/                  # Next.js App Router 페이지
│   ├── posts/[id]/       # 게시글 상세
│   ├── write/            # 일기 작성
│   ├── profile/          # 프로필
│   ├── login/ signup/    # 인증
│   └── ui/ providers/   # 공통 UI 컴포넌트, Context Providers
├── components/
│   ├── ui/               # 재사용 UI (Button, Card, Input)
│   └── providers/        # root-provider (전역 상태)
└── lib/api/              # Axios 기반 API 클라이언트
    ├── client.ts          # baseURL='/api', 401 자동 토큰 갱신
    ├── auth.ts / posts.ts / comments.ts / users.ts
    └── types.ts           # 공유 타입
```

**주요 설계 결정:**
- `next.config.ts`의 `rewrites`로 `/api/*` → `NEXT_PUBLIC_API_URL` (기본 `http://localhost:8080`) 프록시
- Access Token은 `localStorage`에 저장, Refresh Token은 HttpOnly 쿠키 (`withCredentials: true`)
- 401 응답 시 `client.ts` 인터셉터가 자동으로 토큰 갱신 + 요청 큐 처리

## 코딩 컨벤션

`backend/.claude/rules/coding-conventions.md`와 `backend/.claude/rules/test-conventions.md`에 상세 규칙이 있음. 핵심 요약:

- Controller: `ResponseEntity<T>` 반환, POST→201, GET→200, PATCH/DELETE→204
- Service: 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional`
- Entity: `@Setter` 금지, 정적 생성자 `of()`, 변경자 `change~()`
- DTO: `record` 사용, `XxxRequest` / `XxxResponse` 네이밍
- Repository: `findOrThrow()` default 메서드 패턴
- `var` 사용 금지, 명시적 타입 선언

## 환경 설정

Backend `application.properties`는 값이 비어 있으며 환경변수 또는 `application-dev.properties`로 주입.
필요한 설정값: `spring.datasource.*`, `jwt.secret`, `jwt.access-token-expiry`, `jwt.refresh-token-expiry`, `cors.allowed-origins`

Frontend 환경변수: `NEXT_PUBLIC_API_URL` (백엔드 주소, 기본값 `http://localhost:8080`)
