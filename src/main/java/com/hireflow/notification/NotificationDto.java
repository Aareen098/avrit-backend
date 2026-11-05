package com.hireflow.notification;

import java.time.Instant;

public record NotificationDto(Long id, String kind, String title, String body, String link,
                              Instant createdAt, boolean read) {

    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getKind(), n.getTitle(), n.getBody(), n.getLink(),
                n.getCreatedAt(), n.getReadAt() != null);
    }
}