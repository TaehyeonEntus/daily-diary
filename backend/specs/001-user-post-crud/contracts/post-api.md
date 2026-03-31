# API 계약: 게시글 (Post)

**Base URL**: `/posts`

> **참고**: Nginx에서 `/api/` → 백엔드 `/`로 prefix 제거 후 프록시. 클라이언트는 `/api/posts/...`로 호출.

---

## GET /posts — 게시글 목록 조회

**인증 불필요**

### 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| page | int | 0 | 페이지 번호 (0부터 시작) |
| size | int | 10 | 페이지 크기 |

### 응답

**200 OK**
```json
{
  "content": [
    {
      "id": 1,
      "title": "첫 번째 게시글",
      "nickname": "홍길동",
      "createdAt": "2026-03-30T10:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## GET /posts/{id} — 게시글 상세 조회

**인증 불필요**

### 응답

**200 OK**
```json
{
  "id": 1,
  "title": "첫 번째 게시글",
  "content": "내용입니다.",
  "nickname": "홍길동",
  "createdAt": "2026-03-30T10:00:00",
  "updatedAt": "2026-03-30T10:00:00"
}
```

**404 Not Found**
```json
{ "message": "게시글을 찾을 수 없습니다." }
```

---

## POST /posts — 게시글 작성

**인증 필요** (`Authorization: Bearer <accessToken>`)

### 요청

```json
{
  "title": "제목",
  "content": "내용"
}
```

### 응답

**201 Created**
```json
{
  "id": 2,
  "title": "제목",
  "content": "내용",
  "nickname": "홍길동",
  "createdAt": "2026-03-30T11:00:00",
  "updatedAt": "2026-03-30T11:00:00"
}
```

**400 Bad Request** — 유효성 검증 실패
```json
{ "message": "입력값이 올바르지 않습니다." }
```

**401 Unauthorized**
```json
{ "message": "인증이 필요합니다." }
```

---

## PATCH /posts/{id} — 게시글 수정

**인증 필요** (`Authorization: Bearer <accessToken>`)

### 요청

```json
{
  "title": "수정된 제목",
  "content": "수정된 내용"
}
```

### 응답

**200 OK**
```json
{
  "id": 2,
  "title": "수정된 제목",
  "content": "수정된 내용",
  "nickname": "홍길동",
  "createdAt": "2026-03-30T11:00:00",
  "updatedAt": "2026-03-30T12:00:00"
}
```

**403 Forbidden** — 본인 게시글이 아닌 경우
```json
{ "message": "게시글을 수정할 권한이 없습니다." }
```

**404 Not Found**
```json
{ "message": "게시글을 찾을 수 없습니다." }
```

---

## DELETE /posts/{id} — 게시글 삭제

**인증 필요** (`Authorization: Bearer <accessToken>`)

### 응답

**204 No Content**

**403 Forbidden** — 본인 게시글이 아닌 경우
```json
{ "message": "게시글을 삭제할 권한이 없습니다." }
```

**404 Not Found**
```json
{ "message": "게시글을 찾을 수 없습니다." }
```
