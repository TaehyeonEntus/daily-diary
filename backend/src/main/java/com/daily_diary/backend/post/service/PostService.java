package com.daily_diary.backend.post.service;

import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.entity.PostLike;
import com.daily_diary.backend.post.exception.LikeAlreadyExistsException;
import com.daily_diary.backend.post.exception.LikeNotFoundException;
import com.daily_diary.backend.post.exception.PostAccessDeniedException;
import com.daily_diary.backend.post.infra.PostLikeRepository;
import com.daily_diary.backend.post.infra.PostQueryRepository;
import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.post.web.*;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final List<Long> hotPostsCache;

    @Transactional
    public PostDetailResponse create(Long userId, CreatePostRequest request) {
        User user = userRepository.findOrThrow(userId);
        Post post = postRepository.save(Post.of(request.title(), request.content(), user));

        return PostDetailResponse.from(post, false);
    }

    @Transactional
    public PostDetailResponse get(Long postId, Long userId) {
        postRepository.increaseViewCount(postId);
        return postQueryRepository.findPostDetail(postId, userId);
    }

    public PostPageResponse getList(PostSearchCondition condition, OrderType orderType, int page, int size) {
        return PostPageResponse.from(postQueryRepository.getPage(condition, orderType, PageRequest.of(page, size)));
    }

    public PostListResponse getHotList() {
        return PostListResponse.from(postQueryRepository.findSummariesByIds(hotPostsCache));
    }

    @Transactional
    public void update(Long userId, Long postId, UpdatePostRequest request) {
        Post post = postRepository.findOrThrow(postId);

        validatePostOwner(post, userId);

        post.changeTitle(request.title());
        post.changeContent(request.content());
    }

    @Transactional
    public void delete(Long userId, Long postId) {
        Post post = postRepository.findOrThrow(postId);

        validatePostOwner(post, userId);

        postRepository.delete(post);
    }

    @Transactional
    public void like(Long userId, Long postId) {
        Post post = postRepository.getReferenceById(postId);
        User user = userRepository.getReferenceById(userId);

        validateNotAlreadyLiked(postId, userId);

        postLikeRepository.save(PostLike.of(post, user));
        postRepository.increaseLikeCount(postId);
    }

    @Transactional
    public void unlike(Long userId, Long postId) {
        postLikeRepository.delete(postLikeRepository.findByPostIdAndUserId(postId, userId).orElseThrow(LikeNotFoundException::new));
        postRepository.decreaseLikeCount(postId);
    }

    // ─── private ──────────────────────────────────────────────────────────────

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
