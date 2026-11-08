package com.hireflow.platform;

import com.hireflow.application.ApplicationRepository;
import com.hireflow.billing.BillingService;
import com.hireflow.billing.InvoiceRepository;
import com.hireflow.billing.Plan;
import com.hireflow.billing.PlanHistory;
import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.job.JobRepository;
import com.hireflow.platform.PlatformDtos.CompanyDetail;
import com.hireflow.platform.PlatformDtos.CompanyRow;
import com.hireflow.platform.PlatformDtos.MonthPoint;
import com.hireflow.platform.PlatformDtos.Overview;
import com.hireflow.platform.PlatformDtos.PlanInfo;
import com.hireflow.platform.PlatformDtos.Slice;
import com.hireflow.platform.PlatformDtos.TokenPoint;
import com.hireflow.platform.PlatformDtos.Totals;
import com.hireflow.platform.PlatformDtos.UsagePoint;
import com.hireflow.usage.UsageEventRepository;
import com.hireflow.usage.UsageKind;
import com.hireflow.usage.UsageReport;
import com.hireflow.usage.UsageReport.CompanyUsage;
import com.hireflow.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The platform owner's view across ALL companies, limited on purpose to USAGE and BILLING:
// seats, open jobs, how many applications were processed (a count), AI calls and tokens, emails, storage, plan, invoices.
// It never reads candidates, applicants, interviews or the names of a company's team.
// Only PlatformController calls this, and only for the owner.
@Service
public class PlatformService {

    private static final Logger log = LoggerFactory.getLogger(PlatformService.class);
    private static final int MONTHS = 6;

    private final CompanyRepository companies;
    private final UserRepository users;
    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final UsageEventRepository usageEvents;
    private final UsageReport usageReport;
    private final InvoiceRepository invoices;
    private final BillingService billing;
    private final SuspendedCompanies suspendedCompanies;
    private final PlanHistory planHistory;
    private final long emailDailyLimit;

    public PlatformService(CompanyRepository companies, UserRepository users, JobRepository jobs,
                           ApplicationRepository applications, UsageEventRepository usageEvents,
                           UsageReport usageReport, InvoiceRepository invoices, BillingService billing,
                           SuspendedCompanies suspendedCompanies, PlanHistory planHistory,
                           @Value("${app.mail.daily-limit:300}") long emailDailyLimit) {
        this.companies = companies;
        this.users = users;
        this.jobs = jobs;
        this.applications = applications;
        this.usageEvents = usageEvents;
        this.usageReport = usageReport;
        this.invoices = invoices;
        this.billing = billing;
        this.suspendedCompanies = suspendedCompanies;
        this.planHistory = planHistory;
        this.emailDailyLimit = emailDailyLimit;
    }

    // ---- overview ----

    @Transactional(readOnly = true)
    public Overview overview() {
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        List<CompanyRow> rows = rows(now);

        long suspended = rows.stream().filter(CompanyRow::suspended).count();
        long paying = rows.stream().filter(r -> !r.suspended() && Plan.of(r.planCode()) != Plan.FREE).count();
        long expected = rows.stream().filter(r -> !r.suspended()).mapToLong(r -> Plan.of(r.planCode()).priceInr()).sum();

        Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long emailsToday = emailCount(today, today.plusSeconds(86_400));
        long emailsMonth = emailCount(UsageReport.start(now), UsageReport.start(now.plusMonths(1)));

        Totals totals = new Totals(rows.size(), rows.size() - suspended, suspended, paying, expected,
                rows.stream().mapToLong(CompanyRow::applicationsThisMonth).sum(),
                rows.stream().mapToLong(CompanyRow::aiCallsThisMonth).sum(),
                rows.stream().mapToLong(CompanyRow::llmTokensThisMonth).sum(),
                emailsToday, emailsMonth, emailDailyLimit, usageEvents.storageTotal());

        YearMonth first = now.minusMonths(MONTHS - 1);
        List<CompanyRow> top = rows.stream()
                .sorted(Comparator.comparingLong(CompanyRow::llmTokensThisMonth).reversed())
                .limit(5).toList();
        Map<String, Long> mix = new LinkedHashMap<>();
        for (Plan p : Plan.values()) mix.put(p.name(), 0L);
        rows.forEach(r -> mix.merge(Plan.of(r.planCode()).name(), 1L, Long::sum));

        return new Overview(totals, tokensByMonth(first),
                revenueByMonth(first),
                counts(first, companies.createdSince(first.atDay(1)).stream().map(YearMonth::from).toList()),
                mix.entrySet().stream().map(e -> new Slice(e.getKey(), e.getValue())).toList(), top);
    }

    // ---- companies ----

