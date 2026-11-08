package com.hireflow.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findTop200ByOrderByIdDesc();

    List<Invoice> findByPeriodOrderByIdDesc(String period);

    List<Invoice> findByCompanyIdOrderByPeriodDesc(Long companyId);

    boolean existsByCompanyIdAndPeriod(Long companyId, String period);

    // Rows of [period, total of invoices that are not void] from a month onward ("2026-05" and later).
    @Query("select i.period, sum(i.totalAmount) from Invoice i where i.status <> 'VOID' and i.period >= :from group by i.period")
    List<Object[]> revenueSince(@Param("from") String from);
}