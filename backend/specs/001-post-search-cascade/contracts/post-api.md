# API Contracts: Post 검색 및 조회수

## 변경된 엔드포인트

### GET /posts — 목록 조회 (검색 파라미터 추가)

기존 `GET /posts?page=0&size=10`에 검색 파라미터를 추가합니다. 하위 호환 유지 (기존 파라미터만 사용해도 정상 동작).

**Request**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | int | No | 0 | 페이지 번호 |
| `size` | int | No | 10 | 페이지 크기 |
| `nickname` | String | No | null | 작성자 닉네임 부분 일치 |
| `title` | String | No | null | 제목 부분 일치 |
| `content` | String | No | null | 내용 부분 일치 |
| `sort` | SortType | No | DATE | 정렬 기준 (DATE, VIEW, LIKE) |

```
GET /posts?nickname=홍길동&sort=LIKE&page=0&size=10
Authorization: (불필요)
```

**Response** (기존과 동일 구조)

```json
{
  "content": [
    {
      "id": 1,
      "title": "오늘의 일기",
      "nickname": "홍길동",
      "viewCount": 42,
      "likeCount": 10,
      "createdAt": "2026-04-02T12:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

**Status Codes**

| Code | Condition |
|------|-----------|
| 200 | 정상 (검색 결과 없으면 빈 배열) |
| 400 | 유효하지 않은 sort 값 |

---

### GET /posts/{id} — 상세 조회 (viewCount 자동 증가)

기존 엔드포인트 동작 변경: 조회 시 `viewCount` 1 증가 후 반환.

**Request** (변경 없음)

```
GET /posts/1
Authorization: Bearer {accessToken}  (선택)
```

**Response** (viewCount가 이전 조회 대비 +1)

```json
{
  "id": 1,
  "title": "오늘의 일기",
  "content": "오늘 하루는...",
  "nickname": "홍길동",
  "viewCount": 43,
  "likeCount": 10,
  "likedByMe": false,
  "createdAt": "2026-04-02T12:00:00",
  "updatedAt": "2026-04-02T12:00:00"
}
```

**Status Codes** (변경 없음)

| Code | Condition |
|------|-----------|
| 200 | 정상 |
| 404 | 게시글 없음 |

---

## 변경 없는 엔드포인트

- `POST /posts` — 생성 (변경 없음)
- `PATCH /posts/{id}` — 수정 (변경 없음)
- `DELETE /posts/{id}` — 삭제 (내부 동작만 변경: CASCADE by DB, 응답 변경 없음)
- `POST /posts/{id}/likes` — 좋아요 (변경 없음)
- `DELETE /posts/{id}/likes` — 좋아요 취소 (변경 없음)
- Comment 관련 API — 모두 변경 없음

---

## SortType 정의

| Value | Description | Order |
|-------|-------------|-------|
| `DATE` | 최신순 (기본값) | `id DESC` |
| `VIEW` | 조회수 순 | `viewCount DESC, id DESC` |
| `LIKE` | 좋아요 순 | `likeCount DESC, id DESC` |
