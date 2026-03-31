# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Spring Boot 3 REST API backend for a daily diary application ("일기를 써봅시다").

## Commands

```bash
# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.daily_diary.backend.BackendApplicationTests"

# Build
./gradlew build

# Clean build artifacts
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Tech Stack

- **Java 17** (toolchain), runtime is JDK 21
- **Spring Boot 3.5** with Spring MVC, Spring Security 6, Spring Data JPA
- **MySQL** via `mysql-connector-j` (runtime dependency)
- **Hibernate 6** ORM, HikariCP connection pool
- **Lombok** for boilerplate reduction
- **QueryDSL 5.0** (jakarta) — 동적 쿼리. Q클래스는 `build/generated/querydsl`에 생성됨
- **jjwt 0.12.6** — JWT 액세스/리프레시 토큰 발급 및 검증
- **H2** — 테스트 전용 인메모리 DB
- **GitHub Actions** — CI (`backend/**` 변경 시 build + test 자동 실행)

## Architecture

Base package: `com.daily_diary.backend`

도메인 우선(Domain-First) 패키지 구조를 사용한다.
각 도메인은 자신의 하위 패키지에 모든 레이어를 포함한다.
횡단 관심사(보안, 전역 예외 처리)는 `global/`에 위치한다.

```
com.daily_diary.backend
├── auth/
│   ├── web/        # AuthController, DTO (record)
│   ├── service/    # AuthService
│   ├── infra/      # RefreshTokenRepository
│   ├── entity/     # RefreshToken
│   └── exception/  # InvalidCredentialsException, InvalidTokenException
├── user/
│   ├── web/        # UserController, DTO (record)
│   ├── service/    # UserService
│   ├── infra/      # UserRepository
│   ├── entity/     # User
│   └── exception/  # UserNotFoundException, DuplicateEmailException
├── post/
│   ├── web/        # PostController, DTO (record)
│   ├── service/    # PostService
│   ├── infra/      # PostRepository
│   ├── entity/     # Post
│   └── exception/  # PostNotFoundException, PostAccessDeniedException
└── global/
    ├── security/   # SecurityConfig, JwtFilter, JwtProvider
    └── exception/  # BusinessException, GlobalExceptionHandler
```

패키지 역할:
- `{domain}/web/` — HTTP 요청/응답 처리. Controller와 DTO(record)만 위치
- `{domain}/service/` — 비즈니스 로직. Repository 인터페이스에만 의존
- `{domain}/infra/` — JPA Repository 인터페이스
- `{domain}/entity/` — JPA Entity
- `{domain}/exception/` — 도메인별 커스텀 예외 (BusinessException 상속)
- `global/security/` — JWT 필터, 토큰 유틸리티, SecurityFilterChain 설정
- `global/exception/` — BusinessException 베이스 클래스, GlobalExceptionHandler

## Database

`src/main/resources/application.properties`에 로컬 설정:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/daily_diary
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT
jwt.secret=<최소 256비트 시크릿 키>
jwt.access-token-expiry=1800000
jwt.refresh-token-expiry=604800000
```

## Security

Spring Security + JWT 기반 인증. `global/security/SecurityConfig.java`에서 `SecurityFilterChain` 빈을 설정하며, `JwtFilter`를 필터 체인에 등록한다.

공개 엔드포인트 (인증 불필요):
- `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh`
- `GET /posts`, `GET /posts/**`

## Coding Conventions

### Controller
- `@RequiredArgsConstructor`로 생성자 주입
- 반환 타입은 `ResponseEntity<T>` 사용
- API 응답 래퍼 클래스 없이 데이터 직접 반환

### Service
- `@RequiredArgsConstructor`로 생성자 주입
- 클래스 레벨에 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional` 개별 적용

### Entity
- `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 적용 (Lombok)
- `@Setter` 절대 사용 금지
- 정적 생성자는 `of(...)` 형태로 정의
- 필드 변경 메서드는 `change~()` 형태로 정의

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    public static User of(String email, String password, String nickname) {
        User user = new User();
        user.email = email;
        user.password = password;
        user.nickname = nickname;
        return user;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}
```

### DTO
- 모든 DTO는 `record` 사용
- 네이밍: `XxxRequest` / `XxxResponse`

### Exception
- 모든 커스텀 예외는 `global/exception/BusinessException`을 상속
- `global/exception/GlobalExceptionHandler` (`@ControllerAdvice`)에서 전역 처리

### DI
- 모든 의존성 주입은 `@RequiredArgsConstructor` + `private final` 필드

### Test
- Controller: `@WebMvcTest` + `@AutoConfigureRestDocs`
- Repository: `@DataJpaTest`
- Service: `@ExtendWith(MockitoExtension.class)`
- 통합 테스트: `@SpringBootTest` + H2 인메모리 DB + `@ActiveProfiles("test")`

통합 테스트용 설정: `src/test/resources/application-test.properties`

### API 문서 (Spring REST Docs)

Controller 테스트에서 `MockMvc`로 엔드포인트를 호출할 때 반드시 `document()`로 스니펫을 생성한다.
테스트가 통과해야 문서가 생성되므로, 문서와 실제 동작이 항상 일치한다.

```java
@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void signup() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"password\":\"password123\",\"nickname\":\"테스터\"}"))
            .andExpect(status().isCreated())
            .andDo(document("auth/signup",
                requestFields(
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("password").description("비밀번호"),
                    fieldWithPath("nickname").description("닉네임")
                ),
                responseFields(
                    fieldWithPath("id").description("사용자 ID"),
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("nickname").description("닉네임")
                )
            ));
    }
}
```

스니펫은 `build/generated-snippets/`에 생성되며,
`src/docs/asciidoc/index.adoc`에서 `include` 지시어로 조합하여 최종 HTML 문서를 생성한다.

```bash
# 문서 생성 (테스트 실행 후 Asciidoctor 빌드)
./gradlew asciidoctor

# 생성된 문서 위치
build/docs/asciidoc/index.html
```

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
