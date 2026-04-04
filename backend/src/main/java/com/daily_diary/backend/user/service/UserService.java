package com.daily_diary.backend.user.service;

import com.daily_diary.backend.user.infra.UserRepository;
import com.daily_diary.backend.user.web.UserDetailResponse;
import com.daily_diary.backend.user.web.UserNicknameUpdateRequest;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final Cache<Long, String> refreshTokenCache;

    public UserDetailResponse get(Long userId) {
        return UserDetailResponse.from(userRepository.findOrThrow(userId));
    }

    @Transactional
    public void update(Long userId, UserNicknameUpdateRequest request) {
        userRepository.findOrThrow(userId).changeNickname(request.nickname());
    }

    @Transactional
    public void delete(Long userId) {
        userRepository.delete(userRepository.findOrThrow(userId));
        refreshTokenCache.invalidate(userId);
    }
}
