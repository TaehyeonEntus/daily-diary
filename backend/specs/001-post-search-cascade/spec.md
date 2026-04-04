# Feature Specification: Post 검색 및 ON DELETE CASCADE 제약조건 구현

**Feature Branch**: `001-post-search-cascade`
**Created**: 2026-04-02
**Status**: Draft
**Input**: User description: "CommentLike -> Comment로 OnDeleteCascade 제약조건을 걸어야할 것 같아, PostLike -> Post도 마찬가지야. 그리고 Post->User, Comment->Post, Comment -> User도 걸어야할 것 같아. Comment가 Post가 없어도 의미 있나? 이게 처리의 주요 요구사항이 될 것 같은데 현재는 따로 참조되는 기록이 없어서, On Delete Cascade로 지워도 될 것 같음. + 닉네임 기준 검색, 제목 기준 검색, 내용 기준 검색을 따로 만들어야할 것 같아. Post기준의 검색이야, 이거는 querydsl로 동적쿼리 짜면 될 것같아. 정렬기준은 날짜 순, 조회수 순, 좋아요 순 정도 있으면 괜찮을 것 같아. 그리고 getPost에 viewCount를 증가시키는 쿼리를 호출하기 전에 한번 날려야할 것 같은데, 이거 PostRepository에 구현해놓은거"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 게시글 조회 시 조회수 자동 증가 (Priority: P1)

사용자가 게시글 상세를 조회할 때 해당 게시글의 조회수가 자동으로 1 증가하며, 응답에 최신 조회수가 포함된다.

**Why this priority**: 조회수는 이미 응답 DTO에 포함되어 있으나 항상 0을 반환하는 미완성 상태. 가장 단순하고 독립적으로 구현 가능하며 기존 기능의 완성도를 즉시 높임.

**Independent Test**: 동일 게시글을 3회 조회했을 때 `viewCount`가 0 → 1 → 2 → 3으로 증가하는지 확인.

**Acceptance Scenarios**:

1. **Given** 게시글이 존재하고 `viewCount = 0`인 상태에서, **When** 게시글 상세 조회 API를 호출하면, **Then** 응답의 `viewCount`가 1이다.
2. **Given** 동일 게시글을 N회 조회한 상태에서, **When** 다시 조회하면, **Then** `viewCount`가 N+1이다.
3. **Given** 존재하지 않는 게시글 ID로 조회하면, **When** 상세 조회 API 호출 시, **Then** 404 응답이 반환되고 조회수는 증가하지 않는다.

---

### User Story 2 - 데이터 삭제 시 연관 데이터 자동 정리 (Priority: P1)

회원이 탈퇴하거나 게시글·댓글을 삭제하면 연관된 모든 하위 데이터가 DB 제약조건에 의해 자동으로 삭제된다. 어떤 경로로 삭제하더라도 고아 데이터가 남지 않는다.

**Why this priority**: 현재 애플리케이션 코드가 수동으로 연쇄 삭제를 처리하지만 누락 가능성이 있음. DB 레벨 CASCADE 보장이 데이터 무결성의 기본.

**Independent Test**: DB에서 직접 User 레코드를 삭제했을 때 해당 User의 Post, Comment, PostLike, CommentLike가 모두 삭제되는지 확인.

**Acceptance Scenarios**:

1. **Given** 회원(User)이 게시글, 댓글, 좋아요를 보유한 상태에서, **When** 해당 회원이 탈퇴하면, **Then** 회원의 모든 게시글·댓글·좋아요 데이터가 자동 삭제된다.
2. **Given** 게시글(Post)에 댓글과 좋아요가 등록된 상태에서, **When** 게시글이 삭제되면, **Then** 해당 게시글의 댓글·댓글좋아요·게시글좋아요가 모두 자동 삭제된다.
3. **Given** 댓글(Comment)에 좋아요가 등록된 상태에서, **When** 댓글이 삭제되면, **Then** 해당 댓글의 좋아요가 자동 삭제된다.
4. **Given** DB 레벨에서 직접 Post를 삭제해도, **When** 관련 PostLike, Comment, CommentLike 레코드를 조회하면, **Then** 모두 0건이다.

