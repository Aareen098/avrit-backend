package com.hireflow.job;

import java.time.LocalDate;
import java.util.List;

// What staff see in the Jobs table. "applicants" is the number of applications for the job.
public record JobDto(
        Long id,
        String title,
        String department,
        String location,
        String type,
        JobStatus status,
        List<String> skills,
        String description,
        int applicants,
        LocalDate createdAt) {

    public static JobDto from(Job j, int applicants) {
        return new JobDto(j.getId(), j.getTitle(), j.getDepartment(), j.getLocation(), j.getType(),
                j.getStatus(), List.copyOf(j.getSkills()), j.getDescription(), applicants, j.getCreatedAt());
    }
}