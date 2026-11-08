package com.hireflow.billing;

import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.usage.UsageReport;
import com.hireflow.usage.UsageReport.CompanyUsage;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Turns a month of measured usage into invoices. There is no payment gateway: the platform owner generates the
// month's invoices and marks each one paid by hand. Generating twice never creates duplicates.
// A month is invoiced on the plan the company had at the END of that month (see PlanHistory).
@Service
public class BillingService {

    private final InvoiceRepository invoices;
    private final CompanyRepository companies;
    private final UsageReport usageReport;
    private final PlanHistory planHistory;

    public BillingService(InvoiceRepository invoices, CompanyRepository companies, UsageReport usageReport, PlanHistory planHistory) {
        this.invoices = invoices;
        this.companies = companies;
        this.usageReport = usageReport;
        this.planHistory = planHistory;
    }

    // The invoice for one company and month, on the company's current plan. Does not save anything.
    public static Invoice calculate(Company company, String period, CompanyUsage usage) {
        return calculate(company, Plan.of(company.getPlanCode()), period, usage);
    }

    // Same, for a plan that is given (the plan the company had during that month).
    public static Invoice calculate(Company company, Plan plan, String period, CompanyUsage usage) {
        long extraAi = Math.max(0, usage.aiCalls() - plan.aiCallsIncluded());
        long extraApps = Math.max(0, usage.applications() - plan.applicationsIncluded());
        long overage = extraAi * Plan.EXTRA_AI_CALL_INR + extraApps * Plan.EXTRA_APPLICATION_INR;
        return new Invoice(company.getId(), period, plan.name(), plan.priceInr(), overage, usage.aiCalls(),
                plan.aiCallsIncluded(), usage.applications(), plan.applicationsIncluded(), usage.llmTokens());
    }

    // Creates the invoices of one finished or running month for every company that owes something.
    // Companies that joined later than that month, and companies owing 0, are skipped.
    @Transactional
    public int generate(String period) {
        YearMonth month = parse(period);
        if (month.isAfter(YearMonth.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That month has not started yet");
        }
        Map<Long, CompanyUsage> usage = usageReport.forMonth(month);
        // The plan in force at the end of that month (or right now, for the month that is still running).
        Instant monthEnd = UsageReport.start(month.plusMonths(1)).minusSeconds(1);
        Instant moment = monthEnd.isAfter(Instant.now()) ? Instant.now() : monthEnd;
        int created = 0;
        for (Company c : companies.findAll()) {
            if (YearMonth.from(c.getCreatedAt()).isAfter(month)) continue;
            if (invoices.existsByCompanyIdAndPeriod(c.getId(), period)) continue;
            Invoice invoice = calculate(c, planHistory.planAt(c, moment), period,
                    usage.getOrDefault(c.getId(), CompanyUsage.NONE));
            if (invoice.getTotalAmount() == 0) continue;
            invoices.save(invoice);
            created++;
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> list(String period) {
        List<Invoice> list = period == null || period.isBlank()
                ? invoices.findTop200ByOrderByIdDesc()
                : invoices.findByPeriodOrderByIdDesc(parse(period).toString());
        return dtos(list);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> forCompany(Long companyId) {
        String name = companies.findById(companyId).map(Company::getName).orElse("Deleted company");
        return invoices.findByCompanyIdOrderByPeriodDesc(companyId).stream().map(i -> InvoiceDto.from(i, name)).toList();
    }

    @Transactional
    public InvoiceDto setStatus(Long id, String status) {
        Invoice invoice = invoices.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (Invoice.PAID.equals(status)) {
            if (Invoice.VOID.equals(invoice.getStatus())) throw new ApiException(HttpStatus.BAD_REQUEST, "A void invoice cannot be paid");
            invoice.markPaid();
        } else if (Invoice.VOID.equals(status)) {
            if (Invoice.PAID.equals(invoice.getStatus())) throw new ApiException(HttpStatus.BAD_REQUEST, "A paid invoice cannot be voided");
            invoice.markVoid();
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status must be PAID or VOID");
        }
        return dtos(List.of(invoices.save(invoice))).get(0);
    }

    private List<InvoiceDto> dtos(List<Invoice> list) {
        Map<Long, String> names = new HashMap<>();
        companies.findAll().forEach(c -> names.put(c.getId(), c.getName()));
        return list.stream().map(i -> InvoiceDto.from(i, names.getOrDefault(i.getCompanyId(), "Deleted company"))).toList();
    }

    private static YearMonth parse(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must look like 2026-10");
        }
    }
}