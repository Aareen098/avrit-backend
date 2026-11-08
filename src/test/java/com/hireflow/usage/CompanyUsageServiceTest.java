package com.hireflow.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hireflow.application.ApplicationRepository;
import com.hireflow.billing.BillingService;
import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.user.UserRepository;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanyUsageServiceTest {

    private CompanyRepository companies;
    private UsageEventRepository events;
    private ApplicationRepository applications;
    private CompanyUsageService service;

    @BeforeEach
    void setUp() {
        companies = mock(CompanyRepository.class);
        events = mock(UsageEventRepository.class);
        applications = mock(ApplicationRepository.class);
        UserRepository users = mock(UserRepository.class);
        BillingService billing = mock(BillingService.class);
        when(users.findByCompany_IdOrderByIdAsc(7L)).thenReturn(List.of());
        when(billing.forCompany(7L)).thenReturn(List.of());
        when(events.monthlyForCompany(eq(7L), any())).thenReturn(List.<Object[]>of());
        when(applications.applicationsPerMonthForCompany(eq(7L), any())).thenReturn(List.<Object[]>of());
        service = new CompanyUsageService(companies, users, events, applications, billing);
    }

    @Test
    void returnsPlanLimitsSixMonthsAndThisMonthsUsage() {
        String thisMonth = YearMonth.now(ZoneOffset.UTC).toString();
        Company c = new Company("Acme", "acme", "https://a.test", "hr@a.test", "1-10", "Tech");
        c.setPlanCode("PRO");
        when(companies.findById(7L)).thenReturn(Optional.of(c));
        when(events.monthlyForCompany(eq(7L), any())).thenReturn(List.<Object[]>of(
                new Object[] {thisMonth, UsageKind.LLM_SCREENING, 3000L, 10L},
                new Object[] {thisMonth, UsageKind.LLM_ASSISTANT, 400L, 2L},
                new Object[] {thisMonth, UsageKind.EMAIL, 5L, 5L},
                new Object[] {thisMonth, UsageKind.CV_STORAGE, 999L, 1L}));   // storage is not an AI call
        when(applications.applicationsPerMonthForCompany(eq(7L), any())).thenReturn(List.<Object[]>of(new Object[] {thisMonth, 9L}));
        when(events.storageForCompany(7L)).thenReturn(2048L);

        CompanyUsageDto dto = service.get(7L);

        assertThat(dto.plan().code()).isEqualTo("PRO");
        assertThat(dto.plan().aiCallsIncluded()).isEqualTo(500);
        assertThat(dto.history()).hasSize(6);
        assertThat(dto.now().aiCalls()).isEqualTo(12);
        assertThat(dto.now().llmTokens()).isEqualTo(3400);
        assertThat(dto.now().applications()).isEqualTo(9);
        assertThat(dto.now().emails()).isEqualTo(5);
        assertThat(dto.now().storageBytes()).isEqualTo(2048);
        assertThat(dto.history().get(5).month()).isEqualTo(thisMonth);
        assertThat(dto.history().get(0).emails()).isZero();
    }

    @Test
    void unknownCompanyIsNotFound() {
        when(companies.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(ApiException.class);
    }
}