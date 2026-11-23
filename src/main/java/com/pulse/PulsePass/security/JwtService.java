package com.pulse.PulsePass.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final Duration ttl;

    public JwtService(
            @Value("${pulsepass.security.jwt.secret-base64}") String secret,
            @Value("${pulsepass.security.jwt.issuer}") String issuer,
            @Value("${pulsepass.security.jwt.access-token-ttl}") Duration ttl) {
        byte[] bytes = Decoders.BASE64.decode(secret);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT key must have at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.issuer = issuer;
        this.ttl = ttl;
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("JWT TTL must be positive");
        }
    }

    public long expiresIn() {
        return ttl.toSeconds();
    }

    public String issue(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Long verify(String token) {
        var parsed = Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .build().parseSignedClaims(token);
        if (!"HS256".equals(parsed.getHeader().getAlgorithm())) {
            throw new JwtException("Unexpected algorithm");
        }
        Claims c = parsed.getPayload();
        if (c.getSubject() == null
                || c.getSubject().isBlank()
                || c.getExpiration() == null
                || c.getIssuedAt() == null
                || c.getId() == null
                || c.getId().isBlank()
                || !c.getExpiration().after(new Date())) {
            throw new JwtException("Missing required claims");
        }
        try {
            return Long.valueOf(c.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("Invalid subject", e);
        }
    }
}
