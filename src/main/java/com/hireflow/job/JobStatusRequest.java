package com.hireflow.job;

import jakarta.validation.constraints.NotNull;

public record JobStatusRequest(@NotNull JobStatus status) {
}