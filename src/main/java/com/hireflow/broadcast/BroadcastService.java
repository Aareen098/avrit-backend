package com.hireflow.broadcast;

import com.hireflow.broadcast.BroadcastDtos.Item;
import com.hireflow.broadcast.BroadcastDtos.Preview;
import com.hireflow.broadcast.BroadcastDtos.SendRequest;
import com.hireflow.common.ApiException;
import com.hireflow.company.Company;
import com.hireflow.company.CompanyRepository;
import com.hireflow.mail.EmailService;
import com.hireflow.mail.EmailTemplates;
import com.hireflow.notification.Notification;
import com.hireflow.notification.NotificationRepository;
import com.hireflow.platform.PlatformAccess;
import com.hireflow.usage.UsageEventRepository;
import com.hireflow.usage.UsageKind;
import com.hireflow.user.Role;
import com.hireflow.user.User;
import com.hireflow.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The platform owner sends a message to one person, one company's staff, all company staff, or all candidates:
// as a bell notification, as an email, or both. Only PlatformController calls this, and only for the owner.
//  - The owner's own account is never a recipient.
//  - Emails go only to verified addresses, and a send that would pass the daily email limit is refused
//    (Brevo's free plan allows 300 a day), so one message can never use up the whole day.
@Service
public class BroadcastService {

    public static final String USER = "USER";
    public static final String COMPANY = "COMPANY";
    public static final String ALL_STAFF = "ALL_STAFF";
    public static final String ALL_CANDIDATES = "ALL_CANDIDATES";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_TITLE = 100;
    private static final int MAX_MESSAGE = 2000;

    private final BroadcastRepository broadcasts;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final NotificationRepository notifications;
    private final EmailService email;
    private final UsageEventRepository usageEvents;
    private final long emailDailyLimit;

    public BroadcastService(BroadcastRepository broadcasts, UserRepository users, CompanyRepository companies,
                            NotificationRepository notifications, EmailService email, UsageEventRepository usageEvents,
                            @Value("${app.mail.daily-limit:300}") long emailDailyLimit) {
        this.broadcasts = broadcasts;
        this.users = users;
        this.companies = companies;
        this.notifications = notifications;
        this.email = email;
        this.usageEvents = usageEvents;
        this.emailDailyLimit = emailDailyLimit;
    }

    // Who would get it. Nothing is sent. The screen shows this before the owner confirms.
    @Transactional(readOnly = true)
    public Preview preview(SendRequest req) {
        Audience a = resolve(req);
        return new Preview(a.label, a.accounts.size(), emailTargets(a, req.email()).size(), emailsLeftToday());
    }

    @Transactional
    public Item send(SendRequest req, String byEmail) {
        String title = req.title() == null ? "" : req.title().trim();
        String message = req.message() == null ? "" : req.message().trim();
        if (title.isEmpty() || message.isEmpty()) throw bad("Write a title and a message");
        if (title.length() > MAX_TITLE) throw bad("The title can have at most " + MAX_TITLE + " characters");
        if (message.length() > MAX_MESSAGE) throw bad("The message can have at most " + MAX_MESSAGE + " characters");
        if (!req.inApp() && !req.email()) throw bad("Choose in-app, email, or both");

        Audience a = resolve(req);
        List<String> emails = emailTargets(a, req.email());
        if (req.email() && emails.isEmpty()) throw bad("Nobody in this audience has a verified email to send to");
        if (!req.inApp() && a.accounts.isEmpty() && emails.isEmpty()) throw bad("There is nobody to send this to");
        if (emails.size() > emailsLeftToday()) {
            throw bad("This would send " + emails.size() + " emails but only " + emailsLeftToday()
                    + " are left today. Send it in-app only, or to a smaller audience.");
        }

        int inAppCount = 0;
        if (req.inApp()) {
            List<Notification> list = a.accounts.stream()
                    .map(u -> new Notification(u.getId(), "PLATFORM", title, message, null)).toList();
            notifications.saveAll(list);
            inAppCount = list.size();
        }
        if (req.email()) {
            var mail = EmailTemplates.announcement(title, message);
            emails.forEach(to -> email.send(to, mail));
        }
        Broadcast saved = broadcasts.save(new Broadcast(req.audience(), a.label, title, message, req.inApp(),
                req.email(), inAppCount, req.email() ? emails.size() : 0, byEmail));
        return Item.from(saved);
    }

    @Transactional(readOnly = true)
    public List<Item> history() {
        return broadcasts.findTop50ByOrderByIdDesc().stream().map(Item::from).toList();
    }

    // ---- who ----

    private record Audience(String label, List<User> accounts, String directEmail) {
    }

    private Audience resolve(SendRequest req) {
        String kind = req.audience() == null ? "" : req.audience();
        switch (kind) {
            case USER -> {
                String to = req.target() == null ? "" : req.target().trim().toLowerCase(Locale.ROOT);
                if (!EMAIL.matcher(to).matches()) throw bad("Enter a valid email address");
                if (PlatformAccess.isMasterEmail(to)) throw bad("That is your own owner account");
                List<User> found = users.findByEmail(to).map(List::of).orElse(List.of());
                return new Audience(to, found, to);
            }
            case COMPANY -> {
                Company c = companies.findById(parseId(req.target()))
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
                return new Audience(c.getName() + " team", clean(users.findByCompany_IdOrderByIdAsc(c.getId())), null);
            }
            case ALL_STAFF -> {
                return new Audience("All company staff", clean(users.findByCompanyIsNotNull()), null);
            }
            case ALL_CANDIDATES -> {
                return new Audience("All candidates", clean(users.findByRole(Role.CANDIDATE)), null);
            }
            default -> throw bad("Choose who to send to");
        }
    }

    // The owner never receives their own messages, and an account is counted once.
    private static List<User> clean(List<User> list) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User u : list) {
            if (!PlatformAccess.isMasterEmail(u.getEmail())) unique.putIfAbsent(u.getId(), u);
        }
        return new ArrayList<>(unique.values());
    }

    // Verified accounts only. For one typed address, it is emailed even if it has no account.
    private static List<String> emailTargets(Audience a, boolean wantEmail) {
        if (!wantEmail) return List.of();
        if (a.directEmail != null) return List.of(a.directEmail);
        return a.accounts.stream().filter(User::isEmailVerified).map(User::getEmail).distinct().toList();
    }

    private long emailsLeftToday() {
        Instant start = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long sent = usageEvents.byKind(start, start.plusSeconds(86_400)).stream()
                .filter(r -> UsageKind.EMAIL.equals(r[0])).mapToLong(r -> ((Number) r[1]).longValue()).sum();
        return Math.max(0, emailDailyLimit - sent);
    }

    private static Long parseId(String target) {
        try {
            return Long.parseLong(target == null ? "" : target.trim());
        } catch (NumberFormatException e) {
            throw bad("Choose a company");
        }
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}