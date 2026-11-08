package com.hireflow.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

// One invoice per company per month. Amounts are whole rupees. The usage numbers are kept on the invoice,
// so it still explains itself later, even if prices or plans change.
@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "period"}))
public class Invoice {

    public static final String ISSUED = "ISSUED";
    public static final String PAID = "PAID";
    public static final String VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // "2026-10"
    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private String planCode;

    private long baseAmount;
    private long overageAmount;
    private long totalAmount;

    private long aiCalls;
    private long aiCallsIncluded;
    private long applications;
    private long applicationsIncluded;
    private long llmTokens;

    @Column(nullable = false)
    private String status = ISSUED;

    @Column(nullable = false)
    private Instant issuedAt = Instant.now();

    private Instant paidAt;

    protected Invoice() {
        // required by JPA
    }

    public Invoice(Long companyId, String period, String planCode, long baseAmount, long overageAmount,
                   long aiCalls, long aiCallsIncluded, long applications, long applicationsIncluded, long llmTokens) {
        this.companyId = companyId;
        this.period = period;
        this.planCode = planCode;
        this.baseAmount = baseAmount;
        this.overageAmount = overageAmount;
        this.totalAmount = baseAmount + overageAmount;
        this.aiCalls = aiCalls;
        this.aiCallsIncluded = aiCallsIncluded;
        this.applications = applications;
        this.applicationsIncluded = applicationsIncluded;
        this.llmTokens = llmTokens;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getPeriod() { return period; }
    public String getPlanCode() { return planCode; }
    public long getBaseAmount() { return baseAmount; }
    public long getOverageAmount() { return overageAmount; }
    public long getTotalAmount() { return totalAmount; }
    public long getAiCalls() { return aiCalls; }
    public long getAiCallsIncluded() { return aiCallsIncluded; }
    public long getApplications() { return applications; }
    public long getApplicationsIncluded() { return applicationsIncluded; }
    public long getLlmTokens() { return llmTokens; }
    public String getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getPaidAt() { return paidAt; }

    public void markPaid() {
        this.status = PAID;
        this.paidAt = Instant.now();
    }

    public void markVoid() {
        this.status = VOID;
    }
}