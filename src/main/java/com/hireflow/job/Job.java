package com.hireflow.job;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.hireflow.company.Company;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String location;

    // "Full-time", "Part-time", "Contract" or "Internship" - the same text the frontend shows.
    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    // Stored in a separate table "job_skills" (one row per skill).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate createdAt;

    protected Job() {
        // required by JPA
    }

    public Job(Company company,String title, String department, String location, String type,
               String description, JobStatus status, List<String> skills) {
        this.company = company;
        this.title = title;
        this.department = department;
        this.location = location;
        this.type = type;
        this.description = description;
        this.status = status;
        this.skills = new ArrayList<>(skills);
        this.createdAt = LocalDate.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDepartment() { return department; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public JobStatus getStatus() { return status; }
    public List<String> getSkills() { return skills; }
    public LocalDate getCreatedAt() { return createdAt; }
    public Company getCompany() { return company; }

    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}