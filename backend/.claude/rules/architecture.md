# 패키지 구조 (Architecture)

Base package: `com.daily_diary.backend`

도메인 우선(Domain-First) 패키지 구조를 사용한다.
각 도메인은 자신의 하위 패키지에 모든 레이어를 포함한다.
횡단 관심사(보안, 전역 예외 처리)는 `global/`에 위치한다.

```
com.daily_diary.backend
├── auth/
│   ├── config/     # CacheConfig (Caffeine 기반 refresh token 인메모리 캐시 설정)
│   ├── service/    # AuthService, Tokens (서비스 내부 반환용 record)
│   ├── web/        # AuthController, LoginRequest, SignupRequest, LoginResponse, RefreshResponse
│   └── exception/  # InvalidCredentialsException, InvalidTokenException
├── user/
│   ├── entity/     # User (extends BaseEntity)
│   ├── infra/      # UserRepository
│   ├── service/    # UserService
│   ├── web/        # UserController, UserDetailResponse, UserNicknameUpdateRequest
│   └── exception/  # UserNotFoundException, DuplicateUsernameException
├── post/
│   ├── entity/     # Post (extends BaseEntity)
│   ├── infra/      # PostRepository
│   ├── service/    # PostService
│   ├── web/        # PostController, CreatePostRequest, UpdatePostRequest,
│   │               # PostResponse, PostSummaryResponse, PostListResponse
│   └── exception/  # PostNotFoundException, PostAccessDeniedException
└── global/
    ├── entity/     # BaseEntity (createdAt, updatedAt 공통 감사 필드)
    ├── security/   # SecurityConfig, JwtFilter, JwtProvider,
    │               # CustomUserDetails (record), CustomUserDetailsService
    └── exception/  # BusinessException, GlobalExceptionHandler (@RestControllerAdvice)
```

## 패키지 역할

- `{domain}/web/` — HTTP 요청/응답 처리. Controller와 DTO(record)만 위치
- `{domain}/service/` — 비즈니스 로직
- `{domain}/infra/` — JPA Repository 인터페이스
- `{domain}/entity/` — JPA Entity
- `{domain}/exception/` — 도메인별 커스텀 예외 (BusinessException 상속)
- `{domain}/config/` — 도메인별 설정 클래스
- `global/entity/` — 공통 JPA 감사(audit) 필드를 가진 추상 클래스 (BaseEntity)
- `global/security/` — JWT 발급/검증, Security 필터 체인, Spring Security UserDetails 구현
- `global/exception/` — BusinessException 베이스 클래스, 전역 예외 핸들러

## 주요 설계 결정 사항

### Refresh Token 저장 방식
Refresh token은 JPA Entity가 아닌 **Caffeine 인메모리 캐시** (`Cache<Long, String>`)로 관리한다.
- `auth/config/CacheConfig.java` — TTL은 `jwt.refresh-token-expiry` 설정값 사용
- `AuthService`와 `UserService` 모두 캐시를 주입받아 사용 (로그인/로그아웃/회원탈퇴)
- 서버 재시작 시 캐시가 초기화되므로 모든 refresh token이 무효화됨

### 도메인 간 의존
- `PostService` → `UserRepository` (게시글 작성 시 User 엔티티 조회)
- `AuthService` → `UserRepository` (회원가입/로그인 시 User 엔티티 조회)
- `UserService` → `Cache<Long, String>` (회원탈퇴 시 refresh token 무효화)
- `CustomUserDetailsService` → `UserRepository` (Spring Security 인증 처리)
