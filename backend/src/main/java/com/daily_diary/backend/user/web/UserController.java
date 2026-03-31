package com.daily_diary.backend.user.web;

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
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PatchMapping
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal Long userId,
                                                  @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMe(userId, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Long userId) {
        userService.deleteMe(userId);
        return ResponseEntity.noContent().build();
    }
}
