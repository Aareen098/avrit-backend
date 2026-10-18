package com.hireflow.config;

import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

// Development helper: creates the demo company and one demo user per role when the users table is empty.
// Remove (or guard with a profile) before any real deployment.
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true", matchIfMissing = true)
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, CompanyRepository companies, PasswordEncoder encoder) {
        this.users = users;
        this.companies = companies;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        Company acme = companies.findBySlug("acme").orElseGet(() -> companies.save(new Company(
                "Acme Technologies", "acme", "https://acme.example.com", "careers@acme.example.com", null, null)));
        String hash = encoder.encode("password123");
        users.save(new User("Admin User", "admin@hireflow.com", hash, Role.ADMIN, acme));
        users.save(new User("Riya Recruiter", "recruiter@hireflow.com", hash, Role.RECRUITER, acme));
        users.save(new User("Harsh Manager", "manager@hireflow.com", hash, Role.HIRING_MANAGER, acme));
        users.save(new User("Cara Candidate", "candidate@hireflow.com", hash, Role.CANDIDATE));
    }
}