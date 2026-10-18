package com.hireflow.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Turns text into a list of numbers (an "embedding") where texts with a similar MEANING get similar numbers.
// Uses the same OpenAI-compatible service and key as the AI analysis (LLM_API_KEY), with its own model.
@Component
public class EmbeddingClient {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final boolean asymmetric;

    public EmbeddingClient(@Value("${app.llm.base-url}") String baseUrl,
                           @Value("${app.llm.api-key}") String apiKey,
                           @Value("${app.embed.model}") String model,
                           @Value("${app.embed.asymmetric}") boolean asymmetric) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey.trim();
        this.model = model.trim();
        this.asymmetric = asymmetric;
    }

    // False until LLM_API_KEY and EMBED_MODEL are set in .env.
    public boolean enabled() {
        return !apiKey.isEmpty() && !model.isEmpty();
    }

    // One embedding per text, in the same order. isQuery tells NVIDIA's retrieval models whether this is a
    // search question or a CV piece (they are trained to treat the two differently).
    public List<float[]> embed(List<String> texts, boolean isQuery) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", texts);
            payload.put("encoding_format", "float");
            if (asymmetric) {
                payload.put("input_type", isQuery ? "query" : "passage");
                payload.put("truncate", "END");
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/embeddings"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("The embedding service answered with status " + response.statusCode());
            }

            float[][] result = new float[texts.size()][];
            for (JsonNode item : mapper.readTree(response.body()).path("data")) {
                int index = item.path("index").asInt();
                JsonNode numbers = item.path("embedding");
                float[] vector = new float[numbers.size()];
                for (int i = 0; i < vector.length; i++) vector[i] = (float) numbers.get(i).asDouble();
                if (index >= 0 && index < result.length) result[index] = vector;
            }
            for (float[] vector : result) {
                if (vector == null || vector.length == 0) {
                    throw new IllegalStateException("The embedding service sent an incomplete answer");
                }
            }
            return List.of(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The embedding request was interrupted");
        } catch (HttpTimeoutException e) {
            throw new IllegalStateException("The embedding service took too long to answer");
        } catch (IOException e) {
            throw new IllegalStateException("Could not reach the embedding service: " + e.getMessage());
        }
    }
}