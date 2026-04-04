# Quickstart: 001-post-search-cascade 구현 가이드

## 전제 조건

- 브랜치 `004-comment-like`에서 분기 (또는 `main` merge 후 시작)
- `./gradlew clean build` 실행 (QueryDSL Q클래스 생성 확인)
- `build/generated/querydsl/` 에 `QPost`, `QComment` 등 Q클래스 생성 확인

## 구현 순서

### Step 1: ON DELETE CASCADE (P1-A)

엔티티 4개에 `@OnDelete` 추가:

```java
// 예시 (Comment.java)
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "post_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)
private Post post;
```

**대상**: `Post.user`, `PostLike.post`, `PostLike.user`, `Comment.post`, `Comment.user`, `CommentLike.comment`, `CommentLike.user`

수동 삭제 코드 정리:
- `PostService.delete()` — `commentService.deleteAllByPostId()` 제거 및 `CommentService` 의존 제거
- `CommentService.delete()` — `commentLikeRepository.deleteAllByCommentId()` 제거
- `CommentService.deleteAllByPostId()` 메서드 전체 제거
- `CommentLikeRepository.deleteAllByPostId` 벌크 쿼리 제거

테스트 반영:
- `PostServiceTest.delete_정상()` — `verify(commentService).deleteAllByPostId(1L)` 제거

### Step 2: viewCount 증가 (P1-B)

```java
// PostRepository.java — clearAutomatically 추가
@Modifying(clearAutomatically = true)
@Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
void increaseViewCount(@Param("id") Long postId);

// PostDetailResponse.java — 오버로드 메서드 추가
public static PostDetailResponse from(Post post, long viewCount, boolean likedByMe) {
    return new PostDetailResponse(
        post.getId(), post.getTitle(), post.getContent(),
        post.getUser().getNickname(), viewCount,
        post.getLikeCount(), likedByMe,
        post.getCreatedAt(), post.getUpdatedAt()
    );
}

// PostService.java — getPost 수정
@Transactional
public PostDetailResponse getPost(Long postId, Long userId) {
    Post post = postRepository.findById(postId)
            .orElseThrow(PostNotFoundException::new);
    postRepository.increaseViewCount(postId);
    boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(postId, userId);
    return PostDetailResponse.from(post, post.getViewCount() + 1, likedByMe);
}
```

### Step 3: QueryDSL 검색 (P1-C)

```java
// 1. global/config/QueryDslConfig.java
@Configuration
public class QueryDslConfig {
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}

// 2. post/web/SortType.java
public enum SortType { DATE, VIEW, LIKE }

// 3. post/web/PostSearchCondition.java
public record PostSearchCondition(
    String nickname,
    String title,
    String content,
    SortType sort
) {}

// 4. post/infra/PostQueryRepository.java 핵심 로직 (@Repository 독립 클래스)
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final JPAQueryFactory query;
    // search(), buildWhere(), buildOrder() 메서드
}

private BooleanBuilder buildWhere(PostSearchCondition cond) {
    BooleanBuilder builder = new BooleanBuilder();
    if (StringUtils.hasText(cond.nickname()))
        builder.and(QPost.post.user.nickname.containsIgnoreCase(cond.nickname()));
    if (StringUtils.hasText(cond.title()))
        builder.and(QPost.post.title.containsIgnoreCase(cond.title()));
    if (StringUtils.hasText(cond.content()))
        builder.and(QPost.post.content.containsIgnoreCase(cond.content()));
    return builder;
}

private OrderSpecifier<?>[] buildOrder(SortType sort) {
    return switch (sort) {
        case VIEW -> new OrderSpecifier[]{QPost.post.viewCount.desc(), QPost.post.id.desc()};
        case LIKE -> new OrderSpecifier[]{QPost.post.likeCount.desc(), QPost.post.id.desc()};
        default  -> new OrderSpecifier[]{QPost.post.id.desc()};
    };
}
```

## 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트만
./gradlew test --tests "*.PostServiceTest"
./gradlew test --tests "*.PostControllerTest"
```

## 주의사항

- `@OnDelete` 추가 후 H2 in-memory 테스트는 ddl-auto=create로 자동 반영
- MySQL 운영 환경은 스키마 재생성 또는 별도 ALTER TABLE 마이그레이션 필요
- `PostQueryRepositoryImpl` 클래스명은 `PostRepository` + `Impl` 규칙 준수 필수 (Spring Data 자동 주입)
- `@DataJpaTest`에서 QueryDSL 테스트 시 `QueryDslConfig` `@Import` 필요
