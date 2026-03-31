# API 계약: 인증 (Auth)

**Base URL**: `/auth`

> **참고**: Nginx에서 `/api/` → 백엔드 `/`로 prefix 제거 후 프록시. 클라이언트는 `/auth/...`로 호출.

---

## POST /auth/signup — 회원가입

**인증 불필요**

### 요청

```json
{
  "username": "홍길동123",
  "password": "password123",
  "nickname": "홍길동"
}
```

| 필드 | 타입 | 제약 |
|------|------|------|
| username | String | 필수, 4~50자 |
| password | String | 필수, 최소 8자 |
| nickname | String | 필수 |

### 응답

**201 Created**
```json
{
  "id": 1,
  "username": "홍길동123",
  "nickname": "홍길동",
  "createdAt": "2026-03-31T12:00:00"
}
```

**409 Conflict** — 아이디 중복
```json
{ "message": "이미 사용 중인 아이디입니다." }
```

**400 Bad Request** — 유효성 검증 실패
```json
{ "message": "입력값이 올바르지 않습니다." }
```

---

## POST /auth/login — 로그인

**인증 불필요**

### 요청

```json
{
  "username": "홍길동123",
  "password": "password123"
}
```

### 응답

**200 OK**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci..."
}
```

**401 Unauthorized** — 아이디/비밀번호 불일치
```json
{ "message": "아이디 또는 비밀번호가 올바르지 않습니다." }
```

---

## POST /auth/refresh — 액세스 토큰 갱신

**인증 불필요** (리프레시 토큰으로 인증)

> **Refresh Token Rotation 적용**: 갱신 시 액세스 토큰과 리프레시 토큰을 함께 재발급한다.
> 기존 리프레시 토큰은 즉시 무효화되므로 클라이언트는 새 리프레시 토큰을 저장해야 한다.

### 요청

```json
{
  "refreshToken": "eyJhbGci..."
}
```

### 응답

**200 OK**
```json
{
  "accessToken": "eyJhbGci...(새 액세스 토큰)",
  "refreshToken": "eyJhbGci...(새 리프레시 토큰)"
}
```

**401 Unauthorized** — 만료되거나 유효하지 않은 리프레시 토큰
```json
{ "message": "유효하지 않은 토큰입니다. 다시 로그인해 주세요." }
```

---

## DELETE /auth/logout — 로그아웃

**인증 필요** (`Authorization: Bearer <accessToken>`)

### 요청 본문

없음

### 응답

**204 No Content**

**401 Unauthorized**
```json
{ "message": "인증이 필요합니다." }
```
