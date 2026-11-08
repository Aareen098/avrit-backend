package com.hireflow.broadcast;

import java.time.Instant;

public final class BroadcastDtos {

    private BroadcastDtos() {
    }

    // audience: USER | COMPANY | ALL_STAFF | ALL_CANDIDATES
    // target: the email address for USER, the company id for COMPANY, empty for the others
    public record SendRequest(String audience, String target, String title, String message, boolean inApp, boolean email) {
    }

    public record Preview(String audienceLabel, int inAppCount, int emailCount, long emailsLeftToday) {
    }

    public record Item(Long id, String audience, String audienceLabel, String title, String message, boolean inApp,
                       boolean email, int inAppCount, int emailCount, Instant createdAt) {
        static Item from(Broadcast b) {
            return new Item(b.getId(), b.getAudience(), b.getAudienceLabel(), b.getTitle(), b.getMessage(), b.isInApp(),
                    b.isEmail(), b.getInAppCount(), b.getEmailCount(), b.getCreatedAt());
        }
    }
}