package com.hireflow.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

// One measured use of a resource. Holds numbers only: no names, no CV text, no prompts.
@Entity
@Table(name = "usage_events", indexes = {
        @Index(columnList = "company_id, created_at"),
        @Index(columnList = "kind, created_at")})
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null = platform-level use that belongs to no company (candidate AI, emails)
    @Column(name = "company_id")
    private Long companyId;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private long units;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected UsageEvent() {
        // required by JPA
    }

    public UsageEvent(Long companyId, String kind, long units) {
        this.companyId = companyId;
        this.kind = kind;
        this.units = units;
    }
}