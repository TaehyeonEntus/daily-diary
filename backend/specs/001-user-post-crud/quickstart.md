# 빠른 시작 가이드: User 및 게시글 CRUD with JWT 인증

**작성일**: 2026-03-30

## 사전 준비

- JDK 21
- MySQL 8+
- Gradle (또는 `gradlew` 래퍼 사용)

---

## 1. 의존성 추가

`build.gradle`에 다음을 추가한다:

```groovy
// JWT
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

// 입력값 검증
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

---

## 2. 환경 설정

`src/main/resources/application.properties`에 추가:

```properties
# DB
spring.datasource.url=jdbc:mysql://localhost:3306/daily_diary
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT
jwt.secret=<최소 256비트 이상의 시크릿 키>
jwt.access-token-expiry=1800000
jwt.refresh-token-expiry=604800000
```

`src/test/resources/application-test.properties` 확인:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
```

---

## 3. 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

---

## 4. 동작 확인

### 회원가입

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","nickname":"테스터"}'
```

### 로그인

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
# → accessToken, refreshToken 수령
```

### 게시글 작성

```bash
curl -X POST http://localhost:8080/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"title":"첫 게시글","content":"내용입니다."}'
```

### 게시글 목록 조회 (비인증)

```bash
curl http://localhost:8080/posts
```
