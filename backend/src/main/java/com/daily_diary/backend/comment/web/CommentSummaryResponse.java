package com.daily_diary.backend.comment.web;

import com.daily_diary.backend.comment.entity.Comment;

import java.time.LocalDateTime;

public record CommentSummaryResponse(
        Long id,
        String content,
        String nickname,
        LocalDateTime createdAt
) {
    public static CommentSummaryResponse from(Comment comment) {
        return new CommentSummaryResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getNickname(),
                comment.getCreatedAt()
        );
    }
}
