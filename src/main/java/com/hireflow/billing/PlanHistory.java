package com.hireflow.billing;

import com.hireflow.company.Company;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanHistory {

    private final PlanChangeRepository changes;

    public PlanHistory(PlanChangeRepository changes) { this.changes = changes; }

    @Transactional
    public void record(Long companyId, String fromPlan, String toPlan, String changedBy) {
        changes.save(new PlanChange(companyId, fromPlan, toPlan, changedBy));
    }

    @Transactional(readOnly = true)
    public Plan planAt(Company company, Instant moment) {
        for (PlanChange change : changes.findByCompanyIdOrderByChangedAtAsc(company.getId())) {
            if (change.getChangedAt().isAfter(moment)) return Plan.of(change.getFromPlan());
        }
        return Plan.of(company.getPlanCode());
    }
}