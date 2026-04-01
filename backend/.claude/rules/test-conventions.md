# 테스트 컨벤션 (Test Conventions)

## 테스트 전략

- Controller: `@WebMvcTest` + `@AutoConfigureRestDocs` + `@Import(SecurityConfig.class)`
- Repository: `@DataJpaTest`
- Service: `@ExtendWith(MockitoExtension.class)`
- 통합 테스트: `@SpringBootTest` + H2 인메모리 DB + `@ActiveProfiles("test")`

통합 테스트용 설정: `src/test/resources/application-test.properties`

### Controller 테스트 주의사항

- `@Import(SecurityConfig.class)`로 실제 Security 설정을 로드한다 (MockBean으로 우회하지 않음)
- 인증이 필요한 요청은 `.with(csrf())`를 적용한다
- 의존성 Mock은 `@MockitoBean`을 사용한다

## Given-When-Then 구조

모든 테스트는 `// given / // when / // then` 주석으로 구분한다.

- 정상 케이스: 3단 분리
- 예외 케이스: `assertThatThrownBy`가 when+then을 동시에 표현하므로 `// when & then`으로 합친다
- given이 없는 경우(사전 조건 없음): `// given` 생략

```java
@Test
void login_정상() {
    // given
    given(authenticationManager.authenticate(any())).willReturn(auth);
    given(jwtProvider.createAccessToken(1L)).willReturn("access-token");

    // when
    Tokens tokens = authService.login(new LoginRequest("testuser", "password123"));

    // then
    assertThat(tokens.accessToken()).isEqualTo("access-token");
}

@Test
void login_잘못된_credentials_예외() {
    // given
    given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException(""));

    // when & then
    assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "wrong")))
            .isInstanceOf(InvalidCredentialsException.class);
}
```

## verify 사용 기준

반환값으로 증명할 수 없는 중요한 부수 효과가 있을 때만 `verify`를 사용한다.

- `void` 메서드 — save/delete/invalidate 등 호출 여부 자체가 검증 포인트인 경우
- 캐시 put/invalidate 같은 부수 효과 — 반환값에 드러나지 않는 동작
- 반환값으로 이미 동작이 증명되는 경우(getPost, update 등)에는 생략

## API 문서 (Spring REST Docs)

Controller 테스트에서 `MockMvc`로 엔드포인트를 호출할 때 반드시 `document()`로 스니펫을 생성한다.
테스트가 통과해야 문서가 생성되므로, 문서와 실제 동작이 항상 일치한다.

```java
@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;

    @Test
    void signup() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test@test.com\",\"password\":\"password123\",\"nickname\":\"테스터\"}"))
            .andExpect(status().isCreated())
            .andDo(document("auth/signup",
                requestFields(
                    fieldWithPath("username").description("사용자명"),
                    fieldWithPath("password").description("비밀번호"),
                    fieldWithPath("nickname").description("닉네임")
                ),
                responseFields(
                    fieldWithPath("id").description("사용자 ID"),
                    fieldWithPath("username").description("사용자명"),
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
