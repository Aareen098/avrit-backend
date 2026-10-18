package com.hireflow.candidate;

import com.hireflow.application.Application;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

// Reads the text out of a CV (PDF or Word) with Apache Tika, then finds skills and years of
// experience in that text with simple word matching. No AI service is used yet - the LLM
// batch will build on the text saved here.
@Component
public class ResumeAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalyzer.class);
    private static final int MAX_CHARS = 100_000;
    private static final int MAX_SKILLS = 40;

    // Words that are also plain English (Express, Rest, Excel, Spring...) are left out on purpose.
    private static final List<String> COMMON_SKILLS = List.of(
            "Java", "Spring Boot", "Hibernate", "JPA", "Maven", "Gradle", "JUnit", "Kotlin", "Scala",
            "Python", "Django", "Flask", "FastAPI", "JavaScript", "TypeScript", "React", "Redux", "Next.js",
            "Vue", "Angular", "Node.js", "Express.js", "HTML", "CSS", "Tailwind", "SASS", "Webpack", "Vite",
            "Jest", "Cypress", "Selenium", "Postman", "SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis",
            "Elasticsearch", "Kafka", "RabbitMQ", "GraphQL", "RESTful", "Microservices", "Docker",
            "Kubernetes", "AWS", "Azure", "GCP", "Terraform", "Jenkins", "GitHub Actions", "CI/CD", "Git",
            "Linux", "C++", "C#", ".NET", "ASP.NET", "PHP", "Laravel", "Ruby", "Rails", "Swift", "Android",
            "Flutter", "React Native", "Machine Learning", "TensorFlow", "PyTorch", "Pandas", "NumPy",
            "Figma", "Photoshop", "Illustrator", "Prototyping", "User research", "Wireframing", "Canva",
            "Agile", "Scrum", "Jira", "Tableau", "Power BI", "Microsoft Excel");

    // Other ways people write the same skill -> the name we store.
    private static final Map<String, String> ALIASES = Map.of(
            "postgres", "PostgreSQL",
            "k8s", "Kubernetes",
            "reactjs", "React",
            "react.js", "React",
            "nodejs", "Node.js",
            "springboot", "Spring Boot");

    private static final Pattern YEARS = Pattern.compile(
            "(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)\\s*(?:of\\s+)?(?:[a-z]+\\s+){0,2}experience",
            Pattern.CASE_INSENSITIVE);

    private final Tika tika = new Tika();

    public ResumeAnalyzer() {
        tika.setMaxStringLength(MAX_CHARS);
    }

    public record Analysis(String text, List<String> skills, Integer years) {
    }

    // Reads the CV and saves text, skills and experience on the application.
    // Skills already on the application (typed by staff) stay first. Never throws.
    public void fill(Application a, Resource file) {
        Analysis analysis = analyze(file, a.getJob().getSkills());
        a.setResumeText(analysis.text());

        List<String> merged = new ArrayList<>(a.getSkills());
        for (String skill : analysis.skills()) {
            if (merged.stream().noneMatch(m -> m.equalsIgnoreCase(skill))) merged.add(skill);
        }
        a.setSkills(merged.size() > MAX_SKILLS ? merged.subList(0, MAX_SKILLS) : merged);

        if (a.getExperienceYears() == null && analysis.years() != null) {
            a.setExperienceYears(analysis.years());
        }
    }

    public Analysis analyze(Resource file, List<String> jobSkills) {
        String text = read(file);
        if (text.isBlank()) return new Analysis("", List.of(), null);
        return new Analysis(text, detectSkills(text, jobSkills), detectYears(text));
    }

    // ---- helpers ----

    private String read(Resource file) {
        try (InputStream in = file.getInputStream()) {
            String text = tika.parseToString(in);
            // PostgreSQL cannot store the NUL character, and PDFs sometimes contain it.
            return text.replace("\u0000", "").replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        } catch (Exception e) {
            log.warn("Could not read resume text: {}", e.toString());
            return "";
        }
    }

    // The job's own skills are checked first, so they come first in the list.
    private List<String> detectSkills(String text, List<String> jobSkills) {
        String haystack = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        Map<String, String> vocabulary = new LinkedHashMap<>();
        for (String s : jobSkills) vocabulary.putIfAbsent(s.toLowerCase(Locale.ROOT), s);
        for (String s : COMMON_SKILLS) vocabulary.putIfAbsent(s.toLowerCase(Locale.ROOT), s);

        Set<String> found = new LinkedHashSet<>();
        vocabulary.forEach((key, display) -> {
            if (containsWord(haystack, key)) found.add(display);
        });
        ALIASES.forEach((alias, name) -> {
            if (containsWord(haystack, alias)) {
                found.add(vocabulary.getOrDefault(name.toLowerCase(Locale.ROOT), name));
            }
        });
        return found.stream().limit(MAX_SKILLS).toList();
    }

    // Whole-word match, so "Java" is not found inside "JavaScript" or "SQL" inside "NoSQL".
    private static boolean containsWord(String text, String word) {
        int from = 0;
        while ((from = text.indexOf(word, from)) >= 0) {
            int end = from + word.length();
            boolean startOk = from == 0 || !isSkillChar(text.charAt(from - 1));
            boolean endOk = end == text.length() || !isSkillChar(text.charAt(end));
            if (startOk && endOk) return true;
            from++;
        }
        return false;
    }

    private static boolean isSkillChar(char c) {
        return Character.isLetterOrDigit(c) || c == '+' || c == '#';
    }

    // "5 years of experience", "3+ yrs experience" -> the largest number found (0-50), or null.
    private static Integer detectYears(String text) {
        Integer best = null;
        Matcher m = YEARS.matcher(text);
        while (m.find()) {
            int years = Integer.parseInt(m.group(1));
            if (years <= 50 && (best == null || years > best)) best = years;
        }
        return best;
    }
}