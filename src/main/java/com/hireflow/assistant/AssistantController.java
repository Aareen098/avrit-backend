package com.hireflow.assistant;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Staff only.
@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final AssistantService service;

    public AssistantController(AssistantService service) {
        this.service = service;
    }

    // POST /api/assistant/chat  { "message": "Who knows Java?" }
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','HIRING_MANAGER')")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return service.chat(req.message().trim());
    }
}