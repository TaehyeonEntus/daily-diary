package com.daily_diary.backend.auth.service;

import com.daily_diary.backend.auth.exception.InvalidCredentialsException;
import com.daily_diary.backend.auth.exception.InvalidTokenException;
import com.daily_diary.backend.auth.web.LoginRequest;
import com.daily_diary.backend.auth.web.LoginResponse;
import com.daily_diary.backend.auth.web.SignupRequest;
import com.daily_diary.backend.auth.web.TokenRefreshResponse;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.DuplicateUsernameException;
import com.daily_diary.backend.user.infra.UserRepository;
import com.daily_diary.backend.user.web.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String CACHE_NAME = "refreshTokens";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CacheManager cacheManager;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateUsernameException();
        }

        User user = User.of(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );
        userRepository.save(user);

        return UserResponse.from(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenCache().put(user.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    public TokenRefreshResponse refresh(String refreshToken) {
        if (!jwtProvider.validate(refreshToken)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        Cache.ValueWrapper wrapper = refreshTokenCache().get(userId);

        if (wrapper == null || !refreshToken.equals(wrapper.get())) {
            throw new InvalidTokenException();
        }

        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        refreshTokenCache().put(userId, newRefreshToken);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId) {
        refreshTokenCache().evict(userId);
    }

    private Cache refreshTokenCache() {
        return cacheManager.getCache(CACHE_NAME);
    }
}
