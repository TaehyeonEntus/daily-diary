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
- [x] T001-A `build.gradle`에 spring-boot-starter-cache + caffeine 의존성 추가, spring-boot-starter-data-redis 제거
- [x] T001-B `src/main/java/com/daily_diary/backend/global/config/CacheConfig.java` 생성 — @EnableCaching, CaffeineCacheManager 빈 설정, refreshTokens 캐시 TTL = jwt.refresh-token-expiry
- [x] T002 `src/main/resources/application.properties`에 jwt.secret, jwt.access-token-expiry(1800000), jwt.refresh-token-expiry(604800000) 설정 추가
- [x] T003 [P] `src/docs/asciidoc/index.adoc` 생성 — API 문서 목차 및 각 도메인 스니펫 include 골격 작성

---

## Phase 2: Global — 횡단 관심사

**목적**: 모든 도메인이 공유하는 보안 및 예외 처리 인프라

**⚠️ 중요**: 이 Phase가 완료되기 전에는 어떤 도메인도 시작 불가

- [x] T004 `src/main/java/com/daily_diary/backend/global/entity/BaseEntity.java` 생성 — `@MappedSuperclass`, createdAt/updatedAt 필드, `@PrePersist`/`@PreUpdate` 자동 설정
- [x] T005 `src/main/java/com/daily_diary/backend/global/exception/BusinessException.java` 생성 — RuntimeException 상속, message 생성자
- [x] T006 [P] `src/main/java/com/daily_diary/backend/global/exception/GlobalExceptionHandler.java` 생성 — @RestControllerAdvice 골격만 작성 (도메인 예외 핸들러는 각 도메인 Phase에서 추가)
- [x] T007 [P] `src/main/java/com/daily_diary/backend/global/security/JwtProvider.java` 생성 — 액세스/리프레시 토큰 생성, 검증, Claims 파싱, @Value로 secret/expiry 주입
- [x] T008 `src/main/java/com/daily_diary/backend/global/security/JwtFilter.java` 생성 — OncePerRequestFilter 상속, Authorization 헤더에서 토큰 추출 후 SecurityContextHolder 설정
- [x] T009 `src/main/java/com/daily_diary/backend/global/security/SecurityConfig.java` 생성 — SecurityFilterChain 빈, /auth/signup·/auth/login·/auth/refresh·GET /posts·GET /posts/** permitAll, 나머지 authenticated, JwtFilter 등록, BCryptPasswordEncoder 빈 등록

**체크포인트**: 보안 인프라 완료 — 도메인 구현 시작 가능

---

## Phase 3: Auth 도메인 🎯 MVP

**목표**: 회원가입·로그인·토큰 갱신·로그아웃을 한 번에 완성

**설계 결정**:
- User 엔티티는 `username`(아이디), `password`, `nickname` 필드를 가짐
- RefreshToken은 CaffeineCache로 관리: key = userId(Long), value = refreshToken 문자열, TTL = refresh-token-expiry
- Refresh Token Rotation 적용: 갱신 시 accessToken + refreshToken 함께 재발급
- User는 hard delete (삭제 시 DB에서 실제 제거)

**독립 테스트**:
- `POST /auth/signup` → 201, 사용자 정보 반환
- `POST /auth/login` → 200, accessToken + refreshToken 반환
- `POST /auth/refresh` → 200, 새 accessToken + 새 refreshToken 반환
- `DELETE /auth/logout` → 204

- [x] T010 [P] [AUTH] `src/main/java/com/daily_diary/backend/user/entity/User.java` 생성 — BaseEntity 상속, id/username/password/nickname 필드, of() 팩토리, changeNickname() 메서드
- [x] T011 [P] [AUTH] `src/main/java/com/daily_diary/backend/user/infra/UserRepository.java` 생성 — JpaRepository 상속, findByUsername() 메서드 선언
- [x] T012 [P] [AUTH] `src/main/java/com/daily_diary/backend/user/exception/UserNotFoundException.java` 생성 — BusinessException 상속
- [x] T013 [P] [AUTH] `src/main/java/com/daily_diary/backend/user/exception/DuplicateUsernameException.java` 생성 — BusinessException 상속
- [x] T014 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/exception/InvalidCredentialsException.java` 생성 — BusinessException 상속
- [x] T015 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/exception/InvalidTokenException.java` 생성 — BusinessException 상속
- [x] T016 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/SignupRequest.java` 생성 — record(username, password, nickname), @NotBlank @Size 검증
- [x] T017 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/LoginRequest.java` 생성 — record(username, password), @NotBlank 검증
- [x] T018 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/LoginResponse.java` 생성 — record(accessToken, refreshToken)
- [x] T019 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/TokenRefreshRequest.java` 생성 — record(refreshToken), @NotBlank 검증
- [x] T020 [P] [AUTH] `src/main/java/com/daily_diary/backend/auth/web/TokenRefreshResponse.java` 생성 — record(accessToken, refreshToken) — Refresh Token Rotation으로 두 토큰 함께 반환
- [x] T021 [AUTH] `src/main/java/com/daily_diary/backend/auth/service/AuthService.java` 생성 — signup(username 중복 검사, BCrypt 해시, User 저장), login(자격증명 검증, CaffeineCache에 refreshToken 저장), refresh(JWT 검증 → 캐시에서 토큰 비교 → 기존 캐시 삭제 → 새 accessToken+refreshToken 발급 및 저장, Rotation), logout(캐시에서 삭제) 메서드 구현
- [x] T022 [AUTH] `src/main/java/com/daily_diary/backend/auth/web/AuthController.java` 생성 — POST /auth/signup (201), POST /auth/login (200), POST /auth/refresh (200), DELETE /auth/logout (204) 엔드포인트
- [x] T023 [AUTH] `src/test/java/com/daily_diary/backend/auth/web/AuthControllerTest.java` 생성 — @WebMvcTest + @Import(SecurityConfig) + @AutoConfigureRestDocs, signup/login/refresh/logout 각 엔드포인트 테스트 및 document() 스니펫 생성
- [x] T024 [AUTH] `GlobalExceptionHandler.java`에 InvalidCredentialsException(401), InvalidTokenException(401), DuplicateUsernameException(409), MethodArgumentNotValidException(400) 핸들러 추가
- [x] T025 [AUTH] `src/docs/asciidoc/index.adoc`에 Auth 도메인 스니펫 include 확인

- [x] T025-A [AUTH] `AuthService.java` 리팩토링 — StringRedisTemplate → CacheManager, refresh에 Rotation 적용, TokenRefreshResponse(accessToken, refreshToken) 반환
- [x] T025-B [AUTH] `AuthControllerTest.java` 수정 — refresh 테스트의 TokenRefreshResponse에 refreshToken 필드 추가
- [x] T025-C `BackendApplicationTests.java` 수정 — @MockitoBean StringRedisTemplate 제거
- [x] T025-D `application-dev.properties` 수정 — Redis 설정 제거

**체크포인트**: Auth 도메인 전체 독립 동작 확인 → PR 가능

---

## Phase 4: User 도메인

**목표**: 프로필 조회·수정·회원 탈퇴를 한 번에 완성

**설계 결정**:
- 회원 탈퇴는 hard delete (DB에서 실제 제거 + CaffeineCache refreshToken 삭제)
- `UserResponse` record: `id, username, nickname, createdAt`

**독립 테스트**:
- `GET /users/me` → 200, 프로필 반환
- `PATCH /users/me` → 200, 닉네임 수정 확인
- `DELETE /users/me` → 204

- [x] T026 [P] [USER] `src/main/java/com/daily_diary/backend/user/web/UserUpdateRequest.java` 생성 — record(nickname), @NotBlank @Size(max=50) 검증
- [x] T027 [P] [USER] `src/main/java/com/daily_diary/backend/user/web/UserResponse.java` 생성 — record(id, username, nickname, createdAt), UserResponse.from(User) 팩토리
- [x] T028 [USER] `src/main/java/com/daily_diary/backend/user/service/UserService.java` 생성 — getMe(userId) → UserResponse, updateMe(userId, request) → UserResponse, deleteMe(userId, hard delete + CaffeineCache refreshToken 삭제) 메서드 구현
- [x] T029 [USER] `src/main/java/com/daily_diary/backend/user/web/UserController.java` 생성 — GET /users/me (200), PATCH /users/me (200), DELETE /users/me (204) 엔드포인트, SecurityContextHolder에서 userId 추출
- [x] T030 [USER] `src/test/java/com/daily_diary/backend/user/web/UserControllerTest.java` 생성 — @WebMvcTest + @Import(SecurityConfig) + @AutoConfigureRestDocs, getMe/updateMe/deleteMe 테스트 및 document() 스니펫 생성 (user/me-get, user/me-update, user/me-delete)
- [x] T031 [USER] `GlobalExceptionHandler.java`에 UserNotFoundException(404) 핸들러 추가
- [x] T032 [USER] `src/docs/asciidoc/index.adoc`에 User 도메인 스니펫 include 확인

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

- [x] T033 [P] [POST] `src/main/java/com/daily_diary/backend/post/entity/Post.java` 생성 — BaseEntity 상속, id/title/content/user 필드, of() 팩토리, changeTitle()/changeContent() 메서드
- [x] T034 [P] [POST] `src/main/java/com/daily_diary/backend/post/infra/PostRepository.java` 생성 — JpaRepository 상속, findAllByOrderByCreatedAtDesc(Pageable) 메서드 선언
- [x] T035 [P] [POST] `src/main/java/com/daily_diary/backend/post/exception/PostNotFoundException.java` 생성 — BusinessException 상속
- [x] T036 [P] [POST] `src/main/java/com/daily_diary/backend/post/exception/PostAccessDeniedException.java` 생성 — BusinessException 상속
- [x] T037 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/CreatePostRequest.java` 생성 — record(title, content), @NotBlank @Size 검증
- [x] T038 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/UpdatePostRequest.java` 생성 — record(title, content), @NotBlank @Size 검증
- [x] T039 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/PostSummaryResponse.java` 생성 — record(id, title, nickname, createdAt)
- [x] T040 [P] [POST] `src/main/java/com/daily_diary/backend/post/web/PostResponse.java` 생성 — record(id, title, content, nickname, createdAt, updatedAt)
- [x] T041 [POST] `src/main/java/com/daily_diary/backend/post/service/PostService.java` 생성 — getList(Pageable) → Page<PostSummaryResponse>, getOne(id) → PostResponse, create(userId, request) → PostResponse, update(userId, postId, request) → PostResponse (작성자 검증), delete(userId, postId) (작성자 검증) 메서드 구현
- [x] T042 [POST] `src/main/java/com/daily_diary/backend/post/web/PostController.java` 생성 — GET /posts (200), GET /posts/{id} (200/404), POST /posts (201), PATCH /posts/{id} (200/403/404), DELETE /posts/{id} (204/403/404) 엔드포인트
- [x] T043 [POST] `src/test/java/com/daily_diary/backend/post/web/PostControllerTest.java` 생성 — @WebMvcTest + @Import(SecurityConfig) + @AutoConfigureRestDocs, 전체 엔드포인트 테스트 및 document() 스니펫 생성
- [x] T044 [POST] `GlobalExceptionHandler.java`에 PostNotFoundException(404), PostAccessDeniedException(403) 핸들러 추가
- [x] T045 [POST] `src/docs/asciidoc/index.adoc`에 Post 도메인 스니펫 include 확인

**체크포인트**: Post 도메인 전체 독립 동작 확인 → PR 가능

---

## Phase 6: 마무리

- [x] T046 `./gradlew asciidoctor` 실행하여 `build/docs/asciidoc/index.html` 문서 생성 확인
- [ ] T047 quickstart.md 기준으로 전체 시나리오 수동 검증 (회원가입 → 로그인 → 게시글 작성 → 조회 → 수정 → 삭제 → 토큰 갱신 → 탈퇴)

---

## 의존성 및 실행 순서

### Phase 의존성

- **Phase 1 (설정)**: 완료 ✅
- **Phase 2 (Global)**: 완료 ✅
- **Phase 3 (Auth)**: 완료 ✅
- **Phase 4 (User)**: 완료 ✅
- **Phase 5 (Post)**: 완료 ✅
- **Phase 6 (마무리)**: 진행 중

### 도메인 내 실행 순서

entity [P] + exception [P] + DTO [P] → service → controller → 핸들러 추가

### 테스트 패턴 (@WebMvcTest)

```java
@WebMvcTest(XxxController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs
class XxxControllerTest {
    @MockitoBean XxxService xxxService;
    @MockitoBean JwtProvider jwtProvider;   // SecurityConfig 의존성

    // 인증 필요 엔드포인트:
    given(jwtProvider.validate("token")).willReturn(true);
    given(jwtProvider.getUserId("token")).willReturn(1L);
}
```

---

## 참고

- [P] 태스크 = 다른 파일, 의존성 없어 병렬 실행 가능
- GlobalExceptionHandler는 각 도메인 Phase에서 핸들러를 점진적으로 추가
- `./gradlew test`로 전체 테스트 실행
