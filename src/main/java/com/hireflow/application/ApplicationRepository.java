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

    // Rows of [jobId, number of applications] - used for the "applicants" column on the Jobs page.
    @Query("select a.job.id, count(a) from Application a where a.job.company.id = :companyId group by a.job.id")
    List<Object[]> countPerJob(@Param("companyId") Long companyId);
}