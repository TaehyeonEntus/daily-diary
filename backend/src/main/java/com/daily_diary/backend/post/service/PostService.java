package com.daily_diary.backend.post.service;

import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.exception.PostAccessDeniedException;
import com.daily_diary.backend.post.exception.PostNotFoundException;
import com.daily_diary.backend.post.infra.PostRepository;
import com.daily_diary.backend.post.web.CreatePostRequest;
import com.daily_diary.backend.post.web.PostListResponse;
import com.daily_diary.backend.post.web.PostResponse;
import com.daily_diary.backend.post.web.PostSummaryResponse;
import com.daily_diary.backend.post.web.UpdatePostRequest;
import com.daily_diary.backend.user.exception.UserNotFoundException;
import com.daily_diary.backend.user.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostListResponse list(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PostListResponse.from(postRepository.findAll(pageable).map(PostSummaryResponse::from));
    }

    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse create(Long userId, CreatePostRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        Post post = Post.of(request.title(), request.content(), user);
        postRepository.save(post);
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse update(Long userId, Long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        if (!post.getUser().getId().equals(userId)) {
            throw new PostAccessDeniedException("게시글을 수정할 권한이 없습니다.");
        }
        post.changeTitle(request.title());
        post.changeContent(request.content());
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        if (!post.getUser().getId().equals(userId)) {
            throw new PostAccessDeniedException("게시글을 삭제할 권한이 없습니다.");
        }
        postRepository.delete(post);
    }
}
