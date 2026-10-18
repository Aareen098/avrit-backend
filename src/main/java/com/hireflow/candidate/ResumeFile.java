package com.hireflow.candidate;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record ResumeFile(Resource resource, String fileName, MediaType type) {
}