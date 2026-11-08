package com.hireflow.platform;

import com.hireflow.company.CompanyRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

// The ids of suspended companies, re-read from the database at most every 30 seconds.
// This runs on every request, so it must not hit the database each time. With several servers,
// each one notices a change within 30 seconds.
@Component
public class SuspendedCompanies {

    private static final long MAX_AGE_MS = 30_000;

    private final CompanyRepository companies;
    private volatile Set<Long> ids = Set.of();
    private volatile long loadedAt = 0;

    public SuspendedCompanies(CompanyRepository companies) {
        this.companies = companies;
    }

    public boolean contains(Long companyId) {
        if (System.currentTimeMillis() - loadedAt > MAX_AGE_MS) refresh();
        return ids.contains(companyId);
    }

    public synchronized void refresh() {
        ids = new HashSet<>(companies.findSuspendedIds());
        loadedAt = System.currentTimeMillis();
    }
}