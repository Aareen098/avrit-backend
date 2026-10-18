package com.hireflow.auth;

import com.hireflow.common.ApiException;
import com.hireflow.security.JwtService;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserDto;
import com.hireflow.user.UserRepository;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CompanyService companies;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,  CompanyService companies) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.companies = companies;
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

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmail(email)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwt.generate(user), UserDto.from(user));
    }
}