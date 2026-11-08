package com.hireflow.billing;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "plan_changes", indexes = @Index(columnList = "company_id, changed_at"))
public class PlanChange {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false) private String fromPlan;
    @Column(nullable = false) private String toPlan;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    @Column(nullable = false) private String changedBy;

    protected PlanChange() {}

    public PlanChange(Long companyId, String fromPlan, String toPlan, String changedBy) {
        this.companyId = companyId; this.fromPlan = fromPlan; this.toPlan = toPlan; this.changedBy = changedBy;
    }

    public String getFromPlan() { return fromPlan; }
    public String getToPlan() { return toPlan; }
    public Instant getChangedAt() { return changedAt; }
}