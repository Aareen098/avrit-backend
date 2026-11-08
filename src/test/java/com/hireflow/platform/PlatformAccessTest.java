package com.hireflow.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.hireflow.user.Role;
import com.hireflow.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlatformAccessTest {

    @AfterEach
    void reset() {
        new PlatformAccess("");   // the owner email is a static setting, so clean it up after each test
    }

    @Test
    void verifiedAccountWithTheOwnerEmailBecomesPlatformOwner() {
        new PlatformAccess("Owner@Avrit.co.in");
        User user = new User("Owner", "owner@avrit.co.in", "hash", Role.CANDIDATE);

        assertThat(user.effectiveRole()).isEqualTo("PLATFORM_ADMIN");
    }

    @Test
    void unverifiedAccountWithTheOwnerEmailIsNotTheOwner() {
        new PlatformAccess("owner@avrit.co.in");
        User user = new User("Fake", "owner@avrit.co.in", "hash", Role.CANDIDATE);
        user.requireEmailVerification();   // signed up with a password and never clicked the link

        assertThat(user.effectiveRole()).isEqualTo("CANDIDATE");
    }

    @Test
    void otherAccountsKeepTheirRole() {
        new PlatformAccess("owner@avrit.co.in");

        assertThat(new User("A", "a@x.com", "h", Role.ADMIN).effectiveRole()).isEqualTo("ADMIN");
    }

    @Test
    void emptySettingMeansNobodyIsOwner() {
        new PlatformAccess("");

        assertThat(PlatformAccess.isMasterEmail("")).isFalse();
        assertThat(PlatformAccess.isMasterEmail(null)).isFalse();
        assertThat(new User("A", "a@x.com", "h", Role.CANDIDATE).effectiveRole()).isEqualTo("CANDIDATE");
    }
}