package com.hireflow.billing;

import java.time.Instant;

public record InvoiceDto(Long id, String number, Long companyId, String companyName, String period, String planCode,
                         long baseAmount, long overageAmount, long totalAmount, long aiCalls, long aiCallsIncluded,
                         long applications, long applicationsIncluded, long llmTokens, String status,
                         Instant issuedAt, Instant paidAt) {

    public static InvoiceDto from(Invoice i, String companyName) {
        return new InvoiceDto(i.getId(), "AV-" + i.getPeriod().replace("-", "") + "-" + String.format("%04d", i.getId()),
                i.getCompanyId(), companyName, i.getPeriod(), i.getPlanCode(), i.getBaseAmount(), i.getOverageAmount(),
                i.getTotalAmount(), i.getAiCalls(), i.getAiCallsIncluded(), i.getApplications(),
                i.getApplicationsIncluded(), i.getLlmTokens(), i.getStatus(), i.getIssuedAt(), i.getPaidAt());
    }
}