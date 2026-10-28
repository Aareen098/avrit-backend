package com.hireflow.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationRepository;
import com.hireflow.application.ApplicationStage;
import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.mail.EmailService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobs;
    @Mock ApplicationRepository applications;
    @Mock CompanyRepository companies;
    @Mock EmailService emails;

    private JobService service;
    private Job job;

    @BeforeEach
    void setUp() {
        service = new JobService(jobs, applications, companies, emails);
        Company acme = new Company("Acme", "acme", null, null, null, null);
        job = new Job(acme, "Developer", "Engineering", "Remote", "Full-time", "desc", JobStatus.OPEN, List.of("Java"));
        loginAsCompany(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Same shape as the real login: the company id sits in the authentication details.
    private static void loginAsCompany(Long companyId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("staff", null, List.of());
        auth.setDetails(companyId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Application application(String email, ApplicationStage stage) {
        Application a = new Application("REF-" + email, job, "Name", email, "123", null, "cv.pdf", "stored.pdf");
        a.setStage(stage);
        return a;
    }

    // ---- tenant isolation ----

    @Test
    void anotherCompanysJobIsNotFound() {
        // The job exists, but only for company 2. We are company 1.
        lenient().when(jobs.findByIdAndCompany_Id(5L, 2L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.setStatus(5L, JobStatus.CLOSED))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(jobs).findByIdAndCompany_Id(5L, 1L);
        verify(applications, never()).findWithoutResponse(any());
        verify(emails, never()).send(anyString(), any());
    }

    @Test
    void listUsesTheCompanyFromTheToken() {
        loginAsCompany(7L);

        service.list();

        verify(jobs).findByCompany_IdOrderByCreatedAtDescIdDesc(7L);
    }

    @Test
    void noCompanyInTokenMeansForbidden() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.list())
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---- job close / reopen ----

    @Test
    void closingRejectsOnlyUnansweredApplicantsAndEmailsThem() {
        Application unanswered = application("new@x.com", ApplicationStage.APPLIED);
        Application inProgress = application("busy@x.com", ApplicationStage.SCREENING);
        when(jobs.findByIdAndCompany_Id(5L, 1L)).thenReturn(Optional.of(job));
        when(applications.findWithoutResponse(any())).thenReturn(List.of(unanswered));   // inProgress is not returned

        service.setStatus(5L, JobStatus.CLOSED);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(unanswered.getStage()).isEqualTo(ApplicationStage.REJECTED);
        assertThat(unanswered.getRejectionReason()).isEqualTo(Application.JOB_CLOSED_REASON);
        assertThat(inProgress.getStage()).isEqualTo(ApplicationStage.SCREENING);
        verify(emails, times(1)).send(eq("new@x.com"), any());
        verify(emails, times(1)).send(anyString(), any());
    }

    @Test
    void reopeningRestoresApplicantsClosedWithTheJob() {
        job.setStatus(JobStatus.CLOSED);
        Application closedWithJob = application("new@x.com", ApplicationStage.REJECTED);
        closedWithJob.setRejectionReason(Application.JOB_CLOSED_REASON);
        when(jobs.findByIdAndCompany_Id(5L, 1L)).thenReturn(Optional.of(job));
        when(applications.findByJob_IdAndStageAndRejectionReason(any(), eq(ApplicationStage.REJECTED),
                eq(Application.JOB_CLOSED_REASON))).thenReturn(List.of(closedWithJob));

        service.setStatus(5L, JobStatus.OPEN);

        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(closedWithJob.getStage()).isEqualTo(ApplicationStage.APPLIED);
        assertThat(closedWithJob.getRejectionReason()).isNull();
    }

    @Test
    void publishedJobCannotGoBackToDraft() {
        when(jobs.findByIdAndCompany_Id(5L, 1L)).thenReturn(Optional.of(job));   // job is OPEN

        assertThatThrownBy(() -> service.setStatus(5L, JobStatus.DRAFT))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}