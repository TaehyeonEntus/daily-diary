# 코딩 컨벤션 (Coding Conventions)

## Controller

- `@RequiredArgsConstructor`로 생성자 주입
- 반환 타입은 `ResponseEntity<T>` 사용
- API 응답 래퍼 클래스 없이 데이터 직접 반환
- CR 작업은 항상 리소스를 반환한다
  - Create (POST): `201 Created` + 생성된 리소스
  - Read (GET): `200 OK` + 리소스
  - Update (PATCH/PUT): `204 No Content` (body 없음)
  - Delete (DELETE): `204 No Content` (body 없음)

## Service

- `@RequiredArgsConstructor`로 생성자 주입
- 클래스 레벨에 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional` 개별 적용
- private 메서드는 구분선 주석으로 분리한다

```java
// ─── private ──────────────────────────────────────────────────────────────

private void validateOwner(...) { ... }
```

- 단순 조건 검사 후 throw하는 if문은 `validate~` / `valid~` private 메서드로 추출

## Entity

- `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 적용 (Lombok)
- `@Setter` 절대 사용 금지
- 정적 생성자는 `of(...)` 형태로 정의
- 필드 변경 메서드는 `change~()` 형태로 정의
- 정적 생성자와 변경자는 구분선 주석으로 분리한다

```java
// ─── 정적 생성자 ──────────────────────────────────────────────────────────

public static Post of(...) { ... }

// ─── 변경자 ───────────────────────────────────────────────────────────────

public void changeTitle(...) { ... }
```

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

## Repository

- 조회 후 없으면 예외를 던지는 패턴은 `findOrThrow` default 메서드로 정의한다

```java
public interface PostRepository extends JpaRepository<Post, Long> {
    default Post findOrThrow(Long id) {
        return findById(id).orElseThrow(PostNotFoundException::new);
    }
}
```

## QueryDSL Repository

- `BooleanExpression`은 조건별로 private 메서드로 추출한다 (null 반환 시 QueryDSL이 자동으로 조건 무시)
- 검색 조건(`PostSearchCondition`)과 정렬 조건(`OrderType`)은 분리한다
  - 검색: `SearchType` (DEFAULT·NICKNAME·TITLE·CONTENT) + `keyword`
  - 정렬: `OrderType` (DATE·VIEW·LIKE)

```java
private BooleanExpression searchBy(PostSearchCondition search) {
    if (!StringUtils.hasText(search.keyword()))
        return null;
    return switch (search.searchType()) {
        case NICKNAME -> post.user.nickname.containsIgnoreCase(search.keyword());
        case TITLE    -> post.title.containsIgnoreCase(search.keyword());
        case CONTENT  -> post.content.containsIgnoreCase(search.keyword());
        default       -> null;
    };
}
```

## DI

- 모든 의존성 주입은 `@RequiredArgsConstructor` + `private final` 필드

## 일반

- `var` 사용 금지. 모든 변수는 명시적 타입으로 선언한다
