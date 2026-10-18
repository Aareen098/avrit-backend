package com.hireflow.candidate;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationStage;
import java.time.LocalDate;
import java.util.List;

// Staff view of one application. Same field names the React pages already use.
// aiScore and match are null when they cannot be worked out yet.
public record CandidateDto(
        Long id,
        String name,
        String email,
        String phone,
        String location,
        Long jobId,
        String job,
        ApplicationStage stage,
        Integer aiScore,
        int experience,
        List<String> skills,
        String summary,
        LocalDate appliedAt,
        String resume,
        boolean hasResume,
        String rejectionReason,
        String rejectionNote,
        MatchDto match,
        String resumeText) {

    // resumeText is only sent for a single candidate (the profile page), never in the list.
    public static CandidateDto from(Application a, Integer score, MatchDto match, String resumeText) {
        boolean hasResume = !a.getResumeStoredName().isEmpty();
        return new CandidateDto(
                a.getId(), a.getName(), a.getEmail(), nz(a.getPhone()), nz(a.getLocation()),
                a.getJob().getId(), a.getJob().getTitle(), a.getStage(), score,
                a.getExperienceYears() == null ? 0 : a.getExperienceYears(),
                List.copyOf(a.getSkills()), nz(a.getSummary()), a.getAppliedAt(),
                hasResume ? a.getResumeFileName() : null, hasResume,
                a.getRejectionReason(), a.getRejectionNote(), match,
                resumeText == null || resumeText.isBlank() ? null : resumeText);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}