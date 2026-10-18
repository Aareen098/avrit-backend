package com.hireflow.application;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Open to everyone - no login (SecurityConfig permits /public/**).
@RestController
@RequestMapping("/public/applications")
public class PublicApplicationController {

    private final ApplicationService service;

    public PublicApplicationController(ApplicationService service) {
        this.service = service;
    }

    // POST /api/public/applications  (multipart form: jobId, name, email, phone, coverNote, resume)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationCreatedDto apply(@RequestParam(required = false) Long jobId,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String email,
                                       @RequestParam(required = false) String phone,
                                       @RequestParam(required = false) String coverNote,
                                       @RequestParam(name = "resume", required = false) MultipartFile resume) {
        return service.apply(jobId, name, email, phone, coverNote, resume);
    }

    // GET /api/public/applications/status?reference=HF-XXXXXX&email=me@example.com
    @GetMapping("/status")
    public ApplicationStatusDto status(@RequestParam String reference, @RequestParam String email) {
        return service.status(reference, email);
    }
}