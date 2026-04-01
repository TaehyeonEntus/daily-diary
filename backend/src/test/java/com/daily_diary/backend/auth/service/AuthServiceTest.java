package com.daily_diary.backend.auth.service;

import com.daily_diary.backend.auth.exception.InvalidCredentialsException;
import com.daily_diary.backend.auth.exception.InvalidTokenException;
import com.daily_diary.backend.auth.web.LoginRequest;
import com.daily_diary.backend.auth.web.SignupRequest;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.DuplicateUsernameException;
import com.daily_diary.backend.user.infra.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    AuthService authService;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    Cache<Long, String> refreshTokenCache;

    // ─── signup ───────────────────────────────────────────────────────────────

    @Test
    void signup_정상() {
        // given
        given(userRepository.existsByUsername("testuser")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");

        // when
        authService.signup(new SignupRequest("testuser", "password123", "닉네임"));

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getNickname()).isEqualTo("닉네임");
    }

    @Test
    void signup_중복_username_예외() {
        // given
        given(userRepository.existsByUsername("testuser")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(new SignupRequest("testuser", "password123", "닉네임")))
                .isInstanceOf(DuplicateUsernameException.class);
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_정상() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("1", null);
        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");

        // when
        Tokens tokens = authService.login(new LoginRequest("testuser", "password123"));

        // then
        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenCache).put(1L, "refresh-token");
    }

    @Test
    void login_잘못된_credentials_예외() {
        // given
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("bad credentials"));

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ─── refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_정상() {
        // given
        given(jwtProvider.validate("old-refresh")).willReturn(true);
        given(jwtProvider.getUserId("old-refresh")).willReturn(1L);
        given(refreshTokenCache.getIfPresent(1L)).willReturn("old-refresh");
        given(jwtProvider.createAccessToken(1L)).willReturn("new-access");
        given(jwtProvider.createRefreshToken(1L)).willReturn("new-refresh");

        // when
        Tokens tokens = authService.refresh("old-refresh");

        // then
        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenCache).put(1L, "new-refresh");
    }

    @Test
    void refresh_유효하지않은_토큰_예외() {
        // given
        given(jwtProvider.validate(anyString())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refresh("invalid-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_캐시_miss_예외() {
        // given
        given(jwtProvider.validate("token")).willReturn(true);
        given(jwtProvider.getUserId("token")).willReturn(1L);
        given(refreshTokenCache.getIfPresent(1L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_캐시_불일치_예외() {
        // given
        given(jwtProvider.validate("token")).willReturn(true);
        given(jwtProvider.getUserId("token")).willReturn(1L);
        given(refreshTokenCache.getIfPresent(1L)).willReturn("other-token");

        // when & then
        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ─── logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_정상() {
        // when
        authService.logout(1L);

        // then
        verify(refreshTokenCache).invalidate(1L);
    }
}
