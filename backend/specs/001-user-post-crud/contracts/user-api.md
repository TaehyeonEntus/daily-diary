# API 계약: 사용자 (User)

**Base URL**: `/users`

> **참고**: Nginx에서 `/api/` → 백엔드 `/`로 prefix 제거 후 프록시. 클라이언트는 `/api/users/...`로 호출.
**모든 엔드포인트**: 인증 필요 (`Authorization: Bearer <accessToken>`)

---

## GET /users/me — 내 프로필 조회

### 응답

**200 OK**
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "createdAt": "2026-03-30T10:00:00"
}
```

**401 Unauthorized**
```json
{ "message": "인증이 필요합니다." }
```

---

## PATCH /users/me — 내 프로필 수정

### 요청

```json
{
  "nickname": "새닉네임"
}
```

### 응답

**200 OK**
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "새닉네임",
  "createdAt": "2026-03-30T10:00:00"
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

## DELETE /users/me — 회원 탈퇴

### 요청 본문

없음

### 응답

**204 No Content**

**401 Unauthorized**
```json
{ "message": "인증이 필요합니다." }
```
