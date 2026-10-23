package com.hireflow.mail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// The emails the app sends. Everything typed by a person is escaped before it goes into the HTML.
public final class EmailTemplates {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private EmailTemplates() {
    }

    public static Mail resetPassword(String link) {
        return new Mail("Reset your Avrit password", page(
                "Reset your password",
                "<p>We got a request to reset your password. This link works for 15 minutes and only once.</p>",
                "Choose a new password", link,
                "If you did not ask for this, you can ignore this email. Your password stays the same."));
    }

    public static Mail invite(String company, String role, String link) {
        return new Mail("You are invited to join " + company + " on Avrit", page(
                "You are invited",
                "<p><b>" + esc(company) + "</b> invited you to join their hiring team on Avrit as <b>" + esc(role) + "</b>.</p>"
                        + "<p>Set your password to get started. This link works for 7 days.</p>",
                "Accept invite and set password", link,
                "If you were not expecting this, you can ignore this email."));
    }

    public static Mail interview(String name, String job, String company, LocalDate date, LocalTime time,
                                 String type, String interviewer) {
        return new Mail("Interview scheduled: " + job + " at " + company, page(
                "Your interview is scheduled",
                "<p>Hi " + esc(name) + ",</p><p><b>" + esc(company) + "</b> would like to talk to you about <b>"
                        + esc(job) + "</b>.</p>"
                        + "<p><b>When:</b> " + date.format(DATE) + " at " + time.format(TIME) + "<br>"
                        + "<b>Type:</b> " + esc(type) + "<br>"
                        + "<b>With:</b> " + esc(interviewer) + "</p>",
                null, null,
                "You can also see this in your Avrit candidate portal."));
    }

    public static Mail rejected(String name, String job, String company) {
        return new Mail("Update on your application: " + job, page(
                "An update on your application",
                "<p>Hi " + esc(name) + ",</p><p>Thank you for applying for <b>" + esc(job) + "</b> at <b>"
                        + esc(company) + "</b>. After careful review, they decided not to move forward with your application this time.</p>"
                        + "<p>We encourage you to look at their other open roles.</p>",
                null, null, null));
    }

    public static Mail jobClosed(String name, String job, String company) {
        return new Mail("The " + job + " job has closed", page(
                "This job has closed",
                "<p>Hi " + esc(name) + ",</p><p>The <b>" + esc(job) + "</b> job at <b>" + esc(company)
                        + "</b> was closed before they could respond to your application. Thank you for applying.</p>"
                        + "<p>Please have a look at their other open jobs.</p>",
                null, null, null));
    }

    // ---- layout ----

    private static String page(String title, String bodyHtml, String button, String link, String footer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;padding:24px;color:#0f172a\">")
                .append("<h2 style=\"margin:0 0 16px\">").append(esc(title)).append("</h2>")
                .append("<div style=\"font-size:15px;line-height:1.6\">").append(bodyHtml).append("</div>");
        if (button != null && link != null) {
            sb.append("<p style=\"margin:24px 0\"><a href=\"").append(esc(link))
                    .append("\" style=\"background:#4f46e5;color:#ffffff;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:bold;display:inline-block\">")
                    .append(esc(button)).append("</a></p>")
                    .append("<p style=\"font-size:12px;color:#64748b\">Button not working? Copy this link into your browser:<br>")
                    .append(esc(link)).append("</p>");
        }
        if (footer != null) {
            sb.append("<p style=\"font-size:12px;color:#64748b;margin-top:24px\">").append(esc(footer)).append("</p>");
        }
        return sb.append("</div>").toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}