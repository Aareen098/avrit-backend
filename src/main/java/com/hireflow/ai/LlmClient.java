package com.hireflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Talks to ANY OpenAI-compatible "chat completions" API (NVIDIA, OpenAI, Groq, OpenRouter, Ollama...).
// Which one is decided only by the three values in .env, so switching provider needs no code change.
@Component
public class LlmClient {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public LlmClient(@Value("${app.llm.base-url}") String baseUrl,
                     @Value("${app.llm.api-key}") String apiKey,
                     @Value("${app.llm.model}") String model) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey.trim();
        this.model = model.trim();
    }

    // False until LLM_API_KEY and LLM_MODEL are set in .env.
    public boolean enabled() {
        return !apiKey.isEmpty() && !model.isEmpty();
    }

    // Same as chat(), but the answer must be one JSON object. Ignores <think> notes and ```json fences.
    public JsonNode chatJson(String system, String user) {
        String text = chat(system, user).replaceAll("(?s)<think>.*?</think>", "");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalStateException("The AI answer could not be read");
        try {
            return mapper.readTree(text.substring(start, end + 1));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The AI answer could not be read");
        }
    }

    // Sends one system + one user message and returns the text of the answer.
    // Throws IllegalStateException with a short readable message on any failure.
    public String chat(String system, String user) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "max_tokens", 4000,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("The AI service answered with status " + response.statusCode());
            }
            JsonNode text = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (text.asText().isBlank()) throw new IllegalStateException("The AI service sent an empty answer");
            return text.asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The AI request was interrupted");
        } catch (HttpTimeoutException e) {
            throw new IllegalStateException("The AI service took too long to answer. Try again, or use a faster model");
        } catch (IOException e) {
            throw new IllegalStateException("Could not reach the AI service: " + e.getMessage());
        }
    }
}