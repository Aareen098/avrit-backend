package com.hireflow.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// What the "Create your workspace" form sends. "jobTitle" is also sent but not stored yet.
public record RegisterCompanyRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 300) @Pattern(regexp = "^https?://\\S+$", message = "must start with http:// or https://") String website,
        @Size(max = 100) String size,
        @Size(max = 100) String industry,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password) {
}