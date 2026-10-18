package com.hireflow.job;

import java.util.List;

// What the public careers pages see: no status and no applicant count.
public record PublicJobDto(
        Long id,
        String title,
        String department,
        String location,
        String type,
        List<String> skills,
        String description,
        String companyName,
        String companySlug) {

    public static PublicJobDto from(Job j) {
        return new PublicJobDto(j.getId(), j.getTitle(), j.getDepartment(), j.getLocation(), j.getType(),
                List.copyOf(j.getSkills()), j.getDescription(), j.getCompany().getName(), j.getCompany().getSlug());
    }
}