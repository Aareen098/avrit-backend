package com.hireflow.job;

import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

// Development helper: adds the same 5 sample jobs the frontend mock used, when the jobs table is empty.
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true", matchIfMissing = true)
@Order(2)
public class JobSeeder implements CommandLineRunner {

    private final JobRepository jobs;
    private final CompanyRepository companies;

    public JobSeeder(JobRepository jobs, CompanyRepository companies) {
        this.jobs = jobs;
        this.companies = companies;
    }

    @Override
    public void run(String... args) {
        if (jobs.count() > 0) return;
        Company acme = companies.findBySlug("acme").orElse(null);
        if (acme == null) return;
        jobs.saveAll(List.of(
                job(acme, "Senior Frontend Engineer", "Engineering", "Remote", "Full-time", JobStatus.OPEN, "2026-09-28",
                        "Build fast, accessible interfaces for our hiring platform. You will own features end to end, work closely with design, and mentor junior engineers.",
                        "React", "TypeScript", "Tailwind"),
                job(acme, "Product Designer", "Design", "Bengaluru", "Full-time", JobStatus.OPEN, "2026-09-30",
                        "Design clear, calm product experiences. You will run user research, build prototypes and ship alongside engineering.",
                        "Figma", "Prototyping", "Research"),
                job(acme, "Backend Engineer (Java)", "Engineering", "Hyderabad", "Full-time", JobStatus.OPEN, "2026-10-01",
                        "Design and build the REST APIs behind our platform. You will care about clean code, testing and performance.",
                        "Java", "Spring Boot", "PostgreSQL"),
                job(acme, "Marketing Intern", "Marketing", "Remote", "Internship", JobStatus.DRAFT, "2026-10-03",
                        "Help plan campaigns, write content and track results. This role is still a draft."),
                job(acme, "HR Generalist", "People", "Pune", "Contract", JobStatus.CLOSED, "2026-08-12",
                        "Support hiring, onboarding and employee queries. This role has been filled.")));
    }

    private Job job(Company company, String title, String department, String location, String type,
                    JobStatus status, String createdAt, String description, String... skills) {
        Job job = new Job(company, title, department, location, type, description, status, List.of(skills));
        job.setCreatedAt(LocalDate.parse(createdAt));
        return job;
    }
}