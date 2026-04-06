package com.daily_diary.backend.user.service;

import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.InvalidPasswordException;
import com.daily_diary.backend.user.infra.UserRepository;
import com.daily_diary.backend.user.web.UserDetailResponse;
import com.daily_diary.backend.user.web.UserNicknameUpdateRequest;
import com.daily_diary.backend.user.web.UserPasswordUpdateRequest;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final Cache<Long, String> refreshTokenCache;
    private final PasswordEncoder passwordEncoder;

    public UserDetailResponse get(Long userId) {
        return UserDetailResponse.from(userRepository.findOrThrow(userId));
    }

    @Transactional
    public void update(Long userId, UserNicknameUpdateRequest request) {
        userRepository.findOrThrow(userId).changeNickname(request.nickname());
        postRepository.updateAuthorByUserId(userId, request.nickname());
    }

    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        User user = userRepository.findOrThrow(userId);
        validateCurrentPassword(request.currentPassword(), user.getPassword());
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void delete(Long userId) {
        userRepository.delete(userRepository.getReferenceById(userId));
        refreshTokenCache.invalidate(userId);
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private void validateCurrentPassword(String raw, String encoded) {
        if (!passwordEncoder.matches(raw, encoded)) {
            throw new InvalidPasswordException();
        }
    }
}
