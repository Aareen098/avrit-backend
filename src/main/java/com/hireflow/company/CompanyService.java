package com.hireflow.company;

import java.text.Normalizer;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository companies;

    public CompanyService(CompanyRepository companies) {
        this.companies = companies;
    }

    // "Zeta Inc" -> zeta-inc; if taken, zeta-inc-2, zeta-inc-3 ...
    public Company create(String name, String website, String contactEmail, String size, String industry) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFKD)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isEmpty()) base = "company";
        if (base.length() > 60) base = base.substring(0, 60).replaceAll("-+$", "");
        String slug = base;
        for (int n = 2; companies.existsBySlug(slug); n++) slug = base + "-" + n;
        return companies.save(new Company(name.trim(), slug, website.trim(), contactEmail, blankToNull(size), blankToNull(industry)));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}