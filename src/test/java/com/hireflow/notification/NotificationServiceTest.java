package com.hireflow.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hireflow.common.ApiException;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repo;
    @Mock UserRepository users;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repo, users);
    }

    @Test
    void personWithoutAnAccountIsIgnoredAndEmailIsLowercased() {
        when(users.findByEmail("nobody@x.com")).thenReturn(Optional.empty());

        service.toEmail("Nobody@X.com", "INTERVIEW", "Title", "Body", "/x");

        verify(users).findByEmail("nobody@x.com");
        verify(repo, never()).save(any());
    }

    @Test
    void companyAlertsGoOnlyToAdminsAndRecruiters() {
        List<Notification> saved = new ArrayList<>();
        when(repo.saveAll(any())).thenAnswer(inv -> {
            Iterable<Notification> in = inv.getArgument(0);
            in.forEach(saved::add);
            return saved;
        });
        when(users.findByCompany_IdOrderByIdAsc(5L)).thenReturn(List.of(
                new User("A", "a@x.com", "h", Role.ADMIN),
                new User("R", "r@x.com", "h", Role.RECRUITER),
                new User("M", "m@x.com", "h", Role.HIRING_MANAGER)));

        service.toCompanyStaff(5L, "NEW_APPLICATION", "New application", "Body", "/candidates/1");

        assertThat(saved).hasSize(2);
    }

    @Test
    void cannotMarkSomeoneElsesNotificationAsRead() {
        when(users.findByEmail("a@x.com")).thenReturn(Optional.of(new User("A", "a@x.com", "h", Role.CANDIDATE)));
        when(repo.findByIdAndUserId(eq(9L), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead("a@x.com", 9L)).isInstanceOf(ApiException.class);
    }
}