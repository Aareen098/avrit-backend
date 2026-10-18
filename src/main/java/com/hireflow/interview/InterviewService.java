package com.hireflow.interview;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationRepository;
import com.hireflow.application.ApplicationStage;
import com.hireflow.common.ApiException;
import com.hireflow.security.Tenant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewService {

    private static final Set<String> TYPES = Set.of("Video call", "Phone call", "On-site");

    private final InterviewRepository interviews;
    private final ApplicationRepository applications;

    public InterviewService(InterviewRepository interviews, ApplicationRepository applications) {
        this.interviews = interviews;
        this.applications = applications;
    }

    public List<InterviewDto> list() {
        return interviews.findByApplication_Job_Company_IdOrderByInterviewDateDescInterviewTimeDescIdDesc(Tenant.companyId())
                .stream()
                .map(InterviewDto::from)
                .toList();
    }

    // Saves the interview and, if the candidate is still early in the process, moves them to Interview.
    @Transactional
    public InterviewDto schedule(InterviewRequest req) {
        if (!TYPES.contains(req.type().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Type must be Video call, Phone call or On-site");
        }
        if (req.date().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pick today or a later date");
        }
        Application a = applications.findByIdAndJob_Company_Id(req.candidateId(), Tenant.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Candidate not found"));
        if (a.getStage() == ApplicationStage.REJECTED || a.getStage() == ApplicationStage.HIRED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This candidate is already rejected or hired");
        }

        // A candidate who already has an offer stays at Offer.
        if (a.getStage() == ApplicationStage.APPLIED || a.getStage() == ApplicationStage.SCREENING) {
            a.setStage(ApplicationStage.INTERVIEW);
        }

        String notes = req.notes() == null || req.notes().isBlank() ? null : req.notes().trim();
        Interview saved = interviews.save(new Interview(a, req.date(), req.time(),
                req.type().trim(), req.interviewer().trim(), notes));
        return InterviewDto.from(saved);
    }
}