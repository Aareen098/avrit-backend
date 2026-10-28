package com.hireflow.auth;

import com.hireflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

// One link sent by email: "reset my password" or "accept the invite and set a password".
// Only the SHA-256 hash is stored, so a copy of the database cannot be used to take over accounts.
@Entity
@Table(name = "password_tokens")
public class PasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    protected PasswordToken() {
        // required by JPA
    }

    public PasswordToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getTokenHash() { return tokenHash; }
}