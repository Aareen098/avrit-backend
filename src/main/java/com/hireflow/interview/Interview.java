package com.hireflow.interview;

import com.hireflow.application.Application;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

// One scheduled interview for one application.
@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    // Column names avoid the plain words "date" and "time", which are awkward in SQL.
    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Column(name = "interview_time", nullable = false)
    private LocalTime interviewTime;

    // "Video call", "Phone call" or "On-site" - the same text the frontend shows.
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String interviewer;

    @Column(columnDefinition = "text")
    private String notes;

    protected Interview() {
        // required by JPA
    }

    public Interview(Application application, LocalDate interviewDate, LocalTime interviewTime,
                     String type, String interviewer, String notes) {
        this.application = application;
        this.interviewDate = interviewDate;
        this.interviewTime = interviewTime;
        this.type = type;
        this.interviewer = interviewer;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public LocalDate getInterviewDate() { return interviewDate; }
    public LocalTime getInterviewTime() { return interviewTime; }
    public String getType() { return type; }
    public String getInterviewer() { return interviewer; }
    public String getNotes() { return notes; }
}