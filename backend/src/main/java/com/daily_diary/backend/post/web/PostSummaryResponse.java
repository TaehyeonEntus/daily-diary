package com.daily_diary.backend.post.web;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        String nickname,
        long viewCount,
        long likeCount,
        long commentCount,
        LocalDateTime createdAt
) {
}
