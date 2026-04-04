# Research: Post 검색 및 ON DELETE CASCADE

## Decision 1: DB-level FK CASCADE 구현 방법

**Decision**: Hibernate `@OnDelete(action = OnDeleteAction.CASCADE)` 사용

**Rationale**:
- `ddl-auto=create` 환경에서 `ON DELETE CASCADE` DDL이 자동 생성됨
- JPA `CascadeType.ALL`과 달리 DB 레벨에서 동작하므로 어떤 경로(직접 SQL, 다른 서비스)로 삭제해도 보장
- JPA `REMOVE` 이벤트 없이 DB가 직접 처리 → 1차 캐시 비정합 가능성이 있으나 Spring의 요청 단위 트랜잭션에서 허용 범위 내

**Alternatives considered**:
- `CascadeType.ALL` → JPA 메모리 레벨만 보장, DB 직접 삭제 시 고아 레코드 남음
- 수동 연쇄 삭제 코드 유지 → 누락 가능성, 코드 복잡도 증가

**주의사항**:
- `ddl-auto=update` 환경에서는 기존 FK를 CASCADE로 변경하지 않음 → **dev 환경에서 schema 재생성 필요** (clean build 또는 H2 in-memory test는 매번 재생성)
- `@OnDelete` 적용 후 `postRepository.delete(post)` 호출 시 DB CASCADE가 처리하므로 JPA 수동 삭제 코드 불필요

---

## Decision 2: viewCount 증가 타이밍 및 응답 정합성

**Decision**: `getPost()` 내에서 `findById` → `increaseViewCount` 순으로 호출. `PostRepository.increaseViewCount`에 `@Modifying(clearAutomatically = true)` 추가. 응답은 `PostDetailResponse.from(post, likedByMe)` 대신 viewCount만 +1 한 값을 직접 전달.

**Rationale**:
- 게시글 존재 확인(404 처리) 후 증가 → 존재하지 않는 게시글의 viewCount가 증가하는 상황 방지
- `clearAutomatically = true`로 JPQL 벌크 업데이트 후 1차 캐시 자동 clear
- 재조회 없이 `post.getViewCount() + 1` 계산으로 응답 구성 → DB 쿼리 1회 절약

**Alternatives considered**:
- `increaseViewCount` 후 `findById` 재조회 → DB 쿼리 3회 (findById → increment → findById)
- 조회 전 increment → 게시글 없어도 UPDATE 실행됨 (0 rows affected이므로 기능상 문제 없으나 의미론적으로 부적절)

**구현 방향**:
```java
// PostDetailResponse에 viewCount 파라미터를 명시적으로 받는 팩토리 메서드 추가 권장
public static PostDetailResponse from(Post post, long viewCount, boolean likedByMe)
```

---

## Decision 3: QueryDSL 검색 아키텍처

**Decision**: `PostRepository`(Spring Data JPA 인터페이스)와 `PostQueryRepository`(QueryDSL 전용 `@Repository` 클래스)를 완전히 분리. `PostService`에서 두 빈을 각각 주입받아 사용.

**구조**:
```
global/config/QueryDslConfig.java   — JPAQueryFactory @Bean
post/infra/PostRepository.java      — JpaRepository<Post, Long> (변경 없음)
post/infra/PostQueryRepository.java — @Repository 클래스, JPAQueryFactory 사용
post/web/PostSearchCondition.java   — 검색 파라미터 record (nickname, title, content, sort)
post/web/SortType.java              — enum (DATE, VIEW, LIKE)
```

**Rationale**:
- Spring Data의 `{Repository}Impl` 네이밍 규칙에 의존하지 않아 구조가 명확
- `PostQueryRepository`를 독립 클래스로 단위 테스트 가능 (`@ExtendWith(MockitoExtension)`)
- `PostService`는 두 의존성을 명시적으로 선언 → 역할 분리가 코드에 드러남
- 기존 `PostRepository` 변경 없음

**API 설계**:
- 기존 `GET /posts?page=0&size=10` 유지
- 검색 파라미터 추가: `GET /posts?nickname=홍길동&sort=LIKE&page=0&size=10`
- 검색 조건 없으면 기존 전체 목록과 동일하게 동작

**정렬 기준**:
- `DATE` (default): `id DESC` (auto-increment이므로 삽입 순서 보장, PK 인덱스 활용)
- `VIEW`: `viewCount DESC, id DESC`
- `LIKE`: `likeCount DESC, id DESC`

---

## Decision 4: 수동 삭제 코드 정리 범위

**Decision**: `@OnDelete` CASCADE 적용 후 `PostService.delete()`의 `commentService.deleteAllByPostId(postId)` 호출 제거. `CommentService.delete()`의 `commentLikeRepository.deleteAllByCommentId(commentId)` 호출 제거.

**Rationale**:
- DB CASCADE가 보장하므로 애플리케이션 코드의 수동 삭제는 중복
- 코드 단순화 및 N+1 삭제 쿼리 제거
- `likeCount` 갱신은 현재도 DB 레벨에서만 관리하므로 CASCADE 삭제 후 count 불일치는 기존과 동일 (todo.md에 명시된 추후 처리 사항)

**주의사항**:
- `postLikeRepository.delete(postLike)` in PostService.unlike() 등 단건 삭제는 유지
- UserService.delete()에서 User 삭제 시 연관 데이터 cascade 처리됨 → 별도 코드 불필요
