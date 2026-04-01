# 코딩 컨벤션 (Coding Conventions)

## Controller

- `@RequiredArgsConstructor`로 생성자 주입
- 반환 타입은 `ResponseEntity<T>` 사용
- API 응답 래퍼 클래스 없이 데이터 직접 반환

## Service

- `@RequiredArgsConstructor`로 생성자 주입
- 클래스 레벨에 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional` 개별 적용

## Entity

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

## DTO

- 모든 DTO는 `record` 사용
- 네이밍: `XxxRequest` / `XxxResponse`

## Exception

- 모든 커스텀 예외는 `global/exception/BusinessException`을 상속
- `global/exception/GlobalExceptionHandler` (`@ControllerAdvice`)에서 전역 처리
- 단순 조건 검사 후 throw하는 if문은 `validate~` / `valid~` private 메서드로 추출

## DI

- 모든 의존성 주입은 `@RequiredArgsConstructor` + `private final` 필드
