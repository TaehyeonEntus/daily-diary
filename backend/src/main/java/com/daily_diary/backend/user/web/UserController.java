package com.daily_diary.backend.user.web;

import com.daily_diary.backend.global.security.CustomUserDetails;
import com.daily_diary.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserDetailResponse> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getMe(userDetails.userId()));
    }

    @PatchMapping
    public ResponseEntity<UserDetailResponse> updateMe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @Valid @RequestBody UserNicknameUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMe(userDetails.userId(), request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteMe(userDetails.userId());
        return ResponseEntity.noContent().build();
    }
}
