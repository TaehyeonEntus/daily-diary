---
description: "User 및 게시글 CRUD with JWT 인증 — 태스크 목록 (도메인별)"
---

# 태스크: User 및 게시글 CRUD with JWT 인증

**입력**: `specs/001-user-post-crud/` 설계 문서
**전제 조건**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅

**구성**: 도메인별로 그룹화. 각 도메인을 한 번에 완성하여 PR 단위로 분리 가능.

## 형식: `[ID] [P?] [Domain?] 설명`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Domain]**: 해당 태스크가 속한 도메인 (AUTH, USER, POST)
- 각 태스크에 정확한 파일 경로 포함

---

## Phase 1: 설정

**목적**: 의존성 및 환경 설정

- [x] T001 `build.gradle`에 jjwt 0.12.6 (api/impl/jackson), spring-boot-starter-validation, spring-restdocs-mockmvc 의존성 추가 및 asciidoctor 플러그인(3.3.2) + snippetsDir 설정 추가
- [x] T002 `src/main/resources/application.properties`에 jwt.secret, jwt.access-token-expiry(1800000), jwt.refresh-token-expiry(604800000) 설정 추가
- [x] T003 [P] `src/docs/asciidoc/index.adoc` 생성 — API 문서 목차 및 각 도메인 스니펫 include 골격 작성

---

## Phase 2: Global — 횡단 관심사

**목적**: 모든 도메인이 공유하는 보안 및 예외 처리 인프라

**⚠️ 중요**: 이 Phase가 완료되기 전에는 어떤 도메인도 시작 불가

