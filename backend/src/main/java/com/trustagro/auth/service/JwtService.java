package com.trustagro.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set APP_JWT_SECRET or app.jwt.secret "
                            + "(local profile supplies a development secret automatically).");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters. Set APP_JWT_SECRET or app.jwt.secret.");
        }
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        return generateToken(email, null, List.of(), List.of());
    }

    public String generateToken(String email, List<String> permissions, List<Long> farmIds) {
        return generateToken(email, null, permissions, farmIds);
    }

    public String generateToken(String email, String role, List<String> permissions, List<Long> farmIds) {
        var builder = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .claim("permissions", permissions)
                .claim("farmIds", farmIds);
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.signWith(getKey(), SignatureAlgorithm.HS256).compact();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getKey()).build()
                .parseClaimsJws(token).getBody();
        Object perms = claims.get("permissions");
        if (perms instanceof List<?>) return (List<String>) perms;
        return List.of();
    }

    public String extractRole(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getKey()).build()
                .parseClaimsJws(token).getBody();
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }

    @SuppressWarnings("unchecked")
    public List<Long> extractFarmIds(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getKey()).build()
                .parseClaimsJws(token).getBody();
        Object ids = claims.get("farmIds");
        if (ids instanceof List<?>) return (List<Long>) ids;
        return List.of();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) &&
                    !Jwts.parserBuilder().setSigningKey(getKey()).build()
                            .parseClaimsJws(token).getBody().getExpiration().before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}
