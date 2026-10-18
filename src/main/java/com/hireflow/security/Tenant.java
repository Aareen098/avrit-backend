package com.hireflow.security;

import com.hireflow.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// The company of the logged-in staff member. It comes from the signed token only - never from
// the request - so nobody can ask for another company's data.
public final class Tenant {

    private Tenant() {
    }

    public static Long companyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long id) return id;
        throw new ApiException(HttpStatus.FORBIDDEN, "This account does not belong to a company");
    }
}