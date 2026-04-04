# Data Model: Post 검색 및 ON DELETE CASCADE

## Entity 변경 사항

### 1. Post (변경 없음 — FK 소유자 아님)
Post는 User를 참조하므로 `@OnDelete` 추가 대상.

```java
// Post.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)   // 추가: User 삭제 시 Post 자동 삭제
private User user;
```

### 2. PostLike (FK 수정)

```java
// PostLike.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "post_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)   // 추가: Post 삭제 시 PostLike 자동 삭제
private Post post;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)   // 추가: User 삭제 시 PostLike 자동 삭제
private User user;
```

### 3. Comment (FK 수정)

```java
// Comment.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "post_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)   // 추가: Post 삭제 시 Comment 자동 삭제
private Post post;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)   // 추가: User 삭제 시 Comment 자동 삭제
private User user;
```

### 4. CommentLike (FK 수정)

```java
// CommentLike.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "comment_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)   // 추가: Comment 삭제 시 CommentLike 자동 삭제
private Comment comment;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)   // 추가: User 삭제 시 CommentLike 자동 삭제
private User user;
```

---

## CASCADE 관계 다이어그램

```
User
 ├──(CASCADE)──▶ Post
 │                ├──(CASCADE)──▶ PostLike
 │                └──(CASCADE)──▶ Comment
 │                                  └──(CASCADE)──▶ CommentLike
 ├──(CASCADE)──▶ PostLike
 ├──(CASCADE)──▶ Comment
 └──(CASCADE)──▶ CommentLike
```

**결과**:
- User 삭제 → Post, PostLike(user), Comment(user), CommentLike(user) 자동 삭제
- Post 삭제 → PostLike(post), Comment(post) 자동 삭제 → Comment 삭제로 인해 CommentLike도 연쇄 삭제
- Comment 삭제 → CommentLike(comment) 자동 삭제

---

## 신규 타입/클래스

### PostSearchCondition (record, `post/web/`)

```java
public record PostSearchCondition(
    String nickname,   // nullable — 닉네임 부분 일치 검색
    String title,      // nullable — 제목 부분 일치 검색
    String content,    // nullable — 내용 부분 일치 검색
    SortType sort      // not null — 정렬 기준 (기본값: DATE)
) {}
```

### SortType (enum, `post/web/`)

```java
public enum SortType {
    DATE,  // id DESC (기본값 — auto-increment이므로 삽입 순서 보장)
    VIEW,  // viewCount DESC, id DESC
    LIKE   // likeCount DESC, id DESC
}
```

---

## Repository 변경 사항

### PostRepository (변경 없음, `post/infra/`)

```java
// 기존 그대로 유지 — PostQueryRepository 상속 추가 없음
public interface PostRepository extends JpaRepository<Post, Long> {
    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseViewCount(@Param("id") Long postId);
    // 기타 기존 쿼리 메서드 유지
}
```

### PostQueryRepository (신규, `post/infra/`) — 독립 @Repository 클래스

```java
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private final JPAQueryFactory query;

    public Page<PostSummaryResponse> search(PostSearchCondition condition, Pageable pageable) {
        // QPost, QUser로 동적 쿼리 구성
        // where: nickname contains / title contains / content contains (각 null이면 skip)
        // orderBy: SortType에 따라 분기
        // fetchCount + fetchContent 분리 → Page 반환
    }

    private BooleanBuilder buildWhere(PostSearchCondition condition) { ... }

    private OrderSpecifier<?>[] buildOrder(SortType sort) { ... }
}
```

---

## Service 변경 사항

### PostService.getPost() — viewCount 증가 추가

```java
// 변경 전
@Transactional(readOnly = true) — 클래스 레벨 적용됨
public PostDetailResponse getPost(Long postId, Long userId) {
    Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
    boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(postId, userId);
    return PostDetailResponse.from(post, likedByMe);
}

// 변경 후
@Transactional  // write transaction 필요 (readOnly=false)
public PostDetailResponse getPost(Long postId, Long userId) {
    Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
    postRepository.increaseViewCount(postId);
    boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(postId, userId);
    return PostDetailResponse.from(post, post.getViewCount() + 1, likedByMe);
}
```

### PostService.delete() — 수동 삭제 코드 제거

```java
// 변경 전
@Transactional
public void delete(Long userId, Long postId) {
    Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
    validatePostOwner(post, userId);
    commentService.deleteAllByPostId(postId);   // 제거 예정
    postRepository.delete(post);
}

// 변경 후
@Transactional
public void delete(Long userId, Long postId) {
    Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
    validatePostOwner(post, userId);
    // DB CASCADE가 PostLike, Comment, CommentLike 자동 삭제
    postRepository.delete(post);
}
```

### PostService — 의존성 변경

```java
// 변경 전
private final PostRepository postRepository;

// 변경 후: PostQueryRepository 추가 주입
private final PostRepository postRepository;
private final PostQueryRepository postQueryRepository;  // 신규 추가
```

### PostService.list() → search()로 확장

```java
// 변경 전
public PostListResponse list(int page, int size) { ... }

// 변경 후
public PostListResponse search(PostSearchCondition condition, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return PostListResponse.from(postQueryRepository.search(condition, pageable));
}
```

### PostDetailResponse — viewCount 파라미터 추가 팩토리 메서드

```java
// 기존 from(Post post, boolean likedByMe) 유지 + viewCount 오버로드 추가
public static PostDetailResponse from(Post post, long viewCount, boolean likedByMe) {
    return new PostDetailResponse(
        post.getId(), post.getTitle(), post.getContent(),
        post.getUser().getNickname(), viewCount,
        post.getLikeCount(), likedByMe,
        post.getCreatedAt(), post.getUpdatedAt()
    );
}
```

---

## 삭제되는 코드

| 파일 | 삭제 대상 | 이유 |
|------|----------|------|
| PostService.java | `commentService.deleteAllByPostId(postId)` | DB CASCADE로 대체 |
| PostService.java | `CommentService commentService` 필드 의존 | 불필요 |
| CommentService.java | `deleteAllByPostId(Long postId)` 메서드 | DB CASCADE로 대체 |
| CommentService.java | `commentLikeRepository.deleteAllByCommentId(commentId)` in delete() | DB CASCADE로 대체 |
| CommentLikeRepository.java | `deleteAllByPostId` 벌크 쿼리 | 불필요 |
| PostServiceTest.java | `verify(commentService).deleteAllByPostId(1L)` | 해당 로직 제거로 불필요 |
