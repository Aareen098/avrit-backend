package com.hireflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hireflow.common.ApiException;
import com.hireflow.common.RateLimiter;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class GoogleLoginTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock CompanyService companies;
    @Mock PasswordTokenService tokens;
    @Mock EmailService emails;
    @Mock RateLimiter limiter;
    @Mock GoogleAuthService google;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, jwt, companies, tokens, emails, limiter, google,
                "http://localhost:5173");
        lenient().when(encoder.encode(any())).thenReturn("random-hash");
        lenient().when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void googleSays(String sub, String email) {
        when(google.verify("token")).thenReturn(new GoogleAuthService.GoogleUser(sub, email, "Some Name"));
    }

    @Test
    void newEmailBecomesACandidateWithNoCompany() {
        googleSays("sub-1", "new@example.com");

        service.googleLogin("token");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CANDIDATE);
        assertThat(saved.getValue().getCompany()).isNull();
        assertThat(saved.getValue().getGoogleSub()).isEqualTo("sub-1");
        verifyNoInteractions(companies);   // Google sign-in never creates a company
    }

    @Test
    void existingPasswordAccountIsLinkedAndOldPasswordIsReplaced() {
        User existing = new User("Ann", "ann@example.com", "old-hash", Role.RECRUITER);
        when(users.findByEmail("ann@example.com")).thenReturn(Optional.of(existing));
        googleSays("sub-2", "ann@example.com");

        service.googleLogin("token");

        assertThat(existing.getGoogleSub()).isEqualTo("sub-2");
        assertThat(existing.getPasswordHash()).isEqualTo("random-hash");   // old password no longer works
        assertThat(existing.getRole()).isEqualTo(Role.RECRUITER);          // role is not changed
    }

    @Test
    void emailAlreadyLinkedToAnotherGoogleAccountIsRejected() {
        User existing = new User("Ann", "ann@example.com", "hash", Role.CANDIDATE);
        existing.setGoogleSub("someone-else");
        when(users.findByEmail("ann@example.com")).thenReturn(Optional.of(existing));
        googleSays("sub-3", "ann@example.com");

        assertThatThrownBy(() -> service.googleLogin("token"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThat(existing.getGoogleSub()).isEqualTo("someone-else");
        verify(users, never()).save(any());
    }

    @Test
    void knownGoogleAccountLogsInWithoutChangingAnything() {
        User linked = new User("Ann", "ann@example.com", "hash", Role.CANDIDATE);
        linked.setGoogleSub("sub-4");
        when(users.findByGoogleSub("sub-4")).thenReturn(Optional.of(linked));
        googleSays("sub-4", "ann@example.com");

        service.googleLogin("token");

        assertThat(linked.getPasswordHash()).isEqualTo("hash");
        verify(users, never()).save(any());
    }

    @Test
    void invalidGoogleTokenCreatesNothing() {
        when(google.verify("token")).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google sign-in"));

        assertThatThrownBy(() -> service.googleLogin("token")).isInstanceOf(ApiException.class);
        verify(users, never()).save(any());
    }
}