package com.hireflow.job;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    // Newest first (by created date, then id).
    // Staff: only their own company's jobs, newest first.
    List<Job> findByCompany_IdOrderByCreatedAtDescIdDesc(Long companyId);

    Optional<Job> findByIdAndCompany_Id(Long id, Long companyId);

    // Public: one company's open jobs (careers page).
    List<Job> findByCompany_SlugAndStatusOrderByCreatedAtDescIdDesc(String slug, JobStatus status);

    List<Job> findByStatusOrderByCreatedAtDescIdDesc(JobStatus status);

    Optional<Job> findByIdAndStatus(Long id, JobStatus status);
}