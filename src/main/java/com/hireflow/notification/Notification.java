package com.hireflow.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

// One message in a person's bell: "New application", "Interview scheduled", a message from the platform...
@Entity
@Table(name = "notifications", indexes = @Index(columnList = "user_id, created_at"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Plain text on purpose (no enum), so new kinds never need a database change.
    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    // A path inside the app, like /candidates/12. Optional.
    private String link;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    private Instant readAt;

    protected Notification() {
        // required by JPA
    }

    public Notification(Long userId, String kind, String title, String body, String link) {
        this.userId = userId;
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.link = link;
    }

    public Long getId() { return id; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }

    public void markRead() {
        if (readAt == null) readAt = Instant.now();
    }
}