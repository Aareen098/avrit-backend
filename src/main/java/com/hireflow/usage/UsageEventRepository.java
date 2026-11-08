package com.hireflow.usage;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    // Rows of [companyId, kind, sum of units, number of events] for one time window.
    @Query("select e.companyId, e.kind, sum(e.units), count(e) from UsageEvent e "
            + "where e.companyId is not null and e.createdAt >= :from and e.createdAt < :to "
            + "group by e.companyId, e.kind")
    List<Object[]> byCompanyAndKind(@Param("from") Instant from, @Param("to") Instant to);

    // Rows of [kind, sum of units, number of events] for the whole platform in one time window.
    @Query("select e.kind, sum(e.units), count(e) from UsageEvent e "
            + "where e.createdAt >= :from and e.createdAt < :to group by e.kind")
    List<Object[]> byKind(@Param("from") Instant from, @Param("to") Instant to);

    // Rows of [companyId, bytes of CVs stored so far].
    @Query("select e.companyId, sum(e.units) from UsageEvent e "
            + "where e.kind = 'CV_STORAGE' and e.companyId is not null group by e.companyId")
    List<Object[]> storagePerCompany();

    @Query("select coalesce(sum(e.units), 0L) from UsageEvent e where e.kind = 'CV_STORAGE'")
    long storageTotal();

    // Bytes of CVs one company has stored so far.
    @Query("select coalesce(sum(e.units), 0L) from UsageEvent e where e.kind = 'CV_STORAGE' and e.companyId = :companyId")
    long storageForCompany(@Param("companyId") Long companyId);

    // Rows of [createdAt, kind, units] for the monthly charts.
    @Query("select e.createdAt, e.kind, e.units from UsageEvent e where e.createdAt >= :since")
    List<Object[]> since(@Param("since") Instant since);

    @Query("select e.createdAt, e.kind, e.units from UsageEvent e where e.companyId = :companyId and e.createdAt >= :since")
    List<Object[]> sinceForCompany(@Param("companyId") Long companyId, @Param("since") Instant since);

    @Query("select coalesce(sum(e.units), 0L) from UsageEvent e where e.kind = 'EMAIL' and e.companyId = :companyId and e.createdAt >= :from and e.createdAt < :to")
    long emailsForCompany(@Param("companyId") Long companyId, @Param("from") Instant from, @Param("to") Instant to);

    // One company's usage per month since a date. Rows of ["2026-11", kind, sum of units, number of events].
    @Query(value = "select to_char(created_at at time zone 'UTC', 'YYYY-MM'), kind, sum(units), count(*) from usage_events "
            + "where company_id = :companyId and created_at >= :from group by 1, 2", nativeQuery = true)
    List<Object[]> monthlyForCompany(@Param("companyId") Long companyId, @Param("from") Instant from);
}