package com.hireflow.application;

import com.hireflow.candidate.ResumeAnalyzer;
import com.hireflow.common.ApiException;
import com.hireflow.job.Job;
import com.hireflow.job.JobRepository;
import com.hireflow.job.JobStatus;
import com.hireflow.storage.FileStorageService;
import com.hireflow.ai.AiScreeningService;
import com.hireflow.search.SemanticSearchService;
import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ApplicationService {

    // No 0/O/1/I so the code is easy to read out loud.
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    // Letters, digits and . _ % + - only, so commas, spaces and hidden characters are rejected.
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private final SecureRandom random = new SecureRandom();
    private final ApplicationRepository applications;
    private final JobRepository jobs;
    private final FileStorageService storage;
    private final ResumeAnalyzer analyzer;
    private final AiScreeningService screening;
    private final SemanticSearchService search;

    public ApplicationService(ApplicationRepository applications, JobRepository jobs,
                              FileStorageService storage, ResumeAnalyzer analyzer, AiScreeningService screening,
                              SemanticSearchService search) {
        this.screening = screening;
        this.search = search;
        this.applications = applications;
        this.jobs = jobs;
        this.storage = storage;
        this.analyzer = analyzer;
    }

    public ApplicationCreatedDto apply(Long jobId, String name, String email, String phone,
                                       String coverNote, MultipartFile resume) {
        if (jobId == null) throw badRequest("Choose a job to apply for");
        if (name == null || name.trim().length() < 2) throw badRequest("Enter your full name");
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw badRequest("Enter a valid email");

        Job job = jobs.findByIdAndStatus(jobId, JobStatus.OPEN)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This job is no longer available"));

        String cleanEmail = email.trim().toLowerCase();
        if (applications.existsByJob_IdAndEmailIgnoreCase(job.getId(), cleanEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already applied for this job");
        }

        // Checked last, so a rejected application never leaves a file behind.
        String storedName = storage.saveResume(resume);
        String originalName = resume.getOriginalFilename() == null ? "resume" : resume.getOriginalFilename();
        if (originalName.length() > 200) originalName = originalName.substring(0, 200);

        Application application = new Application(newReference(), job, name.trim(), cleanEmail,
                blankToNull(phone), blankToNull(coverNote), originalName, storedName);
        analyzer.fill(application, storage.load(storedName));
        Application saved = applications.save(application);
        screening.evaluateLater(saved.getId());
        search.indexLater(saved.getId());
        return new ApplicationCreatedDto(saved.getReference());
    }

    // Staff adding a candidate by hand (Add Candidate dialog). The CV is optional here.
    public Application addByStaff(Job job, String name, String email, String phone, Integer experience,
                                  List<String> skills, MultipartFile resume) {
        if (name == null || name.trim().length() < 2) throw badRequest("Enter the candidate's full name");
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw badRequest("Enter a valid email");

        String cleanEmail = email.trim().toLowerCase();
        if (applications.existsByJob_IdAndEmailIgnoreCase(job.getId(), cleanEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "This candidate has already applied for this job");
        }

        // Checked before saving the file, so a rejected request never leaves a file behind.
        String originalName = "manual-entry";
        String storedName = "";
        if (resume != null && !resume.isEmpty()) {
            storedName = storage.saveResume(resume);
            originalName = resume.getOriginalFilename() == null ? "resume" : resume.getOriginalFilename();
            if (originalName.length() > 200) originalName = originalName.substring(0, 200);
        }

        Application application = new Application(newReference(), job, name.trim(), cleanEmail,
                blankToNull(phone), null, originalName, storedName);
        application.setExperienceYears(experience);
        application.setSkills(skills);
        if (!storedName.isEmpty()) analyzer.fill(application, storage.load(storedName));
        Application saved = applications.save(application);
        if (!storedName.isEmpty()) {
            screening.evaluateLater(saved.getId());
            search.indexLater(saved.getId());
        }
        return saved;
    }

    // Reference + email must both match, so nobody can browse other people's applications.
    public ApplicationStatusDto status(String reference, String email) {
        return applications.findForStatus(reference.trim(), email.trim())
                .map(ApplicationStatusDto::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "We could not find an application with those details."));
    }

    public List<ApplicationStatusDto> mine(String email) {
        return applications.findByEmailIgnoreCaseOrderByAppliedAtDescIdDesc(email).stream()
                .map(ApplicationStatusDto::from)
                .toList();
    }

    private String newReference() {
        String reference;
        do {
            StringBuilder code = new StringBuilder("HF-");
            for (int i = 0; i < 6; i++) {
                code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            reference = code.toString();
        } while (applications.existsByReference(reference));
        return reference;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}