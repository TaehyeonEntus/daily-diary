package com.daily_diary.backend.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserNicknameUpdateRequest(
        @NotBlank @Size(max = 50) String nickname
) {
}
