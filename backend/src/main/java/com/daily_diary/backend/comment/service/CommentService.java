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
import com.daily_diary.backend.comment.web.CommentSummaryResponse;
import com.daily_diary.backend.comment.web.CreateCommentRequest;
import com.daily_diary.backend.comment.web.UpdateCommentRequest;
import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.exception.PostNotFoundException;
import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.UserNotFoundException;
import com.daily_diary.backend.user.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentListResponse list(Long postId, Long userId, int page, int size) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        Page<Comment> comments = commentRepository.findByPostId(
                postId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))
        );

        Set<Long> likedCommentIds = resolveLikedIds(comments.getContent(), userId);

        Page<CommentSummaryResponse> responsePage = comments.map(comment ->
                CommentSummaryResponse.from(comment, likedCommentIds.contains(comment.getId()))
        );

        return CommentListResponse.from(responsePage);
    }

    @Transactional
    public void create(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        commentRepository.save(Comment.of(request.content(), post, user));
    }

    @Transactional
    public CommentDetailResponse update(Long commentId, Long userId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        validateCommentOwner(comment, userId);

        comment.changeContent(request.content());

        boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        return CommentDetailResponse.from(comment, likedByMe);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        validateCommentOwner(comment, userId);

        commentRepository.delete(comment);
    }

    @Transactional
    public void like(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        validateNotAlreadyLiked(commentId, userId);

        commentLikeRepository.save(CommentLike.of(comment, user));
        commentRepository.increaseLikeCount(commentId);
    }

    @Transactional
    public void unlike(Long commentId, Long userId) {
        CommentLike commentLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
                .orElseThrow(CommentLikeNotFoundException::new);

        commentLikeRepository.delete(commentLike);
        commentRepository.decreaseLikeCount(commentId);
    }

    private void validateNotAlreadyLiked(Long commentId, Long userId) {
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new CommentLikeAlreadyExistsException();
        }
    }

    private void validateCommentOwner(Comment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentAccessDeniedException();
        }
    }

    private Set<Long> resolveLikedIds(List<Comment> comments, Long userId) {
        if (userId == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        return commentLikeRepository.findAllByCommentIdInAndUserId(commentIds, userId)
                .stream()
                .map(like -> like.getComment().getId())
                .collect(Collectors.toSet());
    }
}
