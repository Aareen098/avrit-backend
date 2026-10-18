package com.hireflow.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// What the chat box sends: { message }
public record ChatRequest(@NotBlank @Size(max = 500) String message) {
}