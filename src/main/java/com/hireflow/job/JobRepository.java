package com.hireflow.job;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobRepository extends JpaRepository<Job, Long> {

    long countByCompany_IdAndStatus(Long companyId, JobStatus status);

    // Staff: only their own company's jobs, newest first.
    List<Job> findByCompany_IdOrderByCreatedAtDescIdDesc(Long companyId);

    Optional<Job> findByIdAndCompany_Id(Long id, Long companyId);

    // Public: one company's open jobs (careers page).
    List<Job> findByCompany_SlugAndStatusOrderByCreatedAtDescIdDesc(String slug, JobStatus status);

    List<Job> findByStatusOrderByCreatedAtDescIdDesc(JobStatus status);

    Optional<Job> findByIdAndStatus(Long id, JobStatus status);

    // Platform owner: only how many jobs are open per company, rows of [companyId, open jobs].
    @Query("select j.company.id, count(j) from Job j where j.status = com.hireflow.job.JobStatus.OPEN group by j.company.id")
    List<Object[]> openPerCompany();
}