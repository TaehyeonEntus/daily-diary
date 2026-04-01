package com.daily_diary.backend.user.service;

import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.UserNotFoundException;
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

    public UserDetailResponse getMe(Long userId) {
        return userRepository.findById(userId)
                .map(UserDetailResponse::from)
                .orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public UserDetailResponse updateMe(Long userId, UserNicknameUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.changeNickname(request.nickname());
        return UserDetailResponse.from(user);
    }

    @Transactional
    public void deleteMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        refreshTokenCache.invalidate(userId);
        userRepository.delete(user);
    }
}
