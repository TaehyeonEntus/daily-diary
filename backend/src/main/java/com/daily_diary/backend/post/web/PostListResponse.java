package com.daily_diary.backend.post.web;

import org.springframework.data.domain.Page;

import java.util.List;

public record PostListResponse(
        List<PostSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static PostListResponse from(Page<PostSummaryResponse> pageResult) {
        return new PostListResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
