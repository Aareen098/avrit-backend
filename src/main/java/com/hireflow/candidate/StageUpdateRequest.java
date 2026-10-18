package com.hireflow.candidate;

import com.hireflow.application.ApplicationStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Body of PATCH /candidates/{id}/stage. reason and note only matter when stage is REJECTED.
public record StageUpdateRequest(
        @NotNull ApplicationStage stage,
        @Size(max = 100) String reason,
        @Size(max = 2000) String note) {
}