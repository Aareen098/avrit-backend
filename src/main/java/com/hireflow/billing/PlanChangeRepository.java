package com.hireflow.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanChangeRepository extends JpaRepository<PlanChange, Long> {
    List<PlanChange> findByCompanyIdOrderByChangedAtAsc(Long companyId);
}