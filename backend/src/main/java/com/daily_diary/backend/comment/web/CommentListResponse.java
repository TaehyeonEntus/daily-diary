package com.daily_diary.backend.comment.web;

import org.springframework.data.domain.Page;

import java.util.List;

public record CommentListResponse(
        List<CommentSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CommentListResponse from(Page<CommentSummaryResponse> pageResult) {
        return new CommentListResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
