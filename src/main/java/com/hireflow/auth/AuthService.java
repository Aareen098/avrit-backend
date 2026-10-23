package com.hireflow.auth;

import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.common.RateLimiter;
import com.hireflow.company.CompanyService;
import com.hireflow.mail.EmailService;
import com.hireflow.mail.EmailTemplates;
import com.hireflow.security.JwtService;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserDto;
import com.hireflow.user.UserRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CompanyService companies;

    private final PasswordTokenService tokens;
    private final EmailService emails;
    private final RateLimiter limiter;
    private final String frontendUrl;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, CompanyService companies,
                       PasswordTokenService tokens, EmailService emails, RateLimiter limiter,
                       @Value("${app.cors.allowed-origin}") String frontendUrl) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.companies = companies;
        this.tokens = tokens;
        this.emails = emails;
        this.limiter = limiter;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    // A new company and its first Admin are saved together, or not at all.
    @Transactional
    public AuthResponse registerCompany(RegisterCompanyRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        Company company = companies.create(req.companyName(), req.website(), email, req.size(), req.industry());
        User admin = users.save(new User(req.name().trim(), email, encoder.encode(req.password()), Role.ADMIN, company));
        return toResponse(admin);
    }

    // Public sign-up always creates a CANDIDATE. Staff accounts are created by an admin later.
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = users.save(new User(req.name().trim(), email, encoder.encode(req.password()), Role.CANDIDATE));
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmail(email)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return toResponse(user);
    }

    // Always finishes quietly, so nobody can use this to find out which emails have an account.
    // At most 3 emails per address every 15 minutes.
    public void forgotPassword(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        if (!limiter.allow("forgot:" + email, 3, Duration.ofMinutes(15))) return;
        users.findByEmail(email).ifPresent(user -> {
            String secret = tokens.issue(user, Duration.ofMinutes(15));
            emails.send(user.getEmail(), EmailTemplates.resetPassword(frontendUrl + "/reset-password?token=" + secret));
        });
    }

    // Also used by invited teammates to set their first password.
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = tokens.consume(token);
        user.setPasswordHash(encoder.encode(newPassword));
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwt.generate(user), UserDto.from(user));
    }
}