    @Transactional(readOnly = true)
    public List<CompanyRow> companies() {
        return rows(YearMonth.now(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public CompanyDetail company(Long id) {
        Company c = load(id);
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        CompanyRow row = rows(now).stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
        List<UsagePoint> history = new java.util.ArrayList<>();
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth m = now.minusMonths(i);
            CompanyUsage u = usageReport.forMonth(m).getOrDefault(id, CompanyUsage.NONE);
            history.add(new UsagePoint(m.toString(), u.aiCalls(), u.llmTokens(), u.applications()));
        }
        return new CompanyDetail(row, c.getWebsite(), c.getContactEmail(), planInfo(Plan.of(c.getPlanCode())),
                history, billing.forCompany(id));
    }

    @Transactional
    public CompanyRow setSuspended(Long id, boolean suspended, String byEmail) {
        Company c = load(id);
        if (c.isSuspended() != suspended) {
            c.setSuspended(suspended);
            companies.save(c);
            suspendedCompanies.refresh();
            log.info("Platform owner {} {} company {} ({})", byEmail, suspended ? "suspended" : "reactivated",
                    c.getName(), c.getId());
        }
        return row(id);
    }

    @Transactional
    public CompanyRow setPlan(Long id, String planCode, String byEmail) {
        if (!Plan.isValid(planCode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown plan. Use FREE, PRO or BUSINESS");
        }
        Company c = load(id);
        if (!c.getPlanCode().equals(planCode)) {
            planHistory.record(c.getId(), c.getPlanCode(), planCode, byEmail);   // remember the old plan for invoicing
            c.setPlanCode(planCode);
            companies.save(c);
            log.info("Platform owner {} moved company {} ({}) to plan {}", byEmail, c.getName(), c.getId(), planCode);
        }
        return row(id);
    }

    public List<PlanInfo> plans() {
        return Arrays.stream(Plan.values()).map(PlatformService::planInfo).toList();
    }

    // ---- helpers ----

    private CompanyRow row(Long id) {
        return rows(YearMonth.now(ZoneOffset.UTC)).stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    private Company load(Long id) {
        return companies.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private static PlanInfo planInfo(Plan p) {
        return new PlanInfo(p.name(), p.label(), p.priceInr(), p.seats(), p.applicationsIncluded(), p.aiCallsIncluded());
    }

    // One row per company with this month's usage. A few grouped queries instead of one query per company.
    private List<CompanyRow> rows(YearMonth month) {
        Map<Long, Long> seats = longs(users.countStaffPerCompany());
        Map<Long, Long> openJobs = longs(jobs.openPerCompany());
        Map<Long, Long> storage = longs(usageEvents.storagePerCompany());
        Map<Long, CompanyUsage> usage = usageReport.forMonth(month);
        Map<Long, Long> emails = new HashMap<>();
        for (Object[] r : usageEvents.byCompanyAndKind(UsageReport.start(month), UsageReport.start(month.plusMonths(1)))) {
            if (UsageKind.EMAIL.equals(r[1])) emails.put(((Number) r[0]).longValue(), ((Number) r[2]).longValue());
        }
        Map<Long, LocalDate> lastActive = new HashMap<>();
        for (Object[] r : applications.lastActivityPerCompany()) lastActive.put(((Number) r[0]).longValue(), (LocalDate) r[1]);

        return companies.findAll().stream().map(c -> {
            CompanyUsage u = usage.getOrDefault(c.getId(), CompanyUsage.NONE);
            return new CompanyRow(c.getId(), c.getName(), c.getSlug(), c.getIndustry(), c.getSize(), c.getPlanCode(),
                    c.isSuspended(), c.getCreatedAt(), seats.getOrDefault(c.getId(), 0L),
                    openJobs.getOrDefault(c.getId(), 0L), u.applications(), u.aiCalls(), u.llmTokens(),
                    emails.getOrDefault(c.getId(), 0L), storage.getOrDefault(c.getId(), 0L), lastActive.get(c.getId()));
        }).sorted(Comparator.comparing(CompanyRow::createdAt).reversed()).toList();
    }

    private long emailCount(Instant from, Instant to) {
        return usageEvents.byKind(from, to).stream().filter(r -> UsageKind.EMAIL.equals(r[0]))
                .mapToLong(r -> ((Number) r[1]).longValue()).sum();
    }

    // LLM tokens per month, split by who used them.
    private List<TokenPoint> tokensByMonth(YearMonth first) {
        Map<YearMonth, long[]> sums = new LinkedHashMap<>();
        for (int i = 0; i < MONTHS; i++) sums.put(first.plusMonths(i), new long[3]);
        for (Object[] r : usageEvents.since(UsageReport.start(first))) {
            long[] s = sums.get(YearMonth.from(((Instant) r[0]).atZone(ZoneOffset.UTC)));
            if (s == null) continue;
            long units = ((Number) r[2]).longValue();
            switch ((String) r[1]) {
                case UsageKind.LLM_SCREENING -> s[0] += units;
                case UsageKind.LLM_ASSISTANT -> s[1] += units;
                case UsageKind.LLM_CANDIDATE -> s[2] += units;
                default -> { }
            }
        }
        return sums.entrySet().stream()
                .map(e -> new TokenPoint(e.getKey().toString(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();
    }

    // Money invoiced per month (void invoices excluded).
    private List<MonthPoint> revenueByMonth(YearMonth first) {
        Map<String, Long> byPeriod = new HashMap<>();
        for (Object[] r : invoices.revenueSince(first.toString())) byPeriod.put((String) r[0], ((Number) r[1]).longValue());
        return java.util.stream.IntStream.range(0, MONTHS)
                .mapToObj(i -> first.plusMonths(i).toString())
                .map(m -> new MonthPoint(m, byPeriod.getOrDefault(m, 0L))).toList();
    }

    private static List<MonthPoint> counts(YearMonth first, List<YearMonth> events) {
        Map<YearMonth, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < MONTHS; i++) counts.put(first.plusMonths(i), 0L);
        for (YearMonth m : events) counts.computeIfPresent(m, (k, v) -> v + 1);
        return counts.entrySet().stream().map(e -> new MonthPoint(e.getKey().toString(), e.getValue())).toList();
    }

    private static Map<Long, Long> longs(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] r : rows) map.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        return map;
    }
}