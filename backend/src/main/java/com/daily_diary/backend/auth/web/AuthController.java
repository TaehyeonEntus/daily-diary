package com.daily_diary.backend.auth.web;

import com.daily_diary.backend.auth.service.AuthService;
import com.daily_diary.backend.auth.service.Tokens;
import com.daily_diary.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        Tokens result = authService.login(request);
        ResponseCookie refreshCookie = buildRefreshTokenCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(result.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue("refreshToken") String refreshToken) {
        Tokens result = authService.refresh(refreshToken);
        ResponseCookie refreshCookie = buildRefreshTokenCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new RefreshResponse(result.accessToken()));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.userId());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildExpiredTokenCookie().toString())
                .build();
    }

    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenExpiry))
                .build();
    }

    private ResponseCookie buildExpiredTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
    }
}
