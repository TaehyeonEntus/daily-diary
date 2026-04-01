package com.daily_diary.backend.user.web;

import com.daily_diary.backend.user.entity.User;

import java.time.LocalDateTime;

public record UserDetailResponse(
        Long id,
        String username,
        String nickname,
        LocalDateTime createdAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(user.getId(), user.getUsername(), user.getNickname(), user.getCreatedAt());
    }
}
