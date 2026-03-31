# 구현 플랜: User 및 게시글 CRUD with JWT 인증

**브랜치**: `001-user-post-crud` | **작성일**: 2026-03-30 | **명세**: [spec.md](spec.md)

## 요약

JWT 액세스/리프레시 토큰 기반 인증과 User·Post 엔티티의 CRUD API를 구현한다.
Spring Security 필터 체인에 JWT 필터를 등록하고, 인증 불필요 엔드포인트(게시글 조회)와
인증 필요 엔드포인트(게시글 작성·수정·삭제, 프로필 관리)를 분리한다.

## 기술 컨텍스트

**Language/Version**: Java 17 (툴체인), 런타임 JDK 21
**Primary Dependencies**: Spring Boot 3.5, Spring Security 6, Spring Data JPA, jjwt 0.12.6, Lombok, QueryDSL 5.0
**Storage**: MySQL 8+ (운영), H2 인메모리 (테스트)
**Testing**: JUnit 5, MockMvc (@WebMvcTest), @DataJpaTest, Mockito
**Target Platform**: Linux 서버 (REST API)
**Project Type**: Web Service (REST API)
**Performance Goals**: 일반적인 개인 블로그 수준 (특별한 성능 목표 없음)
**Constraints**: 표준 REST 응답 시간 이내
**Scale/Scope**: 개인 사용자 규모, 단일 인스턴스

## 컨스티튜션 검사

### Gate 1 — 레이어드 아키텍처 ✅

모든 클래스는 `web/`, `infra/`, `service/`, `exception/`, `domain/` 중 하나에 배치.
의존성 방향: `web` → `service` → `domain`, `infra` → `domain`.

### Gate 2 — 불변 도메인 모델 ✅

`User`, `Post`, `RefreshToken` 엔티티 모두 `@Getter` + `@NoArgsConstructor(PROTECTED)` 적용.
`@Setter` 미사용. `of(...)` 팩토리 + `change*()` 메서드 패턴 준수.

### Gate 3 — 생성자 주입 전용 ✅

모든 Service, Controller에 `@RequiredArgsConstructor` + `private final` 사용.

### Gate 4 — 중앙 집중 예외 처리 ✅

`BusinessException` 상속 커스텀 예외, 단일 `@ControllerAdvice`에서 전역 처리.

### Gate 5 — 테스트 레이어 정합성 ✅

Controller: `@WebMvcTest`, Repository: `@DataJpaTest`, Service: Mockito, 통합: `@SpringBootTest`.

## 프로젝트 구조

### 문서 (이 기능)

```text
specs/001-user-post-crud/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── auth-api.md
│   ├── user-api.md
│   └── post-api.md
└── tasks.md
```

### 소스코드

```text
src/main/java/com/daily_diary/backend/
├── auth/
│   ├── web/
│   │   ├── AuthController.java
│   │   ├── SignupRequest.java
│   │   ├── LoginRequest.java
│   │   ├── TokenRefreshRequest.java
│   │   ├── LoginResponse.java
│   │   └── TokenRefreshResponse.java
│   ├── service/
│   │   └── AuthService.java
│   ├── infra/
│   │   └── RefreshTokenRepository.java
│   ├── entity/
│   │   └── RefreshToken.java
│   └── exception/
│       ├── InvalidCredentialsException.java
│       └── InvalidTokenException.java
├── user/
│   ├── web/
│   │   ├── UserController.java
│   │   ├── UserUpdateRequest.java
│   │   └── UserResponse.java
│   ├── service/
│   │   └── UserService.java
│   ├── infra/
│   │   └── UserRepository.java
│   ├── entity/
│   │   └── User.java
│   └── exception/
│       ├── UserNotFoundException.java
│       └── DuplicateEmailException.java
├── post/
│   ├── web/
│   │   ├── PostController.java
│   │   ├── PostCreateRequest.java
│   │   ├── PostUpdateRequest.java
│   │   ├── PostResponse.java
│   │   └── PostSummaryResponse.java
│   ├── service/
│   │   └── PostService.java
│   ├── infra/
│   │   └── PostRepository.java
│   ├── entity/
│   │   └── Post.java
│   └── exception/
│       ├── PostNotFoundException.java
│       └── PostAccessDeniedException.java
└── global/
    ├── security/
    │   ├── SecurityConfig.java
    │   ├── JwtFilter.java
    │   └── JwtProvider.java
    └── exception/
        ├── BusinessException.java
        └── GlobalExceptionHandler.java

src/test/java/com/daily_diary/backend/
├── auth/
│   ├── web/
│   │   └── AuthControllerTest.java
│   └── service/
│       └── AuthServiceTest.java
├── user/
│   ├── web/
│   │   └── UserControllerTest.java
│   ├── service/
│   │   └── UserServiceTest.java
│   └── infra/
│       └── UserRepositoryTest.java
└── post/
    ├── web/
    │   └── PostControllerTest.java
    ├── service/
    │   └── PostServiceTest.java
    └── infra/
        └── PostRepositoryTest.java
```

**구조 결정**: 도메인 우선(Domain-First) 패키지 구조. 횡단 관심사(보안, 전역 예외)는 `global/`에 분리.

## 복잡성 추적

컨스티튜션 위반 없음 — 해당 없음.
