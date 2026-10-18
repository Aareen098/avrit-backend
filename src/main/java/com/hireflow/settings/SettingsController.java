package com.hireflow.settings;

import com.hireflow.user.UserDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    // GET /api/settings - all staff roles (the Candidates pages read the AI settings)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','HIRING_MANAGER')")
    public SettingsDto get() {
        return service.get();
    }

    // PUT /api/settings/company - Admin only
    @PutMapping("/company")
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyDto saveCompany(@Valid @RequestBody CompanyDto req) {
        return service.saveCompany(req);
    }

    // PUT /api/settings/ai - Admin only
    @PutMapping("/ai")
    @PreAuthorize("hasRole('ADMIN')")
    public AiDto saveAi(@Valid @RequestBody AiDto req) {
        return service.saveAi(req);
    }

    // POST /api/settings/team/invite - Admin only
    @PostMapping("/team/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> invite(@Valid @RequestBody InviteRequest req) {
        return service.invite(req);
    }
}