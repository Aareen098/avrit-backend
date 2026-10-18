package com.hireflow.job;

import com.hireflow.application.ApplicationRepository;
import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.security.Tenant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final CompanyRepository companies;

    public JobService(JobRepository jobs, ApplicationRepository applications, CompanyRepository companies) {
        this.jobs = jobs;
        this.applications = applications;
        this.companies = companies;
    }

    // ---- staff ----

    public List<JobDto> list() {
        Long companyId = Tenant.companyId();
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : applications.countPerJob(companyId)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return jobs.findByCompany_IdOrderByCreatedAtDescIdDesc(companyId).stream()
                .map(j -> JobDto.from(j, counts.getOrDefault(j.getId(), 0)))
                .toList();
    }

    public JobDto create(JobRequest req) {
        List<String> skills = req.skills() == null ? List.of() : req.skills().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        Company company = companies.findById(Tenant.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Company not found"));
        Job job = new Job(company, req.title().trim(), req.department().trim(), req.location().trim(),
                req.type().trim(), req.description().trim(), req.status(), skills);
        return JobDto.from(jobs.save(job), 0);
    }

    // ---- public (only OPEN jobs are visible) ----

    public List<PublicJobDto> listPublic() {
        return jobs.findByStatusOrderByCreatedAtDescIdDesc(JobStatus.OPEN).stream().map(PublicJobDto::from).toList();
    }

    public CompanyJobsDto companyJobs(String slug) {
        Company company = companies.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
        List<PublicJobDto> list = jobs.findByCompany_SlugAndStatusOrderByCreatedAtDescIdDesc(slug, JobStatus.OPEN)
                .stream().map(PublicJobDto::from).toList();
        return new CompanyJobsDto(new CompanyJobsDto.CompanyInfo(company.getName(), company.getSlug()), list);
    }

    public PublicJobDto getPublic(Long id) {
        return jobs.findByIdAndStatus(id, JobStatus.OPEN)
                .map(PublicJobDto::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
    }
}