- [ ] T004 `src/main/java/com/daily_diary/backend/global/exception/BusinessException.java` 생성 — RuntimeException 상속, message 필드, 생성자
- [ ] T005 [P] `src/main/java/com/daily_diary/backend/global/exception/GlobalExceptionHandler.java` 생성 — @ControllerAdvice 골격만 작성 (도메인 예외 핸들러는 각 도메인 Phase에서 추가)
- [ ] T006 [P] `src/main/java/com/daily_diary/backend/global/security/JwtProvider.java` 생성 — 액세스/리프레시 토큰 생성, 검증, Claims 파싱, @Value로 secret/expiry 주입
- [ ] T007 `src/main/java/com/daily_diary/backend/global/security/JwtFilter.java` 생성 — OncePerRequestFilter 상속, Authorization 헤더에서 토큰 추출 후 SecurityContextHolder 설정
- [ ] T008 `src/main/java/com/daily_diary/backend/global/security/SecurityConfig.java` 생성 — SecurityFilterChain 빈, /auth/signup·/auth/login·/auth/refresh·GET /posts·GET /posts/** permitAll, 나머지 authenticated, JwtFilter 등록, BCryptPasswordEncoder 빈 등록

**체크포인트**: 보안 인프라 완료 — 도메인 구현 시작 가능

---

## Phase 3: Auth 도메인 🎯 MVP

**목표**: 회원가입·로그인·토큰 갱신·로그아웃을 한 번에 완성

**독립 테스트**:
- `POST /auth/signup` → 201, 사용자 정보 반환
- `POST /auth/login` → 200, accessToken + refreshToken 반환
- `POST /auth/refresh` → 200, 새 accessToken 반환
- `DELETE /auth/logout` → 204

- [ ] T009 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/entity/RefreshToken.java` 생성 — @Entity, id/token/user/expiresAt/createdAt 필드, of() 팩토리, isExpired() 메서드
- [ ] T010 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/infra/RefreshTokenRepository.java` 생성 — JpaRepository 상속, findByToken(), deleteByUser() 메서드 선언
- [ ] T011 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/exception/InvalidCredentialsException.java` 생성 — BusinessException 상속
- [ ] T012 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/exception/InvalidTokenException.java` 생성 — BusinessException 상속
- [ ] T013 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/SignupRequest.java` 생성 — record(email, password, nickname), @NotBlank @Email @Size 검증
- [ ] T014 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/LoginRequest.java` 생성 — record(email, password), @NotBlank 검증
- [ ] T015 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/LoginResponse.java` 생성 — record(accessToken, refreshToken)
- [ ] T016 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/TokenRefreshRequest.java` 생성 — record(refreshToken), @NotBlank 검증
- [ ] T017 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/TokenRefreshResponse.java` 생성 — record(accessToken)
- [ ] T018 [AUTH] `src/main/java/com/daily_diary/backend/auth/service/AuthService.java` 생성 — signup(이메일 중복 검사, BCrypt 해시, User 저장), login(자격증명 검증, RefreshToken 저장, LoginResponse 반환), refresh(토큰 검증·만료 확인, 새 accessToken 반환), logout(RefreshToken 삭제) 메서드 구현
- [ ] T019 [AUTH] `src/main/java/com/daily_diary/backend/auth/web/AuthController.java` 생성 — POST /auth/signup (201), POST /auth/login (200), POST /auth/refresh (200), DELETE /auth/logout (204) 엔드포인트
- [ ] T020 [AUTH] `src/test/java/com/daily_diary/backend/auth/web/AuthControllerTest.java` 생성 — @WebMvcTest + @AutoConfigureRestDocs, signup/login/refresh/logout 각 엔드포인트 테스트 및 document() 스니펫 생성 (auth/signup, auth/login, auth/refresh, auth/logout)
- [ ] T021 [AUTH] `GlobalExceptionHandler.java`에 InvalidCredentialsException(401), InvalidTokenException(401), DuplicateEmailException(409), MethodArgumentNotValidException(400) 핸들러 추가
- [ ] T022 [AUTH] `src/docs/asciidoc/index.adoc`에 Auth 도메인 스니펫 include 추가

**체크포인트**: Auth 도메인 전체 독립 동작 확인 → PR 가능

---

## Phase 4: User 도메인

**목표**: 프로필 조회·수정·회원 탈퇴를 한 번에 완성

**독립 테스트**:
- `GET /users/me` → 200, 프로필 반환
- `PATCH /users/me` → 200, 닉네임 수정 확인
- `DELETE /users/me` → 204, 이후 로그인 불가 확인

- [ ] T023 [P] [USER] `src/main/java/com/daily_diary/backend/user/entity/User.java` 생성 — @Entity, id/email/password/nickname/createdAt/updatedAt/deletedAt 필드, of() 팩토리, changeNickname()/softDelete() 메서드
- [ ] T024 [P] [USER] `src/main/java/com/daily_diary/backend/user/infra/UserRepository.java` 생성 — JpaRepository 상속, findByEmail(), findByIdAndDeletedAtIsNull() 메서드 선언
- [ ] T025 [P] [USER] `src/main/java/com/daily_diary/backend/user/exception/UserNotFoundException.java` 생성 — BusinessException 상속
- [ ] T026 [P] [USER] `src/main/java/com/daily_diary/backend/user/exception/DuplicateEmailException.java` 생성 — BusinessException 상속
- [ ] T027 [P] [USER] `src/main/java/com/daily_diary/backend/user/web/UserUpdateRequest.java` 생성 — record(nickname), @NotBlank @Size(max=50) 검증
- [ ] T028 [P] [USER] `src/main/java/com/daily_diary/backend/user/web/UserResponse.java` 생성 — record(id, email, nickname, createdAt)
- [ ] T029 [USER] `src/main/java/com/daily_diary/backend/user/service/UserService.java` 생성 — getMe(userId) → UserResponse, updateMe(userId, request) → UserResponse, deleteMe(userId, 소프트 삭제 + RefreshToken 삭제) 메서드 구현
- [ ] T030 [USER] `src/main/java/com/daily_diary/backend/user/web/UserController.java` 생성 — GET /users/me (200), PATCH /users/me (200), DELETE /users/me (204) 엔드포인트, SecurityContextHolder에서 userId 추출
- [ ] T031 [USER] `src/test/java/com/daily_diary/backend/user/web/UserControllerTest.java` 생성 — @WebMvcTest + @AutoConfigureRestDocs, getMe/updateMe/deleteMe 테스트 및 document() 스니펫 생성 (user/me-get, user/me-update, user/me-delete)
- [ ] T032 [USER] `GlobalExceptionHandler.java`에 UserNotFoundException(404) 핸들러 추가
- [ ] T033 [USER] `src/docs/asciidoc/index.adoc`에 User 도메인 스니펫 include 추가

**체크포인트**: User 도메인 전체 독립 동작 확인 → PR 가능

---

## Phase 5: Post 도메인

**목표**: 게시글 조회·작성·수정·삭제를 한 번에 완성

**독립 테스트**:
- `GET /posts` → 200, 목록 반환 (비인증)
- `GET /posts/{id}` → 200/404 (비인증)
- `POST /posts` → 201 (인증)
- `PATCH /posts/{id}` → 200/403/404 (인증, 본인 게시글만)
- `DELETE /posts/{id}` → 204/403/404 (인증, 본인 게시글만)

- [ ] T034 [P] [POST] `src/main/java/com/daily_diary/backend/post/entity/Post.java` 생성 — @Entity, id/title/content/user/createdAt/updatedAt 필드, of() 팩토리, changeTitle()/changeContent() 메서드
- [ ] T035 [P] [POST] `src/main/java/com/daily_diary/backend/post/infra/PostRepository.java` 생성 — JpaRepository 상속, findAllByOrderByCreatedAtDesc(Pageable) 메서드 선언
- [ ] T036 [P] [POST] `src/main/java/com/daily_diary/backend/post/exception/PostNotFoundException.java` 생성 — BusinessException 상속
- [ ] T037 [P] [POST] `src/main/java/com/daily_diary/backend/post/exception/PostAccessDeniedException.java` 생성 — BusinessException 상속
- [ ] T038 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/PostCreateRequest.java` 생성 — record(title, content), @NotBlank @Size 검증
- [ ] T039 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/PostUpdateRequest.java` 생성 — record(title, content), @NotBlank @Size 검증
- [ ] T040 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/PostSummaryResponse.java` 생성 — record(id, title, nickname, createdAt)
- [ ] T041 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/PostResponse.java` 생성 — record(id, title, content, nickname, createdAt, updatedAt)
- [ ] T042 [POST] `src/main/java/com/daily_diary/backend/post/service/PostService.java` 생성 — getList(Pageable) → Page<PostSummaryResponse>, getOne(id) → PostResponse, create(userId, request) → PostResponse, update(userId, postId, request) → PostResponse (작성자 검증), delete(userId, postId) (작성자 검증) 메서드 구현
- [ ] T043 [POST] `src/main/java/com/daily_diary/backend/post/web/PostController.java` 생성 — GET /posts (200), GET /posts/{id} (200/404), POST /posts (201), PATCH /posts/{id} (200/403/404), DELETE /posts/{id} (204/403/404) 엔드포인트
- [ ] T044 [POST] `src/test/java/com/daily_diary/backend/post/web/PostControllerTest.java` 생성 — @WebMvcTest + @AutoConfigureRestDocs, 전체 엔드포인트 테스트 및 document() 스니펫 생성 (post/list, post/detail, post/create, post/update, post/delete)
- [ ] T045 [POST] `GlobalExceptionHandler.java`에 PostNotFoundException(404), PostAccessDeniedException(403) 핸들러 추가
- [ ] T046 [POST] `src/docs/asciidoc/index.adoc`에 Post 도메인 스니펫 include 추가

**체크포인트**: Post 도메인 전체 독립 동작 확인 → PR 가능

---

## Phase 6: 마무리

- [ ] T047 `./gradlew asciidoctor` 실행하여 `build/docs/asciidoc/index.html` 문서 생성 확인
- [ ] T048 quickstart.md 기준으로 전체 시나리오 수동 검증 (회원가입 → 로그인 → 게시글 작성 → 조회 → 수정 → 삭제 → 토큰 갱신 → 탈퇴)
- [ ] T049 [P] `src/test/resources/application-test.properties` H2 설정 확인 (datasource, dialect, ddl-auto=create-drop)

---

## 의존성 및 실행 순서

### Phase 의존성

- **Phase 1 (설정)**: 즉시 시작
- **Phase 2 (Global)**: Phase 1 완료 후 — 모든 도메인 블로킹
- **Phase 3 (Auth)**: Phase 2 완료 후
- **Phase 4 (User)**: Phase 2 완료 후 — Auth와 병렬 가능하나 signup 구현이 User 엔티티 필요
- **Phase 5 (Post)**: Phase 4 완료 후 (Post가 User FK 참조)
- **Phase 6 (마무리)**: 모든 도메인 완료 후

### 도메인 내 실행 순서

entity [P] + infra [P] + exception [P] + DTO [P] → service → controller → 핸들러 추가

### 병렬 기회

```bash
# Phase 2 내 병렬 실행 가능
T004 GlobalExceptionHandler 골격
T005 JwtProvider

# Phase 3 내 병렬 실행 가능 (T017 전)
T008 RefreshToken 엔티티
T009 RefreshTokenRepository
T010 InvalidCredentialsException
T011 InvalidTokenException
T012 SignupRequest
T013 LoginRequest
T014 LoginResponse
T015 TokenRefreshRequest
T016 TokenRefreshResponse
```

---

## 구현 전략

### MVP (Auth 도메인만)

1. Phase 1 + Phase 2 완료
2. Phase 3 (Auth) 완료
3. **중지 및 검증** → PR #1: Auth 도메인

### 점진적 PR 전략

- **PR #1** — Global + Auth: 보안 인프라 + 인증 전체
- **PR #2** — User: 프로필 관리
- **PR #3** — Post: 게시글 CRUD

---

## 참고

- [P] 태스크 = 다른 파일, 의존성 없어 병렬 실행 가능
- GlobalExceptionHandler는 각 도메인 Phase에서 핸들러를 점진적으로 추가
- `./gradlew test`로 전체 테스트 실행
