package com.hireflow.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

// What the "Edit job" form sends. The status is not here: it only changes through the Close/Publish/Reopen button.
public record JobUpdateRequest(
        @NotBlank @Size(min = 3, max = 150) String title,
        @NotBlank String department,
        @NotBlank String location,
        @NotBlank String type,
        @NotBlank @Size(min = 20, max = 5000, message = "must be 20 to 5000 characters") String description,
        List<String> skills) {
}