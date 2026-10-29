package com.hireflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hireflow.common.ApiException;
import com.hireflow.common.RateLimiter;
import com.hireflow.company.CompanyService;
import com.hireflow.mail.EmailService;
import com.hireflow.security.JwtService;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserRepository;
import io.jsonwebtoken.JwtException;
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
class EmailVerificationTest {

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
        lenient().when(encoder.encode(any())).thenReturn("hash");
        lenient().when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jwt.generateEmailToken(any())).thenReturn("signed-token");
        lenient().when(limiter.allow(any(), anyInt(), any())).thenReturn(true);
    }

    private User unverifiedUser() {
        User u = new User("Ann", "ann@example.com", "hash", Role.CANDIDATE);
        u.requireEmailVerification();
        return u;
    }

    @Test
    void passwordSignupIsUnverifiedAndGetsAnEmail() {
        service.register(new RegisterRequest("Ann", "Ann@Example.com", "password123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().isEmailVerified()).isFalse();
        verify(emails).send(eq("ann@example.com"), any());
    }

    @Test
    void validLinkMarksTheUserVerified() {
        User user = unverifiedUser();
        when(jwt.parseEmailToken("good")).thenReturn("ann@example.com");
        when(users.findByEmail("ann@example.com")).thenReturn(Optional.of(user));

        service.verifyEmail("good");

        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void badLinkIsRejected() {
        when(jwt.parseEmailToken("bad")).thenThrow(new JwtException("bad"));

        assertThatThrownBy(() -> service.verifyEmail("bad"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void resendGoesOnlyToUnverifiedAccounts() {
        User verified = new User("Bob", "bob@example.com", "hash", Role.CANDIDATE);   // old account: counts as verified
        when(users.findByEmail("bob@example.com")).thenReturn(Optional.of(verified));
        when(users.findByEmail("ann@example.com")).thenReturn(Optional.of(unverifiedUser()));

        service.resendVerification("bob@example.com");
        verify(emails, never()).send(any(), any());

        service.resendVerification("ann@example.com");
        verify(emails).send(eq("ann@example.com"), any());
    }

    @Test
    void googleSignInVerifiesAnExistingUnverifiedAccount() {
        User user = unverifiedUser();
        when(google.verify("token")).thenReturn(new GoogleAuthService.GoogleUser("sub-1", "ann@example.com", "Ann"));
        when(users.findByEmail("ann@example.com")).thenReturn(Optional.of(user));

        service.googleLogin("token");

        assertThat(user.isEmailVerified()).isTrue();
    }
}