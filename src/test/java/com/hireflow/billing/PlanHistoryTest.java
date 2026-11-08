package com.hireflow.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hireflow.company.Company;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlanHistoryTest {

    private PlanChangeRepository changes;
    private PlanHistory history;
    private Company company;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        changes = mock(PlanChangeRepository.class);
        history = new PlanHistory(changes);
        company = new Company("Acme", "acme", "https://acme.test", "hr@acme.test", "1-10", "Tech");
        ReflectionTestUtils.setField(company, "id", 1L);
        company.setPlanCode("BUSINESS");
    }

    private PlanChange change(String from, String to, Instant at) {
        PlanChange c = new PlanChange(1L, from, to, "owner@avrit.test");
        ReflectionTestUtils.setField(c, "changedAt", at);
        return c;
    }

    @Test
    void withNoChangesTheCurrentPlanApplies() {
        when(changes.findByCompanyIdOrderByChangedAtAsc(1L)).thenReturn(List.of());
        assertThat(history.planAt(company, now.minus(40, ChronoUnit.DAYS))).isEqualTo(Plan.BUSINESS);
    }

    @Test
    void aChangeAfterTheMomentMeansTheOldPlanWasInForce() {
        when(changes.findByCompanyIdOrderByChangedAtAsc(1L))
                .thenReturn(List.of(change("PRO", "BUSINESS", now.minus(2, ChronoUnit.DAYS))));
        assertThat(history.planAt(company, now.minus(10, ChronoUnit.DAYS))).isEqualTo(Plan.PRO);
    }

    @Test
    void aChangeBeforeTheMomentMeansTheNewPlanWasInForce() {
        when(changes.findByCompanyIdOrderByChangedAtAsc(1L))
                .thenReturn(List.of(change("PRO", "BUSINESS", now.minus(10, ChronoUnit.DAYS))));
        assertThat(history.planAt(company, now.minus(2, ChronoUnit.DAYS))).isEqualTo(Plan.BUSINESS);
    }
}