package com.hireflow.usage;

import com.hireflow.application.ApplicationRepository;
import com.hireflow.billing.BillingService;
import com.hireflow.billing.Plan;
import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.usage.CompanyUsageDto.Month;
import com.hireflow.usage.CompanyUsageDto.Now;
import com.hireflow.usage.CompanyUsageDto.PlanView;
import com.hireflow.user.UserRepository;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// A company's own plan, usage and invoices. The company id always comes from the login (never from the request),
// so one company can never read another company's numbers. It asks the database only about that one company,
// in a handful of grouped queries (not one query per month).
@Service
public class CompanyUsageService {

    private static final int MONTHS = 6;

    private final CompanyRepository companies;
    private final UserRepository users;
    private final UsageEventRepository usageEvents;
    private final ApplicationRepository applications;
    private final BillingService billing;

    public CompanyUsageService(CompanyRepository companies, UserRepository users, UsageEventRepository usageEvents,
                               ApplicationRepository applications, BillingService billing) {
        this.companies = companies;
        this.users = users;
        this.usageEvents = usageEvents;
        this.applications = applications;
        this.billing = billing;
    }

    @Transactional(readOnly = true)
    public CompanyUsageDto get(Long companyId) {
        Company company = companies.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
        Plan plan = Plan.of(company.getPlanCode());
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        YearMonth first = now.minusMonths(MONTHS - 1);

        // month ("2026-11") -> [aiCalls, tokens, applications, emails]
        Map<String, long[]> sums = new HashMap<>();
        for (Object[] r : usageEvents.monthlyForCompany(companyId, UsageReport.start(first))) {
            long[] s = sums.computeIfAbsent((String) r[0], k -> new long[4]);
            String kind = (String) r[1];
            long units = ((Number) r[2]).longValue();
            if (kind.equals(UsageKind.LLM_SCREENING) || kind.equals(UsageKind.LLM_ASSISTANT)) {
                s[0] += ((Number) r[3]).longValue();
                s[1] += units;
            } else if (kind.equals(UsageKind.EMAIL)) {
                s[3] += units;
            }
        }
        for (Object[] r : applications.applicationsPerMonthForCompany(companyId, first.atDay(1))) {
            sums.computeIfAbsent((String) r[0], k -> new long[4])[2] = ((Number) r[1]).longValue();
        }

        List<Month> history = new ArrayList<>();
        for (int i = 0; i < MONTHS; i++) {
            YearMonth m = first.plusMonths(i);
            long[] s = sums.getOrDefault(m.toString(), new long[4]);
            history.add(new Month(m.toString(), s[0], s[1], s[2], s[3]));
        }
        long[] cur = sums.getOrDefault(now.toString(), new long[4]);
        long seats = users.findByCompany_IdOrderByIdAsc(companyId).size();
        return new CompanyUsageDto(
                new PlanView(plan.name(), plan.label(), plan.priceInr(), plan.seats(), plan.applicationsIncluded(), plan.aiCallsIncluded()),
                new Now(cur[0], cur[1], cur[2], seats, usageEvents.storageForCompany(companyId), cur[3]),
                history, billing.forCompany(companyId));
    }
}