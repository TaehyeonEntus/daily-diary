# 데이터 모델: User 및 게시글 CRUD with JWT 인증

**작성일**: 2026-03-30
**수정일**: 2026-03-31 — email→username, Hard Delete, RefreshToken DB 테이블 제거(CaffeineCache로 대체), Refresh Token Rotation 반영

## 엔티티 관계

```
User (1) ─────────── (N) Post
```

> RefreshToken은 DB 테이블이 아닌 CaffeineCache(인메모리)로 관리한다.
> Key: `userId(Long)`, Value: `refreshToken(String)`, TTL: `jwt.refresh-token-expiry`

---

## User

**테이블명**: `users`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 로그인 아이디 (4~50자) |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 비밀번호 |
| nickname | VARCHAR(50) | NOT NULL | 표시 이름 |
| created_at | DATETIME | NOT NULL | 가입일시 |
| updated_at | DATETIME | NOT NULL | 수정일시 |

**유효성 규칙**:
- `username`: 4~50자, 중복 불허
- `password`: 최소 8자 이상
- `nickname`: 1~50자

**삭제 정책**: Hard Delete — 탈퇴 시 DB에서 즉시 제거. Soft Delete 미사용.

---

## Post

**테이블명**: `posts`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| title | VARCHAR(255) | NOT NULL | 게시글 제목 |
| content | TEXT | NOT NULL | 게시글 내용 |
| user_id | BIGINT | FK(users.id), NOT NULL | 작성자 |
| created_at | DATETIME | NOT NULL | 작성일시 |
| updated_at | DATETIME | NOT NULL | 수정일시 |

**유효성 규칙**:
- `title`: 1~255자
- `content`: 1자 이상

---

## RefreshToken (CaffeineCache)

DB 테이블 없음. 인메모리 캐시로 관리한다.

| 항목 | 값 |
|------|----|
| 캐시 이름 | `refreshTokens` |
| Key | `userId` (Long) |
| Value | `refreshToken` (String) |
| TTL | `jwt.refresh-token-expiry` (기본값: 604800000ms = 7일) |

**규칙**:
- 로그인 시 캐시에 저장 (userId → refreshToken)
- 갱신(refresh) 시 기존 토큰 삭제 후 새 토큰 쌍 발급 및 저장 (Refresh Token Rotation)
- 로그아웃 시 캐시에서 삭제
- 회원 탈퇴 시 캐시에서 삭제 (Hard Delete와 함께)
- 만료된 토큰 또는 캐시에 없는 토큰으로 갱신 요청 시 401 반환

---

## 상태 전이

### 사용자 상태
```
미가입 → [회원가입] → 활성
활성   → [회원탈퇴] → DB에서 즉시 삭제 (Hard Delete)
삭제 후 해당 아이디로 로그인 불가 (404 또는 401)
```

### 토큰 상태
```
없음 → [로그인] → 액세스토큰 + 리프레시토큰 발급 (캐시 저장)
유효 → [토큰갱신] → 기존 리프레시토큰 캐시 삭제 → 새 액세스토큰 + 새 리프레시토큰 발급 (Rotation)
유효 → [로그아웃] → 리프레시토큰 캐시 삭제
```
