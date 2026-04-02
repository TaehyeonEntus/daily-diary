package com.daily_diary.backend.comment.service;

import com.daily_diary.backend.comment.entity.Comment;
import com.daily_diary.backend.comment.entity.CommentLike;
import com.daily_diary.backend.comment.exception.CommentAccessDeniedException;
import com.daily_diary.backend.comment.exception.CommentLikeAlreadyExistsException;
import com.daily_diary.backend.comment.exception.CommentLikeNotFoundException;
import com.daily_diary.backend.comment.exception.CommentNotFoundException;
import com.daily_diary.backend.comment.infra.CommentLikeRepository;
import com.daily_diary.backend.comment.infra.CommentRepository;
import com.daily_diary.backend.comment.web.CommentDetailResponse;
import com.daily_diary.backend.comment.web.CommentListResponse;
import com.daily_diary.backend.comment.web.CreateCommentRequest;
import com.daily_diary.backend.comment.web.UpdateCommentRequest;
import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.exception.PostNotFoundException;
import com.daily_diary.backend.post.infra.PostRepository;
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
class CommentServiceTest {

    @InjectMocks
    CommentService commentService;

    @Mock
    CommentRepository commentRepository;

    @Mock
    CommentLikeRepository commentLikeRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    UserRepository userRepository;

    private User createUser(Long id, String nickname) {
        User user = User.of("user" + id, "encoded", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Post createPost(User user) {
        return Post.of("제목", "내용", user);
    }

    // ─── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_정상_비인증() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("댓글", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        given(postRepository.existsById(1L)).willReturn(true);
        given(commentRepository.findByPostId(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        // when
        CommentListResponse response = commentService.list(1L, null, 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).content()).isEqualTo("댓글");
        assertThat(response.content().get(0).likedByMe()).isFalse();
    }

    @Test
    void list_정상_인증_likedByMe_true() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("댓글", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        CommentLike like = CommentLike.of(comment, user);
        given(postRepository.existsById(1L)).willReturn(true);
        given(commentRepository.findByPostId(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));
        given(commentLikeRepository.findAllByCommentIdInAndUserId(any(), any()))
                .willReturn(List.of(like));

        // when
        CommentListResponse response = commentService.list(1L, 1L, 0, 20);

        // then
        assertThat(response.content().get(0).likedByMe()).isTrue();
    }

    @Test
    void list_빈_목록() {
        // given
        given(postRepository.existsById(1L)).willReturn(true);
        given(commentRepository.findByPostId(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        CommentListResponse response = commentService.list(1L, null, 0, 20);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void list_없는_게시글_예외() {
        // given
        given(postRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.list(99L, null, 0, 20))
                .isInstanceOf(PostNotFoundException.class);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    void create_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        commentService.create(1L, 1L, new CreateCommentRequest("댓글 내용"));

        // then
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("댓글 내용");
        assertThat(captor.getValue().getPost()).isEqualTo(post);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void create_없는_게시글_예외() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(99L, 1L, new CreateCommentRequest("내용")))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void create_없는_사용자_예외() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(1L, 99L, new CreateCommentRequest("내용")))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    void update_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("기존내용", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(commentLikeRepository.existsByCommentIdAndUserId(1L, 1L)).willReturn(false);

        // when
        CommentDetailResponse response = commentService.update(1L, 1L, new UpdateCommentRequest("수정내용"));

        // then
        assertThat(response.content()).isEqualTo("수정내용");
        assertThat(response.likedByMe()).isFalse();
    }

    @Test
    void update_없는_댓글_예외() {
        // given
        given(commentRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.update(99L, 1L, new UpdateCommentRequest("내용")))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    void update_작성자_아닌_사용자_예외() {
        // given
        User owner = createUser(1L, "작성자");
        Post post = createPost(owner);
        Comment comment = Comment.of("내용", post, owner);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.update(1L, 2L, new UpdateCommentRequest("내용")))
                .isInstanceOf(CommentAccessDeniedException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("내용", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // when
        commentService.delete(1L, 1L);

        // then
        verify(commentRepository).delete(comment);
    }

    @Test
    void delete_없는_댓글_예외() {
        // given
        given(commentRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.delete(99L, 1L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    void delete_작성자_아닌_사용자_예외() {
        // given
        User owner = createUser(1L, "작성자");
        Post post = createPost(owner);
        Comment comment = Comment.of("내용", post, owner);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.delete(1L, 2L))
                .isInstanceOf(CommentAccessDeniedException.class);
    }

    // ─── like ─────────────────────────────────────────────────────────────────

    @Test
    void like_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("내용", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(commentLikeRepository.existsByCommentIdAndUserId(1L, 1L)).willReturn(false);

        // when
        commentService.like(1L, 1L);

        // then
        verify(commentLikeRepository).save(any(CommentLike.class));
        verify(commentRepository).increaseLikeCount(1L);
    }

    @Test
    void like_없는_댓글_예외() {
        // given
        given(commentRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.like(99L, 1L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    void like_이미_좋아요_예외() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("내용", post, user);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(commentLikeRepository.existsByCommentIdAndUserId(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> commentService.like(1L, 1L))
                .isInstanceOf(CommentLikeAlreadyExistsException.class);
    }

    // ─── unlike ───────────────────────────────────────────────────────────────

    @Test
    void unlike_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("내용", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        CommentLike commentLike = CommentLike.of(comment, user);
        given(commentLikeRepository.findByCommentIdAndUserId(1L, 1L)).willReturn(Optional.of(commentLike));

        // when
        commentService.unlike(1L, 1L);

        // then
        verify(commentLikeRepository).delete(commentLike);
        verify(commentRepository).decreaseLikeCount(1L);
    }

    @Test
    void unlike_좋아요_없음_예외() {
        // given
        given(commentLikeRepository.findByCommentIdAndUserId(1L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.unlike(1L, 1L))
                .isInstanceOf(CommentLikeNotFoundException.class);
    }
}
