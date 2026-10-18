package com.hireflow.settings;

import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.security.Tenant;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserDto;
import com.hireflow.user.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final SecureRandom random = new SecureRandom();
    private final CompanyRepository companies;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public SettingsService(CompanyRepository companies, UserRepository users, PasswordEncoder encoder) {
        this.companies = companies;
        this.users = users;
        this.encoder = encoder;
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

    // Creates the staff account right away, inside the admin's own company. Emails are not built
    // yet, so the temporary password is printed in the server console for the admin to pass on.
    @Transactional
    public List<UserDto> invite(InviteRequest req) {
        if (req.role() == Role.CANDIDATE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a staff role");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String password = temporaryPassword();
        users.save(new User(email.substring(0, email.indexOf('@')), email, encoder.encode(password), req.role(), load()));
        log.info("Invited {} as {}. Temporary password: {}", email, req.role(), password);
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

    private String temporaryPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        return sb.toString();
    }
}