---

### User Story 3 - 게시글 검색 및 정렬 (Priority: P2)

사용자가 작성자 닉네임, 게시글 제목, 또는 내용 키워드로 게시글을 검색하고, 결과를 원하는 기준(최신순·조회수순·좋아요순)으로 정렬할 수 있다.

**Why this priority**: 게시글 수가 늘어날수록 검색이 필수적이나, 조회수·무결성 보장이 선행되어야 함.

**Independent Test**: 특정 닉네임으로 검색 시 해당 닉네임 작성자의 게시글만 반환되는지 확인.

**Acceptance Scenarios**:

1. **Given** 다양한 작성자의 게시글이 존재할 때, **When** 닉네임 키워드로 검색하면, **Then** 해당 키워드가 포함된 닉네임을 가진 작성자의 게시글만 반환된다.
2. **Given** 다양한 제목의 게시글이 존재할 때, **When** 제목 키워드로 검색하면, **Then** 제목에 해당 키워드가 포함된 게시글만 반환된다.
3. **Given** 다양한 내용의 게시글이 존재할 때, **When** 내용 키워드로 검색하면, **Then** 내용에 해당 키워드가 포함된 게시글만 반환된다.
4. **Given** 검색 결과가 여러 건일 때, **When** 정렬 기준을 `좋아요순`으로 지정하면, **Then** 좋아요 수 내림차순으로 반환된다.
5. **Given** 검색 결과가 여러 건일 때, **When** 정렬 기준을 `조회수순`으로 지정하면, **Then** 조회수 내림차순으로 반환된다.
6. **Given** 정렬 기준을 `최신순`으로 지정하면, **When** 검색 API를 호출하면, **Then** 작성일 내림차순으로 반환된다.
7. **Given** 검색 조건에 일치하는 게시글이 없을 때, **When** 검색하면, **Then** 빈 목록과 `totalElements: 0`이 반환된다.
8. **Given** 검색 조건 없이 호출하면, **When** 검색 API를 호출하면, **Then** 전체 게시글이 기본 정렬(최신순)로 반환된다.

---

### Edge Cases

- 검색 키워드가 빈 문자열이면 전체 게시글을 반환한다 (검색 조건 없음과 동일).
- 검색 키워드에 SQL 특수문자(`%`, `_`)가 포함될 경우 안전하게 처리되어 의도치 않은 결과가 반환되지 않는다.
- 동시에 여러 사용자가 동일 게시글을 조회할 때 `viewCount` 카운트가 손실 없이 증가한다.
- 정렬 기준 값이 유효하지 않으면 기본값(최신순)으로 처리한다.
- 동일 정렬 기준값(동점)이 존재할 때 2차 정렬로 `id DESC`를 적용한다.

---

## Requirements *(mandatory)*

### Functional Requirements

**[그룹 A: 게시글 조회수 증가]**

- **FR-001**: 게시글 상세 조회 시 해당 게시글의 `viewCount`가 1 증가해야 한다.
- **FR-002**: 게시글이 존재하지 않으면 조회수 증가 없이 404 오류를 반환해야 한다.
- **FR-003**: 조회수 증가는 게시글 조회 응답과 동일 요청 내에서 처리되어야 한다.

**[그룹 B: ON DELETE CASCADE 제약조건]**

- **FR-004**: User 삭제 시 해당 User의 Post, Comment, PostLike, CommentLike가 모두 자동 삭제되어야 한다.
- **FR-005**: Post 삭제 시 해당 Post의 PostLike, Comment, CommentLike가 모두 자동 삭제되어야 한다.
- **FR-006**: Comment 삭제 시 해당 Comment의 CommentLike가 자동 삭제되어야 한다.
- **FR-007**: 위 CASCADE는 DB 외래 키 제약조건(`ON DELETE CASCADE`)으로 보장되어야 한다.
- **FR-008**: CASCADE 적용 후 애플리케이션의 수동 연쇄 삭제 코드는 단순화 또는 제거되어야 한다.

