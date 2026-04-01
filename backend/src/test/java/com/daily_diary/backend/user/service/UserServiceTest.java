package com.daily_diary.backend.user.service;

import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.UserNotFoundException;
import com.daily_diary.backend.user.infra.UserRepository;
import com.daily_diary.backend.user.web.UserDetailResponse;
import com.daily_diary.backend.user.web.UserNicknameUpdateRequest;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    Cache<Long, String> refreshTokenCache;

    @InjectMocks
    UserService userService;

    // ─── getMe ────────────────────────────────────────────────────────────────

    @Test
    void getMe_정상() {
        // given
        User user = User.of("testuser", "encoded", "닉네임");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserDetailResponse response = userService.getMe(1L);

        // then
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.nickname()).isEqualTo("닉네임");
    }

    @Test
    void getMe_없는_사용자_예외() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMe(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─── updateMe ─────────────────────────────────────────────────────────────

    @Test
    void updateMe_정상() {
        // given
        User user = User.of("testuser", "encoded", "기존닉네임");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserDetailResponse response = userService.updateMe(1L, new UserNicknameUpdateRequest("새닉네임"));

        // then
        assertThat(response.nickname()).isEqualTo("새닉네임");
    }

    @Test
    void updateMe_없는_사용자_예외() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateMe(99L, new UserNicknameUpdateRequest("닉네임")))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─── deleteMe ─────────────────────────────────────────────────────────────

    @Test
    void deleteMe_정상_캐시무효화후_DB삭제() {
        // given
        User user = User.of("testuser", "encoded", "닉네임");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        userService.deleteMe(1L);

        // then
        InOrder inOrder = inOrder(refreshTokenCache, userRepository);
        inOrder.verify(refreshTokenCache).invalidate(1L);
        inOrder.verify(userRepository).delete(user);
    }

    @Test
    void deleteMe_없는_사용자_예외() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteMe(99L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
