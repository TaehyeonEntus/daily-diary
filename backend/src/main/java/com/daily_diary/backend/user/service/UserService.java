package com.daily_diary.backend.user.service;

import com.daily_diary.backend.user.exception.UserNotFoundException;
import com.daily_diary.backend.user.infra.UserRepository;
import com.daily_diary.backend.user.web.UserResponse;
import com.daily_diary.backend.user.web.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String REFRESH_TOKEN_CACHE = "refreshTokens";

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public UserResponse getMe(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.changeNickname(request.nickname());
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteMe(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        userRepository.delete(user);
        cacheManager.getCache(REFRESH_TOKEN_CACHE).evict(userId);
    }
}
