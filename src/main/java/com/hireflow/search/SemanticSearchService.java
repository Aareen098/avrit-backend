package com.hireflow.search;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationRepository;
import com.hireflow.common.ApiException;
import com.hireflow.security.Tenant;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// Search by MEANING. Each CV is cut into small pieces, every piece is turned into an embedding and saved
// in the cv_chunks table (pgvector). A question is turned into an embedding too, and the database returns
// the pieces closest to it - so "someone who built payment systems" finds a CV that says "Razorpay integration".
@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);
    private static final int CHUNK_CHARS = 900;
    private static final int OVERLAP = 150;
    private static final int MAX_CHUNKS = 40;
    private static final int BATCH = 16;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final EmbeddingClient embedder;
    private final ApplicationRepository applications;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public SemanticSearchService(EmbeddingClient embedder, ApplicationRepository applications,
                                 JdbcTemplate jdbc, TransactionTemplate tx) {
        this.embedder = embedder;
        this.applications = applications;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    // The best matching CV piece of one candidate.
    public record Hit(Long applicationId, double similarity, String snippet) {
    }

    public boolean enabled() {
        return embedder.enabled();
    }

    // ---- indexing ----

    // Used right after someone applies: runs in the background.
    public void indexLater(Long applicationId) {
        if (!enabled()) return;
        pool.submit(() -> {
            try {
                index(applicationId);
            } catch (RuntimeException e) {
                log.warn("Indexing application {} failed: {}", applicationId, e.getMessage());
            }
        });
    }

    // Queues every CV of this company that is not in the index yet (one at a time, to respect rate limits).
    public int indexMissing(Long companyId) {
        if (!enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Semantic search is not set up yet (missing EMBED_MODEL)");
        }
        Set<Long> done = new HashSet<>(jdbc.queryForList(
                "select distinct application_id from cv_chunks where company_id = ?", Long.class, companyId));
        List<Long> todo = applications.findByJob_Company_IdOrderByAppliedAtDescIdDesc(companyId).stream()
                .filter(a -> a.getResumeText() != null && !a.getResumeText().isBlank() && !done.contains(a.getId()))
                .map(Application::getId)
                .toList();
        if (!todo.isEmpty()) {
            pool.submit(() -> {
                for (Long id : todo) {
                    try {
                        index(id);
                        Thread.sleep(1600); // free tier allows about 40 requests a minute
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (RuntimeException e) {
                        log.warn("Indexing application {} failed: {}", id, e.getMessage());
                    }
                }
                log.info("Indexed {} CV(s) for semantic search", todo.size());
            });
        }
        return todo.size();
    }

    // Cuts the CV text into pieces, embeds them, and replaces the old pieces of this candidate.
    void index(Long applicationId) {
        Application a = applications.findById(applicationId).orElse(null);
        if (a == null) return;
        List<String> chunks = split(a.getResumeText());
        if (chunks.isEmpty()) return;

        // Embed everything first, so a failure half-way never leaves the candidate with a broken index.
        List<float[]> vectors = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i += BATCH) {
            vectors.addAll(embedder.embed(chunks.subList(i, Math.min(i + BATCH, chunks.size())), false));
        }
        Long companyId = a.getJob().getCompany().getId();
        tx.executeWithoutResult(status -> {
            jdbc.update("delete from cv_chunks where application_id = ?", applicationId);
            for (int i = 0; i < chunks.size(); i++) {
                jdbc.update("insert into cv_chunks (application_id, company_id, chunk_index, content, embedding) "
                                + "values (?, ?, ?, ?, ?::vector)",
                        applicationId, companyId, i, chunks.get(i), literal(vectors.get(i)));
            }
        });
    }

    // ---- searching ----

    // Best candidates for a question, best first. Only the logged-in company's CVs are searched.
    public List<Hit> search(String query, int limit) {
        if (!enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Semantic search is not set up yet (missing EMBED_MODEL)");
        }
        String vector;
        try {
            vector = literal(embedder.embed(List.of(query), true).get(0));
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
        // "<=>" is pgvector's cosine distance (0 = same meaning), so 1 - distance is the similarity.
        List<Hit> pieces = jdbc.query(
                "select application_id, content, 1 - (embedding <=> ?::vector) as similarity "
                        + "from cv_chunks where company_id = ? order by embedding <=> ?::vector limit 40",
                (rs, row) -> new Hit(rs.getLong("application_id"), rs.getDouble("similarity"), shorten(rs.getString("content"))),
                vector, Tenant.companyId(), vector);

        // The pieces are already best first, so the first piece seen for a candidate is that candidate's best.
        Map<Long, Hit> best = new LinkedHashMap<>();
        for (Hit h : pieces) best.putIfAbsent(h.applicationId(), h);
        return best.values().stream().limit(limit).toList();
    }

    // ---- helpers ----

    // About 900 characters per piece, breaking at a line end or sentence end when possible, with a small overlap.
    static List<String> split(String text) {
        if (text == null) return List.of();
        String t = text.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < t.length() && out.size() < MAX_CHUNKS) {
            int end = Math.min(start + CHUNK_CHARS, t.length());
            if (end < t.length()) {
                int cut = Math.max(t.lastIndexOf('\n', end), t.lastIndexOf(". ", end));
                if (cut > start + CHUNK_CHARS / 2) end = cut + 1;
            }
            String piece = t.substring(start, end).trim();
            if (piece.length() >= 40) out.add(piece);
            if (end >= t.length()) break;
            start = Math.max(end - OVERLAP, start + 1);
        }
        return out;
    }

    // pgvector reads vectors written like [0.1,0.2,0.3]
    private static String literal(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    private static String shorten(String s) {
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 300 ? t.substring(0, 300) : t;
    }

    @PreDestroy
    void stop() {
        pool.shutdownNow();
    }
}