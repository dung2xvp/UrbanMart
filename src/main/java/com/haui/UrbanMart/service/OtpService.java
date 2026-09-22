package com.haui.UrbanMart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpService {
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    private String otpKey(String purpose, String phone) {
        return "otp:%s:%s".formatted(purpose, phone);
    }

    private String cooldownKey(String purpose, String phone) {
        return "otp:cooldown:%s:%s".formatted(purpose, phone);
    }

    public boolean canSend(String purpose, String phone) {
        return Boolean.FALSE.equals(redisTemplate.hasKey(cooldownKey(purpose, phone)));
    }

    public String generateAndStore(String purpose, String phone) {
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        redisTemplate.opsForValue().set(otpKey(purpose, phone), otp, OTP_TTL);
        redisTemplate.opsForValue().set(cooldownKey(purpose, phone), "1", RESEND_COOLDOWN);
        return otp;
    }

    public boolean verify(String purpose, String phone, String inputOtp) {
        String key = otpKey(purpose, phone);
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null || !stored.equals(inputOtp)) {
            return false;
        }

        redisTemplate.delete(key);
        return true;
    }
}
