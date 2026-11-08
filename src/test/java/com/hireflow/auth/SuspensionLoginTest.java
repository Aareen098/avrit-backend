package com.hireflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hireflow.common.ApiException;
import com.hireflow.common.RateLimiter;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyService;
import com.hireflow.mail.EmailService;
import com.hireflow.security.JwtService;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SuspensionLoginTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock CompanyService companies;
    @Mock PasswordTokenService tokens;
    @Mock EmailService emails;
    @Mock RateLimiter limiter;
    @Mock GoogleAuthService google;

    private AuthService service;
    private Company company;
    private User staff;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, jwt, companies, tokens, emails, limiter, google, "http://localhost:5173");
        company = new Company("Acme", "acme", "https://acme.com", "hr@acme.com", null, null);
        staff = new User("Ann", "ann@x.com", "hash", Role.ADMIN, company);
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(staff));
        when(encoder.matches("pw", "hash")).thenReturn(true);
    }

    @Test
    void staffOfASuspendedCompanyCannotLogIn() {
        company.setSuspended(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("ann@x.com", "pw")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(jwt, never()).generate(any());   // no token is created for them
    }

    @Test
    void staffOfAnActiveCompanyCanLogIn() {
        when(jwt.generate(staff)).thenReturn("token");

        assertThat(service.login(new LoginRequest("ann@x.com", "pw")).token()).isEqualTo("token");
    }
}