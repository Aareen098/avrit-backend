package com.hireflow.application;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByReference(String reference);

    // True while any application still uses this CV file, so it must not be deleted.
    boolean existsByResumeStoredName(String storedName);

    boolean existsByJob_IdAndEmailIgnoreCase(Long jobId, String email);

    // Exact query for the status check: reference and email must both match, ignoring upper/lower case.
    @Query("select a from Application a where upper(a.reference) = upper(:reference) and lower(a.email) = lower(:email)")
    Optional<Application> findForStatus(@Param("reference") String reference, @Param("email") String email);

    List<Application> findByEmailIgnoreCaseOrderByAppliedAtDescIdDesc(String email);

    // Staff Candidates list and Pipeline: only this company's applications, newest first.
    List<Application> findByJob_Company_IdOrderByAppliedAtDescIdDesc(Long companyId);

    // Another company's id simply finds nothing, so the answer is 404.
    Optional<Application> findByIdAndJob_Company_Id(Long id, Long companyId);

    // Applications with a CV file whose text has not been read yet - used once at startup.
    List<Application> findByResumeTextIsNullAndResumeStoredNameNot(String storedName);

    long countByJob_Id(Long jobId);

    // Still at APPLIED and no interview scheduled: nobody has responded to these yet.
    @Query("select a from Application a where a.job.id = :jobId "
            + "and a.stage = com.hireflow.application.ApplicationStage.APPLIED "
            + "and not exists (select i.id from Interview i where i.application = a)")
    List<Application> findWithoutResponse(@Param("jobId") Long jobId);

    // Applications that were closed automatically with their job (used when the job is reopened).
    List<Application> findByJob_IdAndStageAndRejectionReason(Long jobId, ApplicationStage stage, String reason);

    // ---- platform owner statistics ----

    // Rows of [jobId, number of applications] - used for the "applicants" column on the Jobs page.
    @Query("select a.job.id, count(a) from Application a where a.job.company.id = :companyId group by a.job.id")
    List<Object[]> countPerJob(@Param("companyId") Long companyId);

    // Platform owner: date of the latest application per company, rows of [companyId, date].
    @Query("select a.job.company.id, max(a.appliedAt) from Application a group by a.job.company.id")
    List<Object[]> lastActivityPerCompany();

    // Platform owner: how many applications each company processed in a date range, rows of [companyId, count].
    @Query("select a.job.company.id, count(a) from Application a "
            + "where a.appliedAt >= :from and a.appliedAt <= :to group by a.job.company.id")
    List<Object[]> countPerCompanyBetween(@Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);

    @Query("select a.appliedAt, count(a) from Application a where a.job.company.id = :companyId and a.appliedAt >= :from group by a.appliedAt")
    List<Object[]> countPerDay(@Param("companyId") Long companyId, @Param("from") java.time.LocalDate from);

    @Query("select a.job.id, a.job.title, a.stage, count(a) from Application a where a.job.company.id = :companyId group by a.job.id, a.job.title, a.stage")
    List<Object[]> countPerJobAndStage(@Param("companyId") Long companyId);

    // Rows of [skill, number of candidates with it], the 8 most common first. Skills are saved as one text
    // column ("Java|Spring Boot"), so the database splits that text itself.
    @Query(value = "select s.skill, count(*) from applications a join jobs j on j.id = a.job_id "
            + "cross join lateral unnest(string_to_array(a.skills, '|')) as s(skill) "
            + "where j.company_id = :companyId and s.skill <> '' "
            + "group by s.skill order by count(*) desc, s.skill asc limit 8", nativeQuery = true)
    List<Object[]> countPerSkill(@Param("companyId") Long companyId);

    // Rows of [years of experience (empty = 0), AI score (null = not scored), number of candidates].
    @Query("select coalesce(a.experienceYears, 0), a.aiScore, count(a) from Application a where a.job.company.id = :companyId "
            + "group by coalesce(a.experienceYears, 0), a.aiScore")
    List<Object[]> countPerExperienceAndScore(@Param("companyId") Long companyId);

    // The best AI matches still in play. Rows of [id, name, job title, stage, AI score].
    @Query("select a.id, a.name, a.job.title, a.stage, a.aiScore from Application a "
            + "where a.job.company.id = :companyId and a.aiScore is not null "
            + "and a.stage not in (com.hireflow.application.ApplicationStage.HIRED, com.hireflow.application.ApplicationStage.REJECTED) "
            + "order by a.aiScore desc, a.id desc")
    List<Object[]> topMatches(@Param("companyId") Long companyId, org.springframework.data.domain.Pageable page);

    // A company's applications per month since a date. Rows of ["2026-11", number].
    @Query(value = "select to_char(a.applied_at, 'YYYY-MM'), count(*) from applications a join jobs j on j.id = a.job_id "
            + "where j.company_id = :companyId and a.applied_at >= :from group by 1", nativeQuery = true)
    List<Object[]> applicationsPerMonthForCompany(@Param("companyId") Long companyId, @Param("from") java.time.LocalDate from);


}