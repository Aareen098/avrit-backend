package com.hireflow.broadcast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hireflow.broadcast.BroadcastDtos.Item;
import com.hireflow.broadcast.BroadcastDtos.SendRequest;
import com.hireflow.common.ApiException;
import com.hireflow.company.CompanyRepository;
import com.hireflow.mail.EmailService;
import com.hireflow.notification.NotificationRepository;
import com.hireflow.platform.PlatformAccess;
import com.hireflow.usage.UsageEventRepository;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BroadcastServiceTest {

    private BroadcastRepository broadcasts;
    private UserRepository users;
    private NotificationRepository notifications;
    private EmailService email;
    private UsageEventRepository usageEvents;
    private BroadcastService service;

    @BeforeEach
    void setUp() {
        new PlatformAccess("owner@platform.test");
        broadcasts = mock(BroadcastRepository.class);
        users = mock(UserRepository.class);
        notifications = mock(NotificationRepository.class);
        email = mock(EmailService.class);
        usageEvents = mock(UsageEventRepository.class);
        when(usageEvents.byKind(any(), any())).thenReturn(List.of());
        when(broadcasts.save(any(Broadcast.class))).thenAnswer(i -> i.getArgument(0));
        service = new BroadcastService(broadcasts, users, mock(CompanyRepository.class), notifications, email,
                usageEvents, 300);
    }

    private static User candidate(long id, String mail, boolean verified) {
        User u = new User("Name " + id, mail, "x", Role.CANDIDATE);
        ReflectionTestUtils.setField(u, "id", id);
        if (!verified) u.requireEmailVerification();
        return u;
    }

    private static SendRequest toAllCandidates(boolean inApp, boolean mail) {
        return new SendRequest("ALL_CANDIDATES", "", "Hello", "A message", inApp, mail);
    }

    @Test
    void inAppReachesEveryoneButNeverTheOwner() {
        when(users.findByRole(Role.CANDIDATE)).thenReturn(List.of(
                candidate(1, "a@x.com", true), candidate(2, "b@x.com", false), candidate(3, "owner@platform.test", true)));

        Item item = service.send(toAllCandidates(true, false), "owner@platform.test");

        assertThat(item.inAppCount()).isEqualTo(2);
        verify(notifications).saveAll(any());
        verify(email, never()).send(anyString(), any());
    }

    @Test
    void emailGoesOnlyToVerifiedAddresses() {
        when(users.findByRole(Role.CANDIDATE)).thenReturn(List.of(
                candidate(1, "a@x.com", true), candidate(2, "b@x.com", false)));

        Item item = service.send(toAllCandidates(false, true), "owner@platform.test");

        assertThat(item.emailCount()).isEqualTo(1);
        verify(email, times(1)).send(anyString(), any());
    }

    @Test
    void refusesWhenTheDailyEmailLimitWouldBePassed() {
        when(usageEvents.byKind(any(), any())).thenReturn(java.util.Collections.singletonList(new Object[] {"EMAIL", 299L, 299L}));
        when(users.findByRole(Role.CANDIDATE)).thenReturn(List.of(
                candidate(1, "a@x.com", true), candidate(2, "b@x.com", true)));

        assertThatThrownBy(() -> service.send(toAllCandidates(true, true), "owner@platform.test"))
                .isInstanceOf(ApiException.class).hasMessageContaining("left today");
        verify(email, never()).send(anyString(), any());
        verify(notifications, never()).saveAll(any());
    }

    @Test
    void oneTypedAddressIsEmailedEvenWithoutAnAccount() {
        when(users.findByEmail("new@person.com")).thenReturn(Optional.empty());

        Item item = service.send(new SendRequest("USER", "New@Person.com", "Hi", "Welcome", true, true), "owner@platform.test");

        assertThat(item.inAppCount()).isZero();
        assertThat(item.emailCount()).isEqualTo(1);
        verify(email).send(anyString(), any());
    }

    @Test
    void badInputIsRejected() {
        assertThatThrownBy(() -> service.send(new SendRequest("ALL_STAFF", "", " ", "text", true, false), "o"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.send(new SendRequest("ALL_STAFF", "", "T", "text", false, false), "o"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.send(new SendRequest("USER", "not-an-email", "T", "text", true, false), "o"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.send(new SendRequest("EVERYONE", "", "T", "text", true, false), "o"))
                .isInstanceOf(ApiException.class);
        verify(broadcasts, never()).save(any());
    }
}