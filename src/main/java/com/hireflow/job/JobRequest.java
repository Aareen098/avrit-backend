package com.hireflow.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// What the "Create a job" form sends: { title, department, location, type, description, skills, status }
public record JobRequest(
        @NotBlank @Size(min = 3, max = 150) String title,
        @NotBlank String department,
        @NotBlank String location,
        @NotBlank String type,
        @NotBlank @Size(min = 20, max = 5000, message = "must be 20 to 5000 characters") String description,
        List<String> skills,
        @NotNull JobStatus status) {
}