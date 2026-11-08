package com.hireflow.platform;

import com.hireflow.billing.BillingService;
import com.hireflow.billing.InvoiceDto;
import com.hireflow.common.ApiException;
import com.hireflow.platform.PlatformDtos.CompanyDetail;
import com.hireflow.platform.PlatformDtos.CompanyRow;
import com.hireflow.platform.PlatformDtos.GenerateResult;
import com.hireflow.platform.PlatformDtos.InvoiceStatusRequest;
import com.hireflow.platform.PlatformDtos.Overview;
import com.hireflow.platform.PlatformDtos.PeriodRequest;
import com.hireflow.platform.PlatformDtos.PlanInfo;
import com.hireflow.platform.PlatformDtos.PlanRequest;
import com.hireflow.platform.PlatformDtos.SuspensionRequest;
import com.hireflow.broadcast.BroadcastDtos;
import com.hireflow.broadcast.BroadcastService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Only the platform owner. Two checks: the token role, and the owner's email in the server configuration.
// The second check means removing MASTER_ADMIN_EMAIL locks everyone out as soon as the server restarts.
@RestController
@RequestMapping("/platform")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformController {

    private final PlatformService service;
    private final BillingService billing;
    private final BroadcastService broadcasts;

    public PlatformController(PlatformService service, BillingService billing, BroadcastService broadcasts) {
        this.service = service;
        this.billing = billing;
        this.broadcasts = broadcasts;
    }

    // GET /api/platform/overview
    @GetMapping("/overview")
    public Overview overview(Authentication auth) {
        owner(auth);
        return service.overview();
    }

    // GET /api/platform/companies
    @GetMapping("/companies")
    public List<CompanyRow> companies(Authentication auth) {
        owner(auth);
        return service.companies();
    }

    // GET /api/platform/companies/{id}
    @GetMapping("/companies/{id}")
    public CompanyDetail company(Authentication auth, @PathVariable Long id) {
        owner(auth);
        return service.company(id);
    }

    // PATCH /api/platform/companies/{id}/suspension  { suspended: true|false }
    @PatchMapping("/companies/{id}/suspension")
    public CompanyRow suspension(Authentication auth, @PathVariable Long id, @RequestBody SuspensionRequest req) {
        owner(auth);
        return service.setSuspended(id, req.suspended(), auth.getName());
    }

    // PATCH /api/platform/companies/{id}/plan  { planCode: "FREE" | "PRO" | "BUSINESS" }
    @PatchMapping("/companies/{id}/plan")
    public CompanyRow plan(Authentication auth, @PathVariable Long id, @RequestBody PlanRequest req) {
        owner(auth);
        return service.setPlan(id, req.planCode(), auth.getName());
    }

    // GET /api/platform/plans
    @GetMapping("/plans")
    public List<PlanInfo> plans(Authentication auth) {
        owner(auth);
        return service.plans();
    }

    // GET /api/platform/invoices?period=2026-10   (no period = the latest 200)
    @GetMapping("/invoices")
    public List<InvoiceDto> invoices(Authentication auth, @RequestParam(required = false) String period) {
        owner(auth);
        return billing.list(period);
    }

    // POST /api/platform/invoices/generate  { period: "2026-10" } -> { created }
    @PostMapping("/invoices/generate")
    public GenerateResult generate(Authentication auth, @RequestBody PeriodRequest req) {
        owner(auth);
        return new GenerateResult(billing.generate(req.period()));
    }

    // PATCH /api/platform/invoices/{id}/status  { status: "PAID" | "VOID" }
    @PatchMapping("/invoices/{id}/status")
    public InvoiceDto invoiceStatus(Authentication auth, @PathVariable Long id, @RequestBody InvoiceStatusRequest req) {
        owner(auth);
        return billing.setStatus(id, req.status());
    }

    // POST /api/platform/broadcasts/preview  -> who it would reach, nothing is sent
    @PostMapping("/broadcasts/preview")
    public BroadcastDtos.Preview broadcastPreview(Authentication auth, @RequestBody BroadcastDtos.SendRequest req) {
        owner(auth);
        return broadcasts.preview(req);
    }

    // POST /api/platform/broadcasts  { audience, target, title, message, inApp, email }
    @PostMapping("/broadcasts")
    public BroadcastDtos.Item broadcastSend(Authentication auth, @RequestBody BroadcastDtos.SendRequest req) {
        owner(auth);
        return broadcasts.send(req, auth.getName());
    }

    // GET /api/platform/broadcasts -> the latest 50 sends
    @GetMapping("/broadcasts")
    public List<BroadcastDtos.Item> broadcastHistory(Authentication auth) {
        owner(auth);
        return broadcasts.history();
    }

    private static void owner(Authentication auth) {
        if (!PlatformAccess.isMasterEmail(auth.getName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Platform owner only");
        }
    }
}