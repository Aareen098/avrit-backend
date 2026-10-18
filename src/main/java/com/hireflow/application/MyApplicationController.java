package com.hireflow.application;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}