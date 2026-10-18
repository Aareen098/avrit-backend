package com.hireflow.candidate;

import java.util.List;

// "Why this score" box on the candidate profile.
public record MatchDto(List<String> reasons, List<String> matched, List<String> missing) {
}