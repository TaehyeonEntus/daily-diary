package com.daily_diary.backend.post.web;

import com.daily_diary.backend.post.entity.Post;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        String nickname,
        long likeCount,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getUser().getNickname(),
                post.getLikeCount(),
                post.getCreatedAt()
        );
    }
}
