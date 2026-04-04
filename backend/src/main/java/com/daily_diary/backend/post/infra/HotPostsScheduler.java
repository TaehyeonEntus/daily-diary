package com.daily_diary.backend.post.infra;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HotPostsScheduler {

    private final PostQueryRepository postQueryRepository;
    private final List<Long> hotPostsCache;

    @Scheduled(cron = "0 0 * * * *")
    public void reload() {
        List<Long> fresh = postQueryRepository.findHotPosts();
        hotPostsCache.clear();
        hotPostsCache.addAll(fresh);
    }
}
