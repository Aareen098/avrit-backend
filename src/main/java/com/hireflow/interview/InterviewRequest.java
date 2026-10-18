package com.hireflow.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

// Body of POST /interviews. Extra fields the form sends (candidateName, job) are ignored.
public record InterviewRequest(
        @NotNull Long candidateId,
        @NotNull LocalDate date,
        @NotNull LocalTime time,
        @NotBlank String type,
        @NotBlank @Size(max = 100) String interviewer,
        @Size(max = 2000) String notes) {
}