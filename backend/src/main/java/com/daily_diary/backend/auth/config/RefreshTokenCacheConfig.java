package com.daily_diary.backend.auth.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RefreshTokenCacheConfig {

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Bean
    public Cache<Long, String> refreshTokenCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(refreshTokenExpiry, TimeUnit.MILLISECONDS)
                .build();
    }
}
