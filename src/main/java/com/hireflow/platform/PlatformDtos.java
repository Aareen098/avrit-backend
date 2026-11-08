package com.hireflow.platform;

import com.hireflow.billing.InvoiceDto;
import java.time.LocalDate;
import java.util.List;

// Everything the platform owner's screens receive. Deliberately only usage and billing numbers:
// no candidates, no applicants, no interviews, no team members' names.
public final class PlatformDtos {

    private PlatformDtos() {
    }

    public record Totals(long companies, long activeCompanies, long suspendedCompanies, long payingCompanies,
                         long expectedMonthlyRevenue, long applicationsThisMonth, long aiCallsThisMonth,
                         long llmTokensThisMonth, long emailsToday, long emailsThisMonth, long emailDailyLimit,
                         long storageBytes) {
    }

    // month is "2026-11"
    public record MonthPoint(String month, long value) {
    }

    public record TokenPoint(String month, long screening, long assistant, long candidate) {
    }

    public record Slice(String label, long value) {
    }

    public record CompanyRow(Long id, String name, String slug, String industry, String size, String planCode,
                             boolean suspended, LocalDate createdAt, long seats, long openJobs,
                             long applicationsThisMonth, long aiCallsThisMonth, long llmTokensThisMonth, long emailsThisMonth,
                             long storageBytes, LocalDate lastActive) {
    }

    public record Overview(Totals totals, List<TokenPoint> tokensByMonth, List<MonthPoint> revenueByMonth,
                           List<MonthPoint> companiesByMonth, List<Slice> planMix, List<CompanyRow> topCompanies) {
    }

    public record UsagePoint(String month, long aiCalls, long llmTokens, long applications) {
    }

    public record PlanInfo(String code, String label, long priceInr, int seats, long applicationsIncluded,
                           long aiCallsIncluded) {
    }

    public record CompanyDetail(CompanyRow company, String website, String contactEmail, PlanInfo plan,
                                List<UsagePoint> usageByMonth, List<InvoiceDto> invoices) {
    }

    public record SuspensionRequest(boolean suspended) {
    }

    public record PlanRequest(String planCode) {
    }

    public record PeriodRequest(String period) {
    }

    public record InvoiceStatusRequest(String status) {
    }

    public record GenerateResult(int created) {
    }
}