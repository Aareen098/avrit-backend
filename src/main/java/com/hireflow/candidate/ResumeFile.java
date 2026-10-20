package com.hireflow.candidate;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record ResumeFile(Resource resource, String fileName, MediaType type) {

    // Picks the download type from the stored file's extension.
    public static ResumeFile of(Resource resource, String fileName, String storedName) {
        String stored = storedName.toLowerCase();
        MediaType type = stored.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : stored.endsWith(".docx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                : MediaType.parseMediaType("application/msword");
        return new ResumeFile(resource, fileName, type);
    }
}