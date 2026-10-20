package com.hireflow.user;

import com.hireflow.candidate.ResumeFile;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/me/profile")
@PreAuthorize("hasRole('CANDIDATE')")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    // GET /api/me/profile
    @GetMapping
    public ProfileDto get(Authentication auth) {
        return service.get(auth.getName());
    }

    // PUT /api/me/profile  (multipart: name, phone, location, headline, about, linkedin, github, portfolio, resume)
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileDto save(Authentication auth,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String location,
                           @RequestParam(required = false) String headline,
                           @RequestParam(required = false) String about,
                           @RequestParam(required = false) String linkedin,
                           @RequestParam(required = false) String github,
                           @RequestParam(required = false) String portfolio,
                           @RequestParam(name = "resume", required = false) MultipartFile resume) {
        return service.save(auth.getName(), name, phone, location, headline, about, linkedin, github, portfolio, resume);
    }

    // GET /api/me/profile/resume - downloads the resume on the profile
    @GetMapping("/resume")
    public ResponseEntity<Resource> resume(Authentication auth) {
        ResumeFile file = service.resume(auth.getName());
        return ResponseEntity.ok()
                .contentType(file.type())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }
}