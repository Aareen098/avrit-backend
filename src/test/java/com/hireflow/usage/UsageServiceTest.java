package com.hireflow.usage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class UsageServiceTest {

    private UsageEventRepository events;
    private UsageService service;

    @BeforeEach
    void setUp() {
        events = mock(UsageEventRepository.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new UsageService(events, tm);
    }

    @Test
    void storesOneRow() {
        service.record(7L, UsageKind.LLM_SCREENING, 120);
        verify(events).save(any(UsageEvent.class));
    }

    @Test
    void ignoresZeroOrNegativeUnits() {
        service.record(7L, UsageKind.LLM_SCREENING, 0);
        service.record(7L, UsageKind.LLM_SCREENING, -5);
        verify(events, never()).save(any());
    }

    @Test
    void neverThrowsWhenTheDatabaseFails() {
        when(events.save(any())).thenThrow(new RuntimeException("db down"));
        service.record(7L, UsageKind.EMAIL, 1);   // must not throw
    }
}