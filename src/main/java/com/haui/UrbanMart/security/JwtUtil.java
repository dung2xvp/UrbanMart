package com.haui.UrbanMart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String role) {
        return generateToken(userId, role, null);
    }

    public String generateToken(UUID userId, String role, UUID branchId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry);

        if (branchId != null) {
            builder.claim("branchId", branchId.toString());
        }
        return builder.signWith(secretKey).compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public UUID getBranchId(String token) {
        String branchId = parseClaims(token).get("branchId", String.class);
        return branchId != null ? UUID.fromString(branchId) : null;
    }
}
