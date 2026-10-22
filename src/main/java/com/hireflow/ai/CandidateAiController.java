package com.hireflow.ai;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/ai")
@PreAuthorize("hasRole('CANDIDATE')")
public class CandidateAiController {

    public record MatchRequest(Long jobId) {
    }

    private final CandidateAiService service;

    public CandidateAiController(CandidateAiService service) {
        this.service = service;
    }

    // POST /api/me/ai/resume-feedback - AI review of the resume on the candidate's profile
    @PostMapping("/resume-feedback")
    public CandidateAiService.ResumeFeedback resumeFeedback(Authentication auth) {
        return service.resumeFeedback(auth.getName());
    }

    // POST /api/me/ai/best-matches - ranks all open jobs (not yet applied to) by how well the resume fits
    @PostMapping("/best-matches")
    public List<CandidateAiService.JobFit> bestMatches(Authentication auth) {
        return service.bestMatches(auth.getName());
    }

    // POST /api/me/ai/job-match  { jobId } - how well that resume fits one open job
    @PostMapping("/job-match")
    public CandidateAiService.JobMatch jobMatch(Authentication auth, @RequestBody MatchRequest req) {
        return service.jobMatch(auth.getName(), req.jobId());
    }
}