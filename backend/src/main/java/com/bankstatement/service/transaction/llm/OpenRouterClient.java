package com.bankstatement.service.transaction.llm;

import com.bankstatement.config.OpenRouterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OpenRouterClient {

    private final OpenRouterProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenRouterClient(OpenRouterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("OpenRouter API key not configured");
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "temperature", properties.getTemperature(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://bankstatement.local")
                    .header("X-Title", "Bank Statement Generator")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenRouter HTTP " + response.statusCode() + ": "
                        + truncate(response.body(), 500));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("OpenRouter returned empty content");
            }
            return content.asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenRouter call interrupted", e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException illegal) {
                throw illegal;
            }
            throw new IllegalStateException("OpenRouter call failed: " + e.getMessage(), e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
