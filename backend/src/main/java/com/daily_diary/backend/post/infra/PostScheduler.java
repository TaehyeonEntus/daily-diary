package com.daily_diary.backend.post.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class PostScheduler {

    private final PostQueryRepository postQueryRepository;
    private final PostRepository postRepository;
    private final AtomicReference<List<Long>> hotPostsCache;

    @Scheduled(cron = "0 0 * * * *")
    public void reloadHotPosts() {
        hotPostsCache.set(postQueryRepository.findHotPostIds());
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void syncCounts() {
        postRepository.syncLikeCount();
        postRepository.syncCommentCount();
    }
}
