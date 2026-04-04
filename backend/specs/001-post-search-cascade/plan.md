# Implementation Plan: Post 검색 및 ON DELETE CASCADE 제약조건

**Branch**: `001-post-search-cascade` | **Date**: 2026-04-02 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-post-search-cascade/spec.md`

## Summary

**주요 요구사항 3가지:**
1. DB FK `ON DELETE CASCADE` 적용 (User→Post/Comment/PostLike/CommentLike, Post→PostLike/Comment, Comment→CommentLike)
2. 게시글 상세 조회 시 `viewCount` 자동 증가 (이미 구현된 `increaseViewCount` 쿼리 활용)
3. 게시글 목록 API에 닉네임/제목/내용 검색 + 날짜/조회수/좋아요 정렬 추가 (QueryDSL 동적 쿼리)

**기술 방향:**
- `@OnDelete(action = OnDeleteAction.CASCADE)` — 엔티티 FK 어노테이션으로 DDL CASCADE 생성
- `PostQueryRepositoryImpl` — QueryDSL `JPAQueryFactory` 활용 동적 검색 쿼리
- `PostService.getPost()` — `@Transactional` 쓰기 모드로 변경 + `increaseViewCount` 호출

---

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.5, Spring Data JPA, Hibernate 6, QueryDSL 5.0.0 (jakarta), Lombok  
**Storage**: MySQL (운영), H2 in-memory (테스트)  
**Testing**: JUnit 5, Mockito, MockMvc, Spring REST Docs  
**Target Platform**: Linux server (Spring Boot embedded Tomcat)  
**Project Type**: REST API (web-service)  
**Performance Goals**: 검색 응답 1초 이내 (10,000건 기준)  
**Constraints**: DB ddl-auto=create 기준 (운영에서는 별도 마이그레이션 필요)  
**Scale/Scope**: 소규모 개인 일기 서비스

---

## Constitution Check

constitution.md가 템플릿 상태(미작성)이므로 별도 gate 없음. 프로젝트 기존 컨벤션(.claude/rules/)을 기준으로 검증:

| 규칙 | 준수 여부 | 비고 |
|------|----------|------|
| Entity: @Getter + @NoArgsConstructor(PROTECTED) + of() + change~() | ✅ | 신규 클래스 모두 적용 |
| Service: @Transactional(readOnly=true) 클래스 레벨 | ✅ | getPost만 개별 @Transactional 추가 |
| DTO: record 사용 | ✅ | PostSearchCondition, SortType(enum) |
| Controller: ResponseEntity<T> 반환 | ✅ | |
| 예외: BusinessException 상속 | N/A | 신규 예외 없음 |

---

## Project Structure

### Documentation (this feature)

```text
specs/001-post-search-cascade/
├── plan.md              ← 이 파일
├── spec.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── post-api.md
└── tasks.md             (추후 /speckit.tasks 생성)
```

### Source Code 변경 대상

```text
backend/src/main/java/com/daily_diary/backend/
├── global/
│   └── config/
│       └── QueryDslConfig.java              [신규] JPAQueryFactory @Bean
├── post/
│   ├── entity/
│   │   └── Post.java                        [수정] user FK에 @OnDelete 추가
│   ├── infra/
│   │   ├── PostRepository.java              [수정] increaseViewCount에 clearAutomatically=true 추가
│   │   └── PostQueryRepository.java         [신규] @Repository 클래스, JPAQueryFactory 사용
│   ├── service/
│   │   └── PostService.java                 [수정] getPost viewCount 증가, delete 단순화, list→search 확장
│   └── web/
│       ├── PostController.java              [수정] list 파라미터 확장
│       ├── PostDetailResponse.java          [수정] viewCount 오버로드 팩토리 메서드 추가
│       ├── PostSearchCondition.java         [신규] 검색 파라미터 record
│       └── SortType.java                    [신규] 정렬 기준 enum
├── comment/
│   ├── entity/
│   │   ├── Comment.java                     [수정] post/user FK에 @OnDelete 추가
│   │   └── CommentLike.java                 [수정] comment/user FK에 @OnDelete 추가
│   └── service/
│       └── CommentService.java              [수정] delete() 단순화, deleteAllByPostId() 제거
├── post/
│   └── entity/
│       └── PostLike.java                    [수정] post/user FK에 @OnDelete 추가
└── infra/
    └── CommentLikeRepository.java           [수정] deleteAllByPostId 벌크 쿼리 제거

