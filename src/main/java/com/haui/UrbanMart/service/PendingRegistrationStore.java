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

    private String key(String phone) {
        return "auth:pending-registratrion:" + phone;
    }

    @SneakyThrows
    public void save(String phone, UserCreationRequest request) {
        String json = objectMapper.writeValueAsString(request);
        redisTemplate.opsForValue().set(key(phone), json, TTL);
    }

    @SneakyThrows
    public UserCreationRequest get(String phone) {
        String json = redisTemplate.opsForValue().get(key(phone));
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, UserCreationRequest.class);
    }

    public void delete(String phone) {
        redisTemplate.delete(key(phone));
    }
}
