# 리서치: User 및 게시글 CRUD with JWT 인증

**작성일**: 2026-03-30
**기반 명세**: [spec.md](spec.md)

## JWT 라이브러리 선택

**결정**: `jjwt` (io.jsonwebtoken) 0.12.x

**근거**:
- Spring Boot 3.x / Jakarta EE 호환
- 액세스 토큰 + 리프레시 토큰 모두 지원
- 업계 표준 라이브러리, 풍부한 레퍼런스

**검토한 대안**:
- `spring-security-oauth2-resource-server` (JWT 검증 전용, 발급 기능 미포함 → 불채택)
- `nimbus-jose-jwt` (기능 충분하나 jjwt 대비 설정 복잡 → 불채택)

---

## 리프레시 토큰 저장 방식

**결정**: DB 저장 (`refresh_tokens` 테이블)

**근거**:
- Redis 등 별도 인프라 없이 구현 가능
- 로그아웃 시 토큰 무효화(블랙리스트) 구현 가능
- v1 규모에서 DB 조회 성능 충분

**검토한 대안**:
- Redis 저장: 성능 우수하나 별도 인프라 필요 → v2 이상에서 검토
- 무상태 방식(DB 미저장): 로그아웃 시 즉시 무효화 불가 → 불채택

---

## 비밀번호 해싱

**결정**: `BCryptPasswordEncoder` (Spring Security 내장)

**근거**:
- Spring Security 표준 제공, 별도 의존성 불필요
- 솔트 자동 처리, 단방향 해싱

---

## 회원 탈퇴 처리 방식

**결정**: 소프트 삭제 (`deleted_at` 컬럼)

**근거**:
- 탈퇴 사용자의 게시글 작성자 정보 보존 가능
- 데이터 복구 여지 확보

---

## 토큰 만료 시간

**결정**:
- 액세스 토큰: 30분
- 리프레시 토큰: 7일

**근거**: 일반적인 웹 서비스 기준. 보안과 UX의 균형.

---

## API 문서화 도구

**결정**: Spring REST Docs + Asciidoctor

**근거**:
- 테스트가 통과해야 문서가 생성되므로 문서와 실제 동작이 항상 일치
- Swagger와 달리 프로덕션 코드에 어노테이션 오염 없음
- MockMvc 테스트와 자연스럽게 통합

**문서 생성 흐름**:
```
@WebMvcTest + MockMvc 테스트 실행
→ build/generated-snippets/ 에 .adoc 스니펫 생성
→ src/docs/asciidoc/index.adoc 에서 스니펫 include
→ Asciidoctor 빌드 → build/docs/asciidoc/index.html 생성
```

**참고**: Spring REST Docs는 Controller 테스트 작성이 필수. 테스트 없이는 문서 생성 불가.

---

## 추가된 Gradle 의존성

```groovy
// JWT
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

// 입력값 검증
implementation 'org.springframework.boot:spring-boot-starter-validation'

// Spring REST Docs
testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
```

```groovy
// build.gradle 플러그인
plugins {
    id 'org.asciidoctor.jvm.convert' version '3.3.2'
}

// build.gradle 설정
ext {
    snippetsDir = file('build/generated-snippets')
}

test {
    outputs.dir snippetsDir
}

asciidoctor {
    inputs.dir snippetsDir
    dependsOn test
}
```
