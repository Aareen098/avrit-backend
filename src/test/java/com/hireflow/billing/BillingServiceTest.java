package com.hireflow.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.usage.UsageReport;
import com.hireflow.usage.UsageReport.CompanyUsage;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class BillingServiceTest {

    private InvoiceRepository invoices;
    private CompanyRepository companies;
    private UsageReport report;
    private PlanHistory history;
    private BillingService service;

    @BeforeEach
    void setUp() {
        invoices = mock(InvoiceRepository.class);
        companies = mock(CompanyRepository.class);
        report = mock(UsageReport.class);
        history = mock(PlanHistory.class);
        // by default a company is billed on the plan it has now
        when(history.planAt(any(), any())).thenAnswer(i -> Plan.of(((Company) i.getArgument(0)).getPlanCode()));
        service = new BillingService(invoices, companies, report, history);
    }

    private static Company company(long id, String plan) {
        Company c = new Company("Acme " + id, "acme-" + id, "https://acme.test", "hr@acme.test", "1-10", "Tech");
        ReflectionTestUtils.setField(c, "id", id);
        c.setPlanCode(plan);
        return c;
    }

    @Test
    void usageInsideTheAllowanceCostsOnlyThePlanPrice() {
        Invoice i = BillingService.calculate(company(1, "PRO"), "2026-10", new CompanyUsage(100, 5000, 100));
        assertThat(i.getBaseAmount()).isEqualTo(2999);
        assertThat(i.getOverageAmount()).isZero();
        assertThat(i.getTotalAmount()).isEqualTo(2999);
    }

    @Test
    void extraUseIsBilledOnTop() {
        // FREE allows 50 AI calls and 50 applications: 10 extra AI calls (2 each) + 5 extra applications (1 each)
        Invoice i = BillingService.calculate(company(1, "FREE"), "2026-10", new CompanyUsage(60, 1000, 55));
        assertThat(i.getBaseAmount()).isZero();
        assertThat(i.getOverageAmount()).isEqualTo(25);
        assertThat(i.getTotalAmount()).isEqualTo(25);
    }

    @Test
    void unknownPlanIsTreatedAsFree() {
        assertThat(Plan.of("GOLD")).isEqualTo(Plan.FREE);
        assertThat(Plan.of(null)).isEqualTo(Plan.FREE);
        assertThat(Plan.isValid("GOLD")).isFalse();
        assertThat(Plan.isValid("PRO")).isTrue();
    }

    @Test
    void generateSkipsExistingInvoicesAndCompaniesOwingNothing() {
        // The test companies are created today, so the month must be the current one
        // (companies that joined after a month are not invoiced for it).
        String period = YearMonth.now().toString();
        Company paying = company(1, "PRO");
        Company alreadyInvoiced = company(2, "PRO");
        Company freeAndIdle = company(3, "FREE");
        when(companies.findAll()).thenReturn(List.of(paying, alreadyInvoiced, freeAndIdle));
        when(report.forMonth(any(YearMonth.class))).thenReturn(Map.of());
        when(invoices.existsByCompanyIdAndPeriod(2L, period)).thenReturn(true);

        int created = service.generate(period);

        assertThat(created).isEqualTo(1);
        verify(invoices).save(any(Invoice.class));
    }

    @Test
    void generateInvoicesOnThePlanFromTheHistoryNotTheCurrentOne() {
        String period = YearMonth.now().toString();
        Company c = company(1, "BUSINESS");           // on BUSINESS now...
        when(companies.findAll()).thenReturn(List.of(c));
        when(report.forMonth(any(YearMonth.class))).thenReturn(Map.of());
        doReturn(Plan.PRO).when(history).planAt(any(), any());   // ...but the history says PRO applied

        service.generate(period);

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getBaseAmount()).isEqualTo(2999);   // PRO price, not BUSINESS (9999)
    }

    @Test
    void futureMonthIsRejected() {
        String future = YearMonth.now().plusMonths(1).toString();
        assertThatThrownBy(() -> service.generate(future)).isInstanceOf(ApiException.class);
        verify(invoices, never()).save(any());
    }

    @Test
    void badMonthTextIsRejected() {
        assertThatThrownBy(() -> service.generate("October")).isInstanceOf(ApiException.class);
    }

    @Test
    void paidInvoiceCannotBeVoided() {
        Invoice paid = BillingService.calculate(company(1, "PRO"), "2026-10", CompanyUsage.NONE);
        paid.markPaid();
        when(invoices.findById(5L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> service.setStatus(5L, "VOID")).isInstanceOf(ApiException.class);
        verify(invoices, never()).save(any());
    }
}