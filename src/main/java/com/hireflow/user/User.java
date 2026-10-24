package com.hireflow.user;

import com.hireflow.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Table is called "users" because "user" is a reserved word in PostgreSQL.
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Staff belong to one company. Candidates have none (they are global).
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    // ---- candidate profile: all optional, so they can be added to a table that already has rows ----
    private String phone;
    private String location;
    private String headline;

    @Column(columnDefinition = "text")
    private String about;

    private String linkedin;
    private String github;
    private String portfolio;

    // Google's id for this person, set the first time they use "Continue with Google". Null for password-only accounts.
    @Column(unique = true)
    private String googleSub;

    // The ONE resume on the profile. Applications keep pointing at the file they applied with.
    private String resumeFileName;
    private String resumeStoredName;

    protected User() {
        // required by JPA
    }

    public User(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public User(String name, String email, String passwordHash, Role role, Company company) {
        this(name, email, passwordHash, role);
        this.company = company;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Company getCompany() { return company; }
    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public String getHeadline() { return headline; }
    public String getAbout() { return about; }
    public String getLinkedin() { return linkedin; }
    public String getGithub() { return github; }
    public String getPortfolio() { return portfolio; }
    public String getResumeFileName() { return resumeFileName; }
    public String getResumeStoredName() { return resumeStoredName; }

    public void updateProfile(String phone, String location, String headline, String about,
                              String linkedin, String github, String portfolio) {
        this.phone = phone;
        this.location = location;
        this.headline = headline;
        this.about = about;
        this.linkedin = linkedin;
        this.github = github;
        this.portfolio = portfolio;
    }

    public void setResume(String fileName, String storedName) {
        this.resumeFileName = fileName;
        this.resumeStoredName = storedName;
    }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setName(String name) { this.name = name; }
    public void setRole(Role role) { this.role = role; }
}