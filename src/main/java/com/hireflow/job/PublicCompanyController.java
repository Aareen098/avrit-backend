package com.hireflow.job;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Open to everyone - a company's own careers page (SecurityConfig permits /public/**).
@RestController
@RequestMapping("/public/companies")
public class PublicCompanyController {

    private final JobService service;

    public PublicCompanyController(JobService service) {
        this.service = service;
    }

    // GET /api/public/companies/{slug}/jobs
    @GetMapping("/{slug}/jobs")
    public CompanyJobsDto jobs(@PathVariable String slug) {
        return service.companyJobs(slug);
    }
}