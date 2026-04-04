package com.daily_diary.backend.comment.service;

import com.daily_diary.backend.comment.entity.Comment;
import com.daily_diary.backend.comment.exception.CommentAccessDeniedException;
import com.daily_diary.backend.comment.exception.CommentNotFoundException;
import com.daily_diary.backend.comment.infra.CommentQueryRepository;
import com.daily_diary.backend.comment.infra.CommentRepository;
import com.daily_diary.backend.comment.web.CommentDetailResponse;
import com.daily_diary.backend.comment.web.CommentPageResponse;
import com.daily_diary.backend.comment.web.CommentSummaryResponse;
import com.daily_diary.backend.comment.web.CreateCommentRequest;
import com.daily_diary.backend.comment.web.UpdateCommentRequest;
import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.exception.PostNotFoundException;
import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.user.exception.UserNotFoundException;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    CommentService commentService;

    @Mock
    CommentRepository commentRepository;

    @Mock
    CommentQueryRepository commentQueryRepository;

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

    // ─── getPage ──────────────────────────────────────────────────────────────

    @Test
    void getPage_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("댓글", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        given(postRepository.existsById(1L)).willReturn(true);
        given(commentQueryRepository.getPage(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(CommentSummaryResponse.from(comment))));

        // when
        CommentPageResponse response = commentService.getPage(1L, 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).content()).isEqualTo("댓글");
    }

    @Test
    void getPage_빈_목록() {
        // given
        given(postRepository.existsById(1L)).willReturn(true);
        given(commentQueryRepository.getPage(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        CommentPageResponse response = commentService.getPage(1L, 0, 20);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void getPage_없는_게시글_예외() {
        // given
        given(postRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.getPage(99L, 0, 20))
                .isInstanceOf(PostNotFoundException.class);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    void create_정상() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        Comment comment = Comment.of("댓글 내용", post, user);
        ReflectionTestUtils.setField(comment, "id", 1L);
        given(postRepository.findOrThrow(1L)).willReturn(post);
        given(userRepository.findOrThrow(1L)).willReturn(user);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        // when
        CommentDetailResponse response = commentService.create(1L, 1L, new CreateCommentRequest("댓글 내용"));

        // then
        assertThat(response.content()).isEqualTo("댓글 내용");
        assertThat(response.nickname()).isEqualTo("닉네임");
    }

    @Test
    void create_없는_게시글_예외() {
        // given
        given(postRepository.findOrThrow(99L)).willThrow(new PostNotFoundException());

        // when & then
        assertThatThrownBy(() -> commentService.create(99L, 1L, new CreateCommentRequest("내용")))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void create_없는_사용자_예외() {
        // given
        User user = createUser(1L, "닉네임");
        Post post = createPost(user);
        given(postRepository.findOrThrow(1L)).willReturn(post);
        given(userRepository.findOrThrow(99L)).willThrow(new UserNotFoundException());

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
        given(commentRepository.findOrThrow(1L)).willReturn(comment);

        // when
        commentService.update(1L, 1L, new UpdateCommentRequest("수정내용"));

        // then
        assertThat(comment.getContent()).isEqualTo("수정내용");
    }

    @Test
    void update_없는_댓글_예외() {
        // given
        given(commentRepository.findOrThrow(99L)).willThrow(new CommentNotFoundException());

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
        given(commentRepository.findOrThrow(1L)).willReturn(comment);

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
        given(commentRepository.findOrThrow(1L)).willReturn(comment);

        // when
        commentService.delete(1L, 1L);

        // then
        verify(commentRepository).delete(comment);
    }

    @Test
    void delete_없는_댓글_예외() {
        // given
        given(commentRepository.findOrThrow(99L)).willThrow(new CommentNotFoundException());

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
        given(commentRepository.findOrThrow(1L)).willReturn(comment);

        // when & then
        assertThatThrownBy(() -> commentService.delete(1L, 2L))
                .isInstanceOf(CommentAccessDeniedException.class);
    }
}
