package com.hireflow.application;

import java.time.LocalDate;
import java.util.List;

// What a candidate may see about one of their own applications. Never add AI scores or notes here.
public record ApplicationDetailDto(String reference, Long jobId, String jobTitle, String company, String location,
                                   String jobType, String description, List<String> skills, String status,
                                   LocalDate appliedAt, String coverNote, String resumeName) {

    public static ApplicationDetailDto from(Application a) {
        var job = a.getJob();
        return new ApplicationDetailDto(a.getReference(), job.getId(), job.getTitle(), job.getCompany().getName(),
                job.getLocation(), job.getType(), job.getDescription(), job.getSkills(), a.getStage().publicStatus(),
                a.getAppliedAt(), a.getCoverNote() == null ? "" : a.getCoverNote(), a.getResumeFileName());
    }
}