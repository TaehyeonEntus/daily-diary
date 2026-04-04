package com.daily_diary.backend.post.web;

import java.util.List;

public record PostListResponse(
        List<PostSummaryResponse> content
) {
    public static PostListResponse from(List<PostSummaryResponse> content) {
        return new PostListResponse(content);
    }
}