backend/src/test/java/com/daily_diary/backend/
├── post/
│   ├── service/PostServiceTest.java         [수정] getPost viewCount 검증 추가, delete 단순화
│   └── web/PostControllerTest.java          [수정] 검색 파라미터 테스트 추가
└── comment/
    └── service/CommentServiceTest.java      [수정] delete 단순화 반영
```

**Structure Decision**: 기존 도메인-우선 패키지 구조 유지. `global/config/`에 QueryDSL 설정 추가.

---

## Phase 0: Research 완료

→ `research.md` 참조

**해결된 결정사항:**
- ✅ `@OnDelete(action = OnDeleteAction.CASCADE)` 사용 (DDL 레벨 CASCADE)
- ✅ viewCount: `findById` → `increaseViewCount` 순서, `PostDetailResponse.from()` 오버로드
- ✅ QueryDSL: `PostQueryRepositoryImpl` + `JPAQueryFactory` 빈
- ✅ 검색 API: 기존 `GET /posts` 파라미터 확장 (하위 호환)
- ✅ 수동 삭제 코드: `@OnDelete` 적용 후 제거

---

## Phase 1: Design 완료

→ `data-model.md`, `contracts/post-api.md` 참조

### 구현 작업 목록 (우선순위 순)

#### P1-A: ON DELETE CASCADE 적용 (DB 무결성)

1. **`Post.java`** — `user` FK에 `@OnDelete(action = OnDeleteAction.CASCADE)` 추가
2. **`PostLike.java`** — `post`, `user` FK에 각각 `@OnDelete` 추가
3. **`Comment.java`** — `post`, `user` FK에 각각 `@OnDelete` 추가
4. **`CommentLike.java`** — `comment`, `user` FK에 각각 `@OnDelete` 추가
5. **`PostService.java`** — `delete()`에서 `commentService.deleteAllByPostId()` 제거, `CommentService` 의존성 제거
6. **`CommentService.java`** — `delete()`에서 `commentLikeRepository.deleteAllByCommentId()` 제거, `deleteAllByPostId()` 메서드 제거
7. **`CommentLikeRepository.java`** — `deleteAllByPostId` 벌크 쿼리 제거
8. **`PostServiceTest.java`** — delete 테스트에서 commentService mock 및 verify 제거

#### P1-B: viewCount 증가 구현

9. **`PostRepository.java`** — `increaseViewCount`에 `@Modifying(clearAutomatically = true)` 추가
10. **`PostDetailResponse.java`** — `from(Post, long viewCount, boolean likedByMe)` 오버로드 팩토리 메서드 추가
11. **`PostService.java`** — `getPost()` 메서드에 `@Transactional` 추가 및 `increaseViewCount` 호출
12. **`PostServiceTest.java`** — getPost 테스트에 viewCount 검증 추가
13. **`PostControllerTest.java`** — getPost 응답의 viewCount 필드 확인

#### P1-C: QueryDSL 검색 구현

14. **`QueryDslConfig.java`** 신규 — `JPAQueryFactory` `@Bean` (global/config/)
15. **`SortType.java`** 신규 — `DATE`, `VIEW`, `LIKE` enum (post/web/)
16. **`PostSearchCondition.java`** 신규 — 검색 파라미터 record (post/web/)
17. **`PostQueryRepository.java`** 신규 — `@Repository` 클래스, `JPAQueryFactory` 주입, `search()` 구현 (post/infra/)
18. **`PostService.java`** — `PostQueryRepository` 의존성 추가, `list()` → `search()` 로직 확장
21. **`PostController.java`** — `list()` 파라미터에 `PostSearchCondition` 바인딩 추가
22. **`PostControllerTest.java`** — 검색 파라미터 테스트 및 REST Docs 업데이트
23. **`PostServiceTest.java`** — search 메서드 테스트 추가
24. **`index.adoc`** — 검색 API 문서 업데이트 (sort, nickname, title, content 파라미터)

---

## Complexity Tracking

해당 없음 — 기존 아키텍처 패턴 내에서 구현.
