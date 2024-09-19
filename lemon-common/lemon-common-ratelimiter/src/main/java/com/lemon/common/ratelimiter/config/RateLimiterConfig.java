package com.lemon.common.ratelimiter.config;

import com.lemon.common.ratelimiter.aspectj.RateLimiterAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConfiguration;

@AutoConfiguration(after = RedisConfiguration.class)
public class RateLimiterConfig {
    /**
     * 注册 Bean
     */
    @Bean
    public RateLimiterAspect rateLimiterAspect() {
        return new RateLimiterAspect();
    }
}
