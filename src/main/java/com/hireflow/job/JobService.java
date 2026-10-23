package com.hireflow.job;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationRepository;
import com.hireflow.application.ApplicationStage;
import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.security.Tenant;
import com.hireflow.mail.EmailService;
import com.hireflow.mail.EmailTemplates;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final CompanyRepository companies;
    private final EmailService emails;

    public JobService(JobRepository jobs, ApplicationRepository applications, CompanyRepository companies,
                      EmailService emails) {
        this.emails = emails;
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

    private static List<String> cleanSkills(List<String> raw) {
        return raw == null ? List.of() : raw.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    @Transactional
    public JobDto update(Long id, JobUpdateRequest req) {
        Job job = jobs.findByIdAndCompany_Id(id, Tenant.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
        job.update(req.title().trim(), req.department().trim(), req.location().trim(), req.type().trim(),
                req.description().trim(), cleanSkills(req.skills()));
        return JobDto.from(jobs.save(job), (int) applications.countByJob_Id(job.getId()));
    }

    public JobDto create(JobRequest req) {
        List<String> skills = cleanSkills(req.skills());
        Company company = companies.findById(Tenant.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Company not found"));
        Job job = new Job(company, req.title().trim(), req.department().trim(), req.location().trim(),
                req.type().trim(), req.description().trim(), req.status(), skills);
        return JobDto.from(jobs.save(job), 0);
    }

    // Close, reopen or publish a job. One transaction, so the job and its applications change together.
    //  - CLOSED: applications nobody has responded to yet (still APPLIED, no interview) are closed too,
    //    so they leave the pipeline and the candidate sees "job closed". Everyone further along is untouched.
    //  - OPEN again: applications that were closed with the job go back to APPLIED.
    @Transactional
    public JobDto setStatus(Long id, JobStatus status) {
        Job job = jobs.findByIdAndCompany_Id(id, Tenant.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
        if (status == JobStatus.DRAFT && job.getStatus() != JobStatus.DRAFT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A published job cannot go back to draft");
        }
        if (job.getStatus() != status) {
            job.setStatus(status);
            if (status == JobStatus.CLOSED) {
                for (Application a : applications.findWithoutResponse(job.getId())) {
                    a.setStage(ApplicationStage.REJECTED);
                    a.setRejectionReason(Application.JOB_CLOSED_REASON);
                    a.setRejectionNote(null);
                    emails.send(a.getEmail(), EmailTemplates.jobClosed(a.getName(), job.getTitle(),
                            job.getCompany().getName()));
                }
            } else if (status == JobStatus.OPEN) {
                for (Application a : applications.findByJob_IdAndStageAndRejectionReason(
                        job.getId(), ApplicationStage.REJECTED, Application.JOB_CLOSED_REASON)) {
                    a.setStage(ApplicationStage.APPLIED);
                    a.setRejectionReason(null);
                    a.setRejectionNote(null);
                }
            }
            jobs.save(job);
        }
        return JobDto.from(job, (int) applications.countByJob_Id(job.getId()));
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