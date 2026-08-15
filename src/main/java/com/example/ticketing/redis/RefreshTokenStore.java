package com.example.ticketing.redis;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String PREFIX = "auth:refresh:";
    private final StringRedisTemplate redisTemplate;

    public void save(
            Long userId,
            String refreshToken,
            Duration expiration
    ) {
        redisTemplate.opsForValue().set(
                PREFIX + userId,
                refreshToken,
                expiration
        );
    }

    public String find(Long userId) {
        return redisTemplate.opsForValue()
                .get(PREFIX + userId);
    }

    public void delete(Long userId) {
        redisTemplate.delete(
                PREFIX + userId
        );
    }



}
