package com.daily_diary.backend.post.web;

public record PostSearchCondition(
        SearchType searchType,
        String keyword
) {}
