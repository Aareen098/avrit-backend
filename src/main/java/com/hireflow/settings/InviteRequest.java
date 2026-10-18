package com.hireflow.settings;

import com.hireflow.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteRequest(@NotBlank @Email String email, @NotNull Role role) {
}