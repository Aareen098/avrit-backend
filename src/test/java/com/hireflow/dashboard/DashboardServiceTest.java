package com.hireflow.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hireflow.application.ApplicationRepository;
import com.hireflow.application.ApplicationStage;
import com.hireflow.job.JobRepository;
import com.hireflow.job.JobStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private ApplicationRepository applications;
    private JobRepository jobs;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        applications = mock(ApplicationRepository.class);
        jobs = mock(JobRepository.class);
        service = new DashboardService(applications, jobs);
        when(applications.countPerDay(eq(1L), any())).thenReturn(List.<Object[]>of());
        when(applications.countPerJobAndStage(1L)).thenReturn(List.<Object[]>of());
        when(applications.countPerSkill(1L)).thenReturn(List.<Object[]>of());
        when(applications.countPerExperienceAndScore(1L)).thenReturn(List.<Object[]>of());
        when(applications.topMatches(eq(1L), any())).thenReturn(List.<Object[]>of());
        when(jobs.countByCompany_IdAndStatus(1L, JobStatus.OPEN)).thenReturn(3L);
    }

    @Test
    void scoresAreBucketedAndAveraged() {
        when(applications.countPerExperienceAndScore(1L)).thenReturn(List.<Object[]>of(
                new Object[] {2, 30, 2L}, new Object[] {2, 70, 1L}, new Object[] {2, 90, 1L}, new Object[] {2, null, 3L}));
        DashboardDto d = service.summary(1L);
        assertThat(d.scoreBuckets()).extracting(DashboardDto.Bucket::value).containsExactly(2L, 0L, 1L, 1L);
        assertThat(d.avgScore()).isEqualTo(55); // (30+30+70+90)/4, the 3 unscored are not counted
        assertThat(d.openJobs()).isEqualTo(3);
    }

    @Test
    void experienceIsBucketedAndEmptyScoresAreNull() {
        when(applications.countPerExperienceAndScore(1L)).thenReturn(List.<Object[]>of(
                new Object[] {0, null, 1L}, new Object[] {3, null, 2L}, new Object[] {12, null, 1L}));
        DashboardDto d = service.summary(1L);
        assertThat(d.experience()).extracting(DashboardDto.Bucket::value).containsExactly(1L, 2L, 0L, 1L);
        assertThat(d.avgScore()).isNull();
    }

    @Test
    void thirtyDaysAreAlwaysReturnedWithZerosFilledIn() {
        when(applications.countPerDay(eq(1L), any())).thenReturn(List.<Object[]>of(new Object[] {LocalDate.now(), 4L}));
        DashboardDto d = service.summary(1L);
        assertThat(d.perDay()).hasSize(30);
        assertThat(d.perDay().get(29).value()).isEqualTo(4);
        assertThat(d.perDay().get(0).value()).isZero();
        assertThat(d.newThisWeek()).isEqualTo(4);
    }

    @Test
    void stageCountsAndTotalsAreAddedUpFromTheJobRows() {
        when(applications.countPerJobAndStage(1L)).thenReturn(List.<Object[]>of(
                new Object[] {10L, "Developer", ApplicationStage.APPLIED, 3L},
                new Object[] {10L, "Developer", ApplicationStage.HIRED, 1L},
                new Object[] {11L, "Designer", ApplicationStage.APPLIED, 2L}));
        DashboardDto d = service.summary(1L);
        assertThat(d.total()).isEqualTo(6);
        assertThat(d.stageCounts().get("APPLIED")).isEqualTo(5);
        assertThat(d.stageCounts().get("HIRED")).isEqualTo(1);
        assertThat(d.byJob().get(0).job()).isEqualTo("Developer");
        assertThat(d.byJob().get(0).total()).isEqualTo(4);
        assertThat(d.unscored()).isEqualTo(6);
    }
}