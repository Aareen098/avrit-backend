package com.hireflow.job;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /api/public/health - no database call. For uptime pings now and a load balancer health check later.
@RestController
public class PublicHealthController {

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}