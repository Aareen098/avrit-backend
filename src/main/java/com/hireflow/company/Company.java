package com.hireflow.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

// One company (tenant). Its settings live here too, so each company has its own.
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Used in the public careers link: /c/{slug}. Never changes after creation.
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String website;

    @Column(nullable = false)
    private String contactEmail;

    private String size;

    private String industry;

    @Column(nullable = false)
    private int shortlistThreshold = 80;

    @Column(nullable = false)
    private boolean showExplanation = true;

    @Column(nullable = false)
    private LocalDate createdAt;

    protected Company() {
        // required by JPA
    }

    public Company(String name, String slug, String website, String contactEmail, String size, String industry) {
        this.name = name;
        this.slug = slug;
        this.website = website;
        this.contactEmail = contactEmail;
        this.size = size;
        this.industry = industry;
        this.createdAt = LocalDate.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getWebsite() { return website; }
    public String getContactEmail() { return contactEmail; }
    public String getSize() { return size; }
    public String getIndustry() { return industry; }
    public int getShortlistThreshold() { return shortlistThreshold; }
    public boolean isShowExplanation() { return showExplanation; }
    public LocalDate getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setWebsite(String website) { this.website = website; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public void setShortlistThreshold(int shortlistThreshold) { this.shortlistThreshold = shortlistThreshold; }
    public void setShowExplanation(boolean showExplanation) { this.showExplanation = showExplanation; }
}