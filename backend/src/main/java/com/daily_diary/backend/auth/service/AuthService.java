package com.daily_diary.backend.auth.service;

import com.daily_diary.backend.auth.exception.InvalidCredentialsException;
import com.daily_diary.backend.auth.exception.InvalidTokenException;
import com.daily_diary.backend.auth.web.LoginRequest;
import com.daily_diary.backend.auth.web.SignupRequest;
import com.daily_diary.backend.global.security.CustomUserDetails;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.DuplicateUsernameException;
import com.daily_diary.backend.user.infra.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final Cache<Long, String> refreshTokenCache;

    @Transactional
    public void signup(SignupRequest request) {
        validUniqueUsername(request.username());

        userRepository.save(User.of(request.username(), passwordEncoder.encode(request.password()), request.nickname()));
    }

    public Tokens login(LoginRequest request) {
        Long userId = ((CustomUserDetails) authenticate(request.username(), request.password()).getPrincipal()).getUserId();

        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenCache.put(userId, refreshToken);

        return new Tokens(accessToken, refreshToken);
    }

    @Transactional
    public Tokens refresh(String refreshToken) {
        validateToken(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);
        validRefreshToken(refreshToken, refreshTokenCache.getIfPresent(userId));

        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenCache.put(userId, newRefreshToken);

        return new Tokens(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId) {
        refreshTokenCache.invalidate(userId);
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private Authentication authenticate(String username, String password) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }
    }

    private void validateToken(String token) {
        if (!jwtProvider.validate(token)) {
            throw new InvalidTokenException();
        }
    }

    private void validUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException();
        }
    }

    private void validRefreshToken(String token, String cached) {
        if (cached == null || !token.equals(cached)) {
            throw new InvalidTokenException();
        }
    }
}
