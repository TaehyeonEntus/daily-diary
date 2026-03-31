# 프로젝트 작업 목록 (TODO)

## 📋 데이터 모델 및 정책 수정 (data-model.md 반영 사항)

### User 엔티티
- [x] `email` 필드를 `username`으로 변경 (로그인 아이디로 사용)
- [x] `createdAt`, `updatedAt` 직접 선언 대신 `BaseEntity` 상속으로 변경
- [x] 삭제 정책 확인: Soft Delete를 사용하지 않고 DB에서 즉시 삭제 (Hard Delete)

### Post 엔티티
- [x] `BaseEntity` 상속 적용 (공통 시간 필드 사용)

### 인증 및 보안 (JWT)
- [x] **RefreshToken 관리 방식 변경**
    - [x] Redis 대신 `CaffeineCache` 기반으로 관리
    - [x] 저장 구조: Key(`refreshToken`), Value(`userId`)
- [x] **토큰 갱신 로직 변경**
    - [x] Refresh 시 `accessToken`만 재발급하는 것이 아니라, `accessToken`과 `refreshToken`을 **함께 재발급** (Refresh Token Rotation 적용)

---

## 🛠 실구현 태스크

### 1. 전역 설정 및 인프라 수정
- [x] CaffeineCache 의존성 추가 (`build.gradle`) 및 설정 클래스 작성
- [ ] `JwtProvider` 수정: 토큰 쌍(Pair) 발급 로직 추가

### 2. Auth 도메인 수정
- [x] `AuthService.refresh()` 로직 수정: 캐시에서 기존 토큰 삭제 및 새로운 토큰 쌍 저장/반환
- [x] `AuthController.refresh()` 응답 DTO 수정 (refreshToken 포함)

### 3. User 도메인 완성 (완료)
- [x] `UserService` 구현: 프로필 조회, 수정, 탈퇴 (Hard Delete)
- [x] `UserController` 구현 및 API 문서(REST Docs) 연동
- [x] `UserControllerTest` 작성

### 4. Post 도메인 구현 (완료)
- [x] 엔티티 및 Repository 작성
- [x] CRUD 서비스 및 컨트롤러 구현
- [x] 작성자 본인 확인 로직 추가

---

## 🔍 검토 사항
- [ ] `BaseEntity`의 `@PrePersist`, `@PreUpdate`가 정상 작동하는지 확인
- [x] CaffeineCache의 만료 시간(TTL)을 `application.properties`의 `jwt.refresh-token-expiry`와 동기화
