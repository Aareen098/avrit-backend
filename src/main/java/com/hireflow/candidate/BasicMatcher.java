package com.hireflow.candidate;

import com.hireflow.application.Application;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// TEMPORARY scoring: compares the candidate's skills with the skills the job asks for.
// It is replaced by the real CV analysis (AI) in a later batch. Returns null when there is nothing to compare.
final class BasicMatcher {

    private BasicMatcher() {
    }

    static MatchDto evaluate(Application a) {
        List<String> required = a.getJob().getSkills();
        if (required.isEmpty() || a.getSkills().isEmpty()) return null;

        List<String> have = a.getSkills().stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : required) {
            (have.contains(skill.toLowerCase(Locale.ROOT)) ? matched : missing).add(skill);
        }

        List<String> reasons = new ArrayList<>();
        reasons.add(matched.size() + " of " + required.size() + " required skills found");
        if (a.getExperienceYears() != null) reasons.add(a.getExperienceYears() + " years of experience");
        reasons.add("Basic skill match - the full CV analysis comes in a later update");
        return new MatchDto(reasons, matched, missing);
    }

    static Integer score(MatchDto match) {
        if (match == null) return null;
        int total = match.matched().size() + match.missing().size();
        return Math.round(100f * match.matched().size() / total);
    }
}