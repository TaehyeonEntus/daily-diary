package com.daily_diary.backend.comment.web;

import com.daily_diary.backend.comment.entity.Comment;

import java.time.LocalDateTime;

public record CommentDetailResponse(
        Long id,
        String content,
        String nickname,
        long likeCount,
        boolean likedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentDetailResponse from(Comment comment, boolean likedByMe) {
        return new CommentDetailResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getNickname(),
                comment.getLikeCount(),
                likedByMe,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
