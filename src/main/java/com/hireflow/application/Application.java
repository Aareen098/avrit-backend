package com.hireflow.application;

import com.hireflow.common.StringListConverter;
import com.hireflow.job.Job;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// One person applying for one job. The Candidates pages are built on this same table.
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Code the candidate keeps, e.g. HF-K7P2QX
    @Column(nullable = false, unique = true)
    private String reference;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(nullable = false)
    private String name;

    // Always stored in lower case.
    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(columnDefinition = "text")
    private String coverNote;

    // The name the candidate uploaded, and the random name it is saved under on disk.
    // A candidate added by staff without a CV has "manual-entry" and an empty stored name.
    @Column(nullable = false)
    private String resumeFileName;

    @Column(nullable = false)
    private String resumeStoredName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStage stage;

    private String rejectionReason;

    @Column(columnDefinition = "text")
    private String rejectionNote;

    @Column(nullable = false)
    private LocalDate appliedAt;

    // ---- candidate details: all optional, so they can be added to a table that already has rows ----
    // Filled by staff now; the CV-reading batch will fill them automatically later.

    private String location;

    private Integer experienceYears;

    @Column(columnDefinition = "text")
    private String summary;

    // Text read from the CV file. Empty if the file could not be read.
    @Column(columnDefinition = "text")
    private String resumeText;
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> skills = new ArrayList<>();

    // ---- AI analysis (empty until the LLM has read the CV) ----

    private Integer aiScore;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> aiReasons = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> aiMatched = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> aiMissing = new ArrayList<>();

    protected Application() {
        // required by JPA
    }

    public Application(String reference, Job job, String name, String email, String phone,
                       String coverNote, String resumeFileName, String resumeStoredName) {
        this.reference = reference;
        this.job = job;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.coverNote = coverNote;
        this.resumeFileName = resumeFileName;
        this.resumeStoredName = resumeStoredName;
        this.stage = ApplicationStage.APPLIED;
        this.appliedAt = LocalDate.now();
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public Job getJob() { return job; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCoverNote() { return coverNote; }
    public String getResumeFileName() { return resumeFileName; }
    public String getResumeStoredName() { return resumeStoredName; }
    public ApplicationStage getStage() { return stage; }
    public String getRejectionReason() { return rejectionReason; }
    public String getRejectionNote() { return rejectionNote; }
    public LocalDate getAppliedAt() { return appliedAt; }
    public String getLocation() { return location; }
    public Integer getExperienceYears() { return experienceYears; }
    public String getSummary() { return summary; }
    public String getResumeText() { return resumeText; }
    public List<String> getSkills() { return skills; }

    public Integer getAiScore() { return aiScore; }
    public List<String> getAiReasons() { return aiReasons; }
    public List<String> getAiMatched() { return aiMatched; }
    public List<String> getAiMissing() { return aiMissing; }

    public void setAi(Integer score, List<String> reasons, List<String> matched, List<String> missing) {
        this.aiScore = score;
        this.aiReasons = new ArrayList<>(reasons);
        this.aiMatched = new ArrayList<>(matched);
        this.aiMissing = new ArrayList<>(missing);
    }
    public void setStage(ApplicationStage stage) { this.stage = stage; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setRejectionNote(String rejectionNote) { this.rejectionNote = rejectionNote; }
    public void setLocation(String location) { this.location = location; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public void setSkills(List<String> skills) { this.skills = new ArrayList<>(skills); }
}