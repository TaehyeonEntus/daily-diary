package com.daily_diary.backend.user.web;

import com.daily_diary.backend.global.security.CustomUserDetails;
import com.daily_diary.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDetailResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.get(userDetails.getUserId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserNicknameUpdateRequest request) {
        userService.update(userDetails.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.delete(userDetails.getUserId());
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .sameSite("Lax")
                                .secure(true)
                                .path("/")
                                .maxAge(0)
                                .build().toString()
                )
                .build();
    }
}
