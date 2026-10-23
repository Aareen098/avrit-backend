package com.hireflow.auth;

import com.hireflow.common.ApiException;
import com.hireflow.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Creates and checks the one-time links used for "forgot password" and for invites.
@Service
public class PasswordTokenService {

    private final SecureRandom random = new SecureRandom();
    private final PasswordTokenRepository tokens;

    public PasswordTokenService(PasswordTokenRepository tokens) {
        this.tokens = tokens;
    }

    // Returns the secret to put in the link. Any older link of the same user stops working.
    @Transactional
    public String issue(User user, Duration lifetime) {
        tokens.deleteExpired(Instant.now());
        tokens.deleteForUser(user.getId());
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new PasswordToken(user, hash(secret), Instant.now().plus(lifetime)));
        return secret;
    }

    // Accepts a valid link once: returns its user and removes every link of that user.
    @Transactional
    public User consume(String secret) {
        PasswordToken token = tokens.findByTokenHash(hash(secret.trim()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "This link is invalid or has expired. Please request a new one."));
        User user = token.getUser();
        tokens.deleteForUser(user.getId());
        return user;
    }

    private static String hash(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}