package com.hireflow.application;

import java.time.LocalDate;

// What a candidate may see about their own application. Never add AI scores or notes here.
public record ApplicationStatusDto(String reference, String jobTitle, String status, LocalDate appliedAt) {

    public static ApplicationStatusDto from(Application a) {
        return new ApplicationStatusDto(a.getReference(), a.getJob().getTitle(),
                a.getStage().publicStatus(), a.getAppliedAt());
    }
}