package com.hireflow.usage;

import com.hireflow.security.Tenant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usage")
public class CompanyUsageController {

    private final CompanyUsageService service;

    public CompanyUsageController(CompanyUsageService service) {
        this.service = service;
    }

    // GET /api/usage - the company's plan, this month's use, six months of history and its invoices.
    // Admin only: recruiters and hiring managers do not see how much the company uses or pays.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyUsageDto mine() {
        return service.get(Tenant.companyId());
    }
}