package com.hireflow.candidate;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationRepository;
import com.hireflow.application.ApplicationService;
import com.hireflow.application.ApplicationStage;
import com.hireflow.common.ApiException;
import com.hireflow.job.Job;
import com.hireflow.job.JobRepository;
import com.hireflow.job.JobStatus;
import com.hireflow.storage.FileStorageService;
import com.hireflow.security.Tenant;
import com.hireflow.ai.AiScreeningService;
import com.hireflow.mail.EmailService;
import com.hireflow.mail.EmailTemplates;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CandidateService {

    private final ApplicationRepository applications;
    private final ApplicationService applicationService;
    private final JobRepository jobs;
    private final FileStorageService storage;
    private final AiScreeningService screening;
    private final EmailService emails;

    public CandidateService(ApplicationRepository applications, ApplicationService applicationService,
                            JobRepository jobs, FileStorageService storage, AiScreeningService screening,
                            EmailService emails) {
        this.emails = emails;
        this.screening = screening;
        this.applications = applications;
        this.applicationService = applicationService;
        this.jobs = jobs;
        this.storage = storage;
    }

    public List<CandidateDto> list() {
        return applications.findByJob_Company_IdOrderByAppliedAtDescIdDesc(Tenant.companyId())
                .stream().map(a -> toDto(a, false)).toList();
    }

    public CandidateDto get(Long id) {
        return toDto(find(id), true);
    }

    // Pipeline drag-and-drop and the Reject dialog both end up here.
    @Transactional
    public CandidateDto updateStage(Long id, StageUpdateRequest req) {
        Application a = find(id);
        if (req.stage() == ApplicationStage.REJECTED) {
            if (req.reason() == null || req.reason().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a reason for rejecting");
            }
            a.setRejectionReason(req.reason().trim());
            a.setRejectionNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        } else {
            // Moving out of Rejected (or any other stage) clears the old reason.
            a.setRejectionReason(null);
            a.setRejectionNote(null);
        }
        boolean newlyRejected = req.stage() == ApplicationStage.REJECTED && a.getStage() != ApplicationStage.REJECTED;
        a.setStage(req.stage());
        if (newlyRejected) {
            emails.send(a.getEmail(), EmailTemplates.rejected(a.getName(), a.getJob().getTitle(),
                    a.getJob().getCompany().getName()));
        }
        return toDto(a, false);
    }

    // Add Candidate dialog. The job is chosen by jobId, or by its title (what the current form sends).
    public CandidateDto add(Long jobId, String jobTitle, String name, String email, String phone,
                            String experience, String skills, MultipartFile resume) {
        Job job = resolveJob(jobId, jobTitle);
        Application saved = applicationService.addByStaff(job, name, email, phone,
                parseExperience(experience), parseSkills(skills), resume);
        return toDto(saved, false);
    }

    public ResumeFile resume(Long id) {
        Application a = find(id);
        if (a.getResumeStoredName().isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "This candidate has no resume on file");
        }
        String stored = a.getResumeStoredName().toLowerCase();
        MediaType type = stored.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : stored.endsWith(".docx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                : MediaType.parseMediaType("application/msword");
        return new ResumeFile(storage.load(a.getResumeStoredName()), a.getResumeFileName(), type);
    }

    // ---- helpers ----

    private Application find(Long id) {
        return applications.findByIdAndJob_Company_Id(id, Tenant.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Candidate not found"));
    }

    // "Run AI analysis" button. find() first, so another company's candidate is a 404.
    public CandidateDto aiEvaluate(Long id) {
        find(id);
        return toDto(screening.evaluate(id), true);
    }

    // The score shown for a candidate: the AI score if there is one, otherwise the basic skill match.
    public Integer score(Application a) {
        return a.getAiScore() != null ? a.getAiScore() : BasicMatcher.score(BasicMatcher.evaluate(a));
    }

    private CandidateDto toDto(Application a, boolean withResumeText) {
        // AI result if the LLM has analysed this CV, otherwise the basic skill match.
        if (a.getAiScore() != null) {
            MatchDto ai = new MatchDto(a.getAiReasons(), a.getAiMatched(), a.getAiMissing());
            return CandidateDto.from(a, a.getAiScore(), ai, withResumeText ? a.getResumeText() : null);
        }
        MatchDto match = BasicMatcher.evaluate(a);
        return CandidateDto.from(a, BasicMatcher.score(match), match, withResumeText ? a.getResumeText() : null);
    }

    private Job resolveJob(Long jobId, String title) {
        if (jobId != null) {
            Job job = jobs.findByIdAndCompany_Id(jobId, Tenant.companyId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "That job does not exist"));
            if (job.getStatus() == JobStatus.CLOSED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "That job is closed");
            }
            return job;
        }
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a job");
        }
        // Newest first, so if two jobs share a title the most recent open one wins.
        return jobs.findByCompany_IdOrderByCreatedAtDescIdDesc(Tenant.companyId()).stream()
                .filter(j -> j.getStatus() != JobStatus.CLOSED && j.getTitle().equalsIgnoreCase(title.trim()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No open job called \"" + title.trim() + "\""));
    }

    private static Integer parseExperience(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            int years = Integer.parseInt(text.trim());
            if (years < 0 || years > 50) throw new NumberFormatException();
            return years;
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Experience must be a number from 0 to 50");
        }
    }

    // "Java, Spring Boot, Docker" -> list. Max 30 skills of up to 50 characters; "|" is removed.
    private static List<String> parseSkills(String text) {
        if (text == null || text.isBlank()) return List.of();
        return java.util.Arrays.stream(text.split(","))
                .map(s -> s.replace("|", "").trim())
                .filter(s -> !s.isEmpty() && s.length() <= 50)
                .distinct()
                .limit(30)
                .toList();
    }
}