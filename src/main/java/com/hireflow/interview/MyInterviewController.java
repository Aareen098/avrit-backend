package com.hireflow.interview;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyInterviewController {

    private final InterviewRepository interviews;

    public MyInterviewController(InterviewRepository interviews) {
        this.interviews = interviews;
    }

    // GET /api/me/interviews - the logged-in candidate's own interviews, soonest first
    @GetMapping("/me/interviews")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<MyInterviewDto> mine(Authentication auth) {
        return interviews.findByApplication_EmailIgnoreCaseOrderByInterviewDateAscInterviewTimeAscIdAsc(auth.getName())
                .stream().map(MyInterviewDto::from).toList();
    }
}