package com.hireflow.usage;

// What is being measured. Plain text on purpose (no enum), so a new kind never needs a database change.
public final class UsageKind {

    public static final String LLM_SCREENING = "LLM_SCREENING";   // AI scoring of an application (units = tokens)
    public static final String LLM_ASSISTANT = "LLM_ASSISTANT";   // the recruiter AI assistant (units = tokens)
    public static final String LLM_CANDIDATE = "LLM_CANDIDATE";   // candidate-side AI, belongs to no company (units = tokens)
    public static final String EMAIL = "EMAIL";                   // one email sent (units = 1), belongs to no company
    public static final String CV_STORAGE = "CV_STORAGE";         // a CV file stored (units = bytes)

    private UsageKind() {
    }
}