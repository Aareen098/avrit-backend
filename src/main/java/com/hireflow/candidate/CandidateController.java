package com.hireflow.candidate;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Staff only. Candidates never use these - they have /me/applications instead.
@RestController
@RequestMapping("/candidates")
public class CandidateController {

    private final CandidateService service;

    public CandidateController(CandidateService service) {
        this.service = service;
    }

    // GET /api/candidates
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','HIRING_MANAGER')")
    public List<CandidateDto> list() {
        return service.list();
    }

    // GET /api/candidates/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','HIRING_MANAGER')")
    public CandidateDto get(@PathVariable Long id) {
        return service.get(id);
    }

    // PATCH /api/candidates/{id}/stage  { "stage": "REJECTED", "reason": "Skills mismatch", "note": "..." }
    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public CandidateDto updateStage(@PathVariable Long id, @Valid @RequestBody StageUpdateRequest req) {
        return service.updateStage(id, req);
    }

    // POST /api/candidates  (multipart: name, email, phone, job or jobId, experience, skills, resume)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public CandidateDto add(@RequestParam(required = false) Long jobId,
                            @RequestParam(name = "job", required = false) String job,
                            @RequestParam(required = false) String name,
                            @RequestParam(required = false) String email,
                            @RequestParam(required = false) String phone,
                            @RequestParam(required = false) String experience,
                            @RequestParam(required = false) String skills,
                            @RequestParam(name = "resume", required = false) MultipartFile resume) {
        return service.add(jobId, job, name, email, phone, experience, skills, resume);
    }

    // POST /api/candidates/{id}/ai-evaluate - (re)runs the AI analysis of the CV and waits for it
    @PostMapping("/{id}/ai-evaluate")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public CandidateDto aiEvaluate(@PathVariable Long id) {
        return service.aiEvaluate(id);
    }

    // GET /api/candidates/{id}/resume - downloads the original CV (needs the login token, so the
    // frontend fetches it as a file instead of using a plain link).
    @GetMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','HIRING_MANAGER')")
    public ResponseEntity<Resource> resume(@PathVariable Long id) {
        ResumeFile file = service.resume(id);
        return ResponseEntity.ok()
                .contentType(file.type())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }
}