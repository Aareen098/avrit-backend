package com.hireflow.candidate;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationRepository;
import com.hireflow.storage.FileStorageService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Runs at startup and reads the CVs of applications made before CV reading existed.
// A CV that cannot be read is marked (empty text) so it is not tried again on every start.
@Component
public class ResumeBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ResumeBackfill.class);

    private final ApplicationRepository applications;
    private final FileStorageService storage;
    private final ResumeAnalyzer analyzer;

    public ResumeBackfill(ApplicationRepository applications, FileStorageService storage, ResumeAnalyzer analyzer) {
        this.applications = applications;
        this.storage = storage;
        this.analyzer = analyzer;
    }

    @Override
    public void run(String... args) {
        List<Application> todo = applications.findByResumeTextIsNullAndResumeStoredNameNot("");
        for (Application a : todo) {
            try {
                analyzer.fill(a, storage.load(a.getResumeStoredName()));
            } catch (RuntimeException e) {
                log.warn("Skipping resume of application {}: {}", a.getId(), e.getMessage());
                a.setResumeText("");
            }
            applications.save(a);
        }
        if (!todo.isEmpty()) log.info("Read {} existing resume(s)", todo.size());
    }
}