package com.hireflow.interview;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Staff only.
@RestController
@RequestMapping("/interviews")
public class InterviewController {

    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    // GET /api/interviews - all staff roles
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','HIRING_MANAGER')")
    public List<InterviewDto> list() {
        return service.list();
    }

    // POST /api/interviews - Admin and Recruiter only
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public InterviewDto schedule(@Valid @RequestBody InterviewRequest req) {
        return service.schedule(req);
    }
}