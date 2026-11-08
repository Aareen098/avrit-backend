package com.hireflow.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.usage.UsageKind;
import com.hireflow.usage.UsageService;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Sends email through Brevo's HTTPS API (Render's free plan blocks SMTP ports).
// Sending never slows down or breaks a request: it runs on a small background pool, and if the
// request is inside a database transaction it only starts after that transaction is saved.
// Without BREVO_API_KEY / MAIL_FROM nothing is sent; the link in the email is printed in the log instead.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern LINK = Pattern.compile("href=\"([^\"]+)\"");

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final UsageService usage;

    public EmailService(@Value("${app.mail.api-key:}") String apiKey,
                        @Value("${app.mail.from-email:}") String fromEmail,
                        @Value("${app.mail.from-name:Avrit}") String fromName,
                        UsageService usage) {
        this.usage = usage;
        this.apiKey = apiKey.trim();
        this.fromEmail = fromEmail.trim();
        this.fromName = fromName.trim();
    }

    // An email that belongs to no company (sign-in link, password reset, owner broadcast).
    public void send(String to, Mail mail) { send(to, mail, null); }

    // An email caused by a company (interview, rejection, job closed, team invite).
    // companyId is only used to count the email in that company's usage.
    public void send(String to, Mail mail, Long companyId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { queue(to, mail, companyId); }
            });
        } else {
            queue(to, mail, companyId);
        }
    }

    private void queue(String to, Mail mail, Long companyId) { pool.submit(() -> deliver(to, mail, companyId)); }

    private void deliver(String to, Mail mail, Long companyId) {
        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            Matcher m = LINK.matcher(mail.html());
            log.info("Email is not configured, so it was NOT sent. To: {} | Subject: {} | Link: {}",
                    to, mail.subject(), m.find() ? m.group(1) : "-");
            return;
        }
        try {
            String body = json.writeValueAsString(Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", mail.subject(),
                    "htmlContent", mail.html()));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .header("accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Email to {} failed with status {}: {}", to, response.statusCode(), response.body());
            } else {
                usage.record(companyId, UsageKind.EMAIL, 1);   // counted for the company (if any) and the platform owner
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.toString());
        }
    }

    @PreDestroy
    void shutdown() {
        pool.shutdown();
    }
}