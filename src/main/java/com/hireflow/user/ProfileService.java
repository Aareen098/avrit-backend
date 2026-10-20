package com.hireflow.user;

import com.hireflow.application.ApplicationRepository;
import com.hireflow.candidate.ResumeFile;
import com.hireflow.common.ApiException;
import com.hireflow.storage.FileStorageService;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// A candidate's own profile. The profile holds ONE resume; replacing it removes the old file,
// unless an application was sent with it (those applications keep the file they applied with).
@Service
public class ProfileService {

    private static final Pattern URL = Pattern.compile("^https?://[^\\s]+$", Pattern.CASE_INSENSITIVE);

    private final UserRepository users;
    private final ApplicationRepository applications;
    private final FileStorageService storage;

    public ProfileService(UserRepository users, ApplicationRepository applications, FileStorageService storage) {
        this.users = users;
        this.applications = applications;
        this.storage = storage;
    }

    public ProfileDto get(String email) {
        return ProfileDto.from(find(email));
    }

    public ProfileDto save(String email, String name, String phone, String location, String headline, String about,
                           String linkedin, String github, String portfolio, MultipartFile resume) {
        User user = find(email);
        if (name == null || name.trim().length() < 2) throw bad("Enter your full name");

        user.setName(name.trim());
        user.updateProfile(
                text(phone, 30, "Phone"), text(location, 100, "Location"), text(headline, 120, "Headline"),
                text(about, 500, "About you"), url(linkedin), url(github), url(portfolio));

        String oldStored = user.getResumeStoredName();
        boolean replaced = resume != null && !resume.isEmpty();
        if (replaced) {
            // Checked and saved before anything else changes, so a bad file leaves the profile as it was.
            String storedName = storage.saveResume(resume);
            String original = resume.getOriginalFilename() == null ? "resume" : resume.getOriginalFilename();
            user.setResume(original.length() > 200 ? original.substring(0, 200) : original, storedName);
        }
        User saved = users.save(user);

        if (replaced && oldStored != null && !oldStored.isBlank() && !applications.existsByResumeStoredName(oldStored)) {
            storage.delete(oldStored);
        }
        return ProfileDto.from(saved);
    }

    public ResumeFile resume(String email) {
        User user = find(email);
        String stored = user.getResumeStoredName();
        if (stored == null || stored.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "You have not uploaded a resume yet");
        }
        return ResumeFile.of(storage.load(stored), user.getResumeFileName(), stored);
    }

    private User find(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private static String text(String value, int max, String label) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.length() > max) throw bad(label + " is too long (max " + max + " characters)");
        return v;
    }

    // Only http/https links are kept, so a profile can never hold a script link.
    private static String url(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (!v.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) v = "https://" + v;
        if (v.length() > 200 || !URL.matcher(v).matches()) throw bad("Enter a valid link, like https://github.com/you");
        return v;
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}