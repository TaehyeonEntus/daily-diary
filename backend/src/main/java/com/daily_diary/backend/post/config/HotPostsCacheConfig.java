package com.daily_diary.backend.post.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class HotPostsCacheConfig {

    @Bean
    public List<Long> hotPostsCache() {
        return new ArrayList<>();
    }
}