**[그룹 C: 게시글 검색 및 정렬]**

- **FR-009**: 닉네임, 제목, 내용 각각에 대한 검색 조건을 단독으로 사용할 수 있어야 한다.
- **FR-010**: 검색은 키워드 부분 일치(contains)로 동작해야 한다.
- **FR-011**: 검색 조건이 없으면 전체 게시글을 반환해야 한다.
- **FR-012**: 검색 결과는 최신순, 조회수순, 좋아요순 중 하나로 정렬할 수 있어야 한다.
- **FR-013**: 정렬 기준이 명시되지 않으면 최신순을 기본값으로 사용해야 한다.
- **FR-014**: 검색 결과는 페이지네이션을 지원해야 한다.
- **FR-015**: 동점인 경우 `id DESC`를 2차 정렬 기준으로 사용해야 한다.

### Key Entities

- **User**: 회원. Post, Comment, PostLike, CommentLike를 소유. 삭제 시 모든 연관 데이터 CASCADE 삭제.
- **Post**: 게시글. User에 종속. `viewCount`(조회수), `likeCount`(좋아요 수) 보유. 삭제 시 PostLike, Comment, CommentLike CASCADE 삭제.
- **Comment**: 댓글. Post와 User 모두에 종속. Post가 없으면 독립적 의미 없음. 삭제 시 CommentLike CASCADE 삭제.
- **PostLike**: 게시글 좋아요. Post와 User에 종속. Post 또는 User 삭제 시 CASCADE 삭제.
- **CommentLike**: 댓글 좋아요. Comment와 User에 종속. Comment 또는 User 삭제 시 CASCADE 삭제.
- **PostSearchQuery**: 검색 조건 파라미터 (닉네임 키워드, 제목 키워드, 내용 키워드, 정렬기준, 페이지 정보). 영속성 없는 요청 객체.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 게시글을 N회 조회한 후 상세 조회 응답의 `viewCount` 값이 정확히 N이다.
- **SC-002**: 회원 탈퇴 후 해당 회원 ID로 관련 데이터를 조회하면 게시글·댓글·좋아요가 모두 0건이다.
- **SC-003**: 게시글 검색 시 조건에 부합하지 않는 게시글은 결과에 포함되지 않는다 (정확도 100%).
- **SC-004**: 10,000건 게시글 기준 검색 응답 시간이 1초 이내다.
- **SC-005**: 반환된 검색 결과 목록이 지정된 정렬 기준과 일치한다.
- **SC-006**: DB에서 직접 부모 레코드 삭제 후 연관 자식 레코드가 0건 남는다.

---

## Assumptions

- 닉네임/제목/내용 검색은 각각 독립적인 단일 조건 검색이며, 복합 AND/OR 조건 검색은 범위 외다.
- viewCount 증가는 중복 방지 없이 조회 API 호출 횟수를 그대로 카운트한다 (로봇·중복 방지는 추후 기능).
- ON DELETE CASCADE는 JPA CascadeType이 아닌 DDL 수준의 FK `ON DELETE CASCADE`로 구현한다.
- CASCADE 적용으로 인해 애플리케이션의 수동 삭제 로직 일부는 단순화 또는 제거한다.
- 기존 운영 데이터 마이그레이션은 범위 외이며, 스키마 변경은 개발 환경 기준으로 처리한다.
- 정렬 기준 동점 처리는 `id DESC`를 2차 정렬 기준으로 사용한다 (auto-increment id가 삽입 순서를 보장하며 PK 인덱스 활용 가능).
- 검색 기능은 Post 도메인에만 한정하며 Comment 검색은 범위 외다.
