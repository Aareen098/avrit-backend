package com.hireflow.application;

// The stages staff move a candidate through (Pipeline page).
public enum ApplicationStage {
    APPLIED,
    SCREENING,
    INTERVIEW,
    OFFER,
    HIRED,
    REJECTED;

    // What the candidate sees: APPLIED, IN_REVIEW, INTERVIEW, OFFER or REJECTED.
    public String publicStatus() {
        return switch (this) {
            case APPLIED -> "APPLIED";
            case SCREENING -> "IN_REVIEW";
            case INTERVIEW -> "INTERVIEW";
            case OFFER, HIRED -> "OFFER";
            case REJECTED -> "REJECTED";
        };
    }
}