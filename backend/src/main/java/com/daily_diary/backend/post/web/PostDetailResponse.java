package com.daily_diary.backend.post.web;

import com.daily_diary.backend.post.entity.Post;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        String nickname,
        long viewCount,
        long likeCount,
        boolean likedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse from(Post post, boolean likedByMe) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUser().getNickname(),
                post.getViewCount(),
                post.getLikeCount(),
                likedByMe,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public static PostDetailResponse from(Post post, long viewCount, boolean likedByMe) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUser().getNickname(),
                viewCount,
                post.getLikeCount(),
                likedByMe,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
