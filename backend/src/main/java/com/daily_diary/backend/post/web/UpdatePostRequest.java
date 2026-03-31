package com.daily_diary.backend.post.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content
) {
}
