# 데이터 모델: User 및 게시글 CRUD with JWT 인증

**작성일**: 2026-03-30

## 엔티티 관계

```
User (1) ─────────── (N) Post
User (1) ─────────── (N) RefreshToken
```

---

## User

**테이블명**: `users`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 이메일 |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 비밀번호 |
| nickname | VARCHAR(50) | NOT NULL | 표시 이름 |
| created_at | DATETIME | NOT NULL | 가입일시 |
| updated_at | DATETIME | NOT NULL | 수정일시 |
| deleted_at | DATETIME | NULL | 탈퇴일시 (소프트 삭제) |

**유효성 규칙**:
- `email`: 이메일 형식 필수, 중복 불허
- `password`: 최소 8자 이상
- `nickname`: 1~50자

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

## RefreshToken

**테이블명**: `refresh_tokens`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| token | VARCHAR(512) | UNIQUE, NOT NULL | 리프레시 토큰 값 |
| user_id | BIGINT | FK(users.id), NOT NULL | 소유 사용자 |
| expires_at | DATETIME | NOT NULL | 만료일시 |
| created_at | DATETIME | NOT NULL | 발급일시 |

**규칙**:
- 로그인 시 기존 토큰 삭제 후 신규 발급 (사용자당 1개 유지)
- 로그아웃 시 해당 토큰 삭제
- 만료된 토큰으로 갱신 요청 시 401 반환

---

## 상태 전이

### 사용자 상태
```
미가입 → [회원가입] → 활성(deleted_at = null)
활성   → [회원탈퇴] → 탈퇴(deleted_at = 현재시각)
탈퇴   → 로그인 불가
```

### 토큰 상태
```
없음 → [로그인] → 액세스토큰 + 리프레시토큰 발급
만료  → [토큰갱신] → 새 액세스토큰 발급
유효  → [로그아웃] → 리프레시토큰 삭제
```
