package com.hireflow.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.hireflow.company.Company;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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

    public void setName(String name) { this.name = name; }
    public void setRole(Role role) { this.role = role; }
}