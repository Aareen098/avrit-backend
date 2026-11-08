package com.hireflow.usage;

import com.hireflow.application.ApplicationRepository;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// How much each company used in one calendar month (UTC). The single place where "usage" is calculated,
// so the company list, the company page and the invoices always agree.
@Service
public class UsageReport {

    public record CompanyUsage(long aiCalls, long llmTokens, long applications) {
        public static final CompanyUsage NONE = new CompanyUsage(0, 0, 0);
    }

    private final UsageEventRepository events;
    private final ApplicationRepository applications;

    public UsageReport(UsageEventRepository events, ApplicationRepository applications) {
        this.events = events;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public Map<Long, CompanyUsage> forMonth(YearMonth month) {
        Map<Long, long[]> sums = new HashMap<>();   // companyId -> [aiCalls, tokens, applications]
        for (Object[] r : events.byCompanyAndKind(start(month), start(month.plusMonths(1)))) {
            String kind = (String) r[1];
            if (!kind.equals(UsageKind.LLM_SCREENING) && !kind.equals(UsageKind.LLM_ASSISTANT)) continue;
            long[] s = sums.computeIfAbsent(((Number) r[0]).longValue(), k -> new long[3]);
            s[0] += ((Number) r[3]).longValue();
            s[1] += ((Number) r[2]).longValue();
        }
        for (Object[] r : applications.countPerCompanyBetween(month.atDay(1), month.atEndOfMonth())) {
            sums.computeIfAbsent(((Number) r[0]).longValue(), k -> new long[3])[2] = ((Number) r[1]).longValue();
        }
        Map<Long, CompanyUsage> result = new HashMap<>();
        sums.forEach((id, s) -> result.put(id, new CompanyUsage(s[0], s[1], s[2])));
        return result;
    }

    public static java.time.Instant start(YearMonth month) {
        return month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}