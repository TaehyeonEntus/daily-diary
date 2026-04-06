package com.daily_diary.backend.user.service;

import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.InvalidPasswordException;
import com.daily_diary.backend.user.exception.UserNotFoundException;
import com.daily_diary.backend.user.infra.UserRepository;
import com.daily_diary.backend.user.web.UserDetailResponse;
import com.daily_diary.backend.user.web.UserNicknameUpdateRequest;
import com.daily_diary.backend.user.web.UserPasswordUpdateRequest;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @InjectMocks
    UserService userService;

    @Mock
    UserRepository userRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    Cache<Long, String> refreshTokenCache;

    @Mock
    PasswordEncoder passwordEncoder;

    // ─── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_정상() {
        // given
        User user = User.of("testuser", "encoded", "닉네임");
        given(userRepository.findOrThrow(1L)).willReturn(user);

        // when
        UserDetailResponse response = userService.get(1L);

        // then
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.nickname()).isEqualTo("닉네임");
    }

    @Test
    void get_없는_사용자_예외() {
        // given
        given(userRepository.findOrThrow(99L)).willThrow(new UserNotFoundException());

        // when & then
        assertThatThrownBy(() -> userService.get(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    void update_정상() {
        // given
        User user = User.of("testuser", "encoded", "기존닉네임");
        given(userRepository.findOrThrow(1L)).willReturn(user);

        // when
        userService.update(1L, new UserNicknameUpdateRequest("새닉네임"));

        // then
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        verify(postRepository).updateAuthorByUserId(1L, "새닉네임");
    }

    @Test
    void update_없는_사용자_예외() {
        // given
        given(userRepository.findOrThrow(99L)).willThrow(new UserNotFoundException());

        // when & then
        assertThatThrownBy(() -> userService.update(99L, new UserNicknameUpdateRequest("닉네임")))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─── updatePassword ───────────────────────────────────────────────────────

    @Test
    void updatePassword_정상() {
        // given
        User user = User.of("testuser", "encoded", "닉네임");
        given(userRepository.findOrThrow(1L)).willReturn(user);
        given(passwordEncoder.matches("currentPw123", "encoded")).willReturn(true);
        given(passwordEncoder.encode("newPw456!")).willReturn("new-encoded");

        // when
        userService.updatePassword(1L, new UserPasswordUpdateRequest("currentPw123", "newPw456!"));

        // then
        assertThat(user.getPassword()).isEqualTo("new-encoded");
    }

    @Test
    void updatePassword_현재_비밀번호_불일치_예외() {
        // given
        User user = User.of("testuser", "encoded", "닉네임");
        given(userRepository.findOrThrow(1L)).willReturn(user);
        given(passwordEncoder.matches("wrongPw", "encoded")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.updatePassword(1L, new UserPasswordUpdateRequest("wrongPw", "newPw456!")))
                .isInstanceOf(InvalidPasswordException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_정상_DB삭제후_캐시무효화() {
        // given
        User user = User.of("testuser", "encoded", "닉네임");
        given(userRepository.getReferenceById(1L)).willReturn(user);

        // when
        userService.delete(1L);

        // then
        verify(userRepository).delete(user);
        verify(refreshTokenCache).invalidate(1L);
    }
}
