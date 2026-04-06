package com.daily_diary.backend.post.service;

import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.entity.PostLike;
import com.daily_diary.backend.post.exception.LikeAlreadyExistsException;
import com.daily_diary.backend.post.exception.LikeNotFoundException;
import com.daily_diary.backend.post.exception.PostAccessDeniedException;
import com.daily_diary.backend.post.exception.PostNotFoundException;
import com.daily_diary.backend.post.infra.PostLikeRepository;
import com.daily_diary.backend.post.infra.PostQueryRepository;
import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.post.web.*;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @InjectMocks
    PostService postService;

    @Mock
    PostRepository postRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    PostQueryRepository postQueryRepository;

    @Mock
    PostLikeRepository postLikeRepository;

    @Mock
    List<Long> hotPostsCache;

    private User createUser(Long id, String nickname) {
        User user = User.of("user" + id, "encoded", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Post createPost(User user, String title, String content) {
        return Post.of(title, content, user);
    }

    // ─── getList ──────────────────────────────────────────────────────────────

    @Test
    void getList_정상() {
        // given
        PostSummaryResponse summary = new PostSummaryResponse(1L, "제목", "닉네임", 0L, 0L, 0L, null);
        given(postQueryRepository.getPage(any(PostSearchCondition.class), any(OrderType.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(summary)));

        // when
        PostPageResponse response = postService.getList(
                new PostSearchCondition(SearchType.DEFAULT, null),
                OrderType.DATE,
                0, 10);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("제목");
    }

    @Test
    void getList_닉네임_필터() {
        // given
        PostSummaryResponse summary = new PostSummaryResponse(1L, "제목", "홍길동", 0L, 0L, 0L, null);
        given(postQueryRepository.getPage(any(PostSearchCondition.class), any(OrderType.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(summary)));

        // when
        PostPageResponse response = postService.getList(
                new PostSearchCondition(SearchType.NICKNAME, "홍길동"),
                OrderType.DATE,
                0, 10);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).nickname()).isEqualTo("홍길동");
    }

    @Test
    void getList_정렬_VIEW() {
        // given
        given(postQueryRepository.getPage(any(PostSearchCondition.class), any(OrderType.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        PostPageResponse response = postService.getList(
                new PostSearchCondition(SearchType.DEFAULT, null),
                OrderType.VIEW,
                0, 10);

        // then
        assertThat(response.content()).isEmpty();
        ArgumentCaptor<OrderType> captor = ArgumentCaptor.forClass(OrderType.class);
        verify(postQueryRepository).getPage(any(PostSearchCondition.class), captor.capture(), any(Pageable.class));
        assertThat(captor.getValue()).isEqualTo(OrderType.VIEW);
    }

    // ─── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_정상_비인증() {
        // given
        PostDetailResponse detail = new PostDetailResponse(1L, "제목", "내용", "닉네임", 10L, 0L, 0L, false, null, null);
        given(postQueryRepository.findPostDetail(1L, null)).willReturn(detail);

        // when
        PostDetailResponse response = postService.get(1L, null);

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.like()).isFalse();
        verify(postRepository).increaseViewCount(1L);
    }

    @Test
    void get_좋아요_누른_경우_likedByMe_true() {
        // given
        PostDetailResponse detail = new PostDetailResponse(1L, "제목", "내용", "닉네임", 10L, 0L, 0L, true, null, null);
        given(postQueryRepository.findPostDetail(1L, 1L)).willReturn(detail);

        // when
        PostDetailResponse response = postService.get(1L, 1L);

        // then
        assertThat(response.like()).isTrue();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    void create_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 1L);
        given(userRepository.findOrThrow(1L)).willReturn(user);
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        PostDetailResponse response = postService.create(1L, new CreatePostRequest("제목", "내용"));

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.like()).isFalse();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    void update_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "기존제목", "기존내용");
        given(postRepository.findOrThrow(1L)).willReturn(post);

        // when
        postService.update(1L, 1L, new UpdatePostRequest("수정제목", "수정내용"));

        // then
        assertThat(post.getTitle()).isEqualTo("수정제목");
        assertThat(post.getContent()).isEqualTo("수정내용");
    }

    @Test
    void update_없는_게시글_예외() {
        // given
        given(postRepository.findOrThrow(99L)).willThrow(new PostNotFoundException());

        // when & then
        assertThatThrownBy(() -> postService.update(1L, 99L, new UpdatePostRequest("제목", "내용")))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void update_작성자_아닌_사용자_예외() {
        // given
        User owner = createUser(1L, "작성자");
        Post post = createPost(owner, "제목", "내용");
        given(postRepository.findOrThrow(1L)).willReturn(post);

        // when & then
        assertThatThrownBy(() -> postService.update(2L, 1L, new UpdatePostRequest("제목", "내용")))
                .isInstanceOf(PostAccessDeniedException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postRepository.findOrThrow(1L)).willReturn(post);

        // when
        postService.delete(1L, 1L);

        // then
        verify(postRepository).delete(post);
    }

    @Test
    void delete_없는_게시글_예외() {
        // given
        given(postRepository.findOrThrow(99L)).willThrow(new PostNotFoundException());

        // when & then
        assertThatThrownBy(() -> postService.delete(1L, 99L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void delete_작성자_아닌_사용자_예외() {
        // given
        User owner = createUser(1L, "작성자");
        Post post = createPost(owner, "제목", "내용");
        given(postRepository.findOrThrow(1L)).willReturn(post);

        // when & then
        assertThatThrownBy(() -> postService.delete(2L, 1L))
                .isInstanceOf(PostAccessDeniedException.class);
    }

    // ─── like ─────────────────────────────────────────────────────────────────

    @Test
    void like_정상() {
        // given
        given(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(false);

        // when
        postService.like(1L, 1L);

        // then
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).increaseLikeCount(1L);
    }

    @Test
    void like_이미_좋아요_예외() {
        // given
        given(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> postService.like(1L, 1L))
                .isInstanceOf(LikeAlreadyExistsException.class);
    }

    // ─── unlike ───────────────────────────────────────────────────────────────

    @Test
    void unlike_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        PostLike postLike = PostLike.of(post, user);
        given(postLikeRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.of(postLike));

        // when
        postService.unlike(1L, 1L);

        // then
        verify(postLikeRepository).delete(postLike);
        verify(postRepository).decreaseLikeCount(1L);
    }

    @Test
    void unlike_좋아요_없음_예외() {
        // given
        given(postLikeRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.unlike(1L, 1L))
                .isInstanceOf(LikeNotFoundException.class);
    }
}
