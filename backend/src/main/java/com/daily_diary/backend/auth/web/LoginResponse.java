package com.daily_diary.backend.auth.web;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
