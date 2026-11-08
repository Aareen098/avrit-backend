package com.hireflow.usage;

import com.hireflow.billing.InvoiceDto;
import java.util.List;

// What a company's Admin sees about its own use of the platform. Numbers only, and only for their own company.
public record CompanyUsageDto(PlanView plan, Now now, List<Month> history, List<InvoiceDto> invoices) {

    public record PlanView(String code, String label, long priceInr, int seats, long applicationsIncluded, long aiCallsIncluded) {
    }

    // This month so far
    public record Now(long aiCalls, long llmTokens, long applications, long seats, long storageBytes, long emails) {
    }

    // One month of the last six ("2026-11")
    public record Month(String month, long aiCalls, long llmTokens, long applications, long emails) {
    }
}