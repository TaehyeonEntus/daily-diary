package com.daily_diary.backend.user.web;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordUpdateRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
