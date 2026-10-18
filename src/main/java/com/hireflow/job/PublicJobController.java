package com.hireflow.job;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Open to everyone - no login (SecurityConfig permits /public/**). Shows OPEN jobs only.
@RestController
@RequestMapping("/public/jobs")
public class PublicJobController {

    private final JobService service;

    public PublicJobController(JobService service) {
        this.service = service;
    }

    // GET /api/public/jobs
    @GetMapping
    public List<PublicJobDto> list() {
        return service.listPublic();
    }

    // GET /api/public/jobs/{id}
    @GetMapping("/{id}")
    public PublicJobDto get(@PathVariable Long id) {
        return service.getPublic(id);
    }
}