package com.hireflow.platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Stops every request from a logged-in person whose company is suspended, even if their token is still valid.
// Not a @Component on purpose: SecurityConfig creates it (same reason as JwtAuthFilter).
public class SuspendedFilter extends OncePerRequestFilter {

    private final SuspendedCompanies suspended;

    public SuspendedFilter(SuspendedCompanies suspended) {
        this.suspended = suspended;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long companyId && suspended.contains(companyId)
                && auth.getAuthorities().stream().noneMatch(g -> g.getAuthority().equals("ROLE_" + PlatformAccess.ROLE))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"This company account is suspended. Please contact support.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}