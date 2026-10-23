package com.hireflow.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    public EmailService(@Value("${app.mail.api-key:}") String apiKey,
                        @Value("${app.mail.from-email:}") String fromEmail,
                        @Value("${app.mail.from-name:Avrit}") String fromName) {
        this.apiKey = apiKey.trim();
        this.fromEmail = fromEmail.trim();
        this.fromName = fromName.trim();
    }

    public void send(String to, Mail mail) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    queue(to, mail);
                }
            });
        } else {
            queue(to, mail);
        }
    }

    private void queue(String to, Mail mail) {
        pool.submit(() -> deliver(to, mail));
    }

    private void deliver(String to, Mail mail) {
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