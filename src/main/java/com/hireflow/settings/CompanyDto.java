package com.hireflow.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Used both as the request body of PUT /settings/company and as the answer.
public record CompanyDto(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 300) @Pattern(regexp = "^https?://\\S+$", message = "must start with http:// or https://") String website,
        @NotBlank @Email @Size(max = 200) String contactEmail) {
}