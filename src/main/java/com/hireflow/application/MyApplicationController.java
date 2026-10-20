package com.hireflow.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyApplicationController {

    private final ApplicationService service;

    public MyApplicationController(ApplicationService service) {
        this.service = service;
    }

    // GET /api/me/applications - the logged-in candidate's own applications (matched by email)
    @GetMapping("/me/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<ApplicationStatusDto> mine(Authentication auth) {
        return service.mine(auth.getName());
    }

    // GET /api/me/applications/{reference} - one of their own applications, with the job details
    @GetMapping("/me/applications/{reference}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationDetailDto detail(Authentication auth, @PathVariable String reference) {
        return service.detail(auth.getName(), reference);
    }

    // POST /api/me/applications  { jobId, coverNote } - apply using the saved profile and resume
    @PostMapping("/me/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationCreatedDto apply(Authentication auth, @RequestBody ApplyRequest req) {
        return service.applyAsCandidate(auth.getName(), req.jobId(), req.coverNote());
    }
}