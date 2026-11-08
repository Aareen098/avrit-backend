package com.hireflow.billing;

// The plans companies can be on. Prices are in rupees per month.
// A plan includes an allowance. Going over is not blocked: it is billed as "extra use" on the invoice.
// To change a price or allowance, edit this file. Companies store only the plan CODE (FREE, PRO, BUSINESS).
public enum Plan {

    FREE("Free", 0, 3, 50, 50),
    PRO("Pro", 2999, 10, 500, 500),
    BUSINESS("Business", 9999, 50, 5000, 5000);

    public static final long EXTRA_APPLICATION_INR = 1;   // each application above the allowance
    public static final long EXTRA_AI_CALL_INR = 2;       // each AI call above the allowance

    private final String label;
    private final long priceInr;
    private final int seats;
    private final long applicationsIncluded;
    private final long aiCallsIncluded;

    Plan(String label, long priceInr, int seats, long applicationsIncluded, long aiCallsIncluded) {
        this.label = label;
        this.priceInr = priceInr;
        this.seats = seats;
        this.applicationsIncluded = applicationsIncluded;
        this.aiCallsIncluded = aiCallsIncluded;
    }

    public String label() { return label; }
    public long priceInr() { return priceInr; }
    public int seats() { return seats; }
    public long applicationsIncluded() { return applicationsIncluded; }
    public long aiCallsIncluded() { return aiCallsIncluded; }

    // Unknown or empty codes count as FREE.
    public static Plan of(String code) {
        for (Plan p : values()) if (p.name().equals(code)) return p;
        return FREE;
    }

    public static boolean isValid(String code) {
        for (Plan p : values()) if (p.name().equals(code)) return true;
        return false;
    }
}