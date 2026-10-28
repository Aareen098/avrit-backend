package com.hireflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hireflow.common.ApiException;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordTokenServiceTest {

    private final List<PasswordToken> store = new ArrayList<>();   // acts like the password_tokens table
    private PasswordTokenService service;
    private final User user = new User("Ann", "ann@example.com", "hash", Role.CANDIDATE);

    @BeforeEach
    void setUp() {
        PasswordTokenRepository repo = mock(PasswordTokenRepository.class);
        when(repo.save(any(PasswordToken.class))).thenAnswer(inv -> {
            store.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(repo.findByTokenHash(any())).thenAnswer(inv -> store.stream()
                .filter(t -> t.getTokenHash().equals(inv.getArgument(0))).findFirst());
        doAnswer(inv -> { store.clear(); return null; }).when(repo).deleteForUser(any());
        service = new PasswordTokenService(repo);
    }

    @Test
    void linkWorksOnlyOnce() {
        String secret = service.issue(user, Duration.ofMinutes(15));

        assertThat(service.consume(secret)).isSameAs(user);
        assertThatThrownBy(() -> service.consume(secret)).isInstanceOf(ApiException.class);
    }

    @Test
    void newLinkKillsTheOldOne() {
        String first = service.issue(user, Duration.ofMinutes(15));
        String second = service.issue(user, Duration.ofMinutes(15));

        assertThatThrownBy(() -> service.consume(first)).isInstanceOf(ApiException.class);
        assertThat(service.consume(second)).isSameAs(user);
    }

    @Test
    void expiredLinkIsRejected() {
        String secret = service.issue(user, Duration.ofMinutes(-1));

        assertThatThrownBy(() -> service.consume(secret)).isInstanceOf(ApiException.class);
    }

    @Test
    void unknownLinkIsRejected() {
        assertThatThrownBy(() -> service.consume("not-a-real-secret")).isInstanceOf(ApiException.class);
    }

    @Test
    void secretIsStoredOnlyAsHash() {
        String secret = service.issue(user, Duration.ofMinutes(15));

        String stored = store.get(0).getTokenHash();
        assertThat(stored).isNotEqualTo(secret).hasSize(64);   // SHA-256 in hex
    }
}