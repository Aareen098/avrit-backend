package com.hireflow.company;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("select c.id from Company c where c.suspended = true")
    List<Long> findSuspendedIds();

    @Query("select c.createdAt from Company c where c.createdAt >= :since")
    List<LocalDate> createdSince(@Param("since") LocalDate since);
}