package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.UserCreationRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PendingRegistrationStore {
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private String key(String email) {
        return "auth:pending-registratrion:" + email;
    }

    @SneakyThrows
    public void save(String email, UserCreationRequest request) {
        String json = objectMapper.writeValueAsString(request);
        redisTemplate.opsForValue().set(key(email), json, TTL);
    }

    @SneakyThrows
    public UserCreationRequest get(String email) {
        String json = redisTemplate.opsForValue().get(key(email));
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, UserCreationRequest.class);
    }

    public void delete(String email) {
        redisTemplate.delete(key(email));
    }
}
