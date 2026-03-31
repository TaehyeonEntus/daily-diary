package com.daily_diary.backend.auth.web;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}
