package com.daily_diary.backend.post.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostLikeCountScheduler {

    private final PostLikeRepository postLikeRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sync() {
        postLikeRepository.syncLikeCount();
    }
}
