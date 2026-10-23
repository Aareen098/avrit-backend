package com.hireflow.settings;

import com.hireflow.common.ApiException;
import com.hireflow.auth.PasswordTokenService;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.mail.EmailService;
import com.hireflow.mail.EmailTemplates;
import com.hireflow.security.Tenant;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserDto;
import com.hireflow.user.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final SecureRandom random = new SecureRandom();
    private final CompanyRepository companies;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    private final PasswordTokenService tokens;
    private final EmailService emails;
    private final String frontendUrl;

    public SettingsService(CompanyRepository companies, UserRepository users, PasswordEncoder encoder,
                           PasswordTokenService tokens, EmailService emails,
                           @Value("${app.cors.allowed-origin}") String frontendUrl) {
        this.companies = companies;
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.emails = emails;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public SettingsDto get() {
        Company s = load();
        return new SettingsDto(company(s), ai(s), team());
    }

    @Transactional
    public CompanyDto saveCompany(CompanyDto req) {
        Company s = load();
        s.setName(req.name().trim());
        s.setWebsite(req.website().trim());
        s.setContactEmail(req.contactEmail().trim());
        return company(s);
    }

    @Transactional
    public AiDto saveAi(AiDto req) {
        Company s = load();
        s.setShortlistThreshold(req.shortlistThreshold());
        s.setShowExplanation(req.showExplanation());
        return ai(s);
    }

    // Creates the staff account and emails the person a link (valid 7 days) to set their own password.
    // The account starts with a random password nobody knows, so the link is the only way in.
    // If the email gets lost, the person can use "Forgot password" with the same address.
    @Transactional
    public List<UserDto> invite(InviteRequest req) {
        if (req.role() == Role.CANDIDATE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a staff role");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        Company company = load();
        User user = users.save(new User(email.substring(0, email.indexOf('@')), email,
                encoder.encode(randomSecret()), req.role(), company));
        String secret = tokens.issue(user, Duration.ofDays(7));
        emails.send(email, EmailTemplates.invite(company.getName(), roleLabel(req.role()),
                frontendUrl + "/reset-password?invite=1&token=" + secret));
        return team();
    }

    // ---- helpers ----

    // The logged-in admin's own company.
    private Company load() {
        return companies.findById(Tenant.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Company not found"));
    }

    private List<UserDto> team() {
        return users.findByCompany_IdOrderByIdAsc(Tenant.companyId()).stream().map(UserDto::from).toList();
    }

    private static CompanyDto company(Company s) {
        return new CompanyDto(s.getName(), s.getWebsite(), s.getContactEmail());
    }

    private static AiDto ai(Company s) {
        return new AiDto(s.getShortlistThreshold(), s.isShowExplanation());
    }

    private static String roleLabel(Role role) {
        return switch (role) {
            case ADMIN -> "Admin";
            case RECRUITER -> "Recruiter";
            case HIRING_MANAGER -> "Hiring Manager";
            default -> role.name();
        };
    }

    private String randomSecret() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        return sb.toString();
    }
}