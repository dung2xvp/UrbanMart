package com.haui.UrbanMart.service;

import com.haui.UrbanMart.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiRateLimitService {

    private static final int MAX_RECIPE_SUGGESTIONS_PER_MINUTE = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void checkRecipeSuggestionLimit(UUID userId) {
        String key = "ai:recipe-suggestions:%s".formatted(userId);
        Long requestCount = redisTemplate.execute(
                INCREMENT_WITH_EXPIRY,
                List.of(key),
                Long.toString(WINDOW.toSeconds())
        );
        if (requestCount == null) {
            throw new IllegalStateException("Redis không trả về kết quả giới hạn AI");
        }
        if (requestCount > MAX_RECIPE_SUGGESTIONS_PER_MINUTE) {
            throw new TooManyRequestsException(
                    "Bạn đã vượt quá 5 yêu cầu gợi ý món ăn trong một phút"
            );
        }
    }
}
