# Tasks: Post 검색 및 ON DELETE CASCADE 제약조건 구현

**Input**: Design documents from `specs/001-post-search-cascade/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Organization**: 3개 User Story 기준으로 구성. US1·US2는 P1 (독립 실행 가능), US3는 P2 (Setup 완료 후 진행).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일 대상 → 병렬 실행 가능
- **[US1]**: viewCount 자동 증가
- **[US2]**: ON DELETE CASCADE
- **[US3]**: 게시글 검색 및 정렬

---

## Phase 1: Setup (공유 인프라)

**Purpose**: US3에서 필요한 QueryDSL 설정 빈 초기화. US1·US2는 이 Phase 완료 없이 바로 시작 가능.

- [ ] T001 `src/main/java/com/daily_diary/backend/global/config/QueryDslConfig.java` 신규 생성 — `@Configuration` + `JPAQueryFactory` `@Bean` (EntityManager 주입)

---

## Phase 2: Foundational (공통 선행 작업)

**Purpose**: US1·US2·US3 모두에서 `PostDetailResponse` 응답 구조 변경이 이미 004 브랜치에서 완료됨. 추가 공통 선행 작업 없음.

**⚠️ CHECKPOINT**: T001 완료 후 US1·US2는 즉시 병렬 진행 가능. US3는 T001 완료 필요.

---

## Phase 3: User Story 2 — 데이터 삭제 시 연관 데이터 자동 정리 (Priority: P1) 🎯

**Goal**: DB FK `ON DELETE CASCADE`로 User/Post/Comment 삭제 시 연관 데이터가 자동 삭제됨. 애플리케이션 수동 삭제 코드 제거.

**Independent Test**: H2 in-memory DB 테스트에서 `postRepository.delete(post)` 호출 후 `PostLike`, `Comment`, `CommentLike` 레코드가 0건임을 확인.

### Implementation

- [ ] T002 [P] [US2] `src/main/java/com/daily_diary/backend/post/entity/Post.java` — `user` 필드의 `@JoinColumn` 아래에 `@OnDelete(action = OnDeleteAction.CASCADE)` 추가
- [ ] T003 [P] [US2] `src/main/java/com/daily_diary/backend/post/entity/PostLike.java` — `post`, `user` 필드 각각에 `@OnDelete(action = OnDeleteAction.CASCADE)` 추가
- [ ] T004 [P] [US2] `src/main/java/com/daily_diary/backend/comment/entity/Comment.java` — `post`, `user` 필드 각각에 `@OnDelete(action = OnDeleteAction.CASCADE)` 추가
- [ ] T005 [P] [US2] `src/main/java/com/daily_diary/backend/comment/entity/CommentLike.java` — `comment`, `user` 필드 각각에 `@OnDelete(action = OnDeleteAction.CASCADE)` 추가
- [ ] T006 [US2] `src/main/java/com/daily_diary/backend/comment/service/CommentService.java` — `delete()` 메서드에서 `commentLikeRepository.deleteAllByCommentId(commentId)` 호출 제거 (DB CASCADE가 처리)
- [ ] T007 [US2] `src/main/java/com/daily_diary/backend/comment/service/CommentService.java` — `deleteAllByPostId(Long postId)` 메서드 전체 제거
- [ ] T008 [US2] `src/main/java/com/daily_diary/backend/comment/infra/CommentLikeRepository.java` — `deleteAllByPostId` `@Modifying @Query` 메서드 제거
- [ ] T009 [US2] `src/main/java/com/daily_diary/backend/post/service/PostService.java` — `delete()` 에서 `commentService.deleteAllByPostId(postId)` 호출 제거 및 `CommentService` 필드 의존성 제거, import 정리
- [ ] T010 [US2] `src/test/java/com/daily_diary/backend/post/service/PostServiceTest.java` — `delete_정상()` 테스트에서 `@Mock CommentService` 및 `verify(commentService).deleteAllByPostId(1L)` 제거, 테스트 통과 확인
- [ ] T011 [US2] `src/test/java/com/daily_diary/backend/comment/service/CommentServiceTest.java` — `delete_정상()` 테스트에서 `verify(commentLikeRepository).deleteAllByCommentId(1L)` 제거 후 테스트 통과 확인

**Checkpoint**: `./gradlew test --tests "*.PostServiceTest" --tests "*.CommentServiceTest"` 통과

---

## Phase 4: User Story 1 — 게시글 조회 시 조회수 자동 증가 (Priority: P1) 🎯

**Goal**: `GET /posts/{id}` 호출 시 `viewCount`가 1 증가하여 응답에 반영됨.

**Independent Test**: 동일 게시글을 3회 조회했을 때 `viewCount`가 0→1→2→3으로 증가하며 각 응답에 포함됨.

### Implementation

- [ ] T012 [US1] `src/main/java/com/daily_diary/backend/post/infra/PostRepository.java` — `increaseViewCount` 어노테이션을 `@Modifying(clearAutomatically = true)`로 변경
- [ ] T013 [US1] `src/main/java/com/daily_diary/backend/post/web/PostDetailResponse.java` — `from(Post post, long viewCount, boolean likedByMe)` 오버로드 팩토리 메서드 추가 (기존 `from(Post, boolean)` 유지)
- [ ] T014 [US1] `src/main/java/com/daily_diary/backend/post/service/PostService.java` — `getPost()` 메서드에 `@Transactional` 추가, `increaseViewCount(postId)` 호출, 응답을 `PostDetailResponse.from(post, post.getViewCount() + 1, likedByMe)`로 변경
- [ ] T015 [US1] `src/test/java/com/daily_diary/backend/post/service/PostServiceTest.java` — `getPost_정상()` 테스트에 `verify(postRepository).increaseViewCount(1L)` 및 `assertThat(response.viewCount()).isEqualTo(1L)` 검증 추가
- [ ] T016 [US1] `src/test/java/com/daily_diary/backend/post/web/PostControllerTest.java` — `getPost()` 테스트에서 `viewCount` 필드 값 `jsonPath("$.viewCount").value(10L)` 검증 확인

**Checkpoint**: `./gradlew test --tests "*.PostServiceTest" --tests "*.PostControllerTest"` 통과

---

## Phase 5: User Story 3 — 게시글 검색 및 정렬 (Priority: P2)

**Goal**: `GET /posts?nickname=홍길동&sort=LIKE` 형태로 닉네임/제목/내용 검색과 최신순/조회수순/좋아요순 정렬이 가능.

**Independent Test**: `nickname=홍길동`으로 검색 시 해당 닉네임 작성자의 게시글만 반환, `sort=LIKE` 시 likeCount 내림차순 정렬 확인.

**Prerequisites**: T001 (QueryDslConfig) 완료 필요

### Implementation

- [ ] T017 [P] [US3] `src/main/java/com/daily_diary/backend/post/web/SortType.java` 신규 생성 — `DATE`, `VIEW`, `LIKE` enum
- [ ] T018 [P] [US3] `src/main/java/com/daily_diary/backend/post/web/PostSearchCondition.java` 신규 생성 — `String nickname`, `String title`, `String content`, `SortType sort` 필드를 가진 record (sort 기본값 DATE)
- [ ] T019 [US3] `src/main/java/com/daily_diary/backend/post/infra/PostQueryRepository.java` 신규 생성 — `@Repository @RequiredArgsConstructor` 클래스, `JPAQueryFactory` 주입, `Page<PostSummaryResponse> search(PostSearchCondition condition, Pageable pageable)` 구현. `BooleanBuilder`로 null 조건 skip, `SortType`에 따라 `id DESC` 2차 정렬 적용 (`DATE: id DESC`, `VIEW: viewCount DESC, id DESC`, `LIKE: likeCount DESC, id DESC`)
- [ ] T020 [US3] `src/main/java/com/daily_diary/backend/post/service/PostService.java` — `PostQueryRepository postQueryRepository` 필드 추가, 기존 `list()` 메서드를 `search(PostSearchCondition condition, int page, int size)`로 교체 (Pageable은 서비스 내부에서 생성, sort는 condition.sort() 기반)
- [ ] T021 [US3] `src/main/java/com/daily_diary/backend/post/web/PostController.java` — `list()` 메서드에 `@RequestParam(required=false) String nickname`, `@RequestParam(required=false) String title`, `@RequestParam(required=false) String content`, `@RequestParam(defaultValue="DATE") SortType sort` 파라미터 추가 후 `PostSearchCondition` 생성하여 `postService.search()` 호출
- [ ] T022 [US3] `src/test/java/com/daily_diary/backend/post/service/PostServiceTest.java` — `search_닉네임_필터()`, `search_정렬_VIEW()` 테스트 추가 (`postQueryRepository` mock 사용)
- [ ] T023 [US3] `src/test/java/com/daily_diary/backend/post/web/PostControllerTest.java` — `list()` 테스트에 `nickname`, `sort` 파라미터 추가, REST Docs `queryParameters`에 `nickname`, `title`, `content`, `sort` 파라미터 문서화

**Checkpoint**: `./gradlew test --tests "*.PostServiceTest" --tests "*.PostControllerTest"` 통과

---

## Phase 6: Polish & Cross-Cutting

- [ ] T024 [P] `src/docs/asciidoc/index.adoc` — `GET /posts` 섹션에 `nickname`, `title`, `content`, `sort` 쿼리 파라미터 include 추가
- [ ] T025 `./gradlew test` 전체 테스트 통과 확인
- [ ] T026 `./gradlew asciidoctor` 실행 후 `build/docs/asciidoc/index.html` 에서 검색 파라미터 문서 확인

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 즉시 시작 가능
- **US2 (Phase 3)**: Setup 완료 불필요 — 즉시 시작 가능 (엔티티/서비스 수정만)
- **US1 (Phase 4)**: Setup 완료 불필요 — US2와 병렬 진행 가능
- **US3 (Phase 5)**: T001 (QueryDslConfig) 완료 필요
- **Polish (Phase 6)**: US3 완료 후

### User Story Dependencies

- **US2 (P1)**: 즉시 시작 가능. 다른 Story에 무의존.
- **US1 (P1)**: 즉시 시작 가능. US2와 독립적으로 병렬 진행 가능.
- **US3 (P2)**: T001 완료 후 시작. US1·US2 완료 불필요.

### Within Each User Story

- T002~T005 (엔티티 수정): 서로 다른 파일 → 완전 병렬
- T006~T009 (서비스 수정): 순차 또는 병렬 (다른 파일)
- T010~T011 (테스트): 다른 파일 → 병렬
- T017~T018 (신규 타입): 서로 독립 → 병렬
- T019 (PostQueryRepository): T017, T018 완료 후

---

## Parallel Execution Examples

### US2 병렬 실행 (T002~T005)

```text
동시 실행 가능:
- T002: Post.java @OnDelete 추가
- T003: PostLike.java @OnDelete 추가
- T004: Comment.java @OnDelete 추가
- T005: CommentLike.java @OnDelete 추가
```

### US3 병렬 실행 (T017~T018)

```text
동시 실행 가능:
- T017: SortType.java 신규 생성
- T018: PostSearchCondition.java 신규 생성
이후 순차:
- T019: PostQueryRepository (T017, T018 의존)
```

---

## Implementation Strategy

### MVP First (US1 + US2 — P1만 완료)

1. T001 Setup
2. T002~T011 US2 (CASCADE) — 엔티티 4개 병렬 수정 → 서비스 정리
3. T012~T016 US1 (viewCount) — US2와 병렬 가능
4. **STOP**: `./gradlew test` 통과 확인 → PR 가능 상태

### Full Delivery

5. T017~T023 US3 (검색) 추가
6. T024~T026 Polish
7. PR 생성

### 혼자 개발 시 권장 순서

```
T001 → T002~T005 병렬 → T006 → T007 → T008 → T009 → T010~T011 병렬
     → T012 → T013 → T014 → T015~T016 병렬
     → T017~T018 병렬 → T019 → T020 → T021 → T022~T023 병렬
     → T024~T026
```

---

## Notes

- [P] tasks = 다른 파일 대상, 의존성 없음 → 병렬 실행 가능
- `@OnDelete` 추가 후 H2 테스트는 ddl-auto=create로 자동 반영됨
- `PostQueryRepositoryImpl` 규칙 불필요 — `PostQueryRepository` 자체가 `@Repository` 클래스
- 각 Phase Checkpoint에서 테스트 통과 확인 후 다음 Phase 진행 권장
