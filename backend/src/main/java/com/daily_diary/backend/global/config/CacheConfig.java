package com.daily_diary.backend.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("refreshTokens");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(refreshTokenExpiry, TimeUnit.MILLISECONDS));
        return manager;
    }
}
