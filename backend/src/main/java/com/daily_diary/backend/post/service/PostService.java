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
import com.daily_diary.backend.post.web.UpdatePostRequest;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.UserNotFoundException;
import com.daily_diary.backend.user.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public void create(Long userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        postRepository.save(Post.of(request.title(), request.content(), user));
    }

    @Transactional
    public PostDetailResponse getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        postRepository.increaseViewCount(postId);

        boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        return PostDetailResponse.from(post, post.getViewCount() + 1, likedByMe);
    }

    public PostListResponse search(PostSearchCondition condition, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PostListResponse.from(postQueryRepository.search(condition, pageable));
    }

    @Transactional
    public PostDetailResponse update(Long userId, Long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        validatePostOwner(post, userId);

        post.changeTitle(request.title());
        post.changeContent(request.content());

        boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        return PostDetailResponse.from(post, likedByMe);
    }

    @Transactional
    public void delete(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        validatePostOwner(post, userId);

        postRepository.delete(post);
    }

    @Transactional
    public void like(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        validateNotAlreadyLiked(postId, userId);

        postLikeRepository.save(PostLike.of(post, user));
        postRepository.increaseLikeCount(postId);
    }

    @Transactional
    public void unlike(Long userId, Long postId) {
        PostLike postLike = postLikeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(LikeNotFoundException::new);

        postLikeRepository.delete(postLike);
        postRepository.decreaseLikeCount(postId);
    }

    private void validatePostOwner(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new PostAccessDeniedException();
        }
    }

    private void validateNotAlreadyLiked(Long postId, Long userId) {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new LikeAlreadyExistsException();
        }
    }
}
