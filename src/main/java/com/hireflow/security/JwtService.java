package com.hireflow.security;

import com.hireflow.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMinutes * 60_000;
    }

    public String generate(User user) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name());
        // Candidates have no company.
        if (user.getCompany() != null) builder.claim("companyId", user.getCompany().getId());
        return builder
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    // Throws JwtException if the token is invalid or expired.
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    // A 24-hour link token for "verify my email". It has no role, so it can never be used to log in.
    public String generateEmailToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("purpose", "verify-email")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 24 * 60 * 60 * 1000L))
                .signWith(key)
                .compact();
    }

    // Returns the email inside a verification token. A normal login token is refused.
    public String parseEmailToken(String token) {
        Claims claims = parse(token);
        if (!"verify-email".equals(claims.get("purpose", String.class))) throw new JwtException("Not an email token");
        return claims.getSubject();
    }
}