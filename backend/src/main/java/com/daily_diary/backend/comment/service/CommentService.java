package com.daily_diary.backend.comment.service;

import com.daily_diary.backend.comment.entity.Comment;
import com.daily_diary.backend.comment.exception.CommentAccessDeniedException;
import com.daily_diary.backend.comment.infra.CommentQueryRepository;
import com.daily_diary.backend.comment.infra.CommentRepository;
import com.daily_diary.backend.comment.web.CommentDetailResponse;
import com.daily_diary.backend.comment.web.CommentPageResponse;
import com.daily_diary.backend.comment.web.CreateCommentRequest;
import com.daily_diary.backend.comment.web.UpdateCommentRequest;
import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.exception.PostNotFoundException;
import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDetailResponse create(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findOrThrow(postId);
        User user = userRepository.findOrThrow(userId);

        Comment comment = commentRepository.save(Comment.of(request.content(), post, user));
        return CommentDetailResponse.from(comment);
    }

    public CommentPageResponse getPage(Long postId, int page, int size) {
        validatePostExists(postId);

        return CommentPageResponse.from(commentQueryRepository.getPage(postId, PageRequest.of(page, size)));
    }

    @Transactional
    public void update(Long commentId, Long userId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findOrThrow(commentId);

        validateCommentOwner(comment, userId);
        comment.changeContent(request.content());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = commentRepository.findOrThrow(commentId);

        validateCommentOwner(comment, userId);
        commentRepository.delete(comment);
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private void validatePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }
    }

    private void validateCommentOwner(Comment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentAccessDeniedException();
        }
    }
}
