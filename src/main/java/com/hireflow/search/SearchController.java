package com.hireflow.search;

import com.hireflow.application.Application;
import com.hireflow.application.ApplicationRepository;
import com.hireflow.application.ApplicationStage;
import com.hireflow.candidate.CandidateService;
import com.hireflow.common.ApiException;
import com.hireflow.security.Tenant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Staff only.
@RestController
@RequestMapping("/search")
public class SearchController {

    // One search result: the candidate, how close the best CV piece is (0-1) and that piece of text.
    public record Result(Long id, String name, String job, Integer aiScore, ApplicationStage stage,
                         double similarity, String evidence) {
    }

    private final SemanticSearchService search;
    private final ApplicationRepository applications;
    private final CandidateService candidates;

    public SearchController(SemanticSearchService search, ApplicationRepository applications, CandidateService candidates) {
        this.search = search;
        this.applications = applications;
        this.candidates = candidates;
    }

    // GET /api/search/candidates?q=someone who built payment systems
    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','HIRING_MANAGER')")
    public List<Result> candidates(@RequestParam String q) {
        String query = q.trim();
        if (query.length() < 2 || query.length() > 300) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Search text must be 2 to 300 characters");
        }
        Long companyId = Tenant.companyId();
        List<SemanticSearchService.Hit> hits = search.search(query, 10);

        // Loaded again with a company check, so nothing outside the company can ever be returned.
        Map<Long, Application> byId = applications.findAllById(hits.stream().map(SemanticSearchService.Hit::applicationId).toList())
                .stream()
                .filter(a -> a.getJob().getCompany().getId().equals(companyId))
                .collect(Collectors.toMap(Application::getId, Function.identity()));
        return hits.stream()
                .filter(h -> byId.containsKey(h.applicationId()))
                .map(h -> {
                    Application a = byId.get(h.applicationId());
                    return new Result(a.getId(), a.getName(), a.getJob().getTitle(), candidates.score(a),
                            a.getStage(), Math.round(h.similarity() * 1000) / 1000.0, h.snippet());
                })
                .toList();
    }

    // POST /api/search/reindex - reads the CVs that are not searchable yet (Admin only). Runs in the background.
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> reindex() {
        return Map.of("queued", search.indexMissing(Tenant.companyId()));
    }
}