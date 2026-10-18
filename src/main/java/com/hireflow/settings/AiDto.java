package com.hireflow.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Used both as the request body of PUT /settings/ai and as the answer.
public record AiDto(
        @Min(0) @Max(100) int shortlistThreshold,
        boolean showExplanation) {
}