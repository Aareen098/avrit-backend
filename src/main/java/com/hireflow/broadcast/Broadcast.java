package com.hireflow.broadcast;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// The history of what the platform owner sent. It stores the message and who it reached (as a number),
// never the list of people.
@Entity
@Table(name = "broadcasts")
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // USER, COMPANY, ALL_STAFF or ALL_CANDIDATES
    @Column(nullable = false)
    private String audience;

    // Readable target, such as "Acme Corp team" or "someone@mail.com"
    @Column(nullable = false)
    private String audienceLabel;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    private boolean inApp;
    private boolean email;
    private int inAppCount;
    private int emailCount;

    @Column(nullable = false)
    private String sentBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Broadcast() {
        // required by JPA
    }

    public Broadcast(String audience, String audienceLabel, String title, String message, boolean inApp, boolean email,
                     int inAppCount, int emailCount, String sentBy) {
        this.audience = audience;
        this.audienceLabel = audienceLabel;
        this.title = title;
        this.message = message;
        this.inApp = inApp;
        this.email = email;
        this.inAppCount = inAppCount;
        this.emailCount = emailCount;
        this.sentBy = sentBy;
    }

    public Long getId() { return id; }
    public String getAudience() { return audience; }
    public String getAudienceLabel() { return audienceLabel; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isInApp() { return inApp; }
    public boolean isEmail() { return email; }
    public int getInAppCount() { return inAppCount; }
    public int getEmailCount() { return emailCount; }
    public Instant getCreatedAt() { return createdAt; }
}