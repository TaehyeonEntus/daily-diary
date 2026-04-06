package com.daily_diary.backend.post.web;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        String nickname,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean like,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
