package com.hireflow.job;

import java.util.List;

// Answer of GET /public/companies/{slug}/jobs: { company: { name, slug }, jobs: [...] }
public record CompanyJobsDto(CompanyInfo company, List<PublicJobDto> jobs) {

    public record CompanyInfo(String name, String slug) {
    }
}