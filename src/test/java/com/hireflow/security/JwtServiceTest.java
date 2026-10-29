package com.hireflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hireflow.user.Role;
import com.hireflow.user.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("test-secret-that-is-at-least-32-characters-long", 60);

    @Test
    void emailTokenGivesBackTheEmail() {
        assertThat(jwt.parseEmailToken(jwt.generateEmailToken("ann@example.com"))).isEqualTo("ann@example.com");
    }

    @Test
    void loginTokenCannotVerifyAnEmail() {
        String login = jwt.generate(new User("Ann", "ann@example.com", "hash", Role.CANDIDATE));

        assertThatThrownBy(() -> jwt.parseEmailToken(login)).isInstanceOf(JwtException.class);
    }

    @Test
    void emailTokenHasNoRoleSoItCannotLogAnyoneIn() {
        String token = jwt.generateEmailToken("ann@example.com");

        assertThat(jwt.parse(token).get("role", String.class)).isNull();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.generateEmailToken("ann@example.com");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwt.parseEmailToken(tampered)).isInstanceOf(JwtException.class);
    }
}