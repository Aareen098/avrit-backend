package com.hireflow.dashboard;

import com.hireflow.application.ApplicationRepository;
import com.hireflow.application.ApplicationStage;
import com.hireflow.dashboard.DashboardDto.Bucket;
import com.hireflow.dashboard.DashboardDto.Day;
import com.hireflow.dashboard.DashboardDto.JobRow;
import com.hireflow.dashboard.DashboardDto.TopMatch;
import com.hireflow.job.JobRepository;
import com.hireflow.job.JobStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The numbers behind the company dashboard. The database does the adding up in 6 small grouped queries,
// so it stays fast with thousands of candidates, and only a small summary goes to the browser.
// The company id is always the caller's own. Scores here are the stored AI scores.
@Service
public class DashboardService {

    private static final int DAYS = 30;

    private final ApplicationRepository applications;
    private final JobRepository jobs;

    public DashboardService(ApplicationRepository applications, JobRepository jobs) {
        this.applications = applications;
        this.jobs = jobs;
    }

    @Transactional(readOnly = true)
    public DashboardDto summary(Long companyId) {
        LocalDate today = LocalDate.now();

        // One query gives every job with its stages. Stage counts, totals and the per-job rows are added up from it.
        Map<String, Long> stages = emptyStages();
        Map<Long, JobRow> byJob = new LinkedHashMap<>();
        Map<Long, Long> totals = new HashMap<>();
        for (Object[] r : applications.countPerJobAndStage(companyId)) {
            Long jobId = ((Number) r[0]).longValue();
            String stage = r[2].toString();
            long n = ((Number) r[3]).longValue();
            stages.merge(stage, n, Long::sum);
            totals.merge(jobId, n, Long::sum);
            byJob.computeIfAbsent(jobId, k -> new JobRow((String) r[1], 0, emptyStages())).stages().put(stage, n);
        }
        long total = stages.values().stream().mapToLong(Long::longValue).sum();
        List<JobRow> jobRows = byJob.entrySet().stream()
                .map(e -> new JobRow(e.getValue().job(), totals.get(e.getKey()), e.getValue().stages()))
                .sorted((a, b) -> Long.compare(b.total(), a.total())).limit(8).toList();

        // the last 30 days, with 0 for days nobody applied
        Map<LocalDate, Long> perDayRaw = new HashMap<>();
        for (Object[] r : applications.countPerDay(companyId, today.minusDays(DAYS - 1L))) {
            perDayRaw.put((LocalDate) r[0], ((Number) r[1]).longValue());
        }
        List<Day> perDay = new ArrayList<>();
        for (int i = DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            perDay.add(new Day(d.toString(), perDayRaw.getOrDefault(d, 0L)));
        }
        long newThisWeek = perDay.subList(DAYS - 7, DAYS).stream().mapToLong(Day::value).sum();

        // One query gives experience and AI score together (score is null when not scored yet).
        long[] score = new long[4];
        long[] exp = new long[4];
        long scoreSum = 0;
        long scored = 0;
        for (Object[] r : applications.countPerExperienceAndScore(companyId)) {
            int y = ((Number) r[0]).intValue();
            long n = ((Number) r[2]).longValue();
            exp[y <= 1 ? 0 : y <= 4 ? 1 : y <= 9 ? 2 : 3] += n;
            if (r[1] != null) {
                int s = ((Number) r[1]).intValue();
                score[s < 40 ? 0 : s < 65 ? 1 : s < 85 ? 2 : 3] += n;
                scoreSum += (long) s * n;
                scored += n;
            }
        }

        List<Bucket> skills = applications.countPerSkill(companyId).stream()
                .map(r -> new Bucket((String) r[0], ((Number) r[1]).longValue())).toList();

        List<TopMatch> top = applications.topMatches(companyId, PageRequest.of(0, 4)).stream()
                .map(r -> new TopMatch(((Number) r[0]).longValue(), (String) r[1], (String) r[2], r[3].toString(),
                        ((Number) r[4]).intValue()))
                .toList();

        return new DashboardDto(
                jobs.countByCompany_IdAndStatus(companyId, JobStatus.OPEN), total, newThisWeek,
                scored == 0 ? null : (int) Math.round((double) scoreSum / scored), total - scored, stages, perDay,
                List.of(new Bucket("Below 40", score[0]), new Bucket("40 to 64", score[1]), new Bucket("65 to 84", score[2]), new Bucket("85 and above", score[3])),
                List.of(new Bucket("0 to 1 years", exp[0]), new Bucket("2 to 4 years", exp[1]), new Bucket("5 to 9 years", exp[2]), new Bucket("10+ years", exp[3])),
                skills, jobRows, top);
    }

    private static Map<String, Long> emptyStages() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (ApplicationStage s : ApplicationStage.values()) m.put(s.name(), 0L);
        return m;
    }
}