package com.hireflow.notification;

import com.hireflow.common.ApiException;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserRepository;
import com.hireflow.platform.PlatformAccess;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Creates and reads the bell messages. The "to..." methods never throw for a missing person,
// so a notification problem can never break the real action (applying, scheduling, rejecting...).
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    // ---- creating ----

    @Transactional
    public void toUser(Long userId, String kind, String title, String body, String link) {
        notifications.save(new Notification(userId, kind, title, body, link));
    }

    // For people who may not have an account (someone who applied without signing up): then nothing happens.
    @Transactional
    public void toEmail(String email, String kind, String title, String body, String link) {
        if (email == null) return;
        users.findByEmail(email.trim().toLowerCase()).ifPresent(u -> toUser(u.getId(), kind, title, body, link));
    }

    // The hiring team that should see new work: the company's Admins and Recruiters.
// The platform owner is never included: the owner sees usage and billing only, never a company's hiring activity.
    @Transactional
    public void toCompanyStaff(Long companyId, String kind, String title, String body, String link) {
        List<Notification> list = users.findByCompany_IdOrderByIdAsc(companyId).stream()
                .filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.RECRUITER)
                .filter(u -> !PlatformAccess.isMasterEmail(u.getEmail()))
                .map(u -> new Notification(u.getId(), kind, title, body, link))
                .toList();
        notifications.saveAll(list);
    }

    // ---- reading (the logged-in person only) ----

    @Transactional
    public List<NotificationDto> list(String email) {
        User user = find(email);
        notifications.deleteReadBefore(user.getId(), Instant.now().minus(Duration.ofDays(60)));
        return notifications.findTop30ByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream()
                .map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notifications.countByUserIdAndReadAtIsNull(find(email).getId());
    }

    @Transactional
    public void markRead(String email, Long id) {
        Notification n = notifications.findByIdAndUserId(id, find(email).getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.markRead();
    }

    @Transactional
    public void markAllRead(String email) {
        notifications.markAllRead(find(email).getId(), Instant.now());
    }

    private User find(String email) {
        Optional<User> user = users.findByEmail(email);
        return user.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}