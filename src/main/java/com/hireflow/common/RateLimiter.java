package com.hireflow.common;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// "At most N actions per key in a time window". Kept in memory, so it is per server instance.
// With several servers (for example behind a load balancer) move this to Redis or use a gateway/WAF rule.
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean allow(String key, int max, Duration window) {
        if (hits.size() > 10_000) hits.clear(); // keeps memory bounded if someone sends many different keys
        long now = System.currentTimeMillis();
        long from = now - window.toMillis();
        Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < from) times.pollFirst();
            if (times.size() >= max) return false;
            times.addLast(now);
            return true;
        }
    }
}