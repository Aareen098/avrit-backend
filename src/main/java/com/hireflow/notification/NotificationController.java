package com.hireflow.notification;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Any logged-in person (candidate, staff or platform owner) sees only their own notifications.
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    // GET /api/notifications -> the latest 30, newest first
    @GetMapping
    public List<NotificationDto> list(Authentication auth) {
        return service.list(auth.getName());
    }

    // GET /api/notifications/unread-count -> { count }
    @GetMapping("/unread-count")
    public Map<String, Long> unread(Authentication auth) {
        return Map.of("count", service.unreadCount(auth.getName()));
    }

    // POST /api/notifications/{id}/read
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(Authentication auth, @PathVariable Long id) {
        service.markRead(auth.getName(), id);
    }

    // POST /api/notifications/read-all
    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(Authentication auth) {
        service.markAllRead(auth.getName());
    }
}