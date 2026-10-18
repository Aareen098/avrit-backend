package com.hireflow.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    // POST /api/auth/register  { name, email, password }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    // POST /api/auth/register-company - creates a company and its first Admin, and logs them in
    @PostMapping("/register-company")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerCompany(@Valid @RequestBody RegisterCompanyRequest req) {
        return auth.registerCompany(req);
    }

    // POST /api/auth/login  { email, password }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}