package com.daily_diary.backend.post.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class HotPostsCacheConfig {

    @Bean
    public AtomicReference<List<Long>> hotPostsCache() {
        return new AtomicReference<>(List.of());
    }
}
