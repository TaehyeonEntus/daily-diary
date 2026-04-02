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
import com.daily_diary.backend.post.web.CreatePostRequest;
import com.daily_diary.backend.post.web.PostListResponse;
import com.daily_diary.backend.post.web.PostDetailResponse;
import com.daily_diary.backend.post.web.PostSearchCondition;
import com.daily_diary.backend.post.web.PostSummaryResponse;
import com.daily_diary.backend.post.web.SortType;
import com.daily_diary.backend.post.web.UpdatePostRequest;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.UserNotFoundException;
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

    private User createUser(Long id, String nickname) {
        User user = User.of("user" + id, "encoded", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Post createPost(User user, String title, String content) {
        return Post.of(title, content, user);
    }

    // ─── search ───────────────────────────────────────────────────────────────

    @Test
    void search_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postQueryRepository.search(any(PostSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(PostSummaryResponse.from(post))));

        // when
        PostListResponse response = postService.search(new PostSearchCondition(null, null, null, SortType.DATE), 0, 10);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("제목");
    }

    @Test
    void search_닉네임_필터() {
        // given
        User user = createUser(1L, "홍길동");
        Post post = createPost(user, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 1L);
        PostSearchCondition condition = new PostSearchCondition("홍길동", null, null, SortType.DATE);
        given(postQueryRepository.search(any(PostSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(PostSummaryResponse.from(post))));

        // when
        PostListResponse response = postService.search(condition, 0, 10);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).nickname()).isEqualTo("홍길동");
    }

    @Test
    void search_정렬_VIEW() {
        // given
        PostSearchCondition condition = new PostSearchCondition(null, null, null, SortType.VIEW);
        given(postQueryRepository.search(any(PostSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        PostListResponse response = postService.search(condition, 0, 10);

        // then
        assertThat(response.content()).isEmpty();
        ArgumentCaptor<PostSearchCondition> captor = ArgumentCaptor.forClass(PostSearchCondition.class);
        verify(postQueryRepository).search(captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().sort()).isEqualTo(SortType.VIEW);
    }

    // ─── getPost ──────────────────────────────────────────────────────────────

    @Test
    void getPost_정상_비인증() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when
        PostDetailResponse response = postService.getPost(1L, null);

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.likedByMe()).isFalse();
        verify(postRepository).increaseViewCount(1L);
        assertThat(response.viewCount()).isEqualTo(1L);
    }

    @Test
    void getPost_좋아요_누른_경우_likedByMe_true() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(true);

        // when
        PostDetailResponse response = postService.getPost(1L, 1L);

        // then
        assertThat(response.likedByMe()).isTrue();
    }

    @Test
    void getPost_없는_게시글_예외() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(99L, null))
                .isInstanceOf(PostNotFoundException.class);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    void create_정상() {
        // given
        User user = createUser(1L, "닉네임");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        postService.create(1L, new CreatePostRequest("제목", "내용"));

        // then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("제목");
        assertThat(saved.getContent()).isEqualTo("내용");
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void create_없는_사용자_예외() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.create(99L, new CreatePostRequest("제목", "내용")))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    void update_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "기존제목", "기존내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when
        PostDetailResponse response = postService.update(1L, 1L, new UpdatePostRequest("수정제목", "수정내용"));

        // then
        assertThat(response.title()).isEqualTo("수정제목");
        assertThat(response.content()).isEqualTo("수정내용");
    }

    @Test
    void update_없는_게시글_예외() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.update(1L, 99L, new UpdatePostRequest("제목", "내용")))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void update_작성자_아닌_사용자_예외() {
        // given
        User owner = createUser(1L, "작성자");
        Post post = createPost(owner, "제목", "내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

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
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when
        postService.delete(1L, 1L);

        // then
        verify(postRepository).delete(post);
    }

    @Test
    void delete_없는_게시글_예외() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.delete(1L, 99L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void delete_작성자_아닌_사용자_예외() {
        // given
        User owner = createUser(1L, "작성자");
        Post post = createPost(owner, "제목", "내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.delete(2L, 1L))
                .isInstanceOf(PostAccessDeniedException.class);
    }

    // ─── like ─────────────────────────────────────────────────────────────────

    @Test
    void like_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(false);

        // when
        postService.like(1L, 1L);

        // then
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).increaseLikeCount(1L);
    }

    @Test
    void like_없는_게시글_예외() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.like(1L, 99L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void like_이미_좋아요_예외() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user, "제목", "내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
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
