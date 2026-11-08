package com.hireflow.dashboard;

import java.util.List;
import java.util.Map;

public record DashboardDto(
        long openJobs, long total, long newThisWeek,
        Integer avgScore,        // null when nobody has an AI score yet
        long unscored,
        Map<String, Long> stageCounts,
        List<Day> perDay,        // last 30 days, empty days as 0
        List<Bucket> scoreBuckets, List<Bucket> experience, List<Bucket> topSkills,
        List<JobRow> byJob, List<TopMatch> topMatches) {

    public record Day(String date, long value) {}
    public record Bucket(String label, long value) {}
    public record JobRow(String job, long total, Map<String, Long> stages) {}
    public record TopMatch(Long id, String name, String job, String stage, Integer aiScore) {